package com.jeremy.lumi.ui.screens.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.theme.LumiEditorialSerif
import com.jeremy.lumi.ui.theme.LumiMotion
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import com.jeremy.lumi.utils.LumiClock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class PlannerCalendarScale { DAY, WEEK, MONTH }
enum class PlannerCreateChoice { TASK, EVENT, REMINDER, WELLBEING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumiPlannerCalendar(
    calendarState: CalendarUiState,
    plannerState: PlannerUiState,
    selectedDate: LocalDate,
    scale: PlannerCalendarScale,
    onScaleChange: (PlannerCalendarScale) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpenMore: () -> Unit,
    onOpenWellbeingLog: () -> Unit,
    onCreateScheduled: (String, Int, Int, PlannerItemKind) -> Unit,
    onCreateInbox: (String, Int, PlannerItemKind) -> Unit,
    onToggleCompleted: (String) -> Unit,
    onScheduleInbox: (String, LocalDate, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiThemeTokens.current
    val haptic = LocalHapticFeedback.current
    val scroll = rememberScrollState()
    var showCreate by remember { mutableStateOf(false) }
    var showInbox by remember { mutableStateOf(false) }
    val locale = Locale.getDefault()
    val titleMonth = selectedDate.month.getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.titlecase(locale) }

    val sidePadding = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp <= 390) 16.dp else 20.dp

    Box(modifier.fillMaxSize().background(tokens.bg)) {
        com.jeremy.lumi.ui.components.AmbientBackgroundLayer(
            screenType = com.jeremy.lumi.ui.theme.AmbientScreen.CONTENT
        )

        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth().height(68.dp).padding(horizontal = sidePadding),
                contentAlignment = Alignment.Center
            ) {
                PlannerTopBar(
                    inboxCount = plannerState.inbox.size,
                    onInbox = { showInbox = true },
                    onMore = onOpenMore
                )
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)
                    .padding(start = sidePadding, end = sidePadding, top = 4.dp, bottom = 104.dp)
            ) {
            PlannerDateHeader(
                month = titleMonth,
                year = selectedDate.year,
                onToday = onToday,
                onPrevious = onPrevious,
                onNext = onNext
            )
            Spacer(Modifier.height(16.dp))
            PlannerScaleSwitch(scale = scale) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onScaleChange(it)
            }
            Spacer(Modifier.height(14.dp))

            AnimatedVisibility(visible = scale != PlannerCalendarScale.MONTH) {
                Column {
                    PlannerWeekStrip(
                        calendarState = calendarState,
                        plannerState = plannerState,
                        selectedDate = selectedDate,
                        onSelectDate = onSelectDate
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            PlannerContextRibbon(
                calendarState = calendarState,
                plannerState = plannerState,
                selectedDate = selectedDate
            )
            Spacer(Modifier.height(10.dp))

            AnimatedContent(
                targetState = scale,
                transitionSpec = {
                    (fadeIn(tween(240, easing = LumiMotion.EaseOutExpo)) +
                        slideInVertically(tween(300, easing = LumiMotion.EaseOutExpo)) { 18 }) togetherWith
                        (fadeOut(tween(120)) + slideOutVertically(tween(160)) { -8 })
                },
                label = "planner_scale"
            ) { active ->
                when (active) {
                    PlannerCalendarScale.DAY -> PlannerDayView(
                        date = selectedDate,
                        plannerState = plannerState,
                        onToggleCompleted = onToggleCompleted
                    )
                    PlannerCalendarScale.WEEK -> PlannerWeekView(
                        selectedDate = selectedDate,
                        calendarState = calendarState,
                        plannerState = plannerState,
                        onOpenDate = {
                            onSelectDate(it)
                            onScaleChange(PlannerCalendarScale.DAY)
                        }
                    )
                    PlannerCalendarScale.MONTH -> PlannerMonthView(
                        selectedDate = selectedDate,
                        calendarState = calendarState,
                        plannerState = plannerState,
                        onSelectDate = onSelectDate
                    )
                }
            }
            }
        }

        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showCreate = true
            },
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp).size(58.dp),
            shape = CircleShape,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF2A1D2E),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp)
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(tokens.rose, tokens.accent))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Crear", modifier = Modifier.size(28.dp))
            }
        }
    }

    if (showCreate) {
        PlannerCreateSheet(
            selectedDate = selectedDate,
            onDismiss = { showCreate = false },
            onWellbeing = {
                showCreate = false
                onOpenWellbeingLog()
            },
            onSave = { title, start, duration, kind ->
                onCreateScheduled(title, start, duration, kind)
                showCreate = false
            },
            onSaveInbox = { title, duration, kind ->
                onCreateInbox(title, duration, kind)
                showCreate = false
            }
        )
    }

    if (showInbox) {
        PlannerInboxSheet(
            state = plannerState,
            selectedDate = selectedDate,
            onDismiss = { showInbox = false },
            onSchedule = onScheduleInbox
        )
    }
}

