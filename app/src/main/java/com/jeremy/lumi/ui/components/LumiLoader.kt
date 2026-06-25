package com.jeremy.lumi.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LumiLoader(modifier: Modifier = Modifier, baseColor: Color = MaterialTheme.colorScheme.primary) {
    val infiniteTransition = rememberInfiniteTransition(label = "firefly")

    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = 22.dp.toPx()

            // Lissajous curve for magical movement
            val xOffset = sin(time * 2) * radius
            val yOffset = cos(time * 3) * radius
            val fireflyPos = Offset(cx + xOffset, cy + yOffset)

            // Glow effect
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(baseColor.copy(alpha = 0.5f * pulse), Color.Transparent),
                    center = fireflyPos,
                    radius = 35.dp.toPx() * pulse
                ),
                radius = 35.dp.toPx() * pulse,
                center = fireflyPos,
                blendMode = BlendMode.Screen
            )

            // Firefly core
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx() * pulse,
                center = fireflyPos
            )
            drawCircle(
                color = baseColor,
                radius = 6.dp.toPx() * pulse,
                center = fireflyPos,
                alpha = 0.8f
            )

            // Mini sparkles trailing
            for (i in 1..4) {
                val delayTime = time - (i * 0.15f)
                val trailX = sin(delayTime * 2) * radius
                val trailY = cos(delayTime * 3) * radius
                val trailPos = Offset(cx + trailX, cy + trailY)

                drawCircle(
                    color = baseColor.copy(alpha = 0.5f / i),
                    radius = 3.dp.toPx() * (1f / i),
                    center = trailPos
                )
            }
        }
    }
}
