package com.jeremy.lumi.ui.screens.partner

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.jeremy.lumi.domain.model.CareAction
import kotlin.math.sin
import kotlin.random.Random

/**
 * Capa de animaciones para los "Detalles" (Care Actions).
 *
 * Todo se dibuja con Compose Canvas: vectores puros, sin emojis ni imágenes.
 * Cada acción tiene su propia coreografía y paleta:
 *  - HUG          → corazones que flotan hacia arriba con deriva suave
 *  - TEA / COFFEE → volutas de vapor cálido ascendiendo
 *  - CHOCOLATE    → onzas redondeadas cayendo con giro
 *  - HEATING_PAD  → ondas de calor concéntricas desde abajo
 *  - PHARMACY     → cruces que pulsan mientras se elevan
 *
 * Uso: `val state = rememberCareReactionState()` en el contenedor,
 * `state.fire(action)` al enviar/recibir un detalle, y
 * `CareReactionOverlay(state)` como capa superior del Box.
 */
class CareReactionState {
    internal var event by mutableStateOf<CareReactionEvent?>(null)

    /** Dispara la animación correspondiente a la acción. */
    fun fire(action: CareAction) {
        event = CareReactionEvent(action, System.nanoTime())
    }
}

internal data class CareReactionEvent(val action: CareAction, val id: Long)

@Composable
fun rememberCareReactionState(): CareReactionState = remember { CareReactionState() }

private const val PARTICLE_COUNT = 22
private const val DURATION_MS = 2600

/** Parámetros aleatorios pero estables de cada partícula de la coreografía. */
private class Particle(random: Random) {
    val x: Float = random.nextFloat()                        // fracción del ancho
    val delay: Float = random.nextFloat() * 0.35f            // arranque escalonado
    val scale: Float = 0.6f + random.nextFloat() * 0.9f
    val drift: Float = (random.nextFloat() - 0.5f) * 0.25f   // deriva horizontal
    val spin: Float = (random.nextFloat() - 0.5f) * 220f     // grados de giro total
    val tint: Float = random.nextFloat()                     // mezcla de paleta
}

