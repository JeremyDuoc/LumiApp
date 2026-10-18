package com.jeremy.lumi.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset

object LumiMotion {
    val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.30f, 1f)
    val EaseInOutQuart = CubicBezierEasing(0.76f, 0f, 0.24f, 1f)
    val SpatialSpring = spring<Float>(dampingRatio = 0.84f, stiffness = 340f)
    val GentleSpring = spring<Float>(dampingRatio = 0.92f, stiffness = 220f)
    val SpatialColorSpring = spring<Color>(dampingRatio = 0.84f, stiffness = 340f)
    val CycleSpring = spring<Float>(dampingRatio = 0.88f, stiffness = 140f)
    val StepSlide = tween<IntOffset>(380, easing = EaseOutExpo)
    val StepExitSlide = tween<IntOffset>(220, easing = EaseOutExpo)
    val StepFade = tween<Float>(380, easing = EaseOutExpo)
    val ShortFade = tween<Float>(220)
    val SummaryExit = tween<Float>(280, easing = EaseOutExpo)
    val Bloom = tween<Float>(900, easing = EaseOutExpo)
    val BloomHandoff = tween<Float>(520, easing = LinearEasing)
    val NavEnterFade = tween<Float>(550, easing = EaseOutExpo)
    val NavEnterScale = tween<Float>(620, easing = EaseOutExpo)
    val NavExitFade = tween<Float>(420, easing = EaseInOutQuart)
    val NavExitScale = tween<Float>(450, easing = EaseInOutQuart)
    val DotBreathing = infiniteRepeatable(tween<Float>(2250, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    fun homeEntrance(delayMs: Int) = tween<Float>(520, delayMillis = delayMs, easing = EaseOutExpo)
}

fun Modifier.lumiPressScale(interactionSource: InteractionSource, targetScale: Float = 0.96f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(if (pressed) targetScale else 1f, LumiMotion.SpatialSpring, label = "lumi_press")
    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}
