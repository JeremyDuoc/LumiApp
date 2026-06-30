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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class CalendarUiState(
    val monthYearTitle   : String       = "",
    val days             : List<CalendarDay> = emptyList(),
    val prediction       : CyclePrediction? = null,
    val displayMonth     : Int          = java.time.LocalDate.now().monthValue,
    val displayYear      : Int          = java.time.LocalDate.now().year,
    val currentPhase     : CyclePhase   = CyclePhase.UNKNOWN,
    val currentDayOfCycle: Int          = 0,
    val yearMonthsData   : List<Pair<String, List<CalendarDay>>> = emptyList() // For Year View
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: LumiRepository,
    private val prefsManager: OnboardingPreferenceManager,
    private val aiPredictor: LumiAIPredictor
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _selectedLog = MutableStateFlow<DailyLogWithSymptoms?>(null)
    val selectedLog: StateFlow<DailyLogWithSymptoms?> = _selectedLog.asStateFlow()

    val activeCategories: StateFlow<Set<String>> = prefsManager.activeLogCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("physical", "digestive", "mucus", "intercourse"))

    private val allLogsMap = MutableStateFlow<Map<Long, DailyLogWithSymptoms>>(emptyMap())

    private val displayYearMonth = MutableStateFlow(
        Pair(LocalDate.now().year, LocalDate.now().monthValue - 1)
    )

    private var isPregnant = false
    private var isOnContraceptive = false

    init {
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
            }.collect { (yearMonth, activeCycle, maps) ->
                val (logsMap, closedCycles) = maps
                updateGridSafely(yearMonth.first, yearMonth.second, activeCycle, closedCycles, logsMap)
            }
        }
    }

    fun setActiveCategories(categories: Set<String>) {
        viewModelScope.launch { prefsManager.setActiveLogCategories(categories) }
    }

    fun navigateToPreviousMonth() {
        displayYearMonth.update { (year, month) ->
            if (month == 0) Pair(year - 1, 11) else Pair(year, month - 1)
        }
    }

    fun navigateToNextMonth() {
        displayYearMonth.update { (year, month) ->
            if (month == 11) Pair(year + 1, 0) else Pair(year, month + 1)
        }
    }

    fun navigateToMonth(year: Int, month: Int) {
        displayYearMonth.value = Pair(year, month)
    }

    fun goToToday() {
        val today = Calendar.getInstance()
        displayYearMonth.value = Pair(today.get(Calendar.YEAR), today.get(Calendar.MONTH))
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
            
            // FASE ACTUAL
            val cycleLength = activeCycle?.cycleLength ?: 28
            val periodLength = activeCycle?.periodLength ?: 5
            val cycleStartLocal: LocalDate? = activeCycle?.let {
                LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.startDate), ZoneId.of("UTC"))
            }

            val todayLocal = LocalDate.now()
            val currentDayOfCycle = cycleStartLocal?.let {
                CyclePredictor.dayInCycle(it, cycleLength, todayLocal)
            } ?: 0
            val currentPhase = if (cycleStartLocal != null && currentDayOfCycle > 0) {
                runCatching {
                    CyclePredictor.phaseForDay(
                        dayInCycle   = currentDayOfCycle,
                        cycleLength  = cycleLength,
                        periodLength = periodLength,
                        isPregnant   = isPregnant,
                        isOnContraceptive = isOnContraceptive
                    )
                }.getOrDefault(CyclePhase.UNKNOWN)
            } else CyclePhase.UNKNOWN

            _uiState.update { it.copy(
                monthYearTitle    = title,
                days              = days,
                prediction        = prediction,
                displayMonth      = displayMonth + 1,
                displayYear       = displayYear,
                currentPhase      = currentPhase,
                currentDayOfCycle = currentDayOfCycle
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

        val nowCal     = Calendar.getInstance()
        val todayDay   = nowCal.get(Calendar.DAY_OF_MONTH)
        val todayMonth = nowCal.get(Calendar.MONTH)
        val todayYear  = nowCal.get(Calendar.YEAR)
        val isCurrentMonth = year == todayYear && month == todayMonth

        val todayStartMs = LocalDate.of(todayYear, todayMonth + 1, todayDay)
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

        val title = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(cal.time).replaceFirstChar { it.uppercase() }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        var firstDow = cal.get(Calendar.DAY_OF_WEEK) - 2
        if (firstDow < 0) firstDow += 7

        val cycleStartLocal: LocalDate? = activeCycle?.let {
            LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.startDate), ZoneId.of("UTC"))
        }
        val periodLength = activeCycle?.periodLength ?: 5

        val mathCycleLen = if (closedCycles.size >= 3) {
            CyclePredictor.weightedAverageCycleLength(closedCycles).toInt().coerceIn(15, 60)
        } else {
            activeCycle?.cycleLength?.coerceAtLeast(15) ?: 28
        }

        val aiResult = if (closedCycles.size >= 3 && !isOnContraceptive) {
            val age = prefsManager.ageFlow.first()?.toFloat()
            val height = prefsManager.heightFlow.first()
            val weight = prefsManager.weightFlow.first()
            aiPredictor.predict(
                closedCycles = closedCycles,
                currentCycleLen = mathCycleLen.toFloat(),
                periodLength = periodLength.toFloat(),
                age = age,
                height = height,
                weight = weight
            )
        } else null

        val cycleLength = aiResult?.predictedCycleLength?.toInt()?.coerceIn(15, 60) ?: mathCycleLen

        val prediction: CyclePrediction? = if (isCurrentMonth) {
            cycleStartLocal?.let { startLocal ->
                runCatching {
                    CyclePredictor.predict(
                        startDate    = startLocal,
                        cycleLength  = cycleLength,
                        periodLength = periodLength,
                        isPregnant   = isPregnant,
                        isOnContraceptive = isOnContraceptive
                    )
                }.getOrNull()
            }
        } else null

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
                isFuture -> {
                    // Predicción hacia el futuro (usamos el activeCycle)
                    val monthsIntoFuture = java.time.temporal.ChronoUnit.MONTHS.between(LocalDate.of(todayYear, todayMonth + 1, todayDay), dayLocal)
                    if (monthsIntoFuture > 6 || activeCycle == null) {
                        Pair(CyclePhase.UNKNOWN, false)
                    } else {
                        val activeCycleStartLocal = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(activeCycle.startDate), ZoneId.of("UTC"))
                        val p = runCatching {
                            CyclePredictor.phaseForDay(
                                CyclePredictor.dayInCycleWrapped(activeCycleStartLocal, cycleLength, dayLocal),
                                cycleLength, periodLength,
                                isPregnant = isPregnant,
                                isOnContraceptive = isOnContraceptive
                            )
                        }.getOrDefault(CyclePhase.UNKNOWN)
                        Pair(p, true)
                    }
                }
                else -> {
                    // Pertenece a un ciclo histórico o al presente
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
                    intercourseProtected = heartProtected
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
        sleepHours: Float?, energyLevel: Int?, stressLevel: Int?,
        basalBodyTemp: Float?, spotting: Boolean
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val year = displayYearMonth.value.first
                val month = displayYearMonth.value.second
                val dateMs = LocalDate.of(year, month + 1, dayOfMonth)
                    .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                val existingLog = repository.getDailyLog(dateMs)

                val correctCycleId = if (existingLog != null && existingLog.dailyLog.cycleId != 0) {
                    existingLog.dailyLog.cycleId
                } else {
                    val matchedCycle = repository.getCycleForDate(dateMs)
                    if (matchedCycle != null) {
                        matchedCycle.id
                    } else {
                        repository.getCurrentActiveCycle()?.id ?: 0
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
                    protectionUsed            = protectionUsed,
                    contraceptionMethod       = contraceptionMethod,
                    intercourseNotes          = intercourseNotes,
                    showIntercourseOnCalendar = showOnCalendar,
                    sleepHours                = sleepHours,
                    energyLevel               = energyLevel,
                    stressLevel               = stressLevel,
                    basalBodyTemp             = basalBodyTemp,
                    spotting                  = spotting
                ),
                selectedSymptoms.map {
                    SymptomEntity(dailyLogId = 0, name = it, intensity = painLevel)
                }
            )
            }
            _selectedLog.value = null
            // Flow emission will naturally call updateGrid()
        }
    }

    fun fetchLogForDay(dayOfMonth: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val year = displayYearMonth.value.first
                val month = displayYearMonth.value.second
                val dateMs = LocalDate.of(year, month + 1, dayOfMonth)
                    .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                val log = repository.getDailyLog(dateMs)
                _selectedLog.value = log
            }
        }
    }

    fun clearSelectedLog() { _selectedLog.value = null }
}