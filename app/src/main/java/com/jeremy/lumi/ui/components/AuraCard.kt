package com.jeremy.lumi.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jeremy.lumi.ui.theme.LocalIsDarkTheme

/**
 * Tarjeta con resplandor ambiental atmosférico inspirada en Calm y Flo.
 * Utiliza [LocalIsDarkTheme] para garantizar armonía absoluta con el tema seleccionado
 * en Lumi (sin depender del modo del sistema Android).
 */
@Composable
fun AuraCard(
    primaryAuraColor: Color,
    modifier: Modifier = Modifier,
    secondaryAuraColor: Color = primaryAuraColor,
    cornerRadius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraBreath")
    
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraScale"
    )

    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraAlpha"
    )

    val isDark = LocalIsDarkTheme.current
    val surfaceColor = if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000)
    val borderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                      else Color(0x1A000000)

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceColor)
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Halo 1: Esquina superior derecha (color activo de la fase)
                val aura1Radius = (canvasWidth * 0.45f) * breathScale
                val center1 = Offset(x = canvasWidth * 0.88f, y = canvasHeight * 0.18f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryAuraColor.copy(alpha = (if (isDark) 0.38f else 0.22f) * breathAlpha),
                            primaryAuraColor.copy(alpha = (if (isDark) 0.12f else 0.06f) * breathAlpha),
                            Color.Transparent
                        ),
                        center = center1,
                        radius = aura1Radius
                    ),
                    radius = aura1Radius,
                    center = center1
                )

                // Halo 2: Esquina inferior izquierda (color secundario/armónico)
                val aura2Radius = (canvasWidth * 0.48f) * (2.13f - breathScale)
                val center2 = Offset(x = canvasWidth * 0.12f, y = canvasHeight * 0.82f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryAuraColor.copy(alpha = (if (isDark) 0.28f else 0.16f) * breathAlpha),
                            secondaryAuraColor.copy(alpha = (if (isDark) 0.08f else 0.04f) * breathAlpha),
                            Color.Transparent
                        ),
                        center = center2,
                        radius = aura2Radius
                    ),
                    radius = aura2Radius,
                    center = center2
                )
            }
            .border(1.dp, borderColor, shape)
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}
