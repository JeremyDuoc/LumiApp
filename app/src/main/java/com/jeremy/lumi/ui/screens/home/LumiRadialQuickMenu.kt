package com.jeremy.lumi.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Réplica nativa del menú de 20essentials en el HTML Lumi v12.
 *
 * Geometría original: lienzo 200x200; toggle 60; entradas 50;
 * pivote en (100,100); al abrir i=0,1,2 gira 0°,45°,90°.
 *
 * La animación central dura 1200ms (CSS ease-in-out) y los elementos
 * duran 500ms con retardo 0/100/200ms (CSS `transition: .5s`).
 * Ningún elemento usa emojis; todos usan los PNG oficiales del prototipo.
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
    val reduce = reducedMotion // puede vincularse a accesibilidad del sistema desde el host
    val duration = if (reduce) 0 else 1200
    val staggerDuration = if (reduce) 0 else 500
    val easing = CubicBezierEasing(.42f, 0f, .58f, 1f) // CSS ease-in-out
    val itemEasing = CubicBezierEasing(.25f, .1f, .25f, 1f) // CSS ease
    val toggleAngle by animateFloatAsState(
        targetValue = if (expanded) 315f else 0f,
        animationSpec = tween(duration, easing = easing),
        label = "lumi_radial_toggle_315"
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
                R.drawable.lumi_quick_diary,
                Color(0xFF62DACA), Color(0xFF10D4A9), onLogClick),
            QuickEntry(1, stringResource(R.string.quick_action_period_started), R.drawable.lumi_quick_period,
                Color(0xFFF97D98), Color(0xFFD41F46), onPeriodClick),
            QuickEntry(2, stringResource(R.string.quick_action_create_reminder), R.drawable.lumi_quick_reminder,
                Color(0xFFF0F8FF), Color(0xFFD169D1), onReminderClick)
        )

        // Debajo del toggle y sobre el Home. La última acción llega arriba.
        items.forEach { entry ->
            val progress by animateFloatAsState(
                targetValue = if (expanded) 1f else 0f,
                animationSpec = tween(
                    durationMillis = staggerDuration,
                    delayMillis = if (reduce) 0 else 100 * entry.index,
                    easing = itemEasing
                ),
                label = "lumi_radial_action_${entry.index}"
            )
            val angleDegrees = 45f * entry.index * progress
            val theta = angleDegrees * PI.toFloat() / 180f
            // CSS: cerrado rotate(0deg) translateX(70px), abierto rotate(45*i).
            val radius = 5f + 70f * progress
            val centerX = 100f - radius * cos(theta)
            val centerY = 100f - radius * sin(theta)
            val xPx = with(density) { (centerX - 25f).dp.roundToPx() }
            val yPx = with(density) { (centerY - 25f).dp.roundToPx() }

            if (expanded || progress > 0.001f) {
                // Caja visual de 90dp: la luz puede salir del círculo sin
                // recortarse. El área pulsable sigue siendo de solo 50dp.
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                xPx - with(density) { 20.dp.roundToPx() },
                                yPx - with(density) { 20.dp.roundToPx() }
                            )
                        }
                        .size(90.dp)
                        .graphicsLayer { rotationZ = -angleDegrees },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(90.dp)) {
                        val c = center
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    entry.first.copy(alpha = .40f),
                                    entry.second.copy(alpha = .17f),
                                    Color.Transparent
                                ),
                                center = c, radius = size.width * .5f
                            ),
                            center = c,
                            radius = size.width * .5f
                        )
                    }
                    Box(
                        Modifier.size(50.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(entry.first, entry.second)))
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
                        Image(
                            painter = painterResource(entry.image),
                            contentDescription = null,
                            modifier = Modifier.size(
                                when (entry.index) { 1 -> 38.dp; 2 -> 37.dp; else -> 35.dp }
                            )
                        )
                    }
                }
            }
        }

        // Halo tenue del botón central, sin fondo oscurecedor de pantalla.
        Canvas(Modifier.offset(x = 45.dp, y = 45.dp).size(110.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tokens.accent.copy(alpha = .24f), Color.Transparent),
                    center = center,
                    radius = size.width * .50f
                ),
                radius = size.width * .50f
            )
        }
        Box(
            Modifier
                .offset(x = 70.dp, y = 70.dp)
                .size(60.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(tokens.rose, tokens.accent)))
                .border(.6.dp, Color.White.copy(alpha = .24f), CircleShape)
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
                val width = 2.5.dp.toPx()
                val ink = Color(0xFF30213A)
                drawLine(ink, Offset(center.x, size.height * .15f),
                    Offset(center.x, size.height * .85f), width)
                drawLine(ink, Offset(size.width * .15f, center.y),
                    Offset(size.width * .85f, center.y), width)
            }
        }
    }
}

private data class QuickEntry(
    val index: Int,
    val label: String,
    @DrawableRes val image: Int,
    val first: Color,
    val second: Color,
    val onClick: () -> Unit
)
