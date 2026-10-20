package com.jeremy.lumi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.jeremy.lumi.ui.theme.AmbientScreen
import com.jeremy.lumi.ui.theme.BackgroundAmbient
import com.jeremy.lumi.ui.theme.LocalBackgroundAmbient
import com.jeremy.lumi.ui.theme.LocalBrandBackgroundGradient
import com.jeremy.lumi.ui.theme.LocalIsDarkTheme
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import androidx.compose.ui.graphics.drawscope.withTransform
import java.time.LocalDate
import kotlin.math.floor
import kotlin.random.Random

/**
 * Capa decorativa viva y reactiva de fondo — se coloca DEBAJO del Scaffold en un Box.
 * Soporta modo Plano (FLAT) y Celeste (CELESTIAL).
 *
 * @param screenType Densidad de contenido de la pantalla — controla la intensidad del efecto.
 * @param accentColor Color de acento opcional.
 * @param scrollOffsetProvider Proveedor lambda del desplazamiento de scroll en píxeles.
 */
@Composable
fun AmbientBackgroundLayer(
    screenType: AmbientScreen = AmbientScreen.HOME,
    accentColor: Color? = null,
    scrollOffsetProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val ambient          = LocalBackgroundAmbient.current
    val isDark           = LocalIsDarkTheme.current
    val brandBgGradient  = LocalBrandBackgroundGradient.current

    // El CSS de Lumi dibuja .ambient CON INDEPENDENCIA de Celestial/Flat.
    // Antes, FLAT sin gradiente de marca no dibujaba nada (caso NOIR).
    Box(modifier = modifier) {
        if (ambient == BackgroundAmbient.FLAT && brandBgGradient != null) {
            Box(Modifier.fillMaxSize().background(brandBgGradient))
        }
        if (screenType == AmbientScreen.HOME) {
            LumiHomeCssAmbient(Modifier.fillMaxSize())
        }
        if (ambient == BackgroundAmbient.CELESTIAL) {
            CelestialLayer(
                isDark = isDark,
                screenType = screenType,
                scrollOffsetProvider = scrollOffsetProvider,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Calco de la capa .ambient del Home HTML v12 (no la escena de estrellas):
 *  ellipse 70% 28% at 23% 14%, var(--glow), transparent;
 *  ellipse 95% 28% at 92% 53%, color-mix(accent 9%,transparent), transparent;
 *  opacity: .65.
 * En Compose cada óvalo conserva su radio XY sin añadir una "nebulosa" artificial.
 */
@Composable
private fun LumiHomeCssAmbient(modifier: Modifier = Modifier) {
    val tokens = LumiThemeTokens.current
    Canvas(modifier) {
        fun ellipseGlow(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
            if (rx <= 0f || ry <= 0f || color.alpha <= 0f) return
            val center = Offset(size.width * cx, size.height * cy)
            val radius = (rx * size.width).coerceAtLeast(1f)
            val radiusY = (ry * size.height).coerceAtLeast(1f)
            withTransform({ scale(scaleX = 1f, scaleY = radiusY / radius, pivot = center) }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to color,
                        1f to Color.Transparent,
                        center = center,
                        radius = radius
                    ),
                    center = center,
                    radius = radius
                )
            }
        }
        ellipseGlow(.23f, .14f, .70f, .28f,
            tokens.glow.copy(alpha = tokens.glow.alpha * .65f))
        ellipseGlow(.92f, .53f, .95f, .28f,
            tokens.accent.copy(alpha = .09f * .65f))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  CELESTIAL — Cielo Estrellado Titilante + Halo Lunar + Parallax
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun CelestialLayer(
    isDark: Boolean,
    screenType: AmbientScreen,
    scrollOffsetProvider: () -> Float,
    modifier: Modifier
) {
    val today          = remember { LocalDate.now() }
    val moonPhaseIndex = remember(today) { calculateMoonPhaseIndex(today) }

    // Posiciones de estrellas estables por mes (52 estrellas)
    val starPositions = remember(today.year, today.monthValue) {
        val rng = Random(today.year * 100 + today.monthValue)
        List(52) { Pair(rng.nextFloat(), rng.nextFloat()) }
    }

    val transition = rememberInfiniteTransition(label = "celestial_motion")

    // 3 fases desincronizadas para titileo estelar natural (twinkle)
    val twinkleA by transition.animateFloat(
        initialValue  = 0.25f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle_a"
    )

    val twinkleB by transition.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle_b"
    )

    val twinkleC by transition.animateFloat(
        initialValue  = 0.20f,
        targetValue   = 0.95f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle_c"
    )

    // Halo lunar pulsante suave
    val moonGlowScale by transition.animateFloat(
        initialValue  = 0.92f,
        targetValue   = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(5400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moon_glow"
    )

    val moonFloatY by transition.animateFloat(
        initialValue  = -3f,
        targetValue   = 3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(7800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moon_float_y"
    )

    val moonColor     = if (isDark) Color(0xFFE8E0D0) else Color(0xFFB0A898)
    val bgColor       = MaterialTheme.colorScheme.background
    val starAlphaMax  = if (isDark) 0.52f else 0.12f
    val moonAlpha     = if (isDark) 0.78f else 0.25f

    Canvas(modifier = modifier) {
        val parallaxY = scrollOffsetProvider() * 0.10f // Parallax sutil cósmico

        // Dibujar estrellas con titileo orgánico desfasado
        val starRng = Random(today.year * 100 + today.monthValue + 7)
        starPositions.forEachIndexed { index, (fx, fy) ->
            val baseStarAlpha = starRng.nextFloat() * (starAlphaMax - 0.08f) + 0.08f
            val twinkleMultiplier = when (index % 3) {
                0    -> twinkleA
                1    -> twinkleB
                else -> twinkleC
            }
            val radius = (starRng.nextFloat() * 1.6f + 0.5f).dp.toPx()
            val starY = size.height * (fy * 0.65f) - parallaxY

            if (starY in -10f..size.height) {
                drawCircle(
                    color  = Color.White.copy(alpha = (baseStarAlpha * twinkleMultiplier).coerceIn(0f, 1f)),
                    radius = radius,
                    center = Offset(size.width * fx, starY)
                )
            }
        }

        // Luna — esquina superior derecha con flotación y halo de resplandor
        val moonRadius = 22.dp.toPx()
        val moonCenter = Offset(
            size.width - 36.dp.toPx(),
            90.dp.toPx() + moonFloatY.dp.toPx() - parallaxY
        )

        // Resplandor difuso detrás de la luna
        val glowRadius = moonRadius * 2.1f * moonGlowScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    moonColor.copy(alpha = if (isDark) 0.18f else 0.07f),
                    Color.Transparent
                ),
                center = moonCenter,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = moonCenter
        )

        if (isDark) {
            drawMoonPhase(
                center     = moonCenter,
                radius     = moonRadius,
                phaseIndex = moonPhaseIndex,
                moonColor  = moonColor.copy(alpha = moonAlpha),
                bgColor    = bgColor
            )
        } else {
            // Temas claros: contorno circular sutil
            drawCircle(
                color  = moonColor.copy(alpha = moonAlpha),
                radius = moonRadius,
                center = moonCenter,
                style  = Stroke(width = 1.5f)
            )
        }
    }
}

/**
 * Índice de fase lunar (0–7) para una fecha usando la fórmula de Jean Meeus.
 */
private fun calculateMoonPhaseIndex(date: LocalDate): Int {
    val y  = date.year.toDouble()
    val m  = date.monthValue.toDouble()
    val d  = date.dayOfMonth.toDouble()
    val jd = 367.0 * y -
            floor(7.0 * (y + floor((m + 9.0) / 12.0)) / 4.0) +
            floor(275.0 * m / 9.0) +
            d + 1721013.5
    val daysSinceNew = (jd - 2451549.5).mod(29.53058853)
    return floor(daysSinceNew / 29.53058853 * 8.0).toInt().coerceIn(0, 7)
}

/**
 * Dibuja la fase lunar mediante máscara de recorte y proyección esférica.
 */
private fun DrawScope.drawMoonPhase(
    center: Offset, radius: Float, phaseIndex: Int,
    moonColor: Color, bgColor: Color
) {
    val moonPath = Path().apply {
        addOval(
            Rect(
                left   = center.x - radius,
                top    = center.y - radius,
                right  = center.x + radius,
                bottom = center.y + radius
            )
        )
    }

    clipPath(moonPath) {
        drawCircle(moonColor, radius, center)

        when (phaseIndex) {
            0 -> {
                // Luna nueva
                drawCircle(bgColor, radius, center)
                drawCircle(
                    color  = moonColor.copy(alpha = 0.18f),
                    radius = radius,
                    center = center,
                    style  = Stroke(width = 1.5f)
                )
            }
            4 -> Unit // Luna llena
            else -> {
                val isWaxing = phaseIndex in 1..3
                val shadowMultiplier = when (phaseIndex) {
                    1, 7 -> 0.10f
                    2, 6 -> 1.00f
                    3, 5 -> 1.90f
                    else -> 1.00f
                }
                val shadowCenterX = if (isWaxing) {
                    center.x - radius * shadowMultiplier
                } else {
                    center.x + radius * shadowMultiplier
                }
                drawCircle(bgColor, radius, Offset(shadowCenterX, center.y))
            }
        }
    }
}
