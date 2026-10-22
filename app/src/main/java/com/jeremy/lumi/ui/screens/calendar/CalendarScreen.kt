package com.jeremy.lumi.ui.screens.calendar

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.usecase.CyclePrediction
import com.jeremy.lumi.ui.theme.IntercourseHeartColor
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

enum class CalendarMode { MONTH, YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    initialDate: java.time.LocalDate? = null,
    onInitialDateConsumed: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel(),
    plannerViewModel: PlannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLog by viewModel.selectedLog.collectAsStateWithLifecycle()
    val plannerState by plannerViewModel.uiState.collectAsStateWithLifecycle()

    val today = com.jeremy.lumi.utils.LumiClock.today()
    val tokens = com.jeremy.lumi.ui.theme.LumiThemeTokens.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMsg = stringResource(R.string.log_saved_confirmation)

    var selectedDate by remember { mutableStateOf(initialDate ?: today) }
    var scale by remember { mutableStateOf(PlannerCalendarScale.DAY) }
    var showYear by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showLegendDialog by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showCycleHistory by remember { mutableStateOf(false) }
    var selectedDayForLog by remember { mutableStateOf<Int?>(null) }
    var pendingFutureDay by remember { mutableStateOf<Int?>(null) }

    fun selectDate(date: java.time.LocalDate) {
        selectedDate = date
        if (date.year != uiState.displayYear || date.monthValue != uiState.displayMonth) {
            viewModel.navigateToMonth(date.year, date.monthValue)
        }
    }

    LaunchedEffect(initialDate) {
        initialDate?.let { date ->
            selectDate(date)
            scale = PlannerCalendarScale.DAY
            showYear = false
            onInitialDateConsumed()
        }
    }

    val focusedModel = uiState.days.firstOrNull {
        !it.isEmptyOffset && it.dayOfMonth == selectedDate.dayOfMonth &&
            selectedDate.year == uiState.displayYear &&
            selectedDate.monthValue == uiState.displayMonth
    }
    val openSelectedLog: () -> Unit = {
        focusedModel?.let { day ->
            if (day.isFuture) {
                pendingFutureDay = day.dayOfMonth
            } else {
                viewModel.fetchLogForDay(day.dayOfMonth)
                selectedDayForLog = day.dayOfMonth
            }
        }
        Unit
    }

