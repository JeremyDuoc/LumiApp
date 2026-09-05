package com.jeremy.lumi.ui.components

import androidx.compose.animation.animateColorAsState
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
import com.jeremy.lumi.ui.theme.LocalSemanticPhaseColors
import com.jeremy.lumi.ui.theme.PhaseColors
import java.time.LocalDate
import kotlin.math.floor
import kotlin.random.Random

/**
 * Capa decorativa viva y reactiva de fondo — se coloca DEBAJO del Scaffold en un Box.
 *
 * Cuenta con animaciones bio-rítmicas sutiles (respiración, pulso, deriva flotante, titileo)
 * y respuesta de desplazamiento parallax con el scroll para una sensación de profundidad 3D.
 *
 * @param screenType Densidad de contenido de la pantalla — controla la intensidad del efecto.
 * @param accentColor Color de acento para el modo AURA. Si es null, usa [MaterialTheme.colorScheme.primary].
 * @param scrollOffsetProvider Proveedor lambda del desplazamiento de scroll en píxeles (para parallax sin recomposición).
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
    val semanticColors   = LocalSemanticPhaseColors.current
    val brandBgGradient  = LocalBrandBackgroundGradient.current

    when (ambient) {
        BackgroundAmbient.FLAT -> {
            // Fondo plano: minimalista, estático, cero distracción o consumo de GPU
            if (brandBgGradient != null) {
                Box(
                    modifier = modifier
                        .background(MaterialTheme.colorScheme.background)
                        .background(brandBgGradient)
                )
            }
        }

        BackgroundAmbient.AURA -> {
            if (screenType != AmbientScreen.CHAT) {
                AuraLayer(
                    accentColor          = accentColor ?: MaterialTheme.colorScheme.primary,
                    isDark               = isDark,
                    screenType           = screenType,
                    scrollOffsetProvider = scrollOffsetProvider,
                    modifier             = modifier
                )
            }
        }

        BackgroundAmbient.CELESTIAL -> {
            CelestialLayer(
                isDark               = isDark,
                screenType           = screenType,
                scrollOffsetProvider = scrollOffsetProvider,
                modifier             = modifier
            )
        }

        BackgroundAmbient.ORGANIC -> {
            if (screenType != AmbientScreen.CHAT) {
                OrganicLayer(
                    phaseColors          = semanticColors,
                    isDark               = isDark,
                    screenType           = screenType,
                    scrollOffsetProvider = scrollOffsetProvider,
                    modifier             = modifier
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  AURA — Respiración Bio-rítmica + Deriva Flotante + Parallax
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AuraLayer(
    accentColor: Color,
    isDark: Boolean,
    screenType: AmbientScreen,
    scrollOffsetProvider: () -> Float,
    modifier: Modifier
) {
    val baseAlpha = when (screenType) {
        AmbientScreen.HOME    -> if (isDark) 0.28f else 0.25f
        AmbientScreen.CONTENT -> if (isDark) 0.14f else 0.13f
        AmbientScreen.CHAT    -> return
    }

    // Transición animada suave cuando cambia la fase del ciclo
    val animatedColor by animateColorAsState(
        targetValue   = accentColor,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label         = "aura_color"
    )

    // Animaciones continuas infinitas: respiración (5.8s), pulso de luz (6.4s) y flotación orgánica (8.6s / 11.2s)
    val transition = rememberInfiniteTransition(label = "aura_motion")

    val pulseScale by transition.animateFloat(
        initialValue  = 0.93f,
        targetValue   = 1.07f,
        animationSpec = infiniteRepeatable(
            animation  = tween(5800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_scale"
    )

    val pulseAlpha by transition.animateFloat(
        initialValue  = 0.88f,
        targetValue   = 1.12f,
        animationSpec = infiniteRepeatable(
            animation  = tween(6400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    val floatOffsetY by transition.animateFloat(
        initialValue  = -10f,
        targetValue   = 10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(8600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_float_y"
    )

    val floatOffsetX by transition.animateFloat(
        initialValue  = -6f,
        targetValue   = 6f,
        animationSpec = infiniteRepeatable(
            animation  = tween(11200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_float_x"
    )

    Canvas(modifier = modifier) {
        val parallaxY = scrollOffsetProvider() * 0.18f
        val baseY = when (screenType) {
            AmbientScreen.HOME    -> size.height * 0.42f
            AmbientScreen.CONTENT -> size.height * 0.10f
            AmbientScreen.CHAT    -> return@Canvas
        }

        val centerX = size.width / 2f + floatOffsetX.dp.toPx()
        val centerY = baseY + floatOffsetY.dp.toPx() - parallaxY
        val auraRadius = size.width * 0.72f * pulseScale
        val finalAlpha = (baseAlpha * pulseAlpha).coerceIn(0f, 1f)

        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to animatedColor.copy(alpha = finalAlpha),
                    0.45f to animatedColor.copy(alpha = finalAlpha * 0.55f),
                    0.78f to animatedColor.copy(alpha = finalAlpha * 0.18f),
                    1.00f to Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = auraRadius
            ),
            radius = auraRadius,
            center = Offset(centerX, centerY)
        )
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

// ──────────────────────────────────────────────────────────────────────────────
//  ORGANIC — Blobs Fluidos con Deriva Elíptica y Morfología Líquida
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun OrganicLayer(
    phaseColors: PhaseColors,
    isDark: Boolean,
    screenType: AmbientScreen,
    scrollOffsetProvider: () -> Float,
    modifier: Modifier
) {
    val baseAlpha = when (screenType) {
        AmbientScreen.HOME    -> if (isDark) 0.30f else 0.28f
        AmbientScreen.CONTENT -> if (isDark) 0.14f else 0.14f
        AmbientScreen.CHAT    -> return
    }

    val transition = rememberInfiniteTransition(label = "organic_liquid_motion")

    // Respiración asimétrica de los blobs
    val breatheA by transition.animateFloat(
        initialValue  = 0.90f,
        targetValue   = 1.10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(7400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob_a_scale"
    )
    val breatheB by transition.animateFloat(
        initialValue  = 1.06f,
        targetValue   = 0.94f,
        animationSpec = infiniteRepeatable(
            animation  = tween(9800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob_b_scale"
    )

    // Deriva flotante en 2D (movimiento elíptico continuo)
    val driftAx by transition.animateFloat(
        initialValue  = -16f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(8800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift_ax"
    )
    val driftAy by transition.animateFloat(
        initialValue  = -12f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(7600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift_ay"
    )

    val driftBx by transition.animateFloat(
        initialValue  = 14f, targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(10200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift_bx"
    )
    val driftBy by transition.animateFloat(
        initialValue  = -14f, targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(8400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift_by"
    )

    val driftCx by transition.animateFloat(
        initialValue  = -12f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift_cx"
    )
    val driftCy by transition.animateFloat(
        initialValue  = 10f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(9200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift_cy"
    )

    Canvas(modifier = modifier) {
        val parallaxY = scrollOffsetProvider() * 0.22f // Profundidad orgánica

        val blobA = size.width * 0.55f * breatheA
        val blobB = size.width * 0.42f * breatheB
        val blobC = size.width * 0.36f * breatheA * 0.88f

        val centerA = Offset(
            -size.width * 0.10f + driftAx.dp.toPx(),
            size.height * 0.07f + driftAy.dp.toPx() - parallaxY
        )
        val centerB = Offset(
            size.width * 1.08f + driftBx.dp.toPx(),
            size.height * 0.12f + driftBy.dp.toPx() - parallaxY
        )
        val centerC = Offset(
            size.width * 1.12f + driftCx.dp.toPx(),
            size.height * 0.82f + driftCy.dp.toPx() - parallaxY
        )

        // Blob A — Menstrual (esquina superior izquierda)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    phaseColors.menstrual.copy(alpha = baseAlpha),
                    phaseColors.menstrual.copy(alpha = baseAlpha * 0.40f),
                    Color.Transparent
                ),
                center = centerA,
                radius = blobA
            ),
            radius = blobA,
            center = centerA
        )

        // Blob B — Folicular (esquina superior derecha)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    phaseColors.follicular.copy(alpha = baseAlpha * 0.78f),
                    phaseColors.follicular.copy(alpha = baseAlpha * 0.28f),
                    Color.Transparent
                ),
                center = centerB,
                radius = blobB
            ),
            radius = blobB,
            center = centerB
        )

        // Blob C — Lútea (esquina inferior derecha) — en HOME
        if (screenType == AmbientScreen.HOME) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        phaseColors.luteal.copy(alpha = baseAlpha * 0.62f),
                        phaseColors.luteal.copy(alpha = baseAlpha * 0.18f),
                        Color.Transparent
                    ),
                    center = centerC,
                    radius = blobC
                ),
                radius = blobC,
                center = centerC
            )
        }
    }
}
