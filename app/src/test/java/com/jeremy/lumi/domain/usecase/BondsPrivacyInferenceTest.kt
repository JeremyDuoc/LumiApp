package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.CycleSnapshot
import com.jeremy.lumi.domain.model.SharePreferences
import com.jeremy.lumi.domain.model.filteredBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Pruebas automatizadas de auditoría para verificar:
 * 1. P0-4: Valores por defecto de privacidad.
 * 2. P1-6: Inferencia indirecta de fases a través del permiso de predicciones.
 * 3. Filtrado completo cuando todos los consentimientos están desactivados.
 */
class BondsPrivacyInferenceTest {

    @Test
    fun testP04_defaultPrivacyFlagsAreTrueByDefault() {
        val defaultPrefs = SharePreferences()
        // P0-4 comprobado: en el código actual, phase, mood y predictions vienen true por defecto
        assertTrue("phase debería ser true por defecto en código actual", defaultPrefs.phase)
        assertTrue("mood debería ser true por defecto en código actual", defaultPrefs.mood)
        assertTrue("predictions debería ser true por defecto en código actual", defaultPrefs.predictions)
    }

    @Test
    fun testP16_predictionsExposeCycleStartAndAllowPhaseReconstructionWhenPhaseIsFalse() {
        val originalSnapshot = CycleSnapshot(
            currentPhase = CyclePhase.MENSTRUAL,
            currentMood = "Sensitive",
            topSymptoms = listOf("Cramps"),
            cycleStartDateMs = System.currentTimeMillis() - (2L * 86400000L), // hace 2 días
            cycleLengthDays = 28,
            periodLengthDays = 5
        )

        // Usuaria explícitamente apaga "phase", pero deja "predictions" activado
        val restrictedPrivacy = SharePreferences(
            phase = false,
            mood = false,
            symptoms = false,
            predictions = true
        )

        val filtered = originalSnapshot.filteredBy(restrictedPrivacy)

        // La fase directa está censurada (UNKNOWN)
        assertEquals(CyclePhase.UNKNOWN, filtered.currentPhase)
        assertNull(filtered.currentMood)
        assertTrue(filtered.topSymptoms.isEmpty())

        // VULNERABILIDAD P1-6 DEMOSTRADA:
        // Los parámetros matemáticos del ciclo NO están censurados
        assertNotNull("cycleStartDateMs sigue expuesto", filtered.cycleStartDateMs)
        assertEquals(28, filtered.cycleLengthDays)
        assertEquals(5, filtered.periodLengthDays)

        // Tercero puede reconstruir que hoy es el día 3 del ciclo (Fase Menstrual)
        val start = java.time.Instant.ofEpochMilli(filtered.cycleStartDateMs!!).atZone(java.time.ZoneOffset.UTC).toLocalDate()
        val today = LocalDate.now(java.time.ZoneOffset.UTC)
        val dayInCycle = CyclePredictor.dayInCycleWrapped(start, filtered.cycleLengthDays, today)
        val reconstructedPhase = CyclePredictor.phaseForDay(dayInCycle, filtered.cycleLengthDays, filtered.periodLengthDays)

        // Se infiere que la persona está menstruando pese a tener sharePhase = false
        assertEquals(CyclePhase.MENSTRUAL, reconstructedPhase)
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
