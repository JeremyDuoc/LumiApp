package com.jeremy.lumi.ui.screens.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CalendarDateNavigationTest {
    @Test fun january31ClampsToFebruaryWithoutSkippingMonth() {
        assertEquals(LocalDate.of(2026, 2, 28), dateInMonth(LocalDate.of(2026, 1, 31), 2026, 2))
    }
    @Test fun leapYearKeepsFebruary29Available() {
        assertEquals(LocalDate.of(2028, 2, 29), dateInMonth(LocalDate.of(2028, 1, 31), 2028, 2))
    }
    @Test fun yearBoundaryKeepsTheSelectedDay() {
        assertEquals(LocalDate.of(2027, 1, 20), dateInMonth(LocalDate.of(2026, 12, 20), 2027, 1))
    }
    @Test fun leavingLeapYearClampsTheDay() {
        assertEquals(LocalDate.of(2027, 2, 28), dateInMonth(LocalDate.of(2028, 2, 29), 2027, 2))
    }
}
