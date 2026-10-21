package com.jeremy.lumi.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import androidx.compose.ui.res.stringResource
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.screens.home.CycleDayUi
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import com.jeremy.lumi.ui.theme.LumiEditorialSerif
import com.jeremy.lumi.ui.theme.LumiMotion
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.utils.LumiClock
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Semana viva de Lumi, alineada con la grilla mensual.
 *
 * No usa tarjetas Material genéricas ni pulsos perpetuos: el tacto, la selección,
 * el paginado y la información contextual hacen el trabajo de la animación.
 * La semana sigue siendo independiente del registro (no abre una hoja al tocar).
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
    val colors = LocalPhaseColors.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val today = remember(weekDays) { LumiClock.today() }
    val weeks = remember(weekDays) { weekDays.chunked(7).filter { it.size == 7 } }
    if (weeks.isEmpty()) return

    val todayIndex = remember(weeks, today) {
        weeks.indexOfFirst { week -> week.any { it.date == today } }.coerceAtLeast(0)
    }
    val pager = rememberPagerState(initialPage = todayIndex, pageCount = { weeks.size })
    LaunchedEffect(selectedDate, weeks) {
        val target = selectedDate?.let { date ->
            weeks.indexOfFirst { week -> week.any { it.date == date } }
        } ?: -1
        if (target >= 0 && pager.currentPage != target) pager.animateScrollToPage(target)
    }
    val visibleWeek = weeks.getOrNull(pager.currentPage).orEmpty()
    val locale = Locale.getDefault()
    val range = remember(visibleWeek, locale) {
        if (visibleWeek.isEmpty()) "" else {
            val fmt = DateTimeFormatter.ofPattern("d MMM", locale)
            visibleWeek.first().date.format(fmt) + " – " + visibleWeek.last().date.format(fmt)
        }
    }
    val selected = selectedDate ?: today
    val selectedModel = weekDays.firstOrNull { it.date == selected }
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.lumi_cal_week_title),
                    fontFamily = LumiEditorialSerif,
                    fontSize = 24.sp,
                    color = tokens.text,
                    lineHeight = 27.sp
                )
                Spacer(Modifier.height(3.dp))
                AnimatedContent(
                    targetState = range,
                    transitionSpec = { fadeIn(tween(230)) togetherWith fadeOut(tween(120)) },
                    label = "week_date_range"
                ) { dates ->
                    Text(dates, fontSize = 11.sp, color = tokens.muted)
                }
            }
            Row(
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onViewCalendarClick)
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.lumi_cal_view_month), fontSize = 12.sp, color = tokens.accent,
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 8.dp
        ) { index ->
            val week = weeks[index]
            val selectedIndex = week.indexOfFirst { it.date == selected }
            val markerIndex by animateFloatAsState(
                targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
                animationSpec = LumiMotion.SpatialSpring,
                label = "lumi_week_selection_travel"
            )
            BoxWithConstraints(Modifier.fillMaxWidth().height(83.dp)) {
                val dayWidth = (maxWidth - 24.dp) / 7
                if (selectedIndex >= 0) {
                    Box(
                        Modifier
                            .offset(x = (dayWidth + 4.dp) * markerIndex)
                            .width(dayWidth)
                            .height(83.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(tokens.accentSoft)
                            .border(
                                width = 1.dp,
                                color = tokens.accent.copy(alpha = .82f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { day ->
                    val active = day.date == selected
                    val periodDay = day.phase == CyclePhase.MENSTRUAL
                    val phaseColor = when (day.phase) {
                        CyclePhase.MENSTRUAL -> colors.menstrual
                        CyclePhase.FOLLICULAR -> colors.follicular
                        CyclePhase.OVULATION -> colors.ovulation
                        CyclePhase.LUTEAL -> colors.luteal
                        else -> tokens.muted
                    }
                    val interaction = remember(day.date) { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        if (pressed) .91f else if (active) 1.035f else 1f,
                        animationSpec = LumiMotion.SpatialSpring,
                        label = "mini_day_press"
                    )
                    val fill by animateColorAsState(
                        when {
                            active -> Color.Transparent
                            periodDay -> phaseColor.copy(alpha = if (day.isPrediction) .07f else .16f)
                            else -> Color.Transparent
                        },
                        animationSpec = tween(260),
                        label = "mini_day_fill"
                    )
                    // La selección viaja como una pieza única, sin bordes duplicados.
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(83.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(shape)
                            .background(fill)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Button
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectDate(day.date)
                            }
                            .semantics {
                                contentDescription = day.date.toString() +
                                    if (day.isToday) ", hoy" else ""
                            }
                            .padding(top = 10.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (day.isToday) stringResource(R.string.cal_today_cd).uppercase(locale)
                            else day.weekdayLabel.take(3).replaceFirstChar { it.uppercase(locale) },
                            fontSize = 10.sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (day.isToday) tokens.accent else if (active) tokens.text else tokens.muted,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier.size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        periodDay && !day.isPrediction -> phaseColor.copy(alpha = .90f)
                                        periodDay && day.isPrediction -> phaseColor.copy(alpha = .16f)
                                        else -> Color.Transparent
                                    }
                                )
                                .then(if (periodDay && day.isPrediction) Modifier.border(
                                    1.dp, phaseColor.copy(alpha = .50f), CircleShape
                                ) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day.dayOfMonth.toString(),
                                fontSize = 16.sp,
                                fontWeight = if (active || day.isToday || periodDay)
                                    FontWeight.SemiBold else FontWeight.Normal,
                                color = if (periodDay && !day.isPrediction) tokens.bg
                                    else tokens.text.copy(alpha = if (day.isPrediction && !active) .77f else 1f),
                                textAlign = TextAlign.Center
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (day.phase != CyclePhase.UNKNOWN) {
                                Box(Modifier.size(4.dp).clip(CircleShape)
                                    .background(phaseColor.copy(alpha = if (day.isPrediction) .56f else .9f)))
                            }
                            if (day.hasLog) {
                                Box(Modifier.size(5.dp).clip(CircleShape).background(tokens.accent))
                            }
                        }
                    }
                }
            }
            }
        }

        Spacer(Modifier.height(15.dp))
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(5.dp).clip(CircleShape)
                .background(if (selectedModel?.hasLog == true) tokens.accent else tokens.dim))
            Spacer(Modifier.width(9.dp))
            AnimatedContent(
                targetState = selectedModel,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(140)) },
                modifier = Modifier.weight(1f),
                label = "mini_selected_context"
            ) { previewDay ->
                // Fecha y contexto del día seleccionado, sin inventar datos.
                val dateName = if (previewDay == null || previewDay.date == today)
                    stringResource(R.string.cal_today_cd)
                    else previewDay.date.format(DateTimeFormatter.ofPattern("d MMM", locale))
                val phase = when (previewDay?.phase) {
                    CyclePhase.MENSTRUAL -> stringResource(R.string.pm_phase_menstrual)
                    CyclePhase.FOLLICULAR -> stringResource(R.string.pm_phase_follicular)
                    CyclePhase.OVULATION -> stringResource(R.string.pm_phase_ovulation)
                    CyclePhase.LUTEAL -> stringResource(R.string.pm_phase_luteal)
                    else -> stringResource(R.string.lumi_cal_week_no_cycle)
                }
                val qualifier = if (previewDay?.isPrediction == true)
                    stringResource(R.string.lumi_cal_estimate_phase, phase)
                    else phase
                val logLabel = if (previewDay?.hasLog == true)
                    " · " + stringResource(R.string.cal_accessibility_with_log)
                    else ""
                Text(
                    text = "$dateName · $qualifier$logLabel",
                    fontSize = 11.sp, color = tokens.muted, lineHeight = 16.sp,
                    maxLines = 2
                )
            }
            if (selectedDate != null && selectedDate != today) {
                Text(
                    text = stringResource(R.string.home_timeline_back_to_today),
                    color = tokens.accent,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onResetToToday()
                            coroutineScope.launch { pager.animateScrollToPage(todayIndex) }
                        }
                        .padding(horizontal = 6.dp, vertical = 9.dp)
                )
            }
        }
    }
}
