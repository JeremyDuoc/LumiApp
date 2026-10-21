package com.jeremy.lumi.ui.screens.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.util.ClinicalLabels
import com.jeremy.lumi.ui.theme.IntercourseHeartColor
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import com.jeremy.lumi.ui.theme.LumiEditorialSerif
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class CalendarMode { MONTH, YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    requestedDate: LocalDate? = null,
    onDateOpened: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(CalendarMode.MONTH) }
    var showLegend by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showCycles by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var editorDate by rememberSaveable { mutableStateOf<String?>(null) }
    var overviewYear by rememberSaveable { mutableIntStateOf(state.displayYear) }
    LaunchedEffect(requestedDate) {
        requestedDate?.let { viewModel.selectDate(it); mode = CalendarMode.MONTH; onDateOpened() }
    }
    val snackbar = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.log_saved_confirmation)
    LaunchedEffect(viewModel) {
        viewModel.logEvents.collect { event ->
            if (event == CalendarViewModel.LogEvent.SAVED) {
                editorDate = null
                snackbar.showSnackbar(savedMessage)
            }
        }
    }
    val yearData by produceState<List<Pair<String, List<CalendarDay>>>?>(null, mode, overviewYear, state.dataVersion) {
        if (mode == CalendarMode.YEAR) { value = null; value = viewModel.getYearData(overviewYear) }
    }
    Scaffold(containerColor = LumiThemeTokens.current.bg, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        CalendarContent(
            state, mode, overviewYear, yearData,
            onModeChange = { overviewYear = state.displayYear; mode = it },
            onToday = { viewModel.goToToday(); mode = CalendarMode.MONTH },
            onPrevious = { if (mode == CalendarMode.MONTH) viewModel.navigateToPreviousMonth() else overviewYear-- },
            onNext = { if (mode == CalendarMode.MONTH) viewModel.navigateToNextMonth() else overviewYear++ },
            onPickDate = { showDatePicker = true }, onSelectDate = viewModel::selectDate,
            onSelectMonth = { viewModel.navigateToMonth(overviewYear, it); mode = CalendarMode.MONTH },
            onEdit = { viewModel.fetchLogForDate(it); editorDate = it.toString() },
            onLegend = { showLegend = true }, onHistory = { showHistory = true }, onCycles = { showCycles = true },
            modifier = Modifier.padding(padding)
        )
    }
    editorDate?.let { rawDate ->
        CalendarLogEditor(LocalDate.parse(rawDate), viewModel) { editorDate = null; viewModel.clearSelectedLog() }
    }
    if (showHistory) LogHistorySheet(onDismiss = { showHistory = false })
    if (showCycles) CycleHistorySheet(onDismiss = { showCycles = false })
    if (showLegend) CalendarLegendDialog { showLegend = false }
    if (showDatePicker) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(enabled = picker.selectedDateMillis != null, onClick = {
                    picker.selectedDateMillis?.let {
                        viewModel.selectDate(java.time.Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    mode = CalendarMode.MONTH; showDatePicker = false
                }) { Text(stringResource(R.string.calendar_open_date)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.dialog_reset_cancel)) } }
        ) { DatePicker(state = picker) }
    }
}

