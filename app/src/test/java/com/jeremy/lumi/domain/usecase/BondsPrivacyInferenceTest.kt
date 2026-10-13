package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.CycleSnapshot
import com.jeremy.lumi.domain.model.SharePreferences
import com.jeremy.lumi.domain.model.filteredBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas automatizadas de validación de remediación de privacidad:
 * 1. P0-4 SOLUCIONADO: Principio de Privacidad por Defecto (Privacy by Default).
 * 2. P1-6 SOLUCIONADO: Imposibilidad de inferencia matemática indirecta de fases menstruales.
 * 3. Filtrado completo cuando todos los consentimientos están desactivados.
 */
class BondsPrivacyInferenceTest {

    @Test
    fun testP04_defaultPrivacyFlagsAreStrictlyFalseByDefault() {
        val defaultPrefs = SharePreferences()
        // P0-4 remediado: Principio de Privacidad por Defecto (Privacy by Default)
        assertFalse("phase debe ser false por defecto (Opt-in estricto)", defaultPrefs.phase)
        assertFalse("mood debe ser false por defecto (Opt-in estricto)", defaultPrefs.mood)
        assertFalse("symptoms debe ser false por defecto (Opt-in estricto)", defaultPrefs.symptoms)
        assertFalse("predictions debe ser false por defecto (Opt-in estricto)", defaultPrefs.predictions)
        assertFalse("consentExplicitlyGranted debe ser false inicialmente", defaultPrefs.consentExplicitlyGranted)
    }

    @Test
    fun testP16_predictionsNeverExposeCycleStartWhenPhaseIsFalse() {
        val originalSnapshot = CycleSnapshot(
            currentPhase = CyclePhase.MENSTRUAL,
            currentMood = "Sensitive",
            topSymptoms = listOf("Cramps"),
            daysUntilNextPhase = 3,
            isLate = false,
            delayDays = 0,
            cycleStartDateMs = System.currentTimeMillis() - (2L * 86400000L), // hace 2 días
            cycleLengthDays = 28,
            periodLengthDays = 5
        )

        // Usuaria apaga explícitamente "phase", pero deja "predictions" activado
        val restrictedPrivacy = SharePreferences(
            phase = false,
            mood = false,
            symptoms = false,
            predictions = true
        )

        val filtered = originalSnapshot.filteredBy(restrictedPrivacy)

        // 1. La fase directa está censurada (UNKNOWN)
        assertEquals(CyclePhase.UNKNOWN, filtered.currentPhase)
        assertNull(filtered.currentMood)
        assertTrue(filtered.topSymptoms.isEmpty())

        // 2. VULNERABILIDAD P1-6 REMEDIADA:
        // Los parámetros matemáticos del ciclo están completamente eliminados
        assertNull("cycleStartDateMs DEBE ser nulo cuando phase=false para imposibilitar inferencia", filtered.cycleStartDateMs)
        assertEquals(0, filtered.cycleLengthDays)
        assertEquals(0, filtered.periodLengthDays)

        // 3. Las métricas relativas de predicción se mantienen seguras
        assertEquals(3, filtered.daysUntilNextPhase)
        assertFalse(filtered.isLate)
        assertEquals(0, filtered.delayDays)
    }

    @Test
    fun testFullOptInAllowsCycleStartDateOnlyWhenBothPhaseAndPredictionsAreTrue() {
        val now = System.currentTimeMillis()
        val originalSnapshot = CycleSnapshot(
            currentPhase = CyclePhase.FOLLICULAR,
            currentMood = "Happy",
            topSymptoms = emptyList(),
            cycleStartDateMs = now - (10L * 86400000L),
            cycleLengthDays = 28,
            periodLengthDays = 5
        )

        val fullOptIn = SharePreferences(
            phase = true,
            mood = true,
            symptoms = true,
            predictions = true,
            consentExplicitlyGranted = true
        )

        val filtered = originalSnapshot.filteredBy(fullOptIn)

        assertEquals(CyclePhase.FOLLICULAR, filtered.currentPhase)
        assertEquals("Happy", filtered.currentMood)
        assertNotNull(filtered.cycleStartDateMs)
        assertEquals(28, filtered.cycleLengthDays)
        assertEquals(5, filtered.periodLengthDays)
    }

    @Test
    fun testFullOptOutRedactsAllHealthData() {
        val originalSnapshot = CycleSnapshot(
            currentPhase = CyclePhase.OVULATION,
            currentMood = "Energetic",
            topSymptoms = listOf("Headache"),
            cycleStartDateMs = System.currentTimeMillis(),
            cycleLengthDays = 28,
            periodLengthDays = 5
        )

        val strictOptOut = SharePreferences(
            phase = false,
            mood = false,
            symptoms = false,
            predictions = false
        )

        val filtered = originalSnapshot.filteredBy(strictOptOut)

        assertEquals(CyclePhase.UNKNOWN, filtered.currentPhase)
        assertNull(filtered.currentMood)
        assertTrue(filtered.topSymptoms.isEmpty())
        assertNull(filtered.cycleStartDateMs)
        assertEquals(0, filtered.cycleLengthDays)
        assertEquals(0, filtered.periodLengthDays)
    }
}
