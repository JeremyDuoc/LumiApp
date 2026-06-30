package com.jeremy.lumi.ui.screens.partner

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import kotlinx.coroutines.launch
import kotlin.math.*

// ─── Palette privada del Modo Pareja ─────────────────────────────────────────
private val SpaceBlack    = Color(0xFF080613)
private val DeepViolet    = Color(0xFF0F0A1E)
private val GlassWhite    = Color(0x14FFFFFF)
private val GlassBorder   = Color(0x22FFFFFF)

// ─── Energía de fase (para decidir la velocidad del pulso del orbe) ──────────
private fun CyclePhase.pulseEnergy(): Float = when (this) {
    CyclePhase.OVULATION  -> 1.0f
    CyclePhase.FOLLICULAR -> 0.75f
    CyclePhase.LUTEAL     -> 0.45f
    CyclePhase.MENSTRUAL  -> 0.25f
    else                  -> 0.5f
}

private fun CyclePhase.phaseColor(phaseColors: com.jeremy.lumi.ui.theme.PhaseColors): Color = when (this) {
    CyclePhase.MENSTRUAL  -> phaseColors.menstrual
    CyclePhase.FOLLICULAR -> phaseColors.follicular
    CyclePhase.OVULATION  -> phaseColors.ovulation
    CyclePhase.LUTEAL     -> phaseColors.luteal
    else                  -> Color(0xFF7B2FBE)
}

// ─── Compatibilidad de fases para CycleEcho ──────────────────────────────────
private fun phasesAreCompatible(a: CyclePhase, b: CyclePhase): Boolean =
    (a == CyclePhase.FOLLICULAR || a == CyclePhase.OVULATION) &&
    (b == CyclePhase.FOLLICULAR || b == CyclePhase.OVULATION)

// ═══════════════════════════════════════════════════════════════════════════════
//  PANTALLA PRINCIPAL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun PartnerConnectionScreen(
    uiState: PartnerUiState,
    onOpenStory: (PartnerLink) -> Unit,
    onSendCareAction: (linkId: String, CareAction) -> Unit,
    onOpenAddPartner: () -> Unit,
    onOpenDiary: (PartnerLink) -> Unit,
    onOpenDualCalendar: (PartnerLink) -> Unit,
) {
    val phaseColors = LocalPhaseColors.current
    val particles = remember { generateParticles(count = 60) }

    Box(modifier = Modifier.fillMaxSize().background(SpaceBlack)) {
        StarfieldCanvas(particles = particles)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            PartnerHeader(hasConnections = uiState.activeLinks.isNotEmpty())

            if (uiState.isLoading) {
                SkeletonOrbRow()
            } else if (uiState.activeLinks.isEmpty()) {
                EmptyConnectionState(onAdd = onOpenAddPartner)
            } else {
                StoryOrbRow(
                    links = uiState.activeLinks,
                    currentUid = uiState.currentUid ?: "",
                    phaseColors = phaseColors,
                    onTap = onOpenStory,
                )

                Spacer(modifier = Modifier.height(32.dp))

                val compatiblePair = uiState.activeLinks
                    .zipWithNext()
                    .firstOrNull { (a, b) ->
                        val phaseA = resolvePartnerPhase(a, uiState.currentUid ?: "")
                        val phaseB = resolvePartnerPhase(b, uiState.currentUid ?: "")
                        phasesAreCompatible(phaseA, phaseB)
                    }

                AnimatedVisibility(
                    visible = compatiblePair != null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut()
                ) {
                    compatiblePair?.let { (a, b) ->
                        CycleEchoBanner(
                            nameA = resolvePartnerName(a, uiState.currentUid ?: ""),
                            nameB = resolvePartnerName(b, uiState.currentUid ?: ""),
                            phaseColors = phaseColors,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                uiState.activeLinks.firstOrNull()?.let { firstLink ->
                    QuickActionsRow(
                        link = firstLink,
                        onOpenDiary = { onOpenDiary(firstLink) },
                        onOpenCalendar = { onOpenDualCalendar(firstLink) },
                    )
                }
            }
        }

        GlassFab(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp),
            onClick = onOpenAddPartner,
        )

        uiState.error?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 88.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFB71C1C).copy(alpha = 0.9f),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = error,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HEADER
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PartnerHeader(hasConnections: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.partner_mode_title),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-0.5).sp
        )
        if (hasConnections) {
            Text(
                text = stringResource(R.string.partner_mode_subtitle),
                color = Color.White.copy(alpha = 0.40f),
                fontSize = 13.sp,
                letterSpacing = 0.2.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  STARFIELD CANVAS
// ═══════════════════════════════════════════════════════════════════════════════

private data class Particle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val speed: Float
)

private fun generateParticles(count: Int): List<Particle> =
    (0 until count).map {
        Particle(
            x      = Math.random().toFloat(),
            y      = Math.random().toFloat(),
            radius = (0.5f + Math.random().toFloat() * 2.0f),
            alpha  = (0.1f + Math.random().toFloat() * 0.6f),
            speed  = (1500 + Math.random() * 3000).toFloat()
        )
    }

@Composable
private fun StarfieldCanvas(particles: List<Particle>) {
    val infiniteTransition = rememberInfiniteTransition(label = "starfield")
    val globalTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "global_tick"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val phase = (globalTick * (6000f / p.speed)) % 1f
            val twinkle = (sin(phase * 2f * PI.toFloat()) * 0.5f + 0.5f)
            val finalAlpha = p.alpha * (0.4f + twinkle * 0.6f)

            drawCircle(
                color = Color.White,
                radius = p.radius,
                center = Offset(p.x * w, p.y * h),
                alpha = finalAlpha
            )
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1A0A3A).copy(alpha = 0.6f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.8f),
                radius = w * 0.9f
            ),
            alpha = 1f
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  STORY ORB ROW
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StoryOrbRow(
    links: List<PartnerLink>,
    currentUid: String,
    phaseColors: com.jeremy.lumi.ui.theme.PhaseColors,
    onTap: (PartnerLink) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        itemsIndexed(links, key = { _, link -> link.linkId }) { index, link ->
            val snapshot = if (link.ownerUid == currentUid) link.partnerSnapshot else link.ownerSnapshot
            val phase = snapshot?.currentPhase ?: CyclePhase.UNKNOWN
            val name  = resolvePartnerName(link, currentUid)

            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay((index * 80L))
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.6f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
                ) + fadeIn()
            ) {
                StoryOrbItem(
                    displayName = name,
                    phase = phase,
                    phaseColors = phaseColors,
                    hasCareAction = hasPendingCareAction(link, currentUid),
                    onTap = { onTap(link) }
                )
            }
        }
    }
}

