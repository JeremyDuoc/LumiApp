package com.jeremy.lumi.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.usecase.CyclePredictor
import com.jeremy.lumi.domain.usecase.CyclePrediction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

data class CalendarUiState(
    val monthYearTitle   : String       = "",
    val days             : List<CalendarDay> = emptyList(),
    val prediction       : CyclePrediction? = null,
    val displayMonth     : Int          = java.time.LocalDate.now().monthValue,
    val displayYear      : Int          = java.time.LocalDate.now().year,
    /** Fase del ciclo HOY — no cambia al navegar meses. */
    val currentPhase     : CyclePhase   = CyclePhase.UNKNOWN,
    /** Día del ciclo hoy (1-based). 0 = sin datos. */
    val currentDayOfCycle: Int          = 0
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

    fun setActiveCategories(categories: Set<String>) {
        viewModelScope.launch { prefsManager.setActiveLogCategories(categories) }
    }

    private val todaySnapshot = Calendar.getInstance()
    private var displayYear  = todaySnapshot.get(Calendar.YEAR)
    private var displayMonth = todaySnapshot.get(Calendar.MONTH)

    private var isPregnant = false

    init { 
        viewModelScope.launch {
            prefsManager.isPregnantFlow.collect { pregnant ->
                isPregnant = pregnant
                generateMonthGrid()
            }
        }
    }

    fun navigateToPreviousMonth() {
        if (displayMonth == 0) { displayMonth = 11; displayYear-- } else displayMonth--
        generateMonthGrid()
    }

    fun navigateToNextMonth() {
        if (displayMonth == 11) { displayMonth = 0; displayYear++ } else displayMonth++
        generateMonthGrid()
    }

    private fun generateMonthGrid() {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR,         displayYear)
                set(Calendar.MONTH,        displayMonth)
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
            val isCurrentMonth = displayYear == todayYear && displayMonth == todayMonth

            // Inicio del día de hoy en millis — para distinguir pasado vs futuro
            val todayStartMs = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val title   = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(cal.time).replaceFirstChar { it.uppercase() }
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            var firstDow = cal.get(Calendar.DAY_OF_WEEK) - 2
            if (firstDow < 0) firstDow += 7

            val activeCycle = repository.getCurrentActiveCycle()

            // Rango del ciclo activo en LocalDate — la clave para no colorear ciclos anteriores
            val cycleStartLocal: LocalDate? = activeCycle?.let {
                LocalDate.ofEpochDay(it.startDate / 86_400_000L)
            }
            val cycleLength = activeCycle?.cycleLength ?: 28
            val periodLength = activeCycle?.periodLength ?: 5
            val cycleEndLocal: LocalDate? = cycleStartLocal?.plusDays(cycleLength.toLong() - 1)

            // Predicción solo para el mes actual
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

            for (day in 1..maxDays) {
                cal.set(Calendar.DAY_OF_MONTH, day)
                val dayMs = cal.timeInMillis
                val isToday = isCurrentMonth && day == todayDay

                // LocalDate del día iterado — para comparar con el rango del ciclo
                val dayLocal = LocalDate.ofInstant(
                    java.time.Instant.ofEpochMilli(dayMs), ZoneId.systemDefault()
                )

                // ¿Está este día DENTRO del ciclo activo?
                // Solo dentro del rango [startDate, startDate + cycleLength - 1] se colorea.
                // Ciclos anteriores quedan sin color aunque tengan log: la usuaria debería
                // haber registrado esos ciclos por separado. Días futuros más allá del ciclo
                // tampoco se colorean (se esperará el próximo inicio de periodo).
                val isWithinActiveCycle = cycleStartLocal != null &&
                        !dayLocal.isBefore(cycleStartLocal) &&
                        !dayLocal.isAfter(cycleEndLocal!!)

                // ¿Es un día futuro (después de hoy)?
                val isFuture = dayMs > todayStartMs

                val logForDay = repository.getDailyLog(dayMs)

                // ── LÓGICA DE COLOR ────────────────────────────────────────────────────────
                // 1. Fuera del ciclo activo → UNKNOWN siempre (sin color)
                // 2. Dentro del ciclo activo:
                //    a. Hoy → siempre colorear (día activo)
                //    b. Pasado con log → colorear (dato real registrado)
                //    c. Pasado sin log → UNKNOWN (no sabemos qué pasó)
                //    d. Futuro → colorear como predicción (isPrediction = true)
                // ──────────────────────────────────────────────────────────────────────────
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
                        // Día futuro dentro del ciclo → predicción
                        val p = runCatching {
                            CyclePredictor.phaseForDay(
                                CyclePredictor.dayInCycle(cycleStartLocal!!, cycleLength, dayLocal),
                                cycleLength, periodLength, isPregnant = isPregnant
                            )
                        }.getOrDefault(CyclePhase.UNKNOWN)
                        Pair(p, true)  // isPrediction = true
                    }

                    logForDay != null -> {
                        // Pasado con log → dato real (color sólido)
                        val p = runCatching {
                            CyclePredictor.phaseForDay(
                                CyclePredictor.dayInCycle(cycleStartLocal!!, cycleLength, dayLocal),
                                cycleLength, periodLength, isPregnant = isPregnant
                            )
                        }.getOrDefault(CyclePhase.UNKNOWN)
                        Pair(p, false)
                    }

                    else -> {
                        // Pasado SIN log pero dentro del ciclo → predicción pasada (color tenue)
                        // Antes era UNKNOWN, ahora mostramos la fase esperada con menor opacidad
                        val p = runCatching {
                            CyclePredictor.phaseForDay(
                                CyclePredictor.dayInCycle(cycleStartLocal!!, cycleLength, dayLocal),
                                cycleLength, periodLength, isPregnant = isPregnant
                            )
                        }.getOrDefault(CyclePhase.UNKNOWN)
                        Pair(p, true)  // isPrediction = true → la UI lo dibuja con menor opacidad
                    }
                }

                val showHeart      = logForDay?.dailyLog?.hadIntercourse == true &&
                        logForDay.dailyLog.showIntercourseOnCalendar
                val heartProtected = if (showHeart) logForDay.dailyLog?.protectionUsed else null

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

            // ── FASE ACTUAL (siempre desde hoy, no desde el mes mostrado) ──────────
            val todayLocal       = LocalDate.now()
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
                days              = daysList,
                prediction        = prediction,
                displayMonth      = displayMonth + 1,
                displayYear       = displayYear,
                currentPhase      = currentPhase,
                currentDayOfCycle = currentDayOfCycle
            ) }
        }
    }

    fun saveDailyLog(
        dayOfMonth: Int, flow: String?, painLevel: Int, mood: String?,
        selectedSymptoms: List<String>, cervicalMucus: String?, notes: String, hadIntercourse: Boolean,
        protectionUsed: Boolean?, contraceptionMethod: String?,
        intercourseNotes: String?, showOnCalendar: Boolean
    ) {
        viewModelScope.launch {
            val activeCycle  = repository.getCurrentActiveCycle()
            val cycleId      = activeCycle?.id ?: 0
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR,         displayYear)
                set(Calendar.MONTH,        displayMonth)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY,  0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND,       0); set(Calendar.MILLISECOND, 0)
            }
            val dateMs      = cal.timeInMillis
            val existingLog = repository.getDailyLog(dateMs)

            repository.saveDailyLogWithSymptoms(
                com.jeremy.lumi.data.local.entity.DailyLogEntity(
                    id                        = existingLog?.dailyLog?.id ?: 0,
                    cycleId                   = cycleId,
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
                    com.jeremy.lumi.data.local.entity.SymptomEntity(
                        dailyLogId = 0, name = it, intensity = painLevel
                    )
                }
            )
            _selectedLog.value = null
            generateMonthGrid()
        }
    }

    fun fetchLogForDay(dayOfMonth: Int) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR,         displayYear)
                set(Calendar.MONTH,        displayMonth)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY,  0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND,       0); set(Calendar.MILLISECOND, 0)
            }
            _selectedLog.value = repository.getDailyLog(cal.timeInMillis)
        }
    }

    fun clearSelectedLog() { _selectedLog.value = null }
}