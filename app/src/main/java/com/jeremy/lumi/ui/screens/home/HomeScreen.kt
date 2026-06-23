package com.jeremy.lumi.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.jeremy.lumi.domain.usecase.DelayState
import com.jeremy.lumi.ui.screens.calendar.DailyLogSheet
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val STAGGER_MS = 80
private const val ENTER_MS   = 420
private const val BREATH_MS  = 3800

private val gentleSpring = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
private val snappySpring = spring<Float>(Spring.DampingRatioLowBouncy,    Spring.StiffnessMedium)

// ─────────────────────────────────────────────────────────────────────────────
//  HOME SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel           : HomeViewModel = hiltViewModel(),
    remindersViewModel  : RemindersViewModel = hiltViewModel(),
    onNavigateToInsights: () -> Unit = {}
) {
    val uiState           by viewModel.uiState.collectAsState()
    var showBalloonGame   by remember { mutableStateOf(false) }
    var showQuickLogSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    var screenReady by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) { delay(60); screenReady = true }
    }

    // Sincronizar recordatorios de ciclo cada vez que cambia la predicción
    LaunchedEffect(uiState.prediction) {
        uiState.prediction?.let {
            remindersViewModel.syncCycleReminders(it)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleDiscreetMode() }) {
                        val icon = if (uiState.isDiscreetMode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = uiState.currentPhase == CyclePhase.MENSTRUAL ||
                            uiState.currentPhase == CyclePhase.UNKNOWN,
                    enter = fadeIn(tween(300)) + scaleIn(
                        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), 0.7f),
                    exit  = fadeOut(tween(200)) + scaleOut(targetScale = 0.7f)
                ) {
                    SmallFloatingActionButton(
                        onClick        = { showBalloonGame = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor   = MaterialTheme.colorScheme.onSecondaryContainer
                    ) { Icon(Icons.Rounded.FavoriteBorder, stringResource(R.string.fab_cramps)) }
                }
                Spacer(Modifier.height(12.dp))
                FloatingActionButton(
                    onClick        = { showQuickLogSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Rounded.Add, stringResource(R.string.log_title)) }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                FadeSlideIn(screenReady, 0) { GreetingHeader() }

                var showConfirmPeriod by remember { mutableStateOf(false) }

                // ── Banner de retraso — aparece solo cuando es necesario ────
                AnimatedVisibility(
                    visible = uiState.isLate,
                    enter   = fadeIn(tween(400)) + expandVertically(),
                    exit    = fadeOut(tween(300)) + shrinkVertically()
                ) {
                    DelayBanner(
                        delayDays    = uiState.delayDays,
                        delayState   = uiState.delayState,
                        onPeriodArrived = { showConfirmPeriod = true },
                        onDismiss       = { viewModel.dismissDelayBanner() }
                    )
                }

                if (uiState.weekDays.isNotEmpty()) {
                    FadeSlideIn(screenReady, STAGGER_MS)     { CycleWeekStrip(uiState.weekDays, uiState.isDiscreetMode) }
                    FadeSlideIn(screenReady, STAGGER_MS * 2) { PhaseLegend(uiState.isDiscreetMode) }
                }

                FadeSlideIn(screenReady, STAGGER_MS * 3) {
                    CycleRingCard(uiState.currentDayOfCycle, uiState.currentPhase, uiState.prediction, uiState.isDiscreetMode)
                }

                // Botón principal: texto cambia si hay retraso
                FadeSlideIn(screenReady, STAGGER_MS * 3) {
                    val btnText = if (uiState.isLate) stringResource(R.string.period_arrived_btn) 
                                  else stringResource(R.string.register_period_btn)
                    
                    Button(
                        onClick = { showConfirmPeriod = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isLate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            contentColor = if (uiState.isLate) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        val icon = if (uiState.isLate) Icons.Rounded.Warning 
                                   else Icons.Rounded.Add
                        Icon(icon, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(btnText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (showConfirmPeriod) {
                    AlertDialog(
                        onDismissRequest = { showConfirmPeriod = false },
                        title = { Text(stringResource(R.string.confirm_period_title), fontWeight = FontWeight.Bold) },
                        text = { Text(stringResource(R.string.confirm_period_desc)) },
                        confirmButton = {
                            Button(
                                onClick = { 
                                    showConfirmPeriod = false
                                    viewModel.startNewPeriod()
                                }
                            ) { Text(stringResource(R.string.confirm_period_yes)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmPeriod = false }) { Text(stringResource(R.string.confirm_period_cancel)) }
                        }
                    )
                }

                if (uiState.prediction != null && uiState.currentDayOfCycle > 0) {
                    FadeSlideIn(screenReady, STAGGER_MS * 4) {
                        PhaseTimelineCard(uiState.currentPhase, uiState.prediction!!, uiState.isDiscreetMode)
                    }
                }

                FadeSlideIn(screenReady, STAGGER_MS * 5) { RemindersSection() }

                FadeSlideIn(screenReady, STAGGER_MS * 6) {
                    // Botón secundario: ir a estadísticas
                    OutlinedButton(
                        onClick  = onNavigateToInsights,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(18.dp),
                        border   = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Rounded.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint     = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = stringResource(R.string.insights_btn_home),
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                    }
                }



                Spacer(Modifier.height(96.dp))
            }
        }
    }

    if (showBalloonGame)   BalloonGameSheet { showBalloonGame = false }
    if (showQuickLogSheet) {
        val quickLogViewModel: QuickLogViewModel = hiltViewModel()
        val todayLog by quickLogViewModel.todayLog.collectAsState()
        val activeCategories by quickLogViewModel.activeCategories.collectAsState()
        
        DailyLogSheet(
            day      = quickLogViewModel.todayDayOfMonth,
            month    = quickLogViewModel.todayMonth,
            year     = quickLogViewModel.todayYear,
            savedLog = todayLog,
            activeCategories = activeCategories,
            onActiveCategoriesChange = { quickLogViewModel.setActiveCategories(it) },
            onDismiss = { showQuickLogSheet = false },
            onSave    = { flow, pain, mood, symptoms, mucus, notes, hadIntercourse,
                          protectionUsed, method, intercourseNotes, showOnCalendar ->
                quickLogViewModel.saveToday(flow, pain, mood, symptoms, mucus, notes,
                    hadIntercourse, protectionUsed, method, intercourseNotes, showOnCalendar)
                showQuickLogSheet = false
                scope.launch { snackbarHostState.showSnackbar("✓ Registro guardado") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DELAY BANNER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DelayBanner(
    delayDays       : Int,
    delayState      : DelayState,
    onPeriodArrived : () -> Unit,
    onDismiss       : () -> Unit
) {
    // Color e intensidad del mensaje según la gravedad del retraso
    val (bgColor, textColor, icon) = when (delayState) {
        DelayState.LATE          -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "~"
        )
        DelayState.VERY_LATE     -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "!"
        )
        DelayState.EXTREMELY_LATE -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "!!"
        )
        DelayState.ON_TIME       -> Triple(Color.Transparent, Color.Transparent, "")
    }

    val message = when (delayState) {
        DelayState.LATE           ->
            "Tu regla lleva $delayDays ${if (delayDays == 1) "día" else "días"} de retraso. Es normal que ocurra."
        DelayState.VERY_LATE      ->
            "$delayDays días de retraso. El estrés, cambios de peso o ejercicio intenso pueden causarlo."
        DelayState.EXTREMELY_LATE ->
            "$delayDays días de retraso. Si descartaste un embarazo, consulta a tu ginecóloga."
        DelayState.ON_TIME        -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Text(
                    text       = message,
                    fontSize   = 14.sp,
                    color      = textColor,
                    lineHeight = 20.sp,
                    modifier   = Modifier.weight(1f)
                )
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier.size(28.dp).offset(x = 8.dp, y = (-4).dp)
                ) {
                    Icon(Icons.Rounded.Close, null,
                        tint     = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp))
                }
            }

            // Botón "mi regla llegó hoy"
            OutlinedButton(
                onClick = onPeriodArrived,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                border   = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text       = stringResource(R.string.period_arrived_btn),
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  El resto de composables sin cambios respecto a la versión anterior
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FadeSlideIn(visible: Boolean, delayMs: Int, content: @Composable () -> Unit) {
    val alpha  = remember { Animatable(0f) }
    val offset = remember { Animatable(12f) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMs.toLong())
            launch { alpha.animateTo(1f, tween(ENTER_MS, easing = FastOutSlowInEasing)) }
            launch { offset.animateTo(0f, gentleSpring) }
        }
    }
    Box(Modifier.alpha(alpha.value).graphicsLayer { translationY = offset.value.dp.toPx() }) {
        content()
    }
}

@Composable
private fun GreetingHeader() {
    // Lee el nombre guardado en el onboarding (null si no lo dio)
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs   = remember {
        OnboardingPreferenceManager(context.applicationContext)
    }
    val userName by prefs.userNameFlow.collectAsState(initial = null)

    val greeting = if (!userName.isNullOrBlank()) "Hola, $userName 👋" else "Hola 👋"

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(greeting, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text(stringResource(R.string.home_subtitle), fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun CycleWeekStrip(days: List<CycleDayUi>, isDiscreetMode: Boolean, onDayClick: ((CycleDayUi) -> Unit)? = null) {
    val listState  = rememberLazyListState()
    val todayIndex = remember(days) { days.indexOfFirst { it.isToday }.coerceAtLeast(0) }
    LaunchedEffect(todayIndex) { listState.scrollToItem(todayIndex, scrollOffset = -220) }
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)) {
        items(days, key = { it.date.toString() }) { DayChip(it, isDiscreetMode, onDayClick) }
    }
}

@Composable
private fun DayChip(day: CycleDayUi, isDiscreetMode: Boolean, onDayClick: ((CycleDayUi) -> Unit)? = null) {
    val phaseColor  = if (isDiscreetMode) Color.Gray.copy(alpha = 0.5f) else homePhaseColor(day.phase)
    val interSource = remember { MutableInteractionSource() }
    val isPressed   by interSource.collectIsPressedAsState()
    val chipScale   by animateFloatAsState(if (isPressed) 0.93f else 1f, snappySpring, label = "chip")
    val bg          = if (day.isToday) phaseColor else phaseColor.copy(alpha = 0.10f)
    val numColor    = if (day.isToday) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.onBackground

    // Días futuros: el punto inferior muestra el color de fase como marcador predictivo
    val isFuture     = !day.isToday && day.date.isAfter(java.time.LocalDate.now())
    val dotColor     = when {
        day.isToday -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        isFuture    -> phaseColor.copy(alpha = 0.75f)   // Marcador predictivo vivo
        else        -> phaseColor.copy(alpha = 0.35f)   // Pasado, atenuado
    }
    val dotSize      = if (isFuture) 6.dp else 5.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(chipScale)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .then(
                if (onDayClick != null) Modifier.clickable(interactionSource = interSource, indication = null) { onDayClick(day) }
                else Modifier
            )
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Text(
            text      = day.weekdayLabel,
            fontSize  = 11.sp,
            fontWeight = FontWeight.Medium,
            color     = numColor.copy(alpha = if (day.isToday) 0.85f else 0.55f)
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text       = day.dayOfMonth.toString(),
            fontSize   = 17.sp,
            fontWeight = FontWeight.Bold,
            color      = numColor
        )
        Spacer(Modifier.height(7.dp))
        // Punto de fase: más grande y vivo en días futuros (marcador predictivo)
        Box(
            Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}

@Composable
private fun PhaseLegend(isDiscreetMode: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        val colorMens = if (isDiscreetMode) Color.Gray.copy(alpha = 0.5f) else homePhaseColor(CyclePhase.MENSTRUAL)
        val colorFoli = if (isDiscreetMode) Color.Gray.copy(alpha = 0.5f) else homePhaseColor(CyclePhase.FOLLICULAR)
        val colorOvu  = if (isDiscreetMode) Color.Gray.copy(alpha = 0.5f) else homePhaseColor(CyclePhase.OVULATION)
        val colorLut  = if (isDiscreetMode) Color.Gray.copy(alpha = 0.5f) else homePhaseColor(CyclePhase.LUTEAL)

        // En modo discreto, las 4 fases tienen etiquetas distintas (A, B, C, D)
        // para que la leyenda siga siendo útil sin revelar información íntima.
        val labelMens = if (isDiscreetMode) stringResource(R.string.discreet_phase_1) else stringResource(R.string.phase_menstrual)
        val labelFoli = if (isDiscreetMode) stringResource(R.string.discreet_phase_2) else stringResource(R.string.phase_follicular)
        val labelOvu  = if (isDiscreetMode) stringResource(R.string.discreet_phase_3) else stringResource(R.string.phase_ovulation)
        val labelLut  = if (isDiscreetMode) stringResource(R.string.discreet_phase_4) else stringResource(R.string.phase_luteal)

        LegendItem(colorMens, labelMens)
        LegendItem(colorFoli, labelFoli)
        LegendItem(colorOvu,  labelOvu)
        LegendItem(colorLut,  labelLut)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}

@Composable
fun CycleRingCard(currentDay: Int, phase: CyclePhase, prediction: CyclePrediction?, isDiscreetMode: Boolean) {
    val phaseColors        = LocalPhaseColors.current
    val actualPhaseColor   = homePhaseColor(phase)
    val phaseColor         = if (isDiscreetMode) Color.Gray.copy(alpha = 0.6f) else actualPhaseColor
    val animatedPhaseColor by animateColorAsState(phaseColor, tween(700), label = "phase_color")

    val cycleLen   = (prediction?.cycleLength  ?: 28)
    val periodLen  = (prediction?.periodLength  ?: 5)
    val cycleLenF  = cycleLen.toFloat()

    // ── Ángulos de cada segmento (en grados, partiendo de -90°) ────────────
    // Menstrual: días 1..periodLen
    // Folicular: días (periodLen+1)..(ovDay-2)
    // Ovulación: días (ovDay-1)..(ovDay+1)   [ventana ±1]
    // Lútea:     días (ovDay+2)..cycleLen
    val ovDay         = cycleLen - 14
    val menSweep      = (periodLen.toFloat()    / cycleLenF) * 360f
    val folStart      = -90f + menSweep
    val folSweep      = ((ovDay - 2 - periodLen).toFloat().coerceAtLeast(1f) / cycleLenF) * 360f
    val ovSweep       = (3f / cycleLenF) * 360f
    val lutSweep      = ((cycleLen - (ovDay + 1)).toFloat().coerceAtLeast(1f) / cycleLenF) * 360f

    // ── Progreso actual (tick) ───────────────────────────────────────────────
    val targetProgress = if (currentDay > 0) (currentDay / cycleLenF).coerceIn(0f, 1f) else 0f
    val animProgress   = remember { Animatable(0f) }
    LaunchedEffect(currentDay) {
        animProgress.animateTo(targetProgress, tween(1100, easing = FastOutSlowInEasing))
    }

    val dayScale = remember { Animatable(0.6f) }
    LaunchedEffect(currentDay) {
        dayScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }
    val breathing   = rememberInfiniteTransition(label = "breath")
    val breathScale by breathing.animateFloat(0.990f, 1.010f,
        infiniteRepeatable(tween(BREATH_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse), "bs")
    val haloAlpha   by breathing.animateFloat(0.04f, 0.12f,
        infiniteRepeatable(tween(BREATH_MS + 200, easing = LinearEasing), RepeatMode.Reverse), "ha")

    // ── Fondo tintado según la fase actual (muy sutil) ──────────────────────
    val cardBg by animateColorAsState(
        phaseColor.copy(alpha = 0.07f), tween(700), label = "card_bg"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(32.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Fondo tintado superpuesto dentro de la Card
        Box(
            Modifier
                .fillMaxWidth()
                .background(cardBg)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 28.dp, start = 28.dp, end = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Anillo ────────────────────────────────────────────────────
                Box(
                    Modifier
                        .size(220.dp)
                        .graphicsLayer { scaleX = breathScale; scaleY = breathScale },
                    contentAlignment = Alignment.Center
                ) {
                    // Halo de resplandor
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            animatedPhaseColor.copy(alpha = haloAlpha),
                            size.minDimension / 2f * 1.07f
                        )
                    }

                    // Anillo segmentado + tick de progreso
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 18f
                        val inset  = stroke / 2f
                        val arcSz  = Size(size.width - stroke, size.height - stroke)
                        val tl     = Offset(inset, inset)
                        val gap    = 2.5f   // gap visual entre segmentos (°)

                        // Segmento Menstrual
                        drawArc(phaseColors.menstrual, -90f, menSweep - gap, false, tl, arcSz,
                            style = Stroke(stroke, cap = StrokeCap.Round))
                        // Segmento Folicular
                        drawArc(phaseColors.follicular, folStart + gap / 2f, folSweep - gap, false, tl, arcSz,
                            style = Stroke(stroke, cap = StrokeCap.Round))
                        // Segmento Ovulación
                        val ovStart = folStart + folSweep + gap / 2f
                        drawArc(phaseColors.ovulation, ovStart, ovSweep - gap, false, tl, arcSz,
                            style = Stroke(stroke, cap = StrokeCap.Round))
                        // Segmento Lútea
                        val lutStart = ovStart + ovSweep + gap / 2f
                        drawArc(phaseColors.luteal, lutStart, lutSweep - gap, false, tl, arcSz,
                            style = Stroke(stroke, cap = StrokeCap.Round))

                        // Tick de progreso (punto blanco con borde de color de fase)
                        if (animProgress.value > 0f) {
                            val tickAngle = Math.toRadians(
                                (-90.0 + 360.0 * animProgress.value)
                            ).toFloat()
                            val r = (size.minDimension - stroke) / 2f
                            val cx = center.x + r * kotlin.math.cos(tickAngle)
                            val cy = center.y + r * kotlin.math.sin(tickAngle)
                            // Borde exterior coloreado
                            drawCircle(animatedPhaseColor, stroke / 2f * 1.6f, Offset(cx, cy))
                            // Centro blanco
                            drawCircle(androidx.compose.ui.graphics.Color.White,
                                stroke / 2f * 0.85f, Offset(cx, cy))
                        }
                    }

                    // Texto central: número de día + etiqueta
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer {
                            scaleX = dayScale.value; scaleY = dayScale.value
                        }
                    ) {
                        Text(
                            text       = if (currentDay > 0) "$currentDay" else "–",
                            fontSize   = 58.sp,
                            fontWeight = FontWeight.Bold,
                            color      = animatedPhaseColor,
                            lineHeight = 58.sp
                        )
                        Text(
                            text       = if (currentDay > 0)
                                stringResource(R.string.day_of_cycle)
                            else
                                stringResource(R.string.inactive_cycle),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(26.dp))

                // Píldora de fase actual
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(animatedPhaseColor.copy(alpha = 0.15f))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text       = if (isDiscreetMode) stringResource(R.string.discreet_observing) else stringResource(phase.phaseNameRes),
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = animatedPhaseColor
                    )
                }

                if (!isDiscreetMode) {
                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                        thickness = 0.5.dp
                    )
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text      = stringResource(phase.descriptionRes),
                        fontSize  = 14.sp,
                        color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PhaseTimelineCard(currentPhase: CyclePhase, p: CyclePrediction, isDiscreetMode: Boolean) {
    val ovulationDay = p.cycleLength - 14
    val cd           = p.currentDayOfCycle

    fun daysUntilStart(startDayInCycle: Int): Int {
        val diff = startDayInCycle - cd
        return if (diff > 0) diff else diff + p.cycleLength
    }

    data class PhaseEvent(val phase: CyclePhase, val daysUntil: Int)
    val events = listOf(
        PhaseEvent(CyclePhase.MENSTRUAL,  daysUntilStart(1)),
        PhaseEvent(CyclePhase.FOLLICULAR, daysUntilStart(p.periodLength + 1)),
        PhaseEvent(CyclePhase.OVULATION,  daysUntilStart(ovulationDay - 1)),
        PhaseEvent(CyclePhase.LUTEAL,     daysUntilStart(ovulationDay + 2))
    ).filter { it.phase != currentPhase }.sortedBy { it.daysUntil }

    Card(Modifier.fillMaxWidth(), RoundedCornerShape(28.dp),
        CardDefaults.cardColors(MaterialTheme.colorScheme.surface), CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(stringResource(R.string.home_upcoming_phases), fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                letterSpacing = 0.3.sp)
            Spacer(Modifier.height(14.dp))
            events.forEachIndexed { index, event ->
                PhaseEventRow(event.phase, event.daysUntil, isDiscreetMode)
                if (index < events.lastIndex) HorizontalDivider(
                    Modifier.padding(top = 10.dp, bottom = 10.dp, start = 44.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun PhaseEventRow(phase: CyclePhase, daysUntil: Int, isDiscreetMode: Boolean) {
    val color = if (isDiscreetMode) Color.Gray.copy(alpha = 0.5f) else homePhaseColor(phase)
    val timeLabel = when (daysUntil) {
        0    -> stringResource(R.string.predict_today)
        1    -> stringResource(R.string.cal_next_phase_tomorrow)
        else -> stringResource(R.string.cal_next_phase_days, daysUntil)
    }
    val phaseLabel = if (isDiscreetMode) {
        stringResource(R.string.discreet_observing)
    } else {
        when (phase) {
            CyclePhase.MENSTRUAL  -> stringResource(R.string.phase_menstrual)
            CyclePhase.FOLLICULAR -> stringResource(R.string.phase_follicular)
            CyclePhase.OVULATION  -> stringResource(R.string.phase_ovulation)
            CyclePhase.LUTEAL     -> stringResource(R.string.phase_luteal)
            CyclePhase.PREGNANCY  -> stringResource(R.string.phase_pregnancy)
            else                  -> ""
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        }
        Spacer(Modifier.width(12.dp))
        Text(phaseLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(timeLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    val interSource = remember { MutableInteractionSource() }
    val isPressed   by interSource.collectIsPressedAsState()
    val btnScale    by animateFloatAsState(if (isPressed) 0.97f else 1f, snappySpring, label = "btn")
    Button(onClick = onClick,
        modifier          = Modifier.fillMaxWidth().height(56.dp).scale(btnScale),
        shape             = RoundedCornerShape(18.dp),
        colors            = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary),
        interactionSource = interSource,
        elevation         = ButtonDefaults.buttonElevation(0.dp, 0.dp)
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun homePhaseColor(phase: CyclePhase): Color {
    val phaseColors = LocalPhaseColors.current
    return when (phase) {
        CyclePhase.MENSTRUAL  -> phaseColors.menstrual
        CyclePhase.FOLLICULAR -> phaseColors.follicular
        CyclePhase.OVULATION  -> phaseColors.ovulation
        CyclePhase.LUTEAL     -> phaseColors.luteal
        CyclePhase.PREGNANCY  -> MaterialTheme.colorScheme.primary
        else                  -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    }
}