package com.jeremy.lumi.ui.screens.onboarding

import android.animation.ValueAnimator
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.components.LumiBrandLockup
import com.jeremy.lumi.ui.theme.LumiEditorialSerif
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import kotlinx.coroutines.isActive
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One clock owns the farewell, travelling brand and reveal of the real Home. */
internal object LumiCeremonyTiming {
    const val INTRO_END = 1200f
    const val OPAQUE_AT = 900f
    const val MESSAGE_EXIT = 11700f
    const val FLIGHT_START = 12000f
    const val FLIGHT_DURATION = 1250f
    const val REVEAL_DURATION = 950f
    const val DURATION = 13800f
    val messageStarts = listOf(1400f, 3550f, 5700f, 7850f, 10000f)
}

private val ceremonyTexts = listOf(
    R.string.atmospheric_title_1 to R.string.atmospheric_sub_1,
    R.string.atmospheric_title_2 to R.string.atmospheric_sub_2,
    R.string.atmospheric_title_3 to R.string.atmospheric_sub_3,
    R.string.atmospheric_title_4 to R.string.atmospheric_sub_4,
    R.string.atmospheric_title_5 to R.string.atmospheric_sub_5
)
private val gatherCurve = CubicBezierEasing(.22f, .72f, .18f, 1f)
private val flightCurve = CubicBezierEasing(.42f, 0f, .18f, 1f)
private val revealCurve = CubicBezierEasing(.25f, 0f, .25f, 1f)

private fun fraction(time: Float, start: Float, duration: Float) =
    ((time - start) / duration).coerceIn(0f, 1f)

private fun soft(time: Float, start: Float, duration: Float): Float {
    val t = fraction(time, start, duration)
    return t * t * (3f - 2f * t)
}

