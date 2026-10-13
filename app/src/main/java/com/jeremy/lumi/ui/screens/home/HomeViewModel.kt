package com.jeremy.lumi.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.ai.LumiAIPredictor
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.usecase.CycleContext
import com.jeremy.lumi.domain.usecase.CyclePrediction
import com.jeremy.lumi.domain.usecase.CyclePredictor
import com.jeremy.lumi.domain.usecase.DelayState
import com.jeremy.lumi.domain.usecase.LumiRulesEngine
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import com.jeremy.lumi.utils.LumiClock
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
    private val repository       : LumiRepository,
    private val prefsManager     : OnboardingPreferenceManager,
    private val partnerRepository: com.jeremy.lumi.domain.repository.PartnerRepository,
    private val aiPredictor      : LumiAIPredictor,
    private val rulesEngine      : LumiRulesEngine,
    private val healthConnectManager: com.jeremy.lumi.data.health.HealthConnectManager,
    private val widgetUpdater    : com.jeremy.lumi.ui.widget.WidgetUpdater,
    private val getEffectiveCyclePredictionUseCase: com.jeremy.lumi.domain.usecase.GetEffectiveCyclePredictionUseCase
) : ViewModel() {

    sealed interface HomeEvent {
        data class CycleStarted(val message: String) : HomeEvent
        data class CycleRestored(val message: String) : HomeEvent
    }

    private val _eventFlow = kotlinx.coroutines.flow.MutableSharedFlow<HomeEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
        observeHugs()
        observeLinkedCycles()
        viewModelScope.launch {
            LumiClock.offsetDays.collect {
                loadCurrentCycle()
            }
        }
    }

    private var lastSeenHugTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private var hugAnimationJob: Job? = null

    // ── Cache para recálculo dinámico al tocar días del timeline ───────────
    private var cachedStartDate: LocalDate? = null
    private var cachedCycleLen: Int = 28
    private var cachedPeriodLen: Int = 5
    private var cachedClosedCycles: List<com.jeremy.lumi.data.local.entity.CycleEntity> = emptyList()
    private var cachedIsPregnant: Boolean = false
    private var cachedIsOnContraceptive: Boolean = false
    private var baseTodayPrediction: CyclePrediction? = null

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
            prefsManager.userNameFlow.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
        viewModelScope.launch {
            prefsManager.isPregnantFlow.collect { pregnant ->
                _uiState.update { it.copy(isPregnant = pregnant) }
                loadCurrentCycle(isPregnant = pregnant)
            }
        }
        // FIX P1-3: Observar cambios de modo pastilla reactivamente,
        // igual que isPregnantFlow, para que el Home se recargue al instante.
        viewModelScope.launch {
            prefsManager.isOnContraceptiveFlow.collect { isOnContraceptive ->
                _uiState.update { it.copy(isOnContraceptive = isOnContraceptive) }
                loadCurrentCycle()
            }
        }
        viewModelScope.launch {
            prefsManager.userGoalFlow.collect { goal ->
                _uiState.update { it.copy(userGoal = goal.name) }
            }
        }
        viewModelScope.launch {
            prefsManager.isHealthConnectEnabledFlow.collect { enabled ->
                if (enabled) {
                    syncHealthConnectData()
                }
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

        val logsFlow = repository.getAllLogs(descending = true)
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

        // Calcular racha de registros
        viewModelScope.launch {
            logsFlow.collect { logs ->
                var streak = 0
                val today = LumiClock.today()
                var currentCheckDate = today

                for (log in logs) {
                    val logDate = java.time.Instant.ofEpochMilli(log.dailyLog.date).atZone(java.time.ZoneId.of("UTC")).toLocalDate()
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
            }
        }

        @OptIn(kotlinx.coroutines.FlowPreview::class)
        viewModelScope.launch {
            logsFlow
                .debounce(500)
                .collect {
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
                val sharePhase = prefsManager.sharePhaseFlow.first()
                val shareMood = prefsManager.shareMoodFlow.first()
                val shareSymptoms = prefsManager.shareSymptomsFlow.first()
                val sharePredictions = prefsManager.sharePredictionsFlow.first()

                val todayEpochMs = LumiClock.today().atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
                val todayLog = repository.getDailyLog(todayEpochMs)

                val currentMood = if (shareMood) todayLog?.dailyLog?.mood else null
                val topSymptoms = if (shareSymptoms) {
                    todayLog?.symptoms?.map { it.name } ?: emptyList()
                } else emptyList()

                // Proyección mínima del ciclo para el Calendario Compartido:
                // tres números, nunca el historial. Solo viaja si comparte predicciones.
                val activeCycle = repository.getCurrentActiveCycle()
                val closedCycles = repository.getClosedCycles()
                val isPregnant = prefsManager.isPregnantFlow.first()
                val isOnContraceptive = prefsManager.isOnContraceptiveFlow.first()
                val age = prefsManager.ageFlow.first()?.toFloat()
                val height = prefsManager.heightFlow.first()
                val weight = prefsManager.weightFlow.first()

                val effectiveResult = getEffectiveCyclePredictionUseCase(
                    activeCycle = activeCycle,
                    closedCycles = closedCycles,
                    isPregnant = isPregnant,
                    isOnContraceptive = isOnContraceptive,
                    age = age,
                    height = height,
                    weight = weight,
                    targetDate = LumiClock.today()
                )

                val effectiveLength = effectiveResult.effectiveCycleLength
                val todayPrediction = effectiveResult.prediction

                val snapshot = com.jeremy.lumi.domain.model.CycleSnapshot(
                    currentPhase = if (sharePhase) todayPrediction?.currentPhase ?: CyclePhase.UNKNOWN else CyclePhase.UNKNOWN,
                    daysUntilNextPhase = if (sharePredictions) (todayPrediction?.daysUntilNextPeriod ?: 0) else 0,
                    isLate = sharePredictions && todayPrediction?.isLate == true,
                    delayDays = if (sharePredictions) (todayPrediction?.delayDays ?: 0) else 0,
                    currentMood = currentMood,
                    topSymptoms = topSymptoms,
                    cycleStartDateMs = if (sharePredictions) activeCycle?.startDate else null,
                    cycleLengthDays = if (sharePredictions) effectiveLength else 0,
                    periodLengthDays = if (sharePredictions) (activeCycle?.periodLength ?: 0) else 0
                )
                partnerRepository.publishSnapshot(snapshot)
                _uiState.update { it.copy(syncError = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(syncError = true) }
            }
        }
    }

    private fun loadCurrentCycle(isPregnant: Boolean = _uiState.value.isPregnant) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val cycle = repository.getCurrentActiveCycle()
            val closedCycles = repository.getClosedCycles()

            if (cycle != null) {
                val startDate = Instant.ofEpochMilli(cycle.startDate)
                    .atZone(ZoneId.of("UTC")).toLocalDate()

                // ── Predicción efectiva unificada (TFLite/LiteRT + Matemático) ──────
                val isOnContraceptive = prefsManager.isOnContraceptiveFlow.first()
                val age = prefsManager.ageFlow.first()?.toFloat()
                val height = prefsManager.heightFlow.first()
                val weight = prefsManager.weightFlow.first()

                val effectiveResult = withContext(Dispatchers.Default) {
                    getEffectiveCyclePredictionUseCase(
                        activeCycle = cycle,
                        closedCycles = closedCycles,
                        isPregnant = isPregnant,
                        isOnContraceptive = isOnContraceptive,
                        age = age,
                        height = height,
                        weight = weight,
                        targetDate = LumiClock.today()
                    )
                }

                val cycleLen = effectiveResult.effectiveCycleLength
                val periodLen = effectiveResult.periodLength
                val prediction = effectiveResult.prediction ?: CyclePredictor.predict(
                    startDate = startDate,
                    cycleLength = cycleLen,
                    periodLength = periodLen,
                    isPregnant = isPregnant,
                    isOnContraceptive = isOnContraceptive,
                    effectiveCycleLength = cycleLen
                )

                // Cache para recálculo dinámico al tocar días del timeline
                cachedStartDate = startDate
                cachedCycleLen = cycleLen
                cachedPeriodLen = periodLen
                cachedClosedCycles = closedCycles
                cachedIsPregnant = isPregnant
                cachedIsOnContraceptive = isOnContraceptive
                baseTodayPrediction = prediction

                val today = LumiClock.today()
                val currentSelected = _uiState.value.selectedDate

                // Timeline continuo: 14 días pasados + 75 días futuros (~3 ciclos)
                val weekDays = (-14..75).map { offset ->
                    val date = today.plusDays(offset.toLong())
                    val (phase, isPrediction) = CyclePredictor.phaseForDateInActiveCycle(
                        startDate = startDate,
                        cycleLength = cycleLen,
                        periodLength = periodLen,
                        date = date,
                        today = today,
                        isPregnant = _uiState.value.isPregnant,
                        isOnContraceptive = isOnContraceptive
                    )
                    val isToday = offset == 0
                    val isSelected = (currentSelected != null && date == currentSelected) || (currentSelected == null && isToday)
                    CycleDayUi(
                        date = date,
                        dayOfMonth = date.dayOfMonth,
                        weekdayLabel = date.dayOfWeek
                            .getDisplayName(TextStyle.SHORT, Locale("es"))
                            .replaceFirstChar { it.uppercase() },
                        phase = phase,
                        isToday = isToday,
                        isPrediction = isPrediction,
                        isSelected = isSelected
                    )
                }

                val activePrediction = if (currentSelected != null && currentSelected != today) {
                    CyclePredictor.predict(
                        startDate = startDate,
                        cycleLength = cycleLen,
                        periodLength = periodLen,
                        today = currentSelected,
                        closedCycles = closedCycles,
                        isPregnant = isPregnant,
                        isOnContraceptive = isOnContraceptive,
                        effectiveCycleLength = cycleLen,
                        predictionConfidence = effectiveResult.aiConfidence
                    )
                } else {
                    prediction
                }

                // ── Factores de predicción honestos ("Basado en:") ───────────
                val stdDev = activePrediction.predictedCycleLengthStdDev
                val regScore = if (closedCycles.isEmpty()) {
                    75
                } else {
                    ((1f - (stdDev.coerceIn(0.5f, 6.0f) / 7.5f)) * 100f).coerceIn(45f, 98f).toInt()
                }

                val histScore = when (closedCycles.size) {
                    0    -> 35
                    1    -> 55
                    2    -> 72
                    3    -> 85
                    else -> 96
                }

                val allLogs = repository.getAllLogs(descending = true).firstOrNull() ?: emptyList()
                val currentCycleLogs = allLogs.filter { it.dailyLog.date >= cycle.startDate }
                val daysWithSymptoms = currentCycleLogs.count { it.symptoms.isNotEmpty() || it.dailyLog.mood != null }
                val currentDayCoerced = activePrediction.currentDayOfCycle.coerceAtLeast(1)
                val sympScore = ((daysWithSymptoms.toFloat() / currentDayCoerced) * 100f).coerceIn(25f, 95f).toInt()

                val confLabel = when {
                    activePrediction.predictionConfidenceIndex >= 0.70f -> "Certeza alta"
                    activePrediction.predictionConfidenceIndex >= 0.45f -> "Certeza moderada"
                    else                                                 -> "En calibración"
                }

                val marginFormatted = if (stdDev <= 0.1f) "±1.5 d" 
                                      else "±${String.format(java.util.Locale.US, "%.1f", stdDev.coerceAtLeast(1.0f))} d"

                val factors = PredictionFactorsUi(
                    regularityScore = regScore,
                    historyScore = histScore,
                    symptomsScore = sympScore,
                    cycleCount = closedCycles.size,
                    confidenceLabel = confLabel,
                    marginDaysText = marginFormatted
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeCycle = cycle,
                        currentPhase = activePrediction.currentPhase,
                        currentDayOfCycle = activePrediction.currentDayOfCycle,
                        prediction = activePrediction,
                        predictionFactors = factors,
                        weekDays = weekDays,
                        delayState = activePrediction.delayState,
                        delayDays = activePrediction.delayDays,
                        isLate = activePrediction.isLate
                    )
                }
                // ── Propagar contexto al motor de chat ───────────────────────
                val todayMillis = LumiClock.today()
                    .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                val todayLog = repository.getDailyLog(todayMillis)

                val cycleContext = CycleContext(
                    phase              = prediction.currentPhase,
                    dayOfCycle         = prediction.currentDayOfCycle,
                    dayOfPhase         = prediction.dayOfPhase,
                    daysUntilPeriod    = prediction.daysUntilNextPeriod,
                    daysUntilOvulation = prediction.daysUntilOvulation,
                    cycleLength        = cycleLen,
                    isLate             = prediction.isLate,
                    delayDays          = prediction.delayDays,
                    // FIX P1-4: Leer el nivel de dolor real del log diario en lugar
                    // del valor hardcodeado (5). Esto permite al motor de chat
                    // detectar dolor alto (>= 7) y dar respuestas apropiadas.
                    todayPainLevel     = todayLog?.dailyLog?.painLevel ?: 0,
                    todayEnergyLevel   = todayLog?.dailyLog?.energyLevel,
                    todaySleepHours    = todayLog?.dailyLog?.sleepHours,
                    todayStressLevel   = todayLog?.dailyLog?.stressLevel,
                    todayMood          = todayLog?.dailyLog?.mood,
                    userGoal           = _uiState.value.userGoal ?: "TRACK_CYCLE",
                    // FIX P2-1: Pasar el modo pastilla al motor de periodos para que
                    // filtre preguntas de ventana fértil y dé respuestas apropiadas.
                    isOnContraceptive  = isOnContraceptive,
                    // FIX P2-4: Temperatura basal sincronizada desde Health Connect.
                    todayBbt           = todayLog?.dailyLog?.basalBodyTemp
                )
                rulesEngine.updateContext(cycleContext)
                
                // Actualizar Widget
                val phaseName = when (prediction.currentPhase) {
                    CyclePhase.MENSTRUAL -> "Menstruación"
                    CyclePhase.FOLLICULAR -> "Fase Folicular"
                    CyclePhase.OVULATION -> "Ovulación"
                    CyclePhase.LUTEAL -> "Fase Lútea"
                    CyclePhase.PREGNANCY -> "Embarazo"
                    CyclePhase.UNKNOWN -> "Desconocido"
                }
                val msg = if (prediction.isLate) "Retraso de ${prediction.delayDays} días" 
                          else "Próximo en ${prediction.daysUntilNextPeriod} días"
                widgetUpdater.updateWidget(prediction.currentDayOfCycle, phaseName, msg, "#9B72C0")
                // FIX P2-3: Re-sincronizar Health Connect en cada carga del ciclo
                // para mantener los datos de sueño/BBT actualizados durante la sesión.
                val hcEnabled = prefsManager.isHealthConnectEnabledFlow.first()
                if (hcEnabled) {
                    syncHealthConnectData()
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
     * La usuaria registra que le llegó la periodo en una fecha determinada (por defecto, hoy).
     */
    fun startNewPeriod(selectedDate: LocalDate = LumiClock.today()) {
        viewModelScope.launch {
            val startMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

            val currentCycle = repository.getCurrentActiveCycle()
            if (currentCycle != null) {
                val startOfLast = Instant.ofEpochMilli(currentCycle.startDate)
                    .atZone(ZoneId.of("UTC")).toLocalDate()
                
                // Guard: Si la fecha seleccionada es igual o anterior al inicio del ciclo actual, ignoramos
                if (!selectedDate.isAfter(startOfLast)) {
                    return@launch
                }

                // The repository closes this cycle and starts the next in one transaction.
            }

            val closedCycles = buildList {
                currentCycle?.let {
                    val end = selectedDate.minusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                    add(it.copy(endDate = end))
                }
                addAll(repository.getClosedCycles())
            }

            val newCycleLen = if (closedCycles.size >= 3) {
                CyclePredictor.weightedAverageCycleLength(closedCycles).toInt().coerceIn(15, 60)
            } else {
                currentCycle?.cycleLength ?: 28
            }
            val newPeriodLen = currentCycle?.periodLength ?: 5

            repository.startNewCycle(
                startDate = startMillis,
                cycleLength = newCycleLen,
                periodLength = newPeriodLen
            )
            prefsManager.resetDelayBannerDismiss()
            loadCurrentCycle()
            _eventFlow.emit(HomeEvent.CycleStarted("Ciclo iniciado"))
        }
    }

    /**
     * Revierte el inicio del último ciclo si se registró por error.
     */
    fun undoLastPeriod() {
        viewModelScope.launch {
            val restored = repository.undoLastPeriodStart()
            if (restored) {
                loadCurrentCycle()
                _eventFlow.emit(HomeEvent.CycleRestored("Ciclo previo restaurado"))
            }
        }
    }

    fun dismissDelayBanner() {
        val dayOfCycle = _uiState.value.currentDayOfCycle
        viewModelScope.launch {
            prefsManager.dismissDelayBannerForDay(dayOfCycle)
        }
        _uiState.update { it.copy(isLate = false) }
    }

    private fun syncHealthConnectData() {
        viewModelScope.launch {
            if (!healthConnectManager.isAvailable() || !healthConnectManager.hasAllPermissions()) return@launch
            
            val today = LumiClock.today()
            val startOfDay = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
            val endOfDay = today.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
            
            val bbt = healthConnectManager.readBasalTemperature(startOfDay, endOfDay)
            val sleep = healthConnectManager.readSleepHours(startOfDay, endOfDay)
            
            if (bbt != null || sleep != null) {
                // Usar UTC para la clave de fecha del log (consistente con el resto de la app)
                val epochMs = today.atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
                val existingLog = repository.getDailyLog(epochMs)
                
                if (existingLog == null) {
                    val activeCycle = repository.getCurrentActiveCycle()
                    if (activeCycle != null) {
                        repository.saveDailyLogWithSymptoms(
                            com.jeremy.lumi.data.local.entity.DailyLogEntity(
                                cycleId = activeCycle.id,
                                date = epochMs,
                                basalBodyTemp = bbt,
                                sleepHours = sleep
                            ),
                            emptyList()
                        )
                    }
                } else {
                    val updatedLog = existingLog.dailyLog.copy(
                        basalBodyTemp = bbt ?: existingLog.dailyLog.basalBodyTemp,
                        sleepHours = sleep ?: existingLog.dailyLog.sleepHours
                    )
                    repository.saveDailyLogWithSymptoms(updatedLog, existingLog.symptoms)
                }
            }
        }
    }

    fun generateRandomSymptomsForToday() {
        viewModelScope.launch {
            val todayDate = LumiClock.today()
            val todayMillis = todayDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            val activeCycle = repository.getCurrentActiveCycle() ?: return@launch

            val moods = listOf("Feliz", "Sensible", "Triste", "Irritado")
            val randomMood = moods.random()
            val randomPain = (1..10).random()
            
            val log = com.jeremy.lumi.data.local.entity.DailyLogEntity(
                cycleId = activeCycle.id,
                date = todayMillis,
                mood = randomMood,
                painLevel = randomPain,
                energyLevel = (1..5).random(),
                sleepHours = (4..9).random().toFloat()
            )
            repository.saveDailyLogWithSymptoms(log, emptyList())
        }
    }

    /**
     * Selecciona una fecha del timeline para previsualizar el estado del ciclo.
     * Si la fecha seleccionada es hoy, restablece la vista a tiempo real.
     */
    fun selectDate(date: LocalDate) {
        val today = LumiClock.today()
        val targetDate = if (date == today) null else date
        _uiState.update { state ->
            state.copy(
                selectedDate = targetDate,
                weekDays = state.weekDays.map { day ->
                    day.copy(isSelected = (targetDate != null && day.date == targetDate) || (targetDate == null && day.isToday))
                }
            )
        }
        updateDisplayForDate(targetDate ?: today)
    }

    /**
     * Restablece la vista del Home al día de hoy.
     */
    fun resetToToday() {
        selectDate(LumiClock.today())
    }

    private fun updateDisplayForDate(targetDate: LocalDate) {
        val startDate = cachedStartDate ?: return
        val today = LumiClock.today()

        val pred = if (targetDate == today) {
            baseTodayPrediction ?: CyclePredictor.predict(
                startDate = startDate,
                cycleLength = cachedCycleLen,
                periodLength = cachedPeriodLen,
                today = today,
                closedCycles = cachedClosedCycles,
                isPregnant = cachedIsPregnant,
                isOnContraceptive = cachedIsOnContraceptive,
                effectiveCycleLength = cachedCycleLen
            )
        } else {
            CyclePredictor.predict(
                startDate = startDate,
                cycleLength = cachedCycleLen,
                periodLength = cachedPeriodLen,
                today = targetDate,
                closedCycles = cachedClosedCycles,
                isPregnant = cachedIsPregnant,
                isOnContraceptive = cachedIsOnContraceptive,
                effectiveCycleLength = cachedCycleLen
            )
        }

        _uiState.update {
            it.copy(
                currentPhase = pred.currentPhase,
                currentDayOfCycle = pred.currentDayOfCycle,
                prediction = pred,
                delayState = pred.delayState,
                delayDays = pred.delayDays,
                isLate = pred.isLate
            )
        }
    }
}
