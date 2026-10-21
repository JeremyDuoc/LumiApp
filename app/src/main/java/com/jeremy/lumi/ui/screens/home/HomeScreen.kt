package com.jeremy.lumi.ui.screens.home

import com.jeremy.lumi.ui.theme.LumiMotion
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import com.jeremy.lumi.ui.theme.LocalIsDarkTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.jeremy.lumi.ui.screens.partner.ObserverScreen
import com.jeremy.lumi.ui.screens.partner.PartnerViewModel
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import com.jeremy.lumi.ui.components.*
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.ui.theme.AmbientScreen
import com.jeremy.lumi.ui.screens.settings.SettingsViewModel
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
private const val BREATH_MS  = 3800

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
    quickLogViewModel: QuickLogViewModel = hiltViewModel(),
    partnerViewModel: PartnerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    // Hooks optativos para el logo compartido con la transición del onboarding.
    onBrandBounds: ((Rect) -> Unit)? = null,
    suppressBrandDuringHandoff: Boolean = false
) {
    val uiState           by viewModel.uiState.collectAsStateWithLifecycle()
    val tokens            = LumiThemeTokens.current
    val currentTheme      by settingsViewModel.currentTheme.collectAsStateWithLifecycle()
    var showThemeSheet    by remember { mutableStateOf(false) }
    var showBalloonGame   by remember { mutableStateOf(false) }
    var showQuickLogSheet by remember { mutableStateOf(false) }
    var showPredictionInfoSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showAddReminderSheet by remember { mutableStateOf(false) }
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
            var showConfirmPeriod by remember { mutableStateOf(false) }
            var showSuccessLottie by remember { mutableStateOf(false) }

            val isDark = LocalIsDarkTheme.current
            var radialOpen by remember { mutableStateOf(false) }

            val blurEased = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }
            val targetBlur = if (radialOpen) (if (isDark) 8.dp else 5.dp) else 0.dp
            val blurRadius by animateDpAsState(
                targetValue = targetBlur,
                animationSpec = if (radialOpen) {
                    tween(durationMillis = 420, easing = blurEased)
                } else {
                    tween(durationMillis = 320, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
                },
                label = "home_content_blur"
            )

            val scrimAlpha by animateFloatAsState(
                targetValue = if (radialOpen) 1f else 0f,
                animationSpec = if (radialOpen) {
                    tween(durationMillis = 400, easing = blurEased)
                } else {
                    tween(durationMillis = 300, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
                },
                label = "home_scrim_alpha"
            )

            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                // Contenido y fondo de Home con desenfoque suave y reactivo al abrir acciones rápidas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (blurRadius > 0.dp) {
                                Modifier.blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    AmbientBackgroundLayer(
                        screenType           = AmbientScreen.HOME,
                        accentColor          = currentAuraColor,
                        scrollOffsetProvider = { homeScrollState.value.toFloat() }
                    )

        Scaffold(
            topBar = {
                val hasNotificationAlert = !hasEntry || uiState.isLate || ((uiState.prediction?.daysUntilNextPeriod ?: 99) in 0..3) || activeReminders.isNotEmpty()
                LumiTopBar(
                    hasNotificationAlert = hasNotificationAlert,
                    onThemeClick = { showThemeSheet = true },
                    onNotificationsClick = { showNotificationsSheet = true },
                    onChatClick = onNavigateToChat,
                    onSettingsClick = onNavigateToSettings,
                    onBrandBounds = onBrandBounds,
                    hideBrand = suppressBrandDuringHandoff
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. SALUDO EDITORIAL LIBRE (sin tarjetas ni fondos)
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
                            titleSize = 29.sp,
                            serifSize = 31.sp,
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
                        Column {
                            Spacer(Modifier.height(14.dp))
                            DelayBanner(
                                delayDays       = uiState.delayDays,
                                delayState      = uiState.delayState,
                                onPeriodArrived = { showConfirmPeriod = true },
                                onDismiss       = { viewModel.dismissDelayBanner() }
                            )
                        }
                    }

                    // Vínculos activos (Bonds) en Stories row si existen
                    if (uiState.linkedCycles.isNotEmpty()) {
                        Column {
                            Spacer(Modifier.height(14.dp))
                            FadeSlideIn(screenReady, STAGGER_MS / 2) {
                                CycleStoriesRow(
                                    links = uiState.linkedCycles,
                                    onLinkClick = { linkedSheetLink = it }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // 2. NÚCLEO VIVO DE LUMI + STATUS DE PREDICCIÓN (Hero central)
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

                        LumiLivingCore(
                            currentDay = uiState.currentDayOfCycle,
                            currentPhase = uiState.currentPhase ?: CyclePhase.UNKNOWN,
                            phaseName = phaseName,
                            estimatedDaysText = estimatedDaysText,
                            confidenceLabel = confidenceLabel,
                            dayContextLabel = uiState.selectedDate?.format(
                                DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                            )?.uppercase(Locale.getDefault())
                                ?: stringResource(R.string.cal_today_cd).uppercase(Locale.getDefault()),
                            onConfidenceClick = { showPredictionInfoSheet = true }
                        )
                    }

                    // 3. Separador fino y semana, como en el prototipo.
                    if (uiState.weekDays.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            color = tokens.line.copy(alpha = .8f),
                            thickness = .5.dp
                        )
                        FadeSlideIn(screenReady, STAGGER_MS * 2) {
                            LumiWeekTimeline(
                                weekDays = uiState.weekDays,
                                selectedDate = uiState.selectedDate,
                                onSelectDate = { viewModel.selectDate(it) },
                                onViewCalendarClick = onNavigateToCalendar,
                                onResetToToday = { viewModel.resetToToday() }
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // 4. Las acciones primarias viven en LumiRadialQuickMenu,
                    //    anclado abajo a la derecha. Evitar el Dock duplicado.
                    Spacer(Modifier.height(12.dp))

                    // 5. INSIGHT / CONTENIDO CONTEXTUAL (Secundario)
                    FadeSlideIn(screenReady, STAGGER_MS * 4) {
                        LumiEditorialInsightCard(
                            phase = uiState.currentPhase,
                            currentDay = uiState.currentDayOfCycle,
                            cycleLength = uiState.prediction?.cycleLength ?: 28,
                            isDiscreetMode = uiState.isDiscreetMode
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // 6. Si hay recordatorios activos se siguen mostrando aquí.
                    //    Crear uno nuevo se hace desde el menú radial, sin duplicar CTA.
                    if (activeReminders.isNotEmpty()) {
                        FadeSlideIn(screenReady, STAGGER_MS * 5) {
                            RemindersSection(viewModel = remindersViewModel)
                        }
                    }

                    Spacer(Modifier.height(88.dp))
                }
            }
        }
        } // Cierre del Box desenfocado del Home

        // Capa de difuminado (scrim) animada muy limpia y suave sobre el Home
        if (scrimAlpha > 0.001f) {
            val scrimColors = if (isDark) {
                listOf(
                    tokens.bg.copy(alpha = 0.18f),
                    tokens.bg.copy(alpha = 0.32f)
                )
            } else {
                listOf(
                    tokens.bg.copy(alpha = 0.16f),
                    tokens.bg.copy(alpha = 0.28f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimAlpha }
                    .background(Brush.verticalGradient(scrimColors))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { radialOpen = false }
            )
        }

        LumiRadialQuickMenu(
            expanded = radialOpen,
            onExpandedChange = { radialOpen = it },
            onLogClick = { showQuickLogSheet = true },
            onPeriodClick = { showConfirmPeriod = true },
            onReminderClick = { showAddReminderSheet = true },
            diaryAlreadyLogged = hasEntry,
            modifier = Modifier.align(Alignment.BottomEnd),
            bottomMargin = 16.dp
        )

        if (showConfirmPeriod) {
            val today = com.jeremy.lumi.utils.LumiClock.today()
            val todayMillis = today.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            val cycleStart = uiState.activeCycle?.startDate
            val selectableDates = remember(todayMillis, cycleStart) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        utcTimeMillis <= todayMillis && (cycleStart == null || utcTimeMillis > cycleStart)
                    override fun isSelectableYear(year: Int): Boolean = year <= today.year
                }
            }
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = todayMillis.takeIf { selectableDates.isSelectableDate(it) },
                selectableDates = selectableDates
            )
            DatePickerDialog(
                onDismissRequest = { showConfirmPeriod = false },
                confirmButton = {
                    TextButton(
                        enabled = dateState.selectedDateMillis?.let(selectableDates::isSelectableDate) == true,
                        onClick = {
                            val millis = dateState.selectedDateMillis ?: return@TextButton
                            if (!selectableDates.isSelectableDate(millis)) return@TextButton
                            showConfirmPeriod = false
                            viewModel.startNewPeriod(java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate())
                        }
                    ) { Text(stringResource(R.string.confirm_period_yes)) }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmPeriod = false }) {
                        Text(stringResource(R.string.confirm_period_cancel))
                    }
                }
            ) {
                DatePicker(
                    state = dateState,
                    title = { Text(stringResource(R.string.home_confirm_period_when), modifier = Modifier.padding(start = 24.dp, top = 16.dp)) }
                )
            }
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
                onConfigureReminders = { 
                    showNotificationsSheet = false
                    showAddReminderSheet = true 
                },
                onDismiss = { showNotificationsSheet = false }
            )
        }

        if (showAddReminderSheet) {
            AddReminderSheetContainer(
                viewModel = remindersViewModel,
                onDismiss = { showAddReminderSheet = false }
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

        val themeRevealTrigger = com.jeremy.lumi.ui.theme.LocalThemeRevealTrigger.current

        if (showThemeSheet) {
            LumiThemeSheet(
                selectedTheme = currentTheme,
                onSelectThemeAt = { palette, center ->
                    if (themeRevealTrigger != null) {
                        themeRevealTrigger.changeThemeWithReveal(palette, center)
                    } else {
                        settingsViewModel.changeTheme(palette)
                    }
                    showThemeSheet = false
                },
                onSelectTheme = { palette ->
                    if (themeRevealTrigger != null) {
                        themeRevealTrigger.changeThemeWithReveal(palette, null)
                    } else {
                        settingsViewModel.changeTheme(palette)
                    }
                    showThemeSheet = false
                },
                onDismiss = { showThemeSheet = false }
            )
        }
    }
        }
    }

    HugAnimationOverlay(visible = uiState.showHugAnimation)
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
                        // Anillo de fase
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
                        // Avatar interior con inicial tipográfica (sin emojis)
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(phaseColor.copy(alpha = 0.25f), phaseColor.copy(alpha = 0.06f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.take(1).uppercase(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = phaseColor
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
                        Icon(
                            Icons.Rounded.PeopleAlt,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(52.dp)
                        )
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(phaseColor)
                    )
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
    val progress = remember { Animatable(0f) }
    val spec = remember(delayMs) { LumiMotion.homeEntrance(delayMs) }
    LaunchedEffect(visible, spec) {
        if (visible) progress.animateTo(1f, spec) else progress.snapTo(0f)
    }
    Box(Modifier.graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 18.dp.toPx()
        scaleX = 0.98f + 0.02f * p
        scaleY = scaleX
    }) { content() }
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

