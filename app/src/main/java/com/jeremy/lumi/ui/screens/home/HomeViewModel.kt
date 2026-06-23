package com.jeremy.lumi.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.usecase.CyclePredictor
import com.jeremy.lumi.domain.usecase.DelayState
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LumiRepository,
    private val prefsManager: OnboardingPreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { 
        loadCurrentCycle() 
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefsManager.isDiscreetModeFlow.collect { isDiscreet ->
                _uiState.update { it.copy(isDiscreetMode = isDiscreet) }
            }
        }
        viewModelScope.launch {
            prefsManager.isPregnantFlow.collect { pregnant ->
                _uiState.update { it.copy(isPregnant = pregnant) }
                loadCurrentCycle() // Reload cycle with new pregnant state
            }
        }
        viewModelScope.launch {
            prefsManager.userGoalFlow.collect { goal ->
                _uiState.update { it.copy(userGoal = goal.name) }
            }
        }
        // Escuchar el día de dismiss persistido para ocultar el banner si corresponde
        viewModelScope.launch {
            prefsManager.delayBannerDismissedDayFlow.collect { dismissedDay ->
                _uiState.update { state ->
                    if (dismissedDay >= 0 && dismissedDay == state.currentDayOfCycle) {
                        state.copy(isLate = false)
                    } else state
                }
            }
        }
    }

    fun toggleDiscreetMode() {
        viewModelScope.launch {
            prefsManager.setDiscreetMode(!_uiState.value.isDiscreetMode)
        }
    }

    private fun loadCurrentCycle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val cycle        = repository.getCurrentActiveCycle()
            // ── Historial real desde Room ────────────────────────────────────
            val closedCycles = repository.getClosedCycles()

            if (cycle != null) {
                val startDate = Instant.ofEpochMilli(cycle.startDate)
                    .atZone(ZoneId.systemDefault()).toLocalDate()

                // ── Longitud de ciclo: usa el promedio ponderado si hay historial ──
                // Con ≥3 ciclos cerrados el predictor cambia automáticamente al
                // algoritmo exponencial. Con menos, usa el valor guardado en la entidad.
                val cycleLen = if (closedCycles.size >= 3) {
                    CyclePredictor.weightedAverageCycleLength(closedCycles).toInt()
                        .coerceIn(15, 60)
                } else {
                    cycle.cycleLength.coerceAtLeast(15)
                }
                val periodLen = cycle.periodLength.coerceAtLeast(1)

                // ── Predicción con historial completo ────────────────────────
                val prediction = CyclePredictor.predict(
                    startDate    = startDate,
                    cycleLength  = cycleLen,
                    periodLength = periodLen,
                    closedCycles = closedCycles,
                    isPregnant   = _uiState.value.isPregnant
                )

                val today = LocalDate.now()

                val weekDays = (-7..14).map { offset ->
                    val date       = today.plusDays(offset.toLong())
                    val dayWrapped = CyclePredictor.dayInCycleWrapped(startDate, cycleLen, date)
                    CycleDayUi(
                        date         = date,
                        dayOfMonth   = date.dayOfMonth,
                        weekdayLabel = date.dayOfWeek
                            .getDisplayName(TextStyle.SHORT, Locale("es"))
                            .replaceFirstChar { it.uppercase() },
                        phase   = CyclePredictor.phaseForDay(dayWrapped, cycleLen, periodLen, isPregnant = _uiState.value.isPregnant),
                        isToday = offset == 0
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading         = false,
                        activeCycle       = cycle,
                        currentPhase      = prediction.currentPhase,
                        currentDayOfCycle = prediction.currentDayOfCycle,
                        prediction        = prediction,
                        weekDays          = weekDays,
                        delayState        = prediction.delayState,
                        delayDays         = prediction.delayDays,
                        isLate            = prediction.isLate
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        currentPhase = CyclePhase.UNKNOWN,
                        prediction   = null,
                        weekDays     = emptyList(),
                        delayState   = DelayState.ON_TIME,
                        delayDays    = 0,
                        isLate       = false
                    )
                }
            }
        }
    }

    /**
     * La usuaria registra que le llegó la regla hoy.
     * - Cierra el ciclo anterior guardando su duración REAL en Room
     * - Abre un nuevo ciclo usando el promedio ponderado como longitud esperada
     */
    fun startNewPeriod() {
        viewModelScope.launch {
            val todayMillis  = System.currentTimeMillis()
            val todayDate    = LocalDate.now()
            val closedCycles = repository.getClosedCycles()

            val currentCycle = repository.getCurrentActiveCycle()
            if (currentCycle != null) {
                val startOfLast  = Instant.ofEpochMilli(currentCycle.startDate)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                val realLength   = java.time.temporal.ChronoUnit.DAYS
                    .between(startOfLast, todayDate).toInt().coerceIn(15, 90)

                val endOfLast = todayDate.minusDays(1)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // ── endCurrentCycle ahora graba la duración real en Room ─────
                repository.endCurrentCycle(endOfLast, realLength)
            }

            // Nuevo ciclo: usa el promedio histórico si ya hay datos suficientes
            val newCycleLen = if (closedCycles.size >= 3) {
                CyclePredictor.weightedAverageCycleLength(closedCycles).toInt().coerceIn(15, 60)
            } else {
                currentCycle?.cycleLength ?: 28
            }
            val newPeriodLen = currentCycle?.periodLength ?: 5

            repository.startNewCycle(
                startDate    = todayMillis,
                cycleLength  = newCycleLen,
                periodLength = newPeriodLen
            )
            // Resetear el dismiss del banner para el nuevo ciclo
            prefsManager.resetDelayBannerDismiss()
            loadCurrentCycle()
        }
    }

    fun dismissDelayBanner() {
        val dayOfCycle = _uiState.value.currentDayOfCycle
        viewModelScope.launch {
            // Persiste el día en que se descartó para que no vuelva a aparecer
            prefsManager.dismissDelayBannerForDay(dayOfCycle)
        }
        _uiState.update { it.copy(isLate = false) }
    }
}
