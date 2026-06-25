package com.jeremy.lumi.ui.screens.partner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.theme.LocalPhaseColors

@Composable
fun StoryAvatar(
    displayName: String,
    phase: CyclePhase,
    size: Dp,
    isPending: Boolean = false
) {
    val phaseColors = LocalPhaseColors.current
    val basePhaseColor = when (phase) {
        CyclePhase.MENSTRUAL -> phaseColors.menstrual
        CyclePhase.FOLLICULAR -> phaseColors.follicular
        CyclePhase.OVULATION -> phaseColors.ovulation
        CyclePhase.LUTEAL -> phaseColors.luteal
        else -> MaterialTheme.colorScheme.primary
    }
    
    val phaseColor = if (isPending) Color.Gray else basePhaseColor
    
    val initial = if (displayName.isNotBlank()) displayName.first().uppercase() else "?"

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(phaseColor.copy(alpha = 0.2f))
            .border(2.dp, phaseColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = phaseColor,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp
        )
    }
}
