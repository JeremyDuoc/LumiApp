package com.jeremy.lumi.domain.repository

import com.jeremy.lumi.domain.model.BondsCircle
import com.jeremy.lumi.domain.model.BondsGesture
import com.jeremy.lumi.domain.model.BondsPrivacySettings
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.model.SharePreferences
import com.jeremy.lumi.ui.screens.bonds.BondMember
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio principal para el módulo BONDS.
 *
 * Administra el círculo de confianza multivínculo, el intercambio seguro
 * y consentido de datos de salud y el envío de gestos afectivos.
 */
interface BondsRepository {
    /** Observa todas las personas conectadas en el círculo de Bonds. */
    fun observeBondsCircle(): Flow<List<BondMember>>

    /** Observa los vínculos brutos de la capa de datos. */
    fun observeRawLinks(): Flow<List<PartnerLink>>

    /** Crea una nueva invitación al círculo y devuelve el código de 6 dígitos. */
    suspend fun createInvitation(
        displayName: String? = null,
        avatarId: String? = null,
        relationLabel: String = "Círculo de confianza",
        privacy: SharePreferences = SharePreferences()
    ): String

    /** Se une a un círculo existente usando el código de 6 dígitos. */
    suspend fun joinCircle(
        code: String,
        displayName: String? = null,
        avatarId: String? = null,
        privacy: SharePreferences = SharePreferences()
    ): PartnerLink?

    /** Actualiza los permisos de privacidad para un vínculo específico. */
    suspend fun updateLinkPrivacy(linkId: String, privacy: BondsPrivacySettings)

    /** Envía un gesto de afecto a una persona vinculada. */
    suspend fun sendGesture(
        linkId: String,
        action: CareAction,
        message: String?
    ): Result<Unit>

    /** Desvincula y revoca el acceso mutuo de una persona del círculo. */
    suspend fun unlinkMember(linkId: String): Result<Unit>
}
