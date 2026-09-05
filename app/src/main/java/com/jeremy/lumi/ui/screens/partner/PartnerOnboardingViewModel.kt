package com.jeremy.lumi.ui.screens.partner

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.R
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.domain.model.LinkType
import com.jeremy.lumi.domain.model.SharePreferences
import com.jeremy.lumi.domain.repository.PartnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingMode { NONE, CREATE, JOIN }

enum class OnboardingStep { CHOOSER, ROLE, PRIVACY, IDENTITY, CODE_RESULT, JOIN_CODE, JOIN_SUCCESS }

data class PartnerOnboardingUiState(
    val mode: OnboardingMode = OnboardingMode.NONE,
    val step: OnboardingStep = OnboardingStep.CHOOSER,

    // Camino CREAR
    val linkType: LinkType = LinkType.OBSERVER,
    val relationLabel: String = "",
    val generatedCode: String? = null,

    // Identidad (ambos caminos)
    val displayName: String = "",
    val avatarId: String? = null,

    // Privacidad granular (preseleccionada con los valores globales)
    val privacy: SharePreferences = SharePreferences(),

    // Camino UNIRSE
    val code: String = "",

    val isWorking: Boolean = false,
    @StringRes val errorRes: Int? = null
)

/**
 * Máquina de pasos del onboarding del Modo Pareja.
 *
 * CREAR:  CHOOSER → ROLE → PRIVACY → IDENTITY → CODE_RESULT
 * UNIRSE: CHOOSER → JOIN_CODE → IDENTITY → JOIN_SUCCESS
 */
@HiltViewModel
class PartnerOnboardingViewModel @Inject constructor(
    private val repository: PartnerRepository,
    private val prefsManager: OnboardingPreferenceManager
) : ViewModel() {

    companion object {
        private const val TAG = "PartnerOnboardingVM"
    }

    private val _uiState = MutableStateFlow(PartnerOnboardingUiState())
    val uiState: StateFlow<PartnerOnboardingUiState> = _uiState.asStateFlow()

    init {
        // Prellenar nombre y privacidad con los valores globales de la usuaria
        viewModelScope.launch {
            val name = prefsManager.userNameFlow.first().orEmpty()
            val privacy = SharePreferences(
                phase = prefsManager.sharePhaseFlow.first(),
                mood = prefsManager.shareMoodFlow.first(),
                symptoms = prefsManager.shareSymptomsFlow.first(),
                predictions = prefsManager.sharePredictionsFlow.first()
            )
            _uiState.update { it.copy(displayName = name, privacy = privacy) }
        }
    }

    // ── Navegación ───────────────────────────────────────────────────────────

    fun startCreate() = _uiState.update {
        it.copy(mode = OnboardingMode.CREATE, step = OnboardingStep.ROLE, errorRes = null)
    }

    fun startJoin() = _uiState.update {
        it.copy(mode = OnboardingMode.JOIN, step = OnboardingStep.JOIN_CODE, errorRes = null)
    }

    /** Entrada por deep link lumi://pair/{code}: salta directo al código. */
    fun startJoinWithCode(code: String) = _uiState.update {
        it.copy(
            mode = OnboardingMode.JOIN,
            step = OnboardingStep.JOIN_CODE,
            code = code.filter(Char::isDigit).take(6),
            errorRes = null
        )
    }

    fun next() {
        val state = _uiState.value
        val nextStep = when (state.step) {
            OnboardingStep.ROLE -> OnboardingStep.PRIVACY
            OnboardingStep.PRIVACY -> OnboardingStep.IDENTITY
            OnboardingStep.JOIN_CODE -> OnboardingStep.IDENTITY
            else -> return
        }
        _uiState.update { it.copy(step = nextStep, errorRes = null) }
    }

    /** @return true si consumió el gesto (retrocedió un paso interno). */
    fun goBack(): Boolean {
        val state = _uiState.value
        val previous = when (state.step) {
            OnboardingStep.ROLE, OnboardingStep.JOIN_CODE -> OnboardingStep.CHOOSER
            OnboardingStep.PRIVACY -> OnboardingStep.ROLE
            OnboardingStep.IDENTITY ->
                if (state.mode == OnboardingMode.CREATE) OnboardingStep.PRIVACY
                else OnboardingStep.JOIN_CODE
            else -> return false   // CHOOSER y pantallas finales: sale del flujo
        }
        _uiState.update { it.copy(step = previous, errorRes = null) }
        return true
    }

    /** Fracción de progreso del asistente para la barra superior. */
    fun progressFraction(): Float {
        val state = _uiState.value
        return when (state.mode) {
            OnboardingMode.CREATE -> when (state.step) {
                OnboardingStep.ROLE -> 0.25f
                OnboardingStep.PRIVACY -> 0.5f
                OnboardingStep.IDENTITY -> 0.75f
                OnboardingStep.CODE_RESULT -> 1f
                else -> 0f
            }
            OnboardingMode.JOIN -> when (state.step) {
                OnboardingStep.JOIN_CODE -> 0.33f
                OnboardingStep.IDENTITY -> 0.66f
                OnboardingStep.JOIN_SUCCESS -> 1f
                else -> 0f
            }
            OnboardingMode.NONE -> 0f
        }
    }

    // ── Setters de formulario ────────────────────────────────────────────────

    fun setLinkType(type: LinkType) = _uiState.update { it.copy(linkType = type) }
    fun setRelationLabel(label: String) = _uiState.update { it.copy(relationLabel = label.take(30)) }
    fun setDisplayName(name: String) = _uiState.update { it.copy(displayName = name.take(30)) }
    fun setAvatar(avatarId: String) = _uiState.update { it.copy(avatarId = avatarId) }
    fun setPrivacy(privacy: SharePreferences) = _uiState.update { it.copy(privacy = privacy) }
    fun setCode(code: String) = _uiState.update {
        it.copy(code = code.filter(Char::isDigit).take(6), errorRes = null)
    }

    // ── Envío final ──────────────────────────────────────────────────────────

    fun submit() {
        val state = _uiState.value
        if (state.isWorking) return
        when (state.mode) {
            OnboardingMode.CREATE -> createLink(state)
            OnboardingMode.JOIN -> joinLink(state)
            OnboardingMode.NONE -> Unit
        }
    }

    private fun createLink(state: PartnerOnboardingUiState) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, errorRes = null) }
            try {
                val code = repository.createPartnerLink(
                    linkType = state.linkType,
                    displayName = state.displayName.trim().ifBlank { null },
                    avatarId = state.avatarId,
                    relationLabel = state.relationLabel.trim(),
                    privacy = state.privacy
                )
                _uiState.update {
                    it.copy(isWorking = false, generatedCode = code, step = OnboardingStep.CODE_RESULT)
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo crear el vínculo: ${e.message}")
                _uiState.update { it.copy(isWorking = false, errorRes = R.string.pm_error_generic) }
            }
        }
    }

    private fun joinLink(state: PartnerOnboardingUiState) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, errorRes = null) }
            try {
                val link = repository.joinPartnerLink(
                    code = state.code,
                    displayName = state.displayName.trim().ifBlank { null },
                    avatarId = state.avatarId,
                    privacy = state.privacy
                )
                if (link == null) {
                    _uiState.update {
                        it.copy(isWorking = false, errorRes = R.string.pm_error_invalid_code)
                    }
                } else {
                    _uiState.update {
                        it.copy(isWorking = false, step = OnboardingStep.JOIN_SUCCESS)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo unir al vínculo: ${e.message}")
                _uiState.update { it.copy(isWorking = false, errorRes = R.string.pm_error_generic) }
            }
        }
    }
}
