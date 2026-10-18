package com.jeremy.lumi.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.domain.model.OnboardingData
import com.jeremy.lumi.domain.model.SecondaryGoal
import com.jeremy.lumi.domain.model.UserGoal
import com.jeremy.lumi.domain.repository.LumiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStepIndex: Int = 0,
    val data: OnboardingData = OnboardingData(),
    val isCompleting: Boolean = false,
    val saveError: String? = null
) {
    // 0: Bienvenida, 1: Seguridad, 2: Tu nombre, 3: Tu ciclo, 4: Último periodo, 5: Preferencias, 6: Personalización, 7: Listo
    val visibleSteps: List<Int>
        get() = if (data.mode == "support") listOf(0, 1, 2, 7) else listOf(0, 1, 2, 3, 4, 5, 6, 7)

    val currentStepNumber: Int
        get() = visibleSteps.getOrElse(currentStepIndex.coerceIn(0, visibleSteps.lastIndex)) { 0 }

    val totalStepsCount: Int
        get() = visibleSteps.size

    val isNextEnabled: Boolean
        get() = when (currentStepNumber) {
            0 -> true
            1 -> data.medicalDisclaimerAccepted && data.privacyAccepted
            2 -> true // Nickname es opcional
            3 -> data.regularityChoice != null
            4 -> !data.lastPeriodKnown || data.lastPeriodDate > 0L
            5 -> true // Objetivos y anticoncepción tienen valores seguros por defecto
            6 -> true
            7 -> !isCompleting
            else -> true
        }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: OnboardingPreferenceManager,
    private val repository: LumiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // ── Navegación entre pasos ────────────────────────────────────────────────

    fun nextStep() {
        _uiState.update { state ->
            val nextIdx = (state.currentStepIndex + 1).coerceAtMost(state.visibleSteps.lastIndex)
            state.copy(currentStepIndex = nextIdx)
        }
    }

    fun prevStep() {
        _uiState.update { state ->
            val prevIdx = (state.currentStepIndex - 1).coerceAtLeast(0)
            state.copy(currentStepIndex = prevIdx)
        }
    }

    fun jumpToStep(targetStep: Int) {
        _uiState.update { state ->
            val idx = state.visibleSteps.indexOf(targetStep)
            if (idx >= 0) {
                state.copy(currentStepIndex = idx)
            } else {
                state
            }
        }
    }

    // ── Actualización de campos ───────────────────────────────────────────────

    fun setMode(mode: String) {
        _uiState.update { state ->
            if (state.data.mode != mode) {
                state.copy(
                    data = state.data.copy(
                        mode = mode,
                        medicalDisclaimerAccepted = false,
                        privacyAccepted = false
                    ),
                    // Mantener el índice de paso dentro del nuevo rango si cambia
                    currentStepIndex = state.currentStepIndex.coerceAtMost(
                        if (mode == "support") 3 else 7
                    )
                )
            } else state
        }
    }

    fun setMedicalAccepted(accepted: Boolean) {
        _uiState.update { it.copy(data = it.data.copy(medicalDisclaimerAccepted = accepted)) }
    }

    fun setPrivacyAccepted(accepted: Boolean) {
        _uiState.update { it.copy(data = it.data.copy(privacyAccepted = accepted)) }
    }

    fun setUserName(name: String) {
        _uiState.update { it.copy(data = it.data.copy(userName = name.takeIf { n -> n.isNotBlank() })) }
    }

    fun setRegularity(choice: String) {
        val isReg = when (choice) {
            "yes" -> true
            "no" -> false
            else -> null
        }
        _uiState.update {
            it.copy(data = it.data.copy(regularityChoice = choice, isRegular = isReg))
        }
    }

    fun setCycleLengthKnown(known: Boolean) {
        _uiState.update { it.copy(data = it.data.copy(cycleLengthKnown = known)) }
    }

    fun setCycleLength(days: Int) {
        _uiState.update { it.copy(data = it.data.copy(cycleLength = days, cycleLengthKnown = true)) }
    }

    fun setLastPeriodKnown(isKnown: Boolean) {
        _uiState.update {
            it.copy(
                data = it.data.copy(
                    lastPeriodKnown = isKnown,
                    lastPeriodDate = if (!isKnown) 0L else it.data.lastPeriodDate
                )
            )
        }
    }

    fun setLastPeriodDate(epochMillis: Long) {
        _uiState.update { it.copy(data = it.data.copy(lastPeriodDate = epochMillis, lastPeriodKnown = true)) }
    }

    fun setUserGoal(goal: UserGoal?) {
        _uiState.update { it.copy(data = it.data.copy(userGoal = goal)) }
    }

    fun setContraceptionChoice(choice: String) {
        val onPill = (choice == "yes")
        _uiState.update {
            it.copy(data = it.data.copy(contraceptionChoice = choice, isOnContraceptive = onPill))
        }
    }

    fun setPeriodLengthKnown(known: Boolean) {
        _uiState.update { it.copy(data = it.data.copy(periodLengthKnown = known)) }
    }

    fun setPeriodLength(days: Int) {
        _uiState.update { it.copy(data = it.data.copy(periodLength = days, periodLengthKnown = true)) }
    }

    fun toggleSecondaryGoal(goal: SecondaryGoal) {
        _uiState.update {
            val current = it.data.secondaryGoals
            val updated = if (goal in current) current - goal else current + goal
            it.copy(data = it.data.copy(secondaryGoals = updated))
        }
    }

    fun setPhysicalProfile(age: Int?, height: Float?, weight: Float?) {
        _uiState.update {
            it.copy(data = it.data.copy(age = age, height = height, weight = weight))
        }
    }

    fun clearSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }

    // ── Finalización del onboarding ───────────────────────────────────────────

    fun completeOnboarding(onDone: () -> Unit) {
        if (_uiState.value.isCompleting) return
        val data = _uiState.value.data
        _uiState.update { it.copy(isCompleting = true, saveError = null) }

        viewModelScope.launch {
            try {
                if (data.mode == "support") {
                    prefs.saveOnboardingProfile(
                        userName = data.userName,
                        isRegular = null,
                        cycleLength = 28,
                        periodLength = 5,
                        goal = UserGoal.TRACK_CYCLE,
                        isObserver = true
                    )
                    prefs.markOnboardingCompleted(isObserver = true)
                } else {
                    prefs.saveOnboardingProfile(
                        userName = data.userName,
                        isRegular = data.isRegular,
                        cycleLength = if (data.cycleLengthKnown) data.cycleLength else 28,
                        periodLength = if (data.periodLengthKnown) data.periodLength else 5,
                        goal = data.userGoal ?: UserGoal.TRACK_CYCLE,
                        age = data.age,
                        height = data.height,
                        weight = data.weight,
                        secondaryGoals = data.secondaryGoals,
                        medicalDisclaimerAccepted = data.medicalDisclaimerAccepted
                    )
                    prefs.setIsOnContraceptive(data.isOnContraceptive)
                    prefs.markOnboardingCompleted(isObserver = false)

                    // Crear el primer ciclo en Room, SÓLO si la usuaria dio una fecha de inicio real
                    if (data.lastPeriodKnown && data.lastPeriodDate > 0L) {
                        repository.startNewCycle(
                            startDate = data.lastPeriodDate,
                            cycleLength = if (data.cycleLengthKnown) data.cycleLength else 28,
                            periodLength = if (data.periodLengthKnown) data.periodLength else 5
                        )
                    }
                }

                _uiState.update { it.copy(isCompleting = false, saveError = null) }
                onDone()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCompleting = false,
                        saveError = e.localizedMessage ?: "No pudimos guardar los datos. Puedes intentarlo de nuevo."
                    )
                }
            }
        }
    }
}
