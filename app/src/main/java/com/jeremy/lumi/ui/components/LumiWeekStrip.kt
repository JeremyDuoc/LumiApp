package com.jeremy.lumi.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.screens.home.CycleDayUi
import com.jeremy.lumi.ui.theme.LumiMotion
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.ui.theme.lumiPressScale
import com.jeremy.lumi.utils.LumiClock
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Línea temporal semanal del ciclo (Rediseño Home Lumi).
 *
 * Características de diseño:
 * - Cabecera limpia: "Esta semana", rango de fechas visible, enlace discreto "Calendario ›".
 *   Sin flechas entre títulos.
 * - Swipe horizontal natural entre semanas con snap y spring.
 * - 7 días con fondo casi invisible en estado normal y micro indicador de fase.
 * - Día seleccionado: outline de color de fase/acento, fondo translúcido y spring espacial.
 * - Día actual vs seleccionado: conceptos visualmente diferenciados (Hoy mantiene micro indicador).
 * - Cero leyenda de fases en Home (la leyenda completa pertenece al Calendario).
 * - Información contextual del día seleccionado debajo de la fila con opción "Volver a hoy".
 */
@Composable
fun LumiWeekStrip(
    modifier: Modifier = Modifier,
    weekDays: List<CycleDayUi>,
    selectedDate: LocalDate?,
    currentPhase: CyclePhase,
    currentDayOfCycle: Int,
    onSelectDate: (LocalDate) -> Unit,
    onViewCalendarClick: () -> Unit,
    onResetToToday: (() -> Unit)? = null
) {
    val tokens = LumiThemeTokens.current
    val coroutineScope = rememberCoroutineScope()
    val today = remember { LumiClock.today() }
    val locale = Locale.getDefault()

    // Agrupamos en semanas de 7 días exactos
    val weeks = remember(weekDays) {
        if (weekDays.isEmpty()) emptyList()
        else weekDays.chunked(7).filter { it.size == 7 }
    }

    if (weeks.isEmpty()) return

    val todayWeekIndex = remember(weeks) {
        val idx = weeks.indexOfFirst { week -> week.any { it.isToday || it.date == today } }
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(
        initialPage = todayWeekIndex,
        pageCount = { weeks.size }
    )

    // Si la fecha seleccionada cambia hacia otra semana, animamos el scroll
    LaunchedEffect(selectedDate) {
        if (selectedDate != null) {
            val targetWeek = weeks.indexOfFirst { week -> week.any { it.date == selectedDate } }
            if (targetWeek >= 0 && targetWeek != pagerState.currentPage) {
                pagerState.animateScrollToPage(targetWeek)
            }
        }
    }

    val currentWeek = weeks.getOrNull(pagerState.currentPage) ?: emptyList()
    val isDateOtherThanToday = selectedDate != null && !selectedDate.isEqual(today)

    // Rango de la semana visible: "12 — 18 de octubre"
    val weekDateRange = remember(currentWeek, locale) {
        if (currentWeek.isEmpty()) ""
        else {
            val first = currentWeek.first().date
            val last = currentWeek.last().date
            formatWeekRangeText(first, last, locale)
        }
    }

    val dayCellShape = remember { RoundedCornerShape(14.dp) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // ── 1. Cabecera Limpia: "Esta semana" + Rango | "Calendario ›" ───────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.home_this_week_title),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    color = tokens.text
                )
                if (weekDateRange.isNotBlank()) {
                    Text(
                        text = weekDateRange,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = tokens.dim
                    )
                }
            }

            // Enlace discreto a Calendario completo con área táctil accesible de 48dp
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.home_calendar_action),
                        onClick = onViewCalendarClick
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_calendar_action),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = tokens.accent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── 2. Fila Semanal (HorizontalPager swipeable) ─────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { pageIndex ->
            val pageWeek = weeks.getOrNull(pageIndex) ?: emptyList()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pageWeek.forEach { day ->
                    val isDaySelected = selectedDate?.isEqual(day.date) == true || (selectedDate == null && (day.isToday || day.date == today))
                    val isDayToday = day.isToday || day.date == today

                    val phaseColor = when (day.phase) {
                        CyclePhase.MENSTRUAL -> tokens.rose
                        CyclePhase.FOLLICULAR -> tokens.gold
                        CyclePhase.OVULATION -> tokens.mint
                        CyclePhase.LUTEAL -> tokens.accent
                        else -> tokens.dim.copy(alpha = 0.35f)
                    }

                    val cellBg by animateColorAsState(
                        targetValue = when {
                            isDaySelected -> phaseColor.copy(alpha = 0.14f)
                            else -> Color.Transparent
                        },
                        animationSpec = LumiMotion.SpatialColorSpring,
                        label = "cell_bg"
                    )

                    val cellBorder by animateColorAsState(
                        targetValue = when {
                            isDaySelected -> phaseColor.copy(alpha = 0.75f)
                            else -> Color.Transparent
                        },
                        animationSpec = LumiMotion.SpatialColorSpring,
                        label = "cell_border"
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .lumiPressScale(interactionSource, 0.94f)
                            .clip(dayCellShape)
                            .background(cellBg)
                            .border(
                                width = if (isDaySelected) 1.5.dp else 0.dp,
                                color = cellBorder,
                                shape = dayCellShape
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = androidx.compose.foundation.LocalIndication.current
                            ) {
                                onSelectDate(day.date)
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            // Weekday en 2 caracteres ("Lu", "Ma", etc.)
                            val weekdayLabel = day.weekdayLabel.take(2).replaceFirstChar { it.uppercase() }
                            Text(
                                text = weekdayLabel,
                                fontSize = 10.5.sp,
                                fontWeight = if (isDaySelected || isDayToday) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isDayToday && !isDaySelected) tokens.accent else tokens.dim
                            )

                            // Número del día
                            Text(
                                text = "${day.dayOfMonth}",
                                fontSize = if (isDaySelected) 15.sp else 13.5.sp,
                                fontWeight = if (isDaySelected) FontWeight.Bold else if (isDayToday) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isDaySelected) tokens.text else if (isDayToday) tokens.accent else tokens.text
                            )

                            // Micro indicador inferior:
                            // Si es hoy y no está seleccionado, mostramos micro dot de hoy.
                            // Si no, mostramos el micro indicador de fase (3.5dp).
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(6.dp)
                            ) {
                                if (isDayToday && !isDaySelected) {
                                    // Micro indicador de hoy
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(tokens.accent)
                                    )
                                } else {
                                    // Micro indicador de fase geométrico discreto (3.5dp)
                                    MicroPhaseDot(phase = day.phase, color = phaseColor)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── 3. Información Contextual del Día Seleccionado ───────────────────────
        val effectiveDate = selectedDate ?: today
        val dateLabelFmt = remember(effectiveDate, locale) {
            val formatter = DateTimeFormatter.ofPattern("EEE d", locale)
            effectiveDate.format(formatter).replaceFirstChar { it.uppercase() }
        }

        val phaseDisplayName = when (currentPhase) {
            CyclePhase.MENSTRUAL -> stringResource(R.string.pm_phase_menstrual)
            CyclePhase.FOLLICULAR -> stringResource(R.string.pm_phase_follicular)
            CyclePhase.OVULATION -> stringResource(R.string.pm_phase_ovulation)
            CyclePhase.LUTEAL -> stringResource(R.string.pm_phase_luteal)
            CyclePhase.PREGNANCY -> stringResource(R.string.phase_pregnancy)
            CyclePhase.UNKNOWN -> stringResource(R.string.phase_unknown)
        }

        val contextualColor = when (currentPhase) {
            CyclePhase.MENSTRUAL -> tokens.rose
            CyclePhase.FOLLICULAR -> tokens.gold
            CyclePhase.OVULATION -> tokens.mint
            CyclePhase.LUTEAL -> tokens.accent
            else -> tokens.muted
        }

        val cycleDayText = if (currentDayOfCycle > 0) " · Día $currentDayOfCycle" else ""
        val contextInfoText = "$dateLabelFmt · $phaseDisplayName$cycleDayText"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = contextInfoText,
                transitionSpec = { fadeIn(LumiMotion.ShortFade) togetherWith fadeOut(LumiMotion.ShortFade) },
                label = "contextual_info"
            ) { info ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(contextualColor)
                    )
                    Text(
                        text = info,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = tokens.muted
                    )
                }
            }

            if (isDateOtherThanToday) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onResetToToday?.invoke()
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(todayWeekIndex)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.home_timeline_back_to_today),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.accent
                    )
                }
            }
        }
    }
}

