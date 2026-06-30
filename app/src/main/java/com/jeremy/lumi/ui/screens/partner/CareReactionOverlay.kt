package com.jeremy.lumi.ui.screens.partner

// ═══════════════════════════════════════════════════════════════════════════════
//  CareReactionOverlay.kt  —  Feature #3: Micro-Reacción al enviar CareAction
//
//  ¿QUÉ ES?
//  Cuando el usuario pulsa uno de los botones de CareAction (🤗☕🍵🍫💊),
//  una animación Lottie "vuela" desde el botón hasta el avatar de la pareja,
//  simulando que el gesto llega físicamente a la otra persona.
//
//  IMPLEMENTACIÓN SIMPLIFICADA (sin physics engine):
//  Usamos un overlay de pantalla completa con un emoji/Lottie que se anima
//  con un trayecto de bezier personalizado usando animateFloat + lerp.
//
//  FLUJO:
//  1. Usuario toca CareActionButton → se llama onSendCareAction()
//  2. El ViewModel envía a Firestore
//  3. Localmente, se dispara CareReactionOverlay con el emoji correcto
//  4. Animación de 1.5s y desaparece
//
//  NOTA: Este componente es PURAMENTE LOCAL — no depende de Firestore.
//  La confirmación visual es inmediata (optimistic UI).
// ═══════════════════════════════════════════════════════════════════════════════

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import com.airbnb.lottie.compose.*
import com.jeremy.lumi.domain.model.CareAction
import kotlinx.coroutines.delay

// ─── Estado observable para disparar la reacción ─────────────────────────────

class CareReactionState {
    var pendingAction by mutableStateOf<CareAction?>(null)
    var trigger       by mutableStateOf(0)    // se incrementa para re-disparar

    fun fire(action: CareAction) {
        pendingAction = action
        trigger++
    }
}

@Composable
fun rememberCareReactionState(): CareReactionState = remember { CareReactionState() }

// ─── Overlay principal ────────────────────────────────────────────────────────
// Colocar en un Box que cubra la pantalla completa, encima de todo el contenido.

@Composable
fun CareReactionOverlay(
    state: CareReactionState,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    var currentEmoji by remember { mutableStateOf("🤗") }

    LaunchedEffect(state.trigger) {
        if (state.trigger > 0 && state.pendingAction != null) {
            currentEmoji = state.pendingAction!!.toEmoji()
            isVisible = true
            delay(1600L)
            isVisible = false
        }
    }

    if (!isVisible) return

    // Trayecto de vuelo: empieza en la parte inferior-centro, sube hacia el avatar
    val flyProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "fly_progress",
        finishedListener = { }
    )

    val scale by animateFloatAsState(
        targetValue = when {
            flyProgress < 0.1f -> 0.3f
            flyProgress < 0.5f -> 1.4f
            flyProgress < 0.9f -> 1.2f
            else               -> 0.1f
        },
        animationSpec = tween(300),
        label = "fly_scale"
    )

    // Posición vertical: desde y=0.85 hasta y=0.25 del contenedor
    val yFraction = 0.85f - (flyProgress * 0.60f)
    // Leve curva horizontal usando seno
    val xOffset   = kotlin.math.sin(flyProgress * kotlin.math.PI.toFloat()) * 40f

    Box(modifier = modifier.fillMaxSize()) {
        // Necesitamos medir la pantalla para posicionar
        BoxWithConstraints {
            val screenHeight = constraints.maxHeight.toFloat()
            val screenWidth  = constraints.maxWidth.toFloat()

            Text(
                text = currentEmoji,
                fontSize = 48.sp,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = (screenWidth / 2f) + xOffset - 30f
                        translationY = screenHeight * yFraction
                        scaleX = scale
                        scaleY = scale
                        alpha  = if (flyProgress > 0.85f)
                            1f - ((flyProgress - 0.85f) / 0.15f)
                        else
                            flyProgress.coerceIn(0f, 1f)
                    }
            )
        }
    }
}

// ─── Helper ───────────────────────────────────────────────────────────────────

private fun CareAction.toEmoji(): String = when (this) {
    CareAction.HUG       -> "🤗"
    CareAction.TEA       -> "🍵"
    CareAction.COFFEE    -> "☕"
    CareAction.CHOCOLATE -> "🍫"
    CareAction.PHARMACY  -> "💊"
}
