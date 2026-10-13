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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.data.local.entity.ReminderEntity
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.usecase.CyclePrediction
import com.jeremy.lumi.ui.theme.LumiThemeTokens

/**
 * Modal BottomSheet de avisos y notificaciones de Lumi.
 *
 * Muestra el estado en tiempo real del ciclo:
 * - Alerta de retraso o periodo inminente
 * - Ventana fértil / ovulación activa
 * - Estado del registro diario de hoy (con acceso directo a registrar)
 * - Recordatorios activos programados por la usuaria
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumiNotificationsSheet(
    isLate: Boolean,
    delayDays: Int,
    prediction: CyclePrediction?,
    currentPhase: CyclePhase,
    hasEntryToday: Boolean,
    activeReminders: List<ReminderEntity> = emptyList(),
    onQuickLogClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = tokens.panel,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(tokens.dim.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Avisos y recordatorios",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Notificaciones activas de tu salud y ciclo",
                        fontSize = 11.5.sp,
                        color = tokens.muted
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tokens.pill)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar",
                        tint = tokens.text,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Alerta de retraso
            if (isLate && delayDays > 0) {
                NotificationAlertCard(
                    icon = Icons.Rounded.Warning,
                    iconColor = tokens.rose,
                    title = "Retraso de $delayDays ${if (delayDays == 1) "día" else "días"}",
                    description = "Tu periodo está demorado respecto a la estimación habitual. Si persiste, realiza un seguimiento de síntomas o consulta a tu especialista.",
                    backgroundColor = tokens.rose.copy(alpha = 0.12f),
                    borderColor = tokens.rose.copy(alpha = 0.35f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2. Alerta de periodo próximo (<= 3 días)
            val daysUntil = prediction?.daysUntilNextPeriod ?: -1
            if (!isLate && daysUntil in 0..3) {
                val periodNotice = when (daysUntil) {
                    0 -> "Tu periodo está estimado para hoy."
                    1 -> "Tu periodo está estimado para mañana."
                    else -> "Tu periodo está estimado en ~$daysUntil días."
                }
                NotificationAlertCard(
                    icon = Icons.Rounded.DateRange,
                    iconColor = tokens.rose,
                    title = "Periodo próximo",
                    description = "$periodNotice Recuerda tener a mano tus productos de cuidado habituales.",
                    backgroundColor = tokens.panel2,
                    borderColor = tokens.line
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 3. Ventana fértil / ovulación
            if (currentPhase == CyclePhase.OVULATION) {
                NotificationAlertCard(
                    icon = Icons.Rounded.CheckCircle,
                    iconColor = tokens.mint,
                    title = "Ventana fértil y ovulación activa",
                    description = "Estás en los días de mayor probabilidad de concepción del ciclo.",
                    backgroundColor = tokens.panel2,
                    borderColor = tokens.line
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. Registro diario pendiente / completado
            if (!hasEntryToday) {
                NotificationAlertCard(
                    icon = Icons.Rounded.EditCalendar,
                    iconColor = tokens.accent,
                    title = "Registro de hoy pendiente",
                    description = "Tómate 30 segundos para registrar cómo te sientes y mantener la calibración de Lumi precisa.",
                    backgroundColor = tokens.panel2,
                    borderColor = tokens.accent.copy(alpha = 0.4f),
                    actionLabel = "Registrar ahora →",
                    onActionClick = {
                        onDismiss()
                        onQuickLogClick()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                NotificationAlertCard(
                    icon = Icons.Rounded.CheckCircle,
                    iconColor = tokens.accent,
                    title = "Registro de hoy al día",
                    description = "Tus síntomas y sensaciones de hoy ya están guardados en tu historial.",
                    backgroundColor = tokens.panel2,
                    borderColor = tokens.line
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 5. Recordatorios activos de la usuaria
            if (activeReminders.isNotEmpty()) {
                Text(
                    text = "Recordatorios programados",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text
                )
                Spacer(modifier = Modifier.height(8.dp))

                activeReminders.forEach { reminder ->
                    val timeFormatted = String.format(java.util.Locale.US, "%02d:%02d", reminder.hourOfDay, reminder.minute)
                    val labelSuffix = if (reminder.label?.isNotBlank() == true) " · ${reminder.label}" else ""
                    NotificationAlertCard(
                        icon = Icons.Rounded.Notifications,
                        iconColor = tokens.accent,
                        title = reminder.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        description = "Programado diariamente a las $timeFormatted$labelSuffix",
                        backgroundColor = tokens.panel2,
                        borderColor = tokens.line
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Entendido
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.accent)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Entendido",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tokens.isDark) tokens.bg else Color.White
                )
            }
        }
    }
}

@Composable
private fun NotificationAlertCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    backgroundColor: Color,
    borderColor: Color,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val tokens = LumiThemeTokens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = tokens.muted
                )
            }
        }

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tokens.accentSoft)
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.accent
                )
            }
        }
    }
}
