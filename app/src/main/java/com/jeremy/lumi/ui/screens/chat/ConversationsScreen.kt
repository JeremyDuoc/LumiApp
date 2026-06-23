package com.jeremy.lumi.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.jeremy.lumi.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeremy.lumi.data.local.entity.ChatMessageType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateToChat: (String) -> Unit // "lumi" o "reminders"
) {
    val messages by viewModel.messages.collectAsState()
    val saveReminders by viewModel.saveRemindersInChat.collectAsState()

    // Filtrar los mensajes
    val lumiMessages = messages.filter { it.messageType == ChatMessageType.GREETING }
    val reminderMessages = messages.filter { it.messageType != ChatMessageType.GREETING }

    // Obtenemos los últimos mensajes
    val lastLumiMsg = lumiMessages.maxByOrNull { it.timestamp }
    val lastReminderMsg = reminderMessages.maxByOrNull { it.timestamp }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.chat_messages_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = 8.dp)
        ) {
            // ── Disclaimer m\u00e9dico \u2014 siempre visible, discreta y clara ──────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text      = stringResource(R.string.chat_disclaimer),
                    fontSize  = 11.sp,
                    lineHeight = 15.sp,
                    color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            // Hilo 1: Lumi (Siempre visible)
            ConversationItem(
                title = "Lumi",
                icon = Icons.Rounded.Person,
                lastMessage = lastLumiMsg?.text ?: "Toca aquí para ver tus resúmenes diarios.",
                timeText = if (lastLumiMsg != null) "Hoy" else "", // Podríamos formatear el timestamp
                onClick = { onNavigateToChat("lumi") }
            )

            // Hilo 2: Recordatorios (Visible si está activado)
            if (saveReminders) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp)
                )

                ConversationItem(
                    title = "Recordatorios",
                    icon = Icons.Rounded.Notifications,
                    lastMessage = lastReminderMsg?.text ?: "No hay recordatorios recientes.",
                    timeText = "",
                    onClick = { onNavigateToChat("reminders") }
                )
            }
        }
    }
}

@Composable
fun ConversationItem(
    title: String,
    icon: ImageVector,
    lastMessage: String,
    timeText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Textos
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = timeText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = lastMessage,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
