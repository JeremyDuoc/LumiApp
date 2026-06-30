package com.jeremy.lumi.ui.screens.partner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DiaryEntry(
    val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val text: String = "",
    val phase: CyclePhase = CyclePhase.UNKNOWN,           // fase de quien escribe en ese momento
    val timestampMs: Long = 0L
)

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

private fun List<DiaryEntry>.groupedByDate(formatter: DateTimeFormatter): Map<String, List<DiaryEntry>> =
    groupBy { it.timestampMs.toLocalDateTime().toLocalDate().format(formatter) }

@Composable
fun SharedDiaryScreen(
    currentUid: String,
    myName: String,
    myPhase: CyclePhase,
    entries: List<DiaryEntry>,        // viene del ViewModel, ordenadas por timestamp asc
    isSending: Boolean = false,
    onSend: (text: String) -> Unit,
    onBack: () -> Unit
) {
    val phaseColors = LocalPhaseColors.current
    var draft by remember { mutableStateOf("") }
    val currentLocale = java.util.Locale.getDefault()
    val grouped = remember(entries, currentLocale) { 
        val formatter = DateTimeFormatter.ofPattern("d MMM", currentLocale)
        entries.groupedByDate(formatter) 
    }

    val myPhaseColor = when (myPhase) {
        CyclePhase.MENSTRUAL  -> phaseColors.menstrual
        CyclePhase.FOLLICULAR -> phaseColors.follicular
        CyclePhase.OVULATION  -> phaseColors.ovulation
        CyclePhase.LUTEAL     -> phaseColors.luteal
        else                  -> Color(0xFF7B2FBE)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0A1A))
    ) {
        // Header
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.dual_calendar_back), tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.shared_diary_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = stringResource(R.string.shared_diary_subtitle),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        // Lista de entradas
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            reverseLayout = false
        ) {
            grouped.forEach { (date, dayEntries) ->
                item(key = "header_$date") {
                    DateSeparator(label = date)
                }
                items(dayEntries, key = { it.id }) { entry ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut()
                    ) {
                        DiaryEntryBubble(
                            entry = entry,
                            isMe = entry.authorUid == currentUid,
                            phaseColors = phaseColors
                        )
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    EmptyDiaryState()
                }
            }
        }

        // Input
        DiaryInput(
            draft = draft,
            isSending = isSending,
            accentColor = myPhaseColor,
            onDraftChange = { draft = it },
            onSend = {
                if (draft.isNotBlank() && !isSending) {
                    onSend(draft.trim())
                    draft = ""
                }
            }
        )
    }
}

@Composable
private fun DateSeparator(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.07f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DiaryEntryBubble(
    entry: DiaryEntry,
    isMe: Boolean,
    phaseColors: com.jeremy.lumi.ui.theme.PhaseColors
) {
    val bubblePhaseColor = when (entry.phase) {
        CyclePhase.MENSTRUAL  -> phaseColors.menstrual
        CyclePhase.FOLLICULAR -> phaseColors.follicular
        CyclePhase.OVULATION  -> phaseColors.ovulation
        CyclePhase.LUTEAL     -> phaseColors.luteal
        else                  -> Color(0xFF7B2FBE)
    }
    val time = entry.timestampMs.toLocalDateTime().format(timeFormatter)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        // Avatar solo para mensajes de la otra persona
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bubblePhaseColor.copy(alpha = 0.2f))
                    .border(1.dp, bubblePhaseColor.copy(alpha = 0.5f), CircleShape)
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.authorName.firstOrNull()?.uppercase() ?: "?",
                    color = bubblePhaseColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isMe) 18.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isMe) bubblePhaseColor.copy(alpha = 0.22f)
                        else Color(0xFF1A1025)
                    )
                    .border(
                        1.dp,
                        bubblePhaseColor.copy(alpha = if (isMe) 0.45f else 0.15f),
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isMe) 18.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 18.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = entry.text,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = time,
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EmptyDiaryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📔", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.shared_diary_empty_title),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.shared_diary_empty_desc),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun DiaryInput(
    draft: String,
    isSending: Boolean,
    accentColor: Color,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF120D20))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1A1025))
                .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (draft.isEmpty()) {
                Text(
                    text = stringResource(R.string.shared_diary_input_hint),
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(accentColor),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (draft.isNotBlank()) accentColor
                    else Color.White.copy(alpha = 0.08f)
                )
                .clickable(enabled = draft.isNotBlank() && !isSending, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(R.string.shared_diary_send),
                    tint = if (draft.isNotBlank()) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
