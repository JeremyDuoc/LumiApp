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

enum class CalendarMode { MONTH, YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLog by viewModel.selectedLog.collectAsStateWithLifecycle()
    
    var mode by remember { mutableStateOf(CalendarMode.MONTH) }
    var showLegendDialog by remember { mutableStateOf(false) }
    var selectedDayForLog by remember { mutableStateOf<Int?>(null) }
    var pendingFutureDay by remember { mutableStateOf<Int?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMsg = stringResource(R.string.log_saved_confirmation)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        ModeToggleSwitch(mode = mode, onModeChange = { mode = it })
                    },
                    navigationIcon = {
                        IconButton(onClick = { showLegendDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Leyenda",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            viewModel.goToToday()
                            mode = CalendarMode.MONTH
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Today,
                                contentDescription = "Hoy",
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
            onSave = { flow, pain, mood, symptoms, mucus, notes, hadIntercourse, protectionUsed, method, intercourseNotes, showOnCalendar, sleepHours, energyLevel, stressLevel, bbt, spotting ->
                viewModel.saveDailyLog(
                    day, flow, pain, mood, symptoms, mucus, notes,
                    hadIntercourse, protectionUsed, method, intercourseNotes, showOnCalendar,
                    sleepHours, energyLevel, stressLevel, bbt, spotting
                )
                selectedDayForLog = null
                scope.launch { snackbarHostState.showSnackbar(savedMsg) }
            }
        )
    }

    if (showHistory) {
        LogHistorySheet(onDismiss = { showHistory = false })
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
                text = "Mes",
                isSelected = mode == CalendarMode.MONTH,
                onClick = { onModeChange(CalendarMode.MONTH) }
            )
            ToggleOption(
                text = "Año",
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
            text = "Fase: $label",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        if (currentDayOfCycle > 0) {
            Text(
                text = " â€¢ Día $currentDayOfCycle",
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
                            contentDescription = "Mes anterior",
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
                            contentDescription = "Mes siguiente",
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
    // Generate a list of years around the current year
    val yearsRange = (initialYear - 5)..(initialYear + 5)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 5) // Start at initialYear

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        items(yearsRange.toList().size) { index ->
            val year = yearsRange.toList()[index]
            YearItem(year = year, viewModel = viewModel, onMonthClick = { month -> onMonthClick(year, month) })
        }
    }
}

@Composable
fun YearItem(year: Int, viewModel: CalendarViewModel, onMonthClick: (Int) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // FIX: Usar data precalculada para el año actual para que se reflejen los logs insertados.
    // Para otros años, recalcular basado en un cambio de mes (que gatilla una vista).
    val yearData = produceState<List<Pair<String, List<CalendarDay>>>>(
        initialValue = if (year == uiState.displayYear) uiState.yearMonthsData else emptyList(),
        year,
        uiState.displayMonth // trigger recomposition if state updates
    ) {
        if (year != uiState.displayYear || value.isEmpty()) {
            value = viewModel.getYearData(year)
        } else {
            value = uiState.yearMonthsData
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = year.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
        )
        
        if (yearData.value.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Grid of 12 months (3 cols x 4 rows)
            val rows = yearData.value.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                rows.forEachIndexed { rowIndex, rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowMonths.forEachIndexed { colIndex, monthPair ->
                            val globalMonthIndex = rowIndex * 3 + colIndex
                            SmallMonthBox(
                                modifier = Modifier.weight(1f),
                                monthName = monthPair.first.split(" ")[0], // "Enero"
                                days = monthPair.second,
                                onClick = { onMonthClick(globalMonthIndex) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmallMonthBox(modifier: Modifier = Modifier, monthName: String, days: List<CalendarDay>, onClick: () -> Unit) {
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val phaseColors = LocalPhaseColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBgColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = monthName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = onBgColor
        )
        Spacer(Modifier.height(6.dp))
        
        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            val rows = (days.size + 6) / 7
            val colWidth = size.width / 7f
            val rowHeight = size.height / 6f // always reserve 6 rows space for consistency

            days.forEachIndexed { index, day ->
                if (!day.isEmptyOffset) {
                    val row = index / 7
                    val col = index % 7
                    
                    val color = when (day.phase) {
                        CyclePhase.MENSTRUAL -> phaseColors.menstrual
                        CyclePhase.OVULATION -> phaseColors.ovulation
                        CyclePhase.PREGNANCY -> primaryColor
                        else -> onBgColor.copy(alpha = 0.35f)
                    }
                    val finalColor = if (day.isPrediction) color.copy(alpha = 0.5f) else color
                    val isBold = day.phase != CyclePhase.UNKNOWN || day.isToday
                    
                    val textLayout = textMeasurer.measure(
                        text = day.dayOfMonth.toString(),
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 10.sp,
                            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Normal,
                            color = finalColor
                        )
                    )
                    
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            x = (col * colWidth) + (colWidth - textLayout.size.width) / 2f,
                            y = (row * rowHeight) + (rowHeight - textLayout.size.height) / 2f
                        )
                    )
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
            Text(text = "Leyenda del Calendario", fontWeight = FontWeight.Bold)
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
                Text("Entendido", color = MaterialTheme.colorScheme.primary)
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
