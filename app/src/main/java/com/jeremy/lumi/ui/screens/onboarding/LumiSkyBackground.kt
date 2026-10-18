package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Estrella individual con coordenadas normalizadas (0..1) y propiedades estéticas.
 */
data class StarDot(
    val xNorm: Float,
    val yNorm: Float,
    val sizePx: Float,
    val alpha: Float,
    val isLavender: Boolean,
    val isTwinkle: Boolean
)

/**
 * Generador pseudoaleatorio con semilla determinista (4409) para garantizar
 * 100% consistencia con el prototipo HTML v7 en cualquier ejecución o dispositivo.
 */
private class LcgRandom(seed: Long) {
    private var state = seed and 0xFFFFFFFFL
    fun nextFloat(): Float {
        state = (state * 1664525L + 1013904223L) and 0xFFFFFFFFL
        return state.toFloat() / 4294967296.0f
    }
}

private fun generateStars(count: Int, minSize: Float, maxSize: Float, minAlpha: Float, maxAlpha: Float, rand: LcgRandom): List<StarDot> {
    val list = ArrayList<StarDot>(count)
    for (i in 0 until count) {
        val x = rand.nextFloat()
        val y = rand.nextFloat()
        val sz = minSize + rand.nextFloat() * (maxSize - minSize)
        val a = minAlpha + rand.nextFloat() * (maxAlpha - minAlpha)
        list.add(
            StarDot(
                xNorm = x,
                yNorm = y,
                sizePx = sz,
                alpha = a,
                isLavender = (i % 11 == 0),
                isTwinkle = (i % 9 == 0)
            )
        )
    }
    return list
}

/**
 * Fondo de cielo nocturno oficial de Lumi Onboarding v7:
 * - Resplandor ambiental de base en temas oscuros (skyA -> skyB)
 * - 3 planos de estrellas ascendentes continuas e infinitas (low 44s, mid 73s, high 116s)
 * - Apagado automático (100% transparente) en temas claros para no ensuciar la superficie
 * - Intensidad adaptativa según paso (más visible en 0 y 7, sutil en 1..6)
 * - Desactivación de movimiento cuando se activa reducedMotion
 */
@Composable
fun LumiSkyBackground(
    stepNumber: Int,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false
) {
    val tokens = LumiOnboardingThemeTokens.current
    if (!tokens.isDark) return // En temas claros el cielo nocturno está 100% apagado

    // Generar estrellas de las 3 capas una sola vez con semilla determinista 4409
    val (lowStars, midStars, highStars) = remember {
        val rand = LcgRandom(4409L)
        Triple(
            generateStars(count = 37, minSize = 1.6f, maxSize = 2.8f, minAlpha = 0.38f, maxAlpha = 0.64f, rand = rand),
            generateStars(count = 28, minSize = 2.0f, maxSize = 3.6f, minAlpha = 0.40f, maxAlpha = 0.70f, rand = rand),
            generateStars(count = 18, minSize = 2.4f, maxSize = 4.4f, minAlpha = 0.38f, maxAlpha = 0.65f, rand = rand)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "lumi_sky_motion")

    val lowProgress = if (!reducedMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 44000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sky_low_drift"
        ).value
    } else 0f

    val midProgress = if (!reducedMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 73000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sky_mid_drift"
        ).value
    } else 0f

    val highProgress = if (!reducedMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 116000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sky_high_drift"
        ).value
    } else 0f

    val twinklePulse = if (!reducedMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "sky_twinkle"
        ).value
    } else 1.0f

    val stepMultiplier = if (stepNumber == 0 || stepNumber == 7) 1.08f else 0.75f

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Luz ambiental de fondo (skyA -> skyB -> transparent)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    tokens.skyA.copy(alpha = (tokens.skyA.alpha * stepMultiplier).coerceIn(0f, 1f)),
                    tokens.skyB.copy(alpha = (tokens.skyB.alpha * stepMultiplier).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 1.05f),
                radius = w * 1.05f
            ),
            size = size
        )

        // 2. Dibujar capa de estrellas baja (low)
        drawStarLayer(lowStars, lowProgress, w, h, stepMultiplier, twinklePulse, baseOpacity = 0.70f)

        // 3. Dibujar capa de estrellas media (mid)
        drawStarLayer(midStars, midProgress, w, h, stepMultiplier, twinklePulse, baseOpacity = 0.67f)

        // 4. Dibujar capa de estrellas alta (high)
        drawStarLayer(highStars, highProgress, w, h, stepMultiplier, twinklePulse, baseOpacity = 0.40f)

        // 5. Scrim sutil superior e inferior
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0x2E0E0C13),
                0.25f to Color.Transparent,
                0.80f to Color.Transparent,
                1.0f to Color(0x280E0C13)
            ),
            size = size
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarLayer(
    stars: List<StarDot>,
    progress: Float,
    width: Float,
    height: Float,
    stepMultiplier: Float,
    twinklePulse: Float,
    baseOpacity: Float
) {
    for (star in stars) {
        val x = star.xNorm * width
        // Desplazamiento continuo vertical hacia arriba (skyRise)
        val rawY = star.yNorm - progress
        val wrappedY = ((rawY % 1f + 1f) % 1f) * height

        val twinkleFactor = if (star.isTwinkle) twinklePulse else 1f
        val finalAlpha = (star.alpha * baseOpacity * stepMultiplier * twinkleFactor).coerceIn(0f, 1f)
        val color = if (star.isLavender) Color(0xFFEED4FB) else Color(0xFFFFF9FC)

        drawCircle(
            color = color.copy(alpha = finalAlpha),
            radius = star.sizePx * 0.5f,
            center = Offset(x, wrappedY)
        )
    }
}
