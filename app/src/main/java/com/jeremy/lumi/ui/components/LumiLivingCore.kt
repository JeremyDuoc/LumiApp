package com.jeremy.lumi.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * LUMI · Living Core
 *
 * Port de lumi_home_nucleo_vivo_v3.html:
 * - Movimiento Natural, intensidad orgánica 50 %.
 * - Célula 135 %, 10 partículas, aura tomada de su superficie.
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
private const val TWO_PI = (PI * 2.0).toFloat()

private data class CellPalette(
    val first: Color, val second: Color, val third: Color, val fourth: Color,
    val pace: Float
)

private fun cellPalette(phase: CyclePhase): CellPalette = when (phase) {
    CyclePhase.MENSTRUAL -> CellPalette(
        Color(0xFFB881A4), Color(0xFFDB94AE), Color(0xFFEDB3BF),
        Color(0xFFDFC3C7), .82f
    )
    CyclePhase.FOLLICULAR -> CellPalette(
        Color(0xFFCBB4EE), Color(0xFFE5B9D4), Color(0xFFF3CEB4),
        Color(0xFFB2D7CD), 1f
    )
    CyclePhase.OVULATION -> CellPalette(
        Color(0xFFE8B7E4), Color(0xFFF4C4C8), Color(0xFFFBD7A9),
        Color(0xFFBCDED0), 1.12f
    )
    CyclePhase.LUTEAL -> CellPalette(
        Color(0xFFAC96BD), Color(0xFFD7A6BF), Color(0xFFEBBBAB),
        Color(0xFFD4C0B3), .87f
    )
    else -> CellPalette(
        Color(0xFFD8B3F0), Color(0xFFE5B9D4), Color(0xFFF5CAAA),
        Color(0xFFA7D1CC), 1f
    )
}

private data class CellGeometry(val path: Path, val edge: Array<Offset>)

private val SKIN_START = Offset(126f, 294f)
private val SKIN_END = Offset(272f, 108f)

/** Same gradient and moving light fields as the skin, sampled at the living edge. */
private fun surfaceColorAt(
    point: Offset, palette: List<Color>, mintCenter: Offset, roseCenter: Offset,
    highlightAlpha: Float, depthAlpha: Float
): Color {
    val axis = SKIN_END - SKIN_START
    val delta = point - SKIN_START
    val fraction = ((delta.x * axis.x + delta.y * axis.y) /
        (axis.x * axis.x + axis.y * axis.y)).coerceIn(0f, 1f)
    var color = when {
        fraction < .33f -> lerp(palette[0], palette[1], fraction / .33f)
        fraction < .67f -> lerp(palette[1], palette[2], (fraction - .33f) / .34f)
        else -> lerp(palette[2], palette[3], (fraction - .67f) / .33f)
    }
    val highlight = ((point - Offset(152f, 143f)).getDistance() / 109f).coerceIn(0f, 1f)
    val whiteAlpha = if (highlight < .5f) {
        highlightAlpha + (.12f - highlightAlpha) * highlight * 2f
    } else .12f * (1f - highlight) * 2f
    color = Color.White.copy(alpha = whiteAlpha).compositeOver(color)
    fun light(tint: Color, center: Offset, radius: Float, alpha: Float) {
        val falloff = (1f - (point - center).getDistance() / radius).coerceIn(0f, 1f)
        color = tint.copy(alpha = alpha * falloff).compositeOver(color)
    }
    light(palette[3], mintCenter, 88f, .49f)
    light(palette[1], roseCenter, 100f, .52f)
    light(Color(0xFF6D447E), Offset(263f, 264f), 105f, depthAlpha)
    return color
}

private fun organGeometry(time: Float, breathTime: Float, beat: Float): CellGeometry {
    val n = 68
    val points = Array(n) { i ->
        val a = i.toFloat() / n * TWO_PI
        val anatomy = 4.1f * sin(a - 1.05f) +
            3.8f * sin(2f * a + .30f) +
            2.8f * sin(3f * a - 1.68f) +
            2f * cos(5f * a + .85f)
        // Natural 50% = 0.72 (natural) * 0.50 (intensity).
        val drift = (.72f * ORGANIC_INTENSITY) * (
            3.25f * sin(3f * a + .62f * time) +
                2.2f * sin(5f * a - .91f * time + .8f) +
                cos(7f * a + .39f * time)
            )
        val breath = 1.2f * sin(1.13f * breathTime)
        val ripple = beat * 3.6f * sin(1.05f * TWO_PI * (1f - beat))
        val radius = 91.1f + anatomy + drift + breath + ripple
        Offset(200f + radius * cos(a), 199f + radius * sin(a))
    }
    // Same closed Catmull–Rom to cubic Bézier interpolation as the HTML.
    return CellGeometry(Path().apply {
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
    }, points)
}

