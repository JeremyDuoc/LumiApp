package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Botón primario ceremonial de Lumi Onboarding v7.
 *
 * Reproduce fielmente la especificación CSS de .primary, .primary-ambient,
 * .primary-light (lumiButtonOrbit 3.7s) y .primary-ambient:before (lumiButtonDrift 4.7s):
 * - Pastilla redondeada (999 dp) con gradiente de marca cálido (btnPink -> btnViolet)
 * - Luz ambiental interior con halo cónico giratorio sin rotar la tipografía
 * - Cinta elíptica difusa que viaja horizontalmente
 * - Microinteracción al presionar (scale 0.973)
 * - Estado deshabilitado no interactivo y atenuado
 * - Respeto total a reducedMotion
 */
@Composable
fun LumiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    reducedMotion: Boolean = false
) {
    val tokens = LumiOnboardingThemeTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.973f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "lumi_btn_press_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "lumi_btn_ambient")

    // Rotación del halo cónico central (3.7s lineal continuo)
    val orbitAngle by if (!reducedMotion && enabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3700, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "btn_halo_orbit"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    // Desplazamiento de la cinta elíptica (4.7s ida y vuelta)
    val driftOffsetFraction by if (!reducedMotion && enabled) {
        infiniteTransition.animateFloat(
            initialValue = -0.3f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "btn_ribbon_drift"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    }

    val baseGradient = remember(tokens.btnPink, tokens.btnViolet) {
        Brush.horizontalGradient(listOf(tokens.btnPink, tokens.btnViolet))
    }

    val haloColors = remember(tokens.btnHaloPink, tokens.btnHaloViolet, tokens.btnHaloPeach) {
        listOf(
            tokens.btnHaloPink,
            tokens.btnHaloViolet,
            tokens.btnHaloPeach,
            tokens.btnHaloPink
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(baseGradient)
            .alpha(if (enabled) 1f else 0.55f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Capa ambiental animada interna (clip a pastilla)
        if (enabled && !reducedMotion) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(14.dp)
                    .alpha(if (isPressed) 1f else 0.72f)
            ) {
                val w = size.width
                val h = size.height

                // Cinta elíptica viajera
                val ribbonX = w * driftOffsetFraction
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            tokens.btnHaloPink.copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(ribbonX, h * 0.5f),
                        radius = h * 1.8f
                    ),
                    radius = h * 1.8f,
                    center = Offset(ribbonX, h * 0.5f)
                )

                // Halo cónico giratorio central
                rotate(degrees = orbitAngle, pivot = Offset(w * 0.5f, h * 0.5f)) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = haloColors,
                            center = Offset(w * 0.5f, h * 0.5f)
                        ),
                        radius = w * 0.45f,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                }
            }
        }

        // Tipografía nítida y estática por encima del resplandor
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF23182C),
            letterSpacing = (-0.25).sp
        )
    }
}