@Composable
fun LumiAtmosphericTransition(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = !ValueAnimator.areAnimatorsEnabled(),
    sourceBrandBounds: Rect? = null,
    targetBrandBounds: Rect? = null,
    onRevealStart: () -> Unit = {},
    homeRevealContent: (@Composable () -> Unit)? = null,
    revealUnderlyingHome: Boolean = false,
    onCurtainOpaque: () -> Unit = {},
    homeReady: Boolean = true,
    skipRequested: Boolean = false,
    onRevealProgress: (Float) -> Unit = {}
) {
    val tokens = LumiOnboardingThemeTokens.current
    val homeTokens = LumiThemeTokens.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestFinished by rememberUpdatedState(onFinished)
    val latestRevealStart by rememberUpdatedState(onRevealStart)
    val latestCurtainOpaque by rememberUpdatedState(onCurtainOpaque)
    val latestProgress by rememberUpdatedState(onRevealProgress)
    val latestHomeReady by rememberUpdatedState(homeReady)
    var elapsedMs by remember { mutableFloatStateOf(0f) }
    var skipAt by remember { mutableStateOf<Float?>(null) }
    var finished by remember { mutableStateOf(false) }
    var layerOrigin by remember { mutableStateOf(Offset.Zero) }

    fun requestArrival() {
        // Skipping takes the same short landing path, without tearing away the curtain.
        if (skipAt == null && elapsedMs < LumiCeremonyTiming.MESSAGE_EXIT) {
            skipAt = maxOf(elapsedMs, LumiCeremonyTiming.INTRO_END)
        }
    }
    LaunchedEffect(skipRequested) { if (skipRequested) requestArrival() }

    val messageExit = skipAt ?: LumiCeremonyTiming.MESSAGE_EXIT
    val flightStart = skipAt?.plus(300f) ?: LumiCeremonyTiming.FLIGHT_START
    // Let the wordmark clear the greeting's vertical band before revealing it.
    val revealStart = flightStart + 700f
    val duration = skipAt?.plus(2100f) ?: LumiCeremonyTiming.DURATION
    val latestRevealAt by rememberUpdatedState(revealStart)
    val latestDuration by rememberUpdatedState(duration)

    LaunchedEffect(reducedMotion, lifecycleOwner) {
        var curtainNotified = false
        var revealNotified = false
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var previousNs = withFrameNanos { it }
            while (isActive && !finished) {
                val nowNs = withFrameNanos { it }
                // Follow real frame time; a lifecycle restart resets only the frame origin.
                val delta = (nowNs - previousNs) / 1_000_000f
                previousNs = nowNs
                val next = if (reducedMotion) latestDuration else elapsedMs + delta
                if (!curtainNotified && next >= LumiCeremonyTiming.OPAQUE_AT) {
                    curtainNotified = true
                    latestCurtainOpaque()
                }
                // Data and destination geometry must be ready before exposing Home.
                elapsedMs = if (!latestHomeReady) minOf(next, latestRevealAt) else next
                if (elapsedMs >= latestRevealAt && latestHomeReady) {
                    if (!revealNotified) {
                        revealNotified = true
                        latestRevealStart()
                    }
                    latestProgress(
                        if (reducedMotion) 1f
                        else fraction(elapsedMs, latestRevealAt, LumiCeremonyTiming.REVEAL_DURATION)
                    )
                }
                if (elapsedMs >= latestDuration && latestHomeReady) {
                    latestProgress(1f)
                    finished = true
                    latestFinished()
                }
            }
        }
    }

    val intro = gatherCurve.transform(fraction(elapsedMs, 0f, LumiCeremonyTiming.INTRO_END))
    val flight = flightCurve.transform(fraction(elapsedMs, flightStart, LumiCeremonyTiming.FLIGHT_DURATION))
    val reveal = revealCurve.transform(fraction(elapsedMs, revealStart, LumiCeremonyTiming.REVEAL_DURATION))
    val farewell = 1f - soft(elapsedMs, messageExit, 300f)
    val bloom = soft(elapsedMs, 150f, 1450f) * (1f - soft(elapsedMs, flightStart + 180f, 1400f))

    BoxWithConstraints(
        modifier.fillMaxSize().onGloballyPositioned { layerOrigin = it.positionInRoot() }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val dp = density.density
        val center = Offset(widthPx * .5f, heightPx * .39f)

        // Own touch input even while the curtain is transparent.
        Box(
            Modifier.fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .clearAndSetSemantics {}
        )
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    alpha = if (reducedMotion) 1f
                    else soft(elapsedMs, 0f, LumiCeremonyTiming.OPAQUE_AT) * (1f - reveal)
                }
                .background(tokens.bg)
        )
        if (!revealUnderlyingHome && homeRevealContent != null && elapsedMs >= revealStart) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = reveal }) { homeRevealContent() }
        }

        if (!reducedMotion) {
            CeremonyLight(
                elapsedMs = elapsedMs,
                opacity = bloom,
                flight = flight,
                center = center,
                dark = tokens.isDark,
                accent = tokens.accent,
                modifier = Modifier.fillMaxSize()
            )

            val source = sourceBrandBounds?.translate(-layerOrigin)
            val target = targetBrandBounds?.translate(-layerOrigin)
            val sourceCenter = source?.center ?: Offset(widthPx * .5f, 126f * dp)
            val gatheredCenter = Offset(
                lerp(sourceCenter.x, center.x, intro),
                lerp(sourceCenter.y, center.y, intro)
            )
            val destination = target?.center ?: Offset(52f * dp, 54f * dp)
            // A restrained bow keeps the flight away from the greeting's left edge.
            val bow = sin(PI.toFloat() * flight) * widthPx * .055f
            val brandCenter = Offset(
                lerp(gatheredCenter.x, destination.x, flight) + bow,
                lerp(gatheredCenter.y, destination.y, flight)
            )
            val breath = sin((elapsedMs - LumiCeremonyTiming.INTRO_END).coerceAtLeast(0f) / 4200f * PI.toFloat())
            val brandScale = lerp(1f + .12f * intro + .008f * breath * intro, 1f, flight)
            // Measure and place in the SAME layout pass: no frame-late size feedback,
            // no top-left scaling drift, exactly the TopBar metrics at touchdown.
            Layout(
                content = {
                    LumiBrandLockup(
                        flowerSize = 46,
                        fontSize = lerp(42f, 24f, flight).sp,
                        starSize = lerp(16f, 11f, flight).sp,
                        letterSpacing = lerp(-2.8f, -1.5f, flight).sp,
                        flowerVisibility = 1f - soft(flight, .08f, .64f),
                        textColor = lerpColor(tokens.text, homeTokens.text, flight),
                        starColor = lerpColor(tokens.accent, homeTokens.accent, flight),
                        ceremonial = true,
                        motionProgress = (.5f - .5f * cos(elapsedMs / 3200f * PI.toFloat())) * (1f - flight),
                        animated = true
                    )
                },
                modifier = Modifier.fillMaxSize().clearAndSetSemantics {}
            ) { measurables, limits ->
                val brand = measurables.single().measure(limits.copy(minWidth = 0, minHeight = 0))
                layout(limits.maxWidth, limits.maxHeight) {
                    brand.placeWithLayer(0, 0) {
                        translationX = brandCenter.x - brand.width / 2f
                        translationY = brandCenter.y - brand.height / 2f
                        scaleX = brandScale
                        scaleY = brandScale
                        // Observer Home has no TopBar brand to receive the lockup.
                        alpha = if (target == null) 1f - reveal else 1f
                    }
                }
            }

            val messageIndex = LumiCeremonyTiming.messageStarts.indexOfLast { elapsedMs >= it }
            if (messageIndex >= 0 && farewell > 0f) {
                val start = LumiCeremonyTiming.messageStarts[messageIndex]
                val next = LumiCeremonyTiming.messageStarts.getOrNull(messageIndex + 1) ?: messageExit
                val appearing = soft(elapsedMs, start, 520f)
                val disappearing = soft(elapsedMs, next - 280f, 280f)
                Column(
                    Modifier.fillMaxWidth()
                        .offset(y = maxHeight * .54f)
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Reserve breathing room, but allow wrapped accessibility text to
                    // expand this area and carry the lights down instead of overlapping.
                    Column(
                        Modifier.heightIn(min = 84.dp * density.fontScale)
                            .graphicsLayer {
                                alpha = appearing * (1f - disappearing) * farewell
                                translationY = (10f * (1f - appearing) - 5f * disappearing) * dp
                            }
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(ceremonyTexts[messageIndex].first),
                            fontFamily = LumiEditorialSerif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 23.sp,
                            lineHeight = 29.sp,
                            color = tokens.text,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(ceremonyTexts[messageIndex].second),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = tokens.muted,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    // Five quiet lights mark the phrases without suggesting network loading.
                    Canvas(
                        Modifier.size(92.dp, 16.dp)
                            .testTag("ceremony_progress")
                            .graphicsLayer { alpha = soft(elapsedMs, 1600f, 500f) * farewell }
                    ) {
                        val spacing = size.width / 5f
                        repeat(5) { i ->
                            val lit = soft(elapsedMs, LumiCeremonyTiming.messageStarts[i], 600f)
                            val point = Offset(spacing * (i + .5f), size.height / 2f)
                            drawCircle(tokens.accent.copy(alpha = .18f + .62f * lit), (1.6f + .5f * lit).dp.toPx(), point)
                            if (lit > 0f) {
                                drawCircle(
                                    Brush.radialGradient(
                                        listOf(tokens.accent.copy(alpha = .16f * lit), Color.Transparent),
                                        point, 7.dp.toPx()
                                    ), 7.dp.toPx(), point
                                )
                            }
                        }
                    }
                }
            }
        }

        if (elapsedMs >= LumiCeremonyTiming.INTRO_END && elapsedMs < messageExit && !reducedMotion) {
            TextButton(
                onClick = { requestArrival() },
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 20.dp)
                    .graphicsLayer { alpha = soft(elapsedMs, LumiCeremonyTiming.INTRO_END, 400f) }
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.atmospheric_enter_now), fontSize = 12.sp, color = tokens.muted)
            }
        }
    }
}

