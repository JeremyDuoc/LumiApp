package com.jeremy.lumi.ui.screens.onboarding

import com.jeremy.lumi.ui.theme.LumiMotion
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithCache
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.lumi.domain.model.OnboardingData
import com.jeremy.lumi.domain.model.SecondaryGoal
import com.jeremy.lumi.domain.model.UserGoal
import com.jeremy.lumi.ui.components.LumiBrandFlower
import com.jeremy.lumi.ui.components.LumiBrandLockup
import com.jeremy.lumi.ui.screens.settings.SettingsViewModel
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import androidx.compose.ui.res.stringResource
import com.jeremy.lumi.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val STEP_TITLE_RES = listOf(
    R.string.onboarding_step_welcome,
    R.string.onboarding_step_security,
    R.string.onboarding_step_name,
    R.string.onboarding_step_cycle,
    R.string.onboarding_step_last_period,
    R.string.onboarding_step_preferences,
    R.string.onboarding_step_customization,
    R.string.onboarding_step_ready
)

/**
 * Onboarding v3 nativo de Lumi — Transcripción fidedigna de:
 * Lumi_Onboarding_Logo_Contextual_v3 (1).html
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.currentTheme.collectAsStateWithLifecycle()
    val tokens = LumiThemeTokens.current
    val scrollState = rememberScrollState()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val stepNumber = uiState.currentStepNumber
    val isFirstStep = uiState.currentStepIndex == 0
    val isFinalStep = stepNumber == 7
    var isLaunchingHome by remember(stepNumber) { mutableStateOf(false) }
    val summaryExit = remember { Animatable(0f) }
    val bloom = remember { Animatable(0f) }
    val handoff = remember { Animatable(0f) }
    val complete by rememberUpdatedState(onComplete)
    LaunchedEffect(isLaunchingHome, stepNumber) {
        if (isLaunchingHome) {
            launch { summaryExit.animateTo(1f, LumiMotion.SummaryExit) }
            launch { bloom.animateTo(1f, LumiMotion.Bloom) }
            handoff.animateTo(1f, LumiMotion.BloomHandoff)
            viewModel.completeOnboarding(complete)
        } else {
            summaryExit.snapTo(0f)
            bloom.snapTo(0f)
            handoff.snapTo(0f)
        }
    }
    val finishExit = Modifier.graphicsLayer {
        alpha = 1f - summaryExit.value
        translationY = summaryExit.value * 28.dp.toPx()
    }
    val showHeaderBrand = stepNumber != 0 && stepNumber != 7

    val isBlush = currentTheme == AppThemePalette.PETALO

    // Resetear scroll al cambiar de paso
    LaunchedEffect(stepNumber) {
        scrollState.scrollTo(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.bg)
    ) {
        // Fondo ambiental suave superior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (tokens.isDark) Color(0x18B884CA) else Color(0x35E4AFDB),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── TOP BAR: Header Brand + Theme Toggle ──────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header Brand: visible solo en pasos intermedios (1..6)
                AnimatedVisibility(
                    visible = showHeaderBrand,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 2 },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 }
                ) {
                    LumiBrandLockup(
                        flowerSize = 21,
                        fontSize = 22.sp,
                        starSize = 11.sp,
                        letterSpacing = (-1.2).sp,
                        animated = true
                    )
                }

                if (!showHeaderBrand) {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Botón de alternancia de tema (Noir <-> Blush)
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(tokens.panel)
                        .border(1.dp, tokens.line, RoundedCornerShape(22.dp))
                        .clickable {
                            val nextTheme = if (isBlush) AppThemePalette.NOIR else AppThemePalette.PETALO
                            settingsViewModel.changeTheme(nextTheme)
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBlush) stringResource(R.string.onboarding_theme_blush) else stringResource(R.string.onboarding_theme_noir),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = tokens.muted
                    )
                }
            }

            // ── STEPBAR: Indicador de paso y barra de progreso animada ────────
            AnimatedVisibility(
                visible = !isFinalStep,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    val stepIndex = uiState.currentStepIndex + 1
                    val totalSteps = uiState.totalStepsCount
                    val title = STEP_TITLE_RES.getOrNull(stepNumber)?.let { stringResource(it) } ?: ""

                    Text(
                        text = "${stepIndex.toString().padStart(2, '0')} / ${totalSteps.toString().padStart(2, '0')} · $title",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = tokens.muted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val animatedProgress by animateFloatAsState(
                        targetValue = stepIndex.toFloat() / totalSteps.toFloat(),
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                        label = "progress_bar"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(tokens.line)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            tokens.rose,
                                            tokens.accent
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            // ── CONTENIDO PRINCIPAL: AnimatedContent por paso ─────────────────
            AnimatedContent(
                targetState = stepNumber,
                transitionSpec = {
                    val forward = targetState > initialState
                    val direction = if (forward) 1 else -1
                    ((slideInHorizontally(LumiMotion.StepSlide) { (it * 0.18f * direction).toInt() } + fadeIn(LumiMotion.StepFade))
                        togetherWith (slideOutHorizontally(LumiMotion.StepExitSlide) { (-it * 0.18f * direction).toInt() } + fadeOut(LumiMotion.ShortFade)))
                        .using(SizeTransform(clip = false))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboarding_step_content"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    when (step) {
                        0 -> StepWelcome(
                            mode = uiState.data.mode,
                            onSelectMode = { viewModel.setMode(it) }
                        )
                        1 -> StepSafety(
                            mode = uiState.data.mode,
                            medicalAccepted = uiState.data.medicalDisclaimerAccepted,
                            privacyAccepted = uiState.data.privacyAccepted,
                            onToggleMedical = { viewModel.setMedicalAccepted(!uiState.data.medicalDisclaimerAccepted) },
                            onTogglePrivacy = { viewModel.setPrivacyAccepted(!uiState.data.privacyAccepted) },
                            onOpenPrivacyInfo = { showPrivacyDialog = true }
                        )
                        2 -> StepName(
                            name = uiState.data.userName ?: "",
                            onNameChange = { viewModel.setUserName(it) }
                        )
                        3 -> StepCycle(
                            regularityChoice = uiState.data.regularityChoice,
                            onSelectRegularity = { viewModel.setRegularity(it) },
                            cycleKnown = uiState.data.cycleLengthKnown,
                            cycleDays = uiState.data.cycleLength,
                            onToggleCycleKnown = { viewModel.setCycleLengthKnown(!uiState.data.cycleLengthKnown) },
                            onCycleDaysChange = { viewModel.setCycleLength(it) }
                        )
                        4 -> StepDate(
                            dateKnown = uiState.data.lastPeriodKnown,
                            dateEpoch = uiState.data.lastPeriodDate,
                            onToggleDateKnown = { viewModel.setLastPeriodKnown(it) },
                            onOpenDatePicker = { showDatePicker = true }
                        )
                        5 -> StepPreferences(
                            mode = uiState.data.mode,
                            selectedGoal = uiState.data.userGoal,
                            onSelectGoal = { viewModel.setUserGoal(it) },
                            contraceptionChoice = uiState.data.contraceptionChoice,
                            onSelectContraception = { viewModel.setContraceptionChoice(it) }
                        )
                        6 -> StepPersonalize(
                            periodKnown = uiState.data.periodLengthKnown,
                            periodDays = uiState.data.periodLength,
                            onTogglePeriodKnown = { viewModel.setPeriodLengthKnown(!uiState.data.periodLengthKnown) },
                            onPeriodDaysChange = { viewModel.setPeriodLength(it) },
                            secondaryGoals = uiState.data.secondaryGoals,
                            onToggleSecondary = { viewModel.toggleSecondaryGoal(it) },
                            age = uiState.data.age,
                            height = uiState.data.height,
                            weight = uiState.data.weight,
                            onUpdatePhysical = { a, h, w -> viewModel.setPhysicalProfile(a, h, w) }
                        )
                        7 -> StepFinish(
                            data = uiState.data,
                            onEditStep = { if (!isLaunchingHome) viewModel.jumpToStep(it) },
                            finishExit = finishExit,
                            bloom = { bloom.value }
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            // ── FOOTER: Botón primario + Navegación secundaria ─────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, tokens.bg.copy(alpha = 0.95f), tokens.bg)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                val primaryText = when {
                    isFirstStep -> stringResource(R.string.onboarding_btn_start)
                    isFinalStep -> stringResource(R.string.onboarding_btn_view_home)
                    stepNumber == 6 -> stringResource(R.string.onboarding_btn_finish_arrow)
                    else -> stringResource(R.string.onboarding_btn_continue_arrow)
                }

                Button(
                    onClick = {
                        if (isLaunchingHome) return@Button
                        if (isFinalStep) {
                            isLaunchingHome = true
                        } else {
                            viewModel.nextStep()
                        }
                    },
                    enabled = uiState.isNextEnabled && !uiState.isCompleting && !isLaunchingHome,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .then(if (isFinalStep) finishExit else Modifier)
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (uiState.isNextEnabled && !uiState.isCompleting) {
                                Brush.linearGradient(listOf(Color(0xFFEAB0CE), Color(0xFFC6B1F4)))
                            } else {
                                Brush.linearGradient(listOf(tokens.line, tokens.line))
                            }
                        )
                ) {
                    if (uiState.isCompleting) {
                        CircularProgressIndicator(
                            color = tokens.text,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = primaryText,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.2).sp,
                            color = if (uiState.isNextEnabled) Color(0xFF23182C) else tokens.muted
                        )
                    }
                }

                // Sub-footer: Volver / Omitir / Nota
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isFirstStep) {
                        TextButton(
                            onClick = { viewModel.prevStep() },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_btn_back_arrow),
                                fontSize = 12.sp,
                                color = tokens.muted
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (stepNumber in listOf(2, 6)) {
                        TextButton(
                            onClick = { viewModel.nextStep() },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_omit),
                                fontSize = 12.sp,
                                color = tokens.muted
                            )
                        }
                    } else if (isFinalStep) {
                        Text(
                            text = stringResource(R.string.onboarding_data_stored_locally),
                            fontSize = 10.5.sp,
                            color = tokens.muted
                        )
                    }
                }
            }
        }
    }

    // ── DIÁLOGO: POLÍTICA DE PRIVACIDAD ──────────────────────────────────────
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = tokens.panel,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.onboarding_privacy_dialog_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.onboarding_privacy_absolute_intro),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = tokens.text
                    )
                    Text(
                        text = stringResource(R.string.onboarding_privacy_point_1),
                        fontSize = 12.sp,
                        color = tokens.muted
                    )
                    Text(
                        text = stringResource(R.string.onboarding_privacy_point_2),
                        fontSize = 12.sp,
                        color = tokens.muted
                    )
                    Text(
                        text = stringResource(R.string.onboarding_privacy_point_3),
                        fontSize = 12.sp,
                        color = tokens.muted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.onboarding_dialog_understood), color = tokens.bg, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── DIÁLOGO: SELECTOR DE FECHA NATIVO M3 ─────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (uiState.data.lastPeriodDate > 0) uiState.data.lastPeriodDate else System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.setLastPeriodDate(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.onboarding_dialog_save), color = tokens.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.onboarding_dialog_cancel), color = tokens.muted)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = tokens.panel)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = tokens.text,
                    headlineContentColor = tokens.text,
                    weekdayContentColor = tokens.muted,
                    subheadContentColor = tokens.muted,
                    yearContentColor = tokens.text,
                    currentYearContentColor = tokens.accent,
                    selectedYearContentColor = tokens.bg,
                    selectedYearContainerColor = tokens.accent,
                    dayContentColor = tokens.text,
                    selectedDayContentColor = tokens.bg,
                    selectedDayContainerColor = tokens.accent
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PASO 0: BIENVENIDA
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepWelcome(
    mode: String,
    onSelectMode: (String) -> Unit
) {
    val tokens = LumiThemeTokens.current

    // Showcase hero grande centrado con flor viva
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        LumiBrandLockup(
            flowerSize = 46,
            fontSize = 42.sp,
            starSize = 16.sp,
            letterSpacing = (-2.8).sp,
            animated = true
        )
    }

    EditorialHeading(
        normalText = stringResource(R.string.onboarding_welcome_normal),
        italicText = stringResource(R.string.onboarding_welcome_italic),
        desc = stringResource(R.string.onboarding_welcome_desc)
    )

    Spacer(modifier = Modifier.height(20.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(
            title = stringResource(R.string.onboarding_mode_track_title),
            subtitle = stringResource(R.string.onboarding_mode_track_subtitle),
            icon = Icons.Rounded.CalendarToday,
            selected = mode == "track",
            onClick = { onSelectMode("track") }
        )

        ChoiceCard(
            title = stringResource(R.string.onboarding_mode_support_title),
            subtitle = stringResource(R.string.onboarding_mode_support_subtitle),
            icon = Icons.Rounded.People,
            selected = mode == "support",
            onClick = { onSelectMode("support") }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PASO 1: SEGURIDAD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepSafety(
    mode: String,
    medicalAccepted: Boolean,
    privacyAccepted: Boolean,
    onToggleMedical: () -> Unit,
    onTogglePrivacy: () -> Unit,
    onOpenPrivacyInfo: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    EditorialHeading(
        normalText = stringResource(R.string.onboarding_safety_normal),
        italicText = stringResource(R.string.onboarding_safety_italic),
        desc = stringResource(R.string.onboarding_safety_desc)
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Tarjeta médica institucional
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.panel)
            .border(1.dp, tokens.line, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.onboarding_safety_card_title),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BulletText(stringResource(R.string.onboarding_safety_bullet_1))
                BulletText(stringResource(R.string.onboarding_safety_bullet_2))
                BulletText(stringResource(R.string.onboarding_safety_bullet_3))
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    val bothAccepted = medicalAccepted && privacyAccepted
    val onToggleAll = {
        if (bothAccepted) {
            if (medicalAccepted) onToggleMedical()
            if (privacyAccepted) onTogglePrivacy()
        } else {
            if (!medicalAccepted) onToggleMedical()
            if (!privacyAccepted) onTogglePrivacy()
        }
    }

    // Tarjeta interactiva unificada de consentimiento (minHeight = 56.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.panel)
            .border(1.dp, tokens.line, RoundedCornerShape(16.dp))
            .toggleable(
                value = bothAccepted,
                role = Role.Checkbox,
                onValueChange = { onToggleAll() }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = bothAccepted,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = tokens.accent,
                uncheckedColor = tokens.line,
                checkmarkColor = tokens.bg
            ),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.onboarding_safety_understand_bold),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )
            Text(
                text = if (mode == "support") {
                    stringResource(R.string.onboarding_safety_support_note)
                } else {
                    stringResource(R.string.onboarding_safety_change_later_note)
                },
                fontSize = 11.sp,
                color = tokens.muted,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.onboarding_safety_privacy_bold),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )
            Text(
                text = stringResource(R.string.onboarding_safety_privacy_sub),
                fontSize = 11.sp,
                color = tokens.muted,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.onboarding_safety_consult_policy)
                    ) { onOpenPrivacyInfo() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.onboarding_safety_consult_policy),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.accent
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PASO 2: TU NOMBRE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepName(
    name: String,
    onNameChange: (String) -> Unit
) {
    val tokens = LumiThemeTokens.current

    EditorialHeading(
        normalText = stringResource(R.string.onboarding_name_normal),
        italicText = stringResource(R.string.onboarding_name_italic),
        desc = stringResource(R.string.onboarding_name_desc)
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = stringResource(R.string.onboarding_name_field_label),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = tokens.muted
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = name,
        onValueChange = { if (it.length <= 32) onNameChange(it) },
        placeholder = { Text(stringResource(R.string.onboarding_name_placeholder), color = tokens.muted) },
        singleLine = true,
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = tokens.panel,
            unfocusedContainerColor = tokens.panel,
            focusedBorderColor = tokens.accent,
            unfocusedBorderColor = tokens.line,
            focusedTextColor = tokens.text,
            unfocusedTextColor = tokens.text
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Preview del saludo en Home
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(tokens.panel)
            .border(1.dp, tokens.line, RoundedCornerShape(17.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.onboarding_name_preview_label),
                fontSize = 11.sp,
                color = tokens.muted
            )
            Spacer(modifier = Modifier.height(6.dp))

            val cleanName = name.trim()
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cleanName.isNotBlank()) {
                    val helloText = stringResource(R.string.home_greeting_hello).trimEnd()
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.text
                                )
                            ) {
                                append("$helloText ")
                            }
                            withStyle(
                                SpanStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Normal,
                                    fontStyle = FontStyle.Italic,
                                    fontFamily = FontFamily.Serif,
                                    color = tokens.accent
                                )
                            ) {
                                append("$cleanName.")
                            }
                        }
                    )
                } else {
                    Text(
                        text = stringResource(R.string.onboarding_hello_simple),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = stringResource(R.string.onboarding_can_change_later),
        fontSize = 11.5.sp,
        color = tokens.muted
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  PASO 3: TU CICLO
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepCycle(
    regularityChoice: String?,
    onSelectRegularity: (String) -> Unit,
    cycleKnown: Boolean,
    cycleDays: Int,
    onToggleCycleKnown: () -> Unit,
    onCycleDaysChange: (Int) -> Unit
) {
    val tokens = LumiThemeTokens.current

    EditorialHeading(
        normalText = stringResource(R.string.onboarding_cycle_normal),
        italicText = stringResource(R.string.onboarding_cycle_italic),
        desc = stringResource(R.string.onboarding_cycle_desc)
    )

    Spacer(modifier = Modifier.height(18.dp))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceCard(
            title = stringResource(R.string.onboarding_reg_regular_title),
            subtitle = stringResource(R.string.onboarding_reg_regular_subtitle),
            icon = Icons.Rounded.Check,
            selected = regularityChoice == "yes",
            onClick = { onSelectRegularity("yes") }
        )

        ChoiceCard(
            title = stringResource(R.string.onboarding_reg_irregular_title),
            subtitle = stringResource(R.string.onboarding_reg_irregular_subtitle),
            icon = Icons.Rounded.Waves,
            selected = regularityChoice == "no",
            onClick = { onSelectRegularity("no") }
        )

        ChoiceCard(
            title = stringResource(R.string.onboarding_reg_unsure_title),
            subtitle = stringResource(R.string.onboarding_reg_unsure_subtitle),
            icon = Icons.AutoMirrored.Rounded.HelpOutline,
            selected = regularityChoice == "unknown",
            onClick = { onSelectRegularity("unknown") }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Caja de duración aproximada
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.panel)
            .border(1.dp, tokens.line, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.onboarding_approx_duration),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Text(
                        text = stringResource(R.string.onboarding_approx_duration_desc),
                        fontSize = 11.sp,
                        color = tokens.muted
                    )
                }

                TextButton(
                    onClick = onToggleCycleKnown,
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (cycleKnown) stringResource(R.string.onboarding_dont_know) else stringResource(R.string.onboarding_estimate_duration),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.accent
                    )
                }
            }

            if (cycleKnown) {
                Spacer(modifier = Modifier.height(16.dp))
                TactileStepperWithChips(
                    value = cycleDays,
                    onValueChange = onCycleDaysChange,
                    min = 21,
                    max = 45,
                    quickChips = listOf(26, 28, 30, 32)
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.onboarding_not_know_habitual),
                    fontSize = 12.sp,
                    color = tokens.muted
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PASO 4: ÚLTIMO PERIODO
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepDate(
    dateKnown: Boolean,
    dateEpoch: Long,
    onToggleDateKnown: (Boolean) -> Unit,
    onOpenDatePicker: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    EditorialHeading(
        normalText = stringResource(R.string.onboarding_last_period_normal),
        italicText = stringResource(R.string.onboarding_last_period_italic),
        desc = stringResource(R.string.onboarding_last_period_desc)
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Selector micro-pills
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MicroPill(
            label = stringResource(R.string.onboarding_choose_date),
            selected = dateKnown,
            onClick = { onToggleDateKnown(true) }
        )
        MicroPill(
            label = stringResource(R.string.onboarding_dont_remember_btn),
            selected = !dateKnown,
            onClick = { onToggleDateKnown(false) }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    val tapSelectDateText = stringResource(R.string.onboarding_tap_select_date)
    if (dateKnown) {
        val formattedDate = remember(dateEpoch, tapSelectDateText) {
            if (dateEpoch > 0) {
                val pattern = if (Locale.getDefault().language == "en") "EEEE, MMMM d, yyyy" else "EEEE, d 'de' MMMM yyyy"
                Instant.ofEpochMilli(dateEpoch)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
            } else {
                tapSelectDateText
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.panel)
                .border(1.dp, tokens.line, RoundedCornerShape(18.dp))
                .clickable { onOpenDatePicker() }
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.onboarding_first_day_bleeding),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.muted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (dateEpoch > 0) tokens.text else tokens.muted
                    )
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.onboarding_correct_calendar_note),
                    fontSize = 11.sp,
                    color = tokens.muted
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.panel)
                .border(1.dp, tokens.line, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.onboarding_start_without_date_title),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.onboarding_start_without_date_desc),
                    fontSize = 11.5.sp,
                    color = tokens.muted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PASO 5: PREFERENCIAS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepPreferences(
    mode: String,
    selectedGoal: UserGoal?,
    onSelectGoal: (UserGoal) -> Unit,
    contraceptionChoice: String,
    onSelectContraception: (String) -> Unit
) {
    val tokens = LumiThemeTokens.current

    EditorialHeading(
        normalText = stringResource(R.string.onboarding_goals_normal),
        italicText = stringResource(R.string.onboarding_goals_italic),
        desc = stringResource(R.string.onboarding_goals_desc)
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Cuadrícula 2x2 de objetivos
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GoalCard(
            title = stringResource(R.string.onboarding_goal_understand_cycle),
            icon = Icons.Rounded.CalendarToday,
            selected = selectedGoal == UserGoal.TRACK_CYCLE,
            onClick = { onSelectGoal(UserGoal.TRACK_CYCLE) },
            modifier = Modifier.weight(1f)
        )
        GoalCard(
            title = stringResource(R.string.onboarding_goal_track_wellness),
            icon = Icons.Rounded.Favorite,
            selected = selectedGoal == UserGoal.HEALTH_MONITORING,
            onClick = { onSelectGoal(UserGoal.HEALTH_MONITORING) },
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GoalCard(
            title = stringResource(R.string.onboarding_goal_seek_pregnancy),
            icon = Icons.Rounded.Eco,
            selected = selectedGoal == UserGoal.SEEK_PREGNANCY,
            onClick = { onSelectGoal(UserGoal.SEEK_PREGNANCY) },
            modifier = Modifier.weight(1f)
        )
        GoalCard(
            title = stringResource(R.string.onboarding_goal_avoid_pregnancy),
            icon = Icons.Rounded.Shield,
            selected = selectedGoal == UserGoal.AVOID_PREGNANCY,
            onClick = { onSelectGoal(UserGoal.AVOID_PREGNANCY) },
            modifier = Modifier.weight(1f)
        )
    }

    if (selectedGoal == UserGoal.AVOID_PREGNANCY) {
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(tokens.field)
                .border(BorderStroke(1.dp, tokens.accent.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_avoid_pregnancy_warning),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = tokens.text
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider(color = tokens.line, thickness = 1.dp)
    Spacer(modifier = Modifier.height(18.dp))

    // Pregunta sobre anticoncepción hormonal
    Text(
        text = stringResource(R.string.onboarding_contraceptives_question),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = tokens.text
    )
    Text(
        text = stringResource(R.string.onboarding_contraceptives_question_desc),
        fontSize = 11.5.sp,
        color = tokens.muted
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MicroPill(
            label = stringResource(R.string.onboarding_contraceptives_yes),
            selected = contraceptionChoice == "yes",
            onClick = { onSelectContraception("yes") }
        )
        MicroPill(
            label = stringResource(R.string.onboarding_contraceptives_no),
            selected = contraceptionChoice == "no",
            onClick = { onSelectContraception("no") }
        )
        MicroPill(
            label = stringResource(R.string.onboarding_contraceptives_prefer_not),
            selected = contraceptionChoice == "unsure",
            onClick = { onSelectContraception("unsure") }
        )
    }

    if (mode == "support") {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.onboarding_companion_no_menstrual_note),
            fontSize = 11.sp,
            color = tokens.muted
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PASO 6: PERSONALIZACIÓN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepPersonalize(
    periodKnown: Boolean,
    periodDays: Int,
    onTogglePeriodKnown: () -> Unit,
    onPeriodDaysChange: (Int) -> Unit,
    secondaryGoals: Set<SecondaryGoal>,
    onToggleSecondary: (SecondaryGoal) -> Unit,
    age: Int?,
    height: Float?,
    weight: Float?,
    onUpdatePhysical: (Int?, Float?, Float?) -> Unit
) {
    val tokens = LumiThemeTokens.current
    var isPhysicalExpanded by remember { mutableStateOf(false) }

    EditorialHeading(
        normalText = stringResource(R.string.onboarding_final_details_normal),
        italicText = stringResource(R.string.onboarding_final_details_italic),
        desc = stringResource(R.string.onboarding_final_details_optional_desc)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Caja de duración del sangrado
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.panel)
            .border(1.dp, tokens.line, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.onboarding_period_duration_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Text(
                        text = stringResource(R.string.onboarding_period_duration_desc),
                        fontSize = 11.sp,
                        color = tokens.muted
                    )
                }

                TextButton(
                    onClick = onTogglePeriodKnown,
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (periodKnown) stringResource(R.string.onboarding_dont_know) else stringResource(R.string.onboarding_choose_action),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.accent
                    )
                }
            }

            if (periodKnown) {
                Spacer(modifier = Modifier.height(14.dp))
                TactileStepperWithChips(
                    value = periodDays,
                    onValueChange = onPeriodDaysChange,
                    min = 1,
                    max = 10,
                    quickChips = listOf(3, 4, 5, 6)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_no_average_yet),
                    fontSize = 11.5.sp,
                    color = tokens.muted
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = stringResource(R.string.onboarding_pay_attention_to),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = tokens.text
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Cuadrícula de síntomas / bienestar
    val symptomTiles = listOf(
        Triple(SecondaryGoal.MANAGE_CRAMPS, stringResource(R.string.onboarding_goal_cramps_label), Icons.Rounded.Favorite),
        Triple(SecondaryGoal.IMPROVE_SLEEP, stringResource(R.string.onboarding_goal_sleep_label), Icons.Rounded.Bedtime),
        Triple(SecondaryGoal.UNDERSTAND_MOOD, stringResource(R.string.onboarding_goal_mood_label), Icons.Rounded.AutoAwesome),
        Triple(SecondaryGoal.TRACK_FERTILITY, stringResource(R.string.onboarding_goal_fertility_label), Icons.Rounded.Eco),
        Triple(SecondaryGoal.REDUCE_STRESS, stringResource(R.string.onboarding_goal_stress_label), Icons.Rounded.Waves)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        symptomTiles.take(3).forEach { (goal, label, icon) ->
            SymptomTile(
                label = label,
                icon = icon,
                selected = goal in secondaryGoals,
                onClick = { onToggleSecondary(goal) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        symptomTiles.drop(3).forEach { (goal, label, icon) ->
            SymptomTile(
                label = label,
                icon = icon,
                selected = goal in secondaryGoals,
                onClick = { onToggleSecondary(goal) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Acordeón Perfil Físico (opcional)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.panel)
            .border(1.dp, tokens.line, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPhysicalExpanded = !isPhysicalExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.onboarding_physical_profile_title),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Icon(
                    imageVector = if (isPhysicalExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = tokens.muted
                )
            }

            AnimatedVisibility(visible = isPhysicalExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    var ageStr by remember { mutableStateOf(age?.toString() ?: "") }
                    var heightStr by remember { mutableStateOf(height?.toString() ?: "") }
                    var weightStr by remember { mutableStateOf(weight?.toString() ?: "") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ageStr,
                            onValueChange = {
                                ageStr = it
                                onUpdatePhysical(it.toIntOrNull(), heightStr.toFloatOrNull(), weightStr.toFloatOrNull())
                            },
                            label = { Text(stringResource(R.string.onboarding_field_age_label), fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = heightStr,
                            onValueChange = {
                                heightStr = it
                                onUpdatePhysical(ageStr.toIntOrNull(), it.toFloatOrNull(), weightStr.toFloatOrNull())
                            },
                            label = { Text(stringResource(R.string.onboarding_field_height_label), fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = {
                                weightStr = it
                                onUpdatePhysical(ageStr.toIntOrNull(), heightStr.toFloatOrNull(), it.toFloatOrNull())
                            },
                            label = { Text(stringResource(R.string.onboarding_field_weight_label), fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_physical_calibrate_desc),
                        fontSize = 11.sp,
                        color = tokens.muted
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PASO 7: LISTO (RESUMEN)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepFinish(
    data: OnboardingData,
    onEditStep: (Int) -> Unit,
    finishExit: Modifier,
    bloom: () -> Float
) {
    val tokens = LumiThemeTokens.current
    val cleanName = data.userName?.trim() ?: ""

    // Showcase hero grande centrado con flor viva
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LumiBrandFlower(
                size = 46,
                animated = false,
                modifier = Modifier
                    .drawWithCache {
                        val radius = 260.dp.toPx()
                        val halo = Brush.radialGradient(
                            0f to tokens.glow,
                            0.3f to tokens.accentSoft.copy(alpha = 0.6f),
                            0.7f to tokens.bg.copy(alpha = 0.2f),
                            1f to tokens.bg.copy(alpha = 0f),
                            center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f), radius = radius
                        )
                        onDrawBehind {
                            val p = bloom()
                            if (p > 0f) drawCircle(halo, radius = radius, alpha = p)
                        }
                    }
                    .graphicsLayer {
                        scaleX = 1f + 0.35f * bloom()
                        scaleY = scaleX
                    }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("lumi", fontSize = 42.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2.8).sp, color = tokens.text)
                Spacer(Modifier.width(3.dp))
                Text("✦", fontSize = 16.sp, color = tokens.accent)
            }
        }
    }

    EditorialHeading(
        normalText = stringResource(R.string.onboarding_ready_title_normal),
        italicText = if (cleanName.isNotBlank()) stringResource(R.string.onboarding_ready_title_with_name, cleanName) else ".",
        desc = stringResource(R.string.onboarding_ready_change_choices_desc)
    )

    Spacer(modifier = Modifier.height(18.dp))

    if (data.mode == "support") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(finishExit)
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.panel)
                .border(1.dp, tokens.line, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.onboarding_support_next_step),
                    fontSize = 11.sp,
                    color = tokens.muted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.onboarding_support_connect_conf),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_support_instructions),
                    fontSize = 12.sp,
                    color = tokens.muted,
                    lineHeight = 17.sp
                )
            }
        }
    } else {
        // Cuadrícula 2x2 de resumen editable
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            SummaryBox(
                title = stringResource(R.string.onboarding_summary_greeting),
                value = if (cleanName.isNotBlank()) stringResource(R.string.onboarding_hello_format, cleanName) else stringResource(R.string.onboarding_hello_simple),
                onEdit = { onEditStep(2) },
                modifier = Modifier.weight(1f).then(finishExit)
            )
            SummaryBox(
                title = stringResource(R.string.onboarding_summary_cycle),
                value = if (data.cycleLengthKnown) stringResource(R.string.onboarding_summary_cycle_val, data.cycleLength) else stringResource(R.string.onboarding_summary_cycle_unknown),
                onEdit = { onEditStep(3) },
                modifier = Modifier.weight(1f).then(finishExit)
            )
        }

        Spacer(modifier = Modifier.height(9.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            val dateLabel = if (data.lastPeriodKnown && data.lastPeriodDate > 0) {
                val pattern = if (Locale.getDefault().language == "en") "MMM d" else "d 'de' MMM"
                Instant.ofEpochMilli(data.lastPeriodDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
            } else {
                stringResource(R.string.onboarding_summary_not_logged)
            }

            val goalLabel = when (data.userGoal) {
                UserGoal.TRACK_CYCLE -> stringResource(R.string.onboarding_goal_understand_cycle)
                UserGoal.HEALTH_MONITORING -> stringResource(R.string.onboarding_goal_track_wellness)
                UserGoal.SEEK_PREGNANCY -> stringResource(R.string.onboarding_goal_seek_pregnancy)
                UserGoal.AVOID_PREGNANCY -> stringResource(R.string.onboarding_goal_avoid_pregnancy)
                null -> stringResource(R.string.onboarding_summary_not_selected)
            }

            SummaryBox(
                title = stringResource(R.string.onboarding_summary_last_period),
                value = dateLabel,
                onEdit = { onEditStep(4) },
                modifier = Modifier.weight(1f).then(finishExit)
            )
            SummaryBox(
                title = stringResource(R.string.onboarding_summary_goal),
                value = goalLabel,
                onEdit = { onEditStep(5) },
                modifier = Modifier.weight(1f).then(finishExit)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  COMPONENTES REUTILIZABLES DE ONBOARDING
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TactileStepperWithChips(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    quickChips: List<Int>,
    unitLabel: String = stringResource(R.string.onboarding_days_unit)
) {
    val tokens = LumiThemeTokens.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón decrementar (48x48 dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tokens.panel)
                    .border(1.dp, tokens.line, CircleShape)
                    .clickable(enabled = value > min) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onValueChange((value - 1).coerceAtLeast(min))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (value > min) tokens.text else tokens.muted.copy(alpha = 0.35f)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value.toString(),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.accent,
                    letterSpacing = (-2).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = unitLabel,
                    fontSize = 14.sp,
                    color = tokens.muted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Botón incrementar (48x48 dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tokens.panel)
                    .border(1.dp, tokens.line, CircleShape)
                    .clickable(enabled = value < max) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onValueChange((value + 1).coerceAtMost(max))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (value < max) tokens.text else tokens.muted.copy(alpha = 0.35f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Chips rápidos de selección directa
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            quickChips.forEach { chipValue ->
                val isSelected = value == chipValue
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) tokens.accentSoft else tokens.panel)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) tokens.accent else tokens.line,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onValueChange(chipValue)
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${chipValue}d",
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) tokens.accent else tokens.text
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorialHeading(
    normalText: String,
    italicText: String,
    desc: String
) {
    val tokens = LumiThemeTokens.current

    Column {
        val annotatedHeading = remember(normalText, italicText, tokens.text, tokens.accent) {
            buildAnnotatedString {
                val cleanNormal = normalText.trimEnd()
                val cleanItalic = italicText.trimStart()
                if (cleanNormal.isNotEmpty()) {
                    withStyle(
                        SpanStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1.2).sp,
                            color = tokens.text
                        )
                    ) {
                        append(cleanNormal)
                    }
                }
                if (cleanNormal.isNotEmpty() && cleanItalic.isNotEmpty()) {
                    append(" ")
                }
                if (cleanItalic.isNotEmpty()) {
                    withStyle(
                        SpanStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Italic,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = (-1.2).sp,
                            color = tokens.accent
                        )
                    ) {
                        append(cleanItalic)
                    }
                }
            }
        }

        Text(text = annotatedHeading)

        if (desc.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = desc,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = tokens.muted
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(19.dp))
            .background(if (selected) tokens.accentSoft else tokens.panel)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) tokens.accent else tokens.line,
                shape = RoundedCornerShape(19.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (selected) tokens.field else tokens.panel2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = tokens.muted
                )
            }
        }

        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) tokens.accent else Color.Transparent)
                .border(1.dp, if (selected) tokens.accent else tokens.line, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Text(
                    text = "✓",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.bg
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiThemeTokens.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) tokens.accentSoft else tokens.panel)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) tokens.accent else tokens.line,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tokens.panel2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = tokens.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MicroPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) tokens.accentSoft else tokens.panel)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) tokens.accent else tokens.line,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) tokens.text else tokens.muted
        )
    }
}

@Composable
private fun SymptomTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiThemeTokens.current

    Column(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) tokens.accentSoft else tokens.panel)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) tokens.accent else tokens.line,
                shape = RoundedCornerShape(15.dp)
            )
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = tokens.text,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SummaryBox(
    title: String,
    value: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiThemeTokens.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.panel)
            .border(1.dp, tokens.line, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = tokens.muted
                )
                Text(
                    text = stringResource(R.string.btn_edit),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.accent,
                    modifier = Modifier.clickable { onEdit() }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BulletText(text: String) {
    val tokens = LumiThemeTokens.current
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("•", fontSize = 12.sp, color = tokens.muted)
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = tokens.muted
        )
    }
}

@Composable
private fun ConsentCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    boldText: String,
    subText: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val tokens = LumiThemeTokens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.panel)
            .border(1.dp, tokens.line, RoundedCornerShape(14.dp))
            .clickable { onCheckedChange() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = CheckboxDefaults.colors(
                checkedColor = tokens.accent,
                uncheckedColor = tokens.line,
                checkmarkColor = tokens.bg
            ),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = boldText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            }
            Text(
                text = subText,
                fontSize = 11.sp,
                color = tokens.muted,
                lineHeight = 16.sp
            )
            if (actionText != null && onActionClick != null) {
                Text(
                    text = actionText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.accent,
                    modifier = Modifier.clickable { onActionClick() }
                )
            }
        }
    }
}
