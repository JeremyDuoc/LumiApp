package com.jeremy.lumi.ui.screens.home

import androidx.activity.compose.BackHandler
import android.animation.ValueAnimator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.components.LumiIconMyDay
import com.jeremy.lumi.ui.components.LumiIconPeriod
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Acciones radiales con las mismas superficies, bordes e iconos del Home.
 * El color identifica cada acción sin iluminar el fondo; los iconos permanecen
 * derechos mientras los botones se despliegan desde el centro.
 */
@Composable
fun LumiRadialQuickMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLogClick: () -> Unit,
    onPeriodClick: () -> Unit,
    onReminderClick: () -> Unit,
    modifier: Modifier = Modifier,
    diaryAlreadyLogged: Boolean = false,
    reducedMotion: Boolean = false,
    rightMargin: Dp = 16.dp,
    bottomMargin: Dp = 16.dp
) {
    val tokens = LumiThemeTokens.current
    val density = LocalDensity.current
    val reduce = reducedMotion || !ValueAnimator.areAnimatorsEnabled()
    val duration = if (reduce) 0 else 320
    val staggerDuration = if (reduce) 0 else if (expanded) 360 else 240
    val easing = CubicBezierEasing(.22f, .8f, .25f, 1f)
    val toggleAngle by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(duration, easing = easing),
        label = "lumi_radial_toggle"
    )
    val toggleSurface by animateColorAsState(
        targetValue = if (expanded) tokens.panel2 else tokens.accent,
        animationSpec = tween(duration),
        label = "lumi_radial_surface"
    )
    val toggleInk by animateColorAsState(
        targetValue = if (expanded) tokens.accent else if (tokens.isDark) tokens.bg else Color.White,
        animationSpec = tween(duration),
        label = "lumi_radial_ink"
    )

    BackHandler(enabled = expanded) { onExpandedChange(false) }

    val closeActionDesc = stringResource(R.string.quick_action_close)
    val openActionDesc = stringResource(R.string.quick_action_open)

    // El centro del dial queda a 46dp del borde derecho/inferior del Home,
    // por tanto el borde del botón de 60dp queda a 16dp de ambos lados.
    Box(
        modifier = modifier
            .offset(x = 100.dp - (rightMargin + 30.dp), y = 100.dp - (bottomMargin + 30.dp))
            .size(200.dp)
    ) {
        val items = listOf(
            QuickEntry(0, if (diaryAlreadyLogged) stringResource(R.string.quick_action_edit_day) else stringResource(R.string.quick_action_log_day),
                tokens.mint, onLogClick),
            QuickEntry(1, stringResource(R.string.quick_action_period_started),
                tokens.rose, onPeriodClick),
            QuickEntry(2, stringResource(R.string.quick_action_create_reminder),
                tokens.accent, onReminderClick)
        )

        // Debajo del toggle y sobre el Home. La última acción llega arriba.
        items.forEach { entry ->
            val progress by animateFloatAsState(
                targetValue = if (expanded) 1f else 0f,
                animationSpec = tween(
                    durationMillis = staggerDuration,
                    delayMillis = if (reduce || !expanded) 0 else 45 * entry.index,
                    easing = easing
                ),
                label = "lumi_radial_action_${entry.index}"
            )
            val angleDegrees = 45f * entry.index
            val theta = angleDegrees * PI.toFloat() / 180f
            val radius = 82f * progress
            val centerX = 100f - radius * cos(theta)
            val centerY = 100f - radius * sin(theta)
            val xPx = with(density) { (centerX - 26f).dp.roundToPx() }
            val yPx = with(density) { (centerY - 26f).dp.roundToPx() }
            val tint = if (tokens.isDark) entry.tint else lerp(entry.tint, tokens.text, .42f)

            if (expanded || progress > 0.001f) {
                Box(
                    Modifier
                        .offset { IntOffset(xPx, yPx) }
                        .size(52.dp)
                        .graphicsLayer {
                            alpha = progress
                            scaleX = .72f + .28f * progress
                            scaleY = scaleX
                        }
                        .shadow(if (tokens.isDark) 2.dp else 5.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(
                            lerp(tokens.panel, tint, .10f),
                            lerp(tokens.panel, tint, .04f)
                        )))
                        .border(1.dp, tint.copy(alpha = .24f), CircleShape)
                        .semantics { contentDescription = entry.label }
                        .clickable(
                            enabled = expanded,
                            role = Role.Button,
                            onClick = {
                                onExpandedChange(false)
                                entry.onClick()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (entry.index) {
                        0 -> LumiIconMyDay(tint = tint, size = 24.dp)
                        1 -> LumiIconPeriod(tint = tint, size = 24.dp)
                        else -> Icon(Icons.Outlined.Notifications, null, tint = tint, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        Box(
            Modifier
                .offset(x = 70.dp, y = 70.dp)
                .size(60.dp)
                .shadow(if (expanded) 2.dp else 6.dp, CircleShape)
                .clip(CircleShape)
                .background(toggleSurface)
                .border(1.dp, tokens.accent.copy(alpha = .28f), CircleShape)
                .clickable(
                    role = Role.Button,
                    onClick = { onExpandedChange(!expanded) }
                )
                .semantics {
                    contentDescription = if (expanded) closeActionDesc else openActionDesc
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                Modifier.size(29.dp).graphicsLayer { rotationZ = toggleAngle }
            ) {
                val width = 2.dp.toPx()
                drawLine(toggleInk, Offset(center.x, size.height * .18f),
                    Offset(center.x, size.height * .82f), width, cap = StrokeCap.Round)
                drawLine(toggleInk, Offset(size.width * .18f, center.y),
                    Offset(size.width * .82f, center.y), width, cap = StrokeCap.Round)
            }
        }
    }
}

private data class QuickEntry(
    val index: Int,
    val label: String,
    val tint: Color,
    val onClick: () -> Unit
)
