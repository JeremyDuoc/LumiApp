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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.ui.theme.LumiTheme
import kotlinx.coroutines.delay

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
fun SplashScreen(onSplashFinished: () -> Unit) {

    // ── Animatables ─────────────────────────────────────────────────────────
    val alpha = remember { Animatable(initialValue = 0f) }
    val scale = remember { Animatable(initialValue = 0.8f) }

    // ── Lanzador de animación ────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        val animationSpec = tween<Float>(
            durationMillis = ANIMATION_DURATION_MS,
            easing = FastOutSlowInEasing
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

        onSplashFinished()
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
            // ── Logo / ícono principal ───────────────────────────────────────
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Lumi logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Nombre de la app ─────────────────────────────────────────────
            Text(
                text = "Lumi",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Tagline ──────────────────────────────────────────────────────
            Text(
                text = "Tu ciclo, tu bienestar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
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
