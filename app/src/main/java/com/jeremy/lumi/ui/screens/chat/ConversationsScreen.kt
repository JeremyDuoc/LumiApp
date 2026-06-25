package com.jeremy.lumi.ui.screens.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.theme.LocalBrandGradient
import com.jeremy.lumi.ui.theme.LocalBrandBackgroundGradient
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
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val saveReminders by viewModel.saveRemindersInChat.collectAsStateWithLifecycle()

    // Filtrar los mensajes
    val lumiMessages = messages.filter { it.messageType == ChatMessageType.GREETING }
    val reminderMessages = messages.filter { it.messageType != ChatMessageType.GREETING }

    // Obtenemos los últimos mensajes
    val lastLumiMsg = lumiMessages.maxByOrNull { it.timestamp }
    val lastReminderMsg = reminderMessages.maxByOrNull { it.timestamp }

    val brandBgGradient = LocalBrandBackgroundGradient.current
    val containerModifier = if (brandBgGradient != null) {
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).background(brandBgGradient)
    } else {
        Modifier.fillMaxSize()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.chat_messages_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (brandBgGradient != null) Color.Transparent else MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = if (brandBgGradient != null) Color.Transparent else MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = containerModifier
                .padding(padding)
                .padding(top = 8.dp)
        ) {
            // â”€â”€ Disclaimer m\u00e9dico \u2014 siempre visible, discreta y clara â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
        LumiConversationItem(
            lastMessage = lastLumiMsg?.text ?: "Toca aquí para ver tus resúmenes diarios.",
            timeText = if (lastLumiMsg != null) "Hoy" else "",
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

// Ítem especial para Lumi con logo PNG real
@Composable
fun LumiConversationItem(
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
        // Avatar circular con la foto real
        Image(
            painter = painterResource(id = R.drawable.lumi_logo),
            contentDescription = "Avatar de Lumi",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0xFFD8B4E2).copy(alpha = 0.5f), CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val brandGradient = LocalBrandGradient.current
                if (brandGradient != null) {
                    Text(
                        text = "Lumi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            brush = brandGradient,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                } else {
                    Text(
                        text = "Lumi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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

