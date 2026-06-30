package com.jeremy.lumi.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
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
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.usecase.CyclePrediction
import com.jeremy.lumi.domain.usecase.DelayState
import com.jeremy.lumi.ui.screens.calendar.DailyLogSheet
import com.jeremy.lumi.ui.screens.partner.GlassCard
import com.jeremy.lumi.ui.screens.partner.ObserverScreen
import com.jeremy.lumi.ui.screens.partner.PartnerViewModel
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
//  HOME SCREEN (Adaptativo: Normal / Observer Gate / Observer Home)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToPartner: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    remindersViewModel: RemindersViewModel = hiltViewModel()
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

    val activeReminders by remindersViewModel.activeReminders.collectAsState()

    LaunchedEffect(uiState.prediction, activeReminders) {
        uiState.prediction?.let {
            remindersViewModel.syncCycleReminders(it)
        }
    }

    // Sheet para ver ciclo de un vínculo (para usuarios normales)
    var linkedSheetLink by remember { mutableStateOf<PartnerLink?>(null) }
    val partnerViewModel: PartnerViewModel = hiltViewModel()
    val partnerUiState by partnerViewModel.uiState.collectAsState()

    // ── Modo Observador sin vínculos ──────────────────────────────────────
    if (uiState.homeMode == HomeMode.OBSERVER_GATE) {
        ObserverGateScreen(onNavigateToPartner = onNavigateToPartner)
        HugAnimationOverlay(visible = uiState.showHugAnimation)
        return
    }

    // ── Modo Observador con vínculo activo ────────────────────────────────
    if (uiState.homeMode == HomeMode.OBSERVER_HOME) {
        val activeLink = uiState.linkedCycles.firstOrNull()
        if (activeLink != null) {
            ObserverScreen(
                link = activeLink,
                currentUid = partnerUiState.currentUid ?: "",
                uiState = partnerUiState,
                onSendHug = { partnerViewModel.sendHug(activeLink.linkId) },
                onUnlink = { partnerViewModel.unlink(activeLink.linkId) },
                onBack = {}
            )
            HugAnimationOverlay(visible = uiState.showHugAnimation)
            return
        }
    }

    // ── Home Normal ───────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        // ConnectionsIndicator — reemplaza el corazón rosa
                        ConnectionsIndicator(
                            links = uiState.linkedCycles,
                            onClick = onNavigateToPartner
                        )
                        IconButton(onClick = { viewModel.toggleDiscreetMode() }) {
                            val icon = if (uiState.isDiscreetMode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
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

                    // ── Cycle Stories — visible si hay vínculos activos ───────
                    if (uiState.linkedCycles.isNotEmpty()) {
                        FadeSlideIn(screenReady, STAGGER_MS / 2) {
                            CycleStoriesRow(
                                links = uiState.linkedCycles,
                                onLinkClick = { linkedSheetLink = it }
                            )
                        }
                    }

                    var showConfirmPeriod by remember { mutableStateOf(false) }

                    // ── Banner de retraso ──────────────────────────────────
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
                        CycleRingCard(
                            currentDay     = uiState.currentDayOfCycle,
                            phase          = uiState.currentPhase,
                            prediction     = uiState.prediction,
                            isDiscreetMode = uiState.isDiscreetMode,
                            streakDays     = uiState.logStreakDays
                        )
                    }

                    // Botón principal
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
                            val icon = if (uiState.isLate) Icons.Rounded.Warning else Icons.Rounded.Add
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
                        OutlinedButton(
                            onClick  = onNavigateToInsights,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(18.dp),
                            border   = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                Icons.Rounded.Analytics,
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

        if (showBalloonGame) BalloonGameSheet { showBalloonGame = false }
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

        // ── Sheet del ciclo vinculado ────────────────────────────────────
        linkedSheetLink?.let { link ->
            LinkedCycleBottomSheet(
                link = link,
                currentUid = partnerUiState.currentUid ?: "",
                partnerUiState = partnerUiState,
                onSendHug = { partnerViewModel.sendHug(link.linkId) },
                onDismiss = { linkedSheetLink = null }
            )
        }
    }

    // ── Overlay de Abrazo Virtual ────────────────────────────────────────────
    HugAnimationOverlay(visible = uiState.showHugAnimation)
}

