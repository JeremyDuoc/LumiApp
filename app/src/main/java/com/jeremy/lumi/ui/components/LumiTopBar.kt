package com.jeremy.lumi.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.ui.res.stringResource
import com.jeremy.lumi.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Flor EXACTA del HTML v11/v12. NO usar addRoundRect ni gradiente giratorio.
 *
 * Fuente de verdad: <symbol id="flower-art" viewBox="0 0 46 46"> del HTML.
 * Se conserva el mismo path Bézier y los cuatro stops del linearGradient.
 * El clip evita que la luz elíptica pinte fuera de la silueta.
 * La animación transforma el SVG completo, sin deformar sus curvas.
 */
private const val LUMI_SVG_FLOWER_PATH =
    "M22 2.1C31 -0.3 42.1 6 44 15.9C46.3 24 42.3 36.2 35.6 41.3C30 45.8 19.6 46.2 12.3 42C4.6 38.1 .9 30 .9 22C1 12 10 3.6 22 2.1Z"

@Composable
fun LumiBrandFlower(
    modifier: Modifier = Modifier,
    size: Int = 20,
    animated: Boolean = true,
    ceremonial: Boolean = false,
    motionProgress: Float? = null
) {
    // HTML: flowerLive 7.8s; handoff: lumiFlowerV7 2.9s alternate.
    val transition = if (animated && motionProgress == null && ValueAnimator.areAnimatorsEnabled()) rememberInfiniteTransition(label = "svg_flower_motion") else null
    val phase = motionProgress ?: (transition?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (ceremonial) 2900 else 3900,
                easing = if (ceremonial) androidx.compose.animation.core.CubicBezierEasing(.4f, .05f, .6f, .95f)
                         else FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "svg_flower_transform"
    )?.value ?: .5f)

    val rotation = if (!animated) 0f else if (ceremonial) -7f + 15f * phase else -6f + 11f * phase
    val zoom = if (!animated) 1f else if (ceremonial) {
        if (phase < .45f) .96f + (.085f / .45f) * phase
        else 1.045f - (.055f / .55f) * (phase - .45f)
    } else .97f + .075f * phase

    val sourcePath = remember { androidx.compose.ui.graphics.vector.PathParser().parsePathString(LUMI_SVG_FLOWER_PATH).toPath() }
    Canvas(modifier = modifier.size(size.dp).graphicsLayer {
        rotationZ = rotation
        scaleX = zoom
        scaleY = zoom
    }) {
        val ratioX = this.size.width / 46f
        val ratioY = this.size.height / 46f
        // SVG linearGradient: x1=9%, y1=87%; x2=95%, y2=12%.
        val gradient = Brush.linearGradient(
            0f to Color(0xFFD8B3F0),
                .32f to Color(0xFFE5B9D4),
                .62f to Color(0xFFF5CAAA),
                1f to Color(0xFFA7D1CC),
            start = Offset(46f * .09f, 46f * .87f),
            end = Offset(46f * .95f, 46f * .12f)
        )
        withTransform({ scale(ratioX, ratioY, Offset.Zero) }) {
            clipPath(sourcePath) {
                drawRect(brush = gradient, size = androidx.compose.ui.geometry.Size(46f, 46f))
                // SVG ellipse: cx=17 cy=13 rx=18 ry=15, #fff .32 -> 0.
                drawOval(
                    brush = Brush.radialGradient(
                        0f to Color.White.copy(alpha = .32f), 1f to Color.Transparent,
                        center = Offset(17f, 13f), radius = 18f
                    ),
                    topLeft = Offset(-1f, -2f),
                    size = androidx.compose.ui.geometry.Size(36f, 30f)
                )
            }
        }
    }
}

/**
 * Lockup de marca unificado oficial de Lumi:
 * - Flor animada ([LumiBrandFlower])
 * - Tipografía "lumi" en negrita editorial
 * - Destello estrella "✦" con animación viva de destello, rotación y pulso
 *
 * Utilizar este componente en TODAS las cabeceras y pantallas para garantizar 100% consistencia.
 */
