package com.jeremy.lumi.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.usecase.CyclePredictor
import com.jeremy.lumi.domain.usecase.DelayState
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/** Modo de visualización del HomeScreen */
enum class HomeMode {
    NORMAL,         // Usuario con ciclo propio
    OBSERVER_GATE,  // Observador sin vínculos → invitar a vincular
    OBSERVER_HOME   // Observador con al menos un vínculo activo → mostrar ciclo vinculado
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LumiRepository,
    private val prefsManager: OnboardingPreferenceManager,
    private val partnerRepository: com.jeremy.lumi.data.remote.PartnerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCurrentCycle()
        observePreferences()
        observeHugs()
        observeLinkedCycles()
    }

    private var lastSeenHugTimestamps = mutableMapOf<String, Long>()
    private var hugAnimationJob: Job? = null

    /** Observa los vínculos activos del usuario y actualiza homeMode */
    private fun observeLinkedCycles() {
        viewModelScope.launch {
            partnerRepository.observeMyLinks().collect { links ->
                val activeLinks = links.filter {
                    it.status == com.jeremy.lumi.domain.model.LinkStatus.ACTIVE
                }
                _uiState.update { state ->
                    val mode = when {
                        state.isObserverOnly && activeLinks.isEmpty() -> HomeMode.OBSERVER_GATE
                        state.isObserverOnly && activeLinks.isNotEmpty() -> HomeMode.OBSERVER_HOME
                        else -> HomeMode.NORMAL
                    }
                    state.copy(
                        linkedCycles = activeLinks,
                        homeMode = mode
                    )
                }
            }
        }
    }

    private fun observeHugs() {
        viewModelScope.launch {
            val uid = partnerRepository.getCurrentUid()
            partnerRepository.observeMyLinks().collect { links ->
                for (link in links) {
                    val myHugTime = if (link.ownerUid == uid) link.lastPartnerCareAction?.timestamp else link.lastOwnerCareAction?.timestamp
                    val newHugTime = myHugTime ?: 0L

                    val lastSeen = lastSeenHugTimestamps[link.linkId] ?: 0L

                    if (newHugTime > 0L) {
                        if (lastSeen == 0L) {
                            lastSeenHugTimestamps[link.linkId] = newHugTime
                            val isRecent = Math.abs(System.currentTimeMillis() - newHugTime) < 2 * 60 * 1000
                            if (isRecent) {
                                playHugAnimation()
                            }
                        } else if (newHugTime != lastSeen) {
                            lastSeenHugTimestamps[link.linkId] = newHugTime
                            playHugAnimation()
                        }
                    }
                }
            }
        }
    }

    private fun playHugAnimation() {
        hugAnimationJob?.cancel()
        hugAnimationJob = viewModelScope.launch {
            _uiState.update { it.copy(showHugAnimation = true) }
            kotlinx.coroutines.delay(3500)
            _uiState.update { it.copy(showHugAnimation = false) }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefsManager.isObserverOnly.collect { isObserver ->
                _uiState.update { state ->
                    val mode = when {
                        isObserver && state.linkedCycles.isEmpty() -> HomeMode.OBSERVER_GATE
                        isObserver && state.linkedCycles.isNotEmpty() -> HomeMode.OBSERVER_HOME
                        else -> HomeMode.NORMAL
                    }
                    state.copy(isObserverOnly = isObserver, homeMode = mode)
                }
            }
        }
        viewModelScope.launch {
            prefsManager.isDiscreetModeFlow.collect { isDiscreet ->
                _uiState.update { it.copy(isDiscreetMode = isDiscreet) }
            }
        }
        viewModelScope.launch {
            prefsManager.isPregnantFlow.collect { pregnant ->
                _uiState.update { it.copy(isPregnant = pregnant) }
                loadCurrentCycle()
            }
        }
        viewModelScope.launch {
            prefsManager.userGoalFlow.collect { goal ->
                _uiState.update { it.copy(userGoal = goal.name) }
            }
        }
        viewModelScope.launch {
            prefsManager.delayBannerDismissedDayFlow.collect { dismissedDay ->
                _uiState.update { state ->
                    if (dismissedDay >= 0 && dismissedDay == state.currentDayOfCycle) {
                        state.copy(isLate = false)
                    } else state
                }
            }
        }

        // Calcular racha de registros
        viewModelScope.launch {
            repository.getAllLogs(descending = true).collect { logs ->
                var streak = 0
                val today = java.time.LocalDate.now()
                var currentCheckDate = today

                for (log in logs) {
                    val logDate = java.time.Instant.ofEpochMilli(log.dailyLog.date).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    if (logDate == currentCheckDate) {
                        streak++
                        currentCheckDate = currentCheckDate.minusDays(1)
                    } else if (logDate == today.minusDays(1) && streak == 0) {
                        streak++
                        currentCheckDate = today.minusDays(2)
                    } else if (logDate.isBefore(currentCheckDate)) {
                        break
                    }
                }
                _uiState.update { it.copy(logStreakDays = streak) }
                syncToPartner()
            }
        }
    }

    fun toggleDiscreetMode() {
        viewModelScope.launch {
            prefsManager.setDiscreetMode(!_uiState.value.isDiscreetMode)
        }
    }

    private fun syncToPartner() {
        // Los observadores no publican snapshot propio
        if (_uiState.value.isObserverOnly) return
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val sharePhase = prefsManager.sharePhaseFlow.first()
                val shareMood = prefsManager.shareMoodFlow.first()
                val shareSymptoms = prefsManager.shareSymptomsFlow.first()
                val sharePredictions = prefsManager.sharePredictionsFlow.first()

                val todayEpochMs = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
                val todayLog = repository.getDailyLog(todayEpochMs)

                val currentMood = if (shareMood) todayLog?.dailyLog?.mood else null
                val topSymptoms = if (shareSymptoms) {
                    todayLog?.symptoms?.map { it.name } ?: emptyList()
                } else emptyList()

                val snapshot = com.jeremy.lumi.domain.model.CycleSnapshot(
                    currentPhase = if (sharePhase) state.currentPhase else CyclePhase.UNKNOWN,
                    daysUntilNextPhase = if (sharePredictions) (state.prediction?.daysUntilNextPeriod ?: 0) else 0,
                    isLate = if (sharePredictions) state.isLate else false,
                    delayDays = if (sharePredictions) state.delayDays else 0,
                    currentMood = currentMood,
                    topSymptoms = topSymptoms
                )
                partnerRepository.publishSnapshot(snapshot)
                _uiState.update { it.copy(syncError = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(syncError = true) }
            }
        }
    }

    private fun loadCurrentCycle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val cycle = repository.getCurrentActiveCycle()
            val closedCycles = repository.getClosedCycles()

            if (cycle != null) {
                val startDate = Instant.ofEpochMilli(cycle.startDate)
                    .atZone(ZoneId.of("UTC")).toLocalDate()

                val cycleLen = if (closedCycles.size >= 3) {
                    CyclePredictor.weightedAverageCycleLength(closedCycles).toInt()
                        .coerceIn(15, 60)
                } else {
                    cycle.cycleLength.coerceAtLeast(15)
                }
                val periodLen = cycle.periodLength.coerceAtLeast(1)

                val prediction = CyclePredictor.predict(
                    startDate = startDate,
                    cycleLength = cycleLen,
                    periodLength = periodLen,
                    closedCycles = closedCycles,
                    isPregnant = _uiState.value.isPregnant
                )

                val today = LocalDate.now()

                val weekDays = (-7..14).map { offset ->
                    val date = today.plusDays(offset.toLong())
                    val dayWrapped = CyclePredictor.dayInCycleWrapped(startDate, cycleLen, date)
                    CycleDayUi(
                        date = date,
                        dayOfMonth = date.dayOfMonth,
                        weekdayLabel = date.dayOfWeek
                            .getDisplayName(TextStyle.SHORT, Locale("es"))
                            .replaceFirstChar { it.uppercase() },
                        phase = CyclePredictor.phaseForDay(dayWrapped, cycleLen, periodLen, isPregnant = _uiState.value.isPregnant),
                        isToday = offset == 0
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeCycle = cycle,
                        currentPhase = prediction.currentPhase,
                        currentDayOfCycle = prediction.currentDayOfCycle,
                        prediction = prediction,
                        weekDays = weekDays,
                        delayState = prediction.delayState,
                        delayDays = prediction.delayDays,
                        isLate = prediction.isLate
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentPhase = CyclePhase.UNKNOWN,
                        prediction = null,
                        weekDays = emptyList(),
                        delayState = DelayState.ON_TIME,
                        delayDays = 0,
                        isLate = false
                    )
                }
            }
            syncToPartner()
        }
    }

    /**
     * La usuaria registra que le llegó la regla hoy.
     */
    fun startNewPeriod() {
        viewModelScope.launch {
            val todayDate = LocalDate.now()
            val todayMillis = todayDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            val closedCycles = repository.getClosedCycles()

            val currentCycle = repository.getCurrentActiveCycle()
            if (currentCycle != null) {
                val startOfLast = Instant.ofEpochMilli(currentCycle.startDate)
                    .atZone(ZoneId.of("UTC")).toLocalDate()
                
                // Guard: Si ya se inició un ciclo hoy, ignoramos para no corromper datos
                if (startOfLast == todayDate) {
                    return@launch
                }

                val realLength = java.time.temporal.ChronoUnit.DAYS
                    .between(startOfLast, todayDate).toInt().coerceIn(15, 90)

                val endOfLast = todayDate.minusDays(1)
                    .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

                repository.endCurrentCycle(endOfLast, realLength)
            }

            val newCycleLen = if (closedCycles.size >= 3) {
                CyclePredictor.weightedAverageCycleLength(closedCycles).toInt().coerceIn(15, 60)
            } else {
                currentCycle?.cycleLength ?: 28
            }
            val newPeriodLen = currentCycle?.periodLength ?: 5

            repository.startNewCycle(
                startDate = todayMillis,
                cycleLength = newCycleLen,
                periodLength = newPeriodLen
            )
            prefsManager.resetDelayBannerDismiss()
            loadCurrentCycle()
        }
    }

    fun dismissDelayBanner() {
        val dayOfCycle = _uiState.value.currentDayOfCycle
        viewModelScope.launch {
            prefsManager.dismissDelayBannerForDay(dayOfCycle)
        }
        _uiState.update { it.copy(isLate = false) }
    }
}