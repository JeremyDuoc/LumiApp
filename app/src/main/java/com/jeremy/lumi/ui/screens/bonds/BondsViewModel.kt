package com.jeremy.lumi.ui.screens.bonds

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.CareActionStatus
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.LinkStatus
import com.jeremy.lumi.domain.model.LinkType
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.model.SharePreferences
import com.jeremy.lumi.domain.model.careActionReceived
import com.jeremy.lumi.domain.model.isOwner
import com.jeremy.lumi.domain.model.mySharePreferences
import com.jeremy.lumi.domain.model.otherAvatarId
import com.jeremy.lumi.domain.model.otherDisplayName
import com.jeremy.lumi.domain.model.otherSharePreferences
import com.jeremy.lumi.domain.model.otherSnapshot
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.repository.PartnerRepository
import com.jeremy.lumi.domain.usecase.CyclePredictor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Representación en UI de una persona conectada en el círculo de BONDS.
 *
 * Principio de privacidad por defecto:
 * - [phase] y [cycleDay] son null si la persona NO autorizó compartir su ciclo.
 * - [mood] es null si NO autorizó compartir su estado de ánimo.
 * - [symptoms] es vacío si NO autorizó compartir síntomas.
 */
data class BondMember(
    val linkId: String,
    val displayName: String,
    val avatarId: String?,
    val phase: CyclePhase?,
    val cycleDay: Int?,
    val mood: String?,
    val symptoms: List<String>,
    val cycleStartDateMs: Long?,
    val cycleLengthDays: Int,
    val periodLengthDays: Int,
    val otherSharePrefs: SharePreferences,
    val mySharePrefs: SharePreferences,
    val isOwner: Boolean,
    val lastReceivedGesture: CareActionStatus?,
    val rawLink: PartnerLink,
    val requiresConsentReconfirmation: Boolean = false
)