/**
 * Four pigment lights in a slow elliptical orbit. Gradients provide softness
 * without full-screen blur or clipped glow rectangles.
 */
@Composable
private fun CeremonyLight(
    elapsedMs: Float,
    opacity: Float,
    flight: Float,
    center: Offset,
    dark: Boolean,
    accent: Color,
    modifier: Modifier
) {
    val colors = remember { listOf(Color(0xFFE58DBB), Color(0xFF9B80E8), Color(0xFF88CDBB), Color(0xFFE8B18D)) }
    val orbitPath = remember { Path() }
    Canvas(modifier.graphicsLayer { alpha = opacity }) {
        val unit = minOf(size.width, size.height * .62f)
        val breathing = .5f - .5f * cos(elapsedMs / 4800f * PI.toFloat())
        val orbit = elapsedMs / 19000f * 2f * PI.toFloat()
        val lightCenter = Offset(center.x, center.y + unit * .03f + flight * unit * .12f)
        colors.forEachIndexed { i, color ->
            val angle = orbit + i * PI.toFloat() / 2f - 2.3f
            val point = lightCenter + Offset(
                cos(angle) * unit * (.15f + .025f * breathing),
                sin(angle) * unit * (.18f + .025f * breathing)
            )
            val radius = unit * (.44f + .035f * sin(angle + orbit))
            withTransform({ scale(1f, 1.14f, point) }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to color.copy(alpha = if (dark) .30f else .20f),
                        .30f to color.copy(alpha = if (dark) .18f else .11f),
                        .65f to color.copy(alpha = if (dark) .045f else .025f),
                        1f to color.copy(alpha = 0f),
                        center = point, radius = radius
                    ),
                    radius = radius, center = point
                )
            }
        }
        // Asymmetric contours echo Lumi's living cell.
        repeat(2) { ring ->
            orbitPath.reset()
            val radius = unit * (.335f + ring * .045f + .012f * breathing)
            for (point in 0..96) {
                val angle = point / 96f * 2f * PI.toFloat()
                val ripple = 1f + .045f * sin(angle * 3f + orbit) + .025f * cos(angle * 2f - orbit)
                val x = lightCenter.x + cos(angle) * radius * ripple
                val y = lightCenter.y + sin(angle) * radius * ripple * 1.08f
                if (point == 0) orbitPath.moveTo(x, y) else orbitPath.lineTo(x, y)
            }
            orbitPath.close()
            rotate(12f * sin(orbit) + ring * 28f, lightCenter) {
                drawPath(
                    orbitPath,
                    accent.copy(alpha = (if (dark) .085f else .10f) * (1f - flight) * (.65f + .35f * breathing)),
                    style = Stroke(.65.dp.toPx())
                )
            }
        }
        // Fixed constellation: no reseeding or glitter bursts between frames.
        repeat(14) { i ->
            val angle = i * 2.39996f + orbit * .07f
            val radius = unit * (.31f + (i % 4) * .075f)
            val point = lightCenter + Offset(cos(angle) * radius, sin(angle) * radius * 1.28f)
            val twinkle = .5f + .5f * sin(elapsedMs / 1800f + i * 1.7f)
            val alpha = (if (dark) .20f else .14f) * (.35f + .65f * twinkle) * (1f - flight)
            drawCircle(accent.copy(alpha = alpha), (.7f + .25f * (i % 3)).dp.toPx(), point)
            if (i % 5 == 0) {
                val arm = (2f + twinkle).dp.toPx()
                drawLine(accent.copy(alpha = alpha), point - Offset(arm, 0f), point + Offset(arm, 0f), .6.dp.toPx())
                drawLine(accent.copy(alpha = alpha), point - Offset(0f, arm), point + Offset(0f, arm), .6.dp.toPx())
            }
        }
    }
}
