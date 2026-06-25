package com.jeremy.lumi.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.local.entity.SymptomEntity
import com.jeremy.lumi.domain.repository.LumiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel liviano para el FAB de "registro rápido" en Home. A diferencia de
 * CalendarViewModel (que trabaja sobre el mes que se está visualizando), este
 * siempre opera sobre la fecha de HOY normalizada a medianoche.
 */
@HiltViewModel
class QuickLogViewModel @Inject constructor(
    private val repository: LumiRepository,
    private val prefsManager: OnboardingPreferenceManager
) : ViewModel() {

    private val todayMidnightMillis: Long = java.time.LocalDate.now()
        .atStartOfDay(java.time.ZoneId.of("UTC"))
        .toInstant()
        .toEpochMilli()

    val todayDayOfMonth: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    /** Month value 1–12 (Calendar.MONTH es 0-based, lo corregimos aquí). */
    val todayMonth: Int      = Calendar.getInstance().get(Calendar.MONTH) + 1
    val todayYear: Int       = Calendar.getInstance().get(Calendar.YEAR)

    private val _todayLog = MutableStateFlow<DailyLogWithSymptoms?>(null)
    val todayLog: StateFlow<DailyLogWithSymptoms?> = _todayLog.asStateFlow()

    sealed class UiEvent {
        object LogSaved : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val activeCategories: StateFlow<Set<String>> = prefsManager.activeLogCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("physical", "digestive", "mucus", "intercourse"))

    fun setActiveCategories(categories: Set<String>) {
        viewModelScope.launch { prefsManager.setActiveLogCategories(categories) }
    }

    init {
        viewModelScope.launch {
            _todayLog.value = repository.getDailyLog(todayMidnightMillis)
        }
    }

    fun saveToday(
        flow: String?,
        painLevel: Int,
        mood: String?,
        selectedSymptoms: List<String>,
        cervicalMucus: String?,
        notes: String,
        hadIntercourse: Boolean,
        protectionUsed: Boolean?,
        contraceptionMethod: String?,
        intercourseNotes: String?,
        showIntercourseOnCalendar: Boolean
    ) {
        viewModelScope.launch {
            try {
                val activeCycle = repository.getCurrentActiveCycle()
                val cycleId = activeCycle?.id ?: 0
                val existingId = _todayLog.value?.dailyLog?.id ?: 0

                val dailyLog = DailyLogEntity(
                    id = existingId,
                    cycleId = cycleId,
                    date = todayMidnightMillis,
                    flowIntensity = flow,
                    painLevel = painLevel,
                    mood = mood,
                    cervicalMucus = cervicalMucus,
                    notes = notes.takeIf { it.isNotBlank() },
                    hadIntercourse = hadIntercourse,
                    protectionUsed = if (hadIntercourse) protectionUsed else null,
                    contraceptionMethod = if (hadIntercourse) contraceptionMethod else null,
                    intercourseNotes = if (hadIntercourse) intercourseNotes else null,
                    showIntercourseOnCalendar = showIntercourseOnCalendar
                )

                val symptoms = selectedSymptoms.map { symptomName ->
                    SymptomEntity(dailyLogId = 0, name = symptomName, intensity = painLevel)
                }

                repository.saveDailyLogWithSymptoms(dailyLog, symptoms)
                _todayLog.value = repository.getDailyLog(todayMidnightMillis)
                _uiEvent.emit(UiEvent.LogSaved)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Error al guardar: ${e.message}"))
            }
        }
    }
}