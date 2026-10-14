package com.jeremy.lumi.ui.screens.onboarding

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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val STEP_TITLES = listOf(
    "BIENVENIDA",
    "SEGURIDAD",
    "TU NOMBRE",
    "TU CICLO",
    "ÚLTIMO PERIODO",
    "PREFERENCIAS",
    "PERSONALIZACIÓN",
    "LISTO"
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
                        .clip(RoundedCornerShape(22.dp))
                        .background(tokens.panel)
                        .border(1.dp, tokens.line, RoundedCornerShape(22.dp))
                        .clickable {
                            val nextTheme = if (isBlush) AppThemePalette.NOIR else AppThemePalette.PETALO
                            settingsViewModel.changeTheme(nextTheme)
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (isBlush) "◑ Tema: Blush" else "◐ Tema: Noir",
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
                    val title = STEP_TITLES.getOrElse(stepNumber) { "" }

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
                            onEditStep = { viewModel.jumpToStep(it) }
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
                    isFirstStep -> "Empezar →"
                    isFinalStep -> "Ver mi inicio"
                    stepNumber == 6 -> "Terminar →"
                    else -> "Continuar →"
                }

                Button(
                    onClick = {
                        if (isFinalStep) {
                            viewModel.completeOnboarding(onComplete)
                        } else {
                            viewModel.nextStep()
                        }
                    },
                    enabled = uiState.isNextEnabled && !uiState.isCompleting,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
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
                        .height(36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isFirstStep) {
                        TextButton(
                            onClick = { viewModel.prevStep() },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "← Volver",
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
                                text = "Omitir",
                                fontSize = 12.sp,
                                color = tokens.muted
                            )
                        }
                    } else if (isFinalStep) {
                        Text(
                            text = "Tus datos se guardan de forma local",
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
                    text = "Política de privacidad",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "En Lumi, tu intimidad y privacidad son absolutas:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = tokens.text
                    )
                    Text(
                        text = "• Todos tus registros menstruales, notas y parámetros fisiológicos se almacenan de manera local y cifrada en tu dispositivo.",
                        fontSize = 12.sp,
                        color = tokens.muted
                    )
                    Text(
                        text = "• No vendemos tus datos a anunciantes ni terceros.",
                        fontSize = 12.sp,
                        color = tokens.muted
                    )
                    Text(
                        text = "• En cualquier momento puedes exportar tus datos en PDF/ZIP o eliminarlos de manera irreversible.",
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
                    Text("Entendido", color = tokens.bg, fontWeight = FontWeight.Bold)
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
                    Text("Guardar", color = tokens.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = tokens.muted)
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
        normalText = "Tu ciclo, ",
        italicText = "a tu manera.",
        desc = "Elige cómo quieres usar Lumi."
    )

    Spacer(modifier = Modifier.height(20.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(
            title = "Seguir mi ciclo",
            subtitle = "Registros y estimaciones personales.",
            icon = Icons.Rounded.CalendarToday,
            selected = mode == "track",
            onClick = { onSelectMode("track") }
        )

        ChoiceCard(
            title = "Acompañar a alguien",
            subtitle = "Conectar con una persona de confianza.",
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
        normalText = "Tu bienestar, ",
        italicText = "con claridad.",
        desc = "Antes de comenzar."
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
                    text = "Lumi acompaña, no diagnostica",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BulletText("Las predicciones son estimaciones, no certezas.")
                BulletText("No utilices Lumi como anticonceptivo.")
                BulletText("Si algo te preocupa o tienes síntomas intensos, busca atención profesional.")
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Checkbox 1: Limitaciones médicas
    ConsentCheckbox(
        checked = medicalAccepted,
        onCheckedChange = { onToggleMedical() },
        boldText = "Entiendo estas limitaciones.",
        subText = if (mode == "support") {
            " Como acompañante, solo veré información que me autoricen a compartir."
        } else {
            " Puedo cambiar mis preferencias más adelante."
        }
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Checkbox 2: Privacidad y consentimiento
    ConsentCheckbox(
        checked = privacyAccepted,
        onCheckedChange = { onTogglePrivacy() },
        boldText = "Acepto la política de privacidad",
        subText = " y autorizo el tratamiento de mis datos para prestar el servicio. ",
        actionText = "Consultar política",
        onActionClick = onOpenPrivacyInfo
    )
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
        normalText = "¿Cómo te gustaría ",
        italicText = "que te saludemos?",
        desc = "Puedes usar un apodo o dejarlo vacío."
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Nombre o apodo · opcional",
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = tokens.muted
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = name,
        onValueChange = { if (it.length <= 32) onNameChange(it) },
        placeholder = { Text("Tu apodo", color = tokens.muted) },
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
                text = "Así aparecerá en Inicio",
                fontSize = 11.sp,
                color = tokens.muted
            )
            Spacer(modifier = Modifier.height(6.dp))

            val cleanName = name.trim()
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cleanName.isNotBlank()) {
                    Text(
                        text = "Hola, ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Text(
                        text = "$cleanName.",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        color = tokens.accent
                    )
                } else {
                    Text(
                        text = "Hola.",
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
        text = "Puedes cambiarlo más adelante.",
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
        normalText = "¿Cómo suelen ser ",
        italicText = "tus ciclos?",
        desc = "No necesitas saberlo con exactitud."
    )

    Spacer(modifier = Modifier.height(18.dp))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceCard(
            title = "Bastante regulares",
            subtitle = "Suelen llegar con intervalos parecidos.",
            icon = Icons.Rounded.Check,
            selected = regularityChoice == "yes",
            onClick = { onSelectRegularity("yes") }
        )

        ChoiceCard(
            title = "Varían bastante",
            subtitle = "No siempre llegan cuando los espero.",
            icon = Icons.Rounded.Waves,
            selected = regularityChoice == "no",
            onClick = { onSelectRegularity("no") }
        )

        ChoiceCard(
            title = "No estoy segura",
            subtitle = "Prefiero empezar sin suponer.",
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
                        text = "Duración aproximada",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Text(
                        text = "De un inicio de periodo al siguiente.",
                        fontSize = 11.sp,
                        color = tokens.muted
                    )
                }

                TextButton(
                    onClick = onToggleCycleKnown,
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (cycleKnown) "No lo sé" else "Estimar duración",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.accent
                    )
                }
            }

            if (cycleKnown) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = cycleDays.toString(),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.accent,
                            letterSpacing = (-2).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "días",
                            fontSize = 14.sp,
                            color = tokens.muted,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Slider(
                        value = cycleDays.toFloat(),
                        onValueChange = { onCycleDaysChange(it.toInt()) },
                        valueRange = 21f..45f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = tokens.accent,
                            activeTrackColor = tokens.accent,
                            inactiveTrackColor = tokens.line
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("21 días", fontSize = 11.sp, color = tokens.muted)
                        Text("45 días", fontSize = 11.sp, color = tokens.muted)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Todavía no conozco mi duración habitual.",
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
        normalText = "¿Cuándo empezó ",
        italicText = "tu último periodo?",
        desc = "La fecha ayuda a iniciar tus estimaciones."
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Selector micro-pills
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MicroPill(
            label = "Elegir fecha",
            selected = dateKnown,
            onClick = { onToggleDateKnown(true) }
        )
        MicroPill(
            label = "No lo recuerdo",
            selected = !dateKnown,
            onClick = { onToggleDateKnown(false) }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (dateKnown) {
        val formattedDate = remember(dateEpoch) {
            if (dateEpoch > 0) {
                Instant.ofEpochMilli(dateEpoch)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", Locale("es")))
            } else {
                "Toca para seleccionar fecha"
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
                    text = "Primer día de sangrado",
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
                    text = "Puedes corregirlo desde Calendario en cualquier momento.",
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
                    text = "Puedes empezar sin una fecha.",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lumi esperará a tu primer registro para iniciar un ciclo. No mostrará un periodo ficticio.",
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
        normalText = "¿Qué te gustaría ",
        italicText = "hacer con Lumi?",
        desc = "Para organizar tu experiencia inicial."
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Cuadrícula 2x2 de objetivos
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GoalCard(
            title = "Entender mi ciclo",
            icon = Icons.Rounded.CalendarToday,
            selected = selectedGoal == UserGoal.TRACK_CYCLE,
            onClick = { onSelectGoal(UserGoal.TRACK_CYCLE) },
            modifier = Modifier.weight(1f)
        )
        GoalCard(
            title = "Conocer mi bienestar",
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
            title = "Buscar embarazo",
            icon = Icons.Rounded.Eco,
            selected = selectedGoal == UserGoal.SEEK_PREGNANCY,
            onClick = { onSelectGoal(UserGoal.SEEK_PREGNANCY) },
            modifier = Modifier.weight(1f)
        )
        GoalCard(
            title = "Evitar embarazo",
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
                text = "Importante: las predicciones del ciclo no sirven para evitar un embarazo. Lumi no ofrece protección anticonceptiva.",
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
        text = "¿Usas anticoncepción hormonal?",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = tokens.text
    )
    Text(
        text = "Puede afectar el sangrado y las predicciones.",
        fontSize = 11.5.sp,
        color = tokens.muted
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MicroPill(
            label = "Sí",
            selected = contraceptionChoice == "yes",
            onClick = { onSelectContraception("yes") }
        )
        MicroPill(
            label = "No",
            selected = contraceptionChoice == "no",
            onClick = { onSelectContraception("no") }
        )
        MicroPill(
            label = "Prefiero no decir",
            selected = contraceptionChoice == "unsure",
            onClick = { onSelectContraception("unsure") }
        )
    }

    if (mode == "support") {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "La persona acompañante no necesita proporcionar información menstrual.",
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
        normalText = "Últimos ",
        italicText = "detalles.",
        desc = "Todo esto es opcional y podrás cambiarlo."
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
                        text = "Duración del sangrado",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Text(
                        text = "Si conoces tu promedio.",
                        fontSize = 11.sp,
                        color = tokens.muted
                    )
                }

                TextButton(
                    onClick = onTogglePeriodKnown,
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (periodKnown) "No lo sé" else "Elegir",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.accent
                    )
                }
            }

            if (periodKnown) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = periodDays.toString(),
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.accent,
                            letterSpacing = (-2).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "días",
                            fontSize = 14.sp,
                            color = tokens.muted,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Slider(
                        value = periodDays.toFloat(),
                        onValueChange = { onPeriodDaysChange(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = tokens.accent,
                            activeTrackColor = tokens.accent,
                            inactiveTrackColor = tokens.line
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1 día", fontSize = 11.sp, color = tokens.muted)
                        Text("10 días", fontSize = 11.sp, color = tokens.muted)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sin promedio por ahora.",
                    fontSize = 11.5.sp,
                    color = tokens.muted
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Quiero prestar atención a...",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = tokens.text
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Cuadrícula de síntomas / bienestar
    val symptomTiles = listOf(
        Triple(SecondaryGoal.MANAGE_CRAMPS, "Cólicos", Icons.Rounded.Favorite),
        Triple(SecondaryGoal.IMPROVE_SLEEP, "Sueño", Icons.Rounded.Bedtime),
        Triple(SecondaryGoal.UNDERSTAND_MOOD, "Estado de ánimo", Icons.Rounded.AutoAwesome),
        Triple(SecondaryGoal.TRACK_FERTILITY, "Fertilidad", Icons.Rounded.Eco),
        Triple(SecondaryGoal.REDUCE_STRESS, "Estrés", Icons.Rounded.Waves)
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
                    text = "Perfil físico (opcional)",
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
                            label = { Text("Edad", fontSize = 10.sp) },
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
                            label = { Text("Altura (cm)", fontSize = 10.sp) },
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
                            label = { Text("Peso (kg)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Solo si quieres añadirlos para calibrar predicciones.",
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
    onEditStep: (Int) -> Unit
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
        LumiBrandLockup(
            flowerSize = 46,
            fontSize = 42.sp,
            starSize = 16.sp,
            letterSpacing = (-2.8).sp,
            animated = true
        )
    }

    EditorialHeading(
        normalText = "Todo listo",
        italicText = if (cleanName.isNotBlank()) ", $cleanName." else ".",
        desc = "Puedes cambiar tus elecciones cuando quieras."
    )

    Spacer(modifier = Modifier.height(18.dp))

    if (data.mode == "support") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.panel)
                .border(1.dp, tokens.line, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = "Tu próximo paso",
                    fontSize = 11.sp,
                    color = tokens.muted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Conectar con confianza.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pide un código de Bonds. No verás registros hasta que la otra persona autorice qué compartir.",
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
                title = "Tu saludo",
                value = if (cleanName.isNotBlank()) "Hola, $cleanName." else "Hola.",
                onEdit = { onEditStep(2) },
                modifier = Modifier.weight(1f)
            )
            SummaryBox(
                title = "Tu ciclo",
                value = if (data.cycleLengthKnown) "${data.cycleLength} días · aprox." else "Por conocer",
                onEdit = { onEditStep(3) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(9.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            val dateLabel = if (data.lastPeriodKnown && data.lastPeriodDate > 0) {
                Instant.ofEpochMilli(data.lastPeriodDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern("d 'de' MMM", Locale("es")))
            } else {
                "Sin registrar"
            }

            val goalLabel = when (data.userGoal) {
                UserGoal.TRACK_CYCLE -> "Entender mi ciclo"
                UserGoal.HEALTH_MONITORING -> "Conocer mi bienestar"
                UserGoal.SEEK_PREGNANCY -> "Buscar embarazo"
                UserGoal.AVOID_PREGNANCY -> "Evitar embarazo"
                null -> "Sin seleccionar"
            }

            SummaryBox(
                title = "Último periodo",
                value = dateLabel,
                onEdit = { onEditStep(4) },
                modifier = Modifier.weight(1f)
            )
            SummaryBox(
                title = "Tu objetivo",
                value = goalLabel,
                onEdit = { onEditStep(5) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  COMPONENTES REUTILIZABLES DE ONBOARDING
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditorialHeading(
    normalText: String,
    italicText: String,
    desc: String
) {
    val tokens = LumiThemeTokens.current

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = normalText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.2).sp,
                color = tokens.text
            )
            Text(
                text = italicText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif,
                letterSpacing = (-1.2).sp,
                color = tokens.accent
            )
        }

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
                    text = "Editar",
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
