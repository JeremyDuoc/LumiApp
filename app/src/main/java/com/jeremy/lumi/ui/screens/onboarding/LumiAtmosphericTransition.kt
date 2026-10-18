package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.ui.components.LumiBrandLockup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Los 5 mensajes contemplativos aprobados del HTML v7 de Lumi:
 * Presentados secuencialmente durante la transición atmosférica (~11.5 s).
 */
private val ATMOSPHERIC_MESSAGES = listOf(
    "Tu espacio empieza aquí" to "Un momento para hacerlo tuyo",
    "Conociendo tus preferencias" to "Solo lo que decidiste compartir",
    "Un lugar a tu manera" to "Sin prisas, sin expectativas",
    "Preparando tu bienvenida" to "Tus elecciones estarán a mano",
    "Todo está listo" to "Bienvenida a Lumi"
)

private val MESSAGE_SCHEDULE_MS = listOf(1300L, 3450L, 5550L, 7780L, 9770L)

/**
 * Transición atmosférica ceremonial final de Lumi Onboarding v7.
 *
 * Reproduce al 100% la especificación de referencia:
 * - Duración total: 11520 ms (~11.5 s)
 * - Velo de alta fidelidad sin halos blancos ni grises en temas OLED/oscuros
 * - Campo cromático con pulso orgánico detrás del logotipo
 * - Posicionamiento y vuelo del logotipo:
 *   * 0..900 ms: Traslado suave hacia el centro (Y ~ 39.7%) escalando a 1.17x
 *   * 900..10760 ms: Respiración sutil en el centro durante los 5 mensajes
 *   * 10760..11350 ms: Vuelo hacia la cabecera real (~590 ms) con cubic-bezier(.17, .79, .21, 1)
 * - 5 Mensajes contemplativos secuenciales con entrada/salida fluida
 * - Botón "Omitir" visible a partir de 850 ms para control del usuario
 * - Respeto estricto a reducedMotion (omisión inmediata o secuencia abreviada)
 * - Contrato no invasivo hacia HomeScreen vía [onFinished]
 */
