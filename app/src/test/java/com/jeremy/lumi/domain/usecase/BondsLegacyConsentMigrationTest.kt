package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.CycleSnapshot
import com.jeremy.lumi.domain.model.LinkStatus
import com.jeremy.lumi.domain.model.LinkType
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.model.SharePreferences
import com.jeremy.lumi.domain.model.filteredBy
import com.jeremy.lumi.domain.model.otherSharePreferences
import com.jeremy.lumi.domain.model.otherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de salvaguarda de consentimiento explícito y neutralización de vínculos heredados (SEC-P0-B):
 * 1. Un vínculo heredado sin consentimiento explícito neutraliza el snapshot y permisos a false.
 * 2. Unirse a un vínculo no autoriza la entrega de datos de salud sin consentimiento explícito.
 * 3. Independencia de consentimiento entre titular y pareja.
 * 4. Tras otorgar consentimiento explícito, los datos autorizados fluyen de forma granular.
 */
class BondsLegacyConsentMigrationTest {

    @Test
    fun testLegacyLinkWithoutConsent_NeutralizesSnapshotAndPreferences() {
        val legacyLink = PartnerLink(
            linkId = "legacy_link_1",
            ownerUid = "alice_uid",
            partnerUid = "bob_uid",
            status = LinkStatus.ACTIVE,
            linkType = LinkType.OBSERVER,
            ownerSharePhase = true,       // Permisos heredados antiguos
            ownerShareMood = true,
            ownerShareSymptoms = true,
            ownerSharePredictions = true,
            ownerConsentExplicitlyGranted = false, // Sin consentimiento explícito demostrable
            ownerSnapshot = CycleSnapshot(
                currentPhase = CyclePhase.FOLLICULAR,
                currentMood = "Energetic",
                topSymptoms = listOf("Headache"),
                cycleStartDateMs = 1700000000000L,
                cycleLengthDays = 28,
                periodLengthDays = 5
            )
        )

        // Bob consulta el snapshot y la privacidad de Alice
        val snapshotForBob = legacyLink.otherSnapshot("bob_uid")
        val prefsForBob = legacyLink.otherSharePreferences("bob_uid")

        // SEC-P0-B: Sin consentimiento explícito, la entrega de datos de salud se neutraliza
        assertNull("El snapshot de la titular debe ser nulo para la pareja sin consentimiento explícito", snapshotForBob)
        assertFalse("phase debe ser forzado a false", prefsForBob.phase)
        assertFalse("mood debe ser forzado a false", prefsForBob.mood)
        assertFalse("symptoms debe ser forzado a false", prefsForBob.symptoms)
        assertFalse("predictions debe ser forzado a false", prefsForBob.predictions)
        assertFalse("consentExplicitlyGranted debe ser false", prefsForBob.consentExplicitlyGranted)
    }

    @Test
    fun testExplicitConsentGranted_PermitsAuthorizedSnapshot() {
        val consentedLink = PartnerLink(
            linkId = "consented_link_1",
            ownerUid = "alice_uid",
            partnerUid = "bob_uid",
            status = LinkStatus.ACTIVE,
            linkType = LinkType.CYCLE_SYNC,
            ownerSharePhase = true,
            ownerShareMood = true,
            ownerShareSymptoms = false,
            ownerSharePredictions = false,
            ownerConsentExplicitlyGranted = true, // Consentimiento explícito otorgado
            ownerSnapshot = CycleSnapshot(
                currentPhase = CyclePhase.FOLLICULAR,
                currentMood = "Calm",
                topSymptoms = emptyList()
            )
        )

        val snapshotForBob = consentedLink.otherSnapshot("bob_uid")
        val prefsForBob = consentedLink.otherSharePreferences("bob_uid")

        assertNotNull(snapshotForBob)
        assertEquals(CyclePhase.FOLLICULAR, snapshotForBob?.currentPhase)
        assertEquals("Calm", snapshotForBob?.currentMood)
        assertTrue(prefsForBob.phase)
        assertTrue(prefsForBob.mood)
        assertFalse(prefsForBob.symptoms)
        assertFalse(prefsForBob.predictions)
        assertTrue(prefsForBob.consentExplicitlyGranted)
    }

    @Test
    fun testCycleSyncDualConsentIndependence() {
        // Alice ha otorgado consentimiento explícito; Bob aún NO lo ha hecho
        val asymmetricLink = PartnerLink(
            linkId = "asymmetric_link",
            ownerUid = "alice_uid",
            partnerUid = "bob_uid",
            status = LinkStatus.ACTIVE,
            linkType = LinkType.CYCLE_SYNC,
            ownerSharePhase = true,
            ownerShareMood = true,
            ownerConsentExplicitlyGranted = true,
            ownerSnapshot = CycleSnapshot(currentPhase = CyclePhase.OVULATION, currentMood = "Happy"),
            partnerSharePhase = true,
            partnerShareMood = true,
            partnerConsentExplicitlyGranted = false, // Bob no ha consentido
            partnerSnapshot = CycleSnapshot(currentPhase = CyclePhase.LUTEAL, currentMood = "Tired")
        )

        // Bob consulta datos de Alice -> Autorizado porque Alice consintió
        val aliceSnapshotForBob = asymmetricLink.otherSnapshot("bob_uid")
        assertNotNull(aliceSnapshotForBob)
        assertEquals(CyclePhase.OVULATION, aliceSnapshotForBob?.currentPhase)

        // Alice consulta datos de Bob -> Neutralizado porque Bob no ha consentido
        val bobSnapshotForAlice = asymmetricLink.otherSnapshot("alice_uid")
        assertNull("Alice no debe ver el snapshot de Bob hasta que él otorgue consentimiento explícito", bobSnapshotForAlice)
        assertFalse(asymmetricLink.otherSharePreferences("alice_uid").phase)
    }

    @Test
    fun testRevokedLinkBlocksHealthDataEvenIfConsentWasGranted() {
        val revokedLink = PartnerLink(
            linkId = "revoked_link",
            ownerUid = "alice_uid",
            partnerUid = "bob_uid",
            status = LinkStatus.REVOKED,
            ownerConsentExplicitlyGranted = true,
            ownerSnapshot = null, // Snapshot vaciado tras revocación
            partnerSnapshot = null
        )

        assertNull(revokedLink.otherSnapshot("bob_uid"))
        assertNull(revokedLink.otherSnapshot("alice_uid"))
    }
}
