package com.jeremy.lumi.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * LUMI · Living Core
 *
 * Port de lumi_home_nucleo_vivo_v3.html:
 * - Movimiento Natural, intensidad orgánica 50 %.
 * - Célula 135 %, 10 partículas, Aurora al 30 %.
 * - Número real del día dentro del núcleo, color según fase.
 * - Se respeta el tema Android elegido; si el usuario usa OLED Noir,
 *   no se cambia ni se sustituye por una paleta clara.
 *
 * ÚNICAMENTE visual: nunca modifica ni calcula datos clínicos/predicciones.
 * La geometría proviene de pathFor() del prototipo; no es un loader circular.
 */
private const val ORGANIC_INTENSITY = .50f
private const val CORE_SCALE = 1.35f
private const val PARTICLE_COUNT = 10
private const val AURORA_POWER = .30f
private const val TWO_PI = (PI * 2.0).toFloat()

private data class CellPalette(
    val first: Color, val second: Color, val third: Color, val fourth: Color,
    val aura: Color, val pace: Float
)

private fun cellPalette(phase: CyclePhase): CellPalette = when (phase) {
    CyclePhase.MENSTRUAL -> CellPalette(
        Color(0xFFB881A4), Color(0xFFDB94AE), Color(0xFFEDB3BF),
        Color(0xFFDFC3C7), Color(0xFFDFA5BB), .82f
    )
    CyclePhase.FOLLICULAR -> CellPalette(
        Color(0xFFCBB4EE), Color(0xFFE5B9D4), Color(0xFFF3CEB4),
        Color(0xFFB2D7CD), Color(0xFFE4C1EF), 1f
    )
    CyclePhase.OVULATION -> CellPalette(
        Color(0xFFE8B7E4), Color(0xFFF4C4C8), Color(0xFFFBD7A9),
        Color(0xFFBCDED0), Color(0xFFF3C9CB), 1.12f
    )
    CyclePhase.LUTEAL -> CellPalette(
        Color(0xFFAC96BD), Color(0xFFD7A6BF), Color(0xFFEBBBAB),
        Color(0xFFD4C0B3), Color(0xFFDEB7CA), .87f
    )
    else -> CellPalette(
        Color(0xFFD8B3F0), Color(0xFFE5B9D4), Color(0xFFF5CAAA),
        Color(0xFFA7D1CC), Color(0xFFE4C1EF), 1f
    )
}

private fun organPath(phase: Float, pace: Float, beat: Float, expansion: Float = 1f): Path {
    val n = 68
    val points = Array(n) { i ->
        val a = i.toFloat() / n * TWO_PI
        val anatomy = 4.1f * sin(a - 1.05f) +
            3.8f * sin(2f * a + .30f) +
            2.8f * sin(3f * a - 1.68f) +
            2f * cos(5f * a + .85f)
        // Natural 50% = 0.72 (natural) * 0.50 (intensity).
        val drift = (.72f * ORGANIC_INTENSITY) * (
            3.25f * sin(3f * a + 3f * phase) +
                2.2f * sin(5f * a - 5f * phase + .8f) +
                cos(7f * a + 2f * phase)
            )
        val breath = 1.2f * sin(3f * phase * pace)
        val ripple = beat * 3f * sin(2.1f * TWO_PI * (1f - beat))
        val radius = (91.1f + anatomy + drift + breath + ripple) * expansion
        Offset(200f + radius * cos(a), 199f + radius * sin(a))
    }
    // Same closed Catmull–Rom to cubic Bézier interpolation as the HTML.
    return Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 0 until n) {
            val p0 = points[(i - 1 + n) % n]
            val p1 = points[i]
            val p2 = points[(i + 1) % n]
            val p3 = points[(i + 2) % n]
            cubicTo(
                p1.x + (p2.x - p0.x) / 6f,
                p1.y + (p2.y - p0.y) / 6f,
                p2.x - (p3.x - p1.x) / 6f,
                p2.y - (p3.y - p1.y) / 6f,
                p2.x, p2.y
            )
        }
        close()
    }
}

