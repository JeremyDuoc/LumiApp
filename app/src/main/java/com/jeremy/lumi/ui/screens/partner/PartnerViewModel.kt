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
    val careActionCooldownUntil: Long = 0L,

    // Shared Diary
    val diaryEntries: List<DiaryEntry> = emptyList(),
    val isDiarySending: Boolean = false
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
            try {
                repository.signInAnonymouslyIfNeeded()
                val uid = repository.getCurrentUid()
                _uiState.update { it.copy(currentUid = uid) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Login error: ${e.message}") }
            }
        }
        viewModelScope.launch {
            repository.observeMyLinks().collect { links ->
                _uiState.update { it.copy(isLoading = false, activeLinks = links) }
            }
        }

        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                prefsManager.isObserverOnly,
                prefsManager.userNameFlow,
                prefsManager.sharePhaseFlow,
                prefsManager.sharePredictionsFlow
            ) { obs, name, phase, pred ->
                _uiState.update { it.copy(
                    isObserverOnly = obs, currentUserName = name,
                    sharePhase = phase, sharePredictions = pred
                ) }
            }.collect { }
        }

        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                prefsManager.shareMoodFlow,
                prefsManager.shareSymptomsFlow,
                prefsManager.hugCooldownUntilFlow
            ) { mood, symp, cooldown ->
                _uiState.update { it.copy(
                    shareMood = mood, shareSymptoms = symp, careActionCooldownUntil = cooldown
                ) }
            }.collect { }
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
            try {
                repository.sendCareAction(linkId, action)
                // 5 minutos de cooldown
                val cooldownUntil = System.currentTimeMillis() + (5 * 60 * 1000L)
                prefsManager.setHugCooldownUntil(cooldownUntil) // Se reusa el mismo preference
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }

    fun unlink(linkId: String) = viewModelScope.launch { repository.unlink(linkId) }

    fun clearError() = _uiState.update { it.copy(error = null) }

    /**
     * Limpiar pendingCode y error al cerrar el Wizard.
     * BUG FIX: Evita mostrar código viejo si el usuario reabre el Wizard.
     */
    fun clearWizardState() = _uiState.update { it.copy(pendingCode = null, error = null) }

    private var diaryJob: kotlinx.coroutines.Job? = null

    fun observeDiary(linkId: String) {
        diaryJob?.cancel()
        diaryJob = viewModelScope.launch {
            repository.observeDiaryEntries(linkId).collect { entries ->
                _uiState.update { it.copy(diaryEntries = entries) }
            }
        }
    }

    fun sendDiaryEntry(linkId: String, text: String, myPhase: com.jeremy.lumi.domain.model.CyclePhase) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isDiarySending = true) }
            repository.sendDiaryEntry(
                linkId = linkId,
                text = text,
                phase = myPhase,
                authorName = state.currentUserName ?: "Yo"
            )
            _uiState.update { it.copy(isDiarySending = false) }
        }
    }
}
