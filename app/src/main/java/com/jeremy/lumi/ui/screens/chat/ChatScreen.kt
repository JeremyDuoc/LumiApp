package com.jeremy.lumi.ui.screens.chat

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.ChatMessageEntity
import com.jeremy.lumi.data.local.entity.ChatMessageType
import com.jeremy.lumi.domain.usecase.ChatQuestion
import com.jeremy.lumi.domain.usecase.QuestionCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.jeremy.lumi.ui.theme.LocalBrandGradient


import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private val MARKDOWN_TOKEN_REGEX = Regex("""(\*\*(.+?)\*\*|\*(.+?)\*)""")

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  CHAT SCREEN
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    thread: String = "lumi",
    onBack: (() -> Unit)? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val allMessages by viewModel.messages.collectAsStateWithLifecycle()
    val questions by viewModel.availableQuestions.collectAsStateWithLifecycle()
    val isTyping by viewModel.isTyping.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable(thread) { mutableStateOf("") }
    var isSearching by rememberSaveable(thread) { mutableStateOf(false) }
    var showTopics by rememberSaveable(thread) { mutableStateOf(false) }
    var selectedCategory by rememberSaveable(thread) { mutableStateOf<QuestionCategory?>(null) }
    var userMessageText by rememberSaveable(thread) { mutableStateOf("") }

    val messages = remember(allMessages, thread, searchQuery) {
        val inThread = allMessages.filter { message ->
            val isConversation = message.messageType in setOf(
                ChatMessageType.GREETING, ChatMessageType.USER,
                ChatMessageType.EDUCATIONAL, ChatMessageType.INSIGHT
            )
            if (thread == "lumi") isConversation else !isConversation
        }
        if (searchQuery.isBlank()) inThread else
            inThread.filter { it.text.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(Unit) { viewModel.markAllAsRead() }

    val todayLabel = stringResource(R.string.chat_day_today)
    val yesterdayLabel = stringResource(R.string.chat_day_yesterday)
    val grouped = remember(messages, todayLabel, yesterdayLabel) {
        groupByDay(messages, todayLabel, yesterdayLabel)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.firstOrNull()?.id, isTyping) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { LumiChatTopBar(thread, onBack) }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(scaffoldPadding)
                .consumeWindowInsets(scaffoldPadding)
                .imePadding()
        ) {
            // Search is an intentional action, not a permanently visible empty field.
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        stringResource(R.string.chat_new_privacy),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                IconButton(
                    onClick = {
                        isSearching = !isSearching
                        if (!isSearching) searchQuery = ""
                    }
                ) {
                    Icon(
                        if (isSearching) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.chat_new_history_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isSearching) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    placeholder = { Text(stringResource(R.string.chat_new_history_search)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
            }

            if (thread == "lumi" && !isSearching &&
                allMessages.none { it.messageType == ChatMessageType.USER } &&
                messages.size <= 2) {
                LumiChatIntroduction()
            }

            if (messages.isEmpty() && searchQuery.isNotBlank()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.chat_search_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (messages.isEmpty()) {
                EmptyChatState(Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp
                    )
                ) {
                    if (thread == "lumi") {
                        item(key = "typing") {
                            AnimatedVisibility(visible = isTyping) { TypingIndicator() }
                        }
                    }
                    grouped.asReversed().forEach { (label, dayMessages) ->
                        items(dayMessages.asReversed(), key = { it.id }) { message ->
                            AnimatedChatBubble(message)
                        }
                        item(key = "label_$label") { AnimatedDayLabel(label) }
                    }
                }
            }

            if (thread == "lumi") {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(top = 9.dp, bottom = 6.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .38f))

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { showTopics = !showTopics }
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.chat_new_suggestions),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            if (showTopics) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    if (showTopics) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text(stringResource(R.string.chat_filter_recommended)) }
                                )
                            }
                            items(QuestionCategory.entries.toTypedArray()) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = {
                                        selectedCategory = if (selectedCategory == category) null else category
                                    },
                                    label = { Text(stringResource(category.labelRes)) }
                                )
                            }
                        }
                    }

                    val suggestedQuestions = remember(questions, showTopics, selectedCategory) {
                        if (!showTopics || selectedCategory == null) questions.take(4)
                        else ChatQuestion.entries.filter { it.category == selectedCategory }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(suggestedQuestions, key = { it.name }) { question ->
                            QuestionChip(
                                text = stringResource(question.displayTextRes),
                                enabled = !isTyping,
                                onClick = { viewModel.askQuestion(question) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = userMessageText,
                            onValueChange = { userMessageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .20f)
                            ),
                            maxLines = 4,
                            enabled = !isTyping
                        )
                        val canSend = userMessageText.isNotBlank() && !isTyping
                        IconButton(
                            enabled = canSend,
                            onClick = {
                                val value = userMessageText.trim()
                                if (value.isNotBlank()) {
                                    viewModel.sendUserMessage(value)
                                    userMessageText = ""
                                }
                            },
                            modifier = Modifier.size(46.dp).clip(CircleShape)
                                .background(
                                    if (canSend) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(R.string.chat_send_cd),
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LumiChatIntroduction() {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .34f))
            .padding(18.dp)
    ) {
        com.jeremy.lumi.ui.components.LumiBrandFlower(
            size = 29, animated = true
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.chat_new_intro_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.chat_new_intro_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LumiChatTopBar(thread: String, onBack: (() -> Unit)?) {
    TopAppBar(
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.chat_cd_back)
                    )
                }
            }
        },
        title = {
            if (thread == "lumi") {
                Column {
                    com.jeremy.lumi.ui.components.LumiBrandLockup(
                        flowerSize = 23,
                        fontSize = 22.sp,
                        starSize = 10.sp,
                        animated = true
                    )
                    Text(
                        stringResource(R.string.chat_new_subtitle),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(stringResource(R.string.chat_title_reminders), fontWeight = FontWeight.SemiBold)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun AnimatedDayLabel(label: String) {
    val alpha  = remember { Animatable(0f)  }
    val offset = remember { Animatable(14f) }

    LaunchedEffect(label) {
        delay(80)
        launch { alpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
        launch { offset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .alpha(alpha.value)
            .graphicsLayer { translationY = offset.value.dp.toPx() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                        )
                    )
                )
                .border(
                    width = 0.5.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f)
                        )
                    ),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 18.dp, vertical = 5.dp)
        ) {
            Text(
                text       = label,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CHAT BUBBLE ANIMADA — slide-up + fade-in por mensaje
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedChatBubble(message: ChatMessageEntity) {
    val isRecent = remember(message.id) {
        System.currentTimeMillis() - message.timestamp < 3500
    }
    if (isRecent) {
        val alpha  = remember(message.id) { Animatable(0f) }
        val offset = remember(message.id) { Animatable(18f) }

        LaunchedEffect(message.id) {
            launch { alpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing)) }
            launch { offset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        }

        Box(
            Modifier.graphicsLayer {
                this.alpha = alpha.value
                translationY = offset.value.dp.toPx()
            }
        ) {
            PremiumChatBubble(message)
        }
    } else {
        PremiumChatBubble(message)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PREMIUM CHAT BUBBLE — tarjeta elevada con gradiente + ícono + seen glow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PremiumChatBubble(message: ChatMessageEntity) {
    val isUser = message.messageType == ChatMessageType.USER
    val isInsight = message.messageType == ChatMessageType.INSIGHT
    val isReminder = message.messageType in setOf(
        ChatMessageType.SUPPLY_REMINDER,
        ChatMessageType.METHOD_REMINDER,
        ChatMessageType.CUSTOM
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 335.dp),
            shape = RoundedCornerShape(
                topStart = if (isUser) 20.dp else 6.dp,
                topEnd = if (isUser) 6.dp else 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .66f),
            border = if (isUser) null else androidx.compose.foundation.BorderStroke(
                .75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .36f)
            ),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                if (isInsight || isReminder) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (isInsight) Icons.Rounded.Insights else chatTypeIcon(message.messageType),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = if (isInsight) stringResource(R.string.chat_new_discovery)
                                   else chatTypeLabelRes(message.messageType)?.let { stringResource(it) }.orEmpty(),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(verticalAlignment = Alignment.Top) {
                    if (!isUser) {
                        com.jeremy.lumi.ui.components.LumiBrandFlower(
                            size = 16, animated = false
                        )
                        Spacer(Modifier.width(11.dp))
                    }
                    FormattedChatMessage(
                        text = message.text,
                        isUser = isUser,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    text = formatTime(message.timestamp),
                    fontSize = 10.sp,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = .68f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .64f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ChatBubbleOutline, null,
                    modifier = Modifier.size(28.dp),
                    tint     = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.chat_empty_title), fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.chat_empty_desc), fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  HELPERS
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private fun chatTypeIcon(type: ChatMessageType): ImageVector = when (type) {
    ChatMessageType.PERIOD_SOON     -> Icons.Rounded.WaterDrop
    ChatMessageType.OVULATION_SOON  -> Icons.Rounded.Favorite
    ChatMessageType.SUPPLY_REMINDER -> Icons.Rounded.ShoppingBag
    ChatMessageType.METHOD_REMINDER -> Icons.Rounded.Medication
    ChatMessageType.LOG_DAILY       -> Icons.Rounded.EditNote
    ChatMessageType.CUSTOM          -> Icons.Rounded.NotificationsActive
    ChatMessageType.USER            -> Icons.Rounded.Person
    ChatMessageType.GREETING,
    ChatMessageType.EDUCATIONAL,
    ChatMessageType.INSIGHT         -> Icons.Rounded.AutoAwesome
}

private fun chatTypeLabelRes(type: ChatMessageType): Int? = when (type) {
    ChatMessageType.SUPPLY_REMINDER -> R.string.chat_label_supply
    ChatMessageType.METHOD_REMINDER -> R.string.chat_label_method
    ChatMessageType.CUSTOM          -> R.string.chat_label_custom
    else                            -> null
}

private val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

private fun formatTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(timeFormatter)

private fun groupByDay(
    messages: List<ChatMessageEntity>,
    todayLabel: String,
    yesterdayLabel: String
): List<Pair<String, List<ChatMessageEntity>>> {
    val today     = LocalDate.now()
    val yesterday = today.minusDays(1)
    return messages
        .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() }
        .toSortedMap()
        .map { (date, msgs) ->
            val label = when (date) {
                today     -> todayLabel
                yesterday -> yesterdayLabel
                else      -> date.toString()
            }
            label to msgs.sortedBy { it.timestamp }
        }
}

@Composable
private fun FormattedChatMessage(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    val defaultColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val boldColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    val annotated = remember(text, defaultColor, boldColor) {
        parseMarkdownToAnnotatedString(text, defaultColor, boldColor)
    }

    Text(
        text       = annotated,
        fontSize   = 14.5.sp,
        lineHeight = 21.sp,
        modifier   = modifier
    )
}

private fun parseMarkdownToAnnotatedString(
    rawText: String,
    defaultColor: Color,
    boldColor: Color
): androidx.compose.ui.text.AnnotatedString {
    val lines = rawText.lines().map { line ->
        val trimmed = line.trimStart()
        if (trimmed.startsWith("- ")) {
            val indent = line.takeWhile { it == ' ' }
            "$indent• " + trimmed.removePrefix("- ")
        } else {
            line
        }
    }
    val processedText = lines.joinToString("\n")

    return buildAnnotatedString {
        val tokenRegex = MARKDOWN_TOKEN_REGEX
        var lastIndex = 0

        for (match in tokenRegex.findAll(processedText)) {
            val range = match.range
            if (range.first > lastIndex) {
                append(processedText.substring(lastIndex, range.first))
            }

            val fullMatch = match.value
            if (fullMatch.startsWith("**") && fullMatch.endsWith("**") && fullMatch.length >= 4) {
                val boldContent = fullMatch.substring(2, fullMatch.length - 2)
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = boldColor)) {
                    append(boldContent)
                }
            } else if (fullMatch.startsWith("*") && fullMatch.endsWith("*") && fullMatch.length >= 2) {
                val italicContent = fullMatch.substring(1, fullMatch.length - 1)
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(italicContent)
                }
            } else {
                append(fullMatch)
            }
            lastIndex = range.last + 1
        }

        if (lastIndex < processedText.length) {
            append(processedText.substring(lastIndex))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  QUESTION CHIP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuestionChip(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "chip_scale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(primary.copy(alpha = 0.08f), secondary.copy(alpha = 0.06f)))
                else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.05f), Color.Gray.copy(alpha = 0.05f)))
            )
            .border(
                1.dp,
                if (enabled) Brush.horizontalGradient(listOf(primary.copy(alpha = 0.30f), secondary.copy(alpha = 0.20f)))
                else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.15f), Color.Gray.copy(alpha = 0.15f))),
                RoundedCornerShape(50.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            com.jeremy.lumi.ui.components.LumiBrandFlower(
                size = 12,
                animated = false
            )
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TYPING INDICATOR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val primary = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    val offsets = List(3) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -6f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 150)
            ),
            label = "typing_dot_$index"
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 100.dp),
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                offsets.forEach { offset ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer { translationY = offset.value.dp.toPx() }
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}