/**
 * Micro indicador de fase geométrico minimalista (3.5dp).
 */
@Composable
private fun MicroPhaseDot(
    phase: CyclePhase,
    color: Color
) {
    Box(
        modifier = Modifier.size(4.dp),
        contentAlignment = Alignment.Center
    ) {
        when (phase) {
            CyclePhase.MENSTRUAL -> {
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            CyclePhase.OVULATION -> {
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .border(1.dp, color, CircleShape)
                )
            }
            CyclePhase.FOLLICULAR, CyclePhase.LUTEAL -> {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(2.5.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.4f))
                )
            }
        }
    }
}

/**
 * Formatea el rango de la semana: e.g. "12 — 18 de octubre" o "28 de sep — 4 de octubre".
 */
private fun formatWeekRangeText(first: LocalDate, last: LocalDate, locale: Locale): String {
    val isSpanish = locale.language == "es"
    return if (first.month == last.month) {
        val monthName = first.month.getDisplayName(TextStyle.FULL, locale)
        if (isSpanish) {
            "${first.dayOfMonth} — ${last.dayOfMonth} de ${monthName.lowercase()}"
        } else {
            "${monthName.replaceFirstChar { it.uppercase() }} ${first.dayOfMonth} — ${last.dayOfMonth}"
        }
    } else {
        val m1 = first.month.getDisplayName(TextStyle.SHORT, locale).lowercase().trimEnd('.')
        val m2 = last.month.getDisplayName(TextStyle.FULL, locale).lowercase()
        if (isSpanish) {
            "${first.dayOfMonth} de $m1 — ${last.dayOfMonth} de $m2"
        } else {
            "$m1 ${first.dayOfMonth} — $m2 ${last.dayOfMonth}"
        }
    }
}
