package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.closedLengthDays
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.CycleSnapshot
import com.jeremy.lumi.domain.model.SharePreferences
import com.jeremy.lumi.domain.model.filteredBy
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class AuditRegressionTest {
    private val start = LocalDate.of(2026, 9, 1)
    private fun cycle(days: Int) = CycleEntity(
        startDate = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        endDate = start.plusDays(days - 1L).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        predictedOvulationDate = null, cycleLength = days
    )

    @Test fun `closed cycle includes its last day`() {
        assertEquals(28, cycle(28).closedLengthDays())
        assertEquals(15f, CyclePredictor.weightedAverageCycleLength(List(3) { cycle(15) }), 0.001f)
        assertEquals(60f, CyclePredictor.weightedAverageCycleLength(List(3) { cycle(60) }), 0.001f)
        assertEquals(28f, CyclePredictor.weightedAverageCycleLength(List(3) { cycle(28) }), 0.001f)
        assertNull(cycle(28).copy(endDate = null).closedLengthDays())
    }

    @Test fun `a delayed projection does not double the number of menstrual days`() {
        val today = start.plusDays(35)
        val phases = (1..28).map {
            CyclePredictor.phaseForDateInActiveCycle(start, 28, 5, today.plusDays(it.toLong()), today).first
        }
        assertEquals(5, phases.count { it == CyclePhase.MENSTRUAL })
        assertEquals(CyclePhase.FOLLICULAR, phases[5])
        assertEquals(CyclePhase.LUTEAL, phases.last())
    }

    @Test fun `link consent removes all health fields in a snapshot`() {
        val original = CycleSnapshot(CyclePhase.LUTEAL, 5, true, 3, "Sensitive", listOf("Pain"), 123L, 28, 5, 99L)
        val filtered = original.filteredBy(SharePreferences(false, false, false, false))
        assertEquals(CycleSnapshot(lastUpdatedAt = 99L), filtered)
        assertEquals(original, original.filteredBy(SharePreferences(true, true, true, true)))
        assertEquals(original.topSymptoms, original.filteredBy(SharePreferences(symptoms = true)).topSymptoms)
        assertTrue(original.filteredBy(SharePreferences()).topSymptoms.isEmpty())
    }
}
