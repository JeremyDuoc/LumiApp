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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import com.jeremy.lumi.R
@Composable
fun StoryDetailScreen(
    link: PartnerLink,
    currentUid: String,
    onClose: () -> Unit,
    onSendCareAction: (CareAction) -> Unit,
    isOnCooldown: Boolean = false
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
        ?: (if (isOwner) stringResource(R.string.partner_default_name) else link.ownerDisplayName ?: stringResource(R.string.partner_default_link_name))

    val phaseEmoji = when (phase) {
        CyclePhase.MENSTRUAL -> "🌺"
        CyclePhase.FOLLICULAR -> "🌱"
        CyclePhase.OVULATION -> "✨"
        CyclePhase.LUTEAL -> "🌙"
        CyclePhase.PREGNANCY -> "🤰"
        else -> "🌸"
    }

    val phaseLabel = when (phase) {
        CyclePhase.MENSTRUAL -> stringResource(R.string.phase_name_menstrual)
        CyclePhase.FOLLICULAR -> stringResource(R.string.phase_name_follicular)
        CyclePhase.OVULATION -> stringResource(R.string.phase_name_ovulation)
        CyclePhase.LUTEAL -> stringResource(R.string.phase_name_luteal)
        CyclePhase.PREGNANCY -> stringResource(R.string.phase_name_pregnancy)
        else -> stringResource(R.string.phase_name_unknown)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            phaseColor.copy(alpha = 0.25f),
                            Color(0xFF0E0A1A)
                        ),
                        radius = 900f
                    )
                )
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
                    color = phaseColor,
                    trackColor = phaseColor.copy(alpha = 0.25f),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Header info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StoryAvatar(displayName = name, phase = phase, size = 42.dp, animated = false)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.btn_close), tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Central Phase Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "phase_pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(pulseScale)
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
                                text = stringResource(R.string.story_mood, snapshot.currentMood),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    val phaseTip = when (phase) {
                        CyclePhase.MENSTRUAL  -> stringResource(R.string.story_phase_menstrual_tip)
                        CyclePhase.FOLLICULAR -> stringResource(R.string.story_phase_follicular_tip)
                        CyclePhase.OVULATION  -> stringResource(R.string.story_phase_ovulation_tip)
                        CyclePhase.LUTEAL     -> stringResource(R.string.story_phase_luteal_tip)
                        else                  -> null
                    }

                    if (phaseTip != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "💡 $phaseTip",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Care Actions Row
                Text(
                    text = if (isOnCooldown) stringResource(R.string.story_care_action_cooldown) else stringResource(R.string.story_send_care_action),
                    color = Color.White.copy(alpha = if (isOnCooldown) 0.4f else 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CareActionButton(
                        emoji = "🤗", label = stringResource(R.string.story_action_hug),
                        isOnCooldown = isOnCooldown,
                        phaseColor = phaseColor,
                        onClick = { onSendCareAction(CareAction.HUG); onClose() }
                    )
                    CareActionButton(
                        emoji = "🍵", label = stringResource(R.string.story_action_tea),
                        isOnCooldown = isOnCooldown,
                        phaseColor = phaseColor,
                        onClick = { onSendCareAction(CareAction.TEA); onClose() }
                    )
                    CareActionButton(
                        emoji = "☕", label = stringResource(R.string.story_action_coffee),
                        isOnCooldown = isOnCooldown,
                        phaseColor = phaseColor,
                        onClick = { onSendCareAction(CareAction.COFFEE); onClose() }
                    )
                    CareActionButton(
                        emoji = "🍫", label = stringResource(R.string.story_action_choco),
                        isOnCooldown = isOnCooldown,
                        phaseColor = phaseColor,
                        onClick = { onSendCareAction(CareAction.CHOCOLATE); onClose() }
                    )
                    CareActionButton(
                        emoji = "💊", label = stringResource(R.string.story_action_help),
                        isOnCooldown = isOnCooldown,
                        phaseColor = phaseColor,
                        onClick = { onSendCareAction(CareAction.PHARMACY); onClose() }
                    )
                }
            }
        }
    }
}

@Composable
fun CareActionButton(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    isOnCooldown: Boolean = false,
    phaseColor: Color = Color.White
) {
    val contentAlpha = if (isOnCooldown) 0.35f else 1f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isOnCooldown, onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(phaseColor.copy(alpha = if (isOnCooldown) 0.05f else 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 26.sp, color = Color.White.copy(alpha = contentAlpha))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = contentAlpha),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}