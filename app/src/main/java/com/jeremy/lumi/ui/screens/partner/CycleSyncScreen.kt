package com.jeremy.lumi.ui.screens.partner

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.CycleSnapshot
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla de Sincronización de Ciclos — Modo Vínculo (CycleSync).
 * Muestra los ciclos de ambas personas sincronizados visualmente.
 * Lenguaje completamente neutro (sin alusión a género).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleSyncScreen(
    link: PartnerLink,
    currentUid: String,
    uiState: PartnerUiState,
    onSendCareAction: (com.jeremy.lumi.domain.model.CareAction) -> Unit,
    onUnlink: () -> Unit,
    onBack: () -> Unit,
    /** Cuando se llama desde un BottomSheet, pasar false para omitir el Scaffold/TopAppBar propio */
    showTopBar: Boolean = true
) {
    val isOwner = link.ownerUid == currentUid
    val mySnapshot = if (isOwner) link.ownerSnapshot else link.partnerSnapshot
    val theirSnapshot = if (isOwner) link.partnerSnapshot else link.ownerSnapshot
    val theirName = link.ownerDisplayName?.takeIf { !isOwner }

    val phaseColors = LocalPhaseColors.current

    fun phaseColor(phase: CyclePhase) = when (phase) {
        CyclePhase.MENSTRUAL  -> phaseColors.menstrual
        CyclePhase.FOLLICULAR -> phaseColors.follicular
        CyclePhase.OVULATION  -> phaseColors.ovulation
        CyclePhase.LUTEAL     -> phaseColors.luteal
        else -> Color(0xFF9E9E9E)
    }

    val myPhase = mySnapshot?.currentPhase ?: CyclePhase.UNKNOWN
    val theirPhase = theirSnapshot?.currentPhase ?: CyclePhase.UNKNOWN
    val myColor = phaseColor(myPhase)
    val theirColor = phaseColor(theirPhase)

    // Compatibilidad de energías
    val compatibility = phaseCompatibility(myPhase, theirPhase)

    // Animación del fondo compartido
    val bgColor1 by animateColorAsState(
        targetValue = myColor.copy(alpha = 0.06f),
        animationSpec = tween(1000),
        label = "bg1"
    )
    val bgColor2 by animateColorAsState(
        targetValue = theirColor.copy(alpha = 0.06f),
        animationSpec = tween(1000),
        label = "bg2"
    )

    // Anillo pulsante compartido
    val infiniteTransition = rememberInfiniteTransition(label = "sync_pulse")
    val syncPulse by infiniteTransition.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sync_scale"
    )
    val syncAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sync_alpha"
    )

    // Abrazo
    val isOnCooldown = uiState.isCareActionOnCooldown()
    val cooldownUntil = uiState.careActionCooldownUntil
    var displayCooldown by remember { mutableIntStateOf(0) }
    LaunchedEffect(cooldownUntil) {
        while (cooldownUntil > System.currentTimeMillis()) {
            displayCooldown = ((cooldownUntil - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
            delay(1000)
        }
        displayCooldown = 0
    }

    var showHugEffect by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val hugHaloAlpha by animateFloatAsState(
        targetValue = if (showHugEffect) 0f else 1f,
        animationSpec = tween(700),
        label = "hug_halo"
    )

    // Abrazo recibido
    val lastHugReceived = if (isOwner) link.lastPartnerCareAction?.timestamp else link.lastOwnerCareAction?.timestamp
    val hugReceivedRecently = lastHugReceived != null &&
            (System.currentTimeMillis() - lastHugReceived) < 60_000L

    if (showTopBar) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (theirName != null) stringResource(R.string.cycle_sync_sync_title, theirName) else stringResource(R.string.cycle_sync_title),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 0.3.sp
                            )
                            AnimatedVisibility(visible = hugReceivedRecently) {
                                Text(
                                    stringResource(R.string.cycle_sync_hug_received),
                                    fontSize = 12.sp,
                                    color = theirColor,
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
        ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0E0A1A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ── Doble anillo de fases ─────────────────────────────────────
                PhaseDualOrbit(
                    myPhase = myPhase,
                    partnerPhase = theirPhase,
                    myColor = myColor,
                    partnerColor = theirColor,
                    myEmoji = syncPhaseEmoji(myPhase),
                    partnerEmoji = syncPhaseEmoji(theirPhase),
                    myName = stringResource(R.string.story_you),
                    partnerName = theirName ?: stringResource(R.string.partner_default_link_name)
                )

                // ── Compatibilidad de energías ────────────────────────────────
                CompatibilityCard(compatibility = compatibility, myColor = myColor, theirColor = theirColor)

                // ── Tarjetas individuales de cada ciclo ───────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniCycleCard(
                        label = stringResource(R.string.story_you),
                        snapshot = mySnapshot,
                        phaseColor = myColor,
                        modifier = Modifier.weight(1f)
                    )
                    MiniCycleCard(
                        label = theirName ?: stringResource(R.string.partner_default_link_name),
                        snapshot = theirSnapshot,
                        phaseColor = theirColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Consejo de fase cruzada ───────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1025))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(compatibility.emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                compatibility.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                compatibility.advice,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Botón de abrazo ───────────────────────────────────────────
                SyncHugButton(
                    myColor = myColor,
                    theirColor = theirColor,
                    isOnCooldown = isOnCooldown,
                    cooldownSeconds = displayCooldown,
                    onClick = {
                        if (!isOnCooldown) {
                            coroutineScope.launch {
                                showHugEffect = true
                                onSendCareAction(com.jeremy.lumi.domain.model.CareAction.HUG)
                                delay(700)
                                showHugEffect = false
                            }
                        }
                    }
                )

                TextButton(onClick = onUnlink) {
                    Text(
                        stringResource(R.string.partner_unlink),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                    )
                }
            }
        }
        }   // cierre lambda Scaffold { padding -> }
    } else {
        // Sin Scaffold propio — directo al contenido para embeber en BottomSheet
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0E0A1A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (hugReceivedRecently) {
                    Text(
                        stringResource(R.string.cycle_sync_hug_received),
                        fontSize = 13.sp,
                        color = theirColor,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
                PhaseDualOrbit(
                    myPhase = myPhase, partnerPhase = theirPhase,
                    myColor = myColor, partnerColor = theirColor,
                    myEmoji = syncPhaseEmoji(myPhase), partnerEmoji = syncPhaseEmoji(theirPhase),
                    myName = stringResource(R.string.story_you), partnerName = theirName ?: stringResource(R.string.partner_default_link_name)
                )
                CompatibilityCard(compatibility = compatibility, myColor = myColor, theirColor = theirColor)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniCycleCard(label = stringResource(R.string.story_you), snapshot = mySnapshot, phaseColor = myColor, modifier = Modifier.weight(1f))
                    MiniCycleCard(label = theirName ?: stringResource(R.string.partner_default_link_name), snapshot = theirSnapshot, phaseColor = theirColor, modifier = Modifier.weight(1f))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1025))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(compatibility.emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(compatibility.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.height(2.dp))
                            Text(compatibility.advice, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                        }
                    }
                }
                SyncHugButton(
                    myColor = myColor, theirColor = theirColor,
                    isOnCooldown = isOnCooldown, cooldownSeconds = displayCooldown,
                    onClick = {
                        if (!isOnCooldown) {
                            coroutineScope.launch {
                                showHugEffect = true
                                onSendCareAction(com.jeremy.lumi.domain.model.CareAction.HUG)
                                delay(700)
                                showHugEffect = false
                            }
                        }
                    }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Doble Anillo Visual
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhaseDualOrbit(
    myPhase: CyclePhase,
    partnerPhase: CyclePhase,
    myColor: Color,
    partnerColor: Color,
    myEmoji: String,
    partnerEmoji: String,
    myName: String,
    partnerName: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbit_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Círculo izquierdo (tú)
        Box(
            modifier = Modifier
                .offset(x = (-48).dp)
                .size(140.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(myColor.copy(alpha = 0.18f))
                .border(1.5.dp, myColor.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = myEmoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = myName, color = myColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Círculo derecho (pareja)
        Box(
            modifier = Modifier
                .offset(x = 48.dp)
                .size(140.dp)
                .scale(2f - pulse)   // pulso inverso
                .clip(CircleShape)
                .background(partnerColor.copy(alpha = 0.18f))
                .border(1.5.dp, partnerColor.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = partnerEmoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = partnerName, color = partnerColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tarjeta Mini de cada ciclo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MiniCycleCard(
    label: String,
    snapshot: CycleSnapshot?,
    phaseColor: Color,
    modifier: Modifier = Modifier
) {
    val phase = snapshot?.currentPhase ?: CyclePhase.UNKNOWN
    val days = snapshot?.daysUntilNextPhase ?: 0
    val mood = snapshot?.currentMood

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1025))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )
            Text(syncPhaseEmoji(phase), fontSize = 28.sp)
            Text(
                phaseLabelShort(phase),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = phaseColor,
                textAlign = TextAlign.Center
            )
            if (days > 0 && phase != CyclePhase.UNKNOWN) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(phaseColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${days}d",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = phaseColor
                    )
                }
            }
            if (mood != null) {
                Text(
                    mood,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tarjeta de Compatibilidad
// ─────────────────────────────────────────────────────────────────────────────

data class PhaseCompatibility(
    val title: String,
    val advice: String,
    val emoji: String,
    val color: Color
)

@Composable
private fun CompatibilityCard(compatibility: PhaseCompatibility, myColor: Color, theirColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1025))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.cycle_sync_shared_energy),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(10.dp))

            // Barra de compatibilidad (gradiente entre los dos colores)
            val gradientBrush = Brush.horizontalGradient(
                colors = listOf(myColor, theirColor)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .fillMaxHeight()
                        .background(gradientBrush)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                compatibility.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Botón de abrazo sincronizado
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SyncHugButton(
    myColor: Color,
    theirColor: Color,
    isOnCooldown: Boolean,
    cooldownSeconds: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isOnCooldown,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                brush = if (!isOnCooldown)
                    Brush.linearGradient(listOf(Color(0xFF7B2FBE), Color(0xFFE91E8C)))
                else
                    Brush.linearGradient(listOf(Color.Gray, Color.Gray)),
                shape = RoundedCornerShape(50)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(50)
    ) {
        if (isOnCooldown) {
            val minutes = cooldownSeconds / 60
            val seconds = cooldownSeconds % 60
            val timeStr = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
            Text(
                text = stringResource(R.string.cycle_sync_hug_cooldown, timeStr),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        } else {
            Text(
                text = stringResource(R.string.cycle_sync_send_hug_button),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Lógica de compatibilidad de fases
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun phaseCompatibility(myPhase: CyclePhase, theirPhase: CyclePhase): PhaseCompatibility {
    val myColor = LocalPhaseColors.current.run {
        when (myPhase) {
            CyclePhase.MENSTRUAL -> menstrual
            CyclePhase.FOLLICULAR -> follicular
            CyclePhase.OVULATION -> ovulation
            CyclePhase.LUTEAL -> luteal
            else -> Color(0xFF9E9E9E)
        }
    }
    // Semana difícil: ambas en fase lútea o menstrual
    val bothHard = (myPhase == CyclePhase.LUTEAL || myPhase == CyclePhase.MENSTRUAL) &&
                   (theirPhase == CyclePhase.LUTEAL || theirPhase == CyclePhase.MENSTRUAL)
    // Semana brillante: ambas en folicular u ovulación
    val bothEnergetic = (myPhase == CyclePhase.FOLLICULAR || myPhase == CyclePhase.OVULATION) &&
                        (theirPhase == CyclePhase.FOLLICULAR || theirPhase == CyclePhase.OVULATION)
    // Una en alta, otra en baja
    val complementary = (myPhase == CyclePhase.OVULATION || myPhase == CyclePhase.FOLLICULAR) &&
                        (theirPhase == CyclePhase.LUTEAL || theirPhase == CyclePhase.MENSTRUAL) ||
                        (theirPhase == CyclePhase.OVULATION || theirPhase == CyclePhase.FOLLICULAR) &&
                        (myPhase == CyclePhase.LUTEAL || myPhase == CyclePhase.MENSTRUAL)
    val synced = myPhase == theirPhase && myPhase != CyclePhase.UNKNOWN

    return when {
        synced -> PhaseCompatibility(
            title = stringResource(R.string.compat_synced_title),
            advice = stringResource(R.string.compat_synced_advice),
            emoji = "🌕",
            color = myColor
        )
        bothHard -> PhaseCompatibility(
            title = stringResource(R.string.compat_hard_title),
            advice = stringResource(R.string.compat_hard_advice),
            emoji = "🌙",
            color = Color(0xFF7E57C2)
        )
        bothEnergetic -> PhaseCompatibility(
            title = stringResource(R.string.compat_energetic_title),
            advice = stringResource(R.string.compat_energetic_advice),
            emoji = "⚡",
            color = Color(0xFFF9A825)
        )
        complementary -> PhaseCompatibility(
            title = stringResource(R.string.compat_complementary_title),
            advice = stringResource(R.string.compat_complementary_advice),
            emoji = "☯️",
            color = Color(0xFF26A69A)
        )
        myPhase == CyclePhase.UNKNOWN || theirPhase == CyclePhase.UNKNOWN -> PhaseCompatibility(
            title = stringResource(R.string.compat_pending_title),
            advice = stringResource(R.string.compat_pending_advice),
            emoji = "🌸",
            color = Color(0xFF9E9E9E)
        )
        else -> PhaseCompatibility(
            title = stringResource(R.string.compat_moving_title),
            advice = stringResource(R.string.compat_moving_advice),
            emoji = "💫",
            color = myColor
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun syncPhaseEmoji(phase: CyclePhase): String = when (phase) {
    CyclePhase.MENSTRUAL  -> "🌺"
    CyclePhase.FOLLICULAR -> "🌱"
    CyclePhase.OVULATION  -> "✨"
    CyclePhase.LUTEAL     -> "🌙"
    CyclePhase.PREGNANCY  -> "🤱"
    else                  -> "🌸"
}

@Composable
private fun phaseLabelShort(phase: CyclePhase): String = when (phase) {
    CyclePhase.MENSTRUAL  -> stringResource(R.string.phase_name_menstrual)
    CyclePhase.FOLLICULAR -> stringResource(R.string.phase_name_follicular)
    CyclePhase.OVULATION  -> stringResource(R.string.phase_name_ovulation)
    CyclePhase.LUTEAL     -> stringResource(R.string.phase_name_luteal)
    CyclePhase.PREGNANCY  -> stringResource(R.string.phase_name_pregnancy)
    else                  -> stringResource(R.string.phase_name_unknown)
}
