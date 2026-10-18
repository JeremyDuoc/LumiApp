package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Checkbox ceremonial con trazo dibujado continuo.
 *
 * Reproduce al 100% la animación de reference/lumi_onboarding_v7_luz_cromatica.html:
 * - .check-frame: marco base neutral
 * - .check-fill: relleno sutil de acento (opacity 0.12)
 * - .check-outline: contorno dibujado con PathMeasure (550ms cubic-bezier(.2,.72,.3,1))
 * - .check-tick: tilde dibujada secuencialmente (380ms diferido 155ms)
 */
@Composable
fun LumiConsentCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false
) {
    val tokens = LumiOnboardingThemeTokens.current
    val interactionSource = remember { MutableInteractionSource() }

    val outlineProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (!reducedMotion) {
            tween(
                durationMillis = 550,
                easing = CubicBezierEasing(0.2f, 0.72f, 0.3f, 1f)
            )
        } else {
            tween(durationMillis = 0)
        },
        label = "checkbox_outline_progress"
    )

    val tickProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (!reducedMotion) {
            tween(
                durationMillis = 380,
                delayMillis = if (checked) 155 else 0,
                easing = CubicBezierEasing(0.15f, 0.85f, 0.25f, 1f)
            )
        } else {
            tween(durationMillis = 0)
        },
        label = "checkbox_tick_progress"
    )

    val fillAlpha by animateFloatAsState(
        targetValue = if (checked) 0.12f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "checkbox_fill_alpha"
    )

    val outlinePath = remember { Path() }
    val tickPath = remember { Path() }
    val pathMeasure = remember { PathMeasure() }
    val segmentPath = remember { Path() }

    Box(
        modifier = modifier
            .size(32.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val w = size.width
            val h = size.height
            val strokeWidthBase = 1.2.dp.toPx()
            val strokeWidthOutline = 2.0.dp.toPx()
            val strokeWidthTick = 2.8.dp.toPx()
            val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

            // 1. Marco base neutro inactivo
            drawRoundRect(
                color = tokens.line,
                size = Size(w - strokeWidthBase, h - strokeWidthBase),
                topLeft = Offset(strokeWidthBase / 2, strokeWidthBase / 2),
                cornerRadius = cornerRadius,
                style = Stroke(width = strokeWidthBase)
            )

            // 2. Relleno suave con opacidad 0.12
            if (fillAlpha > 0f) {
                drawRoundRect(
                    color = tokens.accent.copy(alpha = fillAlpha),
                    size = Size(w - strokeWidthBase * 2, h - strokeWidthBase * 2),
                    topLeft = Offset(strokeWidthBase, strokeWidthBase),
                    cornerRadius = cornerRadius
                )
            }

            // 3. Contorno dibujado progresivamente
            if (outlineProgress > 0f) {
                outlinePath.reset()
                outlinePath.addRoundRect(
                    RoundRect(
                        left = strokeWidthOutline / 2,
                        top = strokeWidthOutline / 2,
                        right = w - strokeWidthOutline / 2,
                        bottom = h - strokeWidthOutline / 2,
                        cornerRadius = cornerRadius
                    )
                )

                pathMeasure.setPath(outlinePath, false)
                val totalLength = pathMeasure.length
                segmentPath.reset()
                pathMeasure.getSegment(0f, totalLength * outlineProgress, segmentPath, true)

                drawPath(
                    path = segmentPath,
                    color = tokens.accent,
                    style = Stroke(
                        width = strokeWidthOutline,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 4. Tilde dibujada progresivamente (puntos: (9, 15) -> (13, 19) -> (21, 10) normalizados a 30)
            if (tickProgress > 0f) {
                val scaleFactor = w / 30f
                tickPath.reset()
                tickPath.moveTo(9f * scaleFactor, 15f * scaleFactor)
                tickPath.lineTo(13.2f * scaleFactor, 19.2f * scaleFactor)
                tickPath.lineTo(21.5f * scaleFactor, 10f * scaleFactor)

                pathMeasure.setPath(tickPath, false)
                val totalTickLength = pathMeasure.length
                segmentPath.reset()
                pathMeasure.getSegment(0f, totalTickLength * tickProgress, segmentPath, true)

                drawPath(
                    path = segmentPath,
                    color = tokens.accent,
                    style = Stroke(
                        width = strokeWidthTick,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
