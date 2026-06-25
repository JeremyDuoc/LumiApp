package com.jeremy.lumi.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.remember
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

    // ── Lanzador de animación ────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        // Reemplaza el animationSpec en SplashScreen
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
    }

    // Usamos otro LaunchedEffect para esperar al onboardingState
    LaunchedEffect(onboardingState, alpha.value) {
        // Solo continuamos si la animación de entrada terminó (alpha == 1f) y el estado ya cargó
        if (alpha.value == 1f && onboardingState != null) {
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

                Image(
                    painter = painterResource(id = R.drawable.lumi_logo),
                    contentDescription = "Lumi logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(100.dp)
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
                    text = "Lumi",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp,
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
