package com.jeremy.lumi.domain.repository

import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.CycleSnapshot
import com.jeremy.lumi.domain.model.DiaryEntry
import com.jeremy.lumi.domain.model.LinkType
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.model.SharePreferences
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del Modo Pareja. La implementación vive en la capa de datos
 * (Firestore + FCM); el dominio y la UI solo conocen esta interfaz.
 *
 * Caché offline: la implementación se apoya en la persistencia local de
 * Firestore, así que los Flows emiten primero el estado cacheado y luego
 * el fresco cuando vuelve la red.
 */
interface PartnerRepository {

    /** Garantiza sesión anónima de Firebase (silenciosa, sin UI). */
    suspend fun signInAnonymouslyIfNeeded(): Result<Unit>

    /** UID anónimo de esta instalación, o null si no hay red la primera vez. */
    suspend fun getCurrentUid(): String?

    /** Observa en tiempo real todos mis vínculos (PENDING + ACTIVE, ambos roles). */
    fun observeMyLinks(): Flow<List<PartnerLink>>

    /** Observa un vínculo concreto (para pantallas de detalle). */
    fun observeLink(linkId: String): Flow<PartnerLink?>

    /**
     * Crea un vínculo nuevo como titular y devuelve el código de 6 dígitos
     * que la otra persona usará para unirse.
     */
    suspend fun createPartnerLink(
        linkType: LinkType,
        displayName: String?,
        avatarId: String?,
        relationLabel: String,
        privacy: SharePreferences
    ): String

    /**
     * Se une a un vínculo PENDING usando el código.
     * @return el vínculo activado, o null si el código no existe o ya se usó.
     */
    suspend fun joinPartnerLink(
        code: String,
        displayName: String?,
        avatarId: String?,
        privacy: SharePreferences
    ): PartnerLink?

    /** Actualiza MI privacidad granular dentro de un vínculo concreto. */
    suspend fun updateMyPrivacy(linkId: String, privacy: SharePreferences)

    /** Actualiza MI nombre/avatar dentro de un vínculo concreto. */
    suspend fun updateMyIdentity(linkId: String, displayName: String?, avatarId: String?)

    /**
     * Publica mi estado de ciclo (ya filtrado por mi privacidad) en todos los
     * vínculos activos donde corresponda.
     */
    suspend fun publishSnapshot(snapshot: CycleSnapshot)

    /** Envía un detalle (abrazo, té, chocolate…) al vínculo con mensaje opcional. */
    suspend fun sendCareAction(linkId: String, action: CareAction, message: String? = null)

    /** Revoca el vínculo (cualquiera de las dos personas puede hacerlo). */
    suspend fun unlink(linkId: String)

    /** Observa el Diario Compartido del vínculo, ordenado ascendente. */
    fun observeDiaryEntries(linkId: String): Flow<List<DiaryEntry>>

    /** Escribe una entrada en el Diario Compartido. */
    suspend fun sendDiaryEntry(
        linkId: String,
        text: String,
        phase: CyclePhase,
        authorName: String
    )

    /** Refresca mi token FCM en todos mis vínculos activos (tras onNewToken). */
    suspend fun refreshMyFcmToken()
}
