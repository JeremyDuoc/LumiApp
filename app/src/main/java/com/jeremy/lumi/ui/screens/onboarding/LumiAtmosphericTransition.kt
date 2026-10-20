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
    onCurtainOpaque: () -> Unit = {}
) {
    val tokens = LumiOnboardingThemeTokens.current
    val density = LocalDensity.current
    val latestFinished by rememberUpdatedState(onFinished)
    val latestRevealStart by rememberUpdatedState(onRevealStart)
    val latestCurtainOpaque by rememberUpdatedState(onCurtainOpaque)
    var curtainNotified by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableFloatStateOf(0f) }
    var completeRequested by remember { mutableStateOf(false) }
    var revealNotified by remember { mutableStateOf(false) }
    var layerOrigin by remember { mutableStateOf(Offset.Zero) }
    val duration = 11520f
    val centerEnd = 900f
    val flightStart = 10760f
    val flightEnd = 11350f
    val revealStart = 10400f
    val opaqueAt = 1350f // El velo ya alcanzó el 100%; cargar Home detrás.

    fun finishOnce() {
        if (!completeRequested) {
            completeRequested = true
            latestFinished()
        }
    }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            finishOnce()
            return@LaunchedEffect
        }
        // Ticker sincronizado al VSYNC: el timeline CSS (11520ms) es la única fuente.
        val initialNs = withFrameNanos { it }
        while (isActive && !completeRequested) {
            val nowNs = withFrameNanos { it }
            val t = ((nowNs - initialNs) / 1_000_000f).coerceAtMost(duration + 36f)
            elapsedMs = t
            if (t >= opaqueAt && !curtainNotified) {
                curtainNotified = true
                latestCurtainOpaque()
            }
            if (t >= revealStart && !revealNotified) {
                revealNotified = true
                latestRevealStart()
            }
            if (t >= duration + 35f) finishOnce()
        }
    }
    if (reducedMotion) return

    val progress = (elapsedMs / duration).coerceIn(0f, 1f)
    val veilAlpha = keyframes(progress, 0f to 0f, .11f to 1f, .84f to 1f, .93f to .98f, 1f to 0f)
    val fieldAlpha = keyframes(progress, 0f to 0f, .13f to .78f, .37f to 1f, .66f to .97f, .84f to .85f, 1f to 0f)
    val fieldScale = keyframes(progress, 0f to .80f, .13f to .95f, .37f to 1.07f, .66f to 1.02f, .84f to 1.12f, 1f to 1.24f)
    val messageIndex = messageStarts.indexOfLast { elapsedMs >= it }
    val exitMessageAlpha = (1f - (elapsedMs - revealStart).coerceIn(0f, 440f) / 440f)
    val homeAlpha = if (elapsedMs < revealStart) 0f else
        curve(elapsedMs - revealStart, 850f, enterCurve)
    // Bajo el overlay ya existe MainScreen; descubrirlo sin volver a crearlo.
    val curtainAlpha = if (revealUnderlyingHome) {
        veilAlpha * (1f - homeAlpha)
    } else veilAlpha

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { layerOrigin = it.positionInRoot() }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val dp = density.density

        // El fondo previo NO debe desaparecer al instante: HTML .handoff-veil.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = curtainAlpha }
                .background(Brush.verticalGradient(listOf(
                    tokens.bg,
                    lerpColor(tokens.bg, tokens.panel, .64f),
                    tokens.bg
                )))
                // Mientras transcurre la ceremonia, no permitir pulsar el
                // botón de Guardar que permanece montado detrás del velo.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        )

        // Destapar el Home real desde 10.400 ms, detrás del logo viajero.
        // El compositor padre debe proporcionar la pantalla REAL y ocultar su marca
        // durante el vuelo para que no aparezcan dos logos.
        if (!revealUnderlyingHome && homeRevealContent != null && elapsedMs >= revealStart) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    alpha = homeAlpha
                    translationY = 10f * dp * (1f - homeAlpha)
                }.blur(((1f - homeAlpha) * 3f).dp)
            ) { homeRevealContent() }
        }

        // ONE transformed parent matches `.handoff-glow`:
        // opacity/scale apply to STATIC gradient washes, moving orbs and ring.
        // V16 scaled/blended separate canvases inconsistently, generating a
        // visually exaggerated drop of one colored cloud in the emulator.
        if (fieldAlpha > 0f) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        alpha = fieldAlpha
                        scaleX = fieldScale
                        scaleY = fieldScale
                    }
                    .clipToBounds()
            ) {
                // CSS .handoff-glow::before uses ellipse radii IN PERCENT OF
                // THE WHOLE VIEWPORT. The old code incorrectly halved them.
                Canvas(Modifier.fillMaxSize().graphicsLayer {
                    alpha = if (tokens.isDark) 1f else .28f
                }) {
                    fun wash(
                        centerPoint: Offset,
                        radiusX: Float,
                        radiusY: Float,
                        tint: Color,
                        transparentAt: Float
                    ) {
                        withTransform({ scale(radiusX / radiusY, 1f, centerPoint) }) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0f to tint,
                                    transparentAt to Color.Transparent,
                                    1f to Color.Transparent,
                                    center = centerPoint,
                                    radius = radiusY
                                ),
                                center = centerPoint,
                                radius = radiusY
                            )
                        }
                    }
                    wash(Offset(widthPx * .32f, heightPx * .37f),
                        widthPx * .50f, heightPx * .34f, Color(0x2EE865AC), .83f)
                    wash(Offset(widthPx * .71f, heightPx * .38f),
                        widthPx * .49f, heightPx * .42f, Color(0x30855CE5), .83f)
                    wash(Offset(widthPx * .66f, heightPx * .53f),
                        widthPx * .34f, heightPx * .25f, Color(0x215FC8B0), .88f)
                    wash(Offset(widthPx * .36f, heightPx * .54f),
                        widthPx * .36f, heightPx * .31f, Color(0x1FF5A369), .87f)
                }

                // Separate CSS filter(blur(30px)) on each of four positioned
                // elements; own-element percentage translation and scale.
                Box(Modifier.fillMaxSize().graphicsLayer {
                    alpha = if (tokens.isDark) 1f else .46f
                }) {
                    cssAuras.forEach { aura ->
                        key(aura.id) {
                            CssAuraLayer(aura, elapsedMs, widthPx, heightPx, dp)
                        }
                    }
                }

                val ringPhase = .5f - .5f * cos(2f * PI.toFloat() * elapsedMs / 5400f)
                Canvas(Modifier.fillMaxSize().graphicsLayer {
                    alpha = if (tokens.isDark) 1f else .2f
                }) {
                    drawCircle(
                        color = Color(0xFFD7A4DD).copy(alpha = .014f + .042f * ringPhase),
                        center = Offset(widthPx * .5f, heightPx * .40f),
                        radius = 130.dp.toPx() * lerp(.85f, 1.22f, ringPhase),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }

        // Compartir la misma marca de Step7Summary: tamaño y posición REALES.
        // HTML usa `source.getBoundingClientRect()` y `target.getBoundingClientRect()`.
        val measuredSource = sourceBrandBounds?.let {
            Rect(it.left - layerOrigin.x, it.top - layerOrigin.y,
                it.right - layerOrigin.x, it.bottom - layerOrigin.y)
        }
        val initialWidth = measuredSource?.width?.takeIf { it > 0f } ?: (181f * dp)
        val initialHeight = measuredSource?.height?.takeIf { it > 0f } ?: (48f * dp)
        val sourceLeft = measuredSource?.left ?: (widthPx - initialWidth) / 2f
        val sourceTop = measuredSource?.top ?: (100f * dp)
        val centeredLeft = (widthPx - initialWidth) / 2f
        val centeredTop = heightPx * .397f - initialHeight / 2f
        val target = targetBrandBounds?.let {
            Rect(it.left - layerOrigin.x, it.top - layerOrigin.y,
                it.right - layerOrigin.x, it.bottom - layerOrigin.y)
        }
        val destinationX = target?.left ?: 24f * dp
        val destinationY = target?.top ?: 50f * dp
        val destinationScale = target?.height?.let { it / initialHeight } ?: 0.48f
        val introEased = curve(elapsedMs, centerEnd, firstFlightCurve)
        val outroEased = curve(elapsedMs - flightStart, flightEnd - flightStart, lastFlightCurve)
        val brandX: Float
        val brandY: Float
        val brandScale: Float
        when {
            elapsedMs < centerEnd -> {
                brandX = lerp(sourceLeft, centeredLeft, introEased)
                brandY = lerp(sourceTop, centeredTop, introEased)
                brandScale = lerp(1f, 1.17f, introEased)
            }
            elapsedMs < flightStart -> {
                brandX = centeredLeft
                brandY = centeredTop
                brandScale = 1.17f
            }
            else -> {
                brandX = lerp(centeredLeft, destinationX, outroEased)
                brandY = lerp(centeredTop, destinationY, outroEased)
                brandScale = lerp(1.17f, destinationScale, outroEased)
            }
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = brandX
                    translationY = brandY
                    scaleX = brandScale
                    scaleY = brandScale
                },
            contentAlignment = Alignment.TopStart
        ) {
            LumiBrandLockup(
                flowerSize = 46,
                fontSize = 42.sp,
                starSize = 16.sp,
                letterSpacing = (-2.8).sp,
                ceremonial = true,
                flowerVisibility = 1f - outroEased,
                textColor = tokens.text,
                starColor = tokens.accent,
                animated = true
            )
        }

        // HTML showMessage(ix): los 400ms de salida TERMINAN antes de
        // sustituir el texto. AnimatedContent simultáneo causaba solapamiento.
        if (messageIndex >= 0 && elapsedMs < revealStart + 440f) {
            val since = (elapsedMs - messageStarts[messageIndex]).coerceAtLeast(0f)
            val isOutgoing = messageIndex > 0 && since < 400f
            val displayedIndex = if (isOutgoing) messageIndex - 1 else messageIndex
            val textAlpha: Float
            val textOffsetY: Float
            if (isOutgoing) {
                val f = (since / 400f).coerceIn(0f, 1f)
                textAlpha = 1f - f
                textOffsetY = -13f * f
            } else {
                val enterElapsed = if (messageIndex == 0) since else since - 400f
                val ease = curve(enterElapsed,
                    if (messageIndex == 0) 750f else 680f, enterCurve)
                textAlpha = ease
                textOffsetY = (if (messageIndex == 0) 13f else 15f) * (1f - ease)
            }
            Column(
                Modifier.fillMaxWidth()
                    .offset { IntOffset(0, (heightPx * .55f).roundToInt()) }
                    .graphicsLayer {
                        alpha = exitMessageAlpha * textAlpha
                        translationY = textOffsetY * dp
                    }
                    .padding(horizontal = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(ceremonyTexts[displayedIndex].first),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-.16).sp,
                    lineHeight = 20.sp,
                    color = tokens.text,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    stringResource(ceremonyTexts[displayedIndex].second),
                    fontSize = 11.5.sp,
                    color = tokens.muted.copy(alpha = .85f),
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    repeat(3) { dot ->
                        val wave = sin((elapsedMs / 1650f - dot * .22f / 1.65f) * 2f * PI.toFloat())
                        val pulse = (.5f + .5f * wave)
                        Box(
                            Modifier.size(4.dp)
                                .graphicsLayer {
                                    alpha = lerp(.25f, .94f, pulse)
                                    scaleX = lerp(.85f, 1.24f, pulse)
                                    scaleY = lerp(.85f, 1.24f, pulse)
                                }
                                .background(tokens.accent, CircleShape)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = elapsedMs >= 850f && elapsedMs < revealStart,
            enter = fadeIn(tween(350)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 25.dp, bottom = 53.dp)
        ) {
            Box(
                Modifier.clip(RoundedCornerShape(999.dp))
                    .background(tokens.panel.copy(alpha = .82f))
                    .border(1.dp, tokens.line, RoundedCornerShape(999.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { finishOnce() }
                    )
                    .padding(horizontal = 13.dp, vertical = 10.dp)
            ) {
                Text(stringResource(R.string.atmospheric_enter_now), fontSize = 11.sp, color = tokens.text)
            }
        }
    }
}