@Composable
private fun PlannerTopBar(
    inboxCount: Int,
    onInbox: () -> Unit,
    onMore: () -> Unit
) {
    val tokens = LumiThemeTokens.current
    val logoMotion = rememberInfiniteTransition(label = "planner_logo_breathe")
    val logoRotation by logoMotion.animateFloat(
        initialValue = -5f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LumiMotion.EaseInOutQuart),
            repeatMode = RepeatMode.Reverse
        ),
        label = "planner_logo_rotation"
    )
    val logoScale by logoMotion.animateFloat(
        initialValue = .96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LumiMotion.EaseInOutQuart),
            repeatMode = RepeatMode.Reverse
        ),
        label = "planner_logo_scale"
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            val logoShape = RoundedCornerShape(
                topStart = 11.dp, topEnd = 15.dp, bottomEnd = 12.dp, bottomStart = 14.dp
            )
            Box(
                Modifier.size(26.dp)
                    .graphicsLayer {
                        rotationZ = logoRotation
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                    .clip(logoShape)
                    .background(
                        Brush.linearGradient(
                            listOf(tokens.accent, tokens.rose, tokens.peach, tokens.mint)
                        )
                    )
            ) {
                Box(
                    Modifier.fillMaxSize().padding(5.dp).clip(logoShape).background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = .55f), Color.Transparent)
                        )
                    )
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(
                "lumi",
                color = tokens.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.25).sp
            )
        }
        Box {
            PlannerIconButton(onClick = onInbox) {
                Icon(
                    Icons.Rounded.Inbox,
                    contentDescription = "Pendientes",
                    tint = tokens.muted,
                    modifier = Modifier.size(19.dp)
                )
            }
            if (inboxCount > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp)
                        .heightIn(min = 17.dp).clip(CircleShape).background(tokens.rose)
                        .border(2.dp, tokens.bg, CircleShape)
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        inboxCount.coerceAtMost(99).toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF281B26)
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        PlannerIconButton(onClick = onMore) {
            Icon(
                Icons.Rounded.MoreHoriz,
                contentDescription = "Más opciones",
                tint = tokens.muted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PlannerIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val tokens = LumiThemeTokens.current
    Box(
        Modifier.size(38.dp).clip(RoundedCornerShape(13.dp))
            .background(tokens.panel.copy(alpha = .78f))
            .border(.8.dp, tokens.line, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun PlannerDateHeader(
    month: String,
    year: Int,
    onToday: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val tokens = LumiThemeTokens.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(
                "TU CALENDARIO",
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = tokens.muted,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    month,
                    fontFamily = LumiEditorialSerif,
                    fontSize = 30.sp,
                    lineHeight = 32.sp,
                    color = tokens.text
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    year.toString(),
                    fontFamily = LumiEditorialSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 28.sp,
                    color = tokens.accent
                )
            }
        }
        TextButton(
            onClick = onToday,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text("Hoy", color = tokens.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(35.dp)) {
            Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = "Anterior", tint = tokens.muted)
        }
        IconButton(onClick = onNext, modifier = Modifier.size(35.dp)) {
            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "Siguiente", tint = tokens.muted)
        }
    }
}

