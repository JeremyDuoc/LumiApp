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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.text.TextStyle
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
import com.jeremy.lumi.ui.theme.LocalBrandGradient
import com.jeremy.lumi.ui.theme.LocalBrandBackgroundGradient
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.animateLottieCompositionAsState

private const val STAGGER_MS = 20
private const val ENTER_MS   = 420
private const val BREATH_MS  = 3800

private val gentleSpring = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
private val snappySpring = spring<Float>(Spring.DampingRatioLowBouncy,    Spring.StiffnessMedium)

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  HOME SCREEN (Adaptativo: Normal / Observer Gate / Observer Home)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToPartner: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    remindersViewModel: RemindersViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsStateWithLifecycle()
    var showBalloonGame   by remember { mutableStateOf(false) }
    var showQuickLogSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    var screenReady by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) { screenReady = true }
    }

    val activeReminders by remindersViewModel.activeReminders.collectAsStateWithLifecycle()
    val quickLogViewModel: QuickLogViewModel = hiltViewModel()

    var lastSyncedPrediction by remember { mutableStateOf<CyclePrediction?>(null) }
    var lastSyncedRemindersHash by remember { mutableStateOf(0) }

    LaunchedEffect(uiState.prediction, activeReminders) {
        val currentRemindersHash = activeReminders.hashCode()
        if (uiState.prediction != null && 
            (uiState.prediction != lastSyncedPrediction || currentRemindersHash != lastSyncedRemindersHash)) {
            
            remindersViewModel.syncCycleReminders(uiState.prediction!!)
            lastSyncedPrediction = uiState.prediction
            lastSyncedRemindersHash = currentRemindersHash
        }
    }

    LaunchedEffect(quickLogViewModel) {
        quickLogViewModel.uiEvent.collect { event ->
            when (event) {
                is QuickLogViewModel.UiEvent.LogSaved -> {
                    showQuickLogSheet = false
                    snackbarHostState.showSnackbar("✓ Registro guardado")
                }
                is QuickLogViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // Sheet para ver ciclo de un vínculo (para usuarios normales)
    var linkedSheetLink by remember { mutableStateOf<PartnerLink?>(null) }
    val partnerViewModel: PartnerViewModel = hiltViewModel()
    val partnerUiState by partnerViewModel.uiState.collectAsStateWithLifecycle()

    // â”€â”€ Modo Observador sin vínculos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (uiState.homeMode == HomeMode.OBSERVER_GATE) {
        ObserverGateScreen(onNavigateToPartner = onNavigateToPartner)
        HugAnimationOverlay(visible = uiState.showHugAnimation)
        return
    }

    // â”€â”€ Modo Observador con vínculo activo â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (uiState.homeMode == HomeMode.OBSERVER_HOME) {
        val activeLink = uiState.linkedCycles.firstOrNull()
        if (activeLink != null) {
            ObserverScreen(
                link = activeLink,
                currentUid = partnerUiState.currentUid ?: "",
                uiState = partnerUiState,
                onSendCareAction = { action -> partnerViewModel.sendCareAction(activeLink.linkId, action) },
                onUnlink = { partnerViewModel.unlink(activeLink.linkId) },
                onBack = {}
            )
            HugAnimationOverlay(visible = uiState.showHugAnimation)
            return
        }
    }

    val brandBgGradient = LocalBrandBackgroundGradient.current
    val backgroundModifier = if (brandBgGradient != null) {
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).background(brandBgGradient)
    } else {
        Modifier.fillMaxSize()
    }

    // â”€â”€ Home Normal â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    Box(modifier = backgroundModifier) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        val brandGradient = LocalBrandGradient.current
                        if (brandGradient != null) {
                            Text(
                                stringResource(R.string.app_name),
                                style = androidx.compose.ui.text.TextStyle(
                                    brush = brandGradient,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 20.sp
                                )
                            )
                        } else {
                            Text(
                                stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 20.sp
                            )
                        }
                    },
                    actions = {
                        // Indicador de error de sincronización
                        if (uiState.syncError) {
                            Icon(
                                Icons.Rounded.CloudOff,
                                contentDescription = "Sync Error",
                                modifier = Modifier.padding(end = 8.dp).size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        // ConnectionsIndicator
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
                        containerColor = if (brandBgGradient != null) Color.Transparent else MaterialTheme.colorScheme.background
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
                    val brandGradient = LocalBrandGradient.current
                    if (brandGradient != null) {
                        FloatingActionButton(
                            onClick = { showQuickLogSheet = true },
                            containerColor = Color.Transparent,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp),
                            modifier = Modifier.background(brandGradient, shape = RoundedCornerShape(16.dp))
                        ) {
                            Icon(Icons.Rounded.Add, stringResource(R.string.log_title), tint = Color.White)
                        }
                    } else {
                        FloatingActionButton(
                            onClick        = { showQuickLogSheet = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = MaterialTheme.colorScheme.onPrimary
                        ) { Icon(Icons.Rounded.Add, stringResource(R.string.log_title)) }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.jeremy.lumi.ui.components.LumiLoader(modifier = Modifier.size(80.dp))
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

                    // â”€â”€ Cycle Stories â€” visible si hay vínculos activos â”€â”€â”€â”€â”€â”€â”€
                    if (uiState.linkedCycles.isNotEmpty()) {
                        FadeSlideIn(screenReady, STAGGER_MS / 2) {
                            CycleStoriesRow(
                                links = uiState.linkedCycles,
                                onLinkClick = { linkedSheetLink = it }
                            )
                        }
                    }else {
                        FadeSlideIn(screenReady, STAGGER_MS / 2) {
                            EmptyLinksCard(onClick = onNavigateToPartner)
                        }
                    }

                    var showConfirmPeriod by remember { mutableStateOf(false) }
                    var showSuccessLottie by remember { mutableStateOf(false) }

                    // â”€â”€ Banner de retraso â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                    }

                    FadeSlideIn(screenReady, STAGGER_MS * 3) {
                        CycleRingCard(
                            currentDay     = uiState.currentDayOfCycle,
                            phase          = uiState.currentPhase,
                            prediction     = uiState.prediction,
                            isDiscreetMode = uiState.isDiscreetMode,
                            streakDays     = uiState.logStreakDays,
                            isLate         = uiState.isLate,
                            onLogPeriod    = { showConfirmPeriod = true }
                        )
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
                                        showSuccessLottie = true
                                    }
                                ) { Text(stringResource(R.string.confirm_period_yes)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfirmPeriod = false }) { Text(stringResource(R.string.confirm_period_cancel)) }
                            }
                        )
                    }

                    if (showSuccessLottie) {
                        androidx.compose.ui.window.Dialog(onDismissRequest = { showSuccessLottie = false }) {
                            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_checkmark))
                            val progress by animateLottieCompositionAsState(
                                composition = composition,
                                iterations = 1
                            )
                            LaunchedEffect(progress) {
                                if (progress == 1f) {
                                    delay(200)
                                    showSuccessLottie = false
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (composition != null) {
                                    LottieAnimation(
                                        composition = composition,
                                        progress = { progress },
                                        modifier = Modifier.fillMaxSize().padding(16.dp)
                                    )
                                }
                            }
                        }
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
            val todayLog by quickLogViewModel.todayLog.collectAsStateWithLifecycle()
            val activeCategories by quickLogViewModel.activeCategories.collectAsStateWithLifecycle()

            DailyLogSheet(
                day      = quickLogViewModel.todayDayOfMonth,
                month    = quickLogViewModel.todayMonth,
                year     = quickLogViewModel.todayYear,
                savedLog = todayLog,
                activeCategories = activeCategories,
                onActiveCategoriesChange = { quickLogViewModel.setActiveCategories(it) },
                onDismiss = { showQuickLogSheet = false },
                onSave    = { flow, pain, mood, symptoms, mucus, notes, hadIntercourse,
                              protectionUsed, method, intercourseNotes, showOnCalendar,
                              sleepHours, energyLevel, stressLevel, bbt, spotting ->
                    quickLogViewModel.saveToday(flow, pain, mood, symptoms, mucus, notes,
                        hadIntercourse, protectionUsed, method, intercourseNotes, showOnCalendar,
                        sleepHours, energyLevel, stressLevel, bbt, spotting)
                }
            )
        }

        // â”€â”€ Sheet del ciclo vinculado â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        linkedSheetLink?.let { link ->
            LinkedCycleBottomSheet(
                link = link,
                currentUid = partnerUiState.currentUid ?: "",
                partnerUiState = partnerUiState,
                onSendCareAction = { action -> partnerViewModel.sendCareAction(link.linkId, action) },
                onDismiss = { linkedSheetLink = null }
            )
        }
    }

    // â”€â”€ Overlay de Abrazo Virtual â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    HugAnimationOverlay(visible = uiState.showHugAnimation)
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  CONNECTIONS INDICATOR (reemplaza el botón del corazón)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
                        stringResource(R.string.home_link_chip),
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  CYCLE STORIES ROW (para usuarios normales con vínculos)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
            items(links, key = { it.linkId }) { link ->
                val phase = link.ownerSnapshot?.currentPhase ?: CyclePhase.UNKNOWN
                val phaseColor = when (phase) {
                    CyclePhase.MENSTRUAL  -> phaseColors.menstrual
                    CyclePhase.FOLLICULAR -> phaseColors.follicular
                    CyclePhase.OVULATION  -> phaseColors.ovulation
                    CyclePhase.LUTEAL     -> phaseColors.luteal
                    else -> Color(0xFF9E9E9E)
                }
                val displayName = link.ownerDisplayName ?: "?"
                val itemWidth = 60.dp

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
                            Modifier
                                .width(itemWidth)
                                .height(72.dp)
                                .clip(RoundedCornerShape(24.dp))
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  OBSERVER GATE (observador sin vínculos)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
                // Ilustración animada â€” dos círculos conectándose
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  LINKED CYCLE BOTTOM SHEET (para usuarios normales â€” tap en story)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedCycleBottomSheet(
    link: PartnerLink,
    currentUid: String,
    partnerUiState: com.jeremy.lumi.ui.screens.partner.PartnerUiState,
    onSendCareAction: (com.jeremy.lumi.domain.model.CareAction) -> Unit,
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
            onSendCareAction = onSendCareAction,
            onUnlink = { /* No desvincular desde aquí */ },
            onBack = onDismiss
        )
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  HUG ANIMATION OVERLAY
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  DELAY BANNER
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  HELPERS Y COMPOSABLES REUTILIZABLES
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
    val userName by prefs.userNameFlow.collectAsStateWithLifecycle(initialValue = null)
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
    val isBajaFertilidad = day.phase == CyclePhase.FOLLICULAR || day.phase == CyclePhase.LUTEAL || day.phase == CyclePhase.UNKNOWN
    
    val phaseColor  = if (isDiscreetMode || isBajaFertilidad) Color.Transparent else homePhaseColor(day.phase)
    val interSource = remember { MutableInteractionSource() }
    val isPressed   by interSource.collectIsPressedAsState()
    val chipScale   by animateFloatAsState(if (isPressed) 0.93f else 1f, snappySpring, label = "chip")
    
    val bg = when {
        day.isToday && isBajaFertilidad -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
        day.isToday -> phaseColor
        isBajaFertilidad -> Color.Transparent
        else -> phaseColor.copy(alpha = 0.10f)
    }
    
    val numColor    = if (day.isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground

    val isFuture  = !day.isToday && day.date.isAfter(java.time.LocalDate.now())
    val dotColor  = when {
        isBajaFertilidad -> Color.Transparent
        day.isToday -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        isFuture    -> phaseColor.copy(alpha = 0.75f)
        else        -> phaseColor.copy(alpha = 0.35f)
    }
    val dotSize = if (isFuture) 5.dp else 4.dp

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
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(day.weekdayLabel, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = numColor.copy(alpha = if (day.isToday) 0.85f else 0.55f))
        Spacer(Modifier.height(5.dp))
        Text(day.dayOfMonth.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = numColor)
        Spacer(Modifier.height(5.dp))
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
fun CycleRingCard(currentDay: Int, phase: CyclePhase, prediction: CyclePrediction?, isDiscreetMode: Boolean, streakDays: Int = 0, isLate: Boolean = false, onLogPeriod: () -> Unit = {}) {
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

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp, start = 28.dp, end = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(190.dp).graphicsLayer { scaleX = breathScale; scaleY = breathScale },
            contentAlignment = Alignment.Center
        ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(animatedPhaseColor.copy(alpha = haloAlpha), size.minDimension / 2f * 1.05f)
                }
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 36f
                    val inset  = stroke / 2f
                    val arcSz  = Size(size.width - stroke, size.height - stroke)
                    val tl     = Offset(inset, inset)
                    val gap    = 5f

                    // Background Track
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.08f),
                        radius = (size.minDimension - stroke) / 2f,
                        style = Stroke(width = stroke)
                    )

                    fun drawPhaseArc(color: androidx.compose.ui.graphics.Color, startAngle: Float, sweepAngle: Float, isCurrent: Boolean) {
                        if (sweepAngle <= 0f) return
                        if (isCurrent) {
                            drawArc(color.copy(alpha = 0.25f), startAngle, sweepAngle, false, tl, arcSz, style = Stroke(stroke * 1.5f, cap = StrokeCap.Round))
                            drawArc(color.copy(alpha = 0.45f), startAngle, sweepAngle, false, tl, arcSz, style = Stroke(stroke * 1.25f, cap = StrokeCap.Round))
                        }
                        drawArc(color, startAngle, sweepAngle, false, tl, arcSz, style = Stroke(stroke, cap = StrokeCap.Round))
                    }

                    drawPhaseArc(phaseColors.menstrual, -90f, menSweep - gap, phase == CyclePhase.MENSTRUAL)
                    val ovStart = folStart + folSweep + gap / 2f
                    drawPhaseArc(phaseColors.ovulation, ovStart, ovSweep - gap, phase == CyclePhase.OVULATION)
                    if (animProgress.value > 0f) {
                        val tickAngle = Math.toRadians((-90.0 + 360.0 * animProgress.value)).toFloat()
                        val r = (size.minDimension - stroke) / 2f
                        val cx = center.x + r * kotlin.math.cos(tickAngle)
                        val cy = center.y + r * kotlin.math.sin(tickAngle)

                        // Glowing indicator dot
                        drawCircle(animatedPhaseColor.copy(alpha = 0.4f), stroke / 2f * 2.2f, Offset(cx, cy))
                        drawCircle(animatedPhaseColor, stroke / 2f * 1.4f, Offset(cx, cy))
                        drawCircle(androidx.compose.ui.graphics.Color.White, stroke / 2f * 0.8f, Offset(cx, cy))
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { scaleX = dayScale.value; scaleY = dayScale.value }
                ) {
                    val brandGradient = LocalBrandGradient.current
                    if (brandGradient != null && currentDay > 0) {
                        Text(
                            text = "$currentDay",
                            style = TextStyle(
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 54.sp,
                                brush = brandGradient
                            )
                        )
                    } else {
                        Text(
                            text = if (currentDay > 0) "$currentDay" else "â€“",
                            fontSize = 54.sp, fontWeight = FontWeight.Bold,
                            color = animatedPhaseColor, lineHeight = 54.sp
                        )
                    }
                    Text(
                        text = if (currentDay > 0) {
                            if (phase == CyclePhase.PREGNANCY) "Días de gestación" else stringResource(R.string.day_of_cycle)
                        } else {
                            stringResource(R.string.inactive_cycle)
                        },
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
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
                Text(
                    text = getDynamicPhaseMessage(phase, currentDay, cycleLen),
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center, lineHeight = 22.sp
                )
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f), thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onLogPeriod,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                ) {
                    val btnText = if (isLate) "¿Tu periodo llegó?" else "Registrar periodo"

                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_heart))

                    if (composition != null) {
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        val fallbackIcon = if (isLate) Icons.Rounded.Warning else Icons.Rounded.Add
                        Icon(fallbackIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(btnText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
}

@Composable
fun getDynamicPhaseMessage(phase: CyclePhase, currentDay: Int, cycleLength: Int): String {
    return when (phase) {
        CyclePhase.MENSTRUAL -> {
            if (currentDay <= 2) "Tu cuerpo está trabajando duro. Es normal sentir poca energía y más necesidad de descanso hoy."
            else "El flujo empezará a disminuir pronto. Recuerda mantenerte hidratada y ser amable contigo misma."
        }
        CyclePhase.FOLLICULAR -> {
            "Tus niveles de estrógeno están subiendo. Notarás más energía, mejor humor y mayor creatividad. ¡Aprovecha el impulso!"
        }
        CyclePhase.OVULATION -> {
            "Estás en tu pico de energía y magnetismo. Excelente momento para conectar con otros y abordar retos importantes."
        }
        CyclePhase.LUTEAL -> {
            val daysToNext = cycleLength - currentDay
            if (daysToNext > 7) {
                "Tu energía se vuelve más tranquila. Es un buen momento para tareas enfocadas y organización personal."
            } else {
                "Tu cuerpo se prepara para el siguiente ciclo. Puedes notar cambios de humor o menor energía. Tómate las cosas con más calma."
            }
        }
        else -> "Comienza a registrar tu periodo para conocer qué está pasando en tu cuerpo."
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
    val events = listOfNotNull(
        PhaseEvent(CyclePhase.MENSTRUAL,  daysUntilStart(1)),
        if (p.daysUntilOvulation != -1) PhaseEvent(CyclePhase.OVULATION, daysUntilStart(ovulationDay - 1)) else null
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
    val timeLabel = when {
        daysUntil < 0 -> "Retrasado"
        daysUntil == 0 -> stringResource(R.string.predict_today)
        daysUntil == 1 -> stringResource(R.string.cal_next_phase_tomorrow)
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
    CyclePhase.PREGNANCY  -> "🤰"
    else                  -> "🌸"
}

@Composable
fun EmptyLinksCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PeopleAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vincular a tu pareja",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Comparte tu ciclo de forma segura y en tiempo real.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
