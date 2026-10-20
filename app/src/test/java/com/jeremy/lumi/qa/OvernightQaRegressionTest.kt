package com.jeremy.lumi.qa

import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.usecase.CyclePredictor
import com.jeremy.lumi.domain.usecase.DelayState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class OvernightQaRegressionTest {

    @Test
    fun `predict handles today as start date correctly`() {
        val today = LocalDate.of(2026, 10, 20)
        val prediction = CyclePredictor.predict(
            startDate = today,
            cycleLength = 28,
            periodLength = 5,
            today = today
        )
        assertEquals(1, prediction.currentDayOfCycle)
        assertEquals(CyclePhase.MENSTRUAL, prediction.currentPhase)
        assertEquals(DelayState.ON_TIME, prediction.delayState)
        assertEquals(0, prediction.delayDays)
        assertFalse(prediction.isLate)
    }

    @Test
    fun `predict handles extreme delay safely as extended luteal`() {
        val start = LocalDate.of(2026, 7, 1)
        val today = LocalDate.of(2026, 10, 20) // 111 days later (> 28 days)
        val prediction = CyclePredictor.predict(
            startDate = start,
            cycleLength = 28,
            periodLength = 5,
            today = today
        )
        assertEquals(112, prediction.currentDayOfCycle)
        assertEquals(CyclePhase.LUTEAL, prediction.currentPhase)
        assertEquals(DelayState.EXTREMELY_LATE, prediction.delayState)
        assertTrue(prediction.isLate)
        assertTrue(prediction.delayDays > 15)
    }

    @Test
    fun `predict on contraceptive forces ON_TIME and suppresses ovulation window`() {
        val start = LocalDate.of(2026, 9, 1)
        val today = LocalDate.of(2026, 10, 20)
        val prediction = CyclePredictor.predict(
            startDate = start,
            cycleLength = 28,
            periodLength = 5,
            today = today,
            isOnContraceptive = true
        )
        assertEquals(DelayState.ON_TIME, prediction.delayState)
        assertEquals(0, prediction.delayDays)
        assertFalse(prediction.isLate)
        assertEquals(LocalDate.MAX, prediction.nextOvulationDate)
        assertEquals(-1, prediction.daysUntilOvulation)
        assertFalse(prediction.isInFertileWindow)
    }

    @Test
    fun `predict during pregnancy returns pregnancy phase and infinite next period`() {
        val start = LocalDate.of(2026, 8, 1)
        val today = LocalDate.of(2026, 10, 20)
        val prediction = CyclePredictor.predict(
            startDate = start,
            cycleLength = 28,
            periodLength = 5,
            today = today,
            isPregnant = true
        )
        assertEquals(CyclePhase.PREGNANCY, prediction.currentPhase)
        assertEquals(LocalDate.MAX, prediction.nextPeriodDate)
        assertEquals(-1, prediction.daysUntilNextPeriod)
        assertFalse(prediction.isLate)
    }

    @Test
    fun `localized date formatter formats correctly in multiple supported languages`() {
        val date = LocalDate.of(2026, 10, 20)
        val esFmt = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale("es")))
        val enFmt = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.ENGLISH))
        val deFmt = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.GERMAN))
        val frFmt = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.FRENCH))

        assertTrue(esFmt.contains("octubre") || esFmt.contains("10"))
        assertTrue(enFmt.contains("October") || enFmt.contains("10"))
        assertTrue(deFmt.contains("Oktober") || deFmt.contains("10"))
        assertTrue(frFmt.contains("octobre") || frFmt.contains("10"))
    }
}
