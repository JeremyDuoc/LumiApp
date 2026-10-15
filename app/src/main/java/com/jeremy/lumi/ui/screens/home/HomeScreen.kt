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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import androidx.compose.material.icons.rounded.Nightlight
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.usecase.CyclePrediction
import com.jeremy.lumi.domain.usecase.DelayState
import com.jeremy.lumi.ui.screens.calendar.DailyLogSheet
import com.jeremy.lumi.ui.screens.partner.ObserverScreen
import com.jeremy.lumi.ui.screens.partner.PartnerViewModel
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import com.jeremy.lumi.ui.theme.LocalIsDarkTheme
import com.jeremy.lumi.ui.theme.LocalDialStyle
import com.jeremy.lumi.ui.theme.DialStyle
import com.jeremy.lumi.ui.theme.LocalBrandGradient
import com.jeremy.lumi.ui.theme.LocalBrandBackgroundGradient
import com.jeremy.lumi.ui.components.*
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.ui.theme.AmbientScreen
import com.jeremy.lumi.ui.screens.settings.SettingsViewModel
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val STAGGER_MS = 20
private const val ENTER_MS   = 420
private const val BREATH_MS  = 3800

private val gentleSpring = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
private val snappySpring = spring<Float>(Spring.DampingRatioLowBouncy,    Spring.StiffnessMedium)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToPartner: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    remindersViewModel: RemindersViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    quickLogViewModel: QuickLogViewModel = hiltViewModel(),
    partnerViewModel: PartnerViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsStateWithLifecycle()
    var showBalloonGame   by remember { mutableStateOf(false) }
    var showQuickLogSheet by remember { mutableStateOf(false) }
    var showThemeSheet    by remember { mutableStateOf(false) }
    var showPredictionInfoSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()
    val context           = androidx.compose.ui.platform.LocalContext.current

    var screenReady by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) { screenReady = true }
    }

    val activeReminders by remindersViewModel.activeReminders.collectAsStateWithLifecycle()
    val todayLog by quickLogViewModel.todayLog.collectAsStateWithLifecycle()
    val hasEntry = todayLog != null

    LaunchedEffect(uiState.prediction) {
        val prediction = uiState.prediction ?: return@LaunchedEffect
        remindersViewModel.syncCycleReminders(prediction)
    }

    LaunchedEffect(quickLogViewModel) {
        quickLogViewModel.uiEvent.collect { event ->
            when (event) {
                is QuickLogViewModel.UiEvent.LogSaved -> {
                    showQuickLogSheet = false
                    snackbarHostState.showSnackbar(context.getString(R.string.log_saved_confirmation))
                }
                is QuickLogViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is HomeViewModel.HomeEvent.CycleStarted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.home_snackbar_period_logged),
                        actionLabel = context.getString(R.string.home_snackbar_undo),
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoLastPeriod()
                    }
                }
                is HomeViewModel.HomeEvent.CycleRestored -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.home_snackbar_cycle_restored),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Sheet para ver ciclo de un vínculo (para usuarios normales)
    var linkedSheetLink by remember { mutableStateOf<PartnerLink?>(null) }
    val partnerUiState by partnerViewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.homeMode) {
        HomeMode.OBSERVER_GATE -> {
            ObserverGateScreen(onNavigateToPartner = onNavigateToPartner)
        }
        HomeMode.OBSERVER_HOME -> {
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
            } else {
                ObserverGateScreen(onNavigateToPartner = onNavigateToPartner)
            }
        }
        HomeMode.NORMAL -> {
            // Color de acento para AURA: usa el color armónico de la fase actual
            val currentAuraColor: Color? = if (uiState.currentPhase != CyclePhase.UNKNOWN) {
                homePhaseColor(uiState.currentPhase)
            } else null

            val homeScrollState = rememberScrollState()

            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AmbientBackgroundLayer(
            screenType           = AmbientScreen.HOME,
            accentColor          = currentAuraColor,
            scrollOffsetProvider = { homeScrollState.value.toFloat() }
        )
        var showConfirmPeriod by remember { mutableStateOf(false) }
        var showSuccessLottie by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                val hasNotificationAlert = !hasEntry || uiState.isLate || ((uiState.prediction?.daysUntilNextPeriod ?: 99) in 0..3) || activeReminders.isNotEmpty()
                LumiTopBar(
                    hasNotificationAlert = hasNotificationAlert,
                    onThemeClick = { showThemeSheet = true },
                    onNotificationsClick = { showNotificationsSheet = true },
                    onChatClick = onNavigateToChat,
                    onSettingsClick = onNavigateToSettings
                )
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
                        .verticalScroll(homeScrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Encabezado Editorial Aprobado
                    FadeSlideIn(screenReady, 0) {
                        val todayDate = com.jeremy.lumi.utils.LumiClock.today()
                        val dateFmt = remember {
                            val pattern = if (Locale.getDefault().language == "en") "EEEE, MMMM d" else "EEEE, d 'de' MMMM"
                            DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                        }
                        val dateEyebrow = remember(todayDate) { todayDate.format(dateFmt) }
                        val rawName = uiState.userName?.trim()?.takeIf { it.isNotBlank() }
                        val (titleNormal, titleItalic) = if (rawName != null) {
                            stringResource(R.string.home_greeting_hello) to "$rawName."
                        } else {
                            stringResource(R.string.onboarding_hello_simple) to ""
                        }

                        LumiEditorialHeading(
                            eyebrow = dateEyebrow,
                            titleNormal = titleNormal,
                            titleSerifItalic = titleItalic,
                            subtitle = stringResource(R.string.home_day_at_your_pace),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        )
                    }

                    // Banner de retraso si aplica
                    AnimatedVisibility(
                        visible = uiState.isLate,
                        enter   = fadeIn(tween(400)) + expandVertically(),
                        exit    = fadeOut(tween(300)) + shrinkVertically()
                    ) {
                        DelayBanner(
                            delayDays       = uiState.delayDays,
                            delayState      = uiState.delayState,
                            onPeriodArrived = { showConfirmPeriod = true },
                            onDismiss       = { viewModel.dismissDelayBanner() }
                        )
                    }

                    // Vínculos activos (Bonds) en Stories row si existen
                    if (uiState.linkedCycles.isNotEmpty()) {
                        FadeSlideIn(screenReady, STAGGER_MS / 2) {
                            CycleStoriesRow(
                                links = uiState.linkedCycles,
                                onLinkClick = { linkedSheetLink = it }
                            )
                        }
                    }

                    // Aro de Visualización del Ciclo Aprobado
                    FadeSlideIn(screenReady, STAGGER_MS) {
                        val phaseName = when (uiState.currentPhase) {
                            CyclePhase.MENSTRUAL -> stringResource(R.string.pm_phase_menstrual)
                            CyclePhase.FOLLICULAR -> stringResource(R.string.pm_phase_follicular)
                            CyclePhase.OVULATION -> stringResource(R.string.pm_phase_ovulation)
                            CyclePhase.LUTEAL -> stringResource(R.string.pm_phase_luteal)
                            else -> stringResource(R.string.phase_unknown)
                        }

                        val estimatedDaysText = if (uiState.prediction != null) {
                            val daysUntil = uiState.prediction!!.daysUntilNextPeriod
                            if (daysUntil > 0) stringResource(R.string.home_period_estimated_in_days, daysUntil)
                            else if (daysUntil == 0) stringResource(R.string.home_period_estimated_today)
                            else stringResource(R.string.home_period_late_in_days, -daysUntil)
                        } else stringResource(R.string.home_period_estimated_approx)

                        val confidenceLabel = uiState.predictionFactors?.confidenceLabel
                            ?: stringResource(R.string.cycle_ring_confidence_moderate)

                        LumiCycleRing(
                            currentDay = uiState.currentDayOfCycle,
                            cycleLength = uiState.prediction?.cycleLength ?: 28,
                            phaseName = phaseName,
                            estimatedDaysText = estimatedDaysText,
                            confidenceLabel = confidenceLabel
                        )
                    }

                    // Tira Semanal de 7 Días Aprobada
                    if (uiState.weekDays.isNotEmpty()) {
                        FadeSlideIn(screenReady, STAGGER_MS * 2) {
                            LumiWeekStrip(
                                weekDays = uiState.weekDays,
                                selectedDate = uiState.selectedDate,
                                onSelectDate = { viewModel.selectDate(it) },
                                onViewCalendarClick = onNavigateToCalendar,
                                onResetToToday = { viewModel.resetToToday() }
                            )
                        }
                    }

                    // Tarjeta de Registro Diario ("Un momento para ti")
                    FadeSlideIn(screenReady, STAGGER_MS * 3) {
                        val entrySubtitle = if (hasEntry && !todayLog?.dailyLog?.mood.isNullOrBlank()) {
                            stringResource(R.string.home_today_mood_status, todayLog?.dailyLog?.mood?.lowercase() ?: "")
                        } else if (hasEntry) {
                            stringResource(R.string.home_log_saved)
                        } else {
                            stringResource(R.string.home_log_pending)
                        }

                        LumiTodayCard(
                            hasEntry = hasEntry,
                            entrySubtitle = entrySubtitle,
                            onLogClick = { showQuickLogSheet = true }
                        )
                    }

                    // Nota Explicativa al pie con modal "Cómo lo calcula Lumi"
                    LumiPredictionFootnote(
                        onLearnMoreClick = { showPredictionInfoSheet = true }
                    )

                    // Recordatorios activos (si los hay)
                    FadeSlideIn(screenReady, STAGGER_MS * 4) {
                        RemindersSection()
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
        }

        var selectedPeriodOffset by remember { mutableIntStateOf(0) }

        if (showConfirmPeriod) {
            val offsetLabels = listOf(
                stringResource(R.string.home_offset_today),
                stringResource(R.string.home_offset_yesterday),
                stringResource(R.string.home_offset_2d),
                stringResource(R.string.home_offset_3d)
            )
            AlertDialog(
                onDismissRequest = { 
                    showConfirmPeriod = false 
                    selectedPeriodOffset = 0
                },
                title = { Text(stringResource(R.string.confirm_period_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.confirm_period_desc))
                        Text(
                            stringResource(R.string.home_confirm_period_when),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            offsetLabels.forEachIndexed { index, label ->
                                FilterChip(
                                    selected = selectedPeriodOffset == index,
                                    onClick = { selectedPeriodOffset = index },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmPeriod = false
                            val selectedDate = com.jeremy.lumi.utils.LumiClock.today().minusDays(selectedPeriodOffset.toLong())
                            viewModel.startNewPeriod(selectedDate)
                            selectedPeriodOffset = 0
                            showSuccessLottie = true
                        }
                    ) { Text(stringResource(R.string.confirm_period_yes)) }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showConfirmPeriod = false
                        selectedPeriodOffset = 0
                    }) { Text(stringResource(R.string.confirm_period_cancel)) }
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

        if (showThemeSheet) {
            val currentTheme by settingsViewModel.currentTheme.collectAsStateWithLifecycle()
            LumiThemeSheet(
                selectedTheme = currentTheme,
                onSelectTheme = { settingsViewModel.changeTheme(it) },
                onDismiss = { showThemeSheet = false }
            )
        }

        if (showPredictionInfoSheet) {
            LumiPredictionInfoSheet(
                predictionFactors = uiState.predictionFactors,
                prediction = uiState.prediction,
                onDismiss = { showPredictionInfoSheet = false }
            )
        }

        if (showNotificationsSheet) {
            LumiNotificationsSheet(
                isLate = uiState.isLate,
                delayDays = uiState.delayDays,
                prediction = uiState.prediction,
                currentPhase = uiState.currentPhase ?: CyclePhase.UNKNOWN,
                hasEntryToday = hasEntry,
                activeReminders = activeReminders,
                onQuickLogClick = { showQuickLogSheet = true },
                onDismiss = { showNotificationsSheet = false }
            )
        }

        if (showBalloonGame) BalloonGameSheet { showBalloonGame = false }
        if (showQuickLogSheet) {
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
                              sleepHours, energyLevel, stressLevel, waterIntake, bbt, spotting ->
                    quickLogViewModel.saveToday(flow, pain, mood, symptoms, mucus, notes,
                        hadIntercourse, protectionUsed, method, intercourseNotes, showOnCalendar,
                        sleepHours, energyLevel, stressLevel, waterIntake, bbt, spotting)
                }
            )
        }

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
        }
    }

    HugAnimationOverlay(visible = uiState.showHugAnimation)
}


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
                        contentDescription = stringResource(R.string.home_cd_link),
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
                        contentDescription = stringResource(R.string.home_cd_connections),
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
            text = stringResource(R.string.home_your_links),
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
                                .size(46.dp)
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
                    stringResource(R.string.home_link_to_start),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.home_link_to_start_desc),
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
                    Text(stringResource(R.string.home_create_link), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    stringResource(R.string.home_wait_message),
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
                        text = if (ownerName != null) stringResource(R.string.home_linked_cycle_of, ownerName) else stringResource(R.string.home_linked_cycle),
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

//  DELAY BANNER

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
        DelayState.LATE           -> if (delayDays == 1) stringResource(R.string.home_delay_late_one, delayDays) else stringResource(R.string.home_delay_late_other, delayDays)
        DelayState.VERY_LATE      -> stringResource(R.string.home_delay_very_late, delayDays)
        DelayState.EXTREMELY_LATE -> stringResource(R.string.home_delay_extremely_late, delayDays)
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
private fun HeaderSection(
    userName: String?,
    phase: CyclePhase,
    insightText: String,
    isDiscreetMode: Boolean
) {
    val phaseColor = if (isDiscreetMode) Color.Gray else homePhaseColor(phase)
    val phaseName = if (isDiscreetMode) stringResource(R.string.home_phase_hidden) else when (phase) {
        CyclePhase.MENSTRUAL  -> stringResource(R.string.home_phase_menstrual)
        CyclePhase.FOLLICULAR -> stringResource(R.string.home_phase_follicular)
        CyclePhase.OVULATION  -> stringResource(R.string.home_phase_ovulation)
        CyclePhase.LUTEAL     -> stringResource(R.string.home_phase_luteal)
        else                  -> stringResource(R.string.home_phase_unknown)
    }

    val greetingText = when {
        !userName.isNullOrBlank() -> stringResource(R.string.home_greeting_named, userName.split(" ").first())
        else -> stringResource(R.string.home_greeting_hello)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Saludo personalizado — siempre aquí, nunca "Día N"
        Text(
            text = greetingText,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        // Nombre de fase — color de fase
        Text(
            text = phaseName,
            color = phaseColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        // Insight diario — una línea, tipografía secundaria, tono neutro
        if (insightText.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = insightText,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
fun CycleWeekStrip(
    days: List<CycleDayUi>,
    isDiscreetMode: Boolean,
    onDayClick: ((CycleDayUi) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val todayIndex = remember(days) { days.indexOfFirst { it.isToday }.coerceAtLeast(0) }
    LaunchedEffect(todayIndex) {
        if (todayIndex > 0) {
            listState.scrollToItem((todayIndex - 2).coerceAtLeast(0))
        }
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(days, key = { it.date.toString() }) { day ->
            DayChip(day = day, isDiscreetMode = isDiscreetMode, onDayClick = onDayClick)
        }
    }
}

@Composable
private fun DayChip(
    day: CycleDayUi,
    isDiscreetMode: Boolean,
    onDayClick: ((CycleDayUi) -> Unit)? = null
) {
    val baseColor = if (isDiscreetMode) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else homePhaseColor(day.phase)
    val interSource = remember { MutableInteractionSource() }
    val isPressed by interSource.collectIsPressedAsState()
    val chipScale by animateFloatAsState(if (isPressed) 0.93f else 1f, snappySpring, label = "chip_scale")

    val isDashedPrediction = !isDiscreetMode && day.isPrediction && day.phase == CyclePhase.MENSTRUAL && !day.isSelected

    val bg = when {
        day.isSelected -> (if (isDiscreetMode) MaterialTheme.colorScheme.primaryContainer else baseColor).copy(alpha = 0.20f)
        day.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        isDiscreetMode -> Color.Transparent
        day.phase == CyclePhase.MENSTRUAL -> {
            if (day.isPrediction) baseColor.copy(alpha = 0.08f) else baseColor.copy(alpha = 0.22f)
        }
        day.phase == CyclePhase.OVULATION -> {
            if (day.isPrediction) baseColor.copy(alpha = 0.08f) else baseColor.copy(alpha = 0.18f)
        }
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val dotColor = when {
        isDiscreetMode -> Color.Transparent
        day.phase == CyclePhase.MENSTRUAL -> if (day.isPrediction) baseColor.copy(alpha = 0.55f) else baseColor
        day.phase == CyclePhase.OVULATION -> if (day.isPrediction) baseColor.copy(alpha = 0.55f) else baseColor
        day.phase == CyclePhase.FOLLICULAR -> baseColor.copy(alpha = 0.70f)
        day.phase == CyclePhase.LUTEAL -> baseColor.copy(alpha = 0.70f)
        else -> Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .scale(chipScale)
            .width(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isDashedPrediction) {
                    Modifier.drawBehind {
                        val stroke = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                        )
                        drawRoundRect(
                            color = baseColor.copy(alpha = 0.70f),
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                            style = stroke
                        )
                    }
                } else if (day.isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = if (isDiscreetMode) MaterialTheme.colorScheme.primary else baseColor,
                        shape = RoundedCornerShape(18.dp)
                    )
                } else if (day.isToday) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(18.dp)
                    )
                } else Modifier
            )
            .background(bg)
            .then(
                if (onDayClick != null) {
                    Modifier.clickable(
                        interactionSource = interSource,
                        indication = null
                    ) { onDayClick(day) }
                } else Modifier
            )
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = day.weekdayLabel,
            fontSize = 11.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = day.dayOfMonth.toString(),
            fontSize = 16.sp,
            fontWeight = if (day.isToday || day.isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = when {
                day.isToday -> MaterialTheme.colorScheme.primary
                day.phase == CyclePhase.MENSTRUAL -> baseColor
                day.phase == CyclePhase.OVULATION -> baseColor
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
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
fun CycleRingCard(
    currentDay: Int, phase: CyclePhase, prediction: CyclePrediction?, isDiscreetMode: Boolean, 
    predictionFactors: PredictionFactorsUi? = null,
    streakDays: Int = 0, isLate: Boolean = false, 
    todayLog: DailyLogEntity? = null, onMetricClick: () -> Unit = {},
    onLogPeriod: () -> Unit = {}
) {
    val dialStyle = LocalDialStyle.current
    val phaseColors = LocalPhaseColors.current
    val actualPhaseColor = homePhaseColor(phase)
    val phaseColor = if (isDiscreetMode) Color.Gray.copy(alpha = 0.6f) else actualPhaseColor
    val animatedPhaseColor by animateColorAsState(phaseColor, tween(700), label = "phase_color")
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    val cycleLen  = (prediction?.cycleLength  ?: 28)
    val cycleLenF = cycleLen.toFloat()
    val targetProgress = if (currentDay > 0) (currentDay / cycleLenF).coerceIn(0f, 1f) else 0f
    
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(currentDay) {
        animProgress.animateTo(targetProgress, tween(1100, easing = FastOutSlowInEasing))
    }

    val daysLeft = if (currentDay > 0) (prediction?.daysUntilNextPeriod ?: (cycleLen - currentDay)).coerceAtLeast(0) else 0
    val periodLength = prediction?.periodLength ?: 5
    val isPeriodActive = phase == CyclePhase.MENSTRUAL || (currentDay in 1..periodLength)
    val delayDays = prediction?.delayDays ?: 0

    val (ringMainText, ringSubText) = when {
        isDiscreetMode -> {
            Pair(
                stringResource(R.string.home_ring_dash),
                stringResource(R.string.home_phase_hidden)
            )
        }
        currentDay <= 0 -> {
            Pair(
                stringResource(R.string.home_ring_dash),
                stringResource(R.string.home_phase_unknown)
            )
        }
        isPeriodActive -> {
            Pair(
                stringResource(R.string.home_ring_period_day, currentDay),
                stringResource(R.string.home_ring_period_active)
            )
        }
        isLate && delayDays in 1..3 -> {
            Pair(
                stringResource(R.string.home_ring_normal_variation_title),
                stringResource(R.string.home_ring_normal_variation_sub, delayDays)
            )
        }
        isLate && delayDays > 3 -> {
            Pair(
                stringResource(R.string.home_ring_late_title, delayDays),
                stringResource(R.string.home_ring_late_sub)
            )
        }
        daysLeft in 1..4 && prediction != null && prediction.earliestPeriodDate != LocalDate.MAX -> {
            val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
            val earliestStr = prediction.earliestPeriodDate.format(dateFmt)
            val latestStr = prediction.latestPeriodDate.format(dateFmt)
            val windowRange = if (prediction.earliestPeriodDate == prediction.latestPeriodDate) {
                earliestStr
            } else {
                "$earliestStr – $latestStr"
            }
            val main = if (daysLeft <= 2) {
                stringResource(R.string.home_ring_soon)
            } else {
                stringResource(R.string.home_ring_approx_days_range, (daysLeft - 1).coerceAtLeast(1), daysLeft + 1)
            }
            Pair(
                main,
                stringResource(R.string.home_ring_window_subtitle, windowRange)
            )
        }
        else -> {
            Pair(
                stringResource(R.string.home_ring_approx_days, daysLeft),
                stringResource(R.string.home_ring_estimated_arrival)
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val ringBgColor = if (isDark) Color(0xFF33333C) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        val arcColor = if (isDiscreetMode) Color.Gray else animatedPhaseColor

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(280.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 16.dp.toPx()
                // Base neutra circular
                drawCircle(
                    color = ringBgColor,
                    style = Stroke(width = strokeWidth)
                )

                when (dialStyle) {
                    DialStyle.GRADIENT -> {
                        // ── Modo Gradiente Continuo (Diseño original fluido con todos los colores) ──
                        if (!isDiscreetMode && currentDay > 0) {
                            val bgAlpha = if (isDark) 0.28f else 0.22f
                            val bgSweepGradient = Brush.sweepGradient(
                                colors = listOf(
                                    phaseColors.menstrual.copy(alpha = bgAlpha),
                                    phaseColors.follicular.copy(alpha = bgAlpha),
                                    phaseColors.ovulation.copy(alpha = bgAlpha),
                                    phaseColors.luteal.copy(alpha = bgAlpha),
                                    phaseColors.menstrual.copy(alpha = bgAlpha)
                                )
                            )
                            rotate(-90f) {
                                drawCircle(
                                    brush = bgSweepGradient,
                                    style = Stroke(width = strokeWidth)
                                )
                            }
                        }

                        if (animProgress.value > 0f) {
                            if (isDiscreetMode) {
                                drawArc(
                                    color = Color.Gray,
                                    startAngle = -90f,
                                    sweepAngle = animProgress.value * 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            } else {
                                val activeSweepGradient = Brush.sweepGradient(
                                    colors = listOf(
                                        phaseColors.menstrual,
                                        phaseColors.follicular,
                                        phaseColors.ovulation,
                                        phaseColors.luteal,
                                        phaseColors.menstrual
                                    )
                                )
                                rotate(-90f) {
                                    drawArc(
                                        brush = activeSweepGradient,
                                        startAngle = 0f,
                                        sweepAngle = animProgress.value * 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }
                    }

                    DialStyle.CYCLE_MAP -> {
                        // ── Modo Mapa del Ciclo (4 sectores proporcionales bien delimitados) ──
                        if (!isDiscreetMode && currentDay > 0) {
                            val bgAlpha = if (isDark) 0.28f else 0.22f
                            val totalDays = cycleLenF.coerceAtLeast(15f)

                            val pDays = periodLength.toFloat().coerceIn(1f, totalDays)
                            val pSweep = (pDays / totalDays) * 360f

                            val ovDay = (cycleLen - 14).coerceIn((pDays.toInt() + 2), cycleLen - 2)
                            val fertStart = (ovDay - 5).coerceAtLeast(pDays.toInt() + 1)
                            val fertEnd = (ovDay + 1).coerceAtMost(cycleLen - 1)
                            val fertDays = (fertEnd - fertStart + 1).toFloat().coerceAtLeast(1f)
                            val ovSweep = (fertDays / totalDays) * 360f

                            val folDays = (fertStart - 1 - pDays.toInt()).coerceAtLeast(0).toFloat()
                            val folSweep = (folDays / totalDays) * 360f

                            val lutSweep = (360f - (pSweep + folSweep + ovSweep)).coerceAtLeast(0f)

                            drawArc(
                                color = phaseColors.menstrual.copy(alpha = bgAlpha),
                                startAngle = -90f,
                                sweepAngle = pSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                            drawArc(
                                color = phaseColors.follicular.copy(alpha = bgAlpha),
                                startAngle = -90f + pSweep,
                                sweepAngle = folSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                            drawArc(
                                color = phaseColors.ovulation.copy(alpha = bgAlpha),
                                startAngle = -90f + pSweep + folSweep,
                                sweepAngle = ovSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                            drawArc(
                                color = phaseColors.luteal.copy(alpha = bgAlpha),
                                startAngle = -90f + pSweep + folSweep + ovSweep,
                                sweepAngle = lutSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth)
                            )
                        }

                        // Arco de progreso activo al frente
                        if (animProgress.value > 0f) {
                            drawArc(
                                color = arcColor,
                                startAngle = -90f,
                                sweepAngle = animProgress.value * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    DialStyle.MINIMALIST -> {
                        // ── Modo Minimalista (pista monocromática y solo color de fase activa) ──
                        if (animProgress.value > 0f) {
                            drawArc(
                                color = arcColor,
                                startAngle = -90f,
                                sweepAngle = animProgress.value * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = animatedPhaseColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ringMainText,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = if (ringMainText.length > 10) 22.sp else 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = ringSubText,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }

            // ── Badge de certeza integrado al borde del aro (Fix 4) ──────────
            // Posicionado en borde superior-derecho (~45deg). Aro de 280dp.
            if (predictionFactors != null && !isDiscreetMode) {
                val badgeColor = animatedPhaseColor
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = 86.dp, y = 14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(badgeColor.copy(alpha = if (isDark) 0.22f else 0.15f))
                        .border(1.dp, badgeColor.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Text(
                            text = predictionFactors.confidenceLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Fix 6: mostrar tarjeta consolidada si ambos datos están vacíos
        if (todayLog?.waterIntake == null && todayLog?.sleepHours == null) {
            TodayLogCard(onClick = onMetricClick)
        } else {
            DailyMetricsRow(waterIntake = todayLog?.waterIntake, sleepHours = todayLog?.sleepHours, onClick = onMetricClick)
        }
        
        if (predictionFactors != null && !isDiscreetMode) {
            Spacer(Modifier.height(16.dp))
            PredictionFactorsCard(
                factors = predictionFactors,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun DailyInsightCard(phase: CyclePhase, currentDay: Int, cycleLen: Int, isDiscreetMode: Boolean) {
    val color = if (isDiscreetMode) Color.Gray else homePhaseColor(phase)
    val secondaryAura = MaterialTheme.colorScheme.secondary
    val message = if (isDiscreetMode) stringResource(R.string.home_insight_discreet) else getDynamicPhaseMessage(phase, currentDay, cycleLen)

    AuraCard(
        primaryAuraColor = color,
        secondaryAuraColor = secondaryAura,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        // Tag / Badge superior de fase
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDiscreetMode) stringResource(R.string.home_insight_badge) 
                               else "${stringResource(phase.phaseNameRes)} · ${stringResource(R.string.home_greeting_day, currentDay)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.home_insight_title),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = (-0.2).sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
    }
}

@Composable
fun getDynamicPhaseMessage(phase: CyclePhase, currentDay: Int, cycleLength: Int): String {
    return when (phase) {
        CyclePhase.MENSTRUAL -> {
            if (currentDay <= 2) stringResource(R.string.home_insight_menstrual_1)
            else stringResource(R.string.home_insight_menstrual_2)
        }
        CyclePhase.FOLLICULAR -> {
            stringResource(R.string.home_insight_follicular)
        }
        CyclePhase.OVULATION -> {
            stringResource(R.string.home_insight_ovulation)
        }
        CyclePhase.LUTEAL -> {
            val daysToNext = cycleLength - currentDay
            if (daysToNext in 1..4) {
                stringResource(R.string.home_insight_luteal_window)
            } else if (daysToNext > 7) {
                stringResource(R.string.home_insight_luteal_1)
            } else {
                stringResource(R.string.home_insight_luteal_2)
            }
        }
        else -> stringResource(R.string.home_insight_unknown)
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
        daysUntil < 0 -> stringResource(R.string.home_ring_late)
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
    // Versión compacta: TextButton discreto en vez de tarjeta prominente.
    // La función de vincular es secundaria y no debe dominar el layout antes del anillo.
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Rounded.PeopleAlt,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.home_link_partner),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tarjeta consolidada de estado vacío — Fix 6
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodayLogCard(onClick: () -> Unit) {
    val isDark = LocalIsDarkTheme.current
    val surfaceColor = if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000)
    val borderColor  = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                       else Color(0x14000000)
    val shape        = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.home_today_log_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_today_log_hint),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f)
            )
        }
    }
}

@Composable
fun DailyMetricsRow(waterIntake: Float?, sleepHours: Float?, onClick: () -> Unit) {
    val isDark = LocalIsDarkTheme.current
    val waterColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1976D2)
    val sleepColor = if (isDark) Color(0xFFB39DDB) else Color(0xFF6750A4)
    val waterBg    = waterColor.copy(alpha = if (isDark) 0.18f else 0.12f)
    val sleepBg    = sleepColor.copy(alpha = if (isDark) 0.18f else 0.12f)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.WaterDrop,
            iconColor = waterColor,
            iconBgColor = waterBg,
            title = stringResource(R.string.home_metric_water),
            value = waterIntake?.let { stringResource(R.string.home_metric_water_value, it) } ?: stringResource(R.string.home_metric_water_empty),
            onClick = onClick
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Nightlight,
            iconColor = sleepColor,
            iconBgColor = sleepBg,
            title = stringResource(R.string.home_metric_sleep),
            value = sleepHours?.let { stringResource(R.string.home_metric_sleep_value, it) } ?: stringResource(R.string.home_metric_sleep_empty),
            onClick = onClick
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    // En dark themes, el borde usa mayor opacidad para que las tarjetas sean visibles
    // especialmente en NOIR (fondo #000000 donde la superficie casi desaparece)
    val GlassSurface = if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000)
    val GlassBorder  = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                       else Color(0x1A000000)
    val dynamicIconBg = if (isDark) iconBgColor else iconColor.copy(alpha = 0.15f)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.radialGradient(listOf(GlassSurface, Color.Transparent)))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(dynamicIconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