@Composable
private fun PlannerScaleSwitch(
    scale: PlannerCalendarScale,
    onChange: (PlannerCalendarScale) -> Unit
) {
    val tokens = LumiThemeTokens.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .background(tokens.panel.copy(alpha = .72f))
            .border(.8.dp, tokens.line, RoundedCornerShape(15.dp)).padding(4.dp)
    ) {
        listOf(
            PlannerCalendarScale.DAY to "Día",
            PlannerCalendarScale.WEEK to "Semana",
            PlannerCalendarScale.MONTH to "Mes"
        ).forEach { (item, label) ->
            val active = item == scale
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                    .background(if (active) tokens.panel2 else Color.Transparent)
                    .clickable { onChange(item) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) tokens.text else tokens.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlannerWeekStrip(
    calendarState: CalendarUiState,
    plannerState: PlannerUiState,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    val tokens = LumiThemeTokens.current
    val phaseColors = LocalPhaseColors.current
    val weekStart = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
    val locale = Locale.getDefault()
    val today = LumiClock.today()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(7) { index ->
            val date = weekStart.plusDays(index.toLong())
            val day = calendarDayFor(date, calendarState)
            val selected = date == selectedDate
            val menstrual = day?.phase == CyclePhase.MENSTRUAL
            val fertile = day?.phase == CyclePhase.OVULATION
            val bg = when {
                selected -> tokens.accentSoft
                menstrual -> phaseColors.menstrual.copy(
                    alpha = if (day?.isPrediction == true) .16f else .26f
                )
                else -> Color.Transparent
            }
            val border = if (selected) tokens.accent.copy(alpha = .72f) else Color.Transparent
            val scaleValue by animateFloatAsState(
                if (selected) 1.025f else 1f,
                LumiMotion.SpatialSpring,
                label = "planner_week_day"
            )
            Column(
                Modifier.weight(1f).height(76.dp)
                    .graphicsLayer { scaleX = scaleValue; scaleY = scaleValue }
                    .clip(RoundedCornerShape(16.dp)).background(bg)
                    .border(if (selected) 1.dp else 0.dp, border, RoundedCornerShape(16.dp))
                    .clickable { onSelectDate(date) }.padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (date == today) "HOY"
                    else date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(3).uppercase(locale),
                    fontSize = 8.5.sp,
                    color = if (date == today) tokens.accent else tokens.dim,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .background(if (selected) tokens.accent else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        date.dayOfMonth.toString(),
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (selected) tokens.bg else tokens.text
                    )
                }
                Row(
                    Modifier.height(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (day?.hasLog == true) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(tokens.accent))
                    }
                    val count = plannerState.activityCount(date)
                    if (count > 0) {
                        Box(
                            Modifier.width(if (count > 2) 9.dp else 5.dp).height(4.dp)
                                .clip(CircleShape).background(tokens.gold)
                        )
                    }
                    if (fertile) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(phaseColors.ovulation))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannerContextRibbon(
    calendarState: CalendarUiState,
    plannerState: PlannerUiState,
    selectedDate: LocalDate
) {
    val tokens = LumiThemeTokens.current
    val day = calendarDayFor(selectedDate, calendarState)
    val items = plannerState.itemsFor(selectedDate)
    val planned = items.sumOf { it.durationMinutes }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        PlannerPill(
            dot = phaseDot(day?.phase),
            text = phaseName(day?.phase),
            suffix = if (day?.isPrediction == true) "Estimación" else null
        )
        PlannerPill(
            dot = tokens.accent,
            text = if (items.size == 1) "1 actividad" else "${items.size} actividades",
            suffix = if (planned > 0) durationLabel(planned) else null
        )
        if (day?.hasLog == true) {
            PlannerPill(dot = tokens.mint, text = "Registro guardado", suffix = null)
        }
    }
}

@Composable
private fun PlannerPill(dot: Color, text: String, suffix: String?) {
    val tokens = LumiThemeTokens.current
    Row(
        Modifier.clip(CircleShape).background(tokens.panel.copy(alpha = .68f))
            .border(.8.dp, tokens.line, CircleShape)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(7.dp))
        Text(text, color = tokens.text, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        suffix?.let { Text(" · $it", color = tokens.muted, fontSize = 10.5.sp) }
    }
}

@Composable
private fun PlannerDayView(
    date: LocalDate,
    plannerState: PlannerUiState,
    onToggleCompleted: (String) -> Unit
) {
    val tokens = LumiThemeTokens.current
    val items = plannerState.itemsFor(date)
    val planned = items.sumOf { it.durationMinutes }
    val locale = Locale.getDefault()
    val currentMinutes = LocalTime.now().hour * 60 + LocalTime.now().minute
    val nowItem = items.firstOrNull { currentMinutes in it.startMinutes until it.endMinutes }
    val today = LumiClock.today()

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    date.format(DateTimeFormatter.ofPattern("EEEE d", locale))
                        .replaceFirstChar { it.titlecase(locale) },
                    fontFamily = LumiEditorialSerif,
                    fontSize = 25.sp,
                    color = tokens.text
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${if (items.size == 1) "1 actividad" else "${items.size} actividades"} · ${durationLabel(planned)} planificadas",
                    fontSize = 10.5.sp,
                    color = tokens.muted
                )
            }
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(tokens.panel)
                    .border(.8.dp, tokens.line, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = tokens.muted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Ahora", fontSize = 10.5.sp, color = tokens.muted)
            }
        }

        AnimatedVisibility(visible = date == today && nowItem != null) {
            nowItem?.let {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(tokens.accentSoft.copy(alpha = .7f), tokens.panel.copy(alpha = .9f))
                            )
                        )
                        .border(.8.dp, tokens.accent.copy(alpha = .34f), RoundedCornerShape(17.dp))
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "AHORA",
                            fontSize = 8.5.sp,
                            letterSpacing = .8.sp,
                            color = tokens.accent,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            it.title,
                            fontSize = 12.5.sp,
                            color = tokens.text,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${minutesToTime(it.startMinutes)}–${minutesToTime(it.endMinutes)} · ${kindLabel(it.kind)}",
                            fontSize = 9.5.sp,
                            color = tokens.muted
                        )
                    }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(tokens.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = tokens.bg,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        PlannerTimeline(date = date, items = items, onToggleCompleted = onToggleCompleted)
    }
}

