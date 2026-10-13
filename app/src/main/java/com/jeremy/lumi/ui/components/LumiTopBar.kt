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
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.ui.theme.LumiThemeTokens

/**
 * Logotipo orgánico de la flor de Lumi con degradado cónico multicolor.
 */
@Composable
fun LumiBrandFlower(modifier: Modifier = Modifier, size: Int = 20) {
    val colors = listOf(
        Color(0xFFE9B3C4),
        Color(0xFFD4B3ED),
        Color(0xFFA8D1C9),
        Color(0xFFF5CCAA),
        Color(0xFFE9B3C4)
    )

    Canvas(
        modifier = modifier
            .size(size.dp)
            .rotate(-25f)
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

/**
 * Cabecera superior unificada de Lumi:
 * - Brand: [Flor] + "lumi" + "✦"
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
        // Marca Lumi
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LumiBrandFlower(size = 21)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "lumi",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.5).sp,
                    color = tokens.text
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "✦",
                    fontSize = 11.sp,
                    color = tokens.dim
                )
            }
        }

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
