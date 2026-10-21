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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.text.font.FontStyle
import com.jeremy.lumi.ui.theme.LumiSans
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.util.ClinicalLabels
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
                        fontFamily = LumiSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 25.sp,
                        color = tokens.text,
                        letterSpacing = (-0.7).sp
                    )
                    Text(
                        text = "  " + uiState.displayYear,
                        fontFamily = LumiEditorialSerif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 22.sp,
                        color = tokens.accent
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

        Spacer(Modifier.height(5.dp))
        HorizontalDivider(color = tokens.line.copy(alpha = 0.8f), thickness = 0.6.dp)
        Spacer(Modifier.height(9.dp))

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
        Spacer(Modifier.height(6.dp))

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
                        Modifier.fillMaxWidth().height(50.dp),
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

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = tokens.line.copy(alpha = .75f), thickness = .6.dp)
        Spacer(Modifier.height(11.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LumiLegendMark(phaseColor(CyclePhase.MENSTRUAL), stringResource(R.string.lumi_cal_legend_period))
            LumiLegendMark(phaseColor(CyclePhase.OVULATION), stringResource(R.string.lumi_cal_legend_fertile))
            LumiLegendLog(stringResource(R.string.lumi_cal_legend_record))
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
private fun LumiLegendLog(text: String) {
    val tokens = LumiThemeTokens.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Rounded.Check, contentDescription = null,
            tint = tokens.accent, modifier = Modifier.size(11.dp))
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
    val phaseTint = phaseColor(day.phase)
    val menstrual = day.phase == CyclePhase.MENSTRUAL
    val fertile = day.phase == CyclePhase.OVULATION
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .92f else if (selected) 1.045f else 1f,
        animationSpec = LumiMotion.SpatialSpring,
        label = "lumi_month_circle_scale"
    )
    // Phase colors are estimates, never proof of actual ovulation.
    val fill by animateColorAsState(
        targetValue = when {
            menstrual && day.isPrediction -> phaseTint.copy(alpha = .22f)
            menstrual -> phaseTint.copy(alpha = .85f)
            fertile && day.isPrediction -> phaseTint.copy(alpha = .20f)
            fertile -> phaseTint.copy(alpha = .34f)
            else -> Color.Transparent
        },
        animationSpec = tween(260),
        label = "lumi_month_phase_fill"
    )
    val ring by animateColorAsState(
        targetValue = when {
            selected -> tokens.accent
            day.isToday -> tokens.accent.copy(alpha = .58f)
            else -> Color.Transparent
        },
        animationSpec = tween(230),
        label = "lumi_month_selection_ring"
    )
    val phaseName = when(day.phase) {
        CyclePhase.OVULATION -> stringResource(R.string.phase_name_ovulation)
        CyclePhase.UNKNOWN -> stringResource(R.string.phase_unknown)
        else -> phaseLabel(day.phase)
    }
    val predicted = if (day.isPrediction) stringResource(R.string.cal_accessibility_prediction) else ""
    val logged = if (day.hasLog) stringResource(R.string.cal_accessibility_with_log)
        else stringResource(R.string.cal_accessibility_no_log)
    val description = day.dayOfMonth.toString() + ", " + phaseName + ", " + predicted + ", " + logged

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction, indication = null,
                role = Role.Button, onClick = onClick
            )
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(39.dp)
                .clip(CircleShape)
                .border(1.4.dp, ring, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(33.dp).clip(CircleShape).background(fill),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.dayOfMonth.toString(),
                    fontSize = 14.5.sp,
                    fontWeight = if (selected || day.isToday || menstrual)
                        FontWeight.SemiBold else FontWeight.Medium,
                    color = if (menstrual && !day.isPrediction) tokens.bg else tokens.text,
                    textAlign = TextAlign.Center
                )
            }
            if (day.showIntercourseHeart) {
                Icon(
                    if (day.intercourseProtected == true) Icons.Rounded.Favorite
                    else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = IntercourseHeartColor,
                    modifier = Modifier.align(Alignment.TopEnd).size(10.dp)
                )
            }
        }
        if (day.hasLog) {
            Icon(Icons.Rounded.Check,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(9.dp))
        } else {
            Spacer(Modifier.height(9.dp))
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
                    fontFamily = LumiSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 21.sp,
                    color = tokens.text,
                    lineHeight = 27.sp
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
            val phase = if (day.phase == CyclePhase.UNKNOWN) null else
                if (day.phase == CyclePhase.OVULATION)
                    stringResource(R.string.phase_name_ovulation)
                else phaseLabel(day.phase)
            val context = when {
                phase == null -> stringResource(R.string.lumi_cal_no_cycle)
                day.isPrediction -> stringResource(R.string.lumi_cal_estimate_phase, phase)
                else -> phase
            }
            Text(context, fontSize = 12.sp, color = tokens.muted)
            Spacer(Modifier.height(17.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (day.hasLog) tokens.accent else tokens.line)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        if (day.hasLog) stringResource(R.string.lumi_cal_day_logged)
                        else stringResource(R.string.lumi_cal_day_empty),
                        fontSize = 13.sp,
                        color = tokens.text,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (day.hasLog) stringResource(R.string.lumi_cal_day_logged_hint)
                        else stringResource(R.string.lumi_cal_day_empty_hint),
                        fontSize = 11.sp,
                        color = tokens.muted,
                        lineHeight = 15.sp
                    )
                }
            }

            // Read-only details from the saved record; never synthesize symptoms from a phase.
            day.logPreview?.let { log ->
                val mood = log.mood?.let { raw ->
                    ClinicalLabels.findMoodResId(raw)?.let { stringResource(it) } ?: raw
                }
                val flow = log.flow?.let { raw ->
                    ClinicalLabels.findFlowResId(raw)?.let { stringResource(it) } ?: raw
                }
                val symptoms = log.symptoms.map { raw ->
                    ClinicalLabels.findSymptomResId(raw)?.let { stringResource(it) } ?: raw
                }.joinToString(" · ")
                val notes = log.notes
                if (mood != null || flow != null || symptoms.isNotEmpty() || !notes.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(17.dp))
                            .background(tokens.panel.copy(alpha = if (tokens.isDark) .80f else .94f))
                            .padding(horizontal = 15.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (mood != null) LumiSavedDayRow(
                            stringResource(R.string.log_how_feeling_question), mood
                        )
                        if (flow != null) LumiSavedDayRow(
                            stringResource(R.string.log_period_status_question), flow
                        )
                        if (symptoms.isNotEmpty()) LumiSavedDayRow(
                            stringResource(R.string.log_tab_symptoms), symptoms
                        )
                        if (!notes.isNullOrBlank()) LumiSavedDayRow(
                            stringResource(R.string.log_tab_notes), notes
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LumiSavedDayRow(label: String, content: String) {
    val tokens = LumiThemeTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 10.sp, color = tokens.muted, fontWeight = FontWeight.SemiBold)
        Text(content, fontSize = 12.5.sp, color = tokens.text, lineHeight = 17.sp, maxLines = 3)
    }
}
