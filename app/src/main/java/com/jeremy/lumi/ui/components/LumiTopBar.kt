package com.jeremy.lumi.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.ui.theme.LumiThemeTokens

/**
 * Logotipo orgánico de la flor de Lumi con degradado cónico multicolor y animación viva.
 * Conforme al prototipo HTML Lumi_Onboarding_Logo_Contextual_v3:
 * - RoundRect con 4 radios asimétricos
 * - Inclinación base de -25° con suave respiración orgánica
 * - Aura radial suave de destello
 */
@Composable
fun LumiBrandFlower(
    modifier: Modifier = Modifier,
    size: Int = 20,
    animated: Boolean = true
) {
    val colors = listOf(
        Color(0xFFE9B3C4),
        Color(0xFFD4B3ED),
        Color(0xFFA8D1C9),
        Color(0xFFF5CCAA),
        Color(0xFFE9B3C4)
    )

    val infiniteTransition = if (animated) rememberInfiniteTransition(label = "flower_anim") else null
    val flowerScale = infiniteTransition?.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.038f,
        animationSpec = infiniteRepeatable(
            animation = tween(2950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flower_scale"
    )?.value ?: 1.0f

    val rotationOffset = infiniteTransition?.animateFloat(
        initialValue = 0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flower_rotation"
    )?.value ?: 0f

    val glowAlpha = infiniteTransition?.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flower_glow"
    )?.value ?: 0.5f

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        // Aura suave de fondo (Glow)
        if (animated) {
            Box(
                modifier = Modifier
                    .size((size * 1.35f).dp)
                    .alpha(glowAlpha)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x44D3AEF0),
                                Color(0x18D6B0E9),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        Canvas(
            modifier = Modifier
                .size(size.dp)
                .scale(flowerScale)
                .rotate(-25f + rotationOffset)
        ) {
            val sweepBrush = Brush.sweepGradient(colors)
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = 0f,
                        right = this@Canvas.size.width,
                        bottom = this@Canvas.size.height,
                        topLeftCornerRadius = CornerRadius(this@Canvas.size.width * 0.45f, this@Canvas.size.height * 0.55f),
                        topRightCornerRadius = CornerRadius(this@Canvas.size.width * 0.65f, this@Canvas.size.height * 0.40f),
                        bottomRightCornerRadius = CornerRadius(this@Canvas.size.width * 0.55f, this@Canvas.size.height * 0.65f),
                        bottomLeftCornerRadius = CornerRadius(this@Canvas.size.width * 0.40f, this@Canvas.size.height * 0.50f)
                    )
                )
            }
            drawPath(path = path, brush = sweepBrush)
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
    val starScale = infiniteTransition?.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_scale"
    )?.value ?: 1f

    val starAlpha = infiniteTransition?.animateFloat(
        initialValue = 0.46f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_alpha"
    )?.value ?: 1f

    val starRotation = infiniteTransition?.animateFloat(
        initialValue = -10f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_rot"
    )?.value ?: 0f

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
                    .scale(starScale)
                    .alpha(starAlpha)
                    .rotate(starRotation)
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
    onThemeClick: () -> Unit,
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

        // Acciones superiores
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de chat (opcional)
            if (onChatClick != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onChatClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Chat",
                        tint = tokens.muted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Botón de tema
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onThemeClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = "Cambiar tema",
                    tint = tokens.muted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Botón de notificaciones con dot-alert
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNotificationsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notificaciones",
                    tint = tokens.muted,
                    modifier = Modifier.size(20.dp)
                )
                if (hasNotificationAlert) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Ajustes",
                        tint = tokens.muted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
