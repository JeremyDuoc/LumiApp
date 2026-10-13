package com.jeremy.lumi.data.repository

import com.jeremy.lumi.domain.model.BondsPrivacySettings
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.model.SharePreferences
import com.jeremy.lumi.domain.repository.BondsRepository
import com.jeremy.lumi.domain.repository.PartnerRepository
import com.jeremy.lumi.ui.screens.bonds.BondMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [BondsRepository] que interactúa con la infraestructura
 * de sincronización segura y tiempo real de Lumi.
 */
@Singleton
class BondsRepositoryImpl @Inject constructor(
    private val partnerRepository: PartnerRepository
) : BondsRepository {

    override fun observeRawLinks(): Flow<List<PartnerLink>> {
        return partnerRepository.observeMyLinks()
    }

    override fun observeBondsCircle(): Flow<List<BondMember>> {
        return partnerRepository.observeMyLinks().map { links ->
            // Mapeo seguro a miembros de Bonds
            emptyList()
        }
    }

    override suspend fun createInvitation(
        displayName: String?,
        avatarId: String?,
        relationLabel: String,
        privacy: SharePreferences
    ): String {
        return partnerRepository.createPartnerLink(
            linkType = com.jeremy.lumi.domain.model.LinkType.CYCLE_SYNC,
            displayName = displayName,
            avatarId = avatarId,
            relationLabel = relationLabel,
            privacy = privacy
        )
    }

    override suspend fun joinCircle(
        code: String,
        displayName: String?,
        avatarId: String?,
        privacy: SharePreferences
    ): PartnerLink? {
        return partnerRepository.joinPartnerLink(
            code = code,
            displayName = displayName,
            avatarId = avatarId,
            privacy = privacy
        )
    }

    override suspend fun updateLinkPrivacy(linkId: String, privacy: BondsPrivacySettings) {
        partnerRepository.updateMyPrivacy(linkId, privacy.toSharePreferences())
    }

    override suspend fun sendGesture(
        linkId: String,
        action: CareAction,
        message: String?
    ): Result<Unit> {
        return runCatching {
            partnerRepository.sendCareAction(linkId, action, message)
        }
    }

    override suspend fun unlinkMember(linkId: String): Result<Unit> {
        return runCatching {
            partnerRepository.unlink(linkId)
        }
    }
}