/** Stateless presentation lets UI tests exercise navigation without touching health records. */
@Composable
internal fun CalendarContent(
    state: CalendarUiState, mode: CalendarMode, overviewYear: Int,
    yearData: List<Pair<String, List<CalendarDay>>>?,
    onModeChange: (CalendarMode) -> Unit, onToday: () -> Unit,
    onPrevious: () -> Unit, onNext: () -> Unit, onPickDate: () -> Unit,
    onSelectDate: (LocalDate) -> Unit, onSelectMonth: (Int) -> Unit, onEdit: (LocalDate) -> Unit,
    onLegend: () -> Unit, onHistory: () -> Unit, onCycles: () -> Unit, modifier: Modifier = Modifier
) {
    val tokens = LumiThemeTokens.current
    val monthScroll = rememberLazyListState()
    val yearScroll = rememberLazyListState()
    LaunchedEffect(overviewYear) { yearScroll.scrollToItem(0) }
    val selectedDay = state.days.firstOrNull { it.date == state.selectedDate }
    Column(modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("calendar_scroll"),
        state = if (mode == CalendarMode.MONTH) monthScroll else yearScroll,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item("heading") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.calendar_title), fontFamily = LumiEditorialSerif,
                        fontSize = 30.sp, lineHeight = 36.sp, color = tokens.text)
                    Text(stringResource(R.string.calendar_subtitle), fontSize = 13.sp, color = tokens.muted)
                }
                IconButton(onClick = onLegend) {
                    Icon(Icons.Rounded.Info, stringResource(R.string.cal_legend_title_cd),
                        tint = tokens.muted, modifier = Modifier.size(22.dp))
                }
            }
        }
        item("controls") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                ModeToggleSwitch(mode, onModeChange)
                TextButton(onClick = onToday, modifier = Modifier.testTag("calendar_today")) {
                    Icon(Icons.Rounded.Today, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(stringResource(R.string.calendar_today), fontSize = 14.sp)
                }
            }
        }
        if (mode == CalendarMode.MONTH) {
            item("month") { MonthView(state, onPrevious, onNext, onPickDate, onSelectDate, onLegend) }
            item("day") {
                if (selectedDay != null) SelectedDayCard(selectedDay)
                else Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
            item("today_context") { TodayCycleContext(state) }
        } else {
            item("year_header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(overviewYear.toString(), fontFamily = LumiEditorialSerif, fontSize = 30.sp, color = tokens.text)
                        Text(stringResource(R.string.calendar_year_hint), fontSize = 13.sp, color = tokens.muted)
                    }
                    CalendarArrow(false, onPrevious, R.string.calendar_previous_year)
                    CalendarArrow(true, onNext, R.string.calendar_next_year)
                }
            }
            if (yearData == null) {
                item { Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } }
            } else {
                items(yearData.chunked(2).size) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        yearData.chunked(2)[row].forEachIndexed { col, (_, days) ->
                            val month = row * 2 + col
                            SmallMonthBox(overviewYear, month, days, { onSelectMonth(month) }, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item("history") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, tokens.line)) {
                    Icon(Icons.Rounded.History, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.calendar_records))
                }
                OutlinedButton(onClick = onCycles, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, tokens.line)) {
                    Icon(Icons.Rounded.DateRange, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.calendar_cycles))
                }
            }
        }
    }
    if (mode == CalendarMode.MONTH && selectedDay != null && !selectedDay.isFuture) {
        val locale = LocalConfiguration.current.locales[0]
        val date = state.selectedDate
        Surface(color = tokens.bg) {
            Column {
                HorizontalDivider(color = tokens.line)
                Button(onClick = { onEdit(date) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        .fillMaxWidth().heightIn(min = 50.dp).testTag("calendar_edit"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = calendarOnAccent())) {
                    Icon(if (selectedDay.hasLog) Icons.Rounded.Edit else Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (selectedDay.hasLog) R.string.calendar_edit_date else R.string.calendar_add_date,
                        date.format(DateTimeFormatter.ofPattern("d MMM", locale))),
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    }
}

@Composable
fun ModeToggleSwitch(mode: CalendarMode, onModeChange: (CalendarMode) -> Unit) {
    val tokens = LumiThemeTokens.current
    Row(Modifier.clip(RoundedCornerShape(18.dp)).background(tokens.panel).padding(4.dp).selectableGroup()) {
        CalendarMode.entries.forEach { option ->
            val active = mode == option
            Box(Modifier.clip(RoundedCornerShape(14.dp))
                .background(if (active) tokens.accentSoft else Color.Transparent)
                .selectable(active, role = Role.Tab, onClick = { onModeChange(option) })
                .heightIn(min = 44.dp).padding(horizontal = 22.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(if (option == CalendarMode.MONTH) R.string.cal_mode_month else R.string.cal_mode_year),
                    color = if (active) calendarAccent() else tokens.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun MonthView(
    state: CalendarUiState, onPrevious: () -> Unit, onNext: () -> Unit,
    onPickDate: () -> Unit, onSelectDate: (LocalDate) -> Unit, onLegend: () -> Unit
) {
    val tokens = LumiThemeTokens.current
    val locale = LocalConfiguration.current.locales[0]
    val title = LocalDate.of(state.displayYear, state.displayMonth, 1)
        .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }
    Surface(shape = RoundedCornerShape(28.dp), color = tokens.panel, border = BorderStroke(1.dp, tokens.line)) {
        Column(Modifier.padding(horizontal = 10.dp).padding(top = 10.dp, bottom = 6.dp)) {
            Row(Modifier.padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onPickDate)
                    .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f, fill = false), fontSize = 20.sp,
                        fontFamily = LumiEditorialSerif, color = tokens.text)
                    Icon(Icons.Rounded.ExpandMore, stringResource(R.string.calendar_jump_date),
                        Modifier.padding(start = 4.dp).size(18.dp), tint = tokens.muted)
                }
                CalendarArrow(false, onPrevious, R.string.cal_prev_month_desc)
                CalendarArrow(true, onNext, R.string.cal_next_month_desc)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                (1..7).forEach { day ->
                    Text(java.time.DayOfWeek.of(day).getDisplayName(java.time.format.TextStyle.SHORT, locale)
                        .replaceFirstChar { it.titlecase(locale) }.take(3),
                        Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp, color = tokens.muted)
                }
            }
            Spacer(Modifier.height(8.dp))
            var drag by remember { mutableFloatStateOf(0f) }
            Column(Modifier.fillMaxWidth().testTag("calendar_grid")
                .pointerInput(state.displayYear, state.displayMonth) {
                    detectHorizontalDragGestures(onDragStart = { drag = 0f },
                        onDragEnd = {
                            if (drag < -64.dp.toPx()) onNext()
                            if (drag > 64.dp.toPx()) onPrevious()
                        }, onHorizontalDrag = { change, amount -> change.consume(); drag += amount })
                }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.days.isEmpty()) Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                state.days.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(Modifier.weight(1f)) {
                                if (day.isEmptyOffset) Spacer(Modifier.height(48.dp))
                                else DayCell(day, day.date == state.selectedDate, { day.date?.let(onSelectDate) })
                            }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = tokens.line, modifier = Modifier.padding(horizontal = 8.dp))
            CalendarInlineLegend(onLegend)
        }
    }
}

@Composable
private fun CalendarArrow(forward: Boolean, onClick: () -> Unit, description: Int) {
    IconButton(onClick = onClick) {
        Icon(if (forward) Icons.Rounded.ChevronRight else Icons.Rounded.ChevronLeft,
            stringResource(description), tint = LumiThemeTokens.current.text)
    }
}

/** The same marks in Home and the month: selection, today, saved check, estimated outline. */
@Composable
fun CalendarDateMark(day: CalendarDay, selected: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, weekday: String? = null) {
    val tokens = LumiThemeTokens.current
    val accent = calendarAccent()
    val phase = phaseColor(day.phase)
    val emphasizedPhase = day.phase == CyclePhase.MENSTRUAL || day.phase == CyclePhase.OVULATION
    val bg by animateColorAsState(when {
        selected -> tokens.accent
        emphasizedPhase -> phase.copy(alpha = if (day.isPrediction) .07f else .19f)
        else -> Color.Transparent
    }, tween(180), label = "calendar_selection")
    val foreground = if (selected) calendarOnAccent() else tokens.text
    val locale = LocalConfiguration.current.locales[0]
    val fullDate = day.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)) ?: day.dayOfMonth.toString()
    val phaseText = if (day.phase == CyclePhase.UNKNOWN) stringResource(R.string.calendar_no_phase)
        else stringResource(R.string.calendar_estimated_phase, phaseLabel(day.phase))
    val semanticsLabel = listOfNotNull(fullDate,
        if (day.isToday) stringResource(R.string.calendar_today) else null, phaseText,
        stringResource(if (day.hasLog) R.string.calendar_recorded else R.string.calendar_no_record),
        if (day.showIntercourseHeart) stringResource(R.string.cal_legend_intercourse) else null).joinToString(", ")
    val haptic = LocalHapticFeedback.current
    Column(modifier.fillMaxWidth().heightIn(min = if (weekday == null) 48.dp else 78.dp)
        .padding(horizontal = 2.dp).clip(RoundedCornerShape(16.dp)).background(bg)
        .then(if (day.isToday && !selected) Modifier.border(1.5.dp, accent, RoundedCornerShape(16.dp)) else Modifier)
        .then(if (day.isPrediction && emphasizedPhase && !selected) Modifier.drawBehind {
            drawRoundRect(phase.copy(alpha = .7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
        } else Modifier)
        .selectable(selected, role = Role.Button, onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick()
        }).clearAndSetSemantics {
            contentDescription = semanticsLabel; this.selected = selected; role = Role.Button
            testTag = "calendar_day_" + (day.date?.toString() ?: day.dayOfMonth.toString())
            onClick { onClick(); true }
        }
        .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (weekday != null) {
            Text(weekday, fontSize = 10.sp, color = if (selected) foreground else tokens.muted)
            Spacer(Modifier.height(3.dp))
        }
        Text(day.dayOfMonth.toString(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = foreground)
        Row(Modifier.height(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (day.hasLog) Icon(Icons.Rounded.Check, null, Modifier.size(11.dp),
                tint = if (selected) foreground else accent)
            else if (emphasizedPhase) Box(Modifier.size(4.dp).clip(CircleShape)
                .background(if (selected) foreground.copy(alpha = .65f) else phase))
            if (day.showIntercourseHeart) Icon(
                if (day.intercourseProtected == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                null, Modifier.size(9.dp), tint = if (selected) foreground else IntercourseHeartColor)
        }
    }
}

@Composable
fun DayCell(day: CalendarDay, selected: Boolean, onClick: () -> Unit) = CalendarDateMark(day, selected, onClick)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarInlineLegend(onClick: () -> Unit) {
    val tokens = LumiThemeTokens.current
    FlowRow(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendLabel(phaseColor(CyclePhase.MENSTRUAL), stringResource(R.string.calendar_period))
        LegendLabel(phaseColor(CyclePhase.OVULATION), stringResource(R.string.calendar_ovulation))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Check, null, Modifier.size(13.dp), tint = calendarAccent())
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.calendar_record), fontSize = 11.sp, color = tokens.muted)
        }
        Text(stringResource(R.string.calendar_outline_hint), fontSize = 11.sp, color = tokens.muted)
    }
}

