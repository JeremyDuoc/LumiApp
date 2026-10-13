package com.jeremy.lumi

import com.jeremy.lumi.ui.screens.home.PredictionFactorsUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionFactorsCalculationTest {

    @Test
    fun testPredictionFactors_highConfidence_regular() {
        val stdDev = 1.2f
        val closedCycleCount = 4
        val daysWithSymptoms = 14
        val currentDay = 18

        val regScore = ((1f - (stdDev.coerceIn(0.5f, 6.0f) / 7.5f)) * 100f).coerceIn(45f, 98f).toInt()
        val histScore = when (closedCycleCount) {
            0    -> 35
            1    -> 55
            2    -> 72
            3    -> 85
            else -> 96
        }
        val sympScore = ((daysWithSymptoms.toFloat() / currentDay.coerceAtLeast(1)) * 100f).coerceIn(25f, 95f).toInt()

        val factors = PredictionFactorsUi(
            regularityScore = regScore,
            historyScore = histScore,
            symptomsScore = sympScore,
            cycleCount = closedCycleCount,
            confidenceLabel = "Certeza alta",
            marginDaysText = "±1.5 d"
        )

        assertTrue(factors.regularityScore >= 80)
        assertEquals(96, factors.historyScore)
        assertTrue(factors.symptomsScore in 25..95)
        assertEquals(4, factors.cycleCount)
        assertEquals("Certeza alta", factors.confidenceLabel)
    }

    @Test
    fun testPredictionFactors_firstCycle_learning() {
        val stdDev = 0f
        val closedCycleCount = 0
        val daysWithSymptoms = 2
        val currentDay = 5

        val regScore = if (closedCycleCount == 0) 75 else 50
        val histScore = when (closedCycleCount) {
            0    -> 35
            1    -> 55
            2    -> 72
            3    -> 85
            else -> 96
        }
        val sympScore = ((daysWithSymptoms.toFloat() / currentDay.coerceAtLeast(1)) * 100f).coerceIn(25f, 95f).toInt()

        val factors = PredictionFactorsUi(
            regularityScore = regScore,
            historyScore = histScore,
            symptomsScore = sympScore,
            cycleCount = closedCycleCount,
            confidenceLabel = "En calibración",
            marginDaysText = "±1.5 d"
        )

        assertEquals(75, factors.regularityScore)
        assertEquals(35, factors.historyScore)
        assertEquals(40, factors.symptomsScore)
        assertEquals("En calibración", factors.confidenceLabel)
    }
}