@Composable
fun StoryOrbItem(
    displayName: String,
    phase: CyclePhase,
    phaseColors: com.jeremy.lumi.ui.theme.PhaseColors,
    hasCareAction: Boolean = false,
    onTap: () -> Unit
) {
    val phaseColor = phase.phaseColor(phaseColors)
    val energy     = phase.pulseEnergy()
    val pulseDuration = (800f + (1f - energy) * 2200f).toInt()

    // Manejo de optimización de la animación: Solo animamos si la pantalla está visible
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle(
        initialValue = androidx.lifecycle.Lifecycle.State.INITIALIZED
    )
    val isActive = lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)

    val ringScale = remember { Animatable(1f) }
    val ringAlpha = remember { Animatable(0.4f + (energy * 0.2f)) }
    val arcAngle = remember { Animatable(0f) }

    LaunchedEffect(isActive, energy) {
        if (isActive) {
            launch {
                ringScale.animateTo(
                    targetValue = 1f + (0.06f * energy),
                    animationSpec = infiniteRepeatable(tween(pulseDuration, easing = EaseInOutSine), RepeatMode.Reverse)
                )
            }
            launch {
                ringAlpha.animateTo(
                    targetValue = 0.85f,
                    animationSpec = infiniteRepeatable(tween(pulseDuration, easing = EaseInOutSine), RepeatMode.Reverse)
                )
            }
            launch {
                arcAngle.animateTo(
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart)
                )
            }
        } else {
            ringScale.snapTo(1f)
            ringAlpha.snapTo(0.4f + (energy * 0.2f))
            arcAngle.snapTo(0f)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onTap)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp)
        ) {
            // Halo de energía exterior (Canvas)
            Canvas(
                modifier = Modifier
                    .size(72.dp)
                    // GraphicsLayer evita la recomposición del layout, tal como recomendó Claude
                    .graphicsLayer {
                        scaleX = ringScale.value
                        scaleY = ringScale.value
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val orbRadius = size.minDimension / 2f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            phaseColor.copy(alpha = ringAlpha.value * 0.35f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = orbRadius
                    ),
                    radius = orbRadius,
                    center = center
                )

                rotate(degrees = arcAngle.value, pivot = center) {
                    drawArc(
                        color  = phaseColor.copy(alpha = 0.70f),
                        startAngle = 0f,
                        sweepAngle = 120f * energy,
                        useCenter  = false,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                        topLeft = Offset(6f, 6f),
                        size    = androidx.compose.ui.geometry.Size(size.width - 12f, size.height - 12f)
                    )
                }

                drawCircle(
                    color  = phaseColor.copy(alpha = 0.30f),
                    radius = orbRadius - 3f,
                    center = center,
                    style  = Stroke(width = 1.5f)
                )
            }

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                phaseColor.copy(alpha = 0.35f),
                                DeepViolet.copy(alpha = 0.90f)
                            )
                        )
                    )
                    .border(1.dp, phaseColor.copy(alpha = 0.50f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (hasCareAction) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE91E8C))
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💌", fontSize = 9.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = displayName.take(10),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        val phaseEmoji = when (phase) {
            CyclePhase.MENSTRUAL  -> "🌺"
            CyclePhase.FOLLICULAR -> "🌱"
            CyclePhase.OVULATION  -> "✨"
            CyclePhase.LUTEAL     -> "🌙"
            else                  -> "🌸"
        }
        Text(
            text = phaseEmoji,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  CYCLE ECHO BANNER
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CycleEchoBanner(
    nameA: String,
    nameB: String,
    phaseColors: com.jeremy.lumi.ui.theme.PhaseColors
) {
    val infiniteTransition = rememberInfiniteTransition(label = "echo_glow")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "echo_glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        phaseColors.follicular.copy(alpha = 0.15f),
                        phaseColors.ovulation.copy(alpha = 0.15f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        phaseColors.follicular.copy(alpha = glow * 0.6f),
                        phaseColors.ovulation.copy(alpha = glow * 0.6f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚡", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cycle_echo_title),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(R.string.cycle_echo_desc, nameA, nameB),
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  QUICK ACTIONS ROW
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QuickActionsRow(
    link: PartnerLink,
    onOpenDiary: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(
            emoji = "📔",
            label = stringResource(R.string.partner_action_diary),
            modifier = Modifier.weight(1f),
            onClick = onOpenDiary
        )
        QuickActionCard(
            emoji = "📅",
            label = stringResource(R.string.partner_action_calendar),
            modifier = Modifier.weight(1f),
            onClick = onOpenCalendar
        )
    }
}

@Composable
private fun QuickActionCard(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  EMPTY STATE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyConnectionState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "empty_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue  = 1.0f,
            animationSpec = infiniteRepeatable(
                animation  = tween(2400, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "empty_scale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue  = 0.65f,
            animationSpec = infiniteRepeatable(
                animation  = tween(2400, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "empty_alpha"
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF7B2FBE).copy(alpha = alpha * 0.4f), Color.Transparent),
                        center = c, radius = r
                    ),
                    center = c, radius = r
                )
                drawCircle(
                    color  = Color(0xFF7B2FBE).copy(alpha = alpha),
                    center = c,
                    radius = r - 4f,
                    style  = Stroke(width = 1.5f)
                )
            }
            Text("🌙", fontSize = 44.sp)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.partner_empty_title),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            letterSpacing = 0.2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.partner_empty_desc),
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAdd,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7B2FBE)
            ),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.partner_add_btn),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  GLASS FAB
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GlassFab(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF7B2FBE).copy(alpha = 0.85f), Color(0xFFE91E8C).copy(alpha = 0.85f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = stringResource(R.string.partner_add_btn),
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SKELETON LOADING
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SkeletonOrbRow() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue  = 0.5f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Row(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        repeat(3) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = shimmerAlpha))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White.copy(alpha = shimmerAlpha))
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HELPERS PRIVADOS
// ═══════════════════════════════════════════════════════════════════════════════

private fun resolvePartnerPhase(link: PartnerLink, currentUid: String): CyclePhase {
    val isOwner = link.ownerUid == currentUid
    return (if (isOwner) link.partnerSnapshot else link.ownerSnapshot)?.currentPhase ?: CyclePhase.UNKNOWN
}

private fun resolvePartnerName(link: PartnerLink, currentUid: String): String {
    val isOwner = link.ownerUid == currentUid
    return link.relationLabel.takeIf { it.isNotBlank() }
        ?: if (isOwner) "Pareja"
           else (link.ownerDisplayName ?: "Conexión")
}

private fun hasPendingCareAction(link: PartnerLink, currentUid: String): Boolean {
    val isOwner = link.ownerUid == currentUid
    val action  = if (isOwner) link.lastPartnerCareAction else link.lastOwnerCareAction
    return action != null && (System.currentTimeMillis() - (action.timestamp ?: 0L)) < 300_000L
}
