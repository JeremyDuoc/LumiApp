package com.jeremy.lumi.ui.screens.partner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.remote.PartnerRepository
import com.jeremy.lumi.domain.model.PartnerLink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import javax.inject.Inject

data class PartnerUiState(
    val isLoading: Boolean = true,
    val activeLinks: List<PartnerLink> = emptyList(),
    val pendingCode: String? = null,
    val error: String? = null,
    val currentUid: String? = null,
    val currentUserName: String? = null,
    val isObserverOnly: Boolean = false,

    // Privacy settings
    val sharePhase: Boolean = true,
    val sharePredictions: Boolean = true,
    val shareMood: Boolean = true,
    val shareSymptoms: Boolean = true,

    // Cooldown para acciones de cuidado (compartido para todas las acciones)
    val careActionCooldownUntil: Long = 0L
)

/** Returns true if a care action should be disabled */
fun PartnerUiState.isCareActionOnCooldown(): Boolean = careActionCooldownUntil > System.currentTimeMillis()

/** Returns remaining cooldown seconds, or 0 */
fun PartnerUiState.careActionCooldownSeconds(): Int =
    ((careActionCooldownUntil - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()

@HiltViewModel
class PartnerViewModel @Inject constructor(
    private val repository: PartnerRepository,
    private val prefsManager: OnboardingPreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartnerUiState())
    val uiState: StateFlow<PartnerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.signInAnonymouslyIfNeeded()
            val uid = repository.getCurrentUid()
            _uiState.update { it.copy(currentUid = uid) }

            repository.observeMyLinks().collect { links ->
                _uiState.update { it.copy(isLoading = false, activeLinks = links) }
            }
        }
        viewModelScope.launch {
            prefsManager.isObserverOnly.collect { v ->
                _uiState.update { it.copy(isObserverOnly = v) }
            }
        }
        viewModelScope.launch {
            prefsManager.userNameFlow.collect { name ->
                _uiState.update { it.copy(currentUserName = name) }
            }
        }
        viewModelScope.launch {
            prefsManager.sharePhaseFlow.collect { v ->
                _uiState.update { it.copy(sharePhase = v) }
            }
        }
        viewModelScope.launch {
            prefsManager.sharePredictionsFlow.collect { v ->
                _uiState.update { it.copy(sharePredictions = v) }
            }
        }
        viewModelScope.launch {
            prefsManager.shareMoodFlow.collect { v ->
                _uiState.update { it.copy(shareMood = v) }
            }
        }
        viewModelScope.launch {
            prefsManager.shareSymptomsFlow.collect { v ->
                _uiState.update { it.copy(shareSymptoms = v) }
            }
        }
        viewModelScope.launch {
            prefsManager.hugCooldownUntilFlow.collect { v ->
                _uiState.update { it.copy(careActionCooldownUntil = v) }
            }
        }
    }

    fun setSharePhase(share: Boolean) = viewModelScope.launch { prefsManager.setSharePhase(share) }
    fun setSharePredictions(share: Boolean) = viewModelScope.launch { prefsManager.setSharePredictions(share) }
    fun setShareMood(share: Boolean) = viewModelScope.launch { prefsManager.setShareMood(share) }
    fun setShareSymptoms(share: Boolean) = viewModelScope.launch { prefsManager.setShareSymptoms(share) }

    /**
     * Crea un vínculo y guarda las preferencias de privacidad en Firestore.
     * BUG FIX: La privacidad del Wizard ahora se persiste correctamente en Firestore.
     */
    fun createLink() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val state = _uiState.value
                val code = repository.createPartnerLink(
                    sharePhase = state.sharePhase,
                    shareMood = state.shareMood,
                    shareSymptoms = state.shareSymptoms,
                    sharePredictions = state.sharePredictions,
                    ownerDisplayName = state.currentUserName
                )
                _uiState.update { it.copy(isLoading = false, pendingCode = code) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error al crear el vínculo: ${e.message}") }
            }
        }
    }

    fun joinLink(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val link = repository.joinPartnerLink(code)
                if (link != null) {
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Código inválido o ya usado") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }

    /**
     * Envía un CareAction con cooldown de 5 minutos (compartido).
     */
    fun sendCareAction(linkId: String, action: com.jeremy.lumi.domain.model.CareAction) {
        val state = _uiState.value
        if (state.isCareActionOnCooldown()) return // Silently ignore if on cooldown

        viewModelScope.launch {
            repository.sendCareAction(linkId, action)
            // 5 minutos de cooldown
            val cooldownUntil = System.currentTimeMillis() + (5 * 60 * 1000L)
            prefsManager.setHugCooldownUntil(cooldownUntil) // Se reusa el mismo preference
        }
    }

    fun unlink(linkId: String) = viewModelScope.launch { repository.unlink(linkId) }

    fun clearError() = _uiState.update { it.copy(error = null) }

    /**
     * Limpiar pendingCode y error al cerrar el Wizard.
     * BUG FIX: Evita mostrar código viejo si el usuario reabre el Wizard.
     */
    fun clearWizardState() = _uiState.update { it.copy(pendingCode = null, error = null) }
}