@Composable
fun LumiAtmosphericTransition(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false
) {
    val tokens = LumiOnboardingThemeTokens.current
    val density = LocalDensity.current

    // Si reducedMotion está activo, saltar inmediatamente a Home
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            onFinished()
        }
    }
    if (reducedMotion) return

    val totalDurationMs = 11520L
    val tCenterMs = 900L
    val flightMs = 590L
    val flightStartMs = totalDurationMs - flightMs - 170L // 10760L
    val homeRevealMs = totalDurationMs - 1120L // 10400L
    val skipAppearMs = 850L

    var elapsedTimeMs by remember { mutableLongStateOf(0L) }
    var currentMessageIndex by remember { mutableIntStateOf(0) }
    var showSkip by remember { mutableStateOf(false) }
    var messagesVisible by remember { mutableStateOf(true) }

    // Animadores de mensaje individual (salida y entrada)
    val messageAlpha = remember { Animatable(0f) }
    val messageOffsetY = remember { Animatable(13f) }

    // Temporizador principal y secuenciación de mensajes
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()

        // Loop de avance de tiempo de alta precisión
        launch {
            while (true) {
                val now = System.currentTimeMillis() - startTime
                elapsedTimeMs = now

                if (now >= skipAppearMs && !showSkip) {
                    showSkip = true
                }

                if (now >= homeRevealMs && messagesVisible) {
                    messagesVisible = false
                }

                if (now >= totalDurationMs + 35L) {
                    onFinished()
                    break
                }
                delay(16L) // ~60 FPS
            }
        }

        // Secuencia de los 5 mensajes
        MESSAGE_SCHEDULE_MS.forEachIndexed { index, triggerTimeMs ->
            val wait = triggerTimeMs - (System.currentTimeMillis() - startTime)
            if (wait > 0) delay(wait)

            currentMessageIndex = index

            if (index == 0) {
                // Entrada del primer mensaje
                messageAlpha.snapTo(0f)
                messageOffsetY.snapTo(13f)
                launch {
                    messageAlpha.animateTo(1f, tween(750, easing = CubicBezierEasing(0.18f, 1f, 0.3f, 1f)))
                }
                launch {
                    messageOffsetY.animateTo(0f, tween(750, easing = CubicBezierEasing(0.18f, 1f, 0.3f, 1f)))
                }
            } else {
                // Salida hacia arriba del mensaje anterior
                launch {
                    messageAlpha.animateTo(0f, tween(400, easing = LinearEasing))
                }
                messageOffsetY.animateTo(-13f, tween(400, easing = LinearEasing))

                // Entrada desde abajo del nuevo mensaje
                messageOffsetY.snapTo(15f)
                launch {
                    messageAlpha.animateTo(1f, tween(680, easing = CubicBezierEasing(0.18f, 1f, 0.3f, 1f)))
                }
                launch {
                    messageOffsetY.animateTo(0f, tween(680, easing = CubicBezierEasing(0.18f, 1f, 0.3f, 1f)))
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.bg)
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // ── CÁLCULO DE COORDENADAS DE LOGO EN VUELO ─────────────────────────────
        // Origen: Step 7 top (X centrado, Y ~ 80dp)
        val startY = 80.dp.value * density.density
        // Centro de contemplación: Y = 39.7% de la pantalla
        val centerY = screenHeightPx * 0.397f
        // Destino de cabecera: X = 20dp padding, Y = status bar + 14dp
        val targetX = (-screenWidthPx / 2f + 20.dp.value * density.density + 60.dp.value * density.density)
        val targetY = 48.dp.value * density.density

        val logoProgress = when {
            elapsedTimeMs < tCenterMs -> {
                // 0..900ms: viaje de Step 7 al centro
                val fraction = (elapsedTimeMs.toFloat() / tCenterMs.toFloat()).coerceIn(0f, 1f)
                val ease = CubicBezierEasing(0.16f, 0.89f, 0.24f, 1f).transform(fraction)
                startY + (centerY - startY) * ease
            }
            elapsedTimeMs in tCenterMs..flightStartMs -> {
                // 900..10760ms: respiración sutil en el centro
                val breathCycle = ((elapsedTimeMs - tCenterMs) % 3200L) / 3200f
                centerY + (sin(breathCycle * 2f * Math.PI.toFloat()) * 4f * density.density)
            }
            else -> {
                // 10760..11350ms: Vuelo final a cabecera (~590ms)
                val flightFraction = ((elapsedTimeMs - flightStartMs).toFloat() / flightMs.toFloat()).coerceIn(0f, 1f)
                val flightEase = CubicBezierEasing(0.17f, 0.79f, 0.21f, 1f).transform(flightFraction)
                centerY + (targetY - centerY) * flightEase
            }
        }

        val logoX = when {
            elapsedTimeMs < flightStartMs -> 0f
            else -> {
                val flightFraction = ((elapsedTimeMs - flightStartMs).toFloat() / flightMs.toFloat()).coerceIn(0f, 1f)
                val flightEase = CubicBezierEasing(0.17f, 0.79f, 0.21f, 1f).transform(flightFraction)
                targetX * flightEase
            }
        }

        val logoScale = when {
            elapsedTimeMs < tCenterMs -> {
                val fraction = (elapsedTimeMs.toFloat() / tCenterMs.toFloat()).coerceIn(0f, 1f)
                1.0f + 0.17f * fraction
            }
            elapsedTimeMs in tCenterMs..flightStartMs -> {
                1.17f
            }
            else -> {
                val flightFraction = ((elapsedTimeMs - flightStartMs).toFloat() / flightMs.toFloat()).coerceIn(0f, 1f)
                val flightEase = CubicBezierEasing(0.17f, 0.79f, 0.21f, 1f).transform(flightFraction)
                1.17f - (1.17f - 0.52f) * flightEase
            }
        }

        // ── CAMPO CROMÁTICO (GLOW) SUAVE SIN HALOS BLANCOS/GRISES ────────────────
        val glowFraction = (elapsedTimeMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        val (glowAlpha, glowScale) = when {
            glowFraction < 0.13f -> {
                val f = glowFraction / 0.13f
                (0.78f * f) to (0.80f + 0.15f * f)
            }
            glowFraction < 0.37f -> {
                val f = (glowFraction - 0.13f) / 0.24f
                (0.78f + 0.22f * f) to (0.95f + 0.12f * f)
            }
            glowFraction < 0.66f -> {
                val f = (glowFraction - 0.37f) / 0.29f
                (1.0f - 0.03f * f) to (1.07f - 0.05f * f)
            }
            glowFraction < 0.84f -> {
                val f = (glowFraction - 0.66f) / 0.18f
                (0.97f - 0.12f * f) to (1.02f + 0.10f * f)
            }
            else -> {
                val f = (glowFraction - 0.84f) / 0.16f
                (0.85f * (1f - f)) to (1.12f + 0.12f * f)
            }
        }

        // Capa de resplandor elíptico cromático
        if (tokens.isDark && glowAlpha > 0.01f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = glowScale
                        scaleY = glowScale
                        alpha = glowAlpha * 0.66f
                    }
                    .blur(22.dp)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tokens.glow,
                            tokens.veil1.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.44f),
                        radius = size.width * 0.72f
                    )
                )
            }
        }

        // ── LOGO FLOR Y MARCA EN VUELO CONTINUO ─────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationX = logoX
                    translationY = logoProgress
                    scaleX = logoScale
                    scaleY = logoScale
                },
            contentAlignment = Alignment.Center
        ) {
            LumiBrandLockup(
                flowerSize = 46,
                fontSize = 42.sp,
                starSize = 16.sp,
                letterSpacing = (-2.8).sp,
                animated = true
            )
        }

        // ── MENSAJES CONTEMPLATIVOS SECUENCIALES ────────────────────────────────
        if (messagesVisible && elapsedTimeMs >= 1000L) {
            val (currentHeadline, currentSubtitle) = ATMOSPHERIC_MESSAGES.getOrElse(currentMessageIndex) {
                ATMOSPHERIC_MESSAGES.last()
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 170.dp, start = 32.dp, end = 32.dp)
                    .graphicsLayer {
                        alpha = messageAlpha.value
                        translationY = messageOffsetY.value * density.density
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentHeadline,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.4).sp
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = currentSubtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = tokens.muted,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif
                )
            }
        }

        // ── BOTÓN OMITIR (VISIBLE DESDE 850 MS) ──────────────────────────────────
        AnimatedVisibility(
            visible = showSkip && messagesVisible,
            enter = fadeIn(tween(350)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(tokens.panel)
                    .border(1.dp, tokens.line, RoundedCornerShape(999.dp))
                    .clickable { onFinished() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Omitir →",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.muted
                )
            }
        }
    }
}
