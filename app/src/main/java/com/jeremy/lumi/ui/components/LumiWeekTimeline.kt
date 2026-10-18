package com.jeremy.lumi.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.screens.home.CycleDayUi
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import com.jeremy.lumi.ui.theme.LumiMotion
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.utils.LumiClock
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * LumiWeekTimeline — Línea temporal semanal integrada al fondo (sin tarjeta pesada).
 *
 * Características:
 * - Header limpio: fecha de rango en mayúsculas ("12 — 18 OCTUBRE") y enlace discreto "Calendario ›".
 * - 7 días en fila distribuidos de borde a borde con touch targets accesibles de 48dp.
 * - Marcador móvil vertical compartido que viaja horizontalmente con SpatialSpring entre días.
 * - Estado "Hoy" vs "Seleccionado": si se selecciona otro día, Hoy mantiene un micro punto acento (2.5dp).
 * - Indicador contextual debajo ("Viendo 15 oct ... Volver a hoy") con animación fade+translationY
 *   que desaparece al volver a hoy.
 * - Sin resumen duplicado (el aro inmediatamente debajo muestra la fase y el día del ciclo).
 */
@Composable
fun LumiWeekTimeline(
    weekDays: List<CycleDayUi>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onViewCalendarClick: () -> Unit,
    onResetToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiThemeTokens.current
    val phaseColors = LocalPhaseColors.current
    val coroutineScope = rememberCoroutineScope()
    val today = remember { LumiClock.today() }
    val locale = Locale.getDefault()

    // Agrupamos en semanas completas de 7 días exactos (Lunes a Domingo)
    val weeks = remember(weekDays) {
        if (weekDays.isEmpty()) emptyList()
        else weekDays.chunked(7).filter { it.size == 7 }
    }

    if (weeks.isEmpty()) return

    val todayWeekIndex = remember(weeks, today) {
        val idx = weeks.indexOfFirst { week -> week.any { it.isToday || it.date == today } }
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(
        initialPage = todayWeekIndex,
        pageCount = { weeks.size }
    )

    // Si la fecha seleccionada cambia hacia otra semana, animamos el pager
    LaunchedEffect(selectedDate) {
        if (selectedDate != null) {
            val targetWeek = weeks.indexOfFirst { week -> week.any { it.date == selectedDate } }
            if (targetWeek >= 0 && targetWeek != pagerState.currentPage) {
                pagerState.animateScrollToPage(targetWeek)
            }
        }
    }

    val currentWeek = weeks.getOrNull(pagerState.currentPage) ?: emptyList()

    // Rango de fechas para el header: "12 — 18 OCTUBRE"
    val headerDateRange = remember(currentWeek, locale) {
        if (currentWeek.isEmpty()) ""
        else {
            val first = currentWeek.first().date
            val last = currentWeek.last().date
            formatTimelineRange(first, last, locale)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // ── 1. HEADER DEL TIMELINE ───────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Izquierda: "12 — 18 OCTUBRE"
            Text(
                text = headerDateRange,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.7.sp,
                color = tokens.muted
            )

            // Derecha: "Calendario ›" (sin pill ni botón pesado, min 48dp touch target)
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.home_calendar_action),
                        onClick = onViewCalendarClick
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = stringResource(R.string.home_calendar_action),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = tokens.accent
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ── 2. SIETE DÍAS (Paginador swipeable con Marcador Móvil) ────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) { pageIndex ->
            val pageWeek = weeks.getOrNull(pageIndex) ?: emptyList()

            // Identificar día seleccionado en la página actual
            val effectiveSelectedDate = selectedDate ?: today
            val selectedIndexInWeek = pageWeek.indexOfFirst { it.date == effectiveSelectedDate }
            val hasSelectionInWeek = selectedIndexInWeek >= 0

            val selectedPhase = if (hasSelectionInWeek) pageWeek[selectedIndexInWeek].phase else CyclePhase.UNKNOWN
            val selectedPhaseColor = phaseColorFor(selectedPhase, tokens, phaseColors)

            // Animación continua del marcador horizontal mediante SpatialSpring
            val targetXIndex = if (hasSelectionInWeek) selectedIndexInWeek.toFloat() else 0f
            val animatedIndex by animateFloatAsState(
                targetValue = targetXIndex,
                animationSpec = LumiMotion.SpatialSpring,
                label = "timeline_indicator_x"
            )
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (hasSelectionInWeek) 1f else 0f,
                animationSpec = LumiMotion.SpatialSpring,
                label = "timeline_indicator_alpha"
            )

            val columnWidth = 40.dp
            val markerHeight = 62.dp

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val totalWidth = maxWidth
                val density = LocalDensity.current
                val spacing = if (pageWeek.size > 1) {
                    (totalWidth - (columnWidth * pageWeek.size)) / (pageWeek.size - 1)
                } else 0.dp

                // MARCADOR VERTICAL MÓVIL (un solo objeto físico que se desliza)
                if (hasSelectionInWeek || indicatorAlpha > 0.01f) {
                    val indicatorOffset = (columnWidth + spacing) * animatedIndex

                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(columnWidth)
                            .height(markerHeight)
                            .graphicsLayer { alpha = indicatorAlpha }
                            .clip(RoundedCornerShape(18.dp))
                            .background(selectedPhaseColor.copy(alpha = 0.11f))
                            .border(1.dp, selectedPhaseColor.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                            .drawBehind {
                                // Inner highlight superior sutil (White alpha 0.05)
                                drawLine(
                                    color = Color.White.copy(alpha = 0.05f),
                                    start = Offset(10.dp.toPx(), 1f),
                                    end = Offset(size.width - 10.dp.toPx(), 1f),
                                    strokeWidth = 1f
                                )
                                // Halo radial suave detrás del número
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            selectedPhaseColor.copy(alpha = 0.16f),
                                            Color.Transparent
                                        )
                                    ),
                                    center = Offset(size.width / 2f, size.height * 0.44f),
                                    radius = size.width * 0.48f
                                )
                            }
                    )
                }

                // FILA DE LOS SIETE DÍAS (Colocados sobre el marcador)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pageWeek.forEachIndexed { index, day ->
                        val isDaySelected = day.date == effectiveSelectedDate
                        val isDayToday = day.date == today
                        val dayPhaseColor = phaseColorFor(day.phase, tokens, phaseColors)

                        val interactionSource = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .width(columnWidth)
                                .height(markerHeight)
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Button
                                ) {
                                    onSelectDate(day.date)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 5.dp)
                            ) {
                                // Weekday: 10sp Medium muted
                                val weekdayText = day.weekdayLabel.take(3).replaceFirstChar { it.uppercase() }
                                Text(
                                    text = weekdayText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDaySelected) tokens.text else tokens.muted
                                )

                                // Número del día + micro indicador de HOY (si está seleccionado otro día)
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${day.dayOfMonth}",
                                        fontSize = if (isDaySelected) 15.sp else 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDaySelected) tokens.text else if (isDayToday) tokens.accent else tokens.text
                                    )

                                    // Si hoy no es el día seleccionado, muestra el micro punto acento de 2.5dp
                                    if (isDayToday && !isDaySelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .offset(y = 10.dp)
                                                .size(2.5.dp)
                                                .clip(CircleShape)
                                                .background(tokens.accent)
                                        )
                                    }
                                }

                                // Indicador de fase: punto sutil de 4.5dp
                                Box(
                                    modifier = Modifier
                                        .size(4.5.dp)
                                        .clip(CircleShape)
                                        .background(dayPhaseColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 3. FECHA DISTINTA DE HOY (ÚNICAMENTE cuando selectedDate != null) ─
        val isDifferentDaySelected = selectedDate != null && !selectedDate.isEqual(today)

        AnimatedVisibility(
            visible = isDifferentDaySelected,
            enter = fadeIn(tween(260, easing = LumiMotion.EaseOutExpo)) +
                    slideInVertically(tween(260, easing = LumiMotion.EaseOutExpo)) { 8 },
            exit = fadeOut(tween(180, easing = LumiMotion.EaseOutExpo)) +
                    slideOutVertically(tween(180, easing = LumiMotion.EaseOutExpo)) { 8 }
        ) {
            val viewingText = remember(selectedDate, locale) {
                selectedDate?.let {
                    val dFmt = DateTimeFormatter.ofPattern("d MMM", locale)
                    it.format(dFmt)
                } ?: ""
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_timeline_viewing_date, viewingText),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = tokens.muted
                )

                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                onResetToToday()
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(todayWeekIndex)
                                }
                            }
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = stringResource(R.string.home_timeline_back_to_today),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = tokens.accent
                    )
                }
            }
        }
    }
}

private fun formatTimelineRange(first: LocalDate, last: LocalDate, locale: Locale): String {
    val monthName = last.month.getDisplayName(TextStyle.FULL, locale).uppercase()
    return if (first.month == last.month) {
        "${first.dayOfMonth} — ${last.dayOfMonth} $monthName"
    } else {
        val firstMonth = first.month.getDisplayName(TextStyle.SHORT, locale).uppercase()
        "${first.dayOfMonth} $firstMonth — ${last.dayOfMonth} $monthName"
    }
}

private fun phaseColorFor(
    phase: CyclePhase,
    tokens: com.jeremy.lumi.ui.theme.LumiDesignTokens,
    phaseColors: com.jeremy.lumi.ui.theme.PhaseColors
): Color = when (phase) {
    CyclePhase.MENSTRUAL -> phaseColors.menstrual
    CyclePhase.FOLLICULAR -> phaseColors.follicular
    CyclePhase.OVULATION -> phaseColors.ovulation
    CyclePhase.LUTEAL -> phaseColors.luteal
    CyclePhase.PREGNANCY -> tokens.accent
    CyclePhase.UNKNOWN -> tokens.muted.copy(alpha = 0.5f)
}
