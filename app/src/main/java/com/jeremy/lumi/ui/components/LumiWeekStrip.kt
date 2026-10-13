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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.screens.home.CycleDayUi
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import java.time.LocalDate

/**
 * Tira semanal compacta de 7 días aprobada en el prototipo interactivo Lumi.
 *
 * Incluye:
 * - Cabecera con "Tu semana" y enlace "Ver calendario ↗"
 * - Grid de 7 días (Lunes a Domingo)
 * - Indicador de hoy, indicador de selección con píldora suave
 * - Puntos de color semántico de fase menstrual
 * - Leyenda explicativa de las 4 fases
 */
@Composable
fun LumiWeekStrip(
    modifier: Modifier = Modifier,
    weekDays: List<CycleDayUi>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onViewCalendarClick: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Cabecera de sección
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tu semana",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = tokens.text
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewCalendarClick)
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ver calendario ↗",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.accent
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fila de 7 días
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekDays.take(7).forEach { day ->
                val isSelected = selectedDate?.isEqual(day.date) == true || (selectedDate == null && day.isToday)
                val dotColor = when (day.phase) {
                    CyclePhase.MENSTRUAL -> tokens.rose
                    CyclePhase.FOLLICULAR -> tokens.gold
                    CyclePhase.OVULATION -> tokens.mint
                    CyclePhase.LUTEAL -> tokens.accent
                    else -> tokens.accent.copy(alpha = 0.5f)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) tokens.accentSoft else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) tokens.accent.copy(alpha = 0.55f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectDate(day.date) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        // Nombre del día (Lun, Mar...)
                        Text(
                            text = day.weekdayLabel.take(3),
                            fontSize = 10.sp,
                            color = tokens.dim
                        )

                        // Número del día
                        Text(
                            text = "${day.dayOfMonth}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (day.isToday) tokens.accent else tokens.text
                        )

                        // Punto indicador de fase
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Leyenda de fases
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhaseLegendItem(label = "Menstrual", color = tokens.rose)
            Spacer(modifier = Modifier.size(12.dp))
            PhaseLegendItem(label = "Folicular", color = tokens.gold)
            Spacer(modifier = Modifier.size(12.dp))
            PhaseLegendItem(label = "Ovulación", color = tokens.mint)
            Spacer(modifier = Modifier.size(12.dp))
            PhaseLegendItem(label = "Lútea", color = tokens.accent)
        }
    }
}

@Composable
private fun PhaseLegendItem(label: String, color: Color) {
    val tokens = LumiThemeTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 9.5.sp,
            color = tokens.dim
        )
    }
}