@Composable
fun LumiLivingCore(
    modifier: Modifier = Modifier,
    currentDay: Int,
    currentPhase: CyclePhase,
    phaseName: String,
    estimatedDaysText: String,
    confidenceLabel: String,
    onConfidenceClick: (() -> Unit)? = null
) {
    val tokens = LumiThemeTokens.current
    val colors = remember(currentPhase) { cellPalette(currentPhase) }
    val a by animateColorAsState(colors.first, tween(1700), label = "lumi_skin_a")
    val b by animateColorAsState(colors.second, tween(1700), label = "lumi_skin_b")
    val c by animateColorAsState(colors.third, tween(1700), label = "lumi_skin_c")
    val d by animateColorAsState(colors.fourth, tween(1700), label = "lumi_skin_d")
    val aura by animateColorAsState(colors.aura, tween(1700), label = "lumi_aura_phase")

    // Android 'remove animations' setting: do not animate or simulate a loader.
    val mayAnimate = remember { ValueAnimator.areAnimatorsEnabled() }
    val t = if (mayAnimate) {
        val transition = rememberInfiniteTransition(label = "lumi_living_core_natural")
        val motion by transition.animateFloat(
            initialValue = 0f,
            targetValue = TWO_PI,
            animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
            label = "lumi_membrane_24s"
        )
        motion
    } else 0f
    val pulse = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val ink = Color(0xFF35283E) // Inside is a light translucent cell, including OLED Noir.

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val diameter = minOf(maxWidth - 24.dp, 328.dp)
            Box(
                modifier = Modifier.size(diameter)
                    .semantics(mergeDescendants = true) {
                        contentDescription = if (currentDay > 0)
                            "Núcleo vivo Lumi, día $currentDay del ciclo, $phaseName. Toca para sentir un latido."
                        else "Núcleo vivo Lumi, $phaseName. Toca para sentir un latido."
                        role = Role.Button
                    }
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Latido del núcleo Lumi"
                    ) {
                        if (mayAnimate) scope.launch {
                            pulse.snapTo(1f)
                            pulse.animateTo(0f, tween(850))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val ratio = size.minDimension / 400f
                    val center = Offset(200f, 200f)
                    val palette = listOf(a, b, c, d)
                    val shape = organPath(t, colors.pace, pulse.value)
                    val rim = organPath(t, colors.pace, pulse.value, 1.14f)
                    val inner = organPath(t, colors.pace, pulse.value, .77f)

                    withTransform({
                        scale(ratio, ratio, pivot = Offset.Zero)
                    }) {
                        // Aurora 30 %: three low-energy atmospheric colors.
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(aura.copy(alpha = .28f * AURORA_POWER), Color.Transparent),
                                center = Offset(154f, 161f), radius = 175f
                            ), radius = 175f, center = Offset(154f, 161f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(b.copy(alpha = .22f * AURORA_POWER), Color.Transparent),
                                center = Offset(248f, 223f), radius = 164f
                            ), radius = 164f, center = Offset(248f, 223f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(d.copy(alpha = .21f * AURORA_POWER), Color.Transparent),
                                center = Offset(210f, 286f), radius = 166f
                            ), radius = 166f, center = Offset(210f, 286f)
                        )

                        // Stationary dotted orbital reference, very faint; never a progress ring.
                        drawCircle(
                            color = tokens.accent.copy(alpha = if (tokens.isDark) .22f else .14f),
                            radius = 153f,
                            center = center,
                            style = Stroke(
                                width = .55f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5f, 12f))
                            )
                        )
                        // Exactly ten deterministically distributed floating inclusions.
                        for (i in 0 until PARTICLE_COUNT) {
                            val seed = ((i * 41 + 17) % 59) / 59f
                            val angle = i * 2.399963f + seed * .16f +
                                .07f * sin(t * (1f + seed) + i * 1.37f)
                            val r = 118f + (i * 17) % 49 + (CORE_SCALE - 1f) * 102f +
                                3.5f * sin(t * .7f + i * 1.37f)
                            drawCircle(
                                color = palette[i % palette.size].copy(
                                    alpha = (.48f + .16f * sin(t + i * 1.37f)).coerceIn(.20f, .70f)
                                ),
                                radius = .65f + (i % 6) * .25f,
                                center = Offset(200f + r * cos(angle), 200f + r * .94f * sin(angle))
                            )
                        }

                        // The two soft membrane layers scale together around 200,200.
                        withTransform({ scale(CORE_SCALE, CORE_SCALE, pivot = center) }) {
                            drawPath(
                                rim, Color.White.copy(alpha = .26f),
                                style = Stroke(width = .9f)
                            )
                            drawPath(
                                shape,
                                brush = Brush.linearGradient(
                                    0f to a, .33f to b, .67f to c, 1f to d,
                                    start = Offset(126f, 294f), end = Offset(272f, 108f)
                                )
                            )
                            clipPath(shape) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color.White.copy(alpha = .72f), Color.White.copy(alpha = .12f), Color.Transparent),
                                        center = Offset(152f, 143f), radius = 109f
                                    ),
                                    center = Offset(152f, 143f), radius = 109f
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(d.copy(alpha = .49f), Color.Transparent),
                                        center = Offset(238f + 5f * sin(t), 247f), radius = 88f
                                    ), center = Offset(238f + 5f * sin(t), 247f), radius = 88f
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(b.copy(alpha = .52f), Color.Transparent),
                                        center = Offset(158f - 5f * sin(t * .6f), 214f), radius = 100f
                                    ), center = Offset(158f - 5f * sin(t * .6f), 214f), radius = 100f
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0xFF6D447E).copy(alpha = .11f), Color.Transparent),
                                        center = Offset(263f, 264f), radius = 105f
                                    ), center = Offset(263f, 264f), radius = 105f
                                )
                                // Internal cellular contours: static geometry, tiny, slow light drift.
                                drawOval(
                                    color = Color.White.copy(alpha = .16f),
                                    topLeft = Offset(196f, 197f),
                                    size = Size(78f, 62f),
                                    style = Stroke(width = 1.0f)
                                )
                                drawOval(
                                    color = Color.White.copy(alpha = .10f),
                                    topLeft = Offset(203f, 204f),
                                    size = Size(64f, 49f),
                                    style = Stroke(width = .7f)
                                )
                                drawCircle(
                                    Color.White.copy(alpha = .20f), radius = 3f,
                                    center = Offset(161f, 218f)
                                )
                                drawCircle(
                                    Color.White.copy(alpha = .24f), radius = 1.8f,
                                    center = Offset(259f, 174f)
                                )
                                drawPath(
                                    inner, Color.White.copy(alpha = .17f),
                                    style = Stroke(width = .7f)
                                )
                            }
                            drawPath(
                                shape, Color.White.copy(alpha = .52f),
                                style = Stroke(width = 1.1f, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
                // Foreground typography is kept OUTSIDE the deforming membrane.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "DÍA DEL CICLO",
                        color = ink.copy(alpha = .76f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.45.sp
                    )
                    Text(
                        if (currentDay > 0) "$currentDay" else "—",
                        color = ink,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 57.sp,
                        lineHeight = 63.sp,
                        letterSpacing = (-3).sp
                    )
                    if (currentDay > 0) {
                        Text(
                            "HOY",
                            color = ink.copy(alpha = .73f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(1.dp))
        Text(
            "FASE ACTUAL",
            color = tokens.muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.35.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            phaseName,
            color = tokens.accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            estimatedDaysText,
            color = tokens.muted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))
        val confidenceModifier = if (onConfidenceClick != null) {
            Modifier.clickable(
                role = Role.Button,
                onClickLabel = confidenceLabel,
                onClick = onConfidenceClick
            )
        } else Modifier
        Row(
            modifier = confidenceModifier
                .defaultMinSize(minHeight = 44.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = tokens.accent.copy(alpha = .7f),
                modifier = Modifier.size(5.dp)
            ) { }
            Spacer(Modifier.width(7.dp))
            Text(
                confidenceLabel,
                color = tokens.muted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(5.dp))
            LumiIconInfo(tint = tokens.dim, size = 12.dp)
        }
    }
}

/**
 * 30% Aurora atmosphere spread over the Home, like the HTML background pools.
 * Purely decorative, beneath the Scaffold. Does not replace the user's theme.
 */
@Composable
fun LumiLivingAuroraBackdrop(
    currentPhase: CyclePhase,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val tokens = LumiThemeTokens.current
    val colors = remember(currentPhase) { cellPalette(currentPhase) }
    val first by animateColorAsState(colors.first, tween(1700), label = "aurora_phase_a")
    val second by animateColorAsState(colors.second, tween(1700), label = "aurora_phase_b")
    val third by animateColorAsState(colors.fourth, tween(1700), label = "aurora_phase_c")
    val animate = remember { ValueAnimator.areAnimatorsEnabled() }
    val movement = if (animate) {
        val transition = rememberInfiniteTransition(label = "lumi_aurora_backdrop")
        val drift by transition.animateFloat(
            initialValue = -1f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(24_000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_drift"
        )
        drift
    } else 0f
    Canvas(modifier) {
        val strength = AURORA_POWER * if (tokens.isDark) .75f else 1f
        val w = size.width
        val h = size.height
        val pools = listOf(
            Triple(Offset(w * .05f + 22f * movement, h * .24f - 21f * movement), first, .60f),
            Triple(Offset(w * .95f - 19f * movement, h * .43f + 24f * movement), second, .52f),
            Triple(Offset(w * .42f + 10f * movement, h * .81f - 26f * movement), third, .43f)
        )
        for ((center, color, opacity) in pools) {
            val radius = max(w * .90f, h * .29f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = strength * opacity), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                center = center,
                radius = radius
            )
        }
    }
}