@Composable
fun LumiLivingCore(
    modifier: Modifier = Modifier,
    currentDay: Int,
    currentPhase: CyclePhase,
    phaseName: String,
    estimatedDaysText: String,
    confidenceLabel: String,
    dayContextLabel: String = dayContextLabel,
    onConfidenceClick: (() -> Unit)? = null
) {
    val tokens = LumiThemeTokens.current
    val colors = remember(currentPhase) { cellPalette(currentPhase) }
    val a by animateColorAsState(colors.first, tween(1700), label = "lumi_skin_a")
    val b by animateColorAsState(colors.second, tween(1700), label = "lumi_skin_b")
    val c by animateColorAsState(colors.third, tween(1700), label = "lumi_skin_c")
    val d by animateColorAsState(colors.fourth, tween(1700), label = "lumi_skin_d")

    // Continuous time avoids the old 24s reset of non-periodic particle/light motion.
    // Read time only while drawing: labels and layout do not recompose every frame.
    val time = remember { mutableFloatStateOf(0f) }
    val breathTime = remember { mutableFloatStateOf(0f) }
    val pace = rememberUpdatedState(colors.pace)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val motionScale = coroutineContext[MotionDurationScale]
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            snapshotFlow {
                motionScale?.scaleFactor ?: if (ValueAnimator.areAnimatorsEnabled()) 1f else 0f
            }.collectLatest { durationScale ->
                if (durationScale > 0f) {
                    var previousFrame = 0L
                    while (isActive) {
                        withFrameNanos { frame ->
                            if (previousFrame != 0L) {
                                val delta = ((frame - previousFrame) / 1_000_000_000f)
                                    .coerceAtMost(.048f) / durationScale
                                time.floatValue += delta
                                breathTime.floatValue += delta * pace.value
                            }
                            previousFrame = frame
                        }
                    }
                }
            }
        }
    }
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
                modifier = Modifier.size(width = diameter, height = diameter * .86f)
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
                        if (ValueAnimator.areAnimatorsEnabled()) scope.launch {
                            pulse.snapTo(1f)
                            pulse.animateTo(0f, tween(850, easing = LinearEasing))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val ratio = size.width / 400f
                    val center = Offset(200f, 200f)
                    val palette = listOf(a, b, c, d)
                    val seconds = time.floatValue
                    val t = seconds * TWO_PI / 24f
                    val geometry = organGeometry(seconds, breathTime.floatValue, pulse.value)
                    val shape = geometry.path
                    val mintCenter = Offset(238f + 5f * sin(t), 247f)
                    val roseCenter = Offset(158f - 5f * sin(t * .6f), 214f)
                    val highlightAlpha = if (tokens.isDark) .72f else .48f
                    val depthAlpha = if (tokens.isDark) .11f else .17f

                    withTransform({
                        translate(top = (size.height - size.width) / 2f)
                        scale(ratio, ratio, pivot = Offset.Zero)
                    }) {
                        withTransform({ scale(CORE_SCALE, CORE_SCALE, pivot = center) }) {
                            if (!tokens.isDark) {
                                // Contact shading follows the silhouette, never a detached oval below it.
                                withTransform({ translate(top = 3f) }) {
                                    for (width in 32 downTo 2 step 2) {
                                        drawPath(shape, ink.copy(alpha = .003f), style = Stroke(width.toFloat()))
                                    }
                                }
                            }
                            // Overlapping soft lights inherit the nearest edge, including liquid drift.
                            for (i in geometry.edge.indices step 3) {
                                val point = geometry.edge[i]
                                val surface = surfaceColorAt(
                                    point, palette, mintCenter, roseCenter, highlightAlpha, depthAlpha
                                )
                                val tint = if (tokens.isDark) surface else lerp(surface, ink, .14f)
                                val strength = if (tokens.isDark) .13f else .20f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0f to tint.copy(alpha = strength),
                                        .35f to tint.copy(alpha = strength * .55f),
                                        .7f to tint.copy(alpha = strength * .12f),
                                        1f to tint.copy(alpha = 0f),
                                        center = point, radius = 38f
                                    ),
                                    center = point, radius = 38f
                                )
                            }
                        }

                        // Stationary dotted orbital reference, very faint; never a progress ring.
                        drawCircle(
                            color = tokens.accent.copy(alpha = if (tokens.isDark) .22f else .07f),
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
                            withTransform({ scale(1.14f, 1.14f, pivot = center) }) {
                                drawPath(
                                    shape,
                                    if (tokens.isDark) Color.White.copy(alpha = .26f)
                                    else tokens.accent.copy(alpha = .18f),
                                    style = Stroke(width = .9f / 1.14f)
                                )
                            }
                            drawPath(
                                shape,
                                brush = Brush.linearGradient(
                                    0f to a, .33f to b, .67f to c, 1f to d,
                                    start = SKIN_START, end = SKIN_END
                                )
                            )
                            clipPath(shape) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color.White.copy(alpha = highlightAlpha), Color.White.copy(alpha = .12f), Color.Transparent),
                                        center = Offset(152f, 143f), radius = 109f
                                    ),
                                    center = Offset(152f, 143f), radius = 109f
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(d.copy(alpha = .49f), Color.Transparent),
                                        center = mintCenter, radius = 88f
                                    ), center = mintCenter, radius = 88f
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(b.copy(alpha = .52f), Color.Transparent),
                                        center = roseCenter, radius = 100f
                                    ), center = roseCenter, radius = 100f
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0xFF6D447E).copy(alpha = depthAlpha), Color.Transparent),
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
                                withTransform({ scale(.77f, .77f, pivot = center) }) {
                                    drawPath(
                                        shape, Color.White.copy(alpha = .17f),
                                        style = Stroke(width = .7f / .77f)
                                    )
                                }
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
                        fontSize = 68.sp,
                        lineHeight = 74.sp,
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
        val confidenceModifier = if (onConfidenceClick != null) {
            Modifier.clickable(
                role = Role.Button,
                onClickLabel = confidenceLabel,
                onClick = onConfidenceClick
            )
        } else Modifier
        // Keep calibration attached to the estimate, with one accessible touch target.
        Column(
            modifier = confidenceModifier
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
        ) {
            Text(
                estimatedDaysText,
                color = tokens.muted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LumiIconInfo(tint = tokens.dim, size = 12.dp)
                Text(
                    confidenceLabel,
                    color = tokens.dim,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
