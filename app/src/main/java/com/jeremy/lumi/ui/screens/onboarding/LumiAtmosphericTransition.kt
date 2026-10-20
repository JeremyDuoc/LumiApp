package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.jeremy.lumi.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.jeremy.lumi.ui.components.LumiBrandLockup
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Traducción del HTML Lumi v11/v12, secuencia ATMOSFÉRICA.
 *
 * Diferencias relevantes respecto al código anterior:
 *  - La pantalla previa permanece bajo un velo que aparece progresivamente.
 *  - Se usan CUATRO luces independientes, NO un halo circular único.
 *  - La marca parte de sus coordenadas reales (Step7Summary), no de 80.dp.
 *  - La marca se transforma desde TopStart, como CSS transform-origin: top left.
 *  - Las frases permanecen debajo del logo, con indicador de tres puntos.
 *  - El tiempo está sincronizado con los fotogramas de Compose, no con un
 *    bucle de System.currentTimeMillis() + delay(16).
 *
 * Para un handoff visual 100% continuo al Home real, suministrar homeRevealContent
 * desde un contenedor que pueda mantener el Home compuesto debajo del logo.
 * Sin ese contenedor la transición termina limpiamente en el fondo de Lumi,
 * pero la navegación final sigue siendo responsabilidad de onFinished.
 */
private val ceremonyTexts = listOf(
    R.string.atmospheric_title_1 to R.string.atmospheric_sub_1,
    R.string.atmospheric_title_2 to R.string.atmospheric_sub_2,
    R.string.atmospheric_title_3 to R.string.atmospheric_sub_3,
    R.string.atmospheric_title_4 to R.string.atmospheric_sub_4,
    R.string.atmospheric_title_5 to R.string.atmospheric_sub_5
)
private val messageStarts = listOf(1300f, 3450f, 5550f, 7780f, 9770f)
private val enterCurve = CubicBezierEasing(.18f, 1f, .3f, 1f)
private val firstFlightCurve = CubicBezierEasing(.16f, .89f, .24f, 1f)
private val lastFlightCurve = CubicBezierEasing(.17f, .79f, .21f, 1f)

private fun curve(t: Float, duration: Float, easing: androidx.compose.animation.core.Easing): Float =
    easing.transform((t / duration).coerceIn(0f, 1f))

private fun keyframes(f: Float, vararg stops: Pair<Float, Float>): Float {
    if (f <= stops.first().first) return stops.first().second
    for (i in 0 until stops.lastIndex) {
        val a = stops[i]
        val b = stops[i + 1]
        if (f <= b.first) {
            val t = ((f - a.first) / (b.first - a.first)).coerceIn(0f, 1f)
            return lerp(a.second, b.second, t)
        }
    }
    return stops.last().second
}

// Fidelity contract: CSS cubic-bezier(.42, 0, .58, 1), infinite alternate.
// Unlike the old cosine approximation, this uses Compose's cubic solver.
private val cssEaseInOut = CubicBezierEasing(.42f, 0f, .58f, 1f)
private fun cssAlternateEased(elapsedMs: Float, oneWayDurationMs: Float): Float {
    val phase = ((elapsedMs / oneWayDurationMs) % 2f).coerceAtLeast(0f)
    val normalized = if (phase <= 1f) phase else 2f - phase
    return cssEaseInOut.transform(normalized)
}

/**
 * CSS source of truth: `.aura-pink`, `.aura-violet`, `.aura-sage`,
 * `.aura-peach` and their `lumi*Orbit` keyframes from the v12 HTML.
 *
 * left/top refer to the element's pre-transform TOP-LEFT anchor; x/y
 * translate values are fractions of THE ORB'S dimensions (not screen dp).
 * Each CSS radial-gradient(circle) ends at the farthest corner.
 */
private data class CssAura(
    val id: String,
    val left: Float,
    val top: Float,
    val widthDp: Float,
    val heightDp: Float,
    val startTranslateX: Float,
    val startTranslateY: Float,
    val endTranslateX: Float,
    val endTranslateY: Float,
    val endScale: Float,
    val durationMs: Float,
    val inner: Color,
    val middle: Color,
    val middleStop: Float,
    val transparentStop: Float
)

