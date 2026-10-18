package com.jeremy.lumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.theme.LumiThemeTokens

/**
 * Tarjeta editorial compacta de insight diario para el Home de Lumi.
 *
 * Muestra información relevante únicamente cuando existe contenido útil real
 * sobre la fase del ciclo actual, evitando tarjetas genéricas innecesarias.
 */
@Composable
fun LumiEditorialInsightCard(
    phase: CyclePhase,
    currentDay: Int,
    cycleLength: Int = 28,
    isDiscreetMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isDiscreetMode || phase == CyclePhase.UNKNOWN || currentDay <= 0) return

    val message = getDynamicPhaseMessage(phase, currentDay, cycleLength)
    if (message.isBlank()) return

    val tokens = LumiThemeTokens.current
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(shape)
            .background(tokens.panel)
            .border(1.dp, tokens.line, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabecera con glifo abstracto Lumi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LumiIconInsight(
                    tint = tokens.accent,
                    size = 15.dp
                )
                Text(
                    text = stringResource(R.string.home_moment_for_you),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text,
                    letterSpacing = (-0.2).sp
                )
            }

            // Mensaje editorial relevante
            Text(
                text = message,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = tokens.muted
            )
        }
    }
}

@Composable
fun getDynamicPhaseMessage(phase: CyclePhase, currentDay: Int, cycleLength: Int): String {
    return when (phase) {
        CyclePhase.MENSTRUAL -> {
            if (currentDay <= 2) stringResource(R.string.home_insight_menstrual_1)
            else stringResource(R.string.home_insight_menstrual_2)
        }
        CyclePhase.FOLLICULAR -> {
            stringResource(R.string.home_insight_follicular)
        }
        CyclePhase.OVULATION -> {
            stringResource(R.string.home_insight_ovulation)
        }
        CyclePhase.LUTEAL -> {
            val daysToNext = cycleLength - currentDay
            if (daysToNext in 1..4) {
                stringResource(R.string.home_insight_luteal_window)
            } else if (daysToNext > 7) {
                stringResource(R.string.home_insight_luteal_1)
            } else {
                stringResource(R.string.home_insight_luteal_2)
            }
        }
        else -> ""
    }
}
