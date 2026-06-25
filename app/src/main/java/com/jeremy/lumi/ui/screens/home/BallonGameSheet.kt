package com.jeremy.lumi.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

// ─── Constantes del juego ────────────────────────────────────────────────────
private const val MAX_HP     = 10
private const val HIT_FRAMES = 8  // frames de vibración por golpe

// ─── Modelo de partícula de confeti ──────────────────────────────────────────
private data class ConfettiParticle(
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val color: Color,
    val rotation: Float,
    val size: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalloonGameSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()

    // ── Estado del juego ──────────────────────────────────────────────────────
    var hp           by remember { mutableIntStateOf(MAX_HP) }
    var isDefeated   by remember { mutableStateOf(false) }
    var hitFrame     by remember { mutableIntStateOf(0) }   // >0 → animación de golpe activa
    var hitX         by remember { mutableFloatStateOf(0f) }
    var hitY         by remember { mutableFloatStateOf(0f) }
    var comboLabel   by remember { mutableStateOf("") }     // frases de golpe
    var showCombo    by remember { mutableStateOf(false) }
    var randomTip    by remember { mutableStateOf("") }

    val tipsArray    = stringArrayResource(id = R.array.cramp_tips)

    // Frases de empowerment por golpe
    val hitPhrases = stringArrayResource(id = R.array.cramp_hit_phrases)

    // ── Colores ───────────────────────────────────────────────────────────────
    val primary    = MaterialTheme.colorScheme.primary
    val secondary  = MaterialTheme.colorScheme.secondary
    val tertiary   = MaterialTheme.colorScheme.tertiary
    val surface    = MaterialTheme.colorScheme.surface
    val onBg       = MaterialTheme.colorScheme.onBackground

    // Paleta del monstruo: progresa de amenazante → derrotado con el HP
    val hpFraction    = hp.toFloat() / MAX_HP
    val monsterColor  = lerp(Color(0xFFFF6B6B), Color(0xFFCC3333), hpFraction)  // rojo cólico
    val monsterGlow   = lerp(Color(0xFFFFB3B3), Color(0xFFFF4444), hpFraction)

    // Confeti al ganar
    val confettiColors = listOf(
        primary, secondary, tertiary,
        Color(0xFFFFC1CC), Color(0xFFB3F0D1), Color(0xFFFFE4A0)
    )

    // ── Animaciones ───────────────────────────────────────────────────────────

    // Vibración de golpe (shake horizontal)
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(hitFrame) {
        if (hitFrame > 0) {
            shakeX.snapTo(0f)
            shakeX.animateTo(0f, keyframes {
                durationMillis = 300
                (-18f) at 40
                18f  at 100
                (-12f) at 160
                10f  at 200
                (-5f) at 250
                0f   at 300
            })
        }
    }

    // Escala de golpe (squish)
    val squishY = remember { Animatable(1f) }
    LaunchedEffect(hitFrame) {
        if (hitFrame > 0) {
            squishY.snapTo(0.80f)
            squishY.animateTo(1f, spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMedium
            ))
        }
    }

    // Escala de derrota (explota hacia afuera y desaparece)
    val defeatScale = remember { Animatable(1f) }
    val defeatAlpha = remember { Animatable(1f) }
    LaunchedEffect(isDefeated) {
        if (isDefeated) {
            launch { defeatScale.animateTo(2.5f, tween(400, easing = FastOutSlowInEasing)) }
            launch { defeatAlpha.animateTo(0f,   tween(400, easing = FastOutSlowInEasing)) }
        }
    }

    // Partículas de confeti (solo cuando se derrota)
    val confettiParticles = remember { mutableStateListOf<ConfettiParticle>() }
    val confettiProgress  = remember { Animatable(0f) }
    LaunchedEffect(isDefeated) {
        if (isDefeated) {
            // Generar partículas
            repeat(40) {
                confettiParticles.add(
                    ConfettiParticle(
                        x        = 0.5f + Random.nextFloat() * 0.2f - 0.1f,
                        y        = 0.45f,
                        vx       = (Random.nextFloat() - 0.5f) * 1.2f,
                        vy       = -(Random.nextFloat() * 0.8f + 0.3f),
                        color    = confettiColors[Random.nextInt(confettiColors.size)],
                        rotation = Random.nextFloat() * 360f,
                        size     = Random.nextFloat() * 12f + 6f
                    )
                )
            }
            confettiProgress.animateTo(1f, tween(1400, easing = LinearEasing))
        }
    }

    // Pulsación del combo label
    val comboScale = remember { Animatable(1f) }
    LaunchedEffect(comboLabel) {
        if (comboLabel.isNotEmpty()) {
            comboScale.snapTo(0.6f)
            comboScale.animateTo(1f, spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessHigh
            ))
        }
    }

    // Aparición del tip
    val tipAlpha  = remember { Animatable(0f) }
    val tipSlide  = remember { Animatable(20f) }
    LaunchedEffect(isDefeated) {
        if (isDefeated) {
            delay(600)
            launch { tipAlpha.animateTo(1f,  tween(500)) }
            launch { tipSlide.animateTo(0f,  spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        }
    }

    // Barra de rabia: pulsa cuando está casi llena
    val ragePulse = rememberInfiniteTransition(label = "rage")
    val rageGlow  by ragePulse.animateFloat(0.7f, 1f,
        infiniteRepeatable(tween(600), RepeatMode.Reverse), "rage_glow")

    // ── Lógica de golpe ───────────────────────────────────────────────────────
    fun onHit(tapX: Float, tapY: Float) {
        if (isDefeated || hp <= 0) return
        hitX = tapX; hitY = tapY
        hitFrame++
        comboLabel  = hitPhrases[Random.nextInt(hitPhrases.size)]
        showCombo   = true
        val newHp   = (hp - 1).coerceAtLeast(0)
        hp          = newHp
        scope.launch {
            delay(900)
            showCombo = false
        }
        if (newHp == 0) {
            isDefeated  = true
            randomTip   = tipsArray[Random.nextInt(tipsArray.size)]
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Título ────────────────────────────────────────────────────────
            Text(
                text       = if (!isDefeated) stringResource(R.string.cramp_buster_title)
                else stringResource(R.string.cramp_buster_won),
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black,
                color      = if (!isDefeated) primary else secondary,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text      = if (!isDefeated) stringResource(R.string.cramp_buster_subtitle)
                else stringResource(R.string.cramp_buster_subtitle_won),
                fontSize  = 13.sp,
                color     = onBg.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // ── Barra de HP del monstruo ──────────────────────────────────────
            if (!isDefeated) {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text      = stringResource(R.string.cramp_buster_hp, hp, MAX_HP),
                        fontSize  = 11.sp,
                        color     = monsterColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(onBg.copy(alpha = 0.08f))
                    ) {
                        val animHp by animateFloatAsState(
                            targetValue   = hpFraction,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                            label         = "hp_bar"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animHp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFF8A80), monsterColor)
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(if (!isDefeated) 12.dp else 20.dp))

            // ── Arena del monstruo ────────────────────────────────────────────
            Box(
                modifier            = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment    = Alignment.Center
            ) {
                // Confeti de victoria
                if (isDefeated) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        confettiParticles.forEachIndexed { i, p ->
                            val t    = confettiProgress.value
                            val grav = 0.6f
                            val px   = (p.x + p.vx * t) * size.width
                            val py   = (p.y + p.vy * t + grav * t * t) * size.height
                            val rot  = p.rotation + t * 360f * (if (i % 2 == 0) 1 else -1)
                            val alpha = (1f - (t - 0.5f).coerceAtLeast(0f) * 2f).coerceIn(0f, 1f)
                            withTransform({
                                translate(px, py)
                                rotate(rot)
                            }) {
                                drawRect(
                                    color   = p.color.copy(alpha = alpha),
                                    topLeft = Offset(-p.size / 2f, -p.size / 4f),
                                    size    = Size(p.size, p.size / 2f)
                                )
                            }
                        }
                    }
                }

                // El monstruo (Canvas interactivo)
                if (!isDefeated || defeatAlpha.value > 0f) {
                    Canvas(
                        modifier = Modifier
                            .size(170.dp)
                            .graphicsLayer {
                                translationX = shakeX.value
                                scaleX       = defeatScale.value
                                scaleY       = defeatScale.value * squishY.value
                                alpha        = defeatAlpha.value
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null
                            ) { onHit(0.5f, 0.4f) }
                    ) {
                        drawMonster(
                            hpFraction   = hpFraction,
                            monsterColor = monsterColor,
                            monsterGlow  = monsterGlow,
                            onBg         = onBg
                        )

                        // Flash de impacto: círculo blanco que aparece en el tap
                        if (hitFrame > 0 && squishY.value < 0.99f) {
                            drawCircle(
                                color  = Color.White.copy(alpha = (1f - squishY.value) * 2f),
                                radius = size.minDimension * 0.55f
                            )
                        }
                    }

                    // Estrellas de impacto (★) alrededor al golpear
                    if (hitFrame > 0 && squishY.value < 0.95f) {
                        ImpactStars(
                            color    = monsterGlow,
                            progress = 1f - squishY.value
                        )
                    }
                }

                // Frase de combo — alpha manual para evitar conflicto de BoxScope vs ColumnScope
                val comboAlpha by animateFloatAsState(
                    targetValue   = if (showCombo) 1f else 0f,
                    animationSpec = if (showCombo) tween(80) else tween(300),
                    label         = "combo_alpha"
                )
                if (comboAlpha > 0f) {
                    Text(
                        text       = comboLabel,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Black,
                        color      = monsterGlow,
                        modifier   = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .graphicsLayer {
                                alpha  = comboAlpha
                                scaleX = comboScale.value
                                scaleY = comboScale.value
                            }
                    )
                }
            }

            // ── Tip de bienestar (post-victoria) ─────────────────────────────
            if (isDefeated) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(secondary.copy(alpha = 0.10f))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .graphicsLayer {
                            alpha        = tipAlpha.value
                            translationY = tipSlide.value
                        }
                ) {
                    Text(
                        text       = randomTip,
                        fontSize   = 15.sp,
                        lineHeight = 22.sp,
                        textAlign  = TextAlign.Center,
                        color      = onBg.copy(alpha = 0.85f),
                        modifier   = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier       = Modifier.fillMaxWidth(0.6f).height(50.dp),
                    shape          = RoundedCornerShape(16.dp),
                    colors         = ButtonDefaults.buttonColors(containerColor = primary)
                ) {
                    Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                }
            } else {
                // ── Instrucción + indicador de golpes ─────────────────────────
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    repeat(MAX_HP) { i ->
                        val filled  = i >= hp
                        val dotSize by animateDpAsState(
                            targetValue   = if (i == hp) 10.dp else 7.dp,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy),
                            label         = "dot_$i"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(
                                    if (filled) monsterColor.copy(alpha = 0.25f)
                                    else        monsterColor.copy(alpha = if (hpFraction < 0.3f) rageGlow else 0.85f)
                                )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = stringResource(R.string.cramp_buster_tap_hint),
                    fontSize  = 12.sp,
                    color     = onBg.copy(alpha = 0.40f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DIBUJO DEL MONSTRUO (Canvas DrawScope)
//
//  El monstruo es una criatura blob visceral que se degrada visualmente
//  conforme pierde HP: cara de dolor, colmillos, ojos encogidos, grietas.
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawMonster(
    hpFraction: Float,
    monsterColor: Color,
    monsterGlow: Color,
    onBg: Color
) {
    val cx = size.width  / 2f
    val cy = size.height / 2f
    val r  = size.minDimension / 2.2f

    // Halo exterior
    drawCircle(
        brush  = Brush.radialGradient(
            listOf(monsterGlow.copy(alpha = 0.35f), Color.Transparent),
            center = Offset(cx, cy), radius = r * 1.5f
        ),
        center = Offset(cx, cy), radius = r * 1.5f
    )

    // Cuerpo principal — blob ligeramente irregular simulado con Path
    val path = Path().apply {
        val wobbles = 8
        var first = true
        for (i in 0..wobbles) {
            val angle  = (2 * PI / wobbles * i).toFloat()
            val noise  = 1f + sin(angle * 3f + hpFraction * 2f) * 0.08f * (1f - hpFraction)
            val rr     = r * noise
            val px     = cx + cos(angle) * rr
            val py     = cy + sin(angle) * rr
            if (first) { moveTo(px, py); first = false } else lineTo(px, py)
        }
        close()
    }
    drawPath(path, monsterColor)

    // Grietas — aparecen a medida que pierde HP
    val crackAlpha = (1f - hpFraction).coerceIn(0f, 1f)
    if (crackAlpha > 0.1f) {
        val crackPaint = Paint().apply {
            color       = Color.Black.copy(alpha = crackAlpha * 0.5f)
            strokeWidth = 3f
            style       = PaintingStyle.Stroke
        }
        drawContext.canvas.drawPath(
            Path().apply {
                moveTo(cx - r * 0.1f, cy - r * 0.5f)
                lineTo(cx + r * 0.15f, cy - r * 0.1f)
                lineTo(cx - r * 0.05f, cy + r * 0.2f)
            },
            crackPaint
        )
        if (crackAlpha > 0.4f) {
            drawContext.canvas.drawPath(
                Path().apply {
                    moveTo(cx + r * 0.3f, cy - r * 0.35f)
                    lineTo(cx + r * 0.45f, cy + r * 0.05f)
                    lineTo(cx + r * 0.2f,  cy + r * 0.35f)
                },
                crackPaint
            )
        }
    }

    // Cara del monstruo — ojos y boca cambian con HP
    val eyeOffsetY = cy - r * 0.20f
    val eyeSpacing = r * 0.30f
    val eyeRadius  = r * 0.13f * (0.5f + hpFraction * 0.5f)  // encogen al perder HP

    // Ojos: encarnados, con pupila temblorosa
    drawCircle(Color(0xFFFFFFFF), eyeRadius * 1.2f, Offset(cx - eyeSpacing, eyeOffsetY))
    drawCircle(Color(0xFFFFFFFF), eyeRadius * 1.2f, Offset(cx + eyeSpacing, eyeOffsetY))
    // Pupilas — se vuelven más pequeñas y asustadas con menor HP
    val pupilR = eyeRadius * lerp(0.85f, 0.45f, 1f - hpFraction)
    drawCircle(Color(0xFF1A1A1A), pupilR, Offset(cx - eyeSpacing, eyeOffsetY))
    drawCircle(Color(0xFF1A1A1A), pupilR, Offset(cx + eyeSpacing, eyeOffsetY))

    // Cejas enojadas → asustadas
    val browPaint = Paint().apply {
        color       = Color(0xFF8B0000)
        strokeWidth = r * 0.07f
        strokeCap   = StrokeCap.Round
        style       = PaintingStyle.Stroke
    }
    val browTilt = lerp(0.12f, -0.10f, 1f - hpFraction)  // baja la ceja → asustado
    drawContext.canvas.apply {
        // Ceja izquierda
        drawLine(
            Offset(cx - eyeSpacing - eyeRadius, eyeOffsetY - eyeRadius * 1.6f + r * browTilt),
            Offset(cx - eyeSpacing + eyeRadius, eyeOffsetY - eyeRadius * 1.6f - r * browTilt),
            browPaint
        )
        // Ceja derecha
        drawLine(
            Offset(cx + eyeSpacing - eyeRadius, eyeOffsetY - eyeRadius * 1.6f - r * browTilt),
            Offset(cx + eyeSpacing + eyeRadius, eyeOffsetY - eyeRadius * 1.6f + r * browTilt),
            browPaint
        )
    }

    // Boca: enojada → agónica
    val mouthY    = cy + r * 0.28f
    val mouthW    = r * 0.55f
    val mouthCurve = lerp(r * 0.22f, -r * 0.22f, 1f - hpFraction) // curve negativa = boca hacia abajo
    val mouthPaint = Paint().apply {
        color       = Color(0xFF5C0A0A)
        strokeWidth = r * 0.09f
        strokeCap   = StrokeCap.Round
        style       = PaintingStyle.Stroke
    }
    drawContext.canvas.drawPath(
        Path().apply {
            moveTo(cx - mouthW, mouthY)
            quadraticTo(cx, mouthY + mouthCurve, cx + mouthW, mouthY)
        },
        mouthPaint
    )

    // Colmillos — solo cuando tiene mucho HP
    if (hpFraction > 0.35f) {
        val fangAlpha = ((hpFraction - 0.35f) / 0.65f).coerceIn(0f, 1f)
        val fangColor = Color(0xFFFFF8F0).copy(alpha = fangAlpha)
        val fangY     = mouthY + r * 0.05f
        // colmillo izquierdo
        drawPath(Path().apply {
            moveTo(cx - mouthW * 0.35f, fangY)
            lineTo(cx - mouthW * 0.15f, fangY)
            lineTo(cx - mouthW * 0.25f, fangY + r * 0.20f)
            close()
        }, fangColor)
        // colmillo derecho
        drawPath(Path().apply {
            moveTo(cx + mouthW * 0.15f, fangY)
            lineTo(cx + mouthW * 0.35f, fangY)
            lineTo(cx + mouthW * 0.25f, fangY + r * 0.20f)
            close()
        }, fangColor)
    }

    // Puntos de sudor/dolor — aparecen con bajo HP
    if (hpFraction < 0.5f) {
        val sweatAlpha = ((0.5f - hpFraction) * 2f).coerceIn(0f, 1f)
        drawCircle(Color(0xFFADD8E6).copy(alpha = sweatAlpha * 0.8f),
            r * 0.07f, Offset(cx + r * 0.70f, cy - r * 0.40f))
        drawCircle(Color(0xFFADD8E6).copy(alpha = sweatAlpha * 0.6f),
            r * 0.05f, Offset(cx + r * 0.80f, cy - r * 0.15f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ESTRELLAS DE IMPACTO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ImpactStars(color: Color, progress: Float) {
    Canvas(modifier = Modifier.size(170.dp)) {
        val cx = size.width  / 2f
        val cy = size.height / 2f
        val angles = listOf(0f, 72f, 144f, 216f, 288f)
        angles.forEach { angleDeg ->
            val rad  = Math.toRadians(angleDeg.toDouble()).toFloat()
            val dist = size.minDimension * 0.48f * progress
            val sx   = cx + cos(rad) * dist
            val sy   = cy + sin(rad) * dist
            drawCircle(
                color  = color.copy(alpha = progress * 0.9f),
                radius = 9f * progress,
                center = Offset(sx, sy)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Interpolación de Color (helper)
// ─────────────────────────────────────────────────────────────────────────────

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

private fun lerp(a: Color, b: Color, t: Float): Color = Color(
    red   = lerp(a.red,   b.red,   t),
    green = lerp(a.green, b.green, t),
    blue  = lerp(a.blue,  b.blue,  t),
    alpha = lerp(a.alpha, b.alpha, t)
)