package com.jeremy.lumi.ui.screens.partner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.DiaryEntry
import com.jeremy.lumi.domain.model.LinkStatus
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.model.SharePreferences
import com.jeremy.lumi.domain.repository.PartnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado observable del Modo Pareja (Hub, historias, diario).
 * Lo consumen también HomeScreen y MainScreen — mantener compatibilidad.
 */
data class PartnerUiState(
    val isLoading: Boolean = true,
    val activeLinks: List<PartnerLink> = emptyList(),
    val pendingCode: String? = null,
    val error: String? = null,
    val currentUid: String? = null,
    val currentUserName: String? = null,
    val isObserverOnly: Boolean = false,

    // Privacidad por defecto (se usa como preselección en el onboarding;
    // la privacidad REAL de cada vínculo vive en su documento de Firestore)
    val sharePhase: Boolean = true,
    val sharePredictions: Boolean = true,
    val shareMood: Boolean = true,
    val shareSymptoms: Boolean = true,

    // Cooldown compartido para todos los Detalles
    val careActionCooldownUntil: Long = 0L,

    // Diario Compartido
    val diaryEntries: List<DiaryEntry> = emptyList(),
    val isDiarySending: Boolean = false
)

/** ¿El envío de Detalles está en enfriamiento? */
fun PartnerUiState.isCareActionOnCooldown(): Boolean =
    careActionCooldownUntil > System.currentTimeMillis()

/** Segundos restantes de enfriamiento (0 si ya se puede enviar). */
fun PartnerUiState.careActionCooldownSeconds(): Int =
    ((careActionCooldownUntil - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()

private const val CARE_ACTION_COOLDOWN_MS = 5 * 60 * 1000L

@HiltViewModel
class PartnerViewModel @Inject constructor(
    private val repository: PartnerRepository,
    private val prefsManager: OnboardingPreferenceManager
) : ViewModel() {

    companion object {
        private const val TAG = "PartnerViewModel"
    }

    private val _uiState = MutableStateFlow(PartnerUiState())
    val uiState: StateFlow<PartnerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = repository.getCurrentUid()
            _uiState.update { it.copy(currentUid = uid) }
        }

        viewModelScope.launch {
            repository.observeMyLinks().collect { links ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeLinks = links.filter { l -> l.status == LinkStatus.ACTIVE },
                        pendingCode = links.firstOrNull { l -> l.status == LinkStatus.PENDING }?.linkCode
                    )
                }
            }
        }

        viewModelScope.launch {
            combine(
                prefsManager.isObserverOnly,
                prefsManager.userNameFlow,
                prefsManager.sharePhaseFlow,
                prefsManager.sharePredictionsFlow
            ) { observer, name, phase, predictions ->
                _uiState.update {
                    it.copy(
                        isObserverOnly = observer,
                        currentUserName = name,
                        sharePhase = phase,
                        sharePredictions = predictions
                    )
                }
            }.collect { }
        }

        viewModelScope.launch {
            combine(
                prefsManager.shareMoodFlow,
                prefsManager.shareSymptomsFlow,
                prefsManager.hugCooldownUntilFlow
            ) { mood, symptoms, cooldown ->
                _uiState.update {
                    it.copy(
                        shareMood = mood,
                        shareSymptoms = symptoms,
                        careActionCooldownUntil = cooldown
                    )
                }
            }.collect { }
        }
    }

    // ── Privacidad ───────────────────────────────────────────────────────────

    fun setSharePhase(share: Boolean) = viewModelScope.launch { prefsManager.setSharePhase(share) }
    fun setSharePredictions(share: Boolean) = viewModelScope.launch { prefsManager.setSharePredictions(share) }
    fun setShareMood(share: Boolean) = viewModelScope.launch { prefsManager.setShareMood(share) }
    fun setShareSymptoms(share: Boolean) = viewModelScope.launch { prefsManager.setShareSymptoms(share) }

    /** Actualiza mi privacidad granular dentro de un vínculo concreto. */
    fun updateLinkPrivacy(linkId: String, privacy: SharePreferences) {
        viewModelScope.launch {
            try {
                repository.updateMyPrivacy(linkId, privacy)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo actualizar la privacidad del vínculo: ${e.message}")
            }
        }
    }

    // ── Detalles (Care Actions) ──────────────────────────────────────────────

    /** Envía un Detalle con enfriamiento de 5 minutos compartido y mensaje opcional. */
    fun sendCareAction(linkId: String, action: CareAction, message: String? = null) {
        if (_uiState.value.isCareActionOnCooldown()) return
        viewModelScope.launch {
            try {
                repository.sendCareAction(linkId, action, message)
                prefsManager.setHugCooldownUntil(System.currentTimeMillis() + CARE_ACTION_COOLDOWN_MS)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo enviar el detalle: ${e.message}")
            }
        }
    }

    // ── Vínculos ─────────────────────────────────────────────────────────────

    fun unlink(linkId: String) = viewModelScope.launch {
        try {
            repository.unlink(linkId)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo desvincular: ${e.message}")
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    // ── Diario Compartido ────────────────────────────────────────────────────

    private var diaryJob: Job? = null

    fun observeDiary(linkId: String) {
        diaryJob?.cancel()
        diaryJob = viewModelScope.launch {
            repository.observeDiaryEntries(linkId).collect { entries ->
                _uiState.update { it.copy(diaryEntries = entries) }
            }
        }
    }

    fun sendDiaryEntry(linkId: String, text: String, myPhase: CyclePhase) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDiarySending = true) }
            try {
                repository.sendDiaryEntry(
                    linkId = linkId,
                    text = text,
                    phase = myPhase,
                    authorName = _uiState.value.currentUserName ?: "Yo"
                )
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo enviar la entrada del diario: ${e.message}")
            } finally {
                _uiState.update { it.copy(isDiarySending = false) }
            }
        }
    }
}
