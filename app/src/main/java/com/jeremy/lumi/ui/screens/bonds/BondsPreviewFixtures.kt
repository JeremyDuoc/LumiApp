package com.jeremy.lumi.ui.screens.bonds

import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.LinkStatus
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.model.SharePreferences

/**
 * Fixtures aislados para uso exclusivo en Previews de Jetpack Compose y pruebas de diseño.
 * NUNCA se inyectan en tiempo de ejecución de producción.
 */
object BondsPreviewFixtures {
    val sampleMembers: List<BondMember> by lazy {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 3600 * 1000
        val dummyLink = PartnerLink(
            linkId = "demo_placeholder",
            ownerUid = "self",
            status = LinkStatus.ACTIVE,
            ownerConsentExplicitlyGranted = true,
            partnerConsentExplicitlyGranted = true
        )
        listOf(
            BondMember(
                linkId = "sample_camila",
                displayName = "Camila",
                avatarId = "flor",
                phase = CyclePhase.FOLLICULAR,
                cycleDay = 12,
                mood = "Sensible hoy, pero con buen ánimo 💜",
                symptoms = listOf("Calambres suaves"),
                cycleStartDateMs = now - (11L * dayMs),
                cycleLengthDays = 28,
                periodLengthDays = 5,
                otherSharePrefs = SharePreferences(phase = true, mood = true, symptoms = false, predictions = true, consentExplicitlyGranted = true),
                mySharePrefs = SharePreferences(phase = true, mood = true, symptoms = true, predictions = true, consentExplicitlyGranted = true),
                isOwner = true,
                lastReceivedGesture = null,
                rawLink = dummyLink.copy(linkId = "sample_camila")
            ),
            BondMember(
                linkId = "sample_sofia",
                displayName = "Sofia",
                avatarId = "sol",
                phase = CyclePhase.OVULATION,
                cycleDay = 18,
                mood = "Con mucha energía y entusiasmo ✨",
                symptoms = emptyList(),
                cycleStartDateMs = now - (17L * dayMs),
                cycleLengthDays = 30,
                periodLengthDays = 4,
                otherSharePrefs = SharePreferences(phase = true, mood = true, symptoms = false, predictions = true, consentExplicitlyGranted = true),
                mySharePrefs = SharePreferences(phase = true, mood = true, symptoms = false, predictions = true, consentExplicitlyGranted = true),
                isOwner = true,
                lastReceivedGesture = null,
                rawLink = dummyLink.copy(linkId = "sample_sofia")
            ),
            BondMember(
                linkId = "sample_valentina",
                displayName = "Valentina",
                avatarId = "brisa",
                phase = CyclePhase.MENSTRUAL,
                cycleDay = 6,
                mood = "Día tranquilo de descanso ☕",
                symptoms = listOf("Fatiga leve"),
                cycleStartDateMs = now - (5L * dayMs),
                cycleLengthDays = 27,
                periodLengthDays = 5,
                otherSharePrefs = SharePreferences(phase = true, mood = true, symptoms = true, predictions = true, consentExplicitlyGranted = true),
                mySharePrefs = SharePreferences(phase = true, mood = true, symptoms = true, predictions = true, consentExplicitlyGranted = true),
                isOwner = true,
                lastReceivedGesture = null,
                rawLink = dummyLink.copy(linkId = "sample_valentina")
            ),
            BondMember(
                linkId = "sample_anto",
                displayName = "Anto",
                avatarId = "corazon",
                phase = CyclePhase.LUTEAL,
                cycleDay = 22,
                mood = "Necesitando desconexión y tiempo para mí 🌸",
                symptoms = emptyList(),
                cycleStartDateMs = now - (21L * dayMs),
                cycleLengthDays = 29,
                periodLengthDays = 5,
                otherSharePrefs = SharePreferences(phase = true, mood = true, symptoms = false, predictions = true, consentExplicitlyGranted = true),
                mySharePrefs = SharePreferences(phase = true, mood = true, symptoms = true, predictions = true, consentExplicitlyGranted = true),
                isOwner = true,
                lastReceivedGesture = null,
                rawLink = dummyLink.copy(linkId = "sample_anto")
            )
        )
    }
}
