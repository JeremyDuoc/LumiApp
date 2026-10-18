package com.jeremy.lumi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Familia de glifos propios de Lumi — sin emojis ni iconos genéricos de Material.
 * Construidos con geometría vectorial exacta sobre un viewport estándar de 24x24.
 */

/**
 * Glifo propio de "Mi día" inspirado en un cuaderno / journal personal:
 * - Silueta de cuaderno vertical con esquinas suaves
 * - Línea de encuadernación sutil a la izquierda
 * - Dos líneas horizontales de registro interior
 * - Nodo circular orbital Lumi en la parte superior derecha
 * - Indicador de registro completado (hasEntry = true)
 */
@Composable
fun LumiJournalGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    hasEntry: Boolean = false,
    strokeWidth: Dp = 1.7.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val scaleX = w / 24f
        val scaleY = h / 24f
        val strokePx = strokeWidth.toPx()

        val stroke = Stroke(
            width = strokePx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // 1. Contorno del cuaderno vertical con esquinas redondeadas (x: 4.8 a 19.2, y: 3.2 a 20.8)
        val rectLeft = 4.8f * scaleX
        val rectTop = 3.2f * scaleY
        val rectWidth = 14.4f * scaleX
        val rectHeight = 17.6f * scaleY
        val cornerRadius = CornerRadius(3.5f * scaleX, 3.5f * scaleY)

        drawRoundRect(
            color = tint,
            topLeft = Offset(rectLeft, rectTop),
            size = Size(rectWidth, rectHeight),
            cornerRadius = cornerRadius,
            style = stroke
        )

        // 2. Línea de encuadernación vertical en el lomo izquierdo (x = 8.8f)
        val spineX = 8.8f * scaleX
        drawLine(
            color = tint.copy(alpha = 0.55f),
            start = Offset(spineX, 4.2f * scaleY),
            end = Offset(spineX, 19.8f * scaleY),
            strokeWidth = strokePx * 0.85f,
            cap = StrokeCap.Round
        )

        // 3. Dos marcas horizontales interiores de registro
        val lineStart = 11.4f * scaleX
        val lineEnd = 16.4f * scaleX
        drawLine(
            color = tint.copy(alpha = 0.85f),
            start = Offset(lineStart, 11.5f * scaleY),
            end = Offset(lineEnd, 11.5f * scaleY),
            strokeWidth = strokePx * 0.9f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint.copy(alpha = 0.85f),
            start = Offset(lineStart, 15.5f * scaleY),
            end = Offset(15.2f * scaleX, 15.5f * scaleY),
            strokeWidth = strokePx * 0.9f,
            cap = StrokeCap.Round
        )

        // 4. Detalle Lumi: pequeño nodo circular superior derecho
        if (hasEntry) {
            // Nodo sólido de completado (4dp óptico)
            drawCircle(
                color = tint,
                radius = 2.4f * scaleX,
                center = Offset(15.2f * scaleX, 7.2f * scaleY)
            )
        } else {
            // Micro sparkle / dot orbital Lumi
            drawCircle(
                color = tint,
                radius = 1.3f * scaleX,
                center = Offset(15.2f * scaleX, 7.2f * scaleY)
            )
        }
    }
}

/**
 * Glifo propio de "Me llegó" (híbrido gota + pétalo):
 * - Silueta asimétrica orgánica con punta suave y base redondeada
 * - Curva interior secundaria que recorre ~40% del contorno
 * - Geometría compartida con el aro del ciclo y la identidad Lumi
 */
@Composable
fun LumiPeriodGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    innerAlpha: Float = 0.82f,
    strokeWidth: Dp = 1.7.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val scaleX = w / 24f
        val scaleY = h / 24f
        val strokePx = strokeWidth.toPx()

        val stroke = Stroke(
            width = strokePx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // 1. Silueta exterior híbrida (gota + pétalo)
        val outerPath = Path().apply {
            moveTo(12.2f * scaleX, 3.4f * scaleY)
            // Curva izquierda hacia la base redondeada
            cubicTo(
                10.0f * scaleX, 6.5f * scaleY,
                5.2f * scaleX, 11.0f * scaleY,
                5.2f * scaleX, 14.8f * scaleY
            )
            cubicTo(
                5.2f * scaleX, 18.6f * scaleY,
                8.3f * scaleX, 21.0f * scaleY,
                12.0f * scaleX, 21.0f * scaleY
            )
            // Curva derecha asimétrica volviendo a la punta suave
            cubicTo(
                15.7f * scaleX, 21.0f * scaleY,
                18.8f * scaleX, 18.4f * scaleY,
                18.8f * scaleX, 14.2f * scaleY
            )
            cubicTo(
                18.8f * scaleX, 10.5f * scaleY,
                14.2f * scaleX, 6.2f * scaleY,
                12.2f * scaleX, 3.4f * scaleY
            )
            close()
        }

        drawPath(
            path = outerPath,
            color = tint,
            style = stroke
        )

        // 2. Curva secundaria interior que sigue ~40% del contorno dando profundidad e identidad
        val innerPath = Path().apply {
            moveTo(11.8f * scaleX, 9.2f * scaleY)
            cubicTo(
                9.6f * scaleX, 12.0f * scaleY,
                8.8f * scaleX, 14.5f * scaleY,
                9.8f * scaleX, 17.0f * scaleY
            )
        }

        drawPath(
            path = innerPath,
            color = tint.copy(alpha = innerAlpha),
            style = Stroke(
                width = strokePx * 0.85f,
                cap = StrokeCap.Round
            )
        )
    }
}