@Composable
private fun LegendLabel(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(5.dp))
        Text(label, color = LumiThemeTokens.current.muted, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SelectedDayCard(day: CalendarDay) {
    val date = day.date ?: return
    val tokens = LumiThemeTokens.current
    val locale = LocalConfiguration.current.locales[0]
    val record = day.record
    val log = record?.dailyLog
    val dateText = date.format(DateTimeFormatter.ofPattern("EEEE d '·' MMMM", locale)).replaceFirstChar { it.titlecase(locale) }
    Column(Modifier.fillMaxWidth().testTag("calendar_day_detail"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(3.dp).height(38.dp).clip(CircleShape).background(tokens.accent))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (day.isToday) stringResource(R.string.calendar_today_date, dateText) else dateText,
                    fontFamily = LumiEditorialSerif, fontSize = 23.sp, lineHeight = 29.sp, color = tokens.text)
                Text(if (day.phase == CyclePhase.UNKNOWN) stringResource(R.string.calendar_no_phase)
                    else stringResource(R.string.calendar_estimated_phase, phaseLabel(day.phase)),
                    fontSize = 12.sp, color = tokens.muted)
            }
        }
        if (day.isFuture) {
            Surface(color = tokens.panel, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, tokens.line)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.calendar_future_title), color = tokens.text, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.calendar_future_body), fontSize = 13.sp, lineHeight = 20.sp, color = tokens.muted)
                }
            }
        } else {
            if (log == null) {
                Text(stringResource(if (day.isToday) R.string.calendar_empty_today else R.string.calendar_empty_day),
                    color = tokens.muted, fontSize = 14.sp, lineHeight = 21.sp)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    log.mood?.let { SummaryChip(ClinicalLabels.findMoodResId(it)?.let { id -> stringResource(id) } ?: it) }
                    log.flowIntensity?.let { SummaryChip(stringResource(R.string.calendar_flow_summary,
                        ClinicalLabels.findFlowResId(it)?.let { id -> stringResource(id) } ?: it)) }
                    if (log.painLevel > 0) SummaryChip(stringResource(R.string.calendar_pain_summary, log.painLevel))
                    if (log.spotting) SummaryChip(stringResource(R.string.calendar_spotting))
                    record.symptoms.take(3).forEach {
                        SummaryChip(ClinicalLabels.findSymptomResId(it.name)?.let { id -> stringResource(id) } ?: it.name)
                    }
                    if (record.symptoms.size > 3) SummaryChip(stringResource(R.string.calendar_more_symptoms, record.symptoms.size - 3))
                    if (log.mood == null && log.flowIntensity == null && log.painLevel == 0 && !log.spotting && record.symptoms.isEmpty())
                        SummaryChip(stringResource(R.string.calendar_recorded))
                }
                if (!log.notes.isNullOrBlank()) Text(log.notes, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    fontSize = 13.sp, lineHeight = 20.sp, color = tokens.muted)
            }
        }
    }
}

