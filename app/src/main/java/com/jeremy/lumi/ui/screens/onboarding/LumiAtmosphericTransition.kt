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

/**
 * Six-to-seven second first-run welcome. The single frame-clock waits at a
 * contemplative phase for real Home data and a measured destination.
 * No duplicate Home, no artificial loading timer, no extra network requests.
 */
private enum class HandoffPhase { COVER, WELCOME, FLIGHT, REVEAL, FINISHED }

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
    targetBrandRequired: Boolean = true,
    onBrandArrived: () -> Unit = {},
    forceSkip: Boolean = false
) {
    val tokens = LumiOnboardingThemeTokens.current
    val brandColors = com.jeremy.lumi.ui.theme.LumiThemeTokens.current
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val durationScale = remember(context) {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ).coerceAtLeast(0f)
    }
    val latestFinish by rememberUpdatedState(onFinished)
    val latestReveal by rememberUpdatedState(onRevealStart)
    val latestCover by rememberUpdatedState(onCurtainOpaque)
    val latestBrand by rememberUpdatedState(onBrandArrived)
    val destinationReady by rememberUpdatedState(
        homeReady && (!targetBrandRequired || targetBrandBounds != null || !revealUnderlyingHome)
    )

    var clockMs by remember { mutableFloatStateOf(0f) }
    var done by remember { mutableStateOf(false) }
    var coverSent by remember { mutableStateOf(false) }
    var revealSent by remember { mutableStateOf(false) }
    var brandSent by remember { mutableStateOf(false) }
    var skipRequested by remember { mutableStateOf(false) }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }

    val coverEnd = 460f
    val centerStart = 300f
    val centerEnd = 1460f
    val welcomeIn = 1200f
    val welcomeSteady = 1670f
    val holdAt = 3750f
    val welcomeOut = 3930f
    val welcomeGone = 4410f
    val flightStart = 4380f
    val flightEnd = 5580f
    val brandArrive = 5610f
    val revealStart = 5650f
    val duration = 6800f

    fun finishOnce() {
        if (!done) {
            done = true
            latestFinish()
        }
    }
    fun notifyCover() {
        if (!coverSent) {
            coverSent = true
            latestCover()
        }
    }
    fun notifyReveal() {
        if (!revealSent) {
            revealSent = true
            latestReveal()
        }
    }
    fun notifyBrand() {
        if (!brandSent) {
            brandSent = true
            latestBrand()
        }
    }

    val fastExit = forceSkip || skipRequested
    LaunchedEffect(fastExit, reducedMotion, durationScale) {
        if (fastExit || reducedMotion || durationScale == 0f) {
            notifyCover()
            notifyReveal()
            notifyBrand()
            // Real route is composed before removing the outside-NavHost overlay.
            if (fastExit && durationScale > 0f && !reducedMotion) {
                kotlinx.coroutines.delay((180f * durationScale).toLong())
            } else withFrameNanos { }
            finishOnce()
            return@LaunchedEffect
        }

        var lastNs = withFrameNanos { it }
        while (isActive && !done) {
            val frameNs = withFrameNanos { it }
            val delta = ((frameNs - lastNs) / 1_000_000f)
                .coerceIn(0f, 55f) / durationScale
            lastNs = frameNs
            // This pause has no arbitrary timeout: the Home ViewModel must be ready.
            // The user can still choose "Entrar ahora" while waiting.
            val clockLimit = if (destinationReady) duration + 40f else holdAt
            clockMs = (clockMs + delta).coerceAtMost(clockLimit)
            if (clockMs >= coverEnd) notifyCover()
            if (clockMs >= brandArrive) notifyBrand()
            if (clockMs >= revealStart) notifyReveal()
            if (clockMs >= duration + 25f) finishOnce()
        }
    }

    if (reducedMotion || durationScale == 0f) return

    val quickExitAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (fastExit) 0f else 1f,
        animationSpec = tween(durationMillis = (180f * durationScale).roundToInt().coerceAtLeast(1)),
        label = "ceremony_quick_exit"
    )
    val phase = when {
        clockMs < coverEnd -> HandoffPhase.COVER
        clockMs < flightStart -> HandoffPhase.WELCOME
        clockMs < revealStart -> HandoffPhase.FLIGHT
        clockMs < duration -> HandoffPhase.REVEAL
        else -> HandoffPhase.FINISHED
    }

    val veilAlpha = if (fastExit) 1f else keyframes(
        clockMs,
        0f to 0f, coverEnd to 1f, revealStart to 1f,
        6300f to 0f, duration to 0f
    )
    val homeAlpha = curve(clockMs - revealStart, 850f, enterCurve)
    val lightAlpha = keyframes(
        clockMs,
        0f to 0f, 700f to .4f, 2100f to .6f,
        3900f to .52f, 5500f to .16f, 6300f to 0f
    )
    val welcomeAlpha = keyframes(
        clockMs,
        0f to 0f, welcomeIn to 0f,
        welcomeSteady to 1f, welcomeOut to 1f, welcomeGone to 0f
    )
    val centerEase = curve(clockMs - centerStart, centerEnd - centerStart, firstFlightCurve)
    val travelEase = curve(clockMs - flightStart, flightEnd - flightStart, lastFlightCurve)
    val centerScale = if (clockMs < flightStart) {
        lerp(1f, 1.06f, centerEase)
    } else lerp(1.06f, 1f, travelEase)
    // Observer/support routes have no measured Home lockup to receive the wordmark.
    val brandlessDestination = revealUnderlyingHome && !targetBrandRequired
    val travelingAlpha = if (brandlessDestination) keyframes(
        clockMs, 0f to 1f, flightStart to 1f,
        flightStart + 780f to 0f, duration to 0f
    ) else keyframes(
        clockMs, 0f to 1f, flightEnd - 90f to 1f,
        brandArrive + 25f to 0f, duration to 0f
    )
    val flowerAlpha = 1f - curve(clockMs - 4680f, 710f, enterCurve)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
            .graphicsLayer { alpha = quickExitAlpha }
            .onGloballyPositioned { overlayOrigin = it.positionInRoot() }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = {}
            )
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val px = density.density

        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    alpha = if (revealUnderlyingHome) veilAlpha * (1f - homeAlpha) else veilAlpha
                }
                .background(
                    Brush.verticalGradient(listOf(
                        tokens.bg, lerpColor(tokens.bg, tokens.panel, .35f), tokens.bg
                    ))
                )
        )
        // Normally unused: the root graph already owns the only Home.
        if (!revealUnderlyingHome && homeRevealContent != null && phase == HandoffPhase.REVEAL) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = homeAlpha }) {
                homeRevealContent()
            }
        }

        // Quiet organic light. No changes to the permanent approved Home backdrop.
        if (lightAlpha > 0f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = lightAlpha }) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(w * .5f, h * .4f)
                    val radius = 172.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to tokens.accent.copy(alpha = .17f),
                            .48f to tokens.accent.copy(alpha = .045f),
                            1f to Color.Transparent,
                            center = center, radius = radius
                        ),
                        center = center, radius = radius
                    )
                    val breath = .5f - .5f * cos(
                        2f * PI.toFloat() * clockMs / 5200f
                    )
                    drawCircle(
                        color = tokens.accent.copy(alpha = .02f + .035f * breath),
                        center = center,
                        radius = 124.dp.toPx() * lerp(.96f, 1.05f, breath),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                Box(Modifier.fillMaxSize().graphicsLayer {
                    alpha = if (tokens.isDark) .28f else .13f
                }) {
                    cssAuras.forEach { aura ->
                        key(aura.id) {
                            CssAuraLayer(aura, clockMs, w, h, px)
                        }
                    }
                }
            }
        }

        val source = sourceBrandBounds?.let {
            Rect(it.left - overlayOrigin.x, it.top - overlayOrigin.y,
                 it.right - overlayOrigin.x, it.bottom - overlayOrigin.y)
        }
        val destination = targetBrandBounds?.let {
            Rect(it.left - overlayOrigin.x, it.top - overlayOrigin.y,
                 it.right - overlayOrigin.x, it.bottom - overlayOrigin.y)
        }
        val initialWidth = source?.width?.takeIf { it > 0f } ?: 181f * px
        val initialHeight = source?.height?.takeIf { it > 0f } ?: 48f * px
        val centeredLeft = (w - initialWidth * 1.06f) / 2f
        val centeredTop = h * .39f - initialHeight * 1.06f / 2f
        val startLeft = source?.left ?: centeredLeft
        val startTop = source?.top ?: centeredTop
        val groupX = when {
            clockMs < centerEnd -> lerp(startLeft, centeredLeft, centerEase)
            else -> centeredLeft
        }
        val groupY = when {
            clockMs < centerEnd -> lerp(startTop, centeredTop, centerEase)
            else -> centeredTop
        }

        val wordmarkStart = startLeft + 54f * px
        val wordmarkCenter = centeredLeft + 54f * px * 1.06f
        val wordmarkX = when {
            clockMs < centerEnd -> lerp(wordmarkStart, wordmarkCenter, centerEase)
            clockMs < flightStart -> wordmarkCenter
            else -> if (brandlessDestination) wordmarkCenter else lerp(wordmarkCenter, destination?.left ?: 24f * px, travelEase)
        }
        val wordmarkY = when {
            clockMs < centerEnd -> lerp(startTop, centeredTop, centerEase)
            clockMs < flightStart -> centeredTop
            else -> if (brandlessDestination) centeredTop else lerp(centeredTop, destination?.top ?: 50f * px, travelEase)
        }

        // Flower has its OWN position/opacity; fading it never squeezes the wordmark.
        val flowerX = groupX
        val flowerY = groupY + (initialHeight - 46f * px) * .5f
        if (flowerAlpha > .001f && !fastExit) {
            com.jeremy.lumi.ui.components.LumiBrandFlower(
                modifier = Modifier
                    .offset { IntOffset(flowerX.roundToInt(), flowerY.roundToInt()) }
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = centerScale
                        scaleY = centerScale
                        alpha = flowerAlpha
                    },
                size = 46, animated = true, ceremonial = true
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(wordmarkX.roundToInt(), wordmarkY.roundToInt()) }
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = centerScale
                    scaleY = centerScale
                    alpha = travelingAlpha
                },
            contentAlignment = Alignment.TopStart
        ) {
            LumiBrandLockup(
                flowerVisibility = 0f,
                fontSize = lerp(42f, 24f, travelEase).sp,
                starSize = lerp(16f, 11f, travelEase).sp,
                letterSpacing = lerp(-2.8f, -1.5f, travelEase).sp,
                textColor = lerpColor(tokens.text, brandColors.text, travelEase),
                starColor = lerpColor(tokens.accent, brandColors.accent, travelEase),
                animated = true
            )
        }

        if (welcomeAlpha > 0f && !fastExit) {
            Text(
                stringResource(R.string.atmospheric_welcome_single),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp,
                color = tokens.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .offset { IntOffset(0, (h * .54f).roundToInt()) }
                    .padding(horizontal = 28.dp)
                    .graphicsLayer {
                        alpha = welcomeAlpha
                        translationY = (1f - welcomeAlpha) * 10f * px
                    }
            )
        }

        AnimatedVisibility(
            visible = phase == HandoffPhase.WELCOME,
            enter = fadeIn(tween(280)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = 25.dp, bottom = 53.dp)
        ) {
            Box(
                Modifier.clip(RoundedCornerShape(999.dp))
                    .background(tokens.panel.copy(alpha = .82f))
                    .border(1.dp, tokens.line, RoundedCornerShape(999.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { skipRequested = true }
                    )
                    .padding(horizontal = 13.dp, vertical = 10.dp)
            ) {
                Text(
                    stringResource(R.string.atmospheric_enter_now),
                    fontSize = 11.sp, color = tokens.text
                )
            }
        }
    }
}
