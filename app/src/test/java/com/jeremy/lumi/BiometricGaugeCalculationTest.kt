package com.jeremy.lumi

import com.jeremy.lumi.domain.model.HistoricalCycleStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricGaugeCalculationTest {

    @Test
    fun testRegularityScore_highlyRegularCycle() {
        val stats = HistoricalCycleStats(
            cycleCount = 6,
            avgCycleLength = 28.0f,
            avgPeriodLength = 5.0f,
            shortestCycle = 27,
            longestCycle = 29, // variabilidad 2 días
            personalLutealLength = 14,
            maxHistoricalDelay = 1
        )

        val variability = (stats.longestCycle - stats.shortestCycle).coerceAtLeast(0)
        val regularityScore = ((1f - (variability.coerceIn(0, 16) / 20f)) * 100f).coerceIn(45f, 98f).toInt()

        // 2 / 20 = 0.1 -> 1 - 0.1 = 0.9 -> 90%
        assertEquals(90, regularityScore)
        assertTrue(regularityScore >= 85) // Alta regularidad
    }

    @Test
    fun testRegularityScore_variableCycle() {
        val stats = HistoricalCycleStats(
            cycleCount = 6,
            avgCycleLength = 31.0f,
            avgPeriodLength = 6.0f,
            shortestCycle = 24,
            longestCycle = 38, // variabilidad 14 días
            personalLutealLength = 11,
            maxHistoricalDelay = 7
        )

        val variability = (stats.longestCycle - stats.shortestCycle).coerceAtLeast(0)
        val regularityScore = ((1f - (variability.coerceIn(0, 16) / 20f)) * 100f).coerceIn(45f, 98f).toInt()

        // 14 / 20 = 0.7 -> 1 - 0.7 = 0.3 -> 30% -> clamped to 45%
        assertEquals(45, regularityScore)
        assertTrue(regularityScore < 70) // Variable
    }

    @Test
    fun testLutealPhase_optimalClassification() {
        val statsOptimal = HistoricalCycleStats(
            cycleCount = 4,
            avgCycleLength = 28.0f,
            avgPeriodLength = 5.0f,
            shortestCycle = 28,
            longestCycle = 28,
            personalLutealLength = 14,
            maxHistoricalDelay = 0
        )
        assertTrue(statsOptimal.personalLutealLength in 12..16)

        val statsShort = statsOptimal.copy(personalLutealLength = 9)
        assertTrue(statsShort.personalLutealLength < 10)
    }
}
