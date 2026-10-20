package com.jeremy.lumi.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jeremy.lumi.ui.theme.LumiThemeTokens

/**
 * Adaptación nativa del prototipo Lumi_Home_Menu_Desplegable_v1.html.
 * El disparador permanece anclado a la derecha; la cápsula crece a la izquierda.
 * Acciones conservadas: Chat, Temas, Notificaciones, Ajustes.
 */
@Composable
fun LumiCollapsibleActions(
    hasNotificationAlert: Boolean,
    onChatClick: (() -> Unit)?,
    onThemeClick: (() -> Unit)?,
    onNotificationsClick: () -> Unit,
    onSettingsClick: (() -> Unit)?
) {
    val tokens = LumiThemeTokens.current
    // Compose aplica automáticamente la escala de animaciones del sistema (incluido 0x).
    val duration = 350
    val easing = remember { CubicBezierEasing(.5f, -.35f, .35f, 1.5f) }
    val screenDp = LocalConfiguration.current.screenWidthDp
    // No invadir el logo en teléfonos estrechos: cada opción sigue teniendo área táctil.
    val itemSize = if (screenDp < 375) 38.dp else 44.dp
    val actions = listOfNotNull(
        onChatClick?.let { Triple("Chat", Icons.Outlined.ChatBubbleOutline, it) },
        onThemeClick?.let { Triple("Temas", Icons.Outlined.Palette, it) },
        Triple("Notificaciones", Icons.Outlined.Notifications, onNotificationsClick),
        onSettingsClick?.let { Triple("Ajustes", Icons.Outlined.Settings, it) }
    )
    val targetWidth = 44.dp + (itemSize * actions.size) + 8.dp
    var expanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = expanded) { expanded = false }
    val railWidth by animateDpAsState(
        if (expanded) targetWidth else 44.dp,
        animationSpec = tween(durationMillis = duration, easing = CubicBezierEasing(.27f, .9f, .3f, 1f)),
        label = "lumi_actions_rail"
    )
    val morph by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(420, easing = easing),
        label = "lumi_hamburger_morph"
    )
    Row(
        modifier = Modifier
            .width(railWidth)
            .height(44.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (expanded) tokens.panel else tokens.panel.copy(alpha = .85f))
            .then(if (expanded) Modifier.border(BorderStroke(.8.dp, tokens.line), RoundedCornerShape(24.dp)) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        actions.forEachIndexed { index, (name, icon, onClick) ->
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(225, delayMillis = 30 + (actions.size - 1 - index) * 30)) +
                    slideInHorizontally(
                        animationSpec = tween(300, delayMillis = 30 + (actions.size - 1 - index) * 30, easing = CubicBezierEasing(.27f, .9f, .3f, 1f)),
                        initialOffsetX = { it / 2 }
                    ),
                exit = fadeOut(tween(110))
            ) {
                Box(
                    Modifier.size(itemSize)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = name) {
                            expanded = false
                            onClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = name, tint = tokens.muted, modifier = Modifier.size(19.dp))
                    if (name == "Notificaciones" && hasNotificationAlert) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(top = 7.dp, end = 7.dp)
                                .size(6.dp).clip(CircleShape).background(tokens.rose)
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier.size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Cerrar menú" else "Abrir menú") {
                    expanded = !expanded
                }
                .semantics {
                    contentDescription = if (expanded) "Cerrar menú de opciones" else "Abrir menú de opciones"
                    stateDescription = if (expanded) "Expandido" else "Contraído"
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(26.dp)) {
                val ratio = size.width / 26f
                fun pt(x: Float, y: Float): Offset = Offset(x * ratio, y * ratio)
                val stroke = 2.5.dp.toPx()
                // Three parallel bars transform into diagonals. The middle fades.
                drawLine(tokens.accent, pt(5f, 6f), pt(21f, 6f + 14f * morph),
                    strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(tokens.accent.copy(alpha = (1f - morph).coerceIn(0f, 1f)),
                    pt(5f, 13f), pt(21f, 13f),
                    strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(tokens.accent, pt(5f, 20f), pt(21f, 20f - 14f * morph),
                    strokeWidth = stroke, cap = StrokeCap.Round)
            }
            if (hasNotificationAlert && !expanded) {
                Box(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 7.dp)
                    .size(6.dp).clip(CircleShape).background(tokens.rose))
            }
        }
    }
}
