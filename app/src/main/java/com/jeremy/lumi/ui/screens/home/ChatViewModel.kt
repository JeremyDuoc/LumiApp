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
import com.jeremy.lumi.domain.usecase.ChatQuestion
import com.jeremy.lumi.domain.usecase.CycleContext
import com.jeremy.lumi.domain.usecase.CyclePredictor
import com.jeremy.lumi.domain.usecase.DailyInsightGenerator
import com.jeremy.lumi.domain.usecase.LumiRulesEngine
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
    private val rulesEngine: LumiRulesEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val saveRemindersInChat = chatPrefs.saveRemindersFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val messages: StateFlow<List<ChatMessageEntity>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = chatDao.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _askedQuestions = MutableStateFlow<Set<ChatQuestion>>(emptySet())

    /** Preguntas disponibles según el contexto actual del ciclo y preguntas ya hechas. */
    val availableQuestions: StateFlow<List<ChatQuestion>> = combine(
        rulesEngine.currentCycleContext, _askedQuestions
    ) { ctx, asked ->
        val questions = if (ctx != null) rulesEngine.getAvailableQuestions(ctx) else ChatQuestion.entries
        questions.filterNot { it in asked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatQuestion.entries)

    /** Estado para mostrar indicador de "Escribiendo..." de Lumi */
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    /** True cuando el contexto del ciclo aún no ha sido cargado por HomeViewModel.
     *  FIX P3-7: En lugar de fallar silenciosamente, el UI puede mostrar un aviso. */
    val isContextReady: StateFlow<Boolean> = rulesEngine.currentCycleContext
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        checkAndGenerateDailyInsight()
    }

    private fun checkAndGenerateDailyInsight() {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Verificar si el historial está completamente vacío
                val isChatEmpty = chatDao.getAllMessages().first().isEmpty()

                val cycles = repository.getAllCycles().first()
                val activeCycle = cycles.find { it.endDate == null }
                val closedCycles = cycles.filter { it.endDate != null }.sortedByDescending { it.startDate }

                if (activeCycle != null) {
                    val startDate = Instant.ofEpochMilli(activeCycle.startDate)
                        .atZone(ZoneId.of("UTC")).toLocalDate()

                    val cycleLen = if (closedCycles.size >= 3) {
                        CyclePredictor.weightedAverageCycleLength(closedCycles).toInt().coerceIn(15, 60)
                    } else {
                        activeCycle.cycleLength.coerceAtLeast(15)
                    }
                    val periodLen = activeCycle.periodLength.coerceAtLeast(1)
                    val isOnContraceptive = onboardingPrefs.isOnContraceptiveFlow.first()

                    val prediction = CyclePredictor.predict(
                        startDate = startDate,
                        cycleLength = cycleLen,
                        periodLength = periodLen,
                        closedCycles = closedCycles,
                        isOnContraceptive = isOnContraceptive
                    )

                    if (isChatEmpty) {
                        // ── INYECCIÓN DE MENSAJE DE BIENVENIDA CONTEXTUAL ──
                        // Crear un contexto simulado solo con la fase para el saludo
                        val welcomeCtx = CycleContext(
                            phase = prediction.currentPhase,
                            dayOfCycle = prediction.currentDayOfCycle,
                            dayOfPhase = prediction.dayOfPhase,
                            daysUntilPeriod = 0, daysUntilOvulation = 0, cycleLength = cycleLen, isLate = false, delayDays = 0
                        )
                        val welcomeMsg = rulesEngine.generateWelcomeMessage(welcomeCtx)
                        
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                text = welcomeMsg,
                                messageType = ChatMessageType.GREETING,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        // Marcamos el insight de hoy como visto para no saturar
                        chatPrefs.setLastInsightDate(LocalDate.now().toString())
                        return@withContext
                    }

                    // Lógica normal de Insights Diarios
                    val todayStr = LocalDate.now().toString()
                    val lastInsightDate = chatPrefs.lastInsightDateFlow.first()

                    if (lastInsightDate != todayStr) {
                        val userGoal = onboardingPrefs.userGoalFlow.first().name
                        val todayDate = LocalDate.now()
                        val daysUntilPeriodReal = java.time.temporal.ChronoUnit.DAYS.between(todayDate, prediction.nextPeriodDate).toInt()
                        val daysUntilOvulationReal = java.time.temporal.ChronoUnit.DAYS.between(todayDate, prediction.nextOvulationDate).toInt()

                        val chosenText = when {
                            daysUntilPeriodReal in 0..2 -> context.getString(R.string.chat_proactive_period, daysUntilPeriodReal)
                            daysUntilOvulationReal in 0..2 -> context.getString(R.string.chat_proactive_ovulation)
                            else -> {
                                val arrayResId = DailyInsightGenerator.getInsightForDay(
                                    phase = prediction.currentPhase,
                                    dayOfPhase = prediction.dayOfPhase,
                                    cycleDay = prediction.currentDayOfCycle,
                                    isLate = prediction.isLate,
                                    userGoal = userGoal
                                )
                                val options = context.resources.getStringArray(arrayResId)
                                options[Random.nextInt(options.size)]
                            }
                        }

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
                                val patternMsg = context.getString(
                                    R.string.chat_pattern_message,
                                    relevantPattern.symptomName.lowercase(),
                                    relevantPattern.daysBeforePeriod
                                )
                                chatDao.insertMessage(
                                    ChatMessageEntity(
                                        text = patternMsg,
                                        messageType = ChatMessageType.INSIGHT,
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
    }

    fun markAllAsRead() {
        viewModelScope.launch { chatDao.markAllAsRead() }
    }

    /**
     * Responde una pregunta predefinida usando [LumiRulesEngine].
     * Persiste tanto la pregunta como la respuesta en Room.
     */
    fun askQuestion(question: ChatQuestion) {
        // FIX P3-7: En lugar de retornar silenciosamente cuando el contexto
        // es null, el StateFlow isContextReady ya le indica al UI que Lumi
        // aún no está lista. Aquí simplemente no procedemos para no corromper
        // el historial con una respuesta vacía.
        val ctx = rulesEngine.currentCycleContext.value ?: return
        _askedQuestions.value = _askedQuestions.value + question
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Insertar la "pregunta" como mensaje del usuario
                chatDao.insertMessage(
                    ChatMessageEntity(
                        text        = question.displayText,
                        messageType = ChatMessageType.USER,
                        timestamp   = System.currentTimeMillis()
                    )
                )
            }
            
            // Simular que Lumi está pensando/escribiendo
            _isTyping.value = true
            kotlinx.coroutines.delay(1500)
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Generar respuesta del motor de reglas
                val answer = rulesEngine.answerQuestion(question, ctx)
                // Insertar la respuesta de Lumi
                chatDao.insertMessage(
                    ChatMessageEntity(
                        text        = answer,
                        messageType = ChatMessageType.GREETING,
                        timestamp   = System.currentTimeMillis()
                    )
                )
            }
            _isTyping.value = false
        }
    }

    // El contexto se obtiene reactivamente desde rulesEngine.currentCycleContext
}