@Composable
private fun SummaryChip(text: String) {
    Surface(color = LumiThemeTokens.current.panel, shape = RoundedCornerShape(10.dp)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = LumiThemeTokens.current.text, fontSize = 12.sp)
    }
}

@Composable
private fun TodayCycleContext(state: CalendarUiState) {
    val tokens = LumiThemeTokens.current
    val prediction = state.prediction
    if (prediction == null || state.currentPhase == CyclePhase.UNKNOWN) return
    val locale = LocalConfiguration.current.locales[0]
    val periodText = when {
        state.currentPhase == CyclePhase.PREGNANCY || prediction.nextPeriodDate == LocalDate.MAX -> null
        prediction.isLate -> stringResource(R.string.calendar_period_late, prediction.delayDays)
        else -> stringResource(R.string.calendar_next_estimate,
            prediction.nextPeriodDate.format(DateTimeFormatter.ofPattern("d MMM", locale)))
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(tokens.accentSoft).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(stringResource(R.string.calendar_today_cycle, state.currentDayOfCycle),
            color = calendarAccent(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(phaseLabel(state.currentPhase), color = tokens.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        if (periodText != null) Text(periodText, color = tokens.muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun SmallMonthBox(year: Int, month: Int, days: List<CalendarDay>, onClick: () -> Unit, modifier: Modifier) {
    val tokens = LumiThemeTokens.current
    val primary = calendarAccent()
    val locale = LocalConfiguration.current.locales[0]
    val textMeasurer = rememberTextMeasurer()
    val monthName = java.time.Month.of(month + 1).getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.titlecase(locale) }
    val countText = stringResource(R.string.calendar_month_records, days.count { it.hasLog })
    val isCurrent = days.any { it.isToday }
    val periodColor = phaseColor(CyclePhase.MENSTRUAL)
    val ovulationColor = phaseColor(CyclePhase.OVULATION)
    Surface(onClick = onClick, modifier = modifier.testTag("calendar_month_${month + 1}")
        .semantics(mergeDescendants = true) { contentDescription = "$monthName $year, $countText" },
        color = tokens.panel, shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (isCurrent) primary else tokens.line)) {
        Column(Modifier.padding(12.dp)) {
            Text(monthName, fontSize = 16.sp, fontFamily = LumiEditorialSerif, color = tokens.text)
            Spacer(Modifier.height(10.dp))
            Canvas(Modifier.fillMaxWidth().height(112.dp).clearAndSetSemantics { }) {
                val w = size.width / 7; val h = size.height / 6
                days.forEachIndexed { index, day ->
                    if (!day.isEmptyOffset) {
                        val center = Offset((index % 7 + .5f) * w, (index / 7 + .5f) * h)
                        val color = when (day.phase) {
                            CyclePhase.MENSTRUAL -> periodColor
                            CyclePhase.OVULATION -> ovulationColor
                            else -> null
                        }
                        if (color != null) drawCircle(color.copy(alpha = if (day.isPrediction) .09f else .23f), h * .44f, center)
                        if (day.isToday) drawCircle(primary, h * .44f, center, style = Stroke(1.dp.toPx()))
                        val text = textMeasurer.measure(day.dayOfMonth.toString(),
                            androidx.compose.ui.text.TextStyle(fontSize = 9.sp, color = tokens.text))
                        drawText(text, topLeft = Offset(center.x - text.size.width / 2, center.y - text.size.height / 2))
                        if (day.hasLog) drawCircle(primary, 1.2.dp.toPx(), Offset(center.x, center.y + h * .38f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(countText, fontSize = 11.sp, color = tokens.muted)
        }
    }
}

@Composable
private fun CalendarLegendDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.cal_legend_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.calendar_legend_body), fontSize = 14.sp, lineHeight = 21.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.calendar_recorded))
                }
                Text(stringResource(R.string.calendar_legend_selection), fontSize = 13.sp)
                Text(stringResource(R.string.calendar_legend_privacy), fontSize = 13.sp)
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_understood)) } })
}

