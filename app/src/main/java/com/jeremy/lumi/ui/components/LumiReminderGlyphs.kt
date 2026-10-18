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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jeremy.lumi.data.local.entity.ReminderType

/**
 * Familia de glifos propios de recordatorios Lumi — sin iconos genéricos de Material ni emojis.
 * Basados en la geometría exacta 24x24 del prototipo lumi_reminders_reference.html.
 */
object LumiReminderGlyphPaths {
    val periodDroplet by lazy { PathParser().parsePathString("M12 3.2c3.5 4.2 6.1 7.2 6.1 10.5A6.1 6.1 0 1 1 5.9 13.7C5.9 10.4 8.5 7.4 12 3.2Z").toPath() }
    val periodInner by lazy { PathParser().parsePathString("M9.1 15.6c.8 1 1.8 1.5 3 1.5").toPath() }

    val ovuStem by lazy { PathParser().parsePathString("M12 20V9").toPath() }
    val ovuLeafLeft by lazy { PathParser().parsePathString("M11.8 11.2C7 11 4.8 8.5 4.4 4.3c4.5.2 7.1 2.6 7.4 6.9Z").toPath() }
    val ovuLeafRight by lazy { PathParser().parsePathString("M12.2 13c4.4-.3 6.7-2.6 7.3-6.4-4.2.2-6.7 2.3-7.3 6.4Z").toPath() }

    val journalSpine by lazy { PathParser().parsePathString("M8.2 4v16").toPath() }
    val journalLine1 by lazy { PathParser().parsePathString("M11 9h4.6").toPath() }
    val journalLine2 by lazy { PathParser().parsePathString("M11 13h3.5").toPath() }

    val bagBody by lazy { PathParser().parsePathString("M5.4 8.4h13.2l-1 11H6.4l-1-11Z").toPath() }
    val bagHandle by lazy { PathParser().parsePathString("M9 8.3V7a3 3 0 0 1 6 0v1.3").toPath() }
    val bagCross by lazy { PathParser().parsePathString("M12 11.5v4M10 13.5h4").toPath() }

    val pillOutline by lazy { PathParser().parsePathString("M7.3 16.7a4.1 4.1 0 0 1 0-5.8l3.6-3.6a4.1 4.1 0 1 1 5.8 5.8l-3.6 3.6a4.1 4.1 0 0 1-5.8 0Z").toPath() }
    val pillSeam by lazy { PathParser().parsePathString("m9 9 6 6").toPath() }

    val patchPolygon by lazy { PathParser().parsePathString("m12 3 3.2 4.2L21 8.8 16.9 12l1.3 5.7L12 16l-5.1 1.7L7.1 12 3 8.8l5.8-1.6L12 3Z").toPath() }

    val ringMarks by lazy { PathParser().parsePathString("M17.3 8.1 19 6.4M5 17.6l1.7-1.7").toPath() }

    val injBody by lazy { PathParser().parsePathString("m9 5 10 10M14.5 4.5l5 5M7.5 7.5l9 9-3.7 3.2-8.5-8.5 3.2-3.7Z").toPath() }
    val injNeedle by lazy { PathParser().parsePathString("m4 16-1.5 1.5M6 18l-2.6 2.6").toPath() }

    val customSheet by lazy { PathParser().parsePathString("M5 5.5h14v13H5z").toPath() }
    val customBinding by lazy { PathParser().parsePathString("M8 3.8v3.4M16 3.8v3.4M5 9h14").toPath() }
    val customPencil by lazy { PathParser().parsePathString("m10 15 5-5 1.7 1.7-5 5H10V15Z").toPath() }
}

