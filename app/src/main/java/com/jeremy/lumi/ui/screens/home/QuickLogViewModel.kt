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
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
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

    val todayMidnightMillis: Long
        get() = com.jeremy.lumi.utils.LumiClock.todayUtcMillis()

    val todayDayOfMonth: Int get() = com.jeremy.lumi.utils.LumiClock.today().dayOfMonth
    /** Month value 1–12. */
    val todayMonth: Int      get() = com.jeremy.lumi.utils.LumiClock.today().monthValue
    val todayYear: Int       get() = com.jeremy.lumi.utils.LumiClock.today().year

    private val _todayLog = MutableStateFlow<DailyLogWithSymptoms?>(null)
    val todayLog: StateFlow<DailyLogWithSymptoms?> = _todayLog.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed = _loadFailed.asStateFlow()
    private val _saveFailed = MutableStateFlow(false)
    val saveFailed = _saveFailed.asStateFlow()

    sealed class UiEvent {
        object LogSaved : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    val activeCategories: StateFlow<Set<String>> = prefsManager.activeLogCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("physical", "digestive", "mucus", "intercourse"))

    val customSymptoms = prefsManager.customLogSymptomsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val favoriteSymptoms = prefsManager.favoriteLogSymptomsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.jeremy.lumi.data.preferences.DefaultLogFavorites)
    fun setCustomSymptoms(names: Set<String>) { viewModelScope.launch { prefsManager.setCustomLogSymptoms(names) } }
    fun setFavoriteSymptoms(names: Set<String>) { viewModelScope.launch { prefsManager.setFavoriteLogSymptoms(names) } }

    fun setActiveCategories(categories: Set<String>) {
        viewModelScope.launch { prefsManager.setActiveLogCategories(categories) }
    }

    private var loadJob: kotlinx.coroutines.Job? = null
    private var loadVersion = 0

    init { loadToday() }

    fun loadToday() {
        val version = ++loadVersion
        loadJob?.cancel()
        _isLoading.value = true
        _loadFailed.value = false
        _saveFailed.value = false
        loadJob = viewModelScope.launch {
            try {
                val log = repository.getDailyLog(todayMidnightMillis)
                if (version == loadVersion) _todayLog.value = log
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (version == loadVersion) _loadFailed.value = true
            } finally {
                if (version == loadVersion) _isLoading.value = false
            }
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
        showIntercourseOnCalendar: Boolean,
        sleepHours: Float?,
        energyLevel: Int?,
        stressLevel: Int?,
        waterIntake: Float?,
        basalBodyTemp: Float?,
        spotting: Boolean,
        symptomIntensities: Map<String, Int> = emptyMap()
    ) {
        if (_isSaving.value || _isLoading.value || _loadFailed.value) return
        val date = todayMidnightMillis
        _isSaving.value = true
        _saveFailed.value = false
        viewModelScope.launch {
            try {
                val existing = repository.getDailyLog(date)
                val cycleId = existing?.dailyLog?.cycleId?.takeIf { it != 0 }
                    ?: repository.getCycleForDate(date)?.id ?: 0
                val existingId = existing?.dailyLog?.id ?: 0

                val dailyLog = DailyLogEntity(
                    id = existingId,
                    cycleId = cycleId,
                    date = date,
                    flowIntensity = flow,
                    painLevel = painLevel,
                    mood = mood,
                    cervicalMucus = cervicalMucus,
                    notes = notes.takeIf { it.isNotBlank() },
                    hadIntercourse = hadIntercourse,
                    protectionUsed = if (hadIntercourse) protectionUsed else null,
                    contraceptionMethod = if (hadIntercourse) contraceptionMethod else null,
                    intercourseNotes = if (hadIntercourse) intercourseNotes else null,
                    showIntercourseOnCalendar = showIntercourseOnCalendar,
                    sleepHours = sleepHours,
                    energyLevel = energyLevel,
                    stressLevel = stressLevel,
                    waterIntake = waterIntake,
                    basalBodyTemp = basalBodyTemp,
                    spotting = spotting
                )

                val symptoms = selectedSymptoms.map { symptomName ->
                    SymptomEntity(dailyLogId = 0, name = symptomName,
                        intensity = symptomIntensities[symptomName]
                            ?: existing?.symptoms?.find { it.name == symptomName }?.intensity ?: 0)
                }

                repository.saveDailyLogWithSymptoms(dailyLog, symptoms)
                _todayLog.value = repository.getDailyLog(date)
                _uiEvent.send(UiEvent.LogSaved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _saveFailed.value = true
            } finally {
                _isSaving.value = false
            }
        }
    }
}
