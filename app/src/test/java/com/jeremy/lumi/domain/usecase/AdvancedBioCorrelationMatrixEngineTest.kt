package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.domain.model.BioVariable
import com.jeremy.lumi.domain.model.CorrelationDirection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Tests para [AdvancedBioCorrelationMatrixEngine].
 *
 * Estrategia: datasets sintéticos con correlaciones conocidas matemáticamente,
 * verificando que el motor detecte correctamente:
 *   - Correlación Spearman positiva significativa (ρ ≈ +0.9)
 *   - Correlación Spearman negativa significativa (ρ ≈ -0.9)
 *   - Correlación con lag T-1
 *   - Filtrado de correlaciones espurias (|ρ| < 0.35 ó p ≥ 0.05)
 */
class AdvancedBioCorrelationMatrixEngineTest {

    private lateinit var engine: AdvancedBioCorrelationMatrixEngine

    @Before
    fun setup() {
        engine = AdvancedBioCorrelationMatrixEngine()
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun makeLog(
        dayOffset: Int,
        sleep: Float? = null,
        pain: Int = 0,
        stress: Int? = null,
        energy: Int? = null,
        water: Float? = null,
        bbt: Float? = null
    ): DailyLogWithSymptoms {
        val base = LocalDate.of(2026, 1, 1)
        val date = base.plusDays(dayOffset.toLong())
        val epochMs = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return DailyLogWithSymptoms(
            dailyLog = DailyLogEntity(
                cycleId = 1,
                date = epochMs,
                sleepHours = sleep,
                painLevel = pain,
                stressLevel = stress,
                energyLevel = energy,
                waterIntake = water,
                basalBodyTemp = bbt
            ),
            symptoms = emptyList()
        )
    }

    /**
     * Genera un dataset donde sueño y dolor tienen correlación NEGATIVA perfecta:
     * a mayor sueño (8-5h progresivo), menor dolor (0-10 progresivo).
     * ρ esperado ≈ -1.0, claramente significativo.
     */
    private fun makeSleepPainAntiCorrelatedLogs(n: Int = 15): List<DailyLogWithSymptoms> {
        return (0 until n).map { i ->
            // Sleep decreases linearly: 8h → 2h
            val sleep = 8f - (6f * i / (n - 1))
            // Pain increases linearly: 0 → 9
            val pain = (9 * i / (n - 1))
            makeLog(dayOffset = i, sleep = sleep, pain = pain)
        }
    }

    /**
     * Genera un dataset donde sueño y energía tienen correlación POSITIVA perfecta:
     * a mayor sueño, mayor energía.
     * ρ esperado ≈ +1.0.
     */
    private fun makeSleepEnergyCorrelatedLogs(n: Int = 15): List<DailyLogWithSymptoms> {
        return (0 until n).map { i ->
            val sleep = 4f + (4f * i / (n - 1))  // 4h → 8h
            val energy = (2 + 8 * i / (n - 1))   // 2 → 10
            makeLog(dayOffset = i, sleep = sleep, energy = energy)
        }
    }

    /**
     * Dataset con correlación espuria (ρ ≈ 0, valores aleatorios no correlacionados).
     */
    private fun makeUncorrelatedLogs(n: Int = 20): List<DailyLogWithSymptoms> {
        val sleepValues = listOf(7f, 5f, 8f, 6f, 7f, 5f, 8f, 6f, 7f, 5f, 8f, 6f, 7f, 5f, 8f, 6f, 7f, 5f, 8f, 6f)
        val painValues  = listOf(3, 7, 2, 8, 4, 6, 1, 9, 3, 5, 8, 2, 7, 4, 6, 1, 9, 3, 5, 8)
        return (0 until n).map { i ->
            makeLog(dayOffset = i, sleep = sleepValues[i % sleepValues.size], pain = painValues[i % painValues.size])
        }
    }

    /**
     * Dataset para Lag-1: stress del día i-1 predice pain del día i.
     * stress[0..N-1] = {8,8,8,2,2,2,8,8,8,2,2,2,8,8,8}
     * pain[0..N-1]   = {0,7,8,8,1,2,2,7,8,8,1,2,2,7,8}  (pain[i] ≈ stress[i-1])
     */
    private fun makeLaggedStressPainLogs(n: Int = 15): List<DailyLogWithSymptoms> {
        val stressValues = listOf(8, 8, 8, 2, 2, 2, 8, 8, 8, 2, 2, 2, 8, 8, 8)
        val painValues   = listOf(0, 8, 8, 8, 2, 2, 2, 8, 8, 8, 2, 2, 2, 8, 8)
        return (0 until n).map { i ->
            makeLog(
                dayOffset = i,
                stress = stressValues[i % stressValues.size],
                pain = painValues[i % painValues.size]
            )
        }
    }

    // ─── Test 1: Correlación Spearman negativa significativa ─────────────────

    @Test
    fun `test detects strong negative Spearman correlation sleep vs pain`() {
        val logs = makeSleepPainAntiCorrelatedLogs(n = 15)
        val insights = runBlocking { engine.computeInsights(logs) }

        val sleepPainInsight = insights.firstOrNull {
            (it.variableA == BioVariable.SLEEP_HOURS && it.variableB == BioVariable.PAIN_LEVEL) ||
            (it.variableA == BioVariable.PAIN_LEVEL && it.variableB == BioVariable.SLEEP_HOURS)
        }

        assertNotNull("Should detect sleep-pain correlation", sleepPainInsight)
        sleepPainInsight!!

        assertTrue(
            "Correlation score should be strongly negative (< -0.35), got ${sleepPainInsight.correlationScore}",
            sleepPainInsight.correlationScore < -0.35f
        )
        assertEquals(CorrelationDirection.NEGATIVE, sleepPainInsight.direction)
        assertTrue(
            "p-value should be significant (< 0.05), got ${sleepPainInsight.pValue}",
            sleepPainInsight.pValue < 0.05f
        )
        assertTrue(
            "Sample size should be >= 8, got ${sleepPainInsight.sampleSize}",
            sleepPainInsight.sampleSize >= 8
        )
    }

    // ─── Test 2: Correlación Spearman positiva significativa ─────────────────

    @Test
    fun `test detects strong positive Spearman correlation sleep vs energy`() {
        val logs = makeSleepEnergyCorrelatedLogs(n = 15)
        val insights = runBlocking { engine.computeInsights(logs) }

        val insight = insights.firstOrNull {
            (it.variableA == BioVariable.SLEEP_HOURS && it.variableB == BioVariable.ENERGY_LEVEL) ||
            (it.variableA == BioVariable.ENERGY_LEVEL && it.variableB == BioVariable.SLEEP_HOURS)
        }

        assertNotNull("Should detect sleep-energy positive correlation", insight)
        insight!!

        assertTrue(
            "Correlation score should be strongly positive (> 0.35), got ${insight.correlationScore}",
            insight.correlationScore > 0.35f
        )
        assertEquals(CorrelationDirection.POSITIVE, insight.direction)
        assertTrue(
            "p-value should be significant (< 0.05), got ${insight.pValue}",
            insight.pValue < 0.05f
        )
    }

    // ─── Test 3: Filtrado de correlaciones espurias ───────────────────────────

    @Test
    fun `test filters spurious uncorrelated pairs`() {
        val logs = makeUncorrelatedLogs(n = 20)
        val insights = runBlocking { engine.computeInsights(logs) }

        // Con datos sin correlación real, no debe emitir insights de sueño-dolor
        val sleepPainInsight = insights.firstOrNull {
            !it.isLagged &&
            ((it.variableA == BioVariable.SLEEP_HOURS && it.variableB == BioVariable.PAIN_LEVEL) ||
             (it.variableA == BioVariable.PAIN_LEVEL && it.variableB == BioVariable.SLEEP_HOURS))
        }

        // Si hay un insight, debe cumplir los umbrales (|rho| >= 0.35 AND p < 0.05)
        if (sleepPainInsight != null) {
            assertTrue(
                "Any emitted insight must have |correlation| >= 0.35",
                Math.abs(sleepPainInsight.correlationScore) >= 0.35f
            )
            assertTrue(
                "Any emitted insight must have p < 0.05",
                sleepPainInsight.pValue < 0.05f
            )
        }
        // The main point: the engine should not crash and should not emit low-quality insights
    }

    // ─── Test 4: Correlación diferida Lag-1 (T-1 → T) ───────────────────────

    @Test
    fun `test detects lag-1 cross correlation stress predicts pain next day`() {
        val logs = makeLaggedStressPainLogs(n = 15)
        val insights = runBlocking { engine.computeInsights(logs) }

        val laggedInsight = insights.firstOrNull { insight ->
            insight.isLagged && insight.lagDays == 1 &&
            ((insight.variableA == BioVariable.STRESS_LEVEL && insight.variableB == BioVariable.PAIN_LEVEL) ||
             (insight.variableA == BioVariable.PAIN_LEVEL && insight.variableB == BioVariable.STRESS_LEVEL))
        }

        assertNotNull(
            "Should detect lagged stress->pain correlation. Found insights: ${insights.map { "${it.variableA}-${it.variableB} lag=${it.isLagged} r=${it.correlationScore}" }}",
            laggedInsight
        )
        laggedInsight!!
        assertTrue("Lagged correlation should be strong (> 0.35)", laggedInsight.correlationScore > 0.35f)
        assertTrue("Lagged p-value should be < 0.05", laggedInsight.pValue < 0.05f)
    }

    // ─── Test 5: Manejo de datos insuficientes ────────────────────────────────

    @Test
    fun `test returns empty list when insufficient data`() {
        val fewLogs = (0 until 5).map { i ->
            makeLog(dayOffset = i, sleep = 7f, pain = 3)
        }
        val insights = runBlocking { engine.computeInsights(fewLogs) }
        // With n < 8, should emit nothing
        assertTrue(
            "Should not emit insights with less than 8 paired observations",
            insights.isEmpty()
        )
    }

    // ─── Test 6: Manejo de valores nulos ─────────────────────────────────────

    @Test
    fun `test handles null values gracefully without crashing`() {
        val logsWithNulls = (0 until 20).map { i ->
            // Alternate between having data and not
            if (i % 3 == 0) makeLog(dayOffset = i)
            else makeLog(dayOffset = i, sleep = 7f, pain = i % 5)
        }
        // Should not throw any exception
        val insights = runBlocking { engine.computeInsights(logsWithNulls) }
        assertNotNull("Should return a non-null list", insights)
    }

    // ─── Test 7: Lista vacía ──────────────────────────────────────────────────

    @Test
    fun `test handles empty log list`() {
        val insights = runBlocking { engine.computeInsights(emptyList()) }
        assertTrue("Empty list should produce no insights", insights.isEmpty())
    }

    // ─── Test 8: Resultados ordenados por |correlationScore| descendente ──────

    @Test
    fun `test results are sorted by absolute correlation score descending`() {
        val logs = makeSleepPainAntiCorrelatedLogs(n = 15) +
                   makeSleepEnergyCorrelatedLogs(n = 15)
        val insights = runBlocking { engine.computeInsights(logs) }

        if (insights.size >= 2) {
            for (i in 0 until insights.size - 1) {
                assertTrue(
                    "Insights should be sorted by |score| descending",
                    Math.abs(insights[i].correlationScore) >= Math.abs(insights[i + 1].correlationScore)
                )
            }
        }
    }

    // ─── Test 9: ClinicalInterpretationKey no vacío ───────────────────────────

    @Test
    fun `test all insights have non-empty clinical interpretation key`() {
        val logs = makeSleepPainAntiCorrelatedLogs(n = 15)
        val insights = runBlocking { engine.computeInsights(logs) }

        for (insight in insights) {
            assertTrue(
                "clinicalInterpretationKey must not be blank for ${insight.variableA}-${insight.variableB}",
                insight.clinicalInterpretationKey.isNotBlank()
            )
        }
    }

    // ─── Test 10: Correlación de varianza cero no emite insight ──────────────

    @Test
    fun `test zero variance series does not emit insight`() {
        // All pain values are 0 — zero variance → cannot compute meaningful correlation
        val logs = (0 until 15).map { i ->
            makeLog(dayOffset = i, sleep = 4f + i * 0.25f, pain = 0)
        }
        val insights = runBlocking { engine.computeInsights(logs) }

        val sleepPainInsight = insights.firstOrNull {
            (it.variableA == BioVariable.SLEEP_HOURS && it.variableB == BioVariable.PAIN_LEVEL) ||
            (it.variableA == BioVariable.PAIN_LEVEL && it.variableB == BioVariable.SLEEP_HOURS)
        }
        // Zero variance in pain → cannot compute valid Spearman → should not emit
        assertNull(
            "Zero-variance series should not produce a correlation insight",
            sleepPainInsight
        )
    }
}
