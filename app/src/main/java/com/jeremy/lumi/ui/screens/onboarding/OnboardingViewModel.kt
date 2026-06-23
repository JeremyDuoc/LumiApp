package com.jeremy.lumi.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.domain.model.OnboardingData
import com.jeremy.lumi.domain.model.UserGoal
import com.jeremy.lumi.domain.repository.LumiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage  : Int           = 0,    // 0-5 (5 = pantalla de celebración)
    val data         : OnboardingData = OnboardingData(),
    val isCompleting : Boolean        = false // true mientras se guardan los datos
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs      : OnboardingPreferenceManager,
    private val repository : LumiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // ── Navegación entre páginas ──────────────────────────────────────────────

    fun nextPage() {
        _uiState.update { it.copy(currentPage = (it.currentPage + 1).coerceAtMost(5)) }
    }

    fun prevPage() {
        _uiState.update { it.copy(currentPage = (it.currentPage - 1).coerceAtLeast(0)) }
    }

    // ── Actualización de campos ───────────────────────────────────────────────

    fun setUserName(name: String) {
        _uiState.update { it.copy(data = it.data.copy(userName = name.takeIf { n -> n.isNotBlank() })) }
    }

    fun setLastPeriodDate(epochMillis: Long) {
        _uiState.update { it.copy(data = it.data.copy(lastPeriodDate = epochMillis)) }
    }

    fun setCycleLength(days: Int) {
        _uiState.update { it.copy(data = it.data.copy(cycleLength = days)) }
    }

    fun setPeriodLength(days: Int) {
        _uiState.update { it.copy(data = it.data.copy(periodLength = days)) }
    }

    fun setUserGoal(goal: UserGoal) {
        _uiState.update { it.copy(data = it.data.copy(userGoal = goal)) }
    }

    // ── Completar onboarding ──────────────────────────────────────────────────

    /**
     * Persiste todos los datos y crea el primer ciclo en Room.
     * Llama a [onDone] cuando todo esté guardado para que el grafo de navegación
     * pueda navegar a MAIN de forma segura.
     */
    fun completeOnboarding(onDone: () -> Unit) {
        val data = _uiState.value.data
        _uiState.update { it.copy(isCompleting = true) }

        viewModelScope.launch {
            // 1. Guardar perfil en DataStore
            prefs.saveOnboardingProfile(
                userName     = data.userName,
                cycleLength  = data.cycleLength,
                periodLength = data.periodLength,
                goal         = data.userGoal
            )

            // 2. Crear el primer ciclo en Room
            // Usamos la fecha de última regla como startDate.
            // Si la usuaria dijo "no recuerdo" (0L), usamos hoy.
            val startMillis = if (data.lastPeriodDate > 0L) data.lastPeriodDate
            else LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()

            repository.startNewCycle(
                startDate    = startMillis,
                cycleLength  = data.cycleLength,
                periodLength = data.periodLength
            )

            _uiState.update { it.copy(isCompleting = false) }
            onDone()
        }
    }
}
