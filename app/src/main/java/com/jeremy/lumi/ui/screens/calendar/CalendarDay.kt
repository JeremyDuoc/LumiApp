package com.jeremy.lumi.ui.screens.calendar

import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import java.time.LocalDate

/**
 * Representa una celda del grid del calendario.
 *
 * [phase] siempre es calculada; [record] contiene lo que la usuaria registró.
 * [isPrediction] identifica una proyección futura y se dibuja con borde discontinuo.
 */
data class CalendarDay(
    val dayOfMonth: Int,
    val phase: CyclePhase = CyclePhase.UNKNOWN,
    val isToday: Boolean = false,
    val hasLog: Boolean = false,
    val isEmptyOffset: Boolean = false,
    val isPrediction: Boolean = false,
    /** true cuando el día es estrictamente futuro (después de hoy). */
    val isFuture: Boolean = false,
    // true solo si hubo relación sexual ese día Y la usuaria permitió mostrarlo en el calendario
    val showIntercourseHeart: Boolean = false,
    // null = no aplica / no mostrado. true = con protección, false = sin protección.
    val intercourseProtected: Boolean? = null,
    val date: LocalDate? = null,
    /** Recorded information is separate from the calculated phase. */
    val record: DailyLogWithSymptoms? = null
)

/** Keep the day when browsing months, including short months and leap years. */
internal fun dateInMonth(date: LocalDate, year: Int, month: Int): LocalDate =
    LocalDate.of(year, month, date.dayOfMonth.coerceAtMost(java.time.YearMonth.of(year, month).lengthOfMonth()))
