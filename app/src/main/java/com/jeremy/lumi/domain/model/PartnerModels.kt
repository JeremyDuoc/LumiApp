package com.jeremy.lumi.domain.model

import com.google.firebase.firestore.PropertyName

/**
 * Representa el estado del vínculo entre la usuaria titular y su pareja.
 */
data class PartnerLink(
    val linkId: String = "",           // ID del documento en Firestore
    val ownerUid: String = "",         // UID anónimo de Firebase de la chica
    val partnerUid: String? = null,    // UID anónimo de Firebase de la pareja
    val linkCode: String = "",         // Código de 6 dígitos generado
    val status: LinkStatus = LinkStatus.PENDING,
    val linkType: LinkType = LinkType.OBSERVER,

    val ownerDisplayName: String? = null,
    val relationLabel: String = "",

    // Tokens FCM para comunicación Direct-to-Device
    val ownerFcmToken: String? = null,
    val partnerFcmToken: String? = null,

    // Preferencias de privacidad de la titular (owner)
    val ownerSharePhase: Boolean = true,
    val ownerShareMood: Boolean = true,
    val ownerShareSymptoms: Boolean = false,
    val ownerSharePredictions: Boolean = true,

    // Preferencias de privacidad de la pareja (partner, solo aplicable si es CYCLE_SYNC)
    val partnerSharePhase: Boolean = true,
    val partnerShareMood: Boolean = true,
    val partnerShareSymptoms: Boolean = false,
    val partnerSharePredictions: Boolean = true,

    // Snapshots del ciclo (doble vía para CYCLE_SYNC)
    val ownerSnapshot: CycleSnapshot? = null,
    val partnerSnapshot: CycleSnapshot? = null,

    // Reemplazando timestamps de abrazos por acciones de cuidado
    val lastOwnerCareAction: CareActionStatus? = null,
    val lastPartnerCareAction: CareActionStatus? = null
)

enum class LinkStatus { PENDING, ACTIVE, REVOKED }
enum class LinkType { OBSERVER, CYCLE_SYNC }

enum class CareAction { HUG, TEA, COFFEE, CHOCOLATE, PHARMACY }

data class CareActionStatus(
    val action: CareAction = CareAction.HUG,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Snapshot ligero que se sincroniza/envía al dispositivo de la pareja.
 * Esto representa el "Estado Actual" sin el historial médico completo.
 */
data class CycleSnapshot(
    val currentPhase: CyclePhase = CyclePhase.UNKNOWN,
    val daysUntilNextPhase: Int = 0,
    @get:PropertyName("late")
    @set:PropertyName("late")
    var isLate: Boolean = false,
    val delayDays: Int = 0,

    // Si la chica decide compartirlo:
    val currentMood: String? = null,
    val topSymptoms: List<String> = emptyList(),

    val lastUpdatedAt: Long = System.currentTimeMillis()
)