@Composable
fun CareReactionOverlay(state: CareReactionState, modifier: Modifier = Modifier) {
    val event = state.event ?: return
    val progress = remember(event.id) { Animatable(0f) }
    val particles = remember(event.id) {
        val random = Random(event.id)
        List(PARTICLE_COUNT) { Particle(random) }
    }

    LaunchedEffect(event.id) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        state.event = null
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        particles.forEach { particle ->
            // Progreso propio de la partícula, respetando su retraso de entrada
            val local = ((t - particle.delay) / (1f - particle.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            when (event.action) {
                CareAction.HUG -> drawHeart(particle, local)
                CareAction.TEA, CareAction.COFFEE -> drawSteam(particle, local)
                CareAction.CHOCOLATE -> drawChocolate(particle, local)
                CareAction.HEATING_PAD -> drawWarmWave(particle, local)
                CareAction.PHARMACY -> drawCross(particle, local)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Coreografías por acción
// ─────────────────────────────────────────────────────────────────────────────

/** Opacidad con entrada y salida suaves. */
private fun fade(local: Float): Float = when {
    local < 0.15f -> local / 0.15f
    local > 0.75f -> (1f - local) / 0.25f
    else -> 1f
}.coerceIn(0f, 1f)

private fun DrawScope.drawHeart(p: Particle, local: Float) {
    val palette = listOf(Color(0xFFFF6B9D), Color(0xFFE84393), Color(0xFFFF8FA3))
    val color = palette[(p.tint * palette.size).toInt().coerceAtMost(palette.size - 1)]
    val cx = size.width * (p.x + p.drift * local) + sin(local * 6f + p.spin) * 18f
    val cy = size.height * (1.05f - local * 1.1f)
    val s = 34f * p.scale * (0.7f + 0.3f * local)

    val path = Path().apply {
        moveTo(cx, cy + s * 0.28f)
        cubicTo(cx + s * 0.55f, cy - s * 0.18f, cx + s * 0.42f, cy - s * 0.62f, cx, cy - s * 0.28f)
        cubicTo(cx - s * 0.42f, cy - s * 0.62f, cx - s * 0.55f, cy - s * 0.18f, cx, cy + s * 0.28f)
        close()
    }
    rotate(degrees = p.spin * local * 0.15f, pivot = Offset(cx, cy)) {
        drawPath(path, color = color.copy(alpha = fade(local) * 0.9f))
    }
}

private fun DrawScope.drawSteam(p: Particle, local: Float) {
    val palette = listOf(Color(0xFFFFB75E), Color(0xFFD4A373), Color(0xFFB5E48C))
    val color = palette[(p.tint * palette.size).toInt().coerceAtMost(palette.size - 1)]
    val cx = size.width * (0.3f + p.x * 0.4f) + sin(local * 9f + p.spin) * 26f * p.scale
    val cy = size.height * (0.95f - local * 0.85f)
    val radius = 16f * p.scale * (0.5f + local)

    // Voluta: círculo difuso + arco fino que sugiere el rizo del vapor
    drawCircle(color.copy(alpha = fade(local) * 0.35f), radius = radius, center = Offset(cx, cy))
    drawArc(
        color = color.copy(alpha = fade(local) * 0.55f),
        startAngle = p.spin,
        sweepAngle = 200f,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 3f * p.scale)
    )
}

private fun DrawScope.drawChocolate(p: Particle, local: Float) {
    val palette = listOf(Color(0xFF6F4E37), Color(0xFF8B5E3C), Color(0xFF4E342E))
    val color = palette[(p.tint * palette.size).toInt().coerceAtMost(palette.size - 1)]
    val cx = size.width * (p.x + p.drift * local)
    val cy = size.height * (local * 1.1f - 0.05f)
    val s = 30f * p.scale

    rotate(degrees = p.spin * local, pivot = Offset(cx, cy)) {
        drawRoundRect(
            color = color.copy(alpha = fade(local)),
            topLeft = Offset(cx - s / 2, cy - s / 2),
            size = Size(s, s * 0.72f),
            cornerRadius = CornerRadius(6f * p.scale)
        )
        // Surco central de la onza de chocolate
        drawLine(
            color = Color.Black.copy(alpha = fade(local) * 0.25f),
            start = Offset(cx - s * 0.3f, cy),
            end = Offset(cx + s * 0.3f, cy),
            strokeWidth = 2.5f * p.scale
        )
    }
}

private fun DrawScope.drawWarmWave(p: Particle, local: Float) {
    val palette = listOf(Color(0xFFFF7E5F), Color(0xFFFFA07A), Color(0xFFEB3349))
    val color = palette[(p.tint * palette.size).toInt().coerceAtMost(palette.size - 1)]
    val cx = size.width * 0.5f
    val cy = size.height * 0.92f
    val radius = (120f + p.x * 240f) * (0.4f + local * 1.4f) * p.scale

    // Ondas de calor: arcos concéntricos que se expanden y se desvanecen
    drawArc(
        color = color.copy(alpha = fade(local) * 0.4f),
        startAngle = 195f + p.drift * 40f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 8f * p.scale)
    )
}

private fun DrawScope.drawCross(p: Particle, local: Float) {
    val palette = listOf(Color(0xFF38B2AC), Color(0xFF2F80ED), Color(0xFF43E97B))
    val color = palette[(p.tint * palette.size).toInt().coerceAtMost(palette.size - 1)]
    val cx = size.width * (p.x + p.drift * local)
    val cy = size.height * (1.0f - local)
    val s = 26f * p.scale * (0.8f + 0.4f * sin(local * 8f))
    val arm = s * 0.34f

    rotate(degrees = p.spin * local * 0.2f, pivot = Offset(cx, cy)) {
        drawRoundRect(
            color = color.copy(alpha = fade(local) * 0.85f),
            topLeft = Offset(cx - arm / 2, cy - s / 2),
            size = Size(arm, s),
            cornerRadius = CornerRadius(arm / 2)
        )
        drawRoundRect(
            color = color.copy(alpha = fade(local) * 0.85f),
            topLeft = Offset(cx - s / 2, cy - arm / 2),
            size = Size(s, arm),
            cornerRadius = CornerRadius(arm / 2)
        )
    }
}
