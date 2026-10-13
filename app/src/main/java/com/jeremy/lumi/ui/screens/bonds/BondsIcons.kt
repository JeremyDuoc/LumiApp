package com.jeremy.lumi.ui.screens.bonds

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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jeremy.lumi.domain.model.CareAction

/**
 * Iconografía vectorial nativa y limpia para los gestos de Bonds:
 * HUG, TEA, COFFEE, CHOCOLATE, HEATING_PAD, PHARMACY.
 */
@Composable
fun BondsGestureIcon(
    action: CareAction,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        when (action) {
            CareAction.HUG -> drawHugIcon(tint, strokeWidth)
            CareAction.TEA -> drawTeaIcon(tint, strokeWidth)
            CareAction.COFFEE -> drawCoffeeIcon(tint, strokeWidth)
            CareAction.CHOCOLATE -> drawChocolateIcon(tint, strokeWidth)
            CareAction.HEATING_PAD -> drawHeatingPadIcon(tint, strokeWidth)
            CareAction.PHARMACY -> drawPharmacyIcon(tint, strokeWidth)
        }
    }
}

private fun DrawScope.drawHugIcon(tint: Color, strokeWidth: Float) {
    val w = size.width
    val h = size.height

    // Corazón central
    val heartPath = Path().apply {
        val cx = w * 0.5f
        val cy = h * 0.45f
        val hs = w * 0.28f
        moveTo(cx, cy + hs * 0.7f)
        cubicTo(cx - hs, cy + hs * 0.1f, cx - hs * 0.9f, cy - hs * 0.6f, cx, cy - hs * 0.2f)
        cubicTo(cx + hs * 0.9f, cy - hs * 0.6f, cx + hs, cy + hs * 0.1f, cx, cy + hs * 0.7f)
        close()
    }
    drawPath(heartPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

    // Manos/brazos envolventes suaves abajo
    val leftArm = Path().apply {
        moveTo(w * 0.18f, h * 0.72f)
        quadraticTo(w * 0.32f, h * 0.88f, w * 0.48f, h * 0.86f)
    }
    drawPath(leftArm, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

    val rightArm = Path().apply {
        moveTo(w * 0.82f, h * 0.72f)
        quadraticTo(w * 0.68f, h * 0.88f, w * 0.52f, h * 0.86f)
    }
    drawPath(rightArm, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

private fun DrawScope.drawTeaIcon(tint: Color, strokeWidth: Float) {
    val w = size.width
    val h = size.height

    // Taza / bol de té
    val cupPath = Path().apply {
        moveTo(w * 0.24f, h * 0.42f)
        lineTo(w * 0.76f, h * 0.42f)
        cubicTo(w * 0.76f, h * 0.76f, w * 0.66f, h * 0.82f, w * 0.5f, h * 0.82f)
        cubicTo(w * 0.34f, h * 0.82f, w * 0.24f, h * 0.76f, w * 0.24f, h * 0.42f)
        close()
    }
    drawPath(cupPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

    // Asa de la taza
    val handlePath = Path().apply {
        moveTo(w * 0.76f, h * 0.48f)
        cubicTo(w * 0.90f, h * 0.50f, w * 0.90f, h * 0.68f, w * 0.72f, h * 0.72f)
    }
    drawPath(handlePath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

    // Vapor ondulante 1
    val steam1 = Path().apply {
        moveTo(w * 0.42f, h * 0.32f)
        cubicTo(w * 0.38f, h * 0.24f, w * 0.46f, h * 0.18f, w * 0.42f, h * 0.12f)
    }
    drawPath(steam1, color = tint, style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round))

    // Vapor ondulante 2
    val steam2 = Path().apply {
        moveTo(w * 0.58f, h * 0.32f)
        cubicTo(w * 0.54f, h * 0.24f, w * 0.62f, h * 0.18f, w * 0.58f, h * 0.12f)
    }
    drawPath(steam2, color = tint, style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round))
}

private fun DrawScope.drawCoffeeIcon(tint: Color, strokeWidth: Float) {
    val w = size.width
    val h = size.height

    // Taza de café más recta
    val mugPath = Path().apply {
        moveTo(w * 0.28f, h * 0.36f)
        lineTo(w * 0.72f, h * 0.36f)
        lineTo(w * 0.70f, h * 0.78f)
        cubicTo(w * 0.70f, h * 0.84f, w * 0.30f, h * 0.84f, w * 0.30f, h * 0.78f)
        close()
    }
    drawPath(mugPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

    // Asa
    val handle = Path().apply {
        moveTo(w * 0.71f, h * 0.44f)
        cubicTo(w * 0.88f, h * 0.46f, w * 0.88f, h * 0.66f, w * 0.70f, h * 0.70f)
    }
    drawPath(handle, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

    // Vapor único
    val steam = Path().apply {
        moveTo(w * 0.50f, h * 0.28f)
        cubicTo(w * 0.45f, h * 0.20f, w * 0.55f, h * 0.16f, w * 0.50f, h * 0.10f)
    }
    drawPath(steam, color = tint, style = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round))
}

private fun DrawScope.drawChocolateIcon(tint: Color, strokeWidth: Float) {
    val w = size.width
    val h = size.height

    // Tableta de chocolate inclinada
    val left = w * 0.28f
    val top = h * 0.22f
    val barW = w * 0.44f
    val barH = h * 0.56f
    val corner = 4.dp.toPx()

    drawRoundRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = strokeWidth)
    )

    // Líneas divisorias de la cuadrícula
    // Horizontal
    drawLine(
        color = tint,
        start = Offset(left, top + barH * 0.33f),
        end = Offset(left + barW, top + barH * 0.33f),
        strokeWidth = strokeWidth * 0.85f
    )
    drawLine(
        color = tint,
        start = Offset(left, top + barH * 0.66f),
        end = Offset(left + barW, top + barH * 0.66f),
        strokeWidth = strokeWidth * 0.85f
    )
    // Vertical
    drawLine(
        color = tint,
        start = Offset(left + barW * 0.5f, top),
        end = Offset(left + barW * 0.5f, top + barH),
        strokeWidth = strokeWidth * 0.85f
    )
}

private fun DrawScope.drawHeatingPadIcon(tint: Color, strokeWidth: Float) {
    val w = size.width
    val h = size.height

    // Tres líneas ondulantes de calor suave
    for (i in 0..2) {
        val xOffset = w * (0.32f + i * 0.18f)
        val wavePath = Path().apply {
            moveTo(xOffset, h * 0.72f)
            cubicTo(xOffset - w * 0.06f, h * 0.58f, xOffset + w * 0.06f, h * 0.42f, xOffset, h * 0.28f)
        }
        drawPath(wavePath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawPharmacyIcon(tint: Color, strokeWidth: Float) {
    val w = size.width
    val h = size.height

    // Cápsula inclinada o cruz de bienestar
    // Cápsula redondeada
    val capW = w * 0.48f
    val capH = h * 0.26f
    val left = w * 0.26f
    val top = h * 0.37f

    drawRoundRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(capW, capH),
        cornerRadius = CornerRadius(capH / 2f, capH / 2f),
        style = Stroke(width = strokeWidth)
    )

    // Línea divisoria en la mitad de la cápsula
    drawLine(
        color = tint,
        start = Offset(left + capW * 0.5f, top),
        end = Offset(left + capW * 0.5f, top + capH),
        strokeWidth = strokeWidth
    )
}
