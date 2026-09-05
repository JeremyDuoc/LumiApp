package com.jeremy.lumi.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Estados vacíos de Lumi: en lugar de un hueco en blanco, un momento cálido
 * que explica qué aparecerá aquí e invita a la acción.
 *
 * Dos variantes:
 *  - [LumiEmptyState]     → vertical, para llenar áreas completas
 *                           (Insights sin datos, Diario sin notas).
 *  - [LumiEmptyStateCard] → tarjeta compacta horizontal, para insertar dentro
 *                           de una pantalla con contenido (Calendario).
 *
 * Solo vectores: el ícono flota suavemente sobre un halo de dos capas con
 * puntos decorativos que respiran. Sin emojis, sin imágenes rasterizadas.
 */
@Composable
fun LumiEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    ctaText: String? = null,
    ctaIcon: ImageVector? = null,
    onCtaClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingHaloIcon(icon = icon)
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 25.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        if (ctaText != null && onCtaClick != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onCtaClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp)
            ) {
                if (ctaIcon != null) {
                    Icon(
                        imageVector = ctaIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(text = ctaText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Variante tarjeta: ícono + mensaje + botón de acción a lo ancho.
 * Pensada para convivir con el resto del contenido de la pantalla.
 */
@Composable
fun LumiEmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    ctaText: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onCtaClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(text = ctaText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Ícono flotante sobre halo de dos capas, con dos puntos decorativos que
 * "respiran". Toda la animación es sutil y en bucle lento para no cansar.
 */
@Composable
private fun FloatingHaloIcon(icon: ImageVector) {
    val transition = rememberInfiniteTransition(label = "estado_vacio")

    // Flotación vertical del ícono
    val floatOffset by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "flotacion"
    )
    // Respiración de los puntos decorativos
    val dotAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "respiracion_puntos"
    )

    Box(contentAlignment = Alignment.Center) {
        // Halo exterior e interior
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape)
        )
        // Puntos decorativos que respiran
        Box(
            modifier = Modifier
                .offset(x = 38.dp, y = (-30).dp)
                .size(9.dp)
                .background(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = dotAlpha),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .offset(x = (-40).dp, y = 26.dp)
                .size(6.dp)
                .background(
                    MaterialTheme.colorScheme.secondary.copy(alpha = dotAlpha),
                    CircleShape
                )
        )
        // Ícono flotante
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer { translationY = floatOffset.dp.toPx() }
        )
    }
}
