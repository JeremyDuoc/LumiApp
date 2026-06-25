package com.jeremy.lumi.ui.screens.partner

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ObserverScreen(
    link: PartnerLink,
    currentUid: String,
    uiState: PartnerUiState,
    onSendCareAction: (com.jeremy.lumi.domain.model.CareAction) -> Unit,
    onUnlink: () -> Unit,
    onBack: () -> Unit
) {
    val snapshot = link.ownerSnapshot
    val currentPhase = snapshot?.currentPhase ?: CyclePhase.UNKNOWN
    val daysUntilNext = snapshot?.daysUntilNextPhase ?: 0
    val currentMood = snapshot?.currentMood
    val topSymptoms = snapshot?.topSymptoms ?: emptyList()
    val ownerName = link.ownerDisplayName

    val phaseColors = LocalPhaseColors.current
    val phaseColor = when (currentPhase) {
        CyclePhase.MENSTRUAL  -> phaseColors.menstrual
        CyclePhase.FOLLICULAR -> phaseColors.follicular
        CyclePhase.OVULATION  -> phaseColors.ovulation
        CyclePhase.LUTEAL     -> phaseColors.luteal
        else -> MaterialTheme.colorScheme.primary
    }

    // Fondo con gradiente animado que cambia suavemente con la fase
    val bgColor1 by animateColorAsState(
        targetValue = phaseColor.copy(alpha = 0.08f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "bg_color1"
    )
    val bgColor2 = MaterialTheme.colorScheme.background

    // Anillo pulsante estilo iOS
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring_alpha"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring_scale"
    )

    // Cooldown del abrazo
    val isOnCooldown = uiState.isCareActionOnCooldown()
    var displayCooldown by remember { mutableIntStateOf(uiState.careActionCooldownSeconds()) }
    LaunchedEffect(isOnCooldown, uiState.careActionCooldownUntil) {
        if (isOnCooldown) {
            while (uiState.isCareActionOnCooldown()) {
                displayCooldown = uiState.careActionCooldownSeconds()
                delay(1000)
            }
            displayCooldown = 0
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showHugEffect by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val hugScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "hug_scale"
    )
    val hugHaloAlpha by animateFloatAsState(
        targetValue = if (showHugEffect) 0f else 1f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "hug_halo"
    )

    // Abrazo recibido reciente
    val isOwner = link.ownerUid == currentUid
    val lastHugReceived = if (isOwner) link.lastPartnerCareAction?.timestamp else link.lastOwnerCareAction?.timestamp
    val hugReceivedRecently = lastHugReceived != null &&
            (System.currentTimeMillis() - lastHugReceived) < 60_000L

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (ownerName != null) "Ciclo de $ownerName" else stringResource(R.string.observer_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        AnimatedVisibility(visible = hugReceivedRecently) {
                            Text(
                                "💌 ¡Recibiste un abrazo!",
                                fontSize = 12.sp,
                                color = phaseColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(bgColor1, bgColor2),
                        center = Offset(500f, 200f),
                        radius = 1200f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ── Círculo central de fase ──────────────────────────────────
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 8.dp)) {
                    // Halo exterior pulsante
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .graphicsLayer {
                                scaleX = ringScale
                                scaleY = ringScale
                                alpha = ringAlpha
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(phaseColor.copy(alpha = 0.3f), Color.Transparent)
                                )
                            )
                    )

                    // Halo del abrazo
                    if (showHugEffect) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .graphicsLayer { alpha = 1f - hugHaloAlpha }
                                .clip(CircleShape)
                                .background(phaseColor.copy(alpha = 0.25f))
                        )
                    }

                    // Círculo interior
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        phaseColor.copy(alpha = 0.18f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        phaseColor.copy(alpha = 0.5f),
                                        phaseColor.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = observerPhaseEmoji(currentPhase), fontSize = 36.sp)
                            Text(
                                text = phaseLabel(currentPhase),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            if (currentPhase != CyclePhase.UNKNOWN && daysUntilNext > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(phaseColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.observer_days_left, daysUntilNext),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = phaseColor
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Consejo de fase ──────────────────────────────────────────
                GlassCard(phaseColor = phaseColor) {
                    PhaseAdviceContent(currentPhase = currentPhase, phaseColor = phaseColor)
                }

                // ── Estado de ánimo y síntomas ───────────────────────────────
                if (currentMood != null || topSymptoms.isNotEmpty()) {
                    GlassCard(phaseColor = phaseColor.copy(alpha = 0.5f)) {
                        MoodSymptomsContent(
                            currentMood = currentMood,
                            topSymptoms = topSymptoms,
                            phaseColor = phaseColor
                        )
                    }
                }

                // ── Estado UNKNOWN ───────────────────────────────────────────
                if (currentPhase == CyclePhase.UNKNOWN) {
                    GlassCard(phaseColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌙", fontSize = 28.sp)
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Aún no hay datos del ciclo disponibles. Cuando tu pareja registre su ciclo, verás la información aquí.",
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── Botón de abrazo ──────────────────────────────────────────
                // 💖 Acciones Rápidas 💖
                CareActionsRow(
                    phaseColor = phaseColor,
                    isOnCooldown = isOnCooldown,
                    cooldownSeconds = displayCooldown,
                    onSendCareAction = { action ->
                        if (!isOnCooldown) {
                            coroutineScope.launch {
                                if (action == com.jeremy.lumi.domain.model.CareAction.HUG) {
                                    showHugEffect = true
                                    onSendCareAction(action)
                                    kotlinx.coroutines.delay(700)
                                    showHugEffect = false
                                } else {
                                    onSendCareAction(action)
                                }
                            }
                        }
                    }
                )

                // ── Desvincular ──────────────────────────────────────────────
                TextButton(onClick = onUnlink) {
                    Text(
                        text = stringResource(R.string.partner_btn_unlink),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Glassmorphism Card (usada también en PartnerPairingScreen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    phaseColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        phaseColor.copy(alpha = 0.25f),
                        phaseColor.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Care Actions Row con cooldown
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CareActionsRow(
    phaseColor: Color,
    isOnCooldown: Boolean,
    cooldownSeconds: Int,
    onSendCareAction: (com.jeremy.lumi.domain.model.CareAction) -> Unit
) {
    if (isOnCooldown) {
        val minutes = cooldownSeconds / 60
        val seconds = cooldownSeconds % 60
        val timeStr = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                Spacer(Modifier.width(8.dp))
                Text("Siguiente detalle en $timeStr", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ObserverCareActionButton(emoji = "🤗", label = "Abrazo", phaseColor = phaseColor, onClick = { onSendCareAction(com.jeremy.lumi.domain.model.CareAction.HUG) })
            ObserverCareActionButton(emoji = "🍵", label = "Té", phaseColor = phaseColor, onClick = { onSendCareAction(com.jeremy.lumi.domain.model.CareAction.TEA) })
            ObserverCareActionButton(emoji = "☕", label = "Café", phaseColor = phaseColor, onClick = { onSendCareAction(com.jeremy.lumi.domain.model.CareAction.COFFEE) })
            ObserverCareActionButton(emoji = "🍫", label = "Choco", phaseColor = phaseColor, onClick = { onSendCareAction(com.jeremy.lumi.domain.model.CareAction.CHOCOLATE) })
            ObserverCareActionButton(emoji = "💊", label = "Ayuda", phaseColor = phaseColor, onClick = { onSendCareAction(com.jeremy.lumi.domain.model.CareAction.PHARMACY) })
        }
    }
}

@Composable
private fun ObserverCareActionButton(emoji: String, label: String, phaseColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(phaseColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 26.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Contenido de tarjetas
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhaseAdviceContent(currentPhase: CyclePhase, phaseColor: Color) {
    val (icon, advice) = when (currentPhase) {
        CyclePhase.MENSTRUAL  -> Icons.Rounded.SelfImprovement to stringResource(R.string.phase_advice_menstrual)
        CyclePhase.FOLLICULAR -> Icons.Rounded.WbSunny to stringResource(R.string.phase_advice_follicular)
        CyclePhase.OVULATION  -> Icons.Rounded.AutoAwesome to stringResource(R.string.phase_advice_ovulation)
        CyclePhase.LUTEAL     -> Icons.Rounded.NightlightRound to stringResource(R.string.phase_advice_luteal)
        CyclePhase.PREGNANCY  -> Icons.Rounded.ChildCare to stringResource(R.string.phase_advice_pregnancy)
        else                  -> Icons.Rounded.Spa to stringResource(R.string.phase_advice_unknown)
    }

    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(phaseColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = phaseColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = "¿Cómo está?",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = phaseColor,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = advice,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun MoodSymptomsContent(
    currentMood: String?,
    topSymptoms: List<String>,
    phaseColor: Color
) {
    Column(modifier = Modifier.padding(20.dp)) {
        if (currentMood != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = moodEmoji(currentMood),
                    fontSize = 22.sp,
                    modifier = Modifier
                        .size(32.dp)
                        .wrapContentSize(Alignment.Center)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Estado de ánimo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = phaseColor,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = stringResource(R.string.observer_mood_today, currentMood),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        if (topSymptoms.isNotEmpty()) {
            if (currentMood != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
                )
            }
            Text(
                "Síntomas",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                topSymptoms.take(3).forEach { symptom ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = symptom,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers privados
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun phaseLabel(phase: CyclePhase): String = stringResource(
    id = when (phase) {
        CyclePhase.MENSTRUAL  -> R.string.phase_menstrual
        CyclePhase.FOLLICULAR -> R.string.phase_follicular
        CyclePhase.OVULATION  -> R.string.phase_ovulation
        CyclePhase.LUTEAL     -> R.string.phase_luteal
        CyclePhase.PREGNANCY  -> R.string.phase_pregnancy
        else                  -> R.string.phase_unknown
    }
)

// Nombre distinto para evitar conflicto con phaseEmoji en PartnerPairingScreen (mismo paquete)
private fun observerPhaseEmoji(phase: CyclePhase): String = when (phase) {
    CyclePhase.MENSTRUAL  -> "🌺"
    CyclePhase.FOLLICULAR -> "🌱"
    CyclePhase.OVULATION  -> "✨"
    CyclePhase.LUTEAL     -> "🌙"
    CyclePhase.PREGNANCY  -> "🤱"
    else                  -> "🌸"
}

private fun moodEmoji(mood: String): String = when (mood.lowercase()) {
    "feliz", "happy", "great" -> "😊"
    "triste", "sad" -> "😔"
    "irritable", "irritated" -> "😤"
    "cansada", "tired" -> "😴"
    "ansiosa", "anxious" -> "😰"
    "tranquila", "calm" -> "😌"
    "energizada", "energized" -> "⚡"
    else -> "💫"
}