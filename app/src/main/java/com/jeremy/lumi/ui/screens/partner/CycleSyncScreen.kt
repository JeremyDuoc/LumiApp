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
                                text = if (theirName != null) "Sincronía con $theirName" else "Ciclos Sincronizados",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            AnimatedVisibility(visible = hugReceivedRecently) {
                                Text(
                                    "💌 ¡Recibiste un abrazo!",
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
                .background(
                    Brush.linearGradient(
                        colors = listOf(bgColor1, MaterialTheme.colorScheme.background, bgColor2),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 2000f)
                    )
                )
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
                DualRingSection(
                    myPhase = myPhase,
                    theirPhase = theirPhase,
                    myColor = myColor,
                    theirColor = theirColor,
                    mySnapshot = mySnapshot,
                    theirSnapshot = theirSnapshot,
                    theirName = theirName,
                    syncPulse = syncPulse,
                    syncAlpha = syncAlpha,
                    showHugEffect = showHugEffect,
                    hugHaloAlpha = hugHaloAlpha
                )

                // ── Compatibilidad de energías ────────────────────────────────
                CompatibilityCard(compatibility = compatibility, myColor = myColor, theirColor = theirColor)

                // ── Tarjetas individuales de cada ciclo ───────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniCycleCard(
                        label = "Tú",
                        snapshot = mySnapshot,
                        phaseColor = myColor,
                        modifier = Modifier.weight(1f)
                    )
                    MiniCycleCard(
                        label = if (theirName != null) theirName else "Tu vínculo",
                        snapshot = theirSnapshot,
                        phaseColor = theirColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Consejo de fase cruzada ───────────────────────────────────
                GlassCard(phaseColor = compatibility.color.copy(alpha = 0.5f)) {
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
                        "Desvincular",
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
                .background(
                    Brush.linearGradient(
                        colors = listOf(bgColor1, MaterialTheme.colorScheme.background, bgColor2),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 2000f)
                    )
                )
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
                        "💌 ¡Recibiste un abrazo!",
                        fontSize = 13.sp,
                        color = theirColor,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
                DualRingSection(
                    myPhase = myPhase, theirPhase = theirPhase,
                    myColor = myColor, theirColor = theirColor,
                    mySnapshot = mySnapshot, theirSnapshot = theirSnapshot,
                    theirName = theirName, syncPulse = syncPulse, syncAlpha = syncAlpha,
                    showHugEffect = showHugEffect, hugHaloAlpha = hugHaloAlpha
                )
                CompatibilityCard(compatibility = compatibility, myColor = myColor, theirColor = theirColor)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniCycleCard(label = "Tú", snapshot = mySnapshot, phaseColor = myColor, modifier = Modifier.weight(1f))
                    MiniCycleCard(label = if (theirName != null) theirName else "Tu vínculo", snapshot = theirSnapshot, phaseColor = theirColor, modifier = Modifier.weight(1f))
                }
                GlassCard(phaseColor = compatibility.color.copy(alpha = 0.5f)) {
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
private fun DualRingSection(
    myPhase: CyclePhase,
    theirPhase: CyclePhase,
    myColor: Color,
    theirColor: Color,
    mySnapshot: CycleSnapshot?,
    theirSnapshot: CycleSnapshot?,
    theirName: String?,
    syncPulse: Float,
    syncAlpha: Float,
    showHugEffect: Boolean,
    hugHaloAlpha: Float
) {
    val areSynced = myPhase == theirPhase && myPhase != CyclePhase.UNKNOWN

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // Halo de abrazo
        if (showHugEffect) {
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .graphicsLayer { alpha = 1f - hugHaloAlpha }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                myColor.copy(alpha = 0.3f),
                                theirColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Si están sincronizadas: un solo anillo doble pulsante
        if (areSynced) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .graphicsLayer { scaleX = syncPulse; scaleY = syncPulse; alpha = syncAlpha * 0.4f }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(myColor.copy(alpha = 0.4f), Color.Transparent))
                    )
            )
        }

        // Anillo de la otra persona (derecha/arriba)
        Box(
            modifier = Modifier
                .offset(x = 60.dp, y = (-20).dp)
                .size(160.dp)
                .graphicsLayer { alpha = 0.85f }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            theirColor.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(theirColor.copy(alpha = 0.5f), theirColor.copy(alpha = 0.1f))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(syncPhaseEmoji(theirPhase), fontSize = 28.sp)
                Text(
                    text = phaseLabelShort(theirPhase),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theirColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                if (theirName != null) {
                    Text(
                        theirName,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Anillo propio (izquierda/abajo) — más prominente
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = 20.dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            myColor.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    )
                )
                .border(
                    2.dp,
                    Brush.linearGradient(listOf(myColor.copy(alpha = 0.6f), myColor.copy(alpha = 0.1f))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(syncPhaseEmoji(myPhase), fontSize = 32.sp)
                Text(
                    text = phaseLabelShort(myPhase),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = myColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Text(
                    "Tú",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        // Badge de sincronía perfecta
        if (areSynced) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(myColor.copy(alpha = 0.15f))
                    .border(1.dp, myColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "¡Sincronía perfecta!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = myColor
                    )
                }
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

    GlassCard(phaseColor = phaseColor, modifier = modifier) {
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
    val surfaceColor = MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        myColor.copy(alpha = 0.12f),
                        surfaceColor.copy(alpha = 0.5f),
                        theirColor.copy(alpha = 0.12f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(myColor.copy(alpha = 0.3f), theirColor.copy(alpha = 0.3f))),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Energía Compartida",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(10.dp))

            // Barra de compatibilidad (gradiente entre los dos colores)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(listOf(myColor, theirColor))
                    )
            )
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
    val containerBrush = if (!isOnCooldown)
        Brush.horizontalGradient(listOf(myColor, theirColor))
    else
        Brush.horizontalGradient(listOf(
            Color.Gray.copy(alpha = 0.3f),
            Color.Gray.copy(alpha = 0.3f)
        ))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerBrush)
            .then(if (!isOnCooldown) Modifier else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = !isOnCooldown,
            modifier = Modifier.fillMaxSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            AnimatedContent(
                targetState = isOnCooldown,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "sync_hug_content"
            ) { onCooldown ->
                if (onCooldown) {
                    val minutes = cooldownSeconds / 60
                    val seconds = cooldownSeconds % 60
                    val timeStr = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Siguiente abrazo en $timeStr", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Favorite, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Enviar un abrazo 💫", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
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
            title = "¡Estáis sincronizadas! ✨",
            advice = "Vuestras energías están perfectamente alineadas. Es el momento ideal para compartir planes y apoyarse mutuamente.",
            emoji = "🌕",
            color = myColor
        )
        bothHard -> PhaseCompatibility(
            title = "Semana de calma y cuidado",
            advice = "Ambas necesitáis descanso ahora. Sed pacientes la una con la otra y priorizad el autocuidado. Un buen momento para actividades tranquilas juntas.",
            emoji = "🌙",
            color = Color(0xFF7E57C2)
        )
        bothEnergetic -> PhaseCompatibility(
            title = "¡Semana de alta energía!",
            advice = "Las dos estáis en un momento brillante. Perfectas para proyectos, salidas o nuevos planes. ¡Aprovechadlo juntas!",
            emoji = "⚡",
            color = Color(0xFFF9A825)
        )
        complementary -> PhaseCompatibility(
            title = "Energías complementarias",
            advice = "Una de vosotras está en un momento de alta energía. Es una oportunidad para apoyar y ser apoyada con comprensión y cariño.",
            emoji = "☯️",
            color = Color(0xFF26A69A)
        )
        myPhase == CyclePhase.UNKNOWN || theirPhase == CyclePhase.UNKNOWN -> PhaseCompatibility(
            title = "Datos pendientes",
            advice = "Cuando ambas registréis vuestro ciclo, podréis ver vuestra energía compartida aquí.",
            emoji = "🌸",
            color = Color(0xFF9E9E9E)
        )
        else -> PhaseCompatibility(
            title = "Ciclos en movimiento",
            advice = "Cada ciclo es único. Compartir este camino con alguien especial hace que todo sea más llevadero.",
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

private fun phaseLabelShort(phase: CyclePhase): String = when (phase) {
    CyclePhase.MENSTRUAL  -> "Menstrual"
    CyclePhase.FOLLICULAR -> "Folicular"
    CyclePhase.OVULATION  -> "Ovulación"
    CyclePhase.LUTEAL     -> "Lútea"
    CyclePhase.PREGNANCY  -> "Embarazo"
    else                  -> "Sin datos"
}
