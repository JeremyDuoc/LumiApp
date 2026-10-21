package com.jeremy.lumi.ui.screens.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.jeremy.lumi.ui.theme.IntercourseHeartColor
import com.jeremy.lumi.ui.theme.LumiEditorialSerif
import com.jeremy.lumi.ui.theme.LumiMotion
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendario mensual Lumi, sin dependencia de navegación ni persistencia.
 *
 * Un día primero SELECCIONA, y la acción de registrar abre la hoja existente.
 * Se conservan todas las señales de CalendarDay (registro, predicción, protección).
 * La estética comparte tokens con Home y no supone un tema oscuro fijo.
 */
@Composable
fun LumiCalendarMonth(
    uiState: CalendarUiState,
    selectedDay: Int,
    onSelectDay: (CalendarDay) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiThemeTokens.current
    val haptic = LocalHapticFeedback.current
    val locale = Locale.getDefault()
    val monthTitle = remember(uiState.displayMonth, locale) {
        Month.of(uiState.displayMonth).getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.titlecase(locale) }
    }
    val weekdayNames = listOf(R.string.cal_mon, R.string.cal_tue, R.string.cal_wed,
        R.string.cal_thu, R.string.cal_fri, R.string.cal_sat, R.string.cal_sun)

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 23.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = monthTitle,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
                label = "lumi_month_title",
                modifier = Modifier.weight(1f)
            ) { title ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = title,
                        fontFamily = LumiEditorialSerif,
                        fontSize = 30.sp,
                        color = tokens.text,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "  " + uiState.displayYear,
                        fontSize = 13.sp,
                        color = tokens.muted
                    )
                }
            }

            IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPreviousMonth() }) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.cal_prev_month_desc), tint = tokens.text)
            }
            IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onNextMonth() }) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.cal_next_month_desc), tint = tokens.text)
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = tokens.line.copy(alpha = 0.8f), thickness = 0.6.dp)
        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            weekdayNames.forEach { weekday ->
                Text(
                    text = stringResource(weekday),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.muted,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // Swipe responde al gesto horizontal, no roba los taps ni el scroll vertical.
        AnimatedContent(
            targetState = uiState,
            contentKey = { it.displayYear to it.displayMonth },
            transitionSpec = {
                run {
                    val forward = targetState.displayYear * 12 + targetState.displayMonth >
                        initialState.displayYear * 12 + initialState.displayMonth
                    (slideInHorizontally(
                        animationSpec = tween(335, easing = LumiMotion.EaseOutExpo)
                    ) { width -> if (forward) width / 4 else -width / 4 } +
                        fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(210, easing = LumiMotion.EaseOutExpo)
                        ) { width -> if (forward) -width / 5 else width / 5 } +
                            fadeOut(tween(175)))
                }
            },
            label = "lumi_month_pages",
            modifier = Modifier.fillMaxWidth()
        ) { monthState ->
            var swipeDistance = 0f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(monthState.displayYear, monthState.displayMonth) {
                        detectHorizontalDragGestures(
                            onDragStart = { swipeDistance = 0f },
                            onHorizontalDrag = { change, distance ->
                                swipeDistance += distance
                                change.consume()
                            },
                            onDragEnd = {
                                when {
                                    swipeDistance > 60.dp.toPx() -> onPreviousMonth()
                                    swipeDistance < -60.dp.toPx() -> onNextMonth()
                                }
                                swipeDistance = 0f
                            },
                            onDragCancel = { swipeDistance = 0f }
                        )
                    }
            ) {
                monthState.days.chunked(7).forEach { week ->
                    Row(
                        Modifier.fillMaxWidth().height(51.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        week.forEach { day ->
                            if (day.isEmptyOffset) {
                                Spacer(Modifier.weight(1f).fillMaxHeight())
                            } else {
                                LumiMonthDay(
                                    day = day,
                                    selected = monthState.displayYear == uiState.displayYear &&
                                        monthState.displayMonth == uiState.displayMonth &&
                                        selectedDay == day.dayOfMonth,
                                    onClick = { onSelectDay(day) },
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        Spacer(Modifier.height(15.dp))
        HorizontalDivider(color = tokens.line.copy(alpha = .75f), thickness = .6.dp)
        Spacer(Modifier.height(13.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LumiLegendMark(tokens.rose, stringResource(R.string.lumi_cal_legend_period))
            LumiLegendMark(tokens.mint, stringResource(R.string.lumi_cal_legend_fertile))
            LumiLegendMark(tokens.accent, stringResource(R.string.lumi_cal_legend_record))
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun LumiLegendMark(color: Color, text: String) {
    val tokens = LumiThemeTokens.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(color))
        Text(text, fontSize = 10.sp, color = tokens.muted)
    }
}

@Composable
private fun LumiMonthDay(
    day: CalendarDay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiThemeTokens.current
    val color = phaseColor(day.phase)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .91f else if (selected) 1.035f else 1f,
        animationSpec = LumiMotion.SpatialSpring,
        label = "calendar_day_scale"
    )
    val fill by animateColorAsState(
        targetValue = when {
            selected -> tokens.accentSoft
            day.phase == CyclePhase.MENSTRUAL -> color.copy(alpha = if (day.isPrediction) .10f else .24f)
            day.phase == CyclePhase.OVULATION -> color.copy(alpha = if (day.isPrediction) .045f else .085f)
            else -> Color.Transparent
        },
        animationSpec = tween(240),
        label = "calendar_day_fill"
    )
    val border by animateColorAsState(
        targetValue = when {
            selected -> tokens.accent.copy(alpha = .85f)
            day.isToday -> tokens.accent.copy(alpha = .5f)
            else -> Color.Transparent
        },
        animationSpec = tween(250),
        label = "calendar_day_outline"
    )
    val shape = RoundedCornerShape(15.dp)
    val description = when {
        day.isToday -> stringResource(R.string.lumi_cal_a11y_today, day.dayOfMonth)
        day.isPrediction -> stringResource(R.string.lumi_cal_a11y_prediction, day.dayOfMonth)
        day.hasLog -> stringResource(R.string.lumi_cal_a11y_logged, day.dayOfMonth)
        else -> stringResource(R.string.lumi_cal_a11y_empty, day.dayOfMonth)
    }
    Box(
        modifier = modifier
            .padding(horizontal = 1.dp, vertical = 2.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (selected || day.isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) tokens.text else if (day.isPrediction) tokens.text.copy(alpha = .77f) else tokens.text,
            modifier = Modifier.offset(y = if (day.hasLog) (-2).dp else 0.dp)
        )
        if (day.hasLog) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(4.dp)
                .clip(CircleShape).background(tokens.accent))
        } else if (day.phase != CyclePhase.UNKNOWN) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(3.dp)
                .clip(CircleShape).background(color.copy(alpha = if (day.isPrediction) .45f else .85f)))
        }
        if (day.showIntercourseHeart) {
            Icon(
                if (day.intercourseProtected == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = IntercourseHeartColor,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 1.dp, top = 2.dp).size(10.dp)
            )
        }
    }
}

/** El día seleccionado tiene su propio espacio: la grilla no abre hojas accidentalmente. */
@Composable
fun LumiCalendarDayDetail(
    uiState: CalendarUiState,
    selectedDay: Int,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiThemeTokens.current
    val day = uiState.days.firstOrNull { !it.isEmptyOffset && it.dayOfMonth == selectedDay }
    val date = remember(uiState.displayYear, uiState.displayMonth, selectedDay) {
        runCatching { LocalDate.of(uiState.displayYear, uiState.displayMonth, selectedDay) }.getOrNull()
    }
    val locale = Locale.getDefault()
    val datePattern = if (locale.language == "es") "EEEE d 'de' MMMM" else "EEEE d MMMM"
    val title = date?.format(DateTimeFormatter.ofPattern(datePattern, locale))
        ?.replaceFirstChar { it.titlecase(locale) } ?: stringResource(R.string.lumi_cal_choose_day)
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)
        .animateContentSize(animationSpec = tween(300, easing = LumiMotion.EaseOutExpo))) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(130)) },
                label = "lumi_selected_date",
                modifier = Modifier.weight(1f)
            ) { label ->
                Text(
                    text = label,
                    fontFamily = LumiEditorialSerif,
                    fontSize = 23.sp,
                    color = tokens.text,
                    lineHeight = 28.sp
                )
            }
            if (day != null) {
                androidx.compose.material3.IconButton(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.accentSoft)
                ) {
                    Icon(
                        if (day.hasLog) androidx.compose.material.icons.Icons.Rounded.Edit else Icons.Rounded.Add,
                        contentDescription = if (day.hasLog) stringResource(R.string.lumi_cal_view_log)
                            else stringResource(R.string.lumi_cal_record_day),
                        tint = tokens.accent,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        if (day != null) {
            val phase = if (day.phase == CyclePhase.UNKNOWN) null else phaseLabel(day.phase)
            val context = when {
                phase == null -> stringResource(R.string.lumi_cal_no_cycle)
                day.isPrediction -> stringResource(R.string.lumi_cal_estimate_phase, phase)
                else -> phase
            }
            Text(context, fontSize = 12.sp, color = tokens.muted)
            Spacer(Modifier.height(17.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.panel.copy(alpha = if (tokens.isDark) .64f else .75f))
                    .padding(horizontal = 15.dp, vertical = 17.dp)
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape)
                    .background(if (day.hasLog) tokens.accent else tokens.dim))
                Column {
                    Text(
                        if (day.hasLog) stringResource(R.string.lumi_cal_day_logged) else stringResource(R.string.lumi_cal_day_empty),
                        fontSize = 13.sp,
                        color = tokens.text,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (day.hasLog) stringResource(R.string.lumi_cal_day_logged_hint)
                        else stringResource(R.string.lumi_cal_day_empty_hint),
                        fontSize = 11.sp, color = tokens.muted,
                        lineHeight = 16.sp
                    )
                }
            }

        }
    }
}
