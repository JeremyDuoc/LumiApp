package com.jeremy.lumi.ui.screens.calendar

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
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
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLog by viewModel.selectedLog.collectAsStateWithLifecycle()
    
    var mode by remember { mutableStateOf(CalendarMode.MONTH) }
    var showLegendDialog by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showCycleHistory by remember { mutableStateOf(false) }
    var selectedDayForLog by remember { mutableStateOf<Int?>(null) }
    var pendingFutureDay by remember { mutableStateOf<Int?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMsg = stringResource(R.string.log_saved_confirmation)
    
    val haptic = LocalHapticFeedback.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        ModeToggleSwitch(mode = mode, onModeChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            mode = it 
                        })
                    },
                    navigationIcon = {
                        IconButton(onClick = { showLegendDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = stringResource(R.string.cal_legend_title_cd),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.goToToday()
                            mode = CalendarMode.MONTH
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Today,
                                contentDescription = stringResource(R.string.cal_today_cd),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showHistory = true }) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = stringResource(id = R.string.history_title),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showCycleHistory = true }) {
                            Icon(
                                imageVector = Icons.Rounded.DateRange,
                                contentDescription = "Historial de Ciclos",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                
                // Sleek Current Phase Pill (Only in Month view or always, let's keep it minimal)
                AnimatedVisibility(
                    visible = mode == CalendarMode.MONTH,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CurrentPhasePill(
                                currentPhase = uiState.currentPhase,
                                currentDayOfCycle = uiState.currentDayOfCycle
                            )
                            // FIX P2-2: Mostrar el banner de predicción del próximo período.
                            // uiState.prediction existe pero antes nunca se renderizaba.
                            uiState.prediction?.let { pred ->
                                val bannerText = when {
                                    pred.isLate -> stringResource(R.string.calendar_period_late, pred.delayDays)
                                    pred.daysUntilNextPeriod == 0 -> stringResource(R.string.calendar_period_today)
                                    pred.daysUntilNextPeriod == 1 -> stringResource(R.string.calendar_period_tomorrow)
                                    pred.daysUntilNextPeriod > 0 -> stringResource(R.string.calendar_next_period_in, pred.daysUntilNextPeriod)
                                    else -> null
                                }
                                val bannerColor = when {
                                    pred.isLate -> MaterialTheme.colorScheme.error
                                    pred.daysUntilNextPeriod <= 2 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                }
                                if (bannerText != null) {
                                    Text(
                                        text = bannerText,
                                        fontSize = 12.sp,
                                        color = bannerColor,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    if (targetState == CalendarMode.MONTH) {
                        // Zooming INTO month from year (Month starts small, Year grows huge)
                        (scaleIn(tween(450, easing = FastOutSlowInEasing), 0.8f) + fadeIn(tween(300))) togetherWith 
                        (scaleOut(tween(450, easing = FastOutSlowInEasing), 1.2f) + fadeOut(tween(300)))
                    } else {
                        // Zooming OUT TO year from month (Year starts huge, Month shrinks small)
                        (scaleIn(tween(450, easing = FastOutSlowInEasing), 1.2f) + fadeIn(tween(300))) togetherWith 
                        (scaleOut(tween(450, easing = FastOutSlowInEasing), 0.8f) + fadeOut(tween(300)))
                    }
                },
                label = "calendar_zoom_anim"
            ) { currentMode ->
                if (currentMode == CalendarMode.MONTH) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MonthView(
                            uiState = uiState,
                            viewModel = viewModel,
                            onDayClick = { calendarDay ->
                                if (calendarDay.isFuture) {
                                    pendingFutureDay = calendarDay.dayOfMonth
                                } else {
                                    viewModel.fetchLogForDay(calendarDay.dayOfMonth)
                                    selectedDayForLog = calendarDay.dayOfMonth
                                }
                            }
                        )

                        // Estado vacío del día: si hoy no tiene registro (y el mes
                        // visible contiene el día de hoy), invita a registrarlo.
                        val todayCell = uiState.days.firstOrNull { it.isToday }
                        AnimatedVisibility(
                            visible = todayCell != null && !todayCell.hasLog,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            com.jeremy.lumi.ui.components.LumiEmptyStateCard(
                                icon = Icons.Rounded.Today,
                                title = stringResource(R.string.empty_cal_today_title),
                                description = stringResource(R.string.empty_cal_today_desc),
                                ctaText = stringResource(R.string.empty_cal_today_cta),
                                onCtaClick = {
                                    todayCell?.let {
                                        viewModel.fetchLogForDay(it.dayOfMonth)
                                        selectedDayForLog = it.dayOfMonth
                                    }
                                }
                            )
                        }
                    }
                } else {
                    YearView(
                        initialYear = uiState.displayYear,
                        viewModel = viewModel,
                        onMonthClick = { year, month ->
                            viewModel.navigateToMonth(year, month)
                            mode = CalendarMode.MONTH
                        }
                    )
                }
            }
        }
    }

    // Bottom Sheets & Dialogs
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
            onSave = { flow, pain, mood, symptoms, mucus, notes, hadIntercourse, protectionUsed, method, intercourseNotes, showOnCalendar, sleepHours, energyLevel, stressLevel, waterIntake, bbt, spotting ->
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

    if (showHistory) {
        LogHistorySheet(onDismiss = { showHistory = false })
    }

    if (showCycleHistory) {
        CycleHistorySheet(onDismiss = { showCycleHistory = false })
    }

    if (showLegendDialog) {
        LegendDialog(onDismiss = { showLegendDialog = false })
    }

    pendingFutureDay?.let { day ->
        AlertDialog(
            onDismissRequest = { pendingFutureDay = null },
            title = { Text(text = stringResource(R.string.log_future_day_title), fontWeight = FontWeight.Bold) },
            text = { Text(text = stringResource(R.string.log_future_day_desc), fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    pendingFutureDay = null
                    viewModel.fetchLogForDay(day)
                    selectedDayForLog = day
                }) { Text(text = stringResource(R.string.log_future_day_continue), color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { pendingFutureDay = null }) { Text(stringResource(R.string.dialog_reset_cancel)) }
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
            Text(
                text = stringResource(R.string.cal_current_day_pill, currentDayOfCycle),
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
    onDayClick: (CalendarDay) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
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

@Composable
fun YearView(
    initialYear: Int,
    viewModel: CalendarViewModel,
    onMonthClick: (year: Int, month: Int) -> Unit
) {
    val yearsRange = (initialYear - 5)..(initialYear + 5)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 5)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        items(yearsRange.toList().size) { index ->
            val year = yearsRange.toList()[index]
            YearItem(
                year       = year,
                viewModel  = viewModel,
                onMonthClick = { month -> onMonthClick(year, month) }
            )
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

    val currentYear = com.jeremy.lumi.utils.LumiClock.today().year
    val isCurrentYear = year == currentYear

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── Year label ────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 14.dp, start = 4.dp)
        ) {
            if (isCurrentYear) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text       = year.toString(),
                fontSize   = if (isCurrentYear) 28.sp else 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = if (isCurrentYear)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
            )
        }

        if (yearData.value.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
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
    val primaryColor  = MaterialTheme.colorScheme.primary
    val surfaceColor  = MaterialTheme.colorScheme.surface
    val onBgColor     = MaterialTheme.colorScheme.onBackground
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
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (isCurrentMonth)
                primaryColor.copy(alpha = 0.07f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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
                    fontWeight = FontWeight.Bold,
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

    Box(
        modifier = Modifier
            .aspectRatio(1f)
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
        CyclePhase.OVULATION  -> R.string.phase_ovulation
        CyclePhase.LUTEAL     -> R.string.phase_luteal
        CyclePhase.FOLLICULAR -> R.string.phase_follicular
        CyclePhase.PREGNANCY  -> R.string.phase_pregnancy
        else                  -> R.string.phase_unknown
    }
)
