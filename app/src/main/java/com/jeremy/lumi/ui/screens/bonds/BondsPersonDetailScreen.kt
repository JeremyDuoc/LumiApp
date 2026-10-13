package com.jeremy.lumi.ui.screens.bonds

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.SharePreferences
import com.jeremy.lumi.ui.screens.partner.AvatarCatalog

/**
 * Pantalla de Detalle de Vínculo Individual.
 *
 * Se adapta estrictamente a los permisos reales concedidos:
 * si no hay autorización para una categoría, no revela datos ni conjeturas.
 */
@Composable
fun BondsPersonDetailScreen(
    member: BondMember,
    onSendGesture: (CareAction) -> Unit,
    onOpenCalendar: () -> Unit,
    onUpdatePrivacy: (SharePreferences) -> Unit,
    onUnlink: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showUnlinkDialog by remember { mutableStateOf(false) }

    // Estado local de lo que YO comparto con ESTA persona
    var myPhase by remember { mutableStateOf(member.mySharePrefs.phase) }
    var myMood by remember { mutableStateOf(member.mySharePrefs.mood) }
    var mySymptoms by remember { mutableStateOf(member.mySharePrefs.symptoms) }
    var myPredictions by remember { mutableStateOf(member.mySharePrefs.predictions) }

    val spec = AvatarCatalog.byId(member.avatarId)
    val gradient = spec?.gradient ?: AvatarCatalog.fallbackGradient(member.displayName)

    Scaffold(
        containerColor = BondsTheme.WarmBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.pm_back),
                        tint = BondsTheme.DeepNavy
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = member.displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BondsTheme.DeepNavy
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Avatar principal grande con aro degradado ────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(BondsTheme.AvatarRingGradient)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(BondsTheme.WarmWhite)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Brush.linearGradient(gradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (spec != null) {
                                Icon(
                                    imageVector = spec.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            } else {
                                Text(
                                    text = member.displayName.trim().take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Nombre y estado
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = member.displayName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BondsTheme.DeepNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (member.isOwner) "Vínculo creado por ti" else "Conectada a tu círculo",
                    fontSize = 13.sp,
                    color = BondsTheme.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Botones de acción rápida ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onSendGesture(CareAction.TEA) },
                    shape = BondsTheme.PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BondsTheme.DeepNavy,
                        contentColor = if (LumiThemeTokens.current.isDark) Color(0xFF0B0B11) else Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enviar detalle", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onOpenCalendar,
                    shape = BondsTheme.PillShape,
                    border = BorderStroke(1.dp, BondsTheme.BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BondsTheme.DeepNavy
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = BondsTheme.DeepNavy)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver calendario", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = BondsTheme.DeepNavy)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Estado Compartido por la otra persona ────────────────────────────
            Text(
                text = "Información compartida",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = BondsTheme.DeepNavy
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = BondsTheme.CardShapeLarge,
                color = BondsTheme.WarmWhite,
                border = BorderStroke(1.dp, BondsTheme.BorderSubtle),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Fase del ciclo
                    if (member.otherSharePrefs.phase && member.phase != null) {
                        DetailInfoRow(
                            label = "Fase del ciclo",
                            value = "${phaseLocalized(member.phase)} • Día ${member.cycleDay ?: 1}",
                            color = phaseColor(member.phase)
                        )
                    } else {
                        DetailInfoRow(
                            label = "Fase del ciclo",
                            value = stringResource(R.string.bonds_profile_no_phase_shared),
                            color = BondsTheme.TextTertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Estado anímico
                    if (member.otherSharePrefs.mood && member.mood != null) {
                        DetailInfoRow(
                            label = "Estado de ánimo",
                            value = member.mood,
                            color = BondsTheme.DeepNavy
                        )
                    } else {
                        DetailInfoRow(
                            label = "Estado de ánimo",
                            value = stringResource(R.string.bonds_profile_no_mood_shared),
                            color = BondsTheme.TextTertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Síntomas
                    if (member.otherSharePrefs.symptoms && member.symptoms.isNotEmpty()) {
                        DetailInfoRow(
                            label = "Síntomas hoy",
                            value = member.symptoms.joinToString(", "),
                            color = BondsTheme.DeepNavy
                        )
                    } else {
                        DetailInfoRow(
                            label = "Síntomas",
                            value = stringResource(R.string.bonds_profile_no_symptoms_shared),
                            color = BondsTheme.TextTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Mi Privacidad con esta persona específica ────────────────────────
            Text(
                text = "Lo que compartes con ${member.displayName}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = BondsTheme.DeepNavy
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = BondsTheme.CardShapeLarge,
                color = BondsTheme.WarmWhite,
                border = BorderStroke(1.dp, BondsTheme.BorderSubtle),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IndividualPrivacyToggle(
                        icon = Icons.Rounded.DonutLarge,
                        title = "Fase del ciclo",
                        checked = myPhase,
                        onChecked = {
                            myPhase = it
                            onUpdatePrivacy(SharePreferences(myPhase, myMood, mySymptoms, myPredictions))
                        }
                    )
                    IndividualPrivacyToggle(
                        icon = Icons.Rounded.Mood,
                        title = "Estado de ánimo",
                        checked = myMood,
                        onChecked = {
                            myMood = it
                            onUpdatePrivacy(SharePreferences(myPhase, myMood, mySymptoms, myPredictions))
                        }
                    )
                    IndividualPrivacyToggle(
                        icon = Icons.Rounded.FlashOn,
                        title = "Síntomas",
                        checked = mySymptoms,
                        onChecked = {
                            mySymptoms = it
                            onUpdatePrivacy(SharePreferences(myPhase, myMood, mySymptoms, myPredictions))
                        }
                    )
                    IndividualPrivacyToggle(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Predicciones",
                        checked = myPredictions,
                        onChecked = {
                            myPredictions = it
                            onUpdatePrivacy(SharePreferences(myPhase, myMood, mySymptoms, myPredictions))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Botón de Desvinculación ──────────────────────────────────────────
            TextButton(
                onClick = { showUnlinkDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.LinkOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.bonds_profile_unlink_action),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Diálogo de confirmación de desvinculación
    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            containerColor = BondsTheme.WarmWhite,
            title = {
                Text(
                    text = "¿Desvincular a ${member.displayName}?",
                    fontWeight = FontWeight.Bold,
                    color = BondsTheme.DeepNavy
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.bonds_profile_unlink_confirm),
                    fontSize = 14.sp,
                    color = BondsTheme.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnlinkDialog = false
                        onUnlink()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.bonds_confirm_unlink))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) {
                    Text(stringResource(R.string.bonds_cancel), color = BondsTheme.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = BondsTheme.TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun IndividualPrivacyToggle(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BondsTheme.DeepNavy,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BondsTheme.DeepNavy
            )
        }

        val isDark = LumiThemeTokens.current.isDark
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isDark) Color(0xFF0B0B11) else Color.White,
                checkedTrackColor = BondsTheme.DeepNavy,
                uncheckedThumbColor = if (isDark) BondsTheme.TextTertiary else Color.White,
                uncheckedTrackColor = BondsTheme.BorderSubtle
            )
        )
    }
}

@Composable
private fun phaseColor(phase: CyclePhase): Color = when (phase) {
    CyclePhase.MENSTRUAL -> BondsTheme.DotMenstrual
    CyclePhase.FOLLICULAR -> BondsTheme.DotFollicular
    CyclePhase.OVULATION -> BondsTheme.DotOvulation
    CyclePhase.LUTEAL -> BondsTheme.DotLuteal
    else -> BondsTheme.TextSecondary
}

private fun phaseLocalized(phase: CyclePhase): String = when (phase) {
    CyclePhase.MENSTRUAL -> "Menstruación"
    CyclePhase.FOLLICULAR -> "Fase folicular"
    CyclePhase.OVULATION -> "Ovulación"
    CyclePhase.LUTEAL -> "Fase lútea"
    CyclePhase.PREGNANCY -> "Embarazo"
    CyclePhase.UNKNOWN -> "Sin datos"
}