    Scaffold(
        containerColor = tokens.bg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            Modifier.fillMaxSize().padding(paddingValues).background(tokens.bg)
        ) {
            if (showYear) {
                com.jeremy.lumi.ui.components.AmbientBackgroundLayer(
                    screenType = com.jeremy.lumi.ui.theme.AmbientScreen.CONTENT
                )
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showYear = false }) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Volver", tint = tokens.muted)
                        }
                        Text(
                            "Vista anual",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.text,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    YearView(
                        initialYear = uiState.displayYear,
                        viewModel = viewModel,
                        onMonthClick = { year, month ->
                            val date = java.time.LocalDate.of(year, month, 1)
                            viewModel.navigateToMonth(year, month)
                            selectedDate = date
                            scale = PlannerCalendarScale.MONTH
                            showYear = false
                        }
                    )
                }
            } else {
                LumiPlannerCalendar(
                    calendarState = uiState,
                    plannerState = plannerState,
                    selectedDate = selectedDate,
                    scale = scale,
                    onScaleChange = { scale = it },
                    onSelectDate = { date ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectDate(date)
                    },
                    onPrevious = {
                        val date = when (scale) {
                            PlannerCalendarScale.DAY -> selectedDate.minusDays(1)
                            PlannerCalendarScale.WEEK -> selectedDate.minusWeeks(1)
                            PlannerCalendarScale.MONTH -> selectedDate.minusMonths(1)
                        }
                        selectDate(date)
                    },
                    onNext = {
                        val date = when (scale) {
                            PlannerCalendarScale.DAY -> selectedDate.plusDays(1)
                            PlannerCalendarScale.WEEK -> selectedDate.plusWeeks(1)
                            PlannerCalendarScale.MONTH -> selectedDate.plusMonths(1)
                        }
                        selectDate(date)
                    },
                    onToday = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectDate(today)
                    },
                    onOpenMore = { showMoreSheet = true },
                    onOpenWellbeingLog = openSelectedLog,
                    onCreateScheduled = { title, startMinutes, durationMinutes, kind ->
                        plannerViewModel.createScheduled(
                            selectedDate, title, startMinutes, durationMinutes, kind
                        )
                    },
                    onCreateInbox = { title, durationMinutes, kind ->
                        plannerViewModel.addInbox(title, durationMinutes, kind)
                    },
                    onToggleCompleted = plannerViewModel::toggleCompleted,
                    onScheduleInbox = plannerViewModel::scheduleInbox
                )
            }
        }
    }

    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            containerColor = tokens.panel,
            contentColor = tokens.text,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    Modifier.padding(vertical = 11.dp).width(44.dp).height(4.dp)
                        .clip(CircleShape).background(tokens.line)
                )
            }
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
            ) {
                Text(
                    "Más del calendario",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                listOf(
                    Triple("Vista anual", Icons.Rounded.DateRange) {
                        showMoreSheet = false
                        showYear = true
                    },
                    Triple("Qué significan los colores", Icons.Rounded.Info) {
                        showMoreSheet = false
                        showLegendDialog = true
                    },
                    Triple("Historial de registros", Icons.Rounded.History) {
                        showMoreSheet = false
                        showHistory = true
                    }
                ).forEach { (label, icon, action) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = action)
                            .padding(horizontal = 10.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(label, color = tokens.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = tokens.dim, modifier = Modifier.size(18.dp))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .clickable {
                            showMoreSheet = false
                            showCycleHistory = true
                        }
                        .padding(horizontal = 10.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = tokens.rose, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Historial de ciclos", color = tokens.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = tokens.dim, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    selectedDayForLog?.let { day ->
        DailyLogSheet(
            day = day,
            month = uiState.displayMonth,
            year = uiState.displayYear,
            savedLog = selectedLog,
            activeCategories = activeCategories,
            onActiveCategoriesChange = { viewModel.setActiveCategories(it) },
            onDismiss = {
                selectedDayForLog = null
                viewModel.clearSelectedLog()
            },
            onSave = { flow, pain, mood, symptoms, mucus, notes, hadIntercourse, protectionUsed,
                       method, intercourseNotes, showOnCalendar, sleepHours, energyLevel,
                       stressLevel, waterIntake, bbt, spotting ->
                viewModel.saveDailyLog(
                    day, flow, pain, mood, symptoms, mucus, notes,
                    hadIntercourse, protectionUsed, method, intercourseNotes, showOnCalendar,
                    sleepHours, energyLevel, stressLevel, waterIntake, bbt, spotting
                )
                selectedDayForLog = null
                scope.launch { snackbarHostState.showSnackbar(savedMsg) }
            }
        )
    }

    if (showHistory) LogHistorySheet(onDismiss = { showHistory = false })
    if (showCycleHistory) CycleHistorySheet(onDismiss = { showCycleHistory = false })
    if (showLegendDialog) LegendDialog(onDismiss = { showLegendDialog = false })

    pendingFutureDay?.let { day ->
        AlertDialog(
            onDismissRequest = { pendingFutureDay = null },
            title = { Text(stringResource(R.string.log_future_day_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.log_future_day_desc), fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    pendingFutureDay = null
                    viewModel.fetchLogForDay(day)
                    selectedDayForLog = day
                }) {
                    Text(stringResource(R.string.log_future_day_continue), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFutureDay = null }) {
                    Text(stringResource(R.string.dialog_reset_cancel))
                }
            }
        )
    }
}

@Composable
fun ModeToggleSwitch(mode: CalendarMode, onModeChange: (CalendarMode) -> Unit) {
    val bgAnim by animateColorAsState(MaterialTheme.colorScheme.surfaceVariant, label = "bg")
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgAnim)
            .padding(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToggleOption(
                text = stringResource(R.string.cal_mode_month),
                isSelected = mode == CalendarMode.MONTH,
                onClick = { onModeChange(CalendarMode.MONTH) }
            )
            ToggleOption(
                text = stringResource(R.string.cal_mode_year),
                isSelected = mode == CalendarMode.YEAR,
                onClick = { onModeChange(CalendarMode.YEAR) }
            )
        }
    }
}

