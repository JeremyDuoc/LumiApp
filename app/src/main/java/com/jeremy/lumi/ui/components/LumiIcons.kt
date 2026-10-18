package com.jeremy.lumi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sistema de iconografía propia y distintiva de Lumi.
 *
 * Elimina completamente emojis e iconos Material genéricos para las acciones clave:
 * - Mi día: diario/registro personal orgánico con sparkle Lumi integrado.
 * - Periodo / Me llegó: gota asimétrica Lumi con curvatura orgánica e incisión de luz.
 * - Calendario: versión minimalista con dos uniones superiores y foco único interior.
 * - Insight: glifo abstracto de 4 puntas orbital de la marca Lumi.
 * - Info: símbolo ⓘ sutil para chips de estado.
 * - Chevron: flecha elegante para navegación.
 */

@Composable
fun LumiIconMyDay(
    modifier: Modifier = Modifier,
    tint: Color,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 1.6.dp.toPx()

        // 1. Silueta de diario/cuaderno con esquinas redondeadas
        val pageLeft = w * 0.20f
        val pageTop = h * 0.14f
        val pageRight = w * 0.80f
        val pageBottom = h * 0.86f
        val corner = 3.5.dp.toPx()

        drawRoundRect(
            color = tint,
            topLeft = Offset(pageLeft, pageTop),
            size = Size(pageRight - pageLeft, pageBottom - pageTop),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeWidth)
        )

        // 2. Micro sparkle Lumi integrado en la parte superior derecha
        val sparkleCx = w * 0.58f
        val sparkleCy = h * 0.36f
        val sr = w * 0.11f
        val sk = sr * 0.24f

        val sparklePath = Path().apply {
            moveTo(sparkleCx, sparkleCy - sr)
            cubicTo(sparkleCx + sk, sparkleCy - sk, sparkleCx + sk, sparkleCy - sk, sparkleCx + sr, sparkleCy)
            cubicTo(sparkleCx + sk, sparkleCy + sk, sparkleCx + sk, sparkleCy + sk, sparkleCx, sparkleCy + sr)
            cubicTo(sparkleCx - sk, sparkleCy + sk, sparkleCx - sk, sparkleCy + sk, sparkleCx - sr, sparkleCy)
            cubicTo(sparkleCx - sk, sparkleCy - sk, sparkleCx - sk, sparkleCy - sk, sparkleCx, sparkleCy - sr)
            close()
        }
        drawPath(sparklePath, color = tint, style = Fill)

        // 3. Dos líneas orgánicas de registro personal
        drawLine(
            color = tint,
            start = Offset(w * 0.34f, h * 0.56f),
            end = Offset(w * 0.66f, h * 0.56f),
            strokeWidth = strokeWidth * 0.9f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.34f, h * 0.70f),
            end = Offset(w * 0.52f, h * 0.70f),
            strokeWidth = strokeWidth * 0.9f,
            cap = StrokeCap.Round
        )

        // 4. Pequeña muesca/anilla izquierda que da carácter de cuaderno personal
        drawCircle(
            color = tint,
            radius = strokeWidth * 0.7f,
            center = Offset(pageLeft, h * 0.38f)
        )
        drawCircle(
            color = tint,
            radius = strokeWidth * 0.7f,
            center = Offset(pageLeft, h * 0.62f)
        )
    }
}

