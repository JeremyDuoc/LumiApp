package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.domain.model.CyclePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CyclePredictorTest {

    private fun mockCycleEntity(start: LocalDate, end: LocalDate?, defaultCycleLength: Int = 28): CycleEntity {
        return CycleEntity(
            startDate = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            endDate = end?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            predictedOvulationDate = null,
            cycleLength = defaultCycleLength,
            periodLength = 5
        )
    }

    @Test
    fun `test predict basic normal cycle`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 3) // Day 3
        
        val prediction = CyclePredictor.predict(
            startDate = startDate,
            cycleLength = 28,
            periodLength = 5,
            today = today,
            closedCycles = emptyList(),
            isPregnant = false,
            isOnContraceptive = false
        )
        
        assertEquals(3, prediction.currentDayOfCycle)
        assertEquals(CyclePhase.MENSTRUAL, prediction.currentPhase)
        assertEquals(DelayState.ON_TIME, prediction.delayState)
        assertFalse(prediction.isLate)
    }

    @Test
    fun `test predict ovulation phase`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 14) // Day 14 of 28-day cycle (Ovulation)
        
        val prediction = CyclePredictor.predict(
            startDate = startDate,
            cycleLength = 28,
            periodLength = 5,
            today = today,
            closedCycles = emptyList(),
            isPregnant = false,
            isOnContraceptive = false
        )
        
        assertEquals(14, prediction.currentDayOfCycle)
        assertEquals(CyclePhase.OVULATION, prediction.currentPhase)
        assertFalse(prediction.isLate)
    }

    @Test
    fun `test predict luteal phase`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 20) // Day 20 of 28-day cycle
        
        val prediction = CyclePredictor.predict(
            startDate = startDate,
            cycleLength = 28,
            periodLength = 5,
            today = today,
            closedCycles = emptyList(),
            isPregnant = false,
            isOnContraceptive = false
        )
        
        assertEquals(20, prediction.currentDayOfCycle)
        assertEquals(CyclePhase.LUTEAL, prediction.currentPhase)
        assertFalse(prediction.isLate)
    }

    @Test
    fun `test predict late cycle`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 30) // Day 30 of 28-day cycle (2 days late)
        
        val prediction = CyclePredictor.predict(
            startDate = startDate,
            cycleLength = 28,
            periodLength = 5,
            today = today,
            closedCycles = emptyList(),
            isPregnant = false,
            isOnContraceptive = false
        )
        
        assertEquals(30, prediction.currentDayOfCycle)
        assertEquals(CyclePhase.LUTEAL, prediction.currentPhase) // Late is considered luteal
        assertEquals(DelayState.LATE, prediction.delayState)
        assertTrue(prediction.isLate)
        assertEquals(2, prediction.delayDays)
    }

    @Test
    fun `test contraceptive mode suppresses ovulation`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 14) // Day 14 (Normally ovulation)
        
        val prediction = CyclePredictor.predict(
            startDate = startDate,
            cycleLength = 28,
            periodLength = 5,
            today = today,
            closedCycles = emptyList(),
            isPregnant = false,
            isOnContraceptive = true // Pill mode
        )
        
        // With contraceptive, day 14 is FOLLICULAR, not OVULATION
        assertEquals(CyclePhase.FOLLICULAR, prediction.currentPhase)
        assertEquals(LocalDate.MAX, prediction.nextOvulationDate)
        assertEquals(-1, prediction.daysUntilOvulation)
    }

    @Test
    fun `test contraceptive mode ignores delays`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 31) // Day 31 (Normally 3 days late)
        
        val prediction = CyclePredictor.predict(
            startDate = startDate,
            cycleLength = 28,
            periodLength = 5,
            today = today,
            closedCycles = emptyList(),
            isPregnant = false,
            isOnContraceptive = true // Pill mode
        )
        
        // Contraceptive mode doesn't trigger delays
        assertEquals(0, prediction.delayDays)
        assertEquals(DelayState.ON_TIME, prediction.delayState)
        assertFalse(prediction.isLate)
    }

    @Test
    fun `test pregnancy mode overrides everything`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 14)
        
        val prediction = CyclePredictor.predict(
            startDate = startDate,
            cycleLength = 28,
            periodLength = 5,
            today = today,
            closedCycles = emptyList(),
            isPregnant = true,
            isOnContraceptive = false
        )
        
        assertEquals(CyclePhase.PREGNANCY, prediction.currentPhase)
        assertEquals(-1, prediction.daysUntilNextPeriod)
        assertEquals(LocalDate.MAX, prediction.nextPeriodDate)
        assertEquals(-1, prediction.daysUntilOvulation)
        assertEquals(DelayState.ON_TIME, prediction.delayState)
    }

    @Test
    fun `test historical exponential weighted average`() {
        // Create 3 closed cycles: 30, 32, 28 days
        val c1 = mockCycleEntity(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)) // 30 days
        val c2 = mockCycleEntity(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 4)) // 32 days
        val c3 = mockCycleEntity(LocalDate.of(2026, 3, 4), LocalDate.of(2026, 4, 1)) // 28 days
        
        val closedCycles = listOf(c3, c2, c1) // Ordered newest first
        
        val average = CyclePredictor.weightedAverageCycleLength(closedCycles)
        
        // It shouldn't be a simple average, it should be weighted
        assertTrue(average > 27f && average < 32f)
    }

    @Test
    fun `test historical outlier penalty`() {
        // Create 3 closed cycles: 28, 28, and a massive outlier 55 days
        val c1 = mockCycleEntity(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29)) // 28 days
        val c2 = mockCycleEntity(LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 26)) // 28 days
        val c3 = mockCycleEntity(LocalDate.of(2026, 2, 26), LocalDate.of(2026, 4, 22)) // 55 days (Outlier)
        
        val closedCycles = listOf(c3, c2, c1) // Ordered newest first
        
        val average = CyclePredictor.weightedAverageCycleLength(closedCycles)
        
        // The outlier (55) should be heavily penalized by MAD, so average remains close to 28
        assertTrue(average < 32f)
    }

    @Test
    fun `test clinical fertile window spans 6 days`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val cycleLength = 28
        val periodLength = 5
        // In 28-day cycle with 14-day luteal phase, ovulation is Day 14.
        // Fertile window: Day 9 (14-5) through Day 15 (14+1).

        // Day 8 -> Follicular
        val day8 = CyclePredictor.phaseForDay(8, cycleLength, periodLength)
        assertEquals(CyclePhase.FOLLICULAR, day8)

        // Day 9 -> Ovulation / Fertile window start
        val day9 = CyclePredictor.phaseForDay(9, cycleLength, periodLength)
        assertEquals(CyclePhase.OVULATION, day9)

        // Day 14 -> Ovulation peak
        val day14 = CyclePredictor.phaseForDay(14, cycleLength, periodLength)
        assertEquals(CyclePhase.OVULATION, day14)

        // Day 15 -> Last fertile day
        val day15 = CyclePredictor.phaseForDay(15, cycleLength, periodLength)
        assertEquals(CyclePhase.OVULATION, day15)

        // Day 16 -> Luteal phase
        val day16 = CyclePredictor.phaseForDay(16, cycleLength, periodLength)
        assertEquals(CyclePhase.LUTEAL, day16)
    }

    @Test
    fun `test predict includes fertile window and peak metadata`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 14) // Day 14 (Ovulation peak)

        val prediction = CyclePredictor.predict(
            startDate = startDate,
            cycleLength = 28,
            periodLength = 5,
            today = today
        )

        assertTrue(prediction.isInFertileWindow)
        assertTrue(prediction.isOvulationPeak)
        assertEquals(LocalDate.of(2026, 7, 9), prediction.fertileWindowStartDate)
        assertEquals(LocalDate.of(2026, 7, 15), prediction.fertileWindowEndDate)
    }

    @Test
    fun `test symptothermal biphasic BBT shift detection`() {
        // 6 pre-ovulatory low temperatures (max = 36.3°C coverline)
        // followed by 3 higher temperatures (36.5, 36.55, 36.6°C -> +0.3°C above coverline)
        val bbtReadings = listOf(
            Pair(7, 36.2f),
            Pair(8, 36.15f),
            Pair(9, 36.25f),
            Pair(10, 36.30f), // max low = 36.3
            Pair(11, 36.20f),
            Pair(12, 36.25f), // Day 12 is last low day
            Pair(13, 36.50f), // shift day 1
            Pair(14, 36.55f), // shift day 2
            Pair(15, 36.60f)  // shift day 3 (+0.3°C > 0.20°C threshold)
        )

        val detectedOvulationDay = CyclePredictor.detectBiphasicShift(bbtReadings)
        assertEquals(12, detectedOvulationDay)

        // Personal luteal length with observed shifts
        val observedLuteals = listOf(13, 13, 12)
        val lutealLength = CyclePredictor.personalLutealLength(emptyList(), observedLuteals)
        assertEquals(13, lutealLength)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PROBABILISTIC PREDICTION INTERVALS (Module D)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `test prediction includes earliest and latest dates forming valid interval`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 10)

        // 5 historical cycles with some natural variance
        val cycles = listOf(
            mockCycleEntity(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29)),  // 28 days
            mockCycleEntity(LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 27)), // 29 days
            mockCycleEntity(LocalDate.of(2026, 2, 27), LocalDate.of(2026, 3, 27)), // 28 days
            mockCycleEntity(LocalDate.of(2026, 3, 27), LocalDate.of(2026, 4, 24)), // 28 days
            mockCycleEntity(LocalDate.of(2026, 4, 24), LocalDate.of(2026, 5, 23)), // 29 days
        )

        val prediction = CyclePredictor.predict(
            startDate = startDate,
            cycleLength = 28,
            periodLength = 5,
            today = today,
            closedCycles = cycles
        )

        // earliestPeriodDate should be before or equal to expectedPeriodDate
        assertFalse(
            "Earliest period date should be before or equal to expected period date",
            prediction.earliestPeriodDate.isAfter(prediction.nextPeriodDate)
        )

        // latestPeriodDate should be after or equal to expectedPeriodDate
        assertFalse(
            "Latest period date should be after or equal to expected period date",
            prediction.latestPeriodDate.isBefore(prediction.nextPeriodDate)
        )

        // The interval should have positive width
        assertTrue(
            "Prediction interval should have positive width (earliest < latest)",
            prediction.earliestPeriodDate.isBefore(prediction.latestPeriodDate) ||
            prediction.earliestPeriodDate == prediction.latestPeriodDate
        )
    }

    @Test
    fun `test prediction confidence index is between 0 and 1`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 10)

        val cycles = listOf(
            mockCycleEntity(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29)),
            mockCycleEntity(LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 27)),
            mockCycleEntity(LocalDate.of(2026, 2, 27), LocalDate.of(2026, 3, 27)),
        )

        val prediction = CyclePredictor.predict(
            startDate = startDate,
            today = today,
            closedCycles = cycles
        )

        assertTrue(
            "Confidence index must be between 0.0 and 1.0, got ${prediction.predictionConfidenceIndex}",
            prediction.predictionConfidenceIndex in 0.0f..1.0f
        )
    }

    @Test
    fun `test higher confidence with more closed cycles`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 10)

        // Few cycles
        val fewCycles = listOf(
            mockCycleEntity(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 29)),
            mockCycleEntity(LocalDate.of(2026, 4, 29), LocalDate.of(2026, 5, 27)),
            mockCycleEntity(LocalDate.of(2026, 5, 27), LocalDate.of(2026, 6, 25)),
        )

        // Many regular cycles
        val manyCycles = mutableListOf<CycleEntity>()
        var date = LocalDate.of(2026, 1, 1)
        repeat(10) {
            val end = date.plusDays(28)
            manyCycles.add(mockCycleEntity(date, end))
            date = end
        }

        val predictionFew = CyclePredictor.predict(startDate, today = today, closedCycles = fewCycles)
        val predictionMany = CyclePredictor.predict(startDate, today = today, closedCycles = manyCycles)

        assertTrue(
            "More historical cycles should yield higher confidence: few=${predictionFew.predictionConfidenceIndex}, many=${predictionMany.predictionConfidenceIndex}",
            predictionMany.predictionConfidenceIndex >= predictionFew.predictionConfidenceIndex
        )
    }

    @Test
    fun `test narrower interval for very regular cycles`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 10)

        // Perfect regularity: all 28-day cycles
        val regularCycles = mutableListOf<CycleEntity>()
        var date = LocalDate.of(2026, 1, 1)
        repeat(6) {
            val end = date.plusDays(28)
            regularCycles.add(mockCycleEntity(date, end))
            date = end
        }

        // High variance cycles: 22, 35, 28, 40, 24, 33
        val irregularCycles = listOf(
            mockCycleEntity(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 23)),  // 22d
            mockCycleEntity(LocalDate.of(2026, 1, 23), LocalDate.of(2026, 2, 27)), // 35d
            mockCycleEntity(LocalDate.of(2026, 2, 27), LocalDate.of(2026, 3, 27)), // 28d
            mockCycleEntity(LocalDate.of(2026, 3, 27), LocalDate.of(2026, 5, 6)),  // 40d
            mockCycleEntity(LocalDate.of(2026, 5, 6), LocalDate.of(2026, 5, 30)),  // 24d
            mockCycleEntity(LocalDate.of(2026, 5, 30), LocalDate.of(2026, 7, 2)),  // 33d
        )

        val predRegular = CyclePredictor.predict(startDate, today = today, closedCycles = regularCycles)
        val predIrregular = CyclePredictor.predict(startDate, today = today, closedCycles = irregularCycles)

        val regularIntervalWidth = java.time.temporal.ChronoUnit.DAYS.between(
            predRegular.earliestPeriodDate, predRegular.latestPeriodDate
        )
        val irregularIntervalWidth = java.time.temporal.ChronoUnit.DAYS.between(
            predIrregular.earliestPeriodDate, predIrregular.latestPeriodDate
        )

        assertTrue(
            "Regular cycles should have a narrower or equal interval than irregular cycles. " +
            "Regular: $regularIntervalWidth days, Irregular: $irregularIntervalWidth days",
            regularIntervalWidth <= irregularIntervalWidth
        )
    }

    @Test
    fun `test weighted std dev returns positive value with multiple cycles`() {
        val cycles = listOf(
            mockCycleEntity(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29)),  // 28d
            mockCycleEntity(LocalDate.of(2026, 1, 29), LocalDate.of(2026, 3, 4)),  // 34d
            mockCycleEntity(LocalDate.of(2026, 3, 4), LocalDate.of(2026, 4, 1)),   // 28d
            mockCycleEntity(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 24)),  // 23d
        )

        val stdDev = CyclePredictor.weightedStdDevCycleLength(cycles)

        assertTrue(
            "Standard deviation should be positive with varying cycle lengths, got $stdDev",
            stdDev > 0f
        )
        // Should not be unreasonably large
        assertTrue(
            "Standard deviation should be < 20 days for cycles in range 23-34 days, got $stdDev",
            stdDev < 20f
        )
    }

    @Test
    fun `test weighted std dev returns small value for identical cycles`() {
        // All identical 28-day cycles
        val cycles = listOf(
            mockCycleEntity(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29)),
            mockCycleEntity(LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 26)),
            mockCycleEntity(LocalDate.of(2026, 2, 26), LocalDate.of(2026, 3, 26)),
            mockCycleEntity(LocalDate.of(2026, 3, 26), LocalDate.of(2026, 4, 23)),
        )

        val stdDev = CyclePredictor.weightedStdDevCycleLength(cycles)

        // Near-zero (should return minimum 1.0f as per spec)
        assertTrue(
            "Identical cycle std dev should be at minimum 1.0f, got $stdDev",
            stdDev >= 1.0f
        )
        assertTrue(
            "Identical cycles should have near-zero std dev (max 2.0f), got $stdDev",
            stdDev <= 2.0f
        )
    }

    @Test
    fun `test pregnancy mode returns zero confidence and MAX dates`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 10)

        val prediction = CyclePredictor.predict(
            startDate = startDate,
            today = today,
            isPregnant = true
        )

        assertEquals("Pregnancy: nextPeriodDate should be MAX", LocalDate.MAX, prediction.nextPeriodDate)
        assertEquals("Pregnancy: earliestPeriodDate should be MAX", LocalDate.MAX, prediction.earliestPeriodDate)
        assertEquals("Pregnancy: latestPeriodDate should be MAX", LocalDate.MAX, prediction.latestPeriodDate)
        assertEquals("Pregnancy: confidence should be 0f", 0f, prediction.predictionConfidenceIndex)
    }

    @Test
    fun `test prediction interval is bounded to reasonable clinical range`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 7, 10)

        // Extreme outlier cycle to stress-test clamping
        val extremeCycles = listOf(
            mockCycleEntity(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 2)),  // 60 days (max valid)
            mockCycleEntity(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 20)), // 18 days
            mockCycleEntity(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 4, 24)), // 35 days
        )

        val prediction = CyclePredictor.predict(
            startDate = startDate,
            today = today,
            closedCycles = extremeCycles
        )

        // Interval width should never exceed 28 days (2 * 14 days maximum)
        val intervalWidth = java.time.temporal.ChronoUnit.DAYS.between(
            prediction.earliestPeriodDate, prediction.latestPeriodDate
        )
        assertTrue(
            "Prediction interval should be <= 28 days even with extreme data, got $intervalWidth",
            intervalWidth <= 28
        )
    }
}

