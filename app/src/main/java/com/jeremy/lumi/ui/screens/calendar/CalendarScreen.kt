package com.jeremy.lumi.ui.screens.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState     by viewModel.uiState.collectAsState()
    val selectedLog by viewModel.selectedLog.collectAsState()
    var selectedDayForLog  by remember { mutableStateOf<Int?>(null) }
    var pendingFutureDay   by remember { mutableStateOf<Int?>(null) }
    var showHistory        by remember { mutableStateOf(false) }
    val snackbarHostState  = remember { SnackbarHostState() }
    val scope              = rememberCoroutineScope()
    val savedMsg           = stringResource(R.string.log_saved_confirmation)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text       = stringResource(id = R.string.nav_calendar),
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(
                            imageVector        = Icons.Rounded.History,
                            contentDescription = stringResource(id = R.string.history_title),
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            CyclePhaseBanner(
                currentPhase      = uiState.currentPhase,
                currentDayOfCycle = uiState.currentDayOfCycle,
                prediction        = uiState.prediction
            )

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(28.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick  = { viewModel.navigateToPreviousMonth() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Rounded.ChevronLeft,
                                contentDescription = "Mes anterior",
                                tint               = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }

                        AnimatedContent(
                            targetState    = uiState.monthYearTitle,
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                            label          = "month_title"
                        ) { title ->
                            Text(
                                text       = title,
                                fontSize   = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        IconButton(
                            onClick  = { viewModel.navigateToNextMonth() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Rounded.ChevronRight,
                                contentDescription = "Mes siguiente",
                                tint               = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
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
                                text       = stringResource(id = dayRes),
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f),
                                modifier   = Modifier.weight(1f),
                                textAlign  = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns               = GridCells.Fixed(7),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement   = Arrangement.spacedBy(6.dp),
                        modifier              = Modifier.height(316.dp),
                        userScrollEnabled     = false
                    ) {
                        itemsIndexed(uiState.days) { index, calendarDay ->
                            if (calendarDay.isEmptyOffset) {
                                Box(modifier = Modifier.size(48.dp))
                            } else {
                                DayCell(
                                    day     = calendarDay,
                                    index   = index,
                                    onClick = {
                                        if (calendarDay.isFuture) {
                                            // Mostrar advertencia antes de abrir el sheet
                                            pendingFutureDay = calendarDay.dayOfMonth
                                        } else {
                                            viewModel.fetchLogForDay(calendarDay.dayOfMonth)
                                            selectedDayForLog = calendarDay.dayOfMonth
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            PhaseLegend()
            Spacer(Modifier.height(4.dp))
        }
    }

    val activeCategories by viewModel.activeCategories.collectAsState()

    selectedDayForLog?.let { day ->
        DailyLogSheet(
            day      = day,
            month    = uiState.displayMonth,
            year     = uiState.displayYear,
            savedLog = selectedLog,
            activeCategories = activeCategories,
            onActiveCategoriesChange = { viewModel.setActiveCategories(it) },
            onDismiss = {
                selectedDayForLog = null
                viewModel.clearSelectedLog()
            },
            onSave = { flow, pain, mood, symptoms, mucus, notes, hadIntercourse, protectionUsed, method, intercourseNotes, showOnCalendar ->
                viewModel.saveDailyLog(
                    day, flow, pain, mood, symptoms, mucus, notes,
                    hadIntercourse, protectionUsed, method, intercourseNotes, showOnCalendar
                )
                selectedDayForLog = null
                scope.launch { snackbarHostState.showSnackbar(savedMsg) }
            }
        )
    }

    if (showHistory) {
        LogHistorySheet(onDismiss = { showHistory = false })
    }

    // ── Diálogo de advertencia para días futuros ─────────────────────────────
    pendingFutureDay?.let { day ->
        AlertDialog(
            onDismissRequest = { pendingFutureDay = null },
            title = {
                Text(
                    text       = stringResource(R.string.log_future_day_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text  = {
                Text(
                    text      = stringResource(R.string.log_future_day_desc),
                    fontSize  = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingFutureDay = null
                        viewModel.fetchLogForDay(day)
                        selectedDayForLog = day
                    }
                ) {
                    Text(
                        text  = stringResource(R.string.log_future_day_continue),
                        color = MaterialTheme.colorScheme.primary
                    )
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

// ─────────────────────────────────────────────────────────────────────────────
//  BANNER DE FASE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CyclePhaseBanner(
    currentPhase      : CyclePhase,
    currentDayOfCycle : Int,
    prediction        : CyclePrediction?
) {
    val todayPhase = currentPhase
    val phaseColor = phaseColor(todayPhase)

    val animBg by animateColorAsState(
        targetValue   = phaseColor.copy(alpha = 0.12f),
        animationSpec = tween(500), label = "banner_bg"
    )
    val animBorder by animateColorAsState(
        targetValue   = phaseColor.copy(alpha = 0.30f),
        animationSpec = tween(500), label = "banner_border"
    )

    val nextPhaseInfo: Pair<CyclePhase, Int>? = remember(prediction, todayPhase) {
        prediction?.let { p ->
            when (todayPhase) {
                CyclePhase.MENSTRUAL  -> Pair(CyclePhase.FOLLICULAR, p.periodLength - p.currentDayOfCycle + 1)
                CyclePhase.FOLLICULAR -> Pair(CyclePhase.OVULATION,  p.daysUntilOvulation)
                CyclePhase.OVULATION  -> Pair(CyclePhase.LUTEAL,     3 - (p.currentDayOfCycle - (p.cycleLength - 14 - 1)))
                CyclePhase.LUTEAL     -> Pair(CyclePhase.MENSTRUAL,  p.daysUntilNextPeriod)
                else                  -> null
            }?.takeIf { it.second >= 0 }
        }
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = animBg),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = androidx.compose.foundation.BorderStroke(1.dp, animBorder)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(phaseColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(phaseColor))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = stringResource(id = R.string.cal_current_phase),
                    fontSize      = 11.sp,
                    color         = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 0.4.sp
                )
                Text(
                    text       = phaseLabel(todayPhase),
                    fontSize   = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                if (currentDayOfCycle > 0) {
                    val cycleLen = prediction?.cycleLength ?: 28
                    Text(
                        text     = stringResource(id = R.string.cal_cycle_day, currentDayOfCycle, cycleLen),
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            nextPhaseInfo?.let { (nextPhase, daysLeft) ->
                val chipColor = phaseColor(nextPhase)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(chipColor.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text       = if (daysLeft <= 1) stringResource(id = R.string.cal_next_phase_tomorrow)
                        else stringResource(id = R.string.cal_next_phase_days, daysLeft),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = chipColor,
                        textAlign  = TextAlign.Center
                    )
                    Text(
                        text      = phaseLabel(nextPhase),
                        fontSize  = 10.sp,
                        color     = chipColor.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  LEYENDA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhaseLegend() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(phaseColor(CyclePhase.MENSTRUAL),  phaseLabel(CyclePhase.MENSTRUAL))
            LegendItem(phaseColor(CyclePhase.FOLLICULAR), phaseLabel(CyclePhase.FOLLICULAR))
            LegendItem(phaseColor(CyclePhase.OVULATION),  phaseLabel(CyclePhase.OVULATION))
            LegendItem(phaseColor(CyclePhase.LUTEAL),     phaseLabel(CyclePhase.LUTEAL))
        }
        Row(
            modifier          = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leyenda corazón
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint               = IntercourseHeartColor,
                    modifier           = Modifier.size(10.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text     = stringResource(id = R.string.cal_legend_intercourse),
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
            // Leyenda predicción — círculo punteado pequeño
            Row(verticalAlignment = Alignment.CenterVertically) {
                val predColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .drawBehind {
                            drawCircle(
                                color       = predColor,
                                radius      = size.minDimension / 2f - 1f,
                                style       = Stroke(
                                    width       = 1.5f,
                                    pathEffect  = PathEffect.dashPathEffect(floatArrayOf(3f, 2f))
                                )
                            )
                        }
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text     = stringResource(id = R.string.cal_legend_prediction),
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
        }
        Text(
            text      = stringResource(id = R.string.cal_legend_explanation),
            fontSize  = 11.sp,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(
            text     = label,
            fontSize = 11.sp,
            color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DAY CELL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DayCell(day: CalendarDay, index: Int, onClick: () -> Unit) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index % 7) * 18L + (index / 7) * 30L)
        enter.animateTo(1f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }

    val baseColor = phaseColor(day.phase)
    val isUnknown = day.phase == CyclePhase.UNKNOWN

    // Los días de predicción usan opacidad reducida para indicar que es estimado
    val predictionAlphaFactor = if (day.isPrediction) 0.45f else 1f

    val bgAlpha = when (day.phase) {
        CyclePhase.MENSTRUAL  -> 0.26f
        CyclePhase.OVULATION  -> 0.28f
        CyclePhase.FOLLICULAR -> 0.13f
        CyclePhase.LUTEAL     -> 0.15f
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

    // Box exterior: 48 dp, sin clip — el corazón vive aquí y nunca se recorta
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                alpha  = enter.value
                scaleX = (0.5f + 0.5f * enter.value) * pressScale
                scaleY = (0.5f + 0.5f * enter.value) * pressScale
            },
        contentAlignment = Alignment.Center
    ) {
        // Círculo visual: 44 dp, con clip
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (day.phase == CyclePhase.OVULATION) Modifier.background(fertileBrush)
                    else Modifier.background(animBg)
                )
                // Borde punteado para días de predicción
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

        // Corazón: hermano del círculo, fuera del clip — siempre visible completo
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

// ─────────────────────────────────────────────────────────────────────────────
//  HELPERS — colores de fase, resueltos desde LocalPhaseColors (preset elegido
//  en Settings o paleta personalizada de la usuaria)
// ─────────────────────────────────────────────────────────────────────────────

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