package com.jeremy.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.screens.home.CycleDayUi
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.utils.LumiClock
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val SpanishLocale = Locale("es")

/**
 * Tira semanal continua de días aprobada en el prototipo interactivo Lumi.
 *
 * Características:
 * - Agrupada por semanas completas (Lunes a Domingo) con swipe horizontal natural.
 * - Cabecera con "Tu semana", mes/año visible, selector rápido "Hoy ↺" y enlace a calendario completo.
 * - Flechas sutiles ‹ y › para navegar de semana en semana.
 * - Visualización semántica clara:
 *     - "HOY": resalte distinguible para no perder el contexto temporal.
 *     - Fecha seleccionada: píldora suave con borde de acento.
 *     - Puntos indicadores por fase menstrual.
 * - Leyenda compacta de las 4 fases al pie.
 */
@Composable
fun LumiWeekStrip(
    modifier: Modifier = Modifier,
    weekDays: List<CycleDayUi>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onViewCalendarClick: () -> Unit,
    onResetToToday: (() -> Unit)? = null
) {
    val tokens = LumiThemeTokens.current
    val coroutineScope = rememberCoroutineScope()
    val today = LumiClock.today()

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
    val isViewingCurrentWeek = pagerState.currentPage == todayWeekIndex
    val isDateOtherThanTodaySelected = selectedDate != null && !selectedDate.isEqual(today)
    val showReturnToToday = !isViewingCurrentWeek || isDateOtherThanTodaySelected

    val dayShape = remember { RoundedCornerShape(16.dp) }

    val weekMonthLabel = remember(currentWeek) {
        if (currentWeek.isEmpty()) ""
        else {
            val firstDay = currentWeek.first().date
            val lastDay = currentWeek.last().date
            val locale = SpanishLocale
            if (firstDay.month == lastDay.month) {
                val m = firstDay.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase() }
                "$m ${firstDay.year}"
            } else {
                val m1 = firstDay.month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase() }
                val m2 = lastDay.month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase() }
                "$m1 - $m2 ${lastDay.year}"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Cabecera de sección
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tu semana",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = tokens.text
                )
                if (weekMonthLabel.isNotBlank()) {
                    Text(
                        text = weekMonthLabel,
                        fontSize = 11.sp,
                        color = tokens.dim
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showReturnToToday) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tokens.accentSoft)
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
                            text = "Hoy ↺",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.accent
                        )
                    }
                }

                // Flecha semana anterior
                IconButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Semana anterior",
                        tint = if (pagerState.currentPage > 0) tokens.text else tokens.dim.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Flecha semana siguiente
                IconButton(
                    onClick = {
                        if (pagerState.currentPage < weeks.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage < weeks.size - 1,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Semana siguiente",
                        tint = if (pagerState.currentPage < weeks.size - 1) tokens.text else tokens.dim.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Enlace al calendario completo
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onViewCalendarClick)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ver calendario ↗",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.accent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pager de semanas horizontales
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { pageIndex ->
            val pageWeek = weeks.getOrNull(pageIndex) ?: emptyList()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                pageWeek.forEach { day ->
                    val isDaySelected = selectedDate?.isEqual(day.date) == true || (selectedDate == null && (day.isToday || day.date == today))
                    val isDayToday = day.isToday || day.date == today
                    val dotColor = when (day.phase) {
                        CyclePhase.MENSTRUAL -> tokens.rose
                        CyclePhase.FOLLICULAR -> tokens.gold
                        CyclePhase.OVULATION -> tokens.mint
                        CyclePhase.LUTEAL -> tokens.accent
                        else -> tokens.accent.copy(alpha = 0.4f)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(dayShape)
                            .background(if (isDaySelected) tokens.accentSoft else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = when {
                                    isDaySelected -> tokens.accent.copy(alpha = 0.65f)
                                    isDayToday -> tokens.accent.copy(alpha = 0.35f)
                                    else -> Color.Transparent
                                },
                                shape = dayShape
                            )
                            .clickable { onSelectDate(day.date) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Nombre del día (Lun, Mar...)
                            Text(
                                text = day.weekdayLabel.take(3),
                                fontSize = 10.sp,
                                fontWeight = if (isDayToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDayToday) tokens.accent else tokens.dim
                            )

                            // Número del día
                            Text(
                                text = "${day.dayOfMonth}",
                                fontSize = 13.5.sp,
                                fontWeight = if (isDayToday || isDaySelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isDayToday) tokens.accent else tokens.text
                            )

                            // Punto indicador de fase menstrual
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Leyenda de fases
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhaseLegendItem(label = "Menstrual", color = tokens.rose)
            Spacer(modifier = Modifier.size(12.dp))
            PhaseLegendItem(label = "Folicular", color = tokens.gold)
            Spacer(modifier = Modifier.size(12.dp))
            PhaseLegendItem(label = "Ovulación", color = tokens.mint)
            Spacer(modifier = Modifier.size(12.dp))
            PhaseLegendItem(label = "Lútea", color = tokens.accent)
        }
    }
}

@Composable
private fun PhaseLegendItem(label: String, color: Color) {
    val tokens = LumiThemeTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 9.5.sp,
            color = tokens.dim
        )
    }
}
