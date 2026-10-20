package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.components.LumiBrandLockup
import com.jeremy.lumi.ui.components.LumiThemeSheet
import com.jeremy.lumi.ui.components.LumiThemeSnapshotReveal
import com.jeremy.lumi.ui.components.captureThemeRevealSnapshot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.ImageBitmap
import com.jeremy.lumi.ui.screens.settings.SettingsViewModel
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.LumiThemeTokens

private val STEP_NAMES = mapOf(
    0 to R.string.onboarding_step_welcome,
    1 to R.string.onboarding_step_security,
    2 to R.string.onboarding_step_name,
    3 to R.string.onboarding_step_cycle,
    4 to R.string.onboarding_step_last_period,
    5 to R.string.onboarding_step_preferences,
    6 to R.string.onboarding_step_customization,
    7 to R.string.onboarding_step_ready
)

/**
 * Onboarding v7 nativo de Lumi en Jetpack Compose.
 *
 * Reproduce fielmente la arquitectura visual y funcional del prototipo HTML v7:
 * - Flujo de 8 pantallas en modo seguimiento ("track")
 * - Flujo reducido de 4 pantallas en modo acompañante ("support")
 * - Transición de pasos suave
 * - Persistencia previa al éxito y manejo de errores con reintento
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onComplete: () -> Unit,
    // Para un handoff perfecto, el padre debe componer aquí el Home REAL sin
    // duplicar el logo mientras vuela. Es opcional para compatibilidad.
    homeRevealContent: (@Composable () -> Unit)? = null,
    targetBrandBounds: Rect? = null,
    // Permite a AppNavGraph conservar el velo fuera de esta ruta.
    onCeremonyStart: ((Rect?, LumiThemeTokensV7) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.currentTheme.collectAsStateWithLifecycle()
    var onboardingTheme by remember(currentTheme) {
        mutableStateOf(LumiOnboardingTheme.fromAppTheme(currentTheme))
    }
    val tokens = getThemeTokensV7(onboardingTheme)
    val localDensity = LocalDensity.current
    val enterPx = with(localDensity) { 20.dp.roundToPx() }
    val lowerPx = with(localDensity) { 7.dp.roundToPx() }
    val exitUpPx = with(localDensity) { 4.dp.roundToPx() }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    val sceneView = LocalView.current
    var themeOldFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var themeRevealCenter by remember { mutableStateOf<Offset?>(null) }

    var showAtmosphericTransition by remember { mutableStateOf(false) }
    val outgoingContentAlpha by animateFloatAsState(
        targetValue = if (showAtmosphericTransition) 0f else 1f,
        animationSpec = tween(320),
        label = "onboarding_ceremony_outgoing_content"
    )
    var hideOnboardingAtReveal by remember { mutableStateOf(false) }
    var sourceBrandBounds by remember { mutableStateOf<Rect?>(null) }

    val stepNumber = uiState.currentStepNumber
    val isFirstStep = uiState.currentStepIndex == 0
    val isFinalStep = stepNumber == 7
    val showHeaderBrand = stepNumber != 0 && stepNumber != 7

    val selectThemeWithReveal: (LumiOnboardingTheme, Offset?) -> Unit = { next, origin ->
        if (next != onboardingTheme) {
            // Congelar la escena previa ANTES de cambiar colores (sin guardar imagen).
            themeOldFrame = captureThemeRevealSnapshot(sceneView)
            themeRevealCenter = origin
            onboardingTheme = next
            settingsViewModel.changeTheme(next.appThemePalette)
        }
        showThemeSheet = false
    }


    CompositionLocalProvider(
        LocalLumiOnboardingTokens provides tokens
    ) {
        // No desmontar la última página cuando comienza el vuelo. En el HTML
        // ambos elementos comparten pantalla durante la primera parte.
        Box(Modifier.fillMaxSize().background(tokens.bg)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (hideOnboardingAtReveal) 0f else outgoingContentAlpha }
            ) {
            // Cielo estrellado en 3 capas ascendentes en paralaje (solo temas oscuros)
            LumiSkyBackground(stepNumber = stepNumber)

            // El HTML v6/v7 eliminó deliberadamente la neblina superior.
            // No añadir aquí otra capa radial que altere el Noir de referencia.

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // ── TOP BAR: Header Brand + Theme Chip ───────────────────────────
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
                        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { -it / 2 },
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

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(21.dp))
                            .background(tokens.panel)
                            .border(1.dp, tokens.line, RoundedCornerShape(21.dp))
                            .clickable { showThemeSheet = true }
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_theme_indicator_format, onboardingTheme.displayName),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = tokens.muted
                        )
                    }
                }

            // ── STEPBAR: Indicador y barra de progreso ───────────────────────
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
                    val stepTitle = STEP_NAMES[stepNumber]?.let { stringResource(it) } ?: ""

                    Text(
                        text = "${stepIndex.toString().padStart(2, '0')} / ${totalSteps.toString().padStart(2, '0')} · $stepTitle",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.15.sp,
                        color = tokens.muted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val animatedProgress by animateFloatAsState(
                        targetValue = stepIndex.toFloat() / totalSteps.toFloat(),
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        label = "progress_bar_fill"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(tokens.line)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(40.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(tokens.rose, tokens.accent)
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
                    // HTML v11: entrada 20px/7px + fade (460ms con delay 70),
                    // salida -20px/-4px + fade (290ms); no pantalla entera.
                    val forward = targetState > initialState
                    val x = if (forward) enterPx else -enterPx
                    val enter = slideIn(
                        animationSpec = tween(460, delayMillis = 70, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)),
                        initialOffset = { IntOffset(x, lowerPx) }
                    ) + fadeIn(tween(460, delayMillis = 70, easing = CubicBezierEasing(.16f, 1f, .3f, 1f)))
                    val exit = slideOut(
                        animationSpec = tween(290, easing = CubicBezierEasing(.35f, 0f, .45f, 1f)),
                        targetOffset = { IntOffset(-x, -exitUpPx) }
                    ) + fadeOut(tween(290, easing = CubicBezierEasing(.35f, 0f, .45f, 1f)))
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "step_content_anim"
            ) { step ->
                // Cada página posee su propio estado de scroll. El ScrollState
                // único anterior era compartido por dos páginas al animarse.
                val pageScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(pageScrollState)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    when (step) {
                        0 -> Step0Welcome(
                            // La versión pública no puede ofrecer el modo observador sin Bonds.
                            mode = if (com.jeremy.lumi.BuildConfig.ENABLE_BONDS) uiState.data.mode else "track",
                            onSelectMode = { viewModel.setMode(it) },
                            showSupportMode = com.jeremy.lumi.BuildConfig.ENABLE_BONDS
                        )
                        1 -> Step1Safety(
                            accepted = uiState.data.medicalDisclaimerAccepted && uiState.data.privacyAccepted,
                            onToggleConsent = {
                                viewModel.setMedicalAccepted(it)
                                viewModel.setPrivacyAccepted(it)
                            },
                            onOpenPrivacy = { showPrivacyDialog = true }
                        )
                        2 -> Step2Nickname(
                            name = uiState.data.userName ?: "",
                            onNameChange = { viewModel.setUserName(it) }
                        )
                        3 -> Step3Cycle(
                            regularityChoice = uiState.data.regularityChoice,
                            onSelectRegularity = { viewModel.setRegularity(it) },
                            cycleKnown = uiState.data.cycleLengthKnown,
                            cycleLength = uiState.data.cycleLength,
                            onToggleCycleKnown = { viewModel.setCycleLengthKnown(!uiState.data.cycleLengthKnown) },
                            onAdjustCycleLength = { viewModel.setCycleLength(it) }
                        )
                        4 -> Step4LastPeriod(
                            dateKnown = uiState.data.lastPeriodKnown,
                            lastPeriodDate = uiState.data.lastPeriodDate,
                            onSelectDateKnown = { viewModel.setLastPeriodKnown(it) },
                            onOpenDatePicker = { showDatePicker = true }
                        )
                        5 -> Step5Preferences(
                            currentGoal = uiState.data.userGoal,
                            onSelectGoal = { viewModel.setUserGoal(it) },
                            contraceptionChoice = uiState.data.contraceptionChoice,
                            onSelectContraception = { viewModel.setContraceptionChoice(it) }
                        )
                        6 -> Step6Customization(
                            periodKnown = uiState.data.periodLengthKnown,
                            periodLength = uiState.data.periodLength,
                            onTogglePeriodKnown = { viewModel.setPeriodLengthKnown(!uiState.data.periodLengthKnown) },
                            onAdjustPeriodLength = { viewModel.setPeriodLength(it) },
                            secondaryGoals = uiState.data.secondaryGoals,
                            onToggleSecondaryGoal = { viewModel.toggleSecondaryGoal(it) },
                            age = uiState.data.age,
                            height = uiState.data.height,
                            weight = uiState.data.weight,
                            onPhysicalProfileChange = { a, h, w -> viewModel.setPhysicalProfile(a, h, w) }
                        )
                        7 -> Step7Summary(
                            data = uiState.data,
                            onJumpToStep = { viewModel.jumpToStep(it) },
                            saveError = uiState.saveError,
                            onBrandBounds = { sourceBrandBounds = it },
                            brandVisible = !showAtmosphericTransition
                        )
                    }
                }
            }

            // ── FOOTER: Botón primario y navegación secundaria ────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, tokens.bg.copy(alpha = 0.85f), tokens.bg)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                val primaryLabel = when {
                    uiState.isCompleting -> stringResource(R.string.onboarding_saving)
                    isFirstStep -> stringResource(R.string.onboarding_btn_start)
                    isFinalStep -> if (uiState.saveError != null) stringResource(R.string.btn_retry) else stringResource(R.string.onboarding_btn_see_home)
                    stepNumber == 6 -> stringResource(R.string.onboarding_btn_finish)
                    else -> stringResource(R.string.onboarding_btn_continue)
                }

                // Botón principal ceremonial con halo orbital y cinta drift
                LumiPrimaryButton(
                    text = primaryLabel,
                    onClick = {
                        if (isFinalStep) {
                            viewModel.completeOnboarding(onDone = {
                                showAtmosphericTransition = true
                                onCeremonyStart?.invoke(sourceBrandBounds, tokens)
                            })
                        } else {
                            viewModel.nextStep()
                        }
                    },
                    enabled = uiState.isNextEnabled
                )

                // Navegación secundaria (Volver y nota al pie)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 39.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isFirstStep) {
                        Text(
                            text = stringResource(R.string.onboarding_btn_back),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = tokens.muted,
                            modifier = Modifier.clickable { viewModel.prevStep() }
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    val footerNote = when {
                        isFinalStep -> stringResource(R.string.onboarding_local_storage_note)
                        stepNumber == 2 || stepNumber == 6 -> stringResource(R.string.onboarding_footer_skip_step)
                        else -> ""
                    }

                    if (footerNote.isNotEmpty()) {
                        Text(
                            text = footerNote,
                            fontSize = 10.5.sp,
                            color = tokens.muted
                        )
                    }
                }

                if (uiState.saveError != null) {
                    Text(
                        text = uiState.saveError ?: "",
                        fontSize = 11.sp,
                        color = Color(0xFFED9AAC),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }

        // ── DIÁLOGO DE POLÍTICA DE PRIVACIDAD ─────────────────────────────────
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.onboarding_privacy_dialog_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = tokens.text
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.onboarding_privacy_dialog_desc),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = tokens.muted
                    )
                },
                confirmButton = {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(tokens.accent)
                            .clickable { showPrivacyDialog = false }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_close),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = tokens.bg
                        )
                    }
                },
                containerColor = tokens.panel,
                tonalElevation = 0.dp
            )
        }

        // ── DIÁLOGO DE FECHA (ÚLTIMO PERIODO) ─────────────────────────────────
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = if (uiState.data.lastPeriodDate > 0L) uiState.data.lastPeriodDate else System.currentTimeMillis()
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
                        Text("Aceptar", color = tokens.accent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancelar", color = tokens.muted)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // ── THEME SHEET MODAL ────────────────────────────────────────────────
        if (showThemeSheet) {
            LumiOnboardingThemeSheet(
                selectedTheme = onboardingTheme,
                onSelectTheme = { theme -> selectThemeWithReveal(theme, null) },
                onDismiss = { showThemeSheet = false },
                onSelectThemeAt = { theme, center -> selectThemeWithReveal(theme, center) }
            )
        }
            } // contenido anterior, conservado debajo del velo

            // Snapshot exterior del círculo: pantalla antigua congelada sobre UI nueva.
            themeOldFrame?.let { old ->
                LumiThemeSnapshotReveal(
                    oldFrame = old,
                    sourceCenter = themeRevealCenter,
                    onFinished = { themeOldFrame = null }
                )
            }

            if (showAtmosphericTransition && onCeremonyStart == null) {
                LumiAtmosphericTransition(
                    sourceBrandBounds = sourceBrandBounds,
                    targetBrandBounds = targetBrandBounds,
                    homeRevealContent = homeRevealContent,
                    onRevealStart = { hideOnboardingAtReveal = true },
                    onFinished = onComplete
                )
            }
        } // escenario con capas del onboarding
    } // tokens
} // OnboardingScreen
