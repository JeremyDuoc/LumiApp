package com.jeremy.lumi.ui.screens.partner

// ═══════════════════════════════════════════════════════════════════════════════
//  MoodPulseOverlay.kt  —  Feature #1: MoodPulse
//
//  ¿QUÉ ES?
//  Cuando la otra persona registra su estado de ánimo en la app, el orbe de la
//  pantalla principal "late" con una animación especial durante ~5 segundos, y
//  aparece un overlay de Lottie encima del orbe con el emoji del mood.
//
//  FLUJO TÉCNICO:
//  1. Persona A registra mood → escribe en Firestore: links/{id}/ownerSnapshot.currentMood
//  2. Firestore listener (en PartnerRepository) detecta el cambio → emite via Flow
//  3. PartnerViewModel actualiza el UiState
//  4. Este Composable detecta el cambio y dispara la animación local
//
//  ¿Por qué NO usamos FCM aquí? FCM es para notificaciones push cuando la app
//  está en background. Dentro de la app, Firestore realtime es más eficiente.
//  FCM se usa para el caso en que la app está cerrada (ver MoodPulseNotifier.kt).
//
//  NOTA DE RENDIMIENTO:
//  - El Lottie solo se carga cuando `isVisible = true` (lazy).
//  - Usamos `key = mood` en LaunchedEffect para re-disparar si el mood cambia.
//  - La animación Lottie usa `iterations = 1` — no loopea.
// ═══════════════════════════════════════════════════════════════════════════════

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay

// ─── Overlay de reacción sobre el orbe ───────────────────────────────────────
// Usar dentro de un Box que tenga el orbe como fondo.

@Composable
fun MoodPulseOverlay(
    mood: String?,               // El mood actual de la pareja (puede ser null si no compartido)
    modifier: Modifier = Modifier
) {
    // Solo se activa cuando hay un mood nuevo distinto al anterior
    var previousMood by remember { mutableStateOf<String?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(mood) {
        if (mood != null && mood != previousMood && previousMood != null) {
            // Nuevo mood detectado → activar overlay
            isVisible = true
            delay(4500L)
            isVisible = false
        }
        previousMood = mood
    }

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = scaleIn(initialScale = 0.5f, animationSpec = spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit  = scaleOut(targetScale = 0.8f) + fadeOut(animationSpec = tween(500))
    ) {
        // Intentamos usar Lottie; si no existe el asset, hacemos fallback a emoji
        val moodEmoji = moodToEmoji(mood ?: "")
        val composition by rememberLottieComposition(
            // Se espera un asset "lottie/mood_pulse.json" en assets/
            // Si no existe, el composable simplemente no renderiza nada y cae al fallback
            spec = LottieCompositionSpec.Asset("lottie/mood_pulse.json")
        )

        if (composition != null) {
            LottieAnimation(
                composition = composition,
                iterations  = 1,
                modifier    = Modifier.size(80.dp)
            )
        } else {
            // Fallback elegante: emoji grande animado
            Text(
                text = moodEmoji,
                fontSize = 48.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

// ─── Pulso rápido del orbe al detectar nuevo mood ────────────────────────────
// Envuelve tu StoryOrbItem con esto para añadir la animación de pulso.

@Composable
fun MoodPulseWrapper(
    mood: String?,
    content: @Composable () -> Unit
) {
    var pulseTriggered by remember { mutableStateOf(false) }
    var previousMood   by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mood) {
        if (mood != null && mood != previousMood && previousMood != null) {
            pulseTriggered = true
            delay(1200L)
            pulseTriggered = false
        }
        previousMood = mood
    }

    val scale by animateFloatAsState(
        targetValue = if (pulseTriggered) 1.12f else 1f,
        animationSpec = if (pulseTriggered)
            spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)
        else
            spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "mood_pulse_scale"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // No usamos Modifier.scale() para no aplanar el layout — usamos graphicsLayer
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        content()
    }
}

// ─── Helper ───────────────────────────────────────────────────────────────────

internal fun moodToEmoji(mood: String): String = when (mood.lowercase()) {
    "feliz", "happy", "great", "bien"   -> "😊"
    "triste", "sad"                      -> "💙"
    "irritable", "irritated", "molesta" -> "😤"
    "cansada", "tired"                   -> "😴"
    "ansiosa", "anxious"                 -> "💫"
    "tranquila", "calm"                  -> "🌿"
    "energizada", "energized"            -> "⚡"
    "romántica"                          -> "🌹"
    else                                 -> "✨"
}
