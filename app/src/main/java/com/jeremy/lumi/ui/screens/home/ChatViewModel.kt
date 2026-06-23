package com.jeremy.lumi.ui.screens.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.dao.ChatDao
import com.jeremy.lumi.data.local.entity.ChatMessageEntity
import com.jeremy.lumi.data.local.entity.ChatMessageType
import com.jeremy.lumi.data.preferences.ChatPreferenceManager
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.usecase.CyclePredictor
import com.jeremy.lumi.domain.usecase.DailyInsightGenerator
import com.jeremy.lumi.domain.usecase.SymptomCorrelationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val repository: LumiRepository,
    private val chatPrefs: ChatPreferenceManager,
    private val onboardingPrefs: OnboardingPreferenceManager,
    private val correlationEngine: SymptomCorrelationEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val saveRemindersInChat = chatPrefs.saveRemindersFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    // Los mensajes ahora vienen 100% de la base de datos (historial persistente)
    val messages: StateFlow<List<ChatMessageEntity>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = chatDao.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        checkAndGenerateDailyInsight()
    }

    private fun checkAndGenerateDailyInsight() {
        viewModelScope.launch {
            val todayStr = LocalDate.now().toString()
            val lastInsightDate = chatPrefs.lastInsightDateFlow.first()

            if (lastInsightDate != todayStr) {
                // Hay que generar el insight de hoy
                val cycles = repository.getAllCycles().first()
                val activeCycle = cycles.find { it.endDate == null }
                val closedCycles = cycles.filter { it.endDate != null }.sortedByDescending { it.startDate }

                if (activeCycle != null) {
                    val startDate = Instant.ofEpochMilli(activeCycle.startDate)
                        .atZone(ZoneId.systemDefault()).toLocalDate()

                    val cycleLen = if (closedCycles.size >= 3) {
                        CyclePredictor.weightedAverageCycleLength(closedCycles).toInt().coerceIn(15, 60)
                    } else {
                        activeCycle.cycleLength.coerceAtLeast(15)
                    }
                    val periodLen = activeCycle.periodLength.coerceAtLeast(1)

                    val prediction = CyclePredictor.predict(
                        startDate = startDate,
                        cycleLength = cycleLen,
                        periodLength = periodLen,
                        closedCycles = closedCycles
                    )

                    val userGoal = onboardingPrefs.userGoalFlow.first().name

                    val arrayResId = DailyInsightGenerator.getInsightForDay(
                        phase = prediction.currentPhase,
                        dayOfPhase = prediction.dayOfPhase,
                        cycleDay = prediction.currentDayOfCycle,
                        isLate = prediction.isLate,
                        userGoal = userGoal
                    )

                    val options = context.resources.getStringArray(arrayResId)
                    val chosenText = options[Random.nextInt(options.size)]

                    // Insertar físicamente en Room
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            text = chosenText,
                            messageType = ChatMessageType.GREETING,
                            timestamp = System.currentTimeMillis()
                        )
                    )

                    // ── Motor de Correlación de Síntomas ──
                    val daysUntilPeriod = cycleLen - prediction.currentDayOfCycle + 1
                    if (daysUntilPeriod > 0) {
                        val patterns = correlationEngine.findPatterns()
                        // Avisar un día ANTES de que ocurra el síntoma
                        val relevantPattern = patterns.firstOrNull { it.daysBeforePeriod == daysUntilPeriod - 1 }
                        
                        if (relevantPattern != null) {
                            val patternMsg = "He notado un patrón: los últimos meses reportaste '${relevantPattern.symptomName.lowercase()}' justo ${relevantPattern.daysBeforePeriod} días antes de tu periodo. ¿Quieres que te lo recuerde en tu próximo ciclo para que estés preparada?"
                            chatDao.insertMessage(
                                ChatMessageEntity(
                                    text = patternMsg,
                                    messageType = ChatMessageType.SYSTEM,
                                    timestamp = System.currentTimeMillis() + 1000 // 1 segundo después
                                )
                            )
                        }
                    }
                    
                    chatPrefs.setLastInsightDate(todayStr)
                }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch { chatDao.markAllAsRead() }
    }
}