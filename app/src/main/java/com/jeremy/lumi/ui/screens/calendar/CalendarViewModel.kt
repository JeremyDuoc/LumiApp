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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val prefsManager: OnboardingPreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _selectedLog = MutableStateFlow<DailyLogWithSymptoms?>(null)
    val selectedLog: StateFlow<DailyLogWithSymptoms?> = _selectedLog.asStateFlow()

    val activeCategories: StateFlow<Set<String>> = prefsManager.activeLogCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("physical", "digestive", "mucus", "intercourse"))

    private val allLogsMap = MutableStateFlow<Map<Long, DailyLogWithSymptoms>>(emptyMap())

    private val todaySnapshot = Calendar.getInstance()
    private var displayYear  = todaySnapshot.get(Calendar.YEAR)
    private var displayMonth = todaySnapshot.get(Calendar.MONTH)

    private var isPregnant = false

    init {
        viewModelScope.launch {
            repository.getAllLogs(descending = false).collect { logs ->
                allLogsMap.value = logs.associateBy { it.dailyLog.date }
                updateGrid()
            }
        }
        viewModelScope.launch {
            prefsManager.isPregnantFlow.collect { pregnant ->
                isPregnant = pregnant
                updateGrid()
            }
        }
        viewModelScope.launch {
            repository.getAllCycles().collect {
                updateGrid()
            }
        }
    }

    fun setActiveCategories(categories: Set<String>) {
        viewModelScope.launch { prefsManager.setActiveLogCategories(categories) }
    }

    fun navigateToPreviousMonth() {
        if (displayMonth == 0) { displayMonth = 11; displayYear-- } else displayMonth--
        updateGrid()
    }

    fun navigateToNextMonth() {
        if (displayMonth == 11) { displayMonth = 0; displayYear++ } else displayMonth++
        updateGrid()
    }

    fun navigateToMonth(year: Int, month: Int) {
        displayYear = year
        displayMonth = month
        updateGrid()
    }

    fun goToToday() {
        val today = Calendar.getInstance()
        displayYear = today.get(Calendar.YEAR)
        displayMonth = today.get(Calendar.MONTH)
        updateGrid()
    }

    fun generateYearGrid(year: Int) {
        viewModelScope.launch {
            val monthsData = mutableListOf<Pair<String, List<CalendarDay>>>()
            val activeCycle = repository.getCurrentActiveCycle()
            for (month in 0..11) {
                val (title, days, _) = generateMonthData(year, month, activeCycle)
                monthsData.add(Pair(title, days))
            }
            _uiState.update { it.copy(yearMonthsData = monthsData) }
        }
    }

    suspend fun getYearData(year: Int): List<Pair<String, List<CalendarDay>>> {
        val monthsData = mutableListOf<Pair<String, List<CalendarDay>>>()
        val activeCycle = repository.getCurrentActiveCycle()
        for (month in 0..11) {
            val (title, days, _) = generateMonthData(year, month, activeCycle)
            monthsData.add(Pair(title, days))
        }
        return monthsData
    }

    private fun updateGrid() {
        viewModelScope.launch {
            val activeCycle = repository.getCurrentActiveCycle()
            val (title, days, prediction) = generateMonthData(displayYear, displayMonth, activeCycle)
            
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
                        isPregnant   = isPregnant
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

            // Always pre-generate the current year's grid so it's ready for smooth transitions
            generateYearGrid(displayYear)
        }
    }

    private suspend fun generateMonthData(
        year: Int, 
        month: Int, 
        activeCycle: com.jeremy.lumi.data.local.entity.CycleEntity?
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
        val cycleLength = activeCycle?.cycleLength ?: 28
        val periodLength = activeCycle?.periodLength ?: 5

        val prediction: CyclePrediction? = if (isCurrentMonth) {
            cycleStartLocal?.let { startLocal ->
                runCatching {
                    CyclePredictor.predict(
                        startDate    = startLocal,
                        cycleLength  = cycleLength,
                        periodLength = periodLength,
                        isPregnant   = isPregnant
                    )
                }.getOrNull()
            }
        } else null

        val daysList = mutableListOf<CalendarDay>()
        repeat(firstDow) { daysList.add(CalendarDay(0, isEmptyOffset = true)) }

        val logs = allLogsMap.value

        for (day in 1..maxDays) {
            val dayMs = LocalDate.of(year, month + 1, day).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            val isToday = isCurrentMonth && day == todayDay

            val dayLocal = LocalDate.of(year, month + 1, day)

            val isWithinActiveCycle = cycleStartLocal != null && !dayLocal.isBefore(cycleStartLocal)
            val isFuture = dayMs > todayStartMs

            val logForDay = logs[dayMs]

            val (phase, isPrediction) = when {
                !isWithinActiveCycle -> Pair(CyclePhase.UNKNOWN, false)
                isToday -> {
                    val p = runCatching {
                        CyclePredictor.phaseForDay(
                            CyclePredictor.dayInCycle(cycleStartLocal!!, cycleLength, dayLocal),
                            cycleLength, periodLength, isPregnant = isPregnant
                        )
                    }.getOrDefault(CyclePhase.UNKNOWN)
                    Pair(p, false)
                }
                isFuture -> {
                    val monthsIntoFuture = java.time.temporal.ChronoUnit.MONTHS.between(LocalDate.of(todayYear, todayMonth + 1, todayDay), dayLocal)
                    if (monthsIntoFuture > 6) {
                        Pair(CyclePhase.UNKNOWN, false)
                    } else {
                        val p = runCatching {
                            CyclePredictor.phaseForDay(
                                CyclePredictor.dayInCycleWrapped(cycleStartLocal!!, cycleLength, dayLocal),
                                cycleLength, periodLength, isPregnant = isPregnant
                            )
                        }.getOrDefault(CyclePhase.UNKNOWN)
                        Pair(p, true)
                    }
                }
                logForDay != null -> {
                    val p = runCatching {
                        CyclePredictor.phaseForDay(
                            CyclePredictor.dayInCycle(cycleStartLocal!!, cycleLength, dayLocal),
                            cycleLength, periodLength, isPregnant = isPregnant
                        )
                    }.getOrDefault(CyclePhase.UNKNOWN)
                    Pair(p, false)
                }
                else -> {
                    Pair(CyclePhase.UNKNOWN, false)
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
        intercourseNotes: String?, showOnCalendar: Boolean
    ) {
        viewModelScope.launch {
            val dateMs       = LocalDate.of(displayYear, displayMonth + 1, dayOfMonth)
                .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            val existingLog = repository.getDailyLog(dateMs)

            // FIX: Prevent data corruption. A past log must belong to its historical cycle, not the active one.
            val correctCycleId = if (existingLog != null && existingLog.dailyLog.cycleId != 0) {
                existingLog.dailyLog.cycleId
            } else {
                val allClosed = repository.getClosedCycles()
                val active = repository.getCurrentActiveCycle()
                val allCycles = allClosed + listOfNotNull(active)
                
                val matchedCycle = allCycles.find {
                    dateMs >= it.startDate && (it.endDate == null || dateMs <= it.endDate)
                }
                matchedCycle?.id ?: active?.id ?: 0
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
                    showIntercourseOnCalendar = showOnCalendar
                ),
                selectedSymptoms.map {
                    SymptomEntity(dailyLogId = 0, name = it, intensity = painLevel)
                }
            )
            _selectedLog.value = null
            // Flow emission will naturally call updateGrid()
        }
    }

    fun fetchLogForDay(dayOfMonth: Int) {
        viewModelScope.launch {
            val dateMs = LocalDate.of(displayYear, displayMonth + 1, dayOfMonth)
                .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            _selectedLog.value = repository.getDailyLog(dateMs)
        }
    }

    fun clearSelectedLog() { _selectedLog.value = null }
}