@Composable
fun LumiReminderGlyph(
    type: ReminderType,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, Offset.Zero) {
            when (type) {
                ReminderType.PERIOD_SOON -> drawPeriodGlyph(tint)
                ReminderType.OVULATION_SOON -> drawOvulationGlyph(tint)
                ReminderType.LOG_DAILY -> drawJournalGlyph(tint)
                ReminderType.SUPPLY_REMINDER -> drawBagGlyph(tint)
                ReminderType.PILL_DAILY -> drawPillGlyph(tint)
                ReminderType.PATCH_WEEKLY -> drawPatchGlyph(tint)
                ReminderType.RING_MONTHLY -> drawRingGlyph(tint)
                ReminderType.INJECTION_MONTHLY,
                ReminderType.INJECTION_QUARTERLY -> drawInjectionGlyph(tint)
                ReminderType.CUSTOM -> drawCustomGlyph(tint)
            }
        }
    }
}

private fun DrawScope.drawPeriodGlyph(tint: Color) {
    val stroke17 = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke16 = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(LumiReminderGlyphPaths.periodDroplet, color = tint, style = stroke17)
    drawPath(LumiReminderGlyphPaths.periodInner, color = tint, style = stroke16)
}

private fun DrawScope.drawOvulationGlyph(tint: Color) {
    val stroke17 = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke16 = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawLine(color = tint, start = Offset(12f, 20f), end = Offset(12f, 9f), strokeWidth = 1.7f, cap = StrokeCap.Round)
    drawPath(LumiReminderGlyphPaths.ovuLeafLeft, color = tint, style = stroke16)
    drawPath(LumiReminderGlyphPaths.ovuLeafRight, color = tint, style = stroke16)
}

private fun DrawScope.drawJournalGlyph(tint: Color) {
    val stroke16 = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke15 = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawRoundRect(
        color = tint,
        topLeft = Offset(5.2f, 3.8f),
        size = Size(13.6f, 16.4f),
        cornerRadius = CornerRadius(3.2f, 3.2f),
        style = stroke16
    )
    drawPath(LumiReminderGlyphPaths.journalSpine, color = tint, style = stroke15)
    drawPath(LumiReminderGlyphPaths.journalLine1, color = tint, style = stroke15)
    drawPath(LumiReminderGlyphPaths.journalLine2, color = tint, style = stroke15)
    drawCircle(color = tint, radius = 1.0f, center = Offset(16.2f, 6.9f))
}

private fun DrawScope.drawBagGlyph(tint: Color) {
    val stroke16 = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke15 = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(LumiReminderGlyphPaths.bagBody, color = tint, style = stroke16)
    drawPath(LumiReminderGlyphPaths.bagHandle, color = tint, style = stroke16)
    drawPath(LumiReminderGlyphPaths.bagCross, color = tint, style = stroke15)
}

private fun DrawScope.drawPillGlyph(tint: Color) {
    val stroke16 = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke15 = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(LumiReminderGlyphPaths.pillOutline, color = tint, style = stroke16)
    drawPath(LumiReminderGlyphPaths.pillSeam, color = tint, style = stroke15)
}

private fun DrawScope.drawPatchGlyph(tint: Color) {
    val stroke145 = Stroke(width = 1.45f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke135 = Stroke(width = 1.35f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(LumiReminderGlyphPaths.patchPolygon, color = tint, style = stroke145)
    drawCircle(color = tint, radius = 2.1f, center = Offset(12f, 11f), style = stroke135)
}

private fun DrawScope.drawRingGlyph(tint: Color) {
    val stroke17 = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke15 = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawCircle(color = tint, radius = 6.6f, center = Offset(12f, 12f), style = stroke17)
    drawPath(LumiReminderGlyphPaths.ringMarks, color = tint, style = stroke15)
}

private fun DrawScope.drawInjectionGlyph(tint: Color) {
    val stroke15 = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(LumiReminderGlyphPaths.injBody, color = tint, style = stroke15)
    drawPath(LumiReminderGlyphPaths.injNeedle, color = tint, style = stroke15)
}

private fun DrawScope.drawCustomGlyph(tint: Color) {
    val stroke16 = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke15 = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke135 = Stroke(width = 1.35f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(LumiReminderGlyphPaths.customSheet, color = tint, style = stroke16)
    drawPath(LumiReminderGlyphPaths.customBinding, color = tint, style = stroke15)
    drawPath(LumiReminderGlyphPaths.customPencil, color = tint, style = stroke135)
}
