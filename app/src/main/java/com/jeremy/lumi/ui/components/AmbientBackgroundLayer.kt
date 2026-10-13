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
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import com.jeremy.lumi.ui.theme.PhaseColors
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.sin
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
    val phaseColors      = LocalPhaseColors.current
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
                    phaseColors          = phaseColors,
                    isDark               = isDark,
                    screenType           = screenType,
                    scrollOffsetProvider = scrollOffsetProvider,
                    modifier             = modifier
                )
            }
        }

        BackgroundAmbient.AURORA -> {
            if (screenType != AmbientScreen.CHAT) {
                AuroraLayer(
                    isDark               = isDark,
                    screenType           = screenType,
                    scrollOffsetProvider = scrollOffsetProvider,
                    modifier             = modifier
                )
            }
        }

        BackgroundAmbient.PETALS -> {
            if (screenType != AmbientScreen.CHAT) {
                PetalsLayer(
                    isDark               = isDark,
                    screenType           = screenType,
                    scrollOffsetProvider = scrollOffsetProvider,
                    modifier             = modifier
                )
            }
        }

        BackgroundAmbient.NEBULA -> {
            if (screenType != AmbientScreen.CHAT) {
                NebulaLayer(
                    isDark               = isDark,
                    screenType           = screenType,
                    scrollOffsetProvider = scrollOffsetProvider,
                    modifier             = modifier
                )
            }
        }

        BackgroundAmbient.BREATH -> {
            if (screenType != AmbientScreen.CHAT) {
                BreathLayer(
                    accentColor          = accentColor ?: MaterialTheme.colorScheme.primary,
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

// ──────────────────────────────────────────────────────────────────────────────
//  AURORA — Ondas Horizontales Estilo Aurora Boreal + Parallax
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AuroraLayer(
    isDark: Boolean,
    screenType: AmbientScreen,
    scrollOffsetProvider: () -> Float,
    modifier: Modifier
) {
    val baseAlpha = when (screenType) {
        AmbientScreen.HOME    -> if (isDark) 0.32f else 0.18f
        AmbientScreen.CONTENT -> if (isDark) 0.16f else 0.09f
        AmbientScreen.CHAT    -> return
    }

    // Colores de aurora — vibrantes en oscuro, pastel en claro
    val colorA = if (isDark) Color(0xFF00E5A0) else Color(0xFF80D4B8) // Verde teal
    val colorB = if (isDark) Color(0xFF00B0FF) else Color(0xFFAAD8F0) // Cian
    val colorC = if (isDark) Color(0xFFB07CF0) else Color(0xFFD0B4E8) // Violeta

    val transition = rememberInfiniteTransition(label = "aurora_motion")

    // Ondulación horizontal lenta — las tres capas a distintas velocidades para efecto layering
    val wavePhaseA by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(13000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_wave_a"
    )
    val wavePhaseB by transition.animateFloat(
        initialValue  = 0.33f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(16500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_wave_b"
    )
    val wavePhaseC by transition.animateFloat(
        initialValue  = 0.66f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(11800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_wave_c"
    )

    // Respiración global muy suave
    val breathe by transition.animateFloat(
        initialValue  = 0.90f,
        targetValue   = 1.10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(8200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_breathe"
    )

    Canvas(modifier = modifier) {
        val parallaxY   = scrollOffsetProvider() * 0.12f
        val screenH     = size.height
        val screenW     = size.width
        val bandHeight  = screenH * 0.30f * breathe

        // Posición vertical de cada banda — en el tercio superior de la pantalla
        val baseY = when (screenType) {
            AmbientScreen.HOME    -> screenH * 0.18f
            AmbientScreen.CONTENT -> screenH * 0.08f
            else                   -> screenH * 0.18f
        }

        // Ondulación vertical de cada capa (simula la curvatura de la aurora)
        val offsetA = (sin(wavePhaseA * Math.PI * 2).toFloat()) * screenH * 0.04f
        val offsetB = (sin(wavePhaseB * Math.PI * 2 + 1.2f).toFloat()) * screenH * 0.05f
        val offsetC = (sin(wavePhaseC * Math.PI * 2 + 2.4f).toFloat()) * screenH * 0.03f

        // Capa A — teal (banda inferior de las tres)
        val centerA = Offset(screenW * 0.5f, baseY + bandHeight * 0.50f + offsetA - parallaxY)
        drawOvalGradient(
            center     = centerA,
            radiusX    = screenW * 0.75f,
            radiusY    = bandHeight * 0.52f,
            color      = colorA,
            baseAlpha  = baseAlpha * 0.85f
        )

        // Capa B — cian (banda media)
        val centerB = Offset(screenW * 0.45f, baseY + bandHeight * 0.25f + offsetB - parallaxY)
        drawOvalGradient(
            center     = centerB,
            radiusX    = screenW * 0.65f,
            radiusY    = bandHeight * 0.42f,
            color      = colorB,
            baseAlpha  = baseAlpha * 0.70f
        )

        // Capa C — violeta (banda superior, más tenue)
        val centerC = Offset(screenW * 0.55f, baseY - bandHeight * 0.05f + offsetC - parallaxY)
        drawOvalGradient(
            center     = centerC,
            radiusX    = screenW * 0.80f,
            radiusY    = bandHeight * 0.35f,
            color      = colorC,
            baseAlpha  = baseAlpha * 0.55f
        )
    }
}

/** Dibuja una elipse con gradiente radial suave — helper para AuroraLayer. */
private fun DrawScope.drawOvalGradient(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    color: Color,
    baseAlpha: Float
) {
    val radius = maxOf(radiusX, radiusY)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to color.copy(alpha = baseAlpha),
                0.50f to color.copy(alpha = baseAlpha * 0.45f),
                1.00f to Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        radius   = radiusX, // Stretch horizontal usando solo radiusX como radio de dibujo
        center   = center
    )
}

// ──────────────────────────────────────────────────────────────────────────────
//  PETALS — Partículas Circulares Flotantes con Deriva Ascendente
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PetalsLayer(
    isDark: Boolean,
    screenType: AmbientScreen,
    scrollOffsetProvider: () -> Float,
    modifier: Modifier
) {
    val baseAlpha = when (screenType) {
        AmbientScreen.HOME    -> if (isDark) 0.38f else 0.30f
        AmbientScreen.CONTENT -> if (isDark) 0.18f else 0.14f
        AmbientScreen.CHAT    -> return
    }

    // Colores de pétalos — rosa/melocotón en claro, lavanda/magenta en oscuro
    val petalColors = if (isDark) {
        listOf(
            Color(0xFFC890E8), // Lavanda
            Color(0xFFE880B0), // Rosa magenta
            Color(0xFF90A8E0), // Azul lavanda
            Color(0xFFE8A0C8), // Rosa suave
            Color(0xFFB0A0F0), // Violeta
            Color(0xFFD8809A)  // Rosa frambuesa
        )
    } else {
        listOf(
            Color(0xFFE8A0B8), // Rosa pétalo
            Color(0xFFF0B890), // Melocotón
            Color(0xFFD8B0D8), // Malva claro
            Color(0xFFF0C8A8), // Albaricoque
            Color(0xFFD0A8C8), // Rosa lila
            Color(0xFFE8B8A0)  // Salmón suave
        )
    }

    // 6 pétalos con fases de animación desincronizadas para movimiento natural
    val transition = rememberInfiniteTransition(label = "petals_motion")

    // Deriva vertical — cada pétalo flota a su propio ritmo
    val driftY0 by transition.animateFloat(
        initialValue = 0f, targetValue = -1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petal_y0"
    )
    val driftY1 by transition.animateFloat(
        initialValue = -0.15f, targetValue = -1f,
        animationSpec = infiniteRepeatable(tween(14500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petal_y1"
    )
    val driftY2 by transition.animateFloat(
        initialValue = -0.30f, targetValue = -1f,
        animationSpec = infiniteRepeatable(tween(11200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petal_y2"
    )
    val driftY3 by transition.animateFloat(
        initialValue = -0.45f, targetValue = -1f,
        animationSpec = infiniteRepeatable(tween(13800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petal_y3"
    )
    val driftY4 by transition.animateFloat(
        initialValue = -0.60f, targetValue = -1f,
        animationSpec = infiniteRepeatable(tween(10600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petal_y4"
    )
    val driftY5 by transition.animateFloat(
        initialValue = -0.75f, targetValue = -1f,
        animationSpec = infiniteRepeatable(tween(15200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petal_y5"
    )

    // Oscilación horizontal suave (movimiento de vaivén)
    val swayX0 by transition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(9400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petal_sway_x0"
    )
    val swayX1 by transition.animateFloat(
        initialValue = 6f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petal_sway_x1"
    )
    val swayX2 by transition.animateFloat(
        initialValue = -5f, targetValue = 9f,
        animationSpec = infiniteRepeatable(tween(8600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petal_sway_x2"
    )

    // Respiración de tamaño
    val petalScale by transition.animateFloat(
        initialValue = 0.88f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(7800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petal_scale"
    )

    // Posiciones base distribuidas en la pantalla — fijas, elegantes
    val petalBasePositions = remember {
        listOf(
            Pair(0.15f, 0.75f),  // inferior izquierda
            Pair(0.82f, 0.68f),  // inferior derecha
            Pair(0.35f, 0.52f),  // centro izquierda
            Pair(0.68f, 0.40f),  // centro derecha
            Pair(0.22f, 0.28f),  // superior izquierda
            Pair(0.75f, 0.18f)   // superior derecha
        )
    }

    Canvas(modifier = modifier) {
        val parallaxY = scrollOffsetProvider() * 0.15f
        val driftYList = listOf(driftY0, driftY1, driftY2, driftY3, driftY4, driftY5)
        val swayXList  = listOf(swayX0, swayX0, swayX1, swayX1, swayX2, swayX2)

        petalBasePositions.forEachIndexed { i, (fx, fy) ->
            val color  = petalColors[i % petalColors.size]
            // Reutilizar el radio de tamaño variado por índice
            val radius = size.width * (0.12f + (i % 3) * 0.04f) * petalScale

            // Posición con deriva ascendente continua — cuando llega a -1 la transición la reinicia
            val rawY = fy + driftYList[i] * 0.40f
            val posY = (rawY.mod(1.2f) - 0.10f) * size.height - parallaxY
            val posX = size.width * fx + swayXList[i].dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to color.copy(alpha = baseAlpha),
                        0.55f to color.copy(alpha = baseAlpha * 0.38f),
                        1.00f to Color.Transparent
                    ),
                    center = Offset(posX, posY),
                    radius = radius
                ),
                radius = radius,
                center = Offset(posX, posY)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  NEBULA — Nubes de Polvo Cósmico con Color Profundo
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NebulaLayer(
    isDark: Boolean,
    screenType: AmbientScreen,
    scrollOffsetProvider: () -> Float,
    modifier: Modifier
) {
    val baseAlpha = when (screenType) {
        AmbientScreen.HOME    -> if (isDark) 0.22f else 0.18f
        AmbientScreen.CONTENT -> if (isDark) 0.14f else 0.09f
        AmbientScreen.CHAT    -> return
    }

    // Colores nebulosa — profundos en oscuro, muy suaves en claro
    val nebulaColors = if (isDark) {
        listOf(
            Color(0xFF1A3A8A), // Azul profundo
            Color(0xFF601878), // Magenta oscuro
            Color(0xFF0A5060), // Cian profundo
            Color(0xFF3A1060)  // Violeta profundo
        )
    } else {
        listOf(
            Color(0xFFB8C8E8), // Azul lavanda muy suave
            Color(0xFFD8B8D8), // Malva suave
            Color(0xFFB0D0D8), // Cian suave
            Color(0xFFC8B0D8)  // Violeta suave
        )
    }

    val transition = rememberInfiniteTransition(label = "nebula_motion")

    // Deriva muy lenta — la nebulosa se mueve casi imperceptiblemente
    val driftA by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nebula_drift_a"
    )
    val driftB by transition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(14000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nebula_drift_b"
    )
    val driftC by transition.animateFloat(
        initialValue = 0.25f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(22000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nebula_drift_c"
    )

    // Pulso de opacidad muy gradual
    val nebulaPulse by transition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(9600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nebula_pulse"
    )

    Canvas(modifier = modifier) {
        val parallaxY = scrollOffsetProvider() * 0.08f
        val w = size.width
        val h = size.height

        // Nube A — grande, centro-izquierda
        val centerAx = w * (0.20f + driftA * 0.12f)
        val centerAy = h * (0.25f + driftA * 0.08f) - parallaxY
        val radiusA  = w * 0.70f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to nebulaColors[0].copy(alpha = baseAlpha * nebulaPulse),
                    0.55f to nebulaColors[0].copy(alpha = baseAlpha * nebulaPulse * 0.40f),
                    1.00f to Color.Transparent
                ),
                center = Offset(centerAx, centerAy),
                radius = radiusA
            ),
            radius = radiusA,
            center = Offset(centerAx, centerAy)
        )

        // Nube B — magenta, esquina superior derecha
        val centerBx = w * (0.88f - driftB * 0.10f)
        val centerBy = h * (0.12f + driftB * 0.06f) - parallaxY
        val radiusB  = w * 0.58f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to nebulaColors[1].copy(alpha = baseAlpha * 0.80f * nebulaPulse),
                    0.60f to nebulaColors[1].copy(alpha = baseAlpha * 0.28f),
                    1.00f to Color.Transparent
                ),
                center = Offset(centerBx, centerBy),
                radius = radiusB
            ),
            radius = radiusB,
            center = Offset(centerBx, centerBy)
        )

        // Nube C — cian, zona inferior
        val centerCx = w * (0.55f + driftC * 0.10f - 0.05f)
        val centerCy = h * (0.75f - driftC * 0.08f) - parallaxY
        val radiusC  = w * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to nebulaColors[2].copy(alpha = baseAlpha * 0.72f * nebulaPulse),
                    0.55f to nebulaColors[2].copy(alpha = baseAlpha * 0.22f),
                    1.00f to Color.Transparent
                ),
                center = Offset(centerCx, centerCy),
                radius = radiusC
            ),
            radius = radiusC,
            center = Offset(centerCx, centerCy)
        )

        // Nube D — violeta, esquina inferior izquierda (solo HOME)
        if (screenType == AmbientScreen.HOME) {
            val centerDx = w * (0.08f + driftA * 0.08f)
            val centerDy = h * (0.80f - driftB * 0.05f) - parallaxY
            val radiusD  = w * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to nebulaColors[3].copy(alpha = baseAlpha * 0.65f),
                        0.60f to nebulaColors[3].copy(alpha = baseAlpha * 0.18f),
                        1.00f to Color.Transparent
                    ),
                    center = Offset(centerDx, centerDy),
                    radius = radiusD
                ),
                radius = radiusD,
                center = Offset(centerDx, centerDy)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  BREATH — Pulso Radial Ultra-Minimal con Ciclo de ~4 s
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun BreathLayer(
    accentColor: Color,
    isDark: Boolean,
    screenType: AmbientScreen,
    scrollOffsetProvider: () -> Float,
    modifier: Modifier
) {
    val baseAlpha = when (screenType) {
        AmbientScreen.HOME    -> if (isDark) 0.30f else 0.22f
        AmbientScreen.CONTENT -> if (isDark) 0.14f else 0.10f
        AmbientScreen.CHAT    -> return
    }

    // Transición de color suave cuando cambia la fase
    val animatedColor by animateColorAsState(
        targetValue   = accentColor,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label         = "breath_color"
    )

    val transition = rememberInfiniteTransition(label = "breath_motion")

    // Ciclo de respiración principal — inhala 4 s, exhala 4 s
    val breathScale by transition.animateFloat(
        initialValue  = 0.62f,
        targetValue   = 1.00f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    // Opacidad sincronizada con la escala — más brillante al exhalar
    val breathAlpha by transition.animateFloat(
        initialValue  = 0.70f,
        targetValue   = 1.00f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_alpha"
    )

    // Halo exterior muy sutil — "aura de la respiración"
    val haloScale by transition.animateFloat(
        initialValue  = 0.80f,
        targetValue   = 1.20f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_halo"
    )
    val haloAlpha by transition.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_halo_alpha"
    )

    Canvas(modifier = modifier) {
        val parallaxY = scrollOffsetProvider() * 0.10f
        val center = Offset(
            size.width * 0.50f,
            size.height * 0.44f - parallaxY
        )
        val maxRadius   = size.width * 0.60f
        val pulseRadius = maxRadius * breathScale
        val haloRadius  = maxRadius * haloScale * 1.35f
        val finalAlpha  = (baseAlpha * breathAlpha).coerceIn(0f, 1f)
        val finalHalo   = (baseAlpha * haloAlpha * 0.5f).coerceIn(0f, 1f)

        // Halo exterior difuso — desaparece al inhalar
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to animatedColor.copy(alpha = finalHalo),
                    0.60f to animatedColor.copy(alpha = finalHalo * 0.30f),
                    1.00f to Color.Transparent
                ),
                center = center,
                radius = haloRadius
            ),
            radius = haloRadius,
            center = center
        )

        // Pulso central principal
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to animatedColor.copy(alpha = finalAlpha),
                    0.40f to animatedColor.copy(alpha = finalAlpha * 0.60f),
                    0.75f to animatedColor.copy(alpha = finalAlpha * 0.18f),
                    1.00f to Color.Transparent
                ),
                center = center,
                radius = pulseRadius
            ),
            radius = pulseRadius,
            center = center
        )
    }
}