@Composable
private fun PlannerTimeline(
    date: LocalDate,
    items: List<PlannerItem>,
    onToggleCompleted: (String) -> Unit
) {
    val tokens = LumiThemeTokens.current
    val hourHeight = 58.dp
    val startHour = 7
    val endHour = 22
    val totalHeight = hourHeight * (endHour - startHour + 1)
    val today = LumiClock.today()
    val now = LocalTime.now()
    val nowMinutes = now.hour * 60 + now.minute
    val gaps = remember(items) { freeGaps(items, startHour * 60, (endHour + 1) * 60) }

    BoxWithConstraints(
        Modifier.fillMaxWidth().height(totalHeight).drawBehind {
            val hourPx = hourHeight.toPx()
            for (i in 0..(endHour - startHour)) {
                val y = i * hourPx
                drawLine(
                    tokens.line.copy(alpha = .78f),
                    start = androidx.compose.ui.geometry.Offset(50.dp.toPx(), y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
            drawLine(
                tokens.line,
                start = androidx.compose.ui.geometry.Offset(49.dp.toPx(), 0f),
                end = androidx.compose.ui.geometry.Offset(49.dp.toPx(), size.height),
                strokeWidth = 1f
            )
        }
    ) {
        for (hour in startHour..endHour) {
            val y = hourHeight * (hour - startHour)
            Text(
                "%02d:00".format(hour),
                modifier = Modifier.offset(x = 5.dp, y = y - 7.dp),
                fontSize = 9.5.sp,
                color = tokens.dim
            )
        }

        gaps.filter { it.duration >= 75 }.forEach { gap ->
            val midpoint = gap.start + gap.duration / 2
            val top = timelineOffset(midpoint, startHour, hourHeight)
            Row(
                Modifier.offset(x = 82.dp, y = top),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(16.dp).height(1.dp).background(tokens.line))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${durationLabel(gap.duration)} libres",
                    fontSize = 8.5.sp,
                    color = tokens.dim
                )
            }
        }

        items.forEach { item ->
            val baseTop = timelineOffset(item.startMinutes, startHour, hourHeight)
            val top = baseTop + 6.dp
            val rawHeight = hourHeight * (item.durationMinutes / 60f) - 4.dp
            val blockHeight = rawHeight.coerceAtLeast(42.dp)
            val eventColor = itemColor(item.kind)
            Text(
                minutesToTime(item.startMinutes),
                modifier = Modifier.offset(x = 1.dp, y = baseTop + 8.dp),
                color = eventColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                Modifier.offset(x = 63.dp, y = top).fillMaxWidth().height(blockHeight)
                    .clip(RoundedCornerShape(if (blockHeight < 50.dp) 13.dp else 16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(eventColor.copy(alpha = .17f), tokens.panel.copy(alpha = .96f))
                        )
                    )
                    .border(
                        .8.dp,
                        eventColor.copy(alpha = .5f),
                        RoundedCornerShape(if (blockHeight < 50.dp) 13.dp else 16.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.width(3.dp).height(blockHeight - 16.dp)
                        .clip(CircleShape).background(eventColor)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        color = tokens.text.copy(alpha = if (item.completed) .55f else 1f),
                        fontSize = if (blockHeight < 50.dp) 11.sp else 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (blockHeight >= 50.dp) {
                        Text(
                            "${minutesToTime(item.startMinutes)}–${minutesToTime(item.endMinutes)} · ${kindLabel(item.kind)}",
                            color = tokens.muted.copy(alpha = if (item.completed) .55f else 1f),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box(
                    Modifier.padding(end = 10.dp).size(27.dp).clip(CircleShape)
                        .background(if (item.completed) eventColor else tokens.panel.copy(alpha = .78f))
                        .border(.8.dp, eventColor.copy(alpha = .7f), CircleShape)
                        .clickable { onToggleCompleted(item.id) },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.completed) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Completada",
                            tint = tokens.bg,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        if (date == today && now.hour in startHour..endHour) {
            val top = timelineOffset(nowMinutes, startHour, hourHeight)
            Row(
                Modifier.offset(x = 43.dp, y = top + 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(tokens.rose))
                Box(Modifier.weight(1f).height(1.dp).background(tokens.rose))
            }
            Text(
                minutesToTime(nowMinutes),
                modifier = Modifier.offset(x = 0.dp, y = top - 1.dp),
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tokens.rose
            )
        }
    }
}

@Composable
private fun PlannerWeekView(
    selectedDate: LocalDate,
    calendarState: CalendarUiState,
    plannerState: PlannerUiState,
    onOpenDate: (LocalDate) -> Unit
) {
    val tokens = LumiThemeTokens.current
    val locale = Locale.getDefault()
    val today = LumiClock.today()
    val weekStart = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Tu semana",
            fontFamily = LumiEditorialSerif,
            fontSize = 25.sp,
            color = tokens.text,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            "Una vista rápida antes de entrar al detalle",
            fontSize = 10.5.sp,
            color = tokens.muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        repeat(7) { index ->
            val date = weekStart.plusDays(index.toLong())
            val items = plannerState.itemsFor(date)
            val day = calendarDayFor(date, calendarState)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 70.dp)
                    .clickable { onOpenDate(date) }
                    .padding(vertical = 9.dp).drawBehind {
                        drawLine(
                            tokens.line,
                            start = androidx.compose.ui.geometry.Offset(0f, size.height - 1f),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height - 1f),
                            strokeWidth = 1f
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.width(54.dp)) {
                    Text(
                        if (date == today) "HOY"
                        else date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale),
                        fontSize = 9.sp,
                        color = if (date == today) tokens.accent else tokens.muted,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        date.dayOfMonth.toString(),
                        fontFamily = LumiEditorialSerif,
                        fontSize = 23.sp,
                        color = tokens.text
                    )
                }
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        items.take(2).forEach { item -> PlannerMiniEvent(item) }
                        if (items.size > 2) {
                            Text("+${items.size - 2}", fontSize = 9.sp, color = tokens.muted)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (items.isEmpty()) "Día abierto"
                        else "${durationLabel(items.sumOf { it.durationMinutes })} en agenda",
                        fontSize = 8.5.sp,
                        color = tokens.dim
                    )
                }
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .background(phaseDot(day?.phase).copy(alpha = .08f))
                        .border(.8.dp, phaseDot(day?.phase).copy(alpha = .55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        phaseShort(day?.phase),
                        fontSize = 8.sp,
                        color = tokens.muted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannerMiniEvent(item: PlannerItem) {
    val tokens = LumiThemeTokens.current
    val color = itemColor(item.kind)
    Row(
        Modifier.height(29.dp).width(142.dp).clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = .12f))
            .border(.8.dp, color.copy(alpha = .42f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            "${minutesToTime(item.startMinutes)} · ${item.title}",
            fontSize = 9.5.sp,
            color = tokens.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlannerMonthView(
    selectedDate: LocalDate,
    calendarState: CalendarUiState,
    plannerState: PlannerUiState,
    onSelectDate: (LocalDate) -> Unit
) {
    val tokens = LumiThemeTokens.current
    val locale = Locale.getDefault()
    val ym = YearMonth.of(selectedDate.year, selectedDate.month)
    val first = ym.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val cells = ((offset + ym.lengthOfMonth() + 6) / 7) * 7
    val start = first.minusDays(offset.toLong())
    val selectedItems = plannerState.itemsFor(selectedDate)

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
            listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom").forEach {
                Text(
                    it.uppercase(locale),
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 8.5.sp,
                    color = tokens.dim,
                    letterSpacing = .6.sp
                )
            }
        }
        for (row in 0 until cells / 7) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (col in 0 until 7) {
                    val date = start.plusDays((row * 7 + col).toLong())
                    PlannerMonthDay(
                        modifier = Modifier.weight(1f),
                        date = date,
                        inMonth = date.month == ym.month,
                        selected = date == selectedDate,
                        day = calendarDayFor(date, calendarState),
                        activityCount = plannerState.activityCount(date),
                        onClick = { onSelectDate(date) }
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
        }
        Spacer(Modifier.height(15.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(tokens.line))
        Spacer(Modifier.height(16.dp))
        Text(
            selectedDate.format(
                DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale)
            ).replaceFirstChar { it.titlecase(locale) },
            fontFamily = LumiEditorialSerif,
            fontSize = 22.sp,
            color = tokens.text
        )
        Spacer(Modifier.height(7.dp))
        if (selectedItems.isEmpty()) {
            Text("Este día está libre de actividades.", fontSize = 10.5.sp, color = tokens.muted)
        } else {
            selectedItems.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        minutesToTime(item.startMinutes),
                        Modifier.width(44.dp),
                        fontSize = 8.5.sp,
                        color = tokens.dim
                    )
                    Box(Modifier.size(7.dp).clip(CircleShape).background(itemColor(item.kind)))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.title,
                        Modifier.weight(1f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(durationLabel(item.durationMinutes), fontSize = 9.sp, color = tokens.muted)
                }
            }
        }
    }
}

@Composable
private fun PlannerMonthDay(
    modifier: Modifier,
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    day: CalendarDay?,
    activityCount: Int,
    onClick: () -> Unit
) {
    val tokens = LumiThemeTokens.current
    val phaseColors = LocalPhaseColors.current
    val menstrual = day?.phase == CyclePhase.MENSTRUAL
    val fertile = day?.phase == CyclePhase.OVULATION
    val bg = when {
        selected -> tokens.accentSoft
        menstrual && day?.isPrediction == false -> phaseColors.menstrual.copy(alpha = .14f)
        else -> Color.Transparent
    }
    val borderColor = when {
        selected -> tokens.accent
        menstrual && day?.isPrediction == true -> phaseColors.menstrual.copy(alpha = .46f)
        else -> Color.Transparent
    }
    val scaleValue by animateFloatAsState(
        if (selected) 1.035f else 1f,
        LumiMotion.SpatialSpring,
        label = "month_day"
    )
    Column(
        modifier.aspectRatio(1f)
            .graphicsLayer {
                scaleX = scaleValue
                scaleY = scaleValue
                alpha = if (inMonth) 1f else .22f
            }
            .clip(RoundedCornerShape(14.dp)).background(bg)
            .border(
                if (selected || (menstrual && day?.isPrediction == true)) 1.dp else 0.dp,
                borderColor,
                RoundedCornerShape(14.dp)
            )
            .drawBehind {
                if (fertile && !menstrual) {
                    val stroke = 2.5.dp.toPx()
                    drawLine(
                        phaseColors.ovulation.copy(alpha = .52f),
                        start = androidx.compose.ui.geometry.Offset(size.width * .23f, size.height - stroke),
                        end = androidx.compose.ui.geometry.Offset(size.width * .77f, size.height - stroke),
                        strokeWidth = stroke
                    )
                }
            }
            .clickable(enabled = inMonth, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            date.dayOfMonth.toString(),
            fontSize = 12.5.sp,
            fontWeight = if (date == LumiClock.today()) FontWeight.ExtraBold else FontWeight.Normal,
            color = tokens.text
        )
        Spacer(Modifier.height(5.dp))
        Row(Modifier.height(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (day?.hasLog == true) {
                Box(Modifier.size(4.dp).clip(CircleShape).background(tokens.accent))
            }
            if (activityCount > 0) {
                Box(Modifier.size(4.dp).clip(CircleShape).background(tokens.gold))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerCreateSheet(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onWellbeing: () -> Unit,
    onSave: (String, Int, Int, PlannerItemKind) -> Unit,
    onSaveInbox: (String, Int, PlannerItemKind) -> Unit
) {
    val tokens = LumiThemeTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var choice by remember { mutableStateOf(PlannerCreateChoice.TASK) }
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("18:00") }
    var duration by remember { mutableStateOf(45) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.panel,
        contentColor = tokens.text,
        dragHandle = {
            Box(
                Modifier.padding(vertical = 11.dp).width(44.dp).height(4.dp)
                    .clip(CircleShape).background(tokens.line)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Crear en tu día",
                    fontFamily = LumiEditorialSerif,
                    fontSize = 26.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = tokens.muted)
                }
            }
            Text(
                "Lo básico primero. Puedes ajustar más detalles después.",
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = tokens.muted
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                listOf(
                    PlannerCreateChoice.TASK to "Tarea",
                    PlannerCreateChoice.EVENT to "Evento",
                    PlannerCreateChoice.REMINDER to "Recordatorio",
                    PlannerCreateChoice.WELLBEING to "Registro"
                ).forEach { (item, label) ->
                    val active = item == choice
                    AssistChip(
                        onClick = {
                            choice = item
                            if (item == PlannerCreateChoice.WELLBEING) onWellbeing()
                        },
                        label = { Text(label, fontSize = 9.5.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (active) tokens.accentSoft else tokens.panel2,
                            labelColor = if (active) tokens.text else tokens.muted
                        ),
                        border = BorderStroke(
                            .8.dp,
                            if (active) tokens.accent else tokens.line
                        ),
                        shape = CircleShape
                    )
                }
            }
            if (choice != PlannerCreateChoice.WELLBEING) {
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("¿Qué quieres hacer?") },
                    placeholder = { Text("Ej. Estudiar base de datos") },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp)
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it.take(5) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Hora") },
                        singleLine = true,
                        shape = RoundedCornerShape(13.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Duración", fontSize = 9.5.sp, color = tokens.muted)
                        Spacer(Modifier.height(5.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(15, 30, 45, 60, 90).forEach { value ->
                                Box(
                                    Modifier.clip(CircleShape)
                                        .background(
                                            if (duration == value) tokens.accentSoft else tokens.panel2
                                        )
                                        .border(
                                            .8.dp,
                                            if (duration == value) tokens.accent else tokens.line,
                                            CircleShape
                                        )
                                        .clickable { duration = value }
                                        .padding(horizontal = 9.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        if (value < 60) "$value m"
                                        else if (value == 60) "1 h" else "1½ h",
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSaveInbox(title, duration, choice.toKind())
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar sin hora", color = tokens.muted, fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            val minutes = parseTimeMinutes(time) ?: return@Button
                            if (title.isNotBlank()) {
                                onSave(title, minutes, duration, choice.toKind())
                            }
                        },
                        enabled = title.isNotBlank() && parseTimeMinutes(time) != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.accent,
                            contentColor = tokens.bg
                        )
                    ) {
                        Text("Guardar", fontWeight = FontWeight.ExtraBold)
                    }
                }
                Text(
                    selectedDate.format(
                        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.getDefault())
                    ).replaceFirstChar { it.titlecase() },
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                    fontSize = 9.sp,
                    color = tokens.dim
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerInboxSheet(
    state: PlannerUiState,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSchedule: (String, LocalDate, Int) -> Unit
) {
    val tokens = LumiThemeTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = LumiClock.today()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.panel,
        contentColor = tokens.text,
        dragHandle = {
            Box(
                Modifier.padding(vertical = 11.dp).width(44.dp).height(4.dp)
                    .clip(CircleShape).background(tokens.line)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Pendientes",
                    fontFamily = LumiEditorialSerif,
                    fontSize = 26.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = tokens.muted)
                }
            }
            Text(
                "Cosas que quieres hacer, pero todavía no necesitan ocupar un lugar en tu día.",
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = tokens.muted
            )
            Spacer(Modifier.height(13.dp))
            if (state.inbox.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .border(.8.dp, tokens.line, RoundedCornerShape(16.dp))
                        .padding(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay pendientes sin hora.", fontSize = 11.sp, color = tokens.muted)
                }
            } else {
                state.inbox.forEach { item ->
                    val color = itemColor(item.kind)
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 9.dp)
                            .clip(RoundedCornerShape(16.dp)).background(tokens.panel2)
                            .border(.8.dp, tokens.line, RoundedCornerShape(16.dp))
                            .padding(13.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                item.title,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "${durationLabel(item.durationMinutes)} · ${kindLabel(item.kind)}",
                            fontSize = 9.sp,
                            color = tokens.muted,
                            modifier = Modifier.padding(top = 5.dp, bottom = 7.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            InboxQuickButton("Hoy 18:00") {
                                onSchedule(item.id, today, 18 * 60)
                            }
                            InboxQuickButton("Mañana") {
                                onSchedule(item.id, today.plusDays(1), 18 * 60)
                            }
                            if (selectedDate != today) {
                                InboxQuickButton("Este día") {
                                    onSchedule(item.id, selectedDate, 18 * 60)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxQuickButton(text: String, onClick: () -> Unit) {
    val tokens = LumiThemeTokens.current
    Box(
        Modifier.clip(CircleShape).background(tokens.panel)
            .border(.8.dp, tokens.line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Text(text, fontSize = 8.5.sp, color = tokens.muted)
    }
}

private data class FreeGap(val start: Int, val duration: Int)

private fun freeGaps(
    items: List<PlannerItem>,
    start: Int,
    end: Int
): List<FreeGap> {
    if (items.isEmpty()) return listOf(FreeGap(start, end - start))
    val sorted = items.sortedBy { it.startMinutes }
    val result = mutableListOf<FreeGap>()
    var cursor = start
    sorted.forEach { item ->
        if (item.startMinutes > cursor) {
            result += FreeGap(cursor, item.startMinutes - cursor)
        }
        cursor = maxOf(cursor, item.endMinutes)
    }
    if (cursor < end) result += FreeGap(cursor, end - cursor)
    return result
}

private fun timelineOffset(minutes: Int, startHour: Int, hourHeight: Dp): Dp {
    val delta = (minutes - startHour * 60).coerceAtLeast(0)
    return hourHeight * (delta / 60f)
}

private fun calendarDayFor(date: LocalDate, state: CalendarUiState): CalendarDay? {
    if (date.year != state.displayYear || date.monthValue != state.displayMonth) return null
    return state.days.firstOrNull {
        !it.isEmptyOffset && it.dayOfMonth == date.dayOfMonth
    }
}

@Composable
private fun phaseDot(phase: CyclePhase?): Color {
    val colors = LocalPhaseColors.current
    val tokens = LumiThemeTokens.current
    return when (phase) {
        CyclePhase.MENSTRUAL -> colors.menstrual
        CyclePhase.OVULATION -> colors.ovulation
        CyclePhase.FOLLICULAR -> tokens.rose
        CyclePhase.LUTEAL -> tokens.peach
        else -> tokens.dim
    }
}

private fun phaseName(phase: CyclePhase?): String = when (phase) {
    CyclePhase.MENSTRUAL -> "Menstruación"
    CyclePhase.FOLLICULAR -> "Fase folicular"
    CyclePhase.OVULATION -> "Ventana fértil"
    CyclePhase.LUTEAL -> "Fase lútea"
    else -> "Sin datos del ciclo"
}

private fun phaseShort(phase: CyclePhase?): String = when (phase) {
    CyclePhase.MENSTRUAL -> "P"
    CyclePhase.OVULATION -> "F"
    CyclePhase.FOLLICULAR -> "Fo"
    CyclePhase.LUTEAL -> "L"
    else -> "·"
}

@Composable
private fun itemColor(kind: PlannerItemKind): Color {
    val tokens = LumiThemeTokens.current
    return when (kind) {
        PlannerItemKind.TASK -> tokens.gold
        PlannerItemKind.EVENT -> tokens.mint
        PlannerItemKind.REMINDER -> tokens.gold
    }
}

private fun kindLabel(kind: PlannerItemKind): String = when (kind) {
    PlannerItemKind.TASK -> "Tarea"
    PlannerItemKind.EVENT -> "Evento"
    PlannerItemKind.REMINDER -> "Recordatorio"
}

private fun PlannerCreateChoice.toKind(): PlannerItemKind = when (this) {
    PlannerCreateChoice.EVENT -> PlannerItemKind.EVENT
    PlannerCreateChoice.REMINDER -> PlannerItemKind.REMINDER
    else -> PlannerItemKind.TASK
}

private fun durationLabel(minutes: Int): String {
    if (minutes <= 0) return "0 min"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}

private fun minutesToTime(minutes: Int): String =
    "%02d:%02d".format((minutes / 60).coerceIn(0, 23), minutes % 60)

private fun parseTimeMinutes(raw: String): Int? {
    val match = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(raw.trim()) ?: return null
    val h = match.groupValues[1].toIntOrNull() ?: return null
    val m = match.groupValues[2].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}
