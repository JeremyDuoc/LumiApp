package com.jeremy.lumi.domain.model

/**
 * Representa una interacción afectiva realizada en Bonds (Abrazo, Té, Café, etc.).
 */
data class BondsGesture(
    val id: String = "",
    val linkId: String = "",
    val senderUid: String = "",
    val recipientUid: String = "",
    val action: CareAction = CareAction.HUG,
    val message: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)
