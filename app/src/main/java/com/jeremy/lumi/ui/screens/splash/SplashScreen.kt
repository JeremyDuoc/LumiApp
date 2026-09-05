package com.jeremy.lumi.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.jeremy.lumi.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.ui.theme.LumiTheme
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
// ─────────────────────────────────────────────
//  Duración y comportamiento de la animación
// ─────────────────────────────────────────────
private const val ANIMATION_DURATION_MS  = 1200
private const val POST_ANIMATION_DELAY_MS = 300L

/**
 * Pantalla de bienvenida (Splash) de Lumi.
 *
 * Aplica simultáneamente:
 *  - **Fade-In**: opacidad de 0f → 1f
 *  - **Scale-In**: escala de 0.8f → 1.0f
 *
 * Usa [Animatable] con un [tween] de [ANIMATION_DURATION_MS] ms y
 * [FastOutSlowInEasing] para garantizar una curva de movimiento fluida y
 * profesional. Al finalizar, espera [POST_ANIMATION_DELAY_MS] ms antes de
 * invocar [onSplashFinished] para que la transición al Home sea suave.
 *
 * @param onSplashFinished Callback que se ejecuta al terminar la secuencia de
 *                         animación. Úsalo para navegar a la pantalla principal.
 */
@Composable
fun SplashScreen(
    onboardingState: Boolean?,
    onSplashFinished: (Boolean) -> Unit
) {

    // ── Animatables ─────────────────────────────────────────────────────────
    val alpha = remember { Animatable(initialValue = 0f) }
    val scale = remember { Animatable(initialValue = 0.8f) }
    var isAnimationFinished by remember { mutableStateOf(false) }

    // ── Lanzador de animación ────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        val animationSpec = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )

        // Fade-In y Scale-In en paralelo dentro de un scope explícito
        coroutineScope {
            val fadeJob  = async { alpha.animateTo(targetValue = 1f, animationSpec = animationSpec) }
            val scaleJob = async { scale.animateTo(targetValue = 1f, animationSpec = animationSpec) }
            fadeJob.await()
            scaleJob.await()
        }

        // Pequeña pausa para que el ojo "asiente" la imagen antes de navegar
        delay(POST_ANIMATION_DELAY_MS)
        isAnimationFinished = true
    }

    // Usamos otro LaunchedEffect para esperar al onboardingState
    LaunchedEffect(onboardingState, isAnimationFinished) {
        // Solo continuamos si la animación de entrada terminó y el estado ya cargó
        if (isAnimationFinished && onboardingState != null) {
            onSplashFinished(onboardingState)
        }
    }

    // ── Contenido visual ────────────────────────────────────────────────────
    SplashContent(
        alpha = alpha.value,
        scale = scale.value
    )
}

/**
 * Composable puro (sin lógica de animación) que renderiza el layout del Splash.
 *
 * Separado de [SplashScreen] para facilitar Preview y pruebas unitarias.
 *
 * @param alpha Valor actual de opacidad (0f…1f).
 * @param scale Valor actual de escala (0.8f…1.0f).
 */
@Composable
fun SplashContent(
    alpha: Float = 1f,
    scale: Float = 1f
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .alpha(alpha)
                .scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "logo")

            // Float suave vertical — sin scale agresivo
            val floatY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "floatY"
            )

            // Opacidad del glow — arranca solo cuando alpha ya llegó a 1f
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowAlpha"
            )

            // Solo animar glow después del fade-in
            val showGlow = alpha >= 0.99f

            Box(contentAlignment = Alignment.Center) {
                // Glow: radial gradient sin blur — evita artefactos de rendering
                if (showGlow) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .alpha(glowAlpha)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }

                // NUEVO LOGO ANIMADO DE FASES LUNARES
                AnimatedMoonLogo(
                    progress = alpha, // Usamos el alpha del Splash como progreso de apertura
                    modifier = Modifier
                        .height(60.dp)
                        .offset(y = if (showGlow) floatY.dp else 0.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Nombre con delay escalonado
            AnimatedVisibility(
                visible = alpha > 0.5f,
                enter = fadeIn(tween(600)) + slideInVertically(
                    tween(600),
                    initialOffsetY = { it / 4 }
                )
            ) {
                val brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
                Text(
                    text = "LUMI", // Cambiado a MAYÚSCULAS como en el diseño de referencia
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 12.sp, // Letra más espaciada como en el diseño
                        brush = brush
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline con delay mayor
            AnimatedVisibility(
                visible = alpha > 0.75f,
                enter = fadeIn(tween(500, delayMillis = 150))
            ) {
                Text(
                    text = "Tu ciclo, tu bienestar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Renderiza 7 lunas que se abren desde el centro hacia los lados de forma fluida.
 */
@Composable
fun AnimatedMoonLogo(
    progress: Float, // de 0f a 1f
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val moonCount = 7
    val centerIndex = moonCount / 2
    
    // Curva de animación para que se abra suavemente
    val easedProgress = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
    
    // Espaciado final entre lunas
    val finalSpacing = 36.dp

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        for (i in 0 until moonCount) {
            val distFromCenter = i - centerIndex
            val currentOffset = finalSpacing * distFromCenter * easedProgress
            
            // Opacidad según distancia al centro (centro = 1f, bordes = más transparente)
            val alpha = 1f - (kotlin.math.abs(distFromCenter) * 0.25f)
            val color = primaryColor.copy(alpha = alpha)

            Canvas(
                modifier = Modifier
                    .size(30.dp)
                    .offset(x = currentOffset)
            ) {
                val radius = size.height / 2f
                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)

                if (distFromCenter == 0) {
                    // Luna Llena (centro)
                    drawCircle(color = color, radius = radius, center = center)
                } else {
                    // Lunas crecientes / menguantes
                    // Usamos Path operations para hacer la figura de la luna restando un círculo
                    val pathFull = androidx.compose.ui.graphics.Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(center = center, radius = radius))
                    }
                    
                    // Qué tanto desplazamos el círculo recortador
                    // Si está más lejos del centro, el recorte es mayor (luna más fina)
                    val cutOffsetRatio = if (kotlin.math.abs(distFromCenter) == 1) 0.5f else 1.2f
                    // Dirección del recorte (izquierda o derecha)
                    val cutDirection = if (distFromCenter < 0) 1f else -1f
                    
                    val pathCut = androidx.compose.ui.graphics.Path().apply {
                        addOval(
                            androidx.compose.ui.geometry.Rect(
                                center = center.copy(x = center.x + (radius * cutOffsetRatio * cutDirection)), 
                                radius = radius * 1.1f // Un poco más grande para que el corte sea suave
                            )
                        )
                    }

                    val crescentPath = androidx.compose.ui.graphics.Path().apply {
                        op(pathFull, pathCut, androidx.compose.ui.graphics.PathOperation.Difference)
                    }

                    drawPath(path = crescentPath, color = color)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Previews
// ─────────────────────────────────────────────

@Preview(name = "Splash · Estado inicial (invisible)", showBackground = true)
@Composable
private fun SplashContentInitialPreview() {
    LumiTheme {
        SplashContent(alpha = 0f, scale = 0.8f)
    }
}

@Preview(name = "Splash · Estado final (visible)", showBackground = true)
@Composable
private fun SplashContentFinalPreview() {
    LumiTheme {
        SplashContent(alpha = 1f, scale = 1f)
    }
}
