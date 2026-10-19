package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.CycleEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class LumiV2PredictionTest {
    private fun stamp(day: LocalDate) = day.atStartOfDay(ZoneOffset.UTC)
        .toInstant().toEpochMilli()

    private fun closed(start: String, durationDays: Long) = LocalDate.parse(start).let { startDay ->
        CycleEntity(
            startDate = stamp(startDay),
            endDate = stamp(startDay.plusDays(durationDays - 1)),
            predictedOvulationDate = null,
            cycleLength = durationDays.toInt(),
            periodLength = 5
        )
    }

    @Test fun historicalFeaturesNeverUseFutureOrDuplicateCycles() {
        val currentStart = stamp(LocalDate.of(2026, 9, 1))
        val valid = closed("2026-08-01", 28)
        val duplicate = valid.copy(id = 99)
        val previous = closed("2026-07-01", 29)
        val future = closed("2026-10-01", 30)
        val outlier = closed("2026-05-01", 100)
        val values = CycleHistoryV2.validCycles(
            listOf(future, duplicate, outlier, previous, valid), currentStart
        )
        assertEquals(2, values.size)
        assertEquals(valid.startDate, values.first().startDate)
        assertTrue(values.all { it.endDate!! < currentStart })
    }

    @Test fun modelCalibratedWindowIsApproximateAndNonzero() {
        val start = LocalDate.of(2026, 9, 1)
        val history = listOf(
            closed("2026-08-01", 28),
            closed("2026-07-01", 29),
            closed("2026-06-01", 27),
            closed("2026-05-01", 30)
        )
        val result = CyclePredictor.predict(
            startDate = start, cycleLength = 28, periodLength = 5,
            today = start.plusDays(8), closedCycles = history
        )
        assertTrue(result.earliestPeriodDate.isBefore(result.nextPeriodDate))
        assertTrue(result.latestPeriodDate.isAfter(result.nextPeriodDate))
        assertTrue(result.predictionConfidenceIndex <= 0.60f)
        assertTrue(result.fertilityIsEstimated)
    }

    @Test fun missingHistoryWidensApproximateDateRange() {
        val start = LocalDate.of(2026, 9, 1)
        val result = CyclePredictor.predict(
            startDate = start, cycleLength = 28, periodLength = 5,
            today = start.plusDays(2)
        )
        assertEquals(start.plusDays(21), result.earliestPeriodDate)
        assertEquals(start.plusDays(35), result.latestPeriodDate)
    }

    @Test fun pregnantUserHasNoProjectedFertileCalendar() {
        val start = LocalDate.of(2026, 9, 1)
        val result = CyclePredictor.predict(
            startDate = start, today = start.plusDays(10), isPregnant = true
        )
        assertEquals(LocalDate.MAX, result.nextPeriodDate)
        assertEquals(LocalDate.MAX, result.fertileWindowStartDate)
        assertFalse(result.isOvulationPeak)
    }
}