@Composable
fun LumiIconPeriod(
    modifier: Modifier = Modifier,
    tint: Color,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 1.7.dp.toPx()

        // 1. Contorno de la gota Lumi (con sutil asimetría orgánica y curvas suaves)
        val dropPath = Path().apply {
            val topX = w * 0.50f
            val topY = h * 0.12f

            moveTo(topX, topY)
            // Lado izquierdo descendente
            cubicTo(
                w * 0.38f, h * 0.28f,
                w * 0.18f, h * 0.50f,
                w * 0.18f, h * 0.66f
            )
            // Curva inferior redondeada
            cubicTo(
                w * 0.18f, h * 0.86f,
                w * 0.34f, h * 0.88f,
                w * 0.50f, h * 0.88f
            )
            // Subida por el lado derecho
            cubicTo(
                w * 0.66f, h * 0.88f,
                w * 0.82f, h * 0.86f,
                w * 0.82f, h * 0.66f
            )
            cubicTo(
                w * 0.82f, h * 0.50f,
                w * 0.62f, h * 0.28f,
                topX, topY
            )
            close()
        }

        // Relleno translúcido muy suave
        drawPath(
            dropPath,
            color = tint.copy(alpha = 0.18f),
            style = Fill
        )

        // Contorno
        drawPath(
            dropPath,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 2. Incisión / highlight de luz interior en el flanco superior izquierdo
        val highlightPath = Path().apply {
            moveTo(w * 0.32f, h * 0.44f)
            cubicTo(
                w * 0.28f, h * 0.54f,
                w * 0.30f, h * 0.68f,
                w * 0.38f, h * 0.74f
            )
        }
        drawPath(
            highlightPath,
            color = tint.copy(alpha = 0.90f),
            style = Stroke(
                width = strokeWidth * 0.85f,
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun LumiIconCalendar(
    modifier: Modifier = Modifier,
    tint: Color,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 1.6.dp.toPx()

        val left = w * 0.18f
        val top = h * 0.22f
        val right = w * 0.82f
        val bottom = h * 0.86f
        val corner = 3.5.dp.toPx()

        // Marco del calendario
        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeWidth)
        )

        // Línea divisoria superior sutil
        drawLine(
            color = tint.copy(alpha = 0.5f),
            start = Offset(left, h * 0.40f),
            end = Offset(right, h * 0.40f),
            strokeWidth = strokeWidth * 0.75f
        )

        // Dos uniones superiores Lumi (pequeñas anillas)
        val hook1X = w * 0.36f
        val hook2X = w * 0.64f
        val hookTop = h * 0.10f
        val hookBottom = h * 0.24f

        drawLine(
            color = tint,
            start = Offset(hook1X, hookTop),
            end = Offset(hook1X, hookBottom),
            strokeWidth = strokeWidth * 1.1f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(hook2X, hookTop),
            end = Offset(hook2X, hookBottom),
            strokeWidth = strokeWidth * 1.1f,
            cap = StrokeCap.Round
        )

        // Único foco interior: un punto central orgánico
        drawCircle(
            color = tint,
            radius = w * 0.09f,
            center = Offset(w * 0.50f, h * 0.63f)
        )
    }
}

@Composable
fun LumiIconInsight(
    modifier: Modifier = Modifier,
    tint: Color,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.48f
        val cy = h * 0.52f
        val r = w * 0.36f
        val k = r * 0.26f

        // Glifo abstracto de 4 puntas cóncavas orgánicas
        val starPath = Path().apply {
            moveTo(cx, cy - r)
            cubicTo(cx + k, cy - k, cx + k, cy - k, cx + r, cy)
            cubicTo(cx + k, cy + k, cx + k, cy + k, cx, cy + r)
            cubicTo(cx - k, cy + k, cx - k, cy + k, cx - r, cy)
            cubicTo(cx - k, cy - k, cx - k, cy - k, cx, cy - r)
            close()
        }
        drawPath(starPath, color = tint, style = Fill)

        // Micro punto orbital superior derecho
        val orbitX = cx + r * 0.68f
        val orbitY = cy - r * 0.68f
        drawCircle(
            color = tint,
            radius = w * 0.065f,
            center = Offset(orbitX, orbitY)
        )
    }
}

@Composable
fun LumiIconInfo(
    modifier: Modifier = Modifier,
    tint: Color,
    size: Dp = 14.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 1.2.dp.toPx()
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = (w * 0.42f)

        // Círculo exterior fino
        drawCircle(
            color = tint,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidth)
        )

        // Punto superior
        drawCircle(
            color = tint,
            radius = strokeWidth * 0.75f,
            center = Offset(cx, cy - r * 0.38f)
        )

        // Línea vertical
        drawLine(
            color = tint,
            start = Offset(cx, cy - r * 0.05f),
            end = Offset(cx, cy + r * 0.48f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun LumiIconChevronRight(
    modifier: Modifier = Modifier,
    tint: Color,
    size: Dp = 16.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 1.6.dp.toPx()

        val chevronPath = Path().apply {
            moveTo(w * 0.38f, h * 0.28f)
            lineTo(w * 0.62f, h * 0.50f)
            lineTo(w * 0.38f, h * 0.72f)
        }

        drawPath(
            path = chevronPath,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