@Composable
fun LumiBrandLockup(
    modifier: Modifier = Modifier,
    flowerSize: Int = 21,
    fontSize: TextUnit = 24.sp,
    starSize: TextUnit = 12.sp,
    letterSpacing: TextUnit = (-1.5).sp,
    textColor: Color = LumiThemeTokens.current.text,
    starColor: Color = LumiThemeTokens.current.accent,
    animated: Boolean = true,
    ceremonial: Boolean = false,
    flowerVisibility: Float = 1f,
    motionProgress: Float? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val flowerAmount = flowerVisibility.coerceIn(0f, 1f)
        if (flowerAmount > 0f) {
            Box(
                Modifier.width(((flowerSize + 8) * flowerAmount).dp)
                    .height((flowerSize * flowerAmount).dp),
                contentAlignment = Alignment.CenterStart
            ) {
                LumiBrandFlower(
                    modifier = Modifier.requiredSize(flowerSize.dp)
                        .graphicsLayer {
                            alpha = flowerAmount
                            scaleX = .65f + .35f * flowerAmount
                            scaleY = scaleX
                        },
                    size = flowerSize, animated = animated, ceremonial = ceremonial,
                    motionProgress = motionProgress
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "lumi",
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = letterSpacing,
                color = textColor
            )
            Spacer(modifier = Modifier.width(3.dp))
            LumiBrandStar(starSize, starColor, animated, motionProgress)
        }
    }
}

/** A brief luminous turn followed by rest; quarter-turn symmetry makes the loop seamless. */
@Composable
private fun LumiBrandStar(starSize: TextUnit, color: Color, animated: Boolean, motionProgress: Float? = null) {
    val transition = if (animated && motionProgress == null && ValueAnimator.areAnimatorsEnabled()) {
        rememberInfiniteTransition(label = "brand_star_light")
    } else null
    val turn = transition?.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 7200
            0f at 0
            0f at 1600 using FastOutSlowInEasing
            1f at 3400
            1f at 7200
        }),
        label = "brand_star_turn"
    )
    val star = remember {
        Path().apply {
            moveTo(0f, -1f)
            cubicTo(.14f, -.14f, .14f, -.14f, 1f, 0f)
            cubicTo(.14f, .14f, .14f, .14f, 0f, 1f)
            cubicTo(-.14f, .14f, -.14f, .14f, -1f, 0f)
            cubicTo(-.14f, -.14f, -.14f, -.14f, 0f, -1f)
            close()
        }
    }
    val diameter = with(LocalDensity.current) { starSize.toDp() }
    Canvas(Modifier.size(diameter)) {
        val progress = motionProgress ?: turn?.value ?: 0f
        val glint = sin(progress * PI.toFloat()).coerceIn(0f, 1f)
        val radius = size.minDimension * (.48f + .08f * glint)
        if (glint > .001f) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to color.copy(alpha = .32f * glint),
                    1f to color.copy(alpha = 0f),
                    center = center, radius = size.minDimension
                ),
                center = center, radius = size.minDimension
            )
        }
        withTransform({
            translate(center.x, center.y)
            rotate(90f * progress, pivot = Offset.Zero)
            scale(radius, radius, pivot = Offset.Zero)
        }) {
            drawPath(star, lerp(color, Color.White, .30f * glint).copy(alpha = .85f + .15f * glint))
        }
    }
}

/**
 * Cabecera superior unificada de Lumi:
 * - Brand: [LumiBrandLockup] con texto y destello vivo, sin flor en Home
 * - Acciones: Selector de tema (paleta) + Notificaciones con badge dot
 */
@Composable
fun LumiTopBar(
    modifier: Modifier = Modifier,
    hasNotificationAlert: Boolean = true,
    onThemeClick: (() -> Unit)? = null,
    onNotificationsClick: () -> Unit,
    onChatClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    // Se mide la marca real; sirve como destino del vuelo desde el onboarding.
    onBrandBounds: ((Rect) -> Unit)? = null,
    hideBrand: Boolean = false,
    arrivalProgress: State<Float>? = null
) {
    val tokens = LumiThemeTokens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Marca Lumi unificada y animada
        LumiBrandLockup(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    val origin = coordinates.positionInRoot()
                    onBrandBounds?.invoke(Rect(
                        origin.x, origin.y,
                        origin.x + coordinates.size.width,
                        origin.y + coordinates.size.height
                    ))
                }
                .graphicsLayer { alpha = if (hideBrand) 0f else 1f },
            flowerSize = 21,
            fontSize = 24.sp,
            starSize = 11.sp,
            letterSpacing = (-1.5).sp,
            flowerVisibility = 0f,
            animated = !hideBrand
        )

        Box(Modifier.graphicsLayer {
            val p = ((arrivalProgress?.value ?: 1f) / .7f).coerceIn(0f, 1f)
            alpha = p * p * (3f - 2f * p)
            translationY = (1f - alpha) * 8.dp.toPx()
        }) {
        LumiCollapsibleActions(
            hasNotificationAlert = hasNotificationAlert,
            onChatClick = onChatClick,
            onThemeClick = onThemeClick,
            onNotificationsClick = onNotificationsClick,
            onSettingsClick = onSettingsClick
        )
        }
    }
}