@Composable
private fun CalendarLogEditor(date: LocalDate, viewModel: CalendarViewModel, onDismiss: () -> Unit) {
    val log by viewModel.selectedLog.collectAsStateWithLifecycle()
    val loadedDate by viewModel.selectedLogDate.collectAsStateWithLifecycle()
    val saving by viewModel.isSaving.collectAsStateWithLifecycle()
    val loading by viewModel.isLoadingLog.collectAsStateWithLifecycle()
    val loadFailed by viewModel.loadFailed.collectAsStateWithLifecycle()
    val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()
    val categories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val custom by viewModel.customSymptoms.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteSymptoms.collectAsStateWithLifecycle()
    LaunchedEffect(date) { if (loadedDate != date) viewModel.fetchLogForDate(date) }
    DailyLogSheet(day = date.dayOfMonth, month = date.monthValue, year = date.year, savedLog = log,
        isSaving = saving, isLoading = loading || loadedDate != date, loadFailed = loadFailed, saveFailed = saveFailed,
        onRetryLoad = { viewModel.fetchLogForDate(date) },
        customSymptoms = custom, onCustomSymptomsChange = viewModel::setCustomSymptoms,
        favoriteSymptoms = favorites, onFavoriteSymptomsChange = viewModel::setFavoriteSymptoms,
        activeCategories = categories, onActiveCategoriesChange = viewModel::setActiveCategories, onDismiss = onDismiss,
        onSave = { flow, pain, mood, symptoms, mucus, notes, intercourse, protection, method, intimacyNotes,
                   showIntimacy, sleep, energy, stress, water, temperature, spotting, intensities ->
            viewModel.saveDailyLog(date.dayOfMonth, flow, pain, mood, symptoms, mucus, notes, intercourse,
                protection, method, intimacyNotes, showIntimacy, sleep, energy, stress, water, temperature, spotting, intensities)
        })
}