private val cssAuras = listOf(
    CssAura("pink", .33f, .42f, 190f, 200f, -.50f, -.70f, -.24f, -.85f,
        1.14f, 6400f, Color(0x7AF563AC), Color(0x42E262AC), .37f, .73f),
    CssAura("violet", .69f, .42f, 198f, 210f, -.50f, -.65f, -.76f, -.54f,
        1.20f, 7300f, Color(0x789567F7), Color(0x3D9166EE), .40f, .74f),
    CssAura("sage", .64f, .53f, 167f, 160f, -.50f, -.45f, -.70f, -.78f,
        1.10f, 6900f, Color(0x6659DCBC), Color(0x2E5CD6B7), .40f, .76f),
    CssAura("peach", .37f, .55f, 170f, 165f, -.50f, -.40f, -.20f, -.20f,
        1.17f, 7800f, Color(0x6BFFA368), Color(0x2BFAA371), .42f, .76f)
)

@Composable
private fun CssAuraLayer(
    aura: CssAura,
    elapsedMs: Float,
    screenWidthPx: Float,
    screenHeightPx: Float,
    density: Float
) {
    val t = cssAlternateEased(elapsedMs, aura.durationMs)
    val orbWidthPx = aura.widthDp * density
    val orbHeightPx = aura.heightDp * density
    // CSS transform: translate(percentage of own size) scale(...)
    // Do NOT translate relative to the parent viewport or scale the offsets.
    val xPx = screenWidthPx * aura.left +
        orbWidthPx * lerp(aura.startTranslateX, aura.endTranslateX, t)
    val yPx = screenHeightPx * aura.top +
        orbHeightPx * lerp(aura.startTranslateY, aura.endTranslateY, t)
    val scale = lerp(1f, aura.endScale, t)
    Canvas(
        modifier = Modifier
            .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
            .size(aura.widthDp.dp, aura.heightDp.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            // CSS filter belongs to EACH orb. Do not blur the full canvas:
            // doing so creates the false downward-moving color cloud.
            .blur(30.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    ) {
        val cssRadius = hypot(size.width / 2f, size.height / 2f)
        drawRect(
            brush = Brush.radialGradient(
                0f to aura.inner,
                aura.middleStop to aura.middle,
                aura.transparentStop to Color.Transparent,
                1f to Color.Transparent,
                center = center,
                radius = cssRadius
            )
        )
    }
}

@Composable
fun LumiAtmosphericTransition(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    sourceBrandBounds: Rect? = null,
    targetBrandBounds: Rect? = null,
    onRevealStart: () -> Unit = {},
    homeRevealContent: (@Composable () -> Unit)? = null,
    revealUnderlyingHome: Boolean = false,
    onCurtainOpaque: () -> Unit = {},
    homeReady: Boolean = true,
    onBrandArrived: () -> Unit = {},
    forceSkip: Boolean = false
) {
    // One clock, with an explicit hold while the REAL Home becomes ready.
    // The ceremony never starts a second Home or hides missing data behind a delay.
    val tokens = LumiOnboardingThemeTokens.current
    val homeTokens = com.jeremy.lumi.ui.theme.LumiThemeTokens.current
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val durationScale = remember(context) {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ).coerceAtLeast(0f)
    }
    val latestFinished by rememberUpdatedState(onFinished)
    val latestReveal by rememberUpdatedState(onRevealStart)
    val latestCurtain by rememberUpdatedState(onCurtainOpaque)
    val latestBrand by rememberUpdatedState(onBrandArrived)
    var elapsedMs by remember { mutableFloatStateOf(0f) }
    var completed by remember { mutableStateOf(false) }
    var curtainNotified by remember { mutableStateOf(false) }
    var revealNotified by remember { mutableStateOf(false) }
    var brandNotified by remember { mutableStateOf(false) }
    var skipRequested by remember { mutableStateOf(false) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var brandSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    val coverEnd = 460f
    val centerStart = 300f
    val centerEnd = 1380f
    val welcomeStart = 1100f
    val welcomeExit = 3600f
    val welcomeGone = 3970f
    val holdAt = 3440f
    val flightStart = 3900f
    val flightEnd = 5420f
    val revealStart = 5450f
    val brandTransfer = 5490f
    val duration = 6500f
    val destinationReady = homeReady && targetBrandBounds != null

    fun finishOnce() {
        if (!completed) {
            completed = true
            latestFinished()
        }
    }

    fun skip() {
        if (!skipRequested) skipRequested = true
    }

    LaunchedEffect(reducedMotion, durationScale, forceSkip, skipRequested, destinationReady) {
        if (reducedMotion || durationScale == 0f || forceSkip || skipRequested) {
            if (!curtainNotified) {
                curtainNotified = true
                latestCurtain()
            }
            if (!revealNotified) {
                revealNotified = true
                latestReveal()
            }
            if (!brandNotified) {
                brandNotified = true
                latestBrand()
            }
            // Keep the real route mounted for a frame before removing the overlay.
            withFrameNanos { }
            finishOnce()
            return@LaunchedEffect
        }

        var previous = withFrameNanos { it }
        while (kotlinx.coroutines.currentCoroutineContext().isActive && !completed) {
            val next = withFrameNanos { it }
            val delta = ((next - previous) / 1_000_000f).coerceIn(0f, 50f) / durationScale
            previous = next
            val limit = if (destinationReady) duration + 40f else holdAt
            val t = (elapsedMs + delta).coerceAtMost(limit)
            elapsedMs = t
            if (!curtainNotified && t >= coverEnd) {
                curtainNotified = true
                latestCurtain()
            }
            if (!revealNotified && t >= revealStart) {
                revealNotified = true
                latestReveal()
            }
            if (!brandNotified && t >= brandTransfer) {
                brandNotified = true
                latestBrand()
            }
            if (t >= duration + 30f) finishOnce()
        }
    }

    if (reducedMotion || durationScale == 0f || forceSkip || skipRequested) return

    val veilAlpha = keyframes(
        elapsedMs, 0f to 0f, coverEnd to 1f,
        revealStart to 1f, 6250f to 0f, duration to 0f
    )
    val fieldAlpha = keyframes(
        elapsedMs, 0f to 0f, 650f to .44f, 1800f to .60f,
        3700f to .54f, 5300f to .18f, 6100f to 0f
    )
    val welcomeAlpha = keyframes(
        elapsedMs, 0f to 0f, welcomeStart to 0f,
        1550f to 1f, welcomeExit to 1f, welcomeGone to 0f
    )
    val flightProgress = curve(elapsedMs - flightStart, flightEnd - flightStart, lastFlightCurve)
    val centerProgress = curve(elapsedMs - centerStart, centerEnd - centerStart, firstFlightCurve)
    val brandAlpha = keyframes(
        elapsedMs, 0f to 1f, flightEnd - 70f to 1f,
        brandTransfer to 0f, duration to 0f
    )
    val flowerAlpha = 1f - curve(elapsedMs - 4530f, 650f, enterCurve)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val px = density.density

        // The veil is opaque BEFORE the NavHost switches routes.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = veilAlpha }
                .background(Brush.verticalGradient(listOf(
                    tokens.bg,
                    lerpColor(tokens.bg, tokens.panel, .38f),
                    tokens.bg
                )))
        )

        // Compatibility path only. AppNavGraph uses the single real Home behind this overlay.
        if (!revealUnderlyingHome && homeRevealContent != null && elapsedMs >= revealStart) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                alpha = curve(elapsedMs - revealStart, 850f, enterCurve)
            }) { homeRevealContent() }
        }

        // Restrained, theme-aware lighting. The permanent Home backdrop is untouched.
        if (fieldAlpha > 0f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = fieldAlpha }) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(widthPx * .5f, heightPx * .40f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to tokens.accent.copy(alpha = .16f),
                            .53f to tokens.accent.copy(alpha = .045f),
                            1f to Color.Transparent,
                            center = center,
                            radius = 160.dp.toPx()
                        ),
                        center = center,
                        radius = 160.dp.toPx()
                    )
                    val ringPhase = .5f - .5f * cos(2f * PI.toFloat() * elapsedMs / 4400f)
                    drawCircle(
                        color = tokens.accent.copy(alpha = .035f + .035f * ringPhase),
                        center = center,
                        radius = 112.dp.toPx() * lerp(.94f, 1.05f, ringPhase),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                // Keep the original organic washes, but much quieter and less saturated.
                Box(Modifier.fillMaxSize().graphicsLayer {
                    alpha = if (tokens.isDark) .33f else .17f
                }) {
                    cssAuras.forEach { aura ->
                        key(aura.id) {
                            CssAuraLayer(aura, elapsedMs, widthPx, heightPx, px)
                        }
                    }
                }
            }
        }

        val source = sourceBrandBounds?.let {
            Rect(it.left - origin.x, it.top - origin.y, it.right - origin.x, it.bottom - origin.y)
        }
        val target = targetBrandBounds?.let {
            Rect(it.left - origin.x, it.top - origin.y, it.right - origin.x, it.bottom - origin.y)
        }
        val baseWidth = brandSize.width.takeIf { it > 0 }?.toFloat()
            ?: (source?.width ?: 181f * px)
        val baseHeight = brandSize.height.takeIf { it > 0 }?.toFloat()
            ?: (source?.height ?: 48f * px)
        val centerScale = if (elapsedMs < flightStart) {
            lerp(1f, 1.06f, centerProgress)
        } else {
            lerp(1.06f, 1f, flightProgress)
        }
        val centeredX = (widthPx - baseWidth * centerScale) / 2f
        val centeredY = heightPx * .39f - baseHeight * centerScale / 2f
        val sourceX = source?.left ?: centeredX
        val sourceY = source?.top ?: centeredY
        val destX = target?.left ?: 24f * px
        val destY = target?.top ?: 50f * px
        val x = when {
            elapsedMs < centerEnd -> lerp(sourceX, centeredX, centerProgress)
            elapsedMs < flightStart -> centeredX
            else -> lerp(centeredX, destX, flightProgress)
        }
        val y = when {
            elapsedMs < centerEnd -> lerp(sourceY, centeredY, centerProgress)
            elapsedMs < flightStart -> centeredY
            else -> lerp(centeredY, destY, flightProgress)
        }
        // Interpolate layout rather than deriving the end scale from height alone.
        // The hidden flower continues to occupy its space during the flight.
        val fontSize = lerp(42f, 24f, flightProgress)
        val flowerSize = lerp(46f, 21f, flightProgress).roundToInt()
        val starSize = lerp(16f, 11f, flightProgress)
        val letterSpacing = lerp(-2.8f, -1.5f, flightProgress)
        Box(
            modifier = Modifier
                .onSizeChanged { brandSize = it }
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = x
                    translationY = y
                    scaleX = centerScale
                    scaleY = centerScale
                    alpha = brandAlpha
                },
            contentAlignment = Alignment.TopStart
        ) {
            LumiBrandLockup(
                flowerSize = flowerSize,
                fontSize = fontSize.sp,
                starSize = starSize.sp,
                letterSpacing = letterSpacing.sp,
                flowerAlpha = flowerAlpha,
                ceremonial = true,
                textColor = lerpColor(tokens.text, homeTokens.text, flightProgress),
                starColor = lerpColor(tokens.accent, homeTokens.accent, flightProgress),
                animated = true
            )
        }

        if (welcomeAlpha > 0f) {
            Text(
                text = stringResource(R.string.atmospheric_welcome_single),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = .05.sp,
                color = tokens.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .offset { IntOffset(0, (heightPx * .55f).roundToInt()) }
                    .padding(horizontal = 28.dp)
                    .graphicsLayer {
                        alpha = welcomeAlpha
                        translationY = (1f - welcomeAlpha) * 10f * px
                    }
            )
        }

        AnimatedVisibility(
            visible = elapsedMs >= 1350f && elapsedMs < flightStart,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = 25.dp, bottom = 53.dp)
        ) {
            Box(
                Modifier.clip(RoundedCornerShape(999.dp))
                    .background(tokens.panel.copy(alpha = .78f))
                    .border(1.dp, tokens.line, RoundedCornerShape(999.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { skip() }
                    )
                    .padding(horizontal = 13.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.atmospheric_enter_now),
                    fontSize = 11.sp, color = tokens.text
                )
            }
        }
    }
}