// ─────────────────────────────────────────────────────────────────────────────
//  CONNECTIONS INDICATOR (reemplaza el botón del corazón)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ConnectionsIndicator(links: List<PartnerLink>, onClick: () -> Unit) {
    val phaseColors = LocalPhaseColors.current

    fun linkPhaseColor(link: PartnerLink): Color {
        val phase = link.ownerSnapshot?.currentPhase ?: CyclePhase.UNKNOWN
        return when (phase) {
            CyclePhase.MENSTRUAL  -> phaseColors.menstrual
            CyclePhase.FOLLICULAR -> phaseColors.follicular
            CyclePhase.OVULATION  -> phaseColors.ovulation
            CyclePhase.LUTEAL     -> phaseColors.luteal
            else -> Color(0xFF9E9E9E)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "conn_pulse")
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot_pulse"
    )

    val enterScale = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) {
        enterScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = enterScale.value; scaleY = enterScale.value }
            .padding(end = 4.dp)
    ) {
        if (links.isEmpty()) {
            // Sin vínculos: chip sutil de "Vincular"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Rounded.PeopleAlt,
                        contentDescription = "Vincular",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Text(
                        "Vincular",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            // Con vínculos: pill con puntos de color de fase
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Rounded.PeopleAlt,
                        contentDescription = "Vínculos",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    // Dots de color de fase (máximo 3)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        links.take(3).forEach { link ->
                            val color = linkPhaseColor(link)
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .graphicsLayer { alpha = dotPulse }
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                    if (links.size > 1) {
                        Text(
                            "${links.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CYCLE STORIES ROW (para usuarios normales con vínculos)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CycleStoriesRow(links: List<PartnerLink>, onLinkClick: (PartnerLink) -> Unit) {
    val phaseColors = LocalPhaseColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "story_pulse")
    val storyRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "story_ring"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Tus vínculos",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            letterSpacing = 0.3.sp
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(links) { link ->
                val phase = link.ownerSnapshot?.currentPhase ?: CyclePhase.UNKNOWN
                val phaseColor = when (phase) {
                    CyclePhase.MENSTRUAL  -> phaseColors.menstrual
                    CyclePhase.FOLLICULAR -> phaseColors.follicular
                    CyclePhase.OVULATION  -> phaseColors.ovulation
                    CyclePhase.LUTEAL     -> phaseColors.luteal
                    else -> Color(0xFF9E9E9E)
                }
                val displayName = link.ownerDisplayName ?: "?"
                val initial = displayName.take(1).uppercase()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            onLinkClick(link)
                        }
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Anillo de fase (como IG Stories)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .graphicsLayer { alpha = storyRingAlpha }
                                .clip(CircleShape)
                                .border(
                                    2.5.dp,
                                    Brush.sweepGradient(
                                        listOf(phaseColor, phaseColor.copy(alpha = 0.3f), phaseColor)
                                    ),
                                    CircleShape
                                )
                        )
                        // Avatar interior con inicial y gradiente
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(phaseColor.copy(alpha = 0.3f), phaseColor.copy(alpha = 0.08f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = storyPhaseEmoji(phase),
                                fontSize = 20.sp
                            )
                        }
                    }
                    Text(
                        text = displayName.split(" ").first(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  OBSERVER GATE (observador sin vínculos)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObserverGateScreen(onNavigateToPartner: () -> Unit) {
    val phaseColors = LocalPhaseColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "gate_float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gate_float_y"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gate_ring_scale"
    )
    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.background
                        ),
                        center = Offset(500f, 300f),
                        radius = 1000f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Ilustración animada — dos círculos conectándose
                Box(
                    modifier = Modifier
                        .graphicsLayer { translationY = floatY }
                        .size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Halo exterior pulsante
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .graphicsLayer { scaleX = ringScale; scaleY = ringScale; alpha = 0.15f }
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    // Círculo interior
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(primaryColor.copy(alpha = 0.18f), primaryColor.copy(alpha = 0.04f))
                                )
                            )
                            .border(2.dp, primaryColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👥", fontSize = 52.sp)
                    }
                }

                Spacer(Modifier.height(40.dp))

                Text(
                    "Vincúlate para comenzar",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Pide a alguien que comparta su ciclo contigo mediante un código de vínculo. Verás su ciclo aquí en tiempo real.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp
                )

                Spacer(Modifier.height(48.dp))

                // Botón principal
                Button(
                    onClick = onNavigateToPartner,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Rounded.PeopleAlt, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Crear un vínculo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Puedes seguir usando el calendario y el chat mientras esperas.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  LINKED CYCLE BOTTOM SHEET (para usuarios normales — tap en story)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedCycleBottomSheet(
    link: PartnerLink,
    currentUid: String,
    partnerUiState: com.jeremy.lumi.ui.screens.partner.PartnerUiState,
    onSendHug: () -> Unit,
    onDismiss: () -> Unit
) {
    val snapshot = link.ownerSnapshot
    val currentPhase = snapshot?.currentPhase ?: CyclePhase.UNKNOWN
    val phaseColors = LocalPhaseColors.current
    val phaseColor = when (currentPhase) {
        CyclePhase.MENSTRUAL  -> phaseColors.menstrual
        CyclePhase.FOLLICULAR -> phaseColors.follicular
        CyclePhase.OVULATION  -> phaseColors.ovulation
        CyclePhase.LUTEAL     -> phaseColors.luteal
        else -> MaterialTheme.colorScheme.primary
    }
    val ownerName = link.ownerDisplayName

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(storyPhaseEmoji(currentPhase), fontSize = 20.sp)
                    Text(
                        text = if (ownerName != null) "Ciclo de $ownerName" else "Ciclo Vinculado",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) {
        // Embeder directamente el ObserverScreen content dentro del sheet
        ObserverScreen(
            link = link,
            currentUid = currentUid,
            uiState = partnerUiState,
            onSendHug = onSendHug,
            onUnlink = { /* No desvincular desde aquí */ },
            onBack = onDismiss
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HUG ANIMATION OVERLAY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HugAnimationOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(400)),
        exit    = fadeOut(tween(400))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            val scale  = remember { Animatable(0.6f) }
            val alpha  = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                launch { scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                launch { alpha.animateTo(1f, tween(350)) }
            }

            val infiniteTransition = rememberInfiniteTransition(label = "hug_pulse")
            val heartScale by infiniteTransition.animateFloat(
                initialValue = 0.94f, targetValue = 1.06f,
                animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "heart_scale"
            )
            val waveScale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 2.2f,
                animationSpec = infiniteRepeatable(tween(1400, easing = LinearOutSlowInEasing), RepeatMode.Restart),
                label = "wave_scale"
            )
            val waveAlpha by infiniteTransition.animateFloat(
                initialValue = 0.35f, targetValue = 0f,
                animationSpec = infiniteRepeatable(tween(1400, easing = LinearOutSlowInEasing), RepeatMode.Restart),
                label = "wave_alpha"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer { scaleX = waveScale; scaleY = waveScale; this.alpha = waveAlpha }
                            .clip(CircleShape)
                            .background(Color(0xFFE91E63).copy(alpha = 0.25f))
                    )
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE91E63).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector     = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint            = Color(0xFFE91E63),
                            modifier        = Modifier
                                .size(52.dp)
                                .graphicsLayer { scaleX = heartScale; scaleY = heartScale }
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                val textAlpha  = remember { Animatable(0f) }
                val textOffset = remember { Animatable(10f) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(200)
                    launch { textAlpha.animateTo(1f, tween(350)) }
                    launch { textOffset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        this.alpha   = textAlpha.value
                        translationY = textOffset.value.dp.toPx()
                    }
                ) {
                    Text(
                        text       = stringResource(R.string.home_hug_received_title),
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text    = stringResource(R.string.home_hug_received_desc),
                        fontSize = 15.sp,
                        color   = Color.White.copy(alpha = 0.78f)
                    )
                }
            }
        }
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
    val (bgColor, textColor, _) = when (delayState) {
        DelayState.LATE           -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "~")
        DelayState.VERY_LATE      -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "!")
        DelayState.EXTREMELY_LATE -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "!!")
        DelayState.ON_TIME        -> Triple(Color.Transparent, Color.Transparent, "")
    }

    val message = when (delayState) {
        DelayState.LATE           -> "Tu regla lleva $delayDays ${if (delayDays == 1) "día" else "días"} de retraso. Es normal que ocurra."
        DelayState.VERY_LATE      -> "$delayDays días de retraso. El estrés, cambios de peso o ejercicio intenso pueden causarlo."
        DelayState.EXTREMELY_LATE -> "$delayDays días de retraso. Si descartaste un embarazo, consulta a tu ginecóloga."
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(text = message, fontSize = 14.sp, color = textColor, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp).offset(x = 8.dp, y = (-4).dp)) {
                    Icon(Icons.Rounded.Close, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
            OutlinedButton(
                onClick = onPeriodArrived,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.4f))
            ) {
                Text(text = stringResource(R.string.period_arrived_btn), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPERS Y COMPOSABLES REUTILIZABLES
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs   = remember { OnboardingPreferenceManager(context.applicationContext) }
    val userName by prefs.userNameFlow.collectAsState(initial = null)
    val greeting = if (!userName.isNullOrBlank()) "Hola, $userName 👋" else "Hola 👋"

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(greeting, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(stringResource(R.string.home_subtitle), fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun CycleWeekStrip(days: List<CycleDayUi>, isDiscreetMode: Boolean, onDayClick: ((CycleDayUi) -> Unit)? = null) {
    val listState  = rememberLazyListState()
    val todayIndex = remember(days) { days.indexOfFirst { it.isToday }.coerceAtLeast(0) }
    LaunchedEffect(todayIndex) { listState.scrollToItem(todayIndex, scrollOffset = -220) }
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
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
    val numColor    = if (day.isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground

    val isFuture  = !day.isToday && day.date.isAfter(java.time.LocalDate.now())
    val dotColor  = when {
        day.isToday -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        isFuture    -> phaseColor.copy(alpha = 0.75f)
        else        -> phaseColor.copy(alpha = 0.35f)
    }
    val dotSize = if (isFuture) 6.dp else 5.dp

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
        Text(day.weekdayLabel, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = numColor.copy(alpha = if (day.isToday) 0.85f else 0.55f))
        Spacer(Modifier.height(7.dp))
        Text(day.dayOfMonth.toString(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = numColor)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
    }
}

@Composable
private fun PhaseLegend(isDiscreetMode: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        val colorMens = if (isDiscreetMode) Color.Gray.copy(alpha = 0.5f) else homePhaseColor(CyclePhase.MENSTRUAL)
        val colorFoli = if (isDiscreetMode) Color.Gray.copy(alpha = 0.5f) else homePhaseColor(CyclePhase.FOLLICULAR)
        val colorOvu  = if (isDiscreetMode) Color.Gray.copy(alpha = 0.5f) else homePhaseColor(CyclePhase.OVULATION)
        val colorLut  = if (isDiscreetMode) Color.Gray.copy(alpha = 0.5f) else homePhaseColor(CyclePhase.LUTEAL)

        val labelMens = if (isDiscreetMode) stringResource(R.string.discreet_phase_1) else stringResource(R.string.phase_menstrual)
        val labelFoli = if (isDiscreetMode) stringResource(R.string.discreet_phase_2) else stringResource(R.string.phase_follicular)
        val labelOvu  = if (isDiscreetMode) stringResource(R.string.discreet_phase_3) else stringResource(R.string.phase_ovulation)
        val labelLut  = if (isDiscreetMode) stringResource(R.string.discreet_phase_4) else stringResource(R.string.phase_luteal)

        LegendItem(colorMens, labelMens)
        LegendItem(colorFoli, labelFoli)
        LegendItem(colorOvu, labelOvu)
        LegendItem(colorLut, labelLut)
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
fun CycleRingCard(currentDay: Int, phase: CyclePhase, prediction: CyclePrediction?, isDiscreetMode: Boolean, streakDays: Int = 0) {
    val phaseColors        = LocalPhaseColors.current
    val actualPhaseColor   = homePhaseColor(phase)
    val phaseColor         = if (isDiscreetMode) Color.Gray.copy(alpha = 0.6f) else actualPhaseColor
    val animatedPhaseColor by animateColorAsState(phaseColor, tween(700), label = "phase_color")

    val cycleLen  = (prediction?.cycleLength  ?: 28)
    val periodLen = (prediction?.periodLength ?: 5)
    val cycleLenF = cycleLen.toFloat()

    val ovDay    = cycleLen - 14
    val menSweep = (periodLen.toFloat() / cycleLenF) * 360f
    val folStart = -90f + menSweep
    val folSweep = ((ovDay - 2 - periodLen).toFloat().coerceAtLeast(1f) / cycleLenF) * 360f
    val ovSweep  = (3f / cycleLenF) * 360f
    val lutSweep = ((cycleLen - (ovDay + 1)).toFloat().coerceAtLeast(1f) / cycleLenF) * 360f

    val targetProgress = if (currentDay > 0) (currentDay / cycleLenF).coerceIn(0f, 1f) else 0f
    val animProgress   = remember { Animatable(0f) }
    LaunchedEffect(currentDay) {
        animProgress.animateTo(targetProgress, tween(1100, easing = FastOutSlowInEasing))
    }

    val dayScale = remember { Animatable(0.6f) }
    LaunchedEffect(currentDay) {
        dayScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }
    val breathing = rememberInfiniteTransition(label = "breath")
    val baseScale = if (streakDays >= 3) 1.05f else 1.0f
    val breathScale by breathing.animateFloat(0.990f * baseScale, 1.010f * baseScale,
        infiniteRepeatable(tween(BREATH_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse), "bs")
    val baseHalo = if (streakDays >= 3) 0.15f else 0.04f
    val maxHalo = if (streakDays >= 3) 0.3f else 0.12f
    val haloAlpha by breathing.animateFloat(baseHalo, maxHalo,
        infiniteRepeatable(tween(BREATH_MS + 200, easing = LinearEasing), RepeatMode.Reverse), "ha")

    val cardBg by animateColorAsState(phaseColor.copy(alpha = 0.07f), tween(700), label = "card_bg")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(32.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxWidth().background(cardBg)) {
            Column(
                Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 28.dp, start = 28.dp, end = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(220.dp).graphicsLayer { scaleX = breathScale; scaleY = breathScale },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(animatedPhaseColor.copy(alpha = haloAlpha), size.minDimension / 2f * 1.07f)
                    }
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 18f
                        val inset  = stroke / 2f
                        val arcSz  = Size(size.width - stroke, size.height - stroke)
                        val tl     = Offset(inset, inset)
                        val gap    = 2.5f
                        drawArc(phaseColors.menstrual, -90f, menSweep - gap, false, tl, arcSz, style = Stroke(stroke, cap = StrokeCap.Round))
                        drawArc(phaseColors.follicular, folStart + gap / 2f, folSweep - gap, false, tl, arcSz, style = Stroke(stroke, cap = StrokeCap.Round))
                        val ovStart = folStart + folSweep + gap / 2f
                        drawArc(phaseColors.ovulation, ovStart, ovSweep - gap, false, tl, arcSz, style = Stroke(stroke, cap = StrokeCap.Round))
                        val lutStart = ovStart + ovSweep + gap / 2f
                        drawArc(phaseColors.luteal, lutStart, lutSweep - gap, false, tl, arcSz, style = Stroke(stroke, cap = StrokeCap.Round))
                        if (animProgress.value > 0f) {
                            val tickAngle = Math.toRadians((-90.0 + 360.0 * animProgress.value)).toFloat()
                            val r = (size.minDimension - stroke) / 2f
                            val cx = center.x + r * kotlin.math.cos(tickAngle)
                            val cy = center.y + r * kotlin.math.sin(tickAngle)
                            drawCircle(animatedPhaseColor, stroke / 2f * 1.6f, Offset(cx, cy))
                            drawCircle(androidx.compose.ui.graphics.Color.White, stroke / 2f * 0.85f, Offset(cx, cy))
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { scaleX = dayScale.value; scaleY = dayScale.value }
                    ) {
                        Text(
                            text = if (currentDay > 0) "$currentDay" else "–",
                            fontSize = 58.sp, fontWeight = FontWeight.Bold,
                            color = animatedPhaseColor, lineHeight = 58.sp
                        )
                        Text(
                            text = if (currentDay > 0) stringResource(R.string.day_of_cycle) else stringResource(R.string.inactive_cycle),
                            fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(26.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(50.dp))
                        .background(animatedPhaseColor.copy(alpha = 0.15f))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isDiscreetMode) stringResource(R.string.discreet_observing) else stringResource(phase.phaseNameRes),
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = animatedPhaseColor
                    )
                }

                if (!isDiscreetMode) {
                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f), thickness = 0.5.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(phase.descriptionRes),
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center, lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PhaseTimelineCard(currentPhase: CyclePhase, p: CyclePrediction, isDiscreetMode: Boolean) {
    val ovulationDay = p.cycleLength - 14
    val cd = p.currentDayOfCycle

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
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
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
        Box(Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        }
        Spacer(Modifier.width(12.dp))
        Text(phaseLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(timeLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
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

private fun storyPhaseEmoji(phase: CyclePhase): String = when (phase) {
    CyclePhase.MENSTRUAL  -> "🌺"
    CyclePhase.FOLLICULAR -> "🌱"
    CyclePhase.OVULATION  -> "✨"
    CyclePhase.LUTEAL     -> "🌙"
    CyclePhase.PREGNANCY  -> "🤱"
    else                  -> "🌸"
}