@Composable
fun calendarAccent(): Color {
    val tokens = LumiThemeTokens.current
    return if (tokens.isDark) tokens.accent else lerp(tokens.accent, tokens.text, .48f)
}

@Composable
private fun calendarOnAccent(): Color {
    val background = LumiThemeTokens.current.accent.luminance()
    val dark = Color(0xFF21182C)
    val darkContrast = (background + .05f) / (dark.luminance() + .05f)
    val lightContrast = 1.05f / (background + .05f)
    return if (lightContrast > darkContrast) Color.White else dark
}

@Composable
fun phaseColor(phase: CyclePhase): Color {
    val colors = LocalPhaseColors.current
    return when (phase) {
        CyclePhase.MENSTRUAL -> colors.menstrual
        CyclePhase.FOLLICULAR -> colors.follicular
        CyclePhase.OVULATION -> colors.ovulation
        CyclePhase.LUTEAL -> colors.luteal
        CyclePhase.PREGNANCY -> LumiThemeTokens.current.accent
        else -> LumiThemeTokens.current.muted
    }
}

@Composable
fun phaseLabel(phase: CyclePhase): String = stringResource(when (phase) {
    CyclePhase.MENSTRUAL -> R.string.phase_menstrual
    CyclePhase.OVULATION -> R.string.calendar_ovulation
    CyclePhase.LUTEAL -> R.string.phase_luteal
    CyclePhase.FOLLICULAR -> R.string.phase_follicular
    CyclePhase.PREGNANCY -> R.string.phase_pregnancy
    else -> R.string.phase_unknown
})