@Composable
fun ToggleOption(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = tween(250), label = "toggle_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        animationSpec = tween(250), label = "toggle_text"
    )
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun CurrentPhasePill(currentPhase: CyclePhase, currentDayOfCycle: Int) {
    val phaseColor = phaseColor(currentPhase)
    val label = phaseLabel(currentPhase)
    
    Row(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(phaseColor.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(phaseColor))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.cal_current_phase_label, label),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        if (currentDayOfCycle > 0) {
            val rawDay = stringResource(R.string.cal_current_day_pill, currentDayOfCycle).trimStart(' ', '•', '·')
            Text(
                text = " · $rawDay",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun MonthView(
    uiState: CalendarUiState,
    viewModel: CalendarViewModel,
    onDayClick: (CalendarDay) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { viewModel.navigateToPreviousMonth() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = stringResource(R.string.cal_prev_month_desc),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    AnimatedContent(
                        targetState = uiState.monthYearTitle,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "month_title"
                    ) { title ->
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateToNextMonth() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = stringResource(R.string.cal_next_month_desc),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                val daysOfWeek = listOf(
                    R.string.cal_mon, R.string.cal_tue, R.string.cal_wed,
                    R.string.cal_thu, R.string.cal_fri, R.string.cal_sat, R.string.cal_sun
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { dayRes ->
                        Text(
                            text = stringResource(id = dayRes),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    // The parent scrolls vertically, so the non-scrolling grid needs a finite height.
                    val rowCount = (uiState.days.size + 6) / 7
                    val cellHeight = maxOf(48.dp, (maxWidth - 12.dp) / 7)
                    val gridHeight = cellHeight * rowCount + 8.dp
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth().height(gridHeight).padding(bottom = 8.dp),
                        userScrollEnabled = false
                    ) {
                        itemsIndexed(uiState.days) { index, calendarDay ->
                            if (calendarDay.isEmptyOffset) {
                                Box(modifier = Modifier.size(48.dp))
                            } else {
                                DayCell(
                                    day = calendarDay,
                                    index = index,
                                    onClick = { onDayClick(calendarDay) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YearView(
    initialYear: Int,
    viewModel: CalendarViewModel,
    onMonthClick: (year: Int, month: Int) -> Unit
) {
    // Solo se carga el año visible: el listado anterior disparaba 11×12
    // generaciones de calendario y mostraba una cascada de spinners.
    val tokens = com.jeremy.lumi.ui.theme.LumiThemeTokens.current
    var year by remember(initialYear) { mutableIntStateOf(initialYear) }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 23.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { year-- }) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.cal_prev_month_desc), tint = tokens.muted)
            }
            AnimatedContent(
                targetState = year,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(130)) },
                label = "lumi_annual_title"
            ) {
                Text(
                    it.toString(),
                    fontFamily = com.jeremy.lumi.ui.theme.LumiEditorialSerif,
                    fontSize = 31.sp,
                    color = tokens.text
                )
            }
            IconButton(onClick = { year++ }) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.cal_next_month_desc), tint = tokens.muted)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 21.dp, vertical = 8.dp)
        ) {
            item(key = year) {
                YearItem(
                    year = year,
                    viewModel = viewModel,
                    onMonthClick = { month -> onMonthClick(year, month) }
                )
            }
        }
    }
}

@Composable
fun YearItem(year: Int, viewModel: CalendarViewModel, onMonthClick: (Int) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val yearData = produceState<List<Pair<String, List<CalendarDay>>>>(
        initialValue = if (year == uiState.displayYear) uiState.yearMonthsData else emptyList(),
        year,
        uiState.displayMonth
    ) {
        value = if (year != uiState.displayYear || value.isEmpty()) {
            viewModel.getYearData(year)
        } else {
            uiState.yearMonthsData
        }
    }

    val tokens = com.jeremy.lumi.ui.theme.LumiThemeTokens.current
    Column(modifier = Modifier.fillMaxWidth()) {
        if (yearData.value.isEmpty()) {
            // Keep the 12-month hierarchy visible while the yearly data is generated.
            // A neutral placeholder avoids a blank annual page and jumping content.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) { rowIndex ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(3) { columnIndex ->
                            val index = rowIndex * 3 + columnIndex + 1
                            val month = java.time.Month.of(index).getDisplayName(
                                java.time.format.TextStyle.FULL, java.util.Locale.getDefault()
                            ).replaceFirstChar { it.uppercase() }
                            Column(
                                modifier = Modifier.weight(1f).height(114.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(tokens.panel.copy(alpha = .64f))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(month, fontSize = 11.sp, color = tokens.muted, maxLines = 1)
                                repeat(3) { line ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth(if (line == 2) .65f else 1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(tokens.line.copy(alpha = .50f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val rows = yearData.value.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEachIndexed { rowIndex, rowMonths ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowMonths.forEachIndexed { colIndex, monthPair ->
                            val globalMonthIndex = rowIndex * 3 + colIndex
                            SmallMonthBox(
                                modifier  = Modifier.weight(1f),
                                monthName = monthPair.first.split(" ")[0],
                                days      = monthPair.second,
                                monthIndex = globalMonthIndex + 1, // 1-based
                                displayYear = year,
                                onClick   = { onMonthClick(globalMonthIndex) }
                            )
                        }
                        // If the last row has < 3 months, pad with empty weights
                        repeat(3 - rowMonths.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmallMonthBox(
    modifier     : Modifier = Modifier,
    monthName    : String,
    days         : List<CalendarDay>,
    monthIndex   : Int,
    displayYear  : Int,
    onClick      : () -> Unit
) {
    val textMeasurer  = androidx.compose.ui.text.rememberTextMeasurer()
    val phaseColors   = LocalPhaseColors.current
    val tokens = com.jeremy.lumi.ui.theme.LumiThemeTokens.current
    val primaryColor  = tokens.accent
    val surfaceColor  = tokens.panel
    val onBgColor     = tokens.text
    val haptic        = LocalHapticFeedback.current

    // Detect if this month contains today
    val today         = com.jeremy.lumi.utils.LumiClock.today()
    val isCurrentMonth = displayYear == today.year && monthIndex == today.monthValue

    // Determine accent color: use menstrual color if any menstrual day, else primary
    val hasMenstrual = days.any { it.phase == CyclePhase.MENSTRUAL && !it.isEmptyOffset }
    val hasOvulation = days.any { it.phase == CyclePhase.OVULATION && !it.isEmptyOffset }
    val accentColor  = when {
        hasMenstrual -> phaseColors.menstrual
        hasOvulation -> phaseColors.ovulation
        else         -> primaryColor.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (isCurrentMonth)
                primaryColor.copy(alpha = 0.07f)
            else
                tokens.panel.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // ── Month name header bar ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isCurrentMonth)
                            Brush.horizontalGradient(
                                listOf(primaryColor.copy(alpha = 0.18f), primaryColor.copy(alpha = 0.06f))
                            )
                        else
                            Brush.horizontalGradient(
                                listOf(accentColor.copy(alpha = 0.12f), Color.Transparent)
                            )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text       = monthName,
                    fontSize   = 11.sp,
                    fontFamily = com.jeremy.lumi.ui.theme.LumiEditorialSerif,
                    fontWeight = FontWeight.Medium,
                    color      = if (isCurrentMonth) primaryColor
                                 else onBgColor.copy(alpha = 0.75f),
                    letterSpacing = 0.3.sp
                )
            }

            // ── Day grid rendered on Canvas ───────────────────────────────
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                val colWidth  = size.width / 7f
                val rowHeight = size.height / 6f

                days.forEachIndexed { index, day ->
                    if (!day.isEmptyOffset) {
                        val row = index / 7
                        val col = index % 7
                        val cx  = col * colWidth + colWidth / 2f
                        val cy  = row * rowHeight + rowHeight / 2f

                        val phaseColor = when (day.phase) {
                            CyclePhase.MENSTRUAL  -> phaseColors.menstrual
                            CyclePhase.FOLLICULAR -> phaseColors.follicular
                            CyclePhase.OVULATION  -> phaseColors.ovulation
                            CyclePhase.LUTEAL     -> phaseColors.luteal
                            CyclePhase.PREGNANCY  -> primaryColor
                            else                  -> null
                        }

                        val dotAlpha = if (day.isPrediction) 0.45f else 0.75f

                        // Background circle for phase days
                        if (phaseColor != null) {
                            drawCircle(
                                color  = phaseColor.copy(alpha = dotAlpha * 0.22f),
                                radius = rowHeight * 0.38f,
                                center = androidx.compose.ui.geometry.Offset(cx, cy)
                            )
                        }

                        // Today: solid primary circle
                        if (day.isToday) {
                            drawCircle(
                                color  = primaryColor,
                                radius = rowHeight * 0.36f,
                                center = androidx.compose.ui.geometry.Offset(cx, cy)
                            )
                        }

                        val textColor = when {
                            day.isToday      -> surfaceColor
                            phaseColor != null -> phaseColor.copy(alpha = dotAlpha)
                            else             -> onBgColor.copy(alpha = 0.55f)
                        }

                        val textLayout = textMeasurer.measure(
                            text  = day.dayOfMonth.toString(),
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize   = 8.sp,
                                fontWeight = if (day.isToday || phaseColor != null) FontWeight.Bold else FontWeight.Normal,
                                color      = textColor
                            )
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                x = cx - textLayout.size.width / 2f,
                                y = cy - textLayout.size.height / 2f
                            )
                        )

                        // Small phase dot below number for logged days
                        if (day.hasLog && phaseColor != null) {
                            drawCircle(
                                color  = phaseColor.copy(alpha = 0.85f),
                                radius = 1.8f,
                                center = androidx.compose.ui.geometry.Offset(cx, cy + rowHeight * 0.28f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.cal_legend_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem(phaseColor(CyclePhase.MENSTRUAL), phaseLabel(CyclePhase.MENSTRUAL))
                LegendItem(phaseColor(CyclePhase.OVULATION), phaseLabel(CyclePhase.OVULATION))
                
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = IntercourseHeartColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.cal_legend_intercourse),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val predColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .drawBehind {
                                drawCircle(
                                    color = predColor,
                                    radius = size.minDimension / 2f - 1f,
                                    style = Stroke(
                                        width = 1.5f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 2f))
                                    )
                                )
                            }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.cal_legend_prediction),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.cal_legend_explanation),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_understood), color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}

// DayCell remains unchanged visually from the user's setup
@Composable
fun DayCell(day: CalendarDay, index: Int, onClick: () -> Unit) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index % 7) * 18L + (index / 7) * 30L)
        enter.animateTo(1f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }

    val baseColor = phaseColor(day.phase)
    val isUnknown = day.phase == CyclePhase.UNKNOWN

    val predictionAlphaFactor = if (day.isPrediction) 0.45f else 1f

    val bgAlpha = when (day.phase) {
        CyclePhase.MENSTRUAL  -> 0.26f
        CyclePhase.OVULATION  -> 0.28f
        CyclePhase.FOLLICULAR -> 0f
        CyclePhase.LUTEAL     -> 0f
        else                  -> 0f
    } * predictionAlphaFactor

    val animBg by animateColorAsState(
        targetValue   = if (isUnknown) Color.Transparent else baseColor.copy(alpha = bgAlpha),
        animationSpec = tween(350),
        label         = "cell_bg_${day.dayOfMonth}"
    )

    val textColor = when {
        isUnknown         -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f)
        day.isPrediction  -> baseColor.copy(alpha = 0.55f)
        else              -> baseColor
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue   = if (isPressed) 0.78f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label         = "press_${day.dayOfMonth}"
    )

    val pulse = rememberInfiniteTransition(label = "pulse_${day.dayOfMonth}")
    val pulseAlpha by pulse.animateFloat(
        initialValue  = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label         = "pulse_a_${day.dayOfMonth}"
    )

    val primary      = MaterialTheme.colorScheme.primary
    val fertileBrush = Brush.radialGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.32f * predictionAlphaFactor),
            baseColor.copy(alpha = 0.08f * predictionAlphaFactor)
        )
    )

    val phaseName = phaseLabel(day.phase)
    val statusDesc = when {
        day.isToday && day.hasLog -> stringResource(R.string.cal_accessibility_today_with_log)
        day.isToday -> stringResource(R.string.cal_accessibility_today_no_log)
        day.isPrediction -> stringResource(R.string.cal_accessibility_prediction)
        day.hasLog -> stringResource(R.string.cal_accessibility_with_log)
        else -> stringResource(R.string.cal_accessibility_no_log)
    }
    val cellSemantics = stringResource(R.string.cal_accessibility_day_semantics, day.dayOfMonth, phaseName, statusDesc)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .semantics { contentDescription = cellSemantics }
            .graphicsLayer {
                alpha  = enter.value
                scaleX = (0.5f + 0.5f * enter.value) * pressScale
                scaleY = (0.5f + 0.5f * enter.value) * pressScale
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .clip(CircleShape)
                .then(
                    if (day.phase == CyclePhase.OVULATION) Modifier.background(fertileBrush)
                    else Modifier.background(animBg)
                )
                .then(
                    if (day.isPrediction && !isUnknown) {
                        val dotColor = baseColor.copy(alpha = 0.45f)
                        Modifier.drawBehind {
                            drawCircle(
                                color      = dotColor,
                                radius     = size.minDimension / 2f - 1f,
                                style      = Stroke(
                                    width      = 1.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))
                                )
                            )
                        }
                    } else Modifier
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (day.isToday) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = primary.copy(alpha = pulseAlpha),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                }
            }

            if (!isUnknown) {
                // Doble codificación geométrica de la fase
                val shapeColor = if (day.isPrediction) baseColor.copy(alpha = 0.5f) else baseColor
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                ) {
                    when (day.phase) {
                        CyclePhase.MENSTRUAL -> {
                            Box(
                                modifier = Modifier
                                    .size(3.5.dp)
                                    .clip(CircleShape)
                                    .background(shapeColor)
                            )
                        }
                        CyclePhase.FOLLICULAR -> {
                            Box(
                                modifier = Modifier
                                    .size(3.5.dp)
                                    .rotate(45f)
                                    .background(shapeColor)
                            )
                        }
                        CyclePhase.OVULATION -> {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .border(1.dp, shapeColor, CircleShape)
                            )
                        }
                        CyclePhase.LUTEAL -> {
                            Box(
                                modifier = Modifier
                                    .size(width = 5.dp, height = 2.5.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(shapeColor)
                            )
                        }
                        else -> Unit
                    }
                }
            }

            Text(
                text       = day.dayOfMonth.toString(),
                fontSize   = 13.sp,
                fontWeight = if (day.isToday || (!isUnknown && !day.isPrediction)) FontWeight.Bold
                else FontWeight.Normal,
                color      = if (day.isToday) primary else textColor,
                modifier   = Modifier.offset(y = if (day.hasLog) (-2).dp else 0.dp)
            )

            if (day.hasLog) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 5.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (day.isToday) primary
                            else baseColor.copy(alpha = if (isUnknown) 0.5f else 0.85f)
                        )
                )
            }
        }

        if (day.showIntercourseHeart) {
            Icon(
                imageVector        = if (day.intercourseProtected == true)
                    Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint               = IntercourseHeartColor,
                modifier           = Modifier
                    .size(14.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
fun phaseColor(phase: CyclePhase): Color {
    val phaseColors = LocalPhaseColors.current
    return when (phase) {
        CyclePhase.MENSTRUAL  -> phaseColors.menstrual
        CyclePhase.FOLLICULAR -> phaseColors.follicular
        CyclePhase.OVULATION  -> phaseColors.ovulation
        CyclePhase.LUTEAL     -> phaseColors.luteal
        CyclePhase.PREGNANCY  -> MaterialTheme.colorScheme.primary
        else                  -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)
    }
}

@Composable
fun phaseLabel(phase: CyclePhase): String = stringResource(
    id = when (phase) {
        CyclePhase.MENSTRUAL  -> R.string.phase_menstrual
        CyclePhase.OVULATION  -> R.string.phase_name_ovulation
        CyclePhase.LUTEAL     -> R.string.phase_luteal
        CyclePhase.FOLLICULAR -> R.string.phase_follicular
        CyclePhase.PREGNANCY  -> R.string.phase_pregnancy
        else                  -> R.string.phase_unknown
    }
)