data class BondsUiState(
    val isLoading: Boolean = true,
    val members: List<BondMember> = emptyList(),
    val selectedMember: BondMember? = null,
    val pendingInviteCode: String? = null,
    val myUid: String? = null,
    val myName: String = "",
    val myAvatarId: String? = null,
    val isObserverOnly: Boolean = false,
    val cooldownUntilMs: Long = 0L,
    val isSendingGesture: Boolean = false,
    val lastGestureSent: CareAction? = null,
    val errorMessage: String? = null,
    val myDefaultPrivacy: SharePreferences = SharePreferences(
        phase = false,
        mood = false,
        symptoms = false,
        predictions = false,
        consentExplicitlyGranted = false
    )
) {
    val isCooldownActive: Boolean
        get() = cooldownUntilMs > System.currentTimeMillis()

    val cooldownSecondsRemaining: Int
        get() = ((cooldownUntilMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
}

@HiltViewModel
class BondsViewModel @Inject constructor(
    private val partnerRepository: PartnerRepository,
    private val lumiRepository: LumiRepository,
    private val prefsManager: OnboardingPreferenceManager
) : ViewModel() {

    companion object {
        private const val TAG = "BondsViewModel"
        private const val COOLDOWN_DURATION_MS = 5 * 60 * 1000L
    }

    private val _uiState = MutableStateFlow(BondsUiState())
    val uiState: StateFlow<BondsUiState> = _uiState.asStateFlow()

    init {
        loadInitialUserSession()
        observeBondsCircle()
        observeUserPreferences()
    }

    private fun loadInitialUserSession() {
        viewModelScope.launch {
            val uid = partnerRepository.getCurrentUid()
            val name = prefsManager.userNameFlow.first().orEmpty()
            val avatar = prefsManager.avatarIdFlow.first()
            val observer = prefsManager.isObserverOnly.first()
            _uiState.update {
                it.copy(
                    myUid = uid,
                    myName = name,
                    myAvatarId = avatar,
                    isObserverOnly = observer
                )
            }
        }
    }

    private fun observeBondsCircle() {
        viewModelScope.launch {
            partnerRepository.observeMyLinks().collect { links ->
                val myUid = _uiState.value.myUid ?: partnerRepository.getCurrentUid()
                val activeLinks = links.filter { it.status == LinkStatus.ACTIVE }

                // Solo reutilizar vínculos pendientes que tengan 8 dígitos y no hayan caducado (48 horas)
                val now = System.currentTimeMillis()
                val pendingCode = links.firstOrNull { link ->
                    link.status == LinkStatus.PENDING &&
                    link.isOwner(myUid) &&
                    link.linkCode.length == 8 &&
                    (link.createdAtMs == 0L || (now - link.createdAtMs) < 48 * 60 * 60 * 1000L)
                }?.linkCode

                val memberList = activeLinks.map { link ->
                    val otherPrefs = link.otherSharePreferences(myUid)
                    val snapshot = link.otherSnapshot(myUid)
                    val otherName = link.otherDisplayName(myUid) ?: "Amiga"
                    val otherAvatar = link.otherAvatarId(myUid)
                    val myPrefs = link.mySharePreferences(myUid)
                    val receivedGesture = link.careActionReceived(myUid)

                    // Cálculo del día del ciclo y fase condicionado estrictamente a privacidad
                    val allowedPhase = if (otherPrefs.phase) snapshot?.currentPhase else null
                    val allowedDay = if (otherPrefs.phase || otherPrefs.predictions) {
                        snapshot?.cycleStartDateMs?.let { startMs ->
                            calculateCycleDay(startMs, snapshot.cycleLengthDays)
                        }
                    } else null

                    val allowedMood = if (otherPrefs.mood) snapshot?.currentMood else null
                    val allowedSymptoms = if (otherPrefs.symptoms) snapshot?.topSymptoms ?: emptyList() else emptyList()

                    val consentGranted = if (link.isOwner(myUid)) link.ownerConsentExplicitlyGranted else link.partnerConsentExplicitlyGranted

                    BondMember(
                        linkId = link.linkId,
                        displayName = otherName,
                        avatarId = otherAvatar,
                        phase = allowedPhase,
                        cycleDay = allowedDay,
                        mood = allowedMood,
                        symptoms = allowedSymptoms,
                        cycleStartDateMs = snapshot?.cycleStartDateMs,
                        cycleLengthDays = snapshot?.cycleLengthDays ?: 28,
                        periodLengthDays = snapshot?.periodLengthDays ?: 5,
                        otherSharePrefs = otherPrefs,
                        mySharePrefs = myPrefs,
                        isOwner = link.isOwner(myUid),
                        lastReceivedGesture = receivedGesture,
                        rawLink = link,
                        requiresConsentReconfirmation = !consentGranted
                    )
                }

                val resolvedMembers = memberList

                _uiState.update { current ->
                    val prevSelectedId = current.selectedMember?.linkId
                    val newSelected = resolvedMembers.find { it.linkId == prevSelectedId } ?: resolvedMembers.firstOrNull()
                    current.copy(
                        isLoading = false,
                        members = resolvedMembers,
                        selectedMember = newSelected,
                        pendingInviteCode = pendingCode
                    )
                }
            }
        }
    }

    private fun observeUserPreferences() {
        viewModelScope.launch {
            combine(
                prefsManager.sharePhaseFlow,
                prefsManager.shareMoodFlow,
                prefsManager.shareSymptomsFlow,
                prefsManager.sharePredictionsFlow,
                prefsManager.hugCooldownUntilFlow
            ) { phase, mood, symptoms, predictions, cooldown ->
                _uiState.update {
                    it.copy(
                        myDefaultPrivacy = SharePreferences(
                            phase = phase,
                            mood = mood,
                            symptoms = symptoms,
                            predictions = predictions
                        ),
                        cooldownUntilMs = cooldown
                    )
                }
            }.collect { }
        }
    }

    fun selectMember(member: BondMember) {
        _uiState.update { it.copy(selectedMember = member) }
    }

    fun selectMemberById(linkId: String) {
        val member = _uiState.value.members.find { it.linkId == linkId }
        if (member != null) {
            _uiState.update { it.copy(selectedMember = member) }
        }
    }

    /**
     * Envía un detalle afectivo con mensaje opcional a una persona vinculada.
     */
    fun sendGesture(
        linkId: String,
        action: CareAction,
        message: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        if (_uiState.value.isCooldownActive) {
            _uiState.update { it.copy(errorMessage = "Debes esperar a que termine el tiempo de enfriamiento.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingGesture = true, errorMessage = null) }
            try {
                partnerRepository.sendCareAction(linkId, action, message)
                val newCooldown = System.currentTimeMillis() + COOLDOWN_DURATION_MS
                prefsManager.setHugCooldownUntil(newCooldown)
                _uiState.update {
                    it.copy(
                        isSendingGesture = false,
                        lastGestureSent = action,
                        cooldownUntilMs = newCooldown
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando gesto: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isSendingGesture = false,
                        errorMessage = "No se pudo enviar el detalle. Revisa tu conexión."
                    )
                }
            }
        }
    }

    fun clearLastGestureSent() {
        _uiState.update { it.copy(lastGestureSent = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Guarda las preferencias de compartición por defecto del usuario.
     */
    fun updateDefaultPrivacy(privacy: SharePreferences) {
        viewModelScope.launch {
            prefsManager.setSharePhase(privacy.phase)
            prefsManager.setShareMood(privacy.mood)
            prefsManager.setShareSymptoms(privacy.symptoms)
            prefsManager.setSharePredictions(privacy.predictions)
            _uiState.update { it.copy(myDefaultPrivacy = privacy) }
        }
    }

    /**
     * Genera una nueva invitación temporal de 8 dígitos.
     */
    suspend fun createInvitation(
        linkType: LinkType = LinkType.CYCLE_SYNC,
        privacy: SharePreferences = _uiState.value.myDefaultPrivacy,
        relationLabel: String = ""
    ): String {
        val name = _uiState.value.myName.ifBlank { "Lumi" }
        val avatar = _uiState.value.myAvatarId
        val code = partnerRepository.createPartnerLink(
            linkType = linkType,
            displayName = name,
            avatarId = avatar,
            relationLabel = relationLabel,
            privacy = privacy
        )
        _uiState.update { it.copy(pendingInviteCode = code) }
        return code
    }

    /**
     * Unirse a un vínculo usando un código de invitación (8 dígitos, o 6 legacy en transición).
     */
    fun joinWithCode(
        code: String,
        privacy: SharePreferences = _uiState.value.myDefaultPrivacy,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cleanCode = code.filter { it.isDigit() }
                val name = _uiState.value.myName.ifBlank { "Lumi" }
                val avatar = _uiState.value.myAvatarId
                val link = partnerRepository.joinPartnerLink(
                    code = cleanCode,
                    displayName = name,
                    avatarId = avatar,
                    privacy = privacy
                )
                if (link != null) {
                    onComplete(true)
                } else {
                    _uiState.update { it.copy(errorMessage = "Código inválido o ya utilizado.") }
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al unirse al vínculo: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Error de red al verificar el código.") }
                onComplete(false)
            }
        }
    }

    /**
     * Actualiza la privacidad específica para un vínculo determinado.
     */
    fun updateLinkPrivacy(linkId: String, privacy: SharePreferences) {
        viewModelScope.launch {
            try {
                partnerRepository.updateMyPrivacy(linkId, privacy)
                // Actualización optimista local
                _uiState.update { current ->
                    val updated = current.members.map { m ->
                        if (m.linkId == linkId) m.copy(mySharePrefs = privacy) else m
                    }
                    val updatedSelected = if (current.selectedMember?.linkId == linkId) {
                        current.selectedMember.copy(mySharePrefs = privacy)
                    } else current.selectedMember
                    current.copy(members = updated, selectedMember = updatedSelected)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error actualizando privacidad de vínculo: ${e.message}", e)
            }
        }
    }

    /**
     * Desvincula una persona específica sin alterar los demás vínculos.
     */
    fun unlinkMember(linkId: String, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                partnerRepository.unlink(linkId)
                _uiState.update { current ->
                    val remaining = current.members.filter { it.linkId != linkId }
                    current.copy(
                        members = remaining,
                        selectedMember = remaining.firstOrNull()
                    )
                }
                onFinished()
            } catch (e: Exception) {
                Log.e(TAG, "Error desvinculando miembro: ${e.message}", e)
            }
        }
    }

    private fun calculateCycleDay(startDateMs: Long, cycleLength: Int): Int {
        val start = Instant.ofEpochMilli(startDateMs).atZone(ZoneOffset.UTC).toLocalDate()
        val today = LocalDate.now(ZoneOffset.UTC)
        if (today.isBefore(start)) return 1
        val days = java.time.temporal.ChronoUnit.DAYS.between(start, today)
        val effLength = if (cycleLength in 20..45) cycleLength else 28
        return ((days % effLength) + 1).toInt()
    }
}
