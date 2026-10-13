package com.jeremy.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.ui.theme.LumiThemeTokens

/**
 * Tarjeta de registro diario "Un momento para ti" aprobada en el prototipo interactivo Lumi.
 *
 * Incluye:
 * - Superficie con degradado sutil de panel a panel2
 * - Borde ultrafino `tokens.line` y radio de 24dp
 * - Marca de agua decorativa `✳`
 * - Ícono en contenedor de acento suave
 * - Título y subtítulo de estado ("Todavía no has registrado..." o "Hoy te sientes...")
 * - Botón de ancho completo "Registrar mi día ↗" / "Ver mi registro ↗"
 */
@Composable
fun LumiTodayCard(
    modifier: Modifier = Modifier,
    hasEntry: Boolean = false,
    entrySubtitle: String? = null,
    onLogClick: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    val subtitle = entrySubtitle ?: if (hasEntry) {
        "Tu registro del día está guardado."
    } else {
        "Todavía no has registrado cómo te sientes."
    }

    val buttonText = if (hasEntry) "Ver mi registro" else "Registrar mi día"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(tokens.panel, tokens.panel2)
                )
            )
            .border(1.dp, tokens.line, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        // Marca de agua decorativa ✳
        Text(
            text = "✳",
            fontSize = 140.sp,
            lineHeight = 140.sp,
            color = tokens.accent.copy(alpha = 0.06f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-40).dp)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Caja del ícono
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(tokens.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditCalendar,
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Un momento para ti",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = tokens.muted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Botón de acción principal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.accent)
                    .clickable(onClick = onLogClick)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tokens.isDark) tokens.bg else androidx.compose.ui.graphics.Color.White
                    )
                    Text(
                        text = "↗",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tokens.isDark) tokens.bg else androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}

/**
 * Nota inferior explicativa con enlace modal "Cómo lo calcula Lumi".
 */
@Composable
fun LumiPredictionFootnote(
    modifier: Modifier = Modifier,
    onLearnMoreClick: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "✳",
            fontSize = 12.sp,
            color = tokens.muted
        )
        Column {
            Text(
                text = "Las predicciones se basan en tus registros y pueden cambiar.",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = tokens.muted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Cómo lo calcula Lumi ↗",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.accent,
                modifier = Modifier.clickable(onClick = onLearnMoreClick)
            )
        }
    }
}
