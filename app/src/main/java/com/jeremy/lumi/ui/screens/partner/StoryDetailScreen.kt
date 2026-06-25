package com.jeremy.lumi.ui.screens.partner

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.border
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.ui.theme.LocalPhaseColors

@Composable
fun StoryDetailScreen(
    link: PartnerLink,
    currentUid: String,
    onClose: () -> Unit,
    onSendCareAction: (CareAction) -> Unit
) {
    val isOwner = link.ownerUid == currentUid
    val snapshot = if (isOwner) link.partnerSnapshot else link.ownerSnapshot
    val phase = snapshot?.currentPhase ?: CyclePhase.UNKNOWN
    val phaseColors = LocalPhaseColors.current

    val phaseColor = when (phase) {
        CyclePhase.MENSTRUAL -> phaseColors.menstrual
        CyclePhase.FOLLICULAR -> phaseColors.follicular
        CyclePhase.OVULATION -> phaseColors.ovulation
        CyclePhase.LUTEAL -> phaseColors.luteal
        else -> MaterialTheme.colorScheme.primary
    }

    val name = link.relationLabel.takeIf { it.isNotBlank() }
        ?: (if (isOwner) "Pareja" else link.ownerDisplayName ?: "Vínculo")

    val phaseEmoji = when (phase) {
        CyclePhase.MENSTRUAL -> "🌺"
        CyclePhase.FOLLICULAR -> "🌱"
        CyclePhase.OVULATION -> "✨"
        CyclePhase.LUTEAL -> "🌙"
        CyclePhase.PREGNANCY -> "🤰"
        else -> "🌸"
    }

    val phaseLabel = when (phase) {
        CyclePhase.MENSTRUAL -> "Menstrual"
        CyclePhase.FOLLICULAR -> "Folicular"
        CyclePhase.OVULATION -> "Ovulación"
        CyclePhase.LUTEAL -> "Lútea"
        CyclePhase.PREGNANCY -> "Embarazo"
        else -> "Fase Desconocida"
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
        ) {
            // ProgressBar and Header
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp, bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                // Fake Progress Bar
                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color.White.copy(alpha = 0.8f),
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Header info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StoryAvatar(displayName = name, phase = phase, size = 42.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Central Phase Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(phaseColor.copy(alpha = 0.2f))
                            .border(2.dp, phaseColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = phaseEmoji, fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = phaseLabel,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }
                    
                    if (snapshot?.currentMood != null && snapshot.currentMood.isNotBlank()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Ánimo: ${snapshot.currentMood}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Care Actions Row
                Text(
                    text = "Enviar un detalle...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CareActionButton(emoji = "🤗", label = "Abrazo", onClick = { onSendCareAction(CareAction.HUG); onClose() })
                    CareActionButton(emoji = "🍵", label = "Té", onClick = { onSendCareAction(CareAction.TEA); onClose() })
                    CareActionButton(emoji = "☕", label = "Café", onClick = { onSendCareAction(CareAction.COFFEE); onClose() })
                    CareActionButton(emoji = "🍫", label = "Choco", onClick = { onSendCareAction(CareAction.CHOCOLATE); onClose() })
                    CareActionButton(emoji = "💊", label = "Ayuda", onClick = { onSendCareAction(CareAction.PHARMACY); onClose() })
                }
            }
        }
    }
}

@Composable
fun CareActionButton(emoji: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 26.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
