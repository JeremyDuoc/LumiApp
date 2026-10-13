package com.jeremy.lumi.domain.model

import com.jeremy.lumi.ui.screens.bonds.BondMember

/**
 * Representa el círculo social privado completo de una persona en Bonds.
 */
data class BondsCircle(
    val members: List<BondMember> = emptyList(),
    val pendingInviteCode: String? = null,
    val isObserverOnly: Boolean = false
)
