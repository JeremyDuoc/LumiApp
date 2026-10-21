package com.jeremy.lumi.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.screens.home.CycleDayUi
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import com.jeremy.lumi.ui.theme.LumiMotion
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.utils.LumiClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Mini calendario del Home: números redondos, como Flo/Structured.
 * - Rosa sólido: periodo inicial calculado a partir del ciclo guardado.
 * - Rosa tenue: proyección futura (NO confirmación).
 * - Menta tenue: ventana fértil estimada, NUNCA ovulación detectada.
 * - ✓: se guardó un registro ese día. Sin registro no hay punto decorativo.
 * - HOY: fecha actual. Anillo animado: fecha seleccionada.
 */
@Composable
fun LumiWeekTimeline(
    weekDays: List<CycleDayUi>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onViewCalendarClick: () -> Unit,
    onResetToToday: () -> Unit,
    activityCounts: Map<LocalDate, Int> = emptyMap(),
    onOpenDate: ((LocalDate) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LumiThemeTokens.current
    val colors = LocalPhaseColors.current
    val today = remember(weekDays) { LumiClock.today() }
    val locale = Locale.getDefault()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val weeks = remember(weekDays) { weekDays.chunked(7).filter { it.size == 7 } }
    if (weeks.isEmpty()) return

    val todayWeekIndex = remember(weeks, today) {
        weeks.indexOfFirst { week -> week.any { it.date == today } }.coerceAtLeast(0)
    }
    val pager = rememberPagerState(initialPage = todayWeekIndex, pageCount = { weeks.size })
    var transientSelected by remember { mutableStateOf<LocalDate?>(null) }
    val selected = transientSelected ?: selectedDate ?: today
    LaunchedEffect(selectedDate, weeks) {
        val target = weeks.indexOfFirst { week -> week.any { it.date == selected } }
        if (target >= 0 && target != pager.currentPage) pager.animateScrollToPage(target)
    }
    val visibleWeek = weeks.getOrNull(pager.currentPage).orEmpty()
    val range = remember(visibleWeek, locale) {
        if (visibleWeek.isEmpty()) "" else {
            val dateFormat = DateTimeFormatter.ofPattern("d MMM", locale)
            visibleWeek.first().date.format(dateFormat) + " – " +
                visibleWeek.last().date.format(dateFormat)
        }
    }
    val selectedModel = weekDays.firstOrNull { it.date == selected }

    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LumiEditorialHeading(
                titleNormal = stringResource(R.string.lumi_cal_week_intro),
                titleSerifItalic = stringResource(R.string.lumi_cal_week_focus),
                subtitle = range, titleSize = 20.sp, serifSize = 23.sp,
                modifier = Modifier.weight(1f)
            )
            Row(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onViewCalendarClick)
                    .padding(horizontal = 9.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.lumi_cal_view_month), fontSize = 12.sp,
                    color = tokens.accent, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(5.dp))
                Icon(Icons.Rounded.ArrowForwardIos, contentDescription = null,
                    tint = tokens.accent, modifier = Modifier.size(10.dp))
            }
        }
        Spacer(Modifier.height(9.dp))
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth(), pageSpacing = 7.dp) { index ->
            val week = weeks[index]
            val selectedIndex = week.indexOfFirst { it.date == selected }
            val animatedIndex by animateFloatAsState(
                targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
                animationSpec = LumiMotion.SpatialSpring,
                label = "lumi_week_circle_travel"
            )
            BoxWithConstraints(Modifier.fillMaxWidth().height(68.dp)) {
                val spacing = 3.dp
                val dayWidth = (maxWidth - spacing * 6) / 7
                // One moving selection ring, behind the individual circular dates.
                if (selectedIndex >= 0) {
                    Box(
                        Modifier
                            .offset(
                                x = (dayWidth + spacing) * animatedIndex + (dayWidth - 39.dp) / 2,
                                y = 17.dp
                            )
                            .size(39.dp).clip(CircleShape)
                            .background(tokens.accentSoft)
                            .border(1.4.dp, tokens.accent.copy(alpha = .9f), CircleShape)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    week.forEach { day ->
                        val selectedHere = day.date == selected
                        val menstrual = day.phase == CyclePhase.MENSTRUAL
                        val fertile = day.phase == CyclePhase.OVULATION
                        val phaseDescription = when (day.phase) {
                            CyclePhase.MENSTRUAL -> stringResource(R.string.pm_phase_menstrual)
                            CyclePhase.OVULATION -> stringResource(R.string.phase_name_ovulation)
                            CyclePhase.FOLLICULAR -> stringResource(R.string.pm_phase_follicular)
                            CyclePhase.LUTEAL -> stringResource(R.string.pm_phase_luteal)
                            else -> stringResource(R.string.lumi_cal_week_no_cycle)
                        }
                        val loggedDescription = if (day.hasLog)
                            stringResource(R.string.cal_accessibility_with_log)
                            else stringResource(R.string.cal_accessibility_no_log)
                        val activityCount = activityCounts[day.date] ?: 0
                        val bg = when {
                            menstrual && !day.isPrediction -> colors.menstrual.copy(alpha = .85f)
                            menstrual -> colors.menstrual.copy(alpha = .17f)
                            fertile -> colors.ovulation.copy(alpha = if (day.isPrediction) .13f else .20f)
                            else -> Color.Transparent
                        }
                        Column(
                            modifier = Modifier.weight(1f).height(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.Button) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (onOpenDate != null) {
                                        transientSelected = day.date
                                        scope.launch {
                                            delay(95)
                                            onOpenDate(day.date)
                                            delay(260)
                                            transientSelected = null
                                        }
                                    } else {
                                        onSelectDate(day.date)
                                    }
                                }
                                .semantics {
                                    contentDescription = day.date.toString() + ", " +
                                        phaseDescription + ", " + loggedDescription +
                                        if (activityCount > 0) ", $activityCount actividades" else ""
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (day.isToday)
                                    stringResource(R.string.cal_today_cd).uppercase(locale)
                                else day.weekdayLabel.take(3).replaceFirstChar { it.titlecase(locale) },
                                color = if (day.isToday) tokens.accent else tokens.muted,
                                fontSize = 9.5.sp, maxLines = 1,
                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Box(
                                Modifier.size(32.dp).clip(CircleShape).background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day.dayOfMonth.toString(), fontSize = 15.sp,
                                    fontWeight = if (selectedHere || day.isToday || menstrual)
                                        FontWeight.Bold else FontWeight.Medium,
                                    color = if (menstrual && !day.isPrediction) (if (tokens.isDark) tokens.bg else tokens.text) else tokens.text,
                                    textAlign = TextAlign.Center)
                            }
                            Row(
                                Modifier.height(11.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (day.hasLog) Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = stringResource(R.string.cal_accessibility_with_log),
                                    tint = tokens.accent, modifier = Modifier.size(10.dp)
                                )
                                if (activityCount > 0) {
                                    Box(
                                        Modifier
                                            .width(if (activityCount > 2) 10.dp else 6.dp)
                                            .height(3.dp)
                                            .clip(CircleShape)
                                            .background(tokens.gold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 31.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (selectedModel?.hasLog == true) Icon(Icons.Rounded.Check,
                contentDescription = null, tint = tokens.accent, modifier = Modifier.size(12.dp))
            AnimatedContent(
                targetState = selectedModel,
                transitionSpec = { fadeIn(tween(230)) togetherWith fadeOut(tween(120)) },
                label = "lumi_selected_day_context",
                modifier = Modifier.weight(1f)
            ) { day ->
                val dayLabel = if (day == null || day.date == today)
                    stringResource(R.string.cal_today_cd)
                    else day.date.format(DateTimeFormatter.ofPattern("d MMM", locale))
                val phaseLabel = when(day?.phase) {
                    CyclePhase.MENSTRUAL -> stringResource(R.string.pm_phase_menstrual)
                    CyclePhase.OVULATION -> stringResource(R.string.phase_name_ovulation)
                    CyclePhase.FOLLICULAR -> stringResource(R.string.pm_phase_follicular)
                    CyclePhase.LUTEAL -> stringResource(R.string.pm_phase_luteal)
                    else -> stringResource(R.string.lumi_cal_week_no_cycle)
                }
                val qualified = if (day?.isPrediction == true && day.phase != CyclePhase.OVULATION)
                    stringResource(R.string.lumi_cal_estimate_phase, phaseLabel) else phaseLabel
                val logSuffix = if (day?.hasLog == true)
                    " · " + stringResource(R.string.cal_accessibility_with_log) else ""
                Text(
                    text = dayLabel + " · " + qualified + logSuffix,
                    fontSize = 11.sp, lineHeight = 16.sp,
                    color = tokens.muted, maxLines = 2
                )
            }
            if (selectedDate != null && selectedDate != today) {
                Text(
                    stringResource(R.string.home_timeline_back_to_today),
                    fontSize = 10.5.sp, color = tokens.accent,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onResetToToday()
                            scope.launch { pager.animateScrollToPage(todayWeekIndex) }
                        }
                        .padding(horizontal = 4.dp, vertical = 7.dp)
                )
            }
        }
    }
}
