package com.jeremy.lumi.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.local.entity.SymptomEntity
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.usecase.CyclePrediction
import com.jeremy.lumi.domain.usecase.CyclePredictor
import com.jeremy.lumi.ai.LumiAIPredictor
import com.jeremy.lumi.data.local.entity.CycleEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class CalendarUiState(
    val selectedDate      : LocalDate = com.jeremy.lumi.utils.LumiClock.today(),
    val dataVersion       : Long = 0,
    val monthYearTitle   : String       = "",
    val days             : List<CalendarDay> = emptyList(),
    val prediction       : CyclePrediction? = null,
    val displayMonth     : Int          = com.jeremy.lumi.utils.LumiClock.today().monthValue,
    val displayYear      : Int          = com.jeremy.lumi.utils.LumiClock.today().year,
    val currentPhase     : CyclePhase   = CyclePhase.UNKNOWN,
    val currentDayOfCycle: Int          = 0,
    val yearMonthsData   : List<Pair<String, List<CalendarDay>>> = emptyList() // For Year View
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: LumiRepository,
    private val prefsManager: OnboardingPreferenceManager,
    private val aiPredictor: LumiAIPredictor,
    private val getEffectiveCyclePredictionUseCase: com.jeremy.lumi.domain.usecase.GetEffectiveCyclePredictionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _selectedLog = MutableStateFlow<DailyLogWithSymptoms?>(null)
    val selectedLog: StateFlow<DailyLogWithSymptoms?> = _selectedLog.asStateFlow()
    private val _selectedLogDate = MutableStateFlow<LocalDate?>(null)
    val selectedLogDate = _selectedLogDate.asStateFlow()
    private val _isLoadingLog = MutableStateFlow(false)
    val isLoadingLog = _isLoadingLog.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed = _loadFailed.asStateFlow()
    private val _saveFailed = MutableStateFlow(false)
    val saveFailed = _saveFailed.asStateFlow()
    enum class LogEvent { SAVED, SAVE_FAILED, LOAD_FAILED }
    private val events = Channel<LogEvent>(Channel.BUFFERED)
    val logEvents = events.receiveAsFlow()
    private var logLoadJob: Job? = null
    private var logLoadVersion = 0L

    val activeCategories: StateFlow<Set<String>> = prefsManager.activeLogCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("physical", "digestive", "mucus", "intercourse"))

    val customSymptoms = prefsManager.customLogSymptomsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val favoriteSymptoms = prefsManager.favoriteLogSymptomsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.jeremy.lumi.data.preferences.DefaultLogFavorites)
    fun setCustomSymptoms(names: Set<String>) { viewModelScope.launch { prefsManager.setCustomLogSymptoms(names) } }
    fun setFavoriteSymptoms(names: Set<String>) { viewModelScope.launch { prefsManager.setFavoriteLogSymptoms(names) } }

    private val allLogsMap = MutableStateFlow<Map<Long, DailyLogWithSymptoms>>(emptyMap())

    private val displayYearMonth = MutableStateFlow(
        Pair(com.jeremy.lumi.utils.LumiClock.today().year, com.jeremy.lumi.utils.LumiClock.today().monthValue - 1)
    )

    private var isPregnant = false
    private var isOnContraceptive = false

    init {
        viewModelScope.launch {
            com.jeremy.lumi.utils.LumiClock.offsetDays.collect {
                // Forzar actualización cuando pasa de día
                _uiState.update { state -> 
                    state.copy(
                        displayMonth = com.jeremy.lumi.utils.LumiClock.today().monthValue,
                        displayYear  = com.jeremy.lumi.utils.LumiClock.today().year,
                        selectedDate = com.jeremy.lumi.utils.LumiClock.today()
                    )
                }
                displayYearMonth.value = Pair(
                    com.jeremy.lumi.utils.LumiClock.today().year,
                    com.jeremy.lumi.utils.LumiClock.today().monthValue - 1
                )
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                repository.getAllLogs(descending = false),
                prefsManager.isPregnantFlow,
                prefsManager.isOnContraceptiveFlow,
                repository.getAllCycles(),
                displayYearMonth
            ) { logs, pregnant, contraceptive, cycles, yearMonth ->
                isPregnant = pregnant
                isOnContraceptive = contraceptive
                val logsMap = logs.associateBy { it.dailyLog.date }
                allLogsMap.value = logsMap
                
                // Identify active cycle
                val activeCycle = cycles.maxByOrNull { it.startDate }?.takeIf { it.endDate == null }
                val closedCycles = cycles.filter { it.endDate != null }.sortedByDescending { it.startDate }
                
                Triple(yearMonth, activeCycle, Pair(logsMap, closedCycles))
            }.collectLatest { (yearMonth, activeCycle, maps) ->
                val (logsMap, closedCycles) = maps
                updateGridSafely(yearMonth.first, yearMonth.second, activeCycle, closedCycles, logsMap)
            }
        }
    }

    fun setActiveCategories(categories: Set<String>) {
        viewModelScope.launch { prefsManager.setActiveLogCategories(categories) }
    }

    fun navigateToPreviousMonth() {
        val (year, month) = displayYearMonth.value
        val target = java.time.YearMonth.of(year, month + 1).minusMonths(1)
        navigateToMonth(target.year, target.monthValue - 1)
    }

    fun navigateToNextMonth() {
        val (year, month) = displayYearMonth.value
        val target = java.time.YearMonth.of(year, month + 1).plusMonths(1)
        navigateToMonth(target.year, target.monthValue - 1)
    }

    fun navigateToMonth(year: Int, month: Int) {
        selectDate(dateInMonth(_uiState.value.selectedDate, year, month + 1))
    }

    fun selectDate(date: LocalDate) {
        val changedMonth = displayYearMonth.value != Pair(date.year, date.monthValue - 1)
        _uiState.update { it.copy(
            selectedDate = date,
            displayYear = date.year,
            displayMonth = date.monthValue,
            days = if (changedMonth) emptyList() else it.days
        ) }
        displayYearMonth.value = Pair(date.year, date.monthValue - 1)
    }

    fun goToToday() {
        val today = com.jeremy.lumi.utils.LumiClock.today()
        selectDate(today)
    }

    fun generateYearGrid(year: Int) {
        viewModelScope.launch {
            val monthsData = mutableListOf<Pair<String, List<CalendarDay>>>()
            val cycles = repository.getAllCycles().first()
            val activeCycle = cycles.maxByOrNull { it.startDate }?.takeIf { it.endDate == null }
            val closedCycles = cycles.filter { it.endDate != null }.sortedByDescending { it.startDate }
            val logs = allLogsMap.value
            for (month in 0..11) {
                val (title, days, _) = generateMonthData(year, month, activeCycle, closedCycles, logs)
                monthsData.add(Pair(title, days))
            }
            _uiState.update { it.copy(yearMonthsData = monthsData) }
        }
    }

    suspend fun getYearData(year: Int): List<Pair<String, List<CalendarDay>>> {
        val monthsData = mutableListOf<Pair<String, List<CalendarDay>>>()
        val cycles = repository.getAllCycles().first()
        val activeCycle = cycles.maxByOrNull { it.startDate }?.takeIf { it.endDate == null }
        val closedCycles = cycles.filter { it.endDate != null }.sortedByDescending { it.startDate }
        val logs = allLogsMap.value
        for (month in 0..11) {
            val (title, days, _) = generateMonthData(year, month, activeCycle, closedCycles, logs)
            monthsData.add(Pair(title, days))
        }
        return monthsData
    }

    private suspend fun updateGridSafely(
        displayYear: Int,
        displayMonth: Int,
        activeCycle: CycleEntity?,
        closedCycles: List<CycleEntity>,
        logsMap: Map<Long, DailyLogWithSymptoms>
    ) {
        val (title, days, prediction) = generateMonthData(displayYear, displayMonth, activeCycle, closedCycles, logsMap)
            
            // FASE ACTUAL: derivada directamente de la predicción unificada efectiva
            val currentDayOfCycle = prediction?.currentDayOfCycle ?: 0
            val currentPhase = prediction?.currentPhase ?: CyclePhase.UNKNOWN

            _uiState.update { it.copy(
                monthYearTitle    = title,
                days              = days,
                prediction        = prediction,
                displayMonth      = displayMonth + 1,
                displayYear       = displayYear,
                currentPhase      = currentPhase,
                currentDayOfCycle = currentDayOfCycle,
                dataVersion       = it.dataVersion + 1
            ) }
            // generateYearGrid(displayYear) is removed from here to avoid excessive calls.
            // It will be called explicitly when navigating to the year view.
    }

    private suspend fun generateMonthData(
        year: Int, 
        month: Int, 
        activeCycle: CycleEntity?,
        closedCycles: List<CycleEntity>,
        logs: Map<Long, DailyLogWithSymptoms>
    ): Triple<String, List<CalendarDay>, CyclePrediction?> = withContext(Dispatchers.Default) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR,         year)
            set(Calendar.MONTH,        month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY,  0)
            set(Calendar.MINUTE,       0)
            set(Calendar.SECOND,       0)
            set(Calendar.MILLISECOND,  0)
        }

        val todayLocal = com.jeremy.lumi.utils.LumiClock.today()
        val todayDay   = todayLocal.dayOfMonth
        val todayMonth = todayLocal.monthValue - 1
        val todayYear  = todayLocal.year
        val isCurrentMonth = year == todayYear && month == todayMonth

        val todayStartMs = todayLocal
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

        val title = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(cal.time).replaceFirstChar { it.uppercase() }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        var firstDow = cal.get(Calendar.DAY_OF_WEEK) - 2
        if (firstDow < 0) firstDow += 7

        val cycleStartLocal: LocalDate? = activeCycle?.let {
            LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.startDate), ZoneId.of("UTC"))
        }

        val effectiveResult = getEffectiveCyclePredictionUseCase(
            activeCycle = activeCycle,
            closedCycles = closedCycles,
            isPregnant = isPregnant,
            isOnContraceptive = isOnContraceptive,
            age = prefsManager.ageFlow.first()?.toFloat(),
            height = prefsManager.heightFlow.first(),
            weight = prefsManager.weightFlow.first(),
            targetDate = LocalDate.of(todayYear, todayMonth + 1, todayDay)
        )

        val cycleLength = effectiveResult.effectiveCycleLength
        val periodLength = effectiveResult.periodLength
        // Today's context must not disappear when browsing another month.
        val prediction: CyclePrediction? = effectiveResult.prediction

        val daysList = mutableListOf<CalendarDay>()
        repeat(firstDow) { daysList.add(CalendarDay(0, isEmptyOffset = true)) }

        for (day in 1..maxDays) {
            val dayLocal = LocalDate.of(year, month + 1, day)
            val dayMs = dayLocal.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            val isToday = isCurrentMonth && day == todayDay
            val isFuture = dayMs > todayStartMs

            // Encontrar a qué ciclo pertenece este día
            val matchedCycle = closedCycles.find {
                val cycleEnd = it.endDate ?: Long.MAX_VALUE
                dayMs in it.startDate..cycleEnd
            } ?: activeCycle?.takeIf { dayMs >= it.startDate }

            val logForDay = logs[dayMs]

            val (phase, isPrediction) = when {
                matchedCycle == null -> Pair(CyclePhase.UNKNOWN, false) // No pertenece a ningún ciclo conocido
                activeCycle != null && (isFuture || matchedCycle.id == activeCycle.id) -> {
                    val monthsIntoFuture = java.time.temporal.ChronoUnit.MONTHS.between(LocalDate.of(todayYear, todayMonth + 1, todayDay), dayLocal)
                    if (monthsIntoFuture > 6) {
                        Pair(CyclePhase.UNKNOWN, false)
                    } else {
                        val activeCycleStartLocal = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(activeCycle.startDate), ZoneId.of("UTC"))
                        runCatching {
                            CyclePredictor.phaseForDateInActiveCycle(
                                startDate = activeCycleStartLocal,
                                cycleLength = cycleLength,
                                periodLength = periodLength,
                                date = dayLocal,
                                today = LocalDate.of(todayYear, todayMonth + 1, todayDay),
                                isPregnant = isPregnant,
                                isOnContraceptive = isOnContraceptive
                            )
                        }.getOrDefault(Pair(CyclePhase.UNKNOWN, false))
                    }
                }
                else -> {
                    // Pertenece a un ciclo histórico cerrado
                    val matchedStartLocal = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(matchedCycle.startDate), ZoneId.of("UTC"))
                    val matchedLen = matchedCycle.cycleLength
                    val matchedPeriodLen = matchedCycle.periodLength
                    val p = runCatching {
                        CyclePredictor.phaseForDay(
                            CyclePredictor.dayInCycle(matchedStartLocal, matchedLen, dayLocal),
                            matchedLen, matchedPeriodLen,
                            isPregnant = isPregnant,
                            isOnContraceptive = isOnContraceptive
                        )
                    }.getOrDefault(CyclePhase.UNKNOWN)
                    Pair(p, false)
                }
            }

            val showHeart = logForDay?.dailyLog?.hadIntercourse == true && logForDay.dailyLog.showIntercourseOnCalendar
            val heartProtected = if (showHeart) logForDay.dailyLog.protectionUsed else null

            daysList.add(
                CalendarDay(
                    dayOfMonth           = day,
                    phase                = phase,
                    isToday              = isToday,
                    hasLog               = logForDay != null,
                    isPrediction         = isPrediction,
                    isFuture             = isFuture,
                    showIntercourseHeart = showHeart,
                    intercourseProtected = heartProtected,
                    date                 = dayLocal,
                    record               = logForDay
                )
            )
        }
        Triple(title, daysList, prediction)
    }

    fun saveDailyLog(
        dayOfMonth: Int, flow: String?, painLevel: Int, mood: String?,
        selectedSymptoms: List<String>, cervicalMucus: String?, notes: String, hadIntercourse: Boolean,
        protectionUsed: Boolean?, contraceptionMethod: String?,
        intercourseNotes: String?, showOnCalendar: Boolean,
        sleepHours: Float?, energyLevel: Int?, stressLevel: Int?, waterIntake: Float?,
        basalBodyTemp: Float?, spotting: Boolean,
        symptomIntensities: Map<String, Int> = emptyMap()
    ) {
        if (_isSaving.value || _isLoadingLog.value) return
        // The editor owns a full date; month navigation must never retarget a save.
        val date = _selectedLogDate.value ?: return
        if (date.dayOfMonth != dayOfMonth || date.isAfter(com.jeremy.lumi.utils.LumiClock.today())) return
        val dateMs = date
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        _isSaving.value = true
        _saveFailed.value = false
        viewModelScope.launch {
            try {
              withContext(Dispatchers.IO) {
                val existingLog = repository.getDailyLog(dateMs)

                val correctCycleId = if (existingLog != null && existingLog.dailyLog.cycleId != 0) {
                    existingLog.dailyLog.cycleId
                } else {
                    val matchedCycle = repository.getCycleForDate(dateMs)
                    if (matchedCycle != null) {
                        matchedCycle.id
                    } else {
                        0
                    }
                }

            repository.saveDailyLogWithSymptoms(
                DailyLogEntity(
                    id                        = existingLog?.dailyLog?.id ?: 0,
                    cycleId                   = correctCycleId,
                    date                      = dateMs,
                    flowIntensity             = flow,
                    painLevel                 = painLevel,
                    mood                      = mood,
                    cervicalMucus             = cervicalMucus,
                    notes                     = notes.takeIf { it.isNotBlank() },
                    hadIntercourse            = hadIntercourse,
                    protectionUsed            = if (hadIntercourse) protectionUsed else null,
                    contraceptionMethod       = if (hadIntercourse) contraceptionMethod else null,
                    intercourseNotes          = if (hadIntercourse) intercourseNotes else null,
                    showIntercourseOnCalendar = showOnCalendar,
                    sleepHours                = sleepHours,
                    energyLevel               = energyLevel,
                    stressLevel               = stressLevel,
                    waterIntake               = waterIntake,
                    basalBodyTemp             = basalBodyTemp,
                    spotting                  = spotting
                ),
                selectedSymptoms.map {
                    SymptomEntity(dailyLogId = 0, name = it,
                        intensity = symptomIntensities[it] ?: existingLog?.symptoms?.find { symptom -> symptom.name == it }?.intensity ?: 0)
                }
            )
            }
            _selectedLog.value = null
            events.send(LogEvent.SAVED)
            // Flow emission will naturally call updateGrid()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _saveFailed.value = true
                events.send(LogEvent.SAVE_FAILED)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun fetchLogForDay(dayOfMonth: Int) {
        val (year, month) = displayYearMonth.value
        fetchLogForDate(LocalDate.of(year, month + 1, dayOfMonth))
    }

    fun fetchLogForDate(date: LocalDate) {
        val version = ++logLoadVersion
        logLoadJob?.cancel()
        _selectedLogDate.value = date
        val dateMs = date
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        _selectedLog.value = null
        _loadFailed.value = false
        _saveFailed.value = false
        _isLoadingLog.value = true
        logLoadJob = viewModelScope.launch {
            try {
                val log = repository.getDailyLog(dateMs)
                if (version == logLoadVersion) _selectedLog.value = log
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (version == logLoadVersion) {
                    _loadFailed.value = true
                    events.send(LogEvent.LOAD_FAILED)
                }
            } finally {
                if (version == logLoadVersion) _isLoadingLog.value = false
            }
        }
    }

    fun clearSelectedLog() {
        logLoadVersion++
        logLoadJob?.cancel()
        _isLoadingLog.value = false
        _selectedLog.value = null
        _selectedLogDate.value = null
    }
}
