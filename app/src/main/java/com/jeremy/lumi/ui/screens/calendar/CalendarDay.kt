package com.jeremy.lumi.ui.screens.calendar

import com.jeremy.lumi.domain.model.CyclePhase

/**
 * Representa una celda del grid del calendario.
 *
 * [isPrediction] es true cuando el día es futuro y está dentro del ciclo activo —
 * significa que el color de fase es una estimación, no un dato real registrado.
 * La UI lo renderiza con opacidad reducida para diferenciarlo visualmente.
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
    val intercourseProtected: Boolean? = null
)