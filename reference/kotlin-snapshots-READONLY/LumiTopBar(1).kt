package com.jeremy.lumi.ui.components

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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import com.jeremy.lumi.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Flor con contorno orgánico, color en movimiento y luz suave.
 * Las fases se leen al dibujar para evitar recomponer la cabecera en cada frame.
 */
@Composable
fun LumiBrandFlower(
    modifier: Modifier = Modifier,
    size: Int = 20,
    animated: Boolean = true
) {
    val colors = remember {
        listOf(
            Color(0xFFE9B3C4),
            Color(0xFFD4B3ED),
            Color(0xFFA8D1C9),
            Color(0xFFF5CCAA),
            Color(0xFFE9B3C4)
        )
    }

    val infiniteTransition = if (animated) rememberInfiniteTransition(label = "flower_anim") else null
    val organicPhase = infiniteTransition?.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flower_organic_phase"
    )

    val gradientRotation = infiniteTransition?.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flower_gradient_rotation"
    )
    val path = remember { Path() }

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val phase = organicPhase?.value ?: 0f
            val breath = if (animated) sin(phase * 2f) else 0f
            val sway = if (animated) sin(phase) else 0f
            val morph = if (animated) sin(phase + 0.8f) * 0.09f else 0f
            val width = this.size.width
            val height = this.size.height

            if (animated) {
                val glowCenter = center + Offset(width * sway * 0.12f, -height * 0.08f)
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to Color(0xFFD4B3ED).copy(alpha = 0.28f + 0.10f * breath),
                        0.5f to Color(0xFFE9B3C4).copy(alpha = 0.14f),
                        1f to Color.Transparent,
                        center = glowCenter,
                        radius = width * (0.82f + 0.04f * breath)
                    ),
                    radius = width * (0.82f + 0.04f * breath),
                    center = glowCenter
                )
            }

            path.reset()
            path.apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = 0f,
                        right = width,
                        bottom = height,
                        topLeftCornerRadius = CornerRadius(width * (0.45f + morph), height * (0.55f - morph)),
                        topRightCornerRadius = CornerRadius(width * (0.65f - morph), height * (0.40f + morph)),
                        bottomRightCornerRadius = CornerRadius(width * (0.55f + morph), height * (0.65f - morph)),
                        bottomLeftCornerRadius = CornerRadius(width * (0.40f - morph), height * (0.50f + morph))
                    )
                )
            }
            // Color rotates inside the silhouette, independently of its gentle sway.
            // Amplitudes remain legible even in the 21 dp header mark.
            translate(left = width * sway * 0.025f, top = height * sway * 0.075f) {
                rotate(-25f + sway * 12f) {
                    scale(1f + breath * 0.09f, 1f - breath * 0.055f) {
                        clipPath(path) {
                            rotate(gradientRotation?.value ?: 0f) {
                                drawCircle(Brush.sweepGradient(colors, center), radius = width)
                            }
                            val lightCenter = Offset(
                                width * (0.32f + sway * 0.20f),
                                height * (0.28f + if (animated) cos(phase) * 0.16f else 0f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                                    center = lightCenter,
                                    radius = width * 0.75f
                                ),
                                radius = width,
                                center = center
                            )
                        }
                    }
                }
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
    animated: Boolean = true
) {
    val infiniteTransition = if (animated) rememberInfiniteTransition(label = "brand_lockup_twinkle") else null
    val starTwinkle = infiniteTransition?.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6200
                0f at 0 using FastOutSlowInEasing
                1f at 900 using FastOutSlowInEasing
                0f at 2100
                0f at 6200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "star_twinkle"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LumiBrandFlower(size = flowerSize, animated = animated)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "lumi",
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = letterSpacing,
                color = textColor
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "✦",
                fontSize = starSize,
                color = starColor,
                modifier = Modifier
                    .graphicsLayer {
                        val twinkle = starTwinkle?.value ?: 0f
                        scaleX = 1f + twinkle * 0.16f
                        scaleY = scaleX
                        alpha = if (animated) 0.72f + twinkle * 0.28f else 1f
                        rotationZ = twinkle * 12f
                    }
            )
        }
    }
}

/**
 * Cabecera superior unificada de Lumi:
 * - Brand: [LumiBrandLockup] con flor animada y destello vivo
 * - Acciones: Selector de tema (paleta) + Notificaciones con badge dot
 */
@Composable
fun LumiTopBar(
    modifier: Modifier = Modifier,
    hasNotificationAlert: Boolean = true,
    onThemeClick: (() -> Unit)? = null,
    onNotificationsClick: () -> Unit,
    onChatClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null
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
            flowerSize = 21,
            fontSize = 24.sp,
            starSize = 11.sp,
            letterSpacing = (-1.5).sp,
            animated = true
        )

        // Acciones superiores: Chat, Notificaciones, Ajustes
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de chat (opcional)
            if (onChatClick != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onChatClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = stringResource(R.string.nav_chat),
                        tint = tokens.muted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Botón de tema (si está explícitamente habilitado)
            if (onThemeClick != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onThemeClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = stringResource(R.string.cd_change_theme),
                        tint = tokens.muted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Botón de notificaciones con dot-alert
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNotificationsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = stringResource(R.string.cd_notifications),
                    tint = tokens.muted,
                    modifier = Modifier.size(20.dp)
                )
                if (hasNotificationAlert) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(tokens.rose)
                    )
                }
            }

            // Botón de ajustes (opcional)
            if (onSettingsClick != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        tint = tokens.muted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
