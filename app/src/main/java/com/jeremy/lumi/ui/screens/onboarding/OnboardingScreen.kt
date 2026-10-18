package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.jeremy.lumi.ui.screens.settings.SettingsViewModel
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.LumiThemeTokens

private val STEP_NAMES = mapOf(
    0 to "BIENVENIDA",
    1 to "SEGURIDAD",
    2 to "TU NOMBRE",
    3 to "TU CICLO",
    4 to "ÚLTIMO PERIODO",
    5 to "PREFERENCIAS",
    6 to "PERSONALIZACIÓN",
    7 to "LISTO"
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
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.currentTheme.collectAsStateWithLifecycle()
    var onboardingTheme by remember(currentTheme) {
        mutableStateOf(LumiOnboardingTheme.fromAppTheme(currentTheme))
    }
    val tokens = getThemeTokensV7(onboardingTheme)
    val scrollState = rememberScrollState()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showAtmosphericTransition by remember { mutableStateOf(false) }

    val stepNumber = uiState.currentStepNumber
    val isFirstStep = uiState.currentStepIndex == 0
    val isFinalStep = stepNumber == 7
    val showHeaderBrand = stepNumber != 0 && stepNumber != 7

    // Resetear scroll al cambiar de paso
    LaunchedEffect(stepNumber) {
        scrollState.scrollTo(0)
    }

    CompositionLocalProvider(
        LocalLumiOnboardingTokens provides tokens
    ) {
        if (showAtmosphericTransition) {
            LumiAtmosphericTransition(
                onFinished = onComplete
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tokens.bg)
            ) {
            // Cielo estrellado en 3 capas ascendentes en paralaje (solo temas oscuros)
            LumiSkyBackground(stepNumber = stepNumber)

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
                            text = "◐ Tema: ${onboardingTheme.displayName}",
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
                    val stepTitle = STEP_NAMES[stepNumber] ?: ""

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
                    val forward = targetState > initialState
                    if (forward) {
                        (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally(tween(250)) { -it } + fadeOut(tween(250)))
                    } else {
                        (slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally(tween(250)) { it } + fadeOut(tween(250)))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "step_content_anim"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    when (step) {
                        0 -> Step0Welcome(
                            mode = uiState.data.mode,
                            onSelectMode = { viewModel.setMode(it) }
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
                            saveError = uiState.saveError
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
                    uiState.isCompleting -> "Guardando…"
                    isFirstStep -> "Empezar →"
                    isFinalStep -> if (uiState.saveError != null) "Reintentar" else "Ver mi inicio"
                    stepNumber == 6 -> "Terminar →"
                    else -> "Continuar →"
                }

                // Botón principal ceremonial con halo orbital y cinta drift
                LumiPrimaryButton(
                    text = primaryLabel,
                    onClick = {
                        if (isFinalStep) {
                            viewModel.completeOnboarding(onDone = {
                                showAtmosphericTransition = true
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
                            text = "← Volver",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = tokens.muted,
                            modifier = Modifier.clickable { viewModel.prevStep() }
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    val footerNote = when {
                        isFinalStep -> "Tus datos se guardan de forma local"
                        stepNumber == 2 || stepNumber == 6 -> "Puedes omitir este paso"
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
                        text = "Política de privacidad",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = tokens.text
                    )
                },
                text = {
                    Text(
                        text = "Tus datos se procesan localmente en tu dispositivo para entregarte predicciones y estimaciones. Lumi nunca vende tus datos de salud ni los comparte sin tu autorización expresa.",
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
                            text = "Cerrar",
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
                onSelectTheme = { theme ->
                    onboardingTheme = theme
                    settingsViewModel.changeTheme(theme.appThemePalette)
                    showThemeSheet = false
                },
                onDismiss = { showThemeSheet = false }
            )
        }
    }
}
}
}
