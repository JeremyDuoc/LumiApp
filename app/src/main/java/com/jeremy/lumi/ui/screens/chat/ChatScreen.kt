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
    val questions   by viewModel.availableQuestions.collectAsStateWithLifecycle()
    val isTyping    by viewModel.isTyping.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable(thread) { mutableStateOf("") }

    val messages = remember(allMessages, thread, searchQuery) {
        val filteredByThread = if (thread == "lumi") {
            allMessages.filter { 
                it.messageType == ChatMessageType.GREETING || 
                it.messageType == ChatMessageType.USER || 
                it.messageType == ChatMessageType.EDUCATIONAL || 
                it.messageType == ChatMessageType.INSIGHT 
            }
        } else {
            allMessages.filter { 
                it.messageType != ChatMessageType.GREETING && 
                it.messageType != ChatMessageType.USER && 
                it.messageType != ChatMessageType.EDUCATIONAL && 
                it.messageType != ChatMessageType.INSIGHT 
            }
        }
        
        if (searchQuery.isBlank()) {
            filteredByThread
        } else {
            filteredByThread.filter { it.text.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    LaunchedEffect(Unit) { viewModel.markAllAsRead() }



    // Chat usa siempre fondo sólido — máxima legibilidad para lectura de mensajes
    val containerModifier = Modifier.fillMaxSize()

    Scaffold(
        topBar  = { LumiChatTopBar(thread = thread, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = containerModifier
            .padding(paddingValues)
            .consumeWindowInsets(paddingValues)
            .imePadding() // Manejo del teclado
        ) {

            // Buscador de mensajes
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .heightIn(min = 56.dp),
                placeholder = { Text(stringResource(R.string.chat_search_placeholder), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.chat_search_cd), modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            if (messages.isEmpty()) {
                if (searchQuery.isNotBlank()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.chat_search_empty), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                } else {
                    EmptyChatState(Modifier.weight(1f))
                }
            } else {
                val todayLabel = stringResource(R.string.chat_day_today)
                val yesterdayLabel = stringResource(R.string.chat_day_yesterday)
                val grouped    = remember(messages, todayLabel, yesterdayLabel) { groupByDay(messages, todayLabel, yesterdayLabel) }
                val listState  = rememberLazyListState()

                // Auto-scroll al nuevo mensaje (index 0 porque reverseLayout = true)
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(0)
                    }
                }

                LazyColumn(
                    state               = listState,
                    modifier            = Modifier.weight(1f),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    reverseLayout       = true
                ) {
                    // El indicador de escribiendo de Lumi va "hasta arriba" en el código (que es abajo visualmente)
                    item {
                        AnimatedVisibility(
                            visible = isTyping,
                            enter   = slideInVertically { it / 2 } + fadeIn(),
                            exit    = fadeOut(tween(150))
                        ) {
                            TypingIndicator()
                        }
                    }

                    grouped.asReversed().forEach { (dayLabel, dayMessages) ->
                        items(dayMessages.asReversed(), key = { it.id }) { msg ->
                            AnimatedChatBubble(msg)
                        }
                        item(key = "label_$dayLabel") { AnimatedDayLabel(dayLabel) }
                    }
                }
            }

            // ── SECCIÓN INFERIOR: SELECTOR GUIADO DE PREGUNTAS Y CAMPO DE TEXTO (Thread Lumi) ──
            if (thread == "lumi") {
                var selectedCategory by rememberSaveable { mutableStateOf<QuestionCategory?>(null) }
                var userMessageText by rememberSaveable { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                        thickness = 0.5.dp
                    )

                    // 1. Selector de categorías temáticas
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text(stringResource(R.string.chat_filter_recommended), fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        items(QuestionCategory.entries.toTypedArray()) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text(stringResource(cat.labelRes), fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    // 2. Preguntas disponibles según la categoría seleccionada o contexto
                    val displayedQuestions = remember(questions, selectedCategory) {
                        if (selectedCategory == null) {
                            questions.take(6)
                        } else {
                            ChatQuestion.entries.filter { it.category == selectedCategory }
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 8.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedQuestions, key = { it.name }) { question ->
                            QuestionChip(
                                text = stringResource(question.displayTextRes),
                                enabled = !isTyping,
                                onClick = { viewModel.askQuestion(question) }
                            )
                        }
                    }

                    // 3. Campo de texto para escribir mensajes personalizados a Lumi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = userMessageText,
                            onValueChange = { userMessageText = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.chat_input_placeholder),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            maxLines = 3,
                            enabled = !isTyping
                        )

                        val canSend = userMessageText.isNotBlank() && !isTyping
                        IconButton(
                            onClick = {
                                val textToSend = userMessageText.trim()
                                if (textToSend.isNotEmpty()) {
                                    viewModel.sendUserMessage(textToSend)
                                    userMessageText = ""
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(R.string.chat_send_cd),
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  TOP BAR â€” Avatar circular de Lumi con flor de loto o icono de campana
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LumiChatTopBar(thread: String, onBack: (() -> Unit)?) {
    val primary   = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    val titleText = if (thread == "lumi") stringResource(R.string.chat_title_lumi) else stringResource(R.string.chat_title_reminders)
    val subtitleText = if (thread == "lumi") stringResource(R.string.chat_status_online) else stringResource(R.string.chat_subtitle_history)
    val iconVector = if (thread == "lumi") Icons.Rounded.AutoAwesome else Icons.Rounded.Notifications

    // Respiración suave del avatar
    val infiniteAnim = rememberInfiniteTransition(label = "avatar_breath")
    val avatarScale  by infiniteAnim.animateFloat(
        initialValue    = 0.97f,
        targetValue     = 1.03f,
        animationSpec   = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label           = "avatar_scale"
    )
    val glowAlpha by infiniteAnim.animateFloat(
        initialValue  = 0.20f,
        targetValue   = 0.55f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label         = "glow_alpha"
    )



    CenterAlignedTopAppBar(
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.chat_cd_back), tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {

                if (thread == "lumi") {
                    // ── Avatar con logo real de Lumi ────────────────────────
                    Image(
                        painter = painterResource(id = R.drawable.lumi_logo),
                        contentDescription = stringResource(R.string.chat_avatar_cd),
                        contentScale = ContentScale.Crop, // Esto lo corta en círculo puro
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFFD8B4E2).copy(alpha = 0.5f), CircleShape) // Borde suave
                    )
                } else {
                    // Avatar para Recordatorios
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconVector, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.width(10.dp))
                Column {
                    if (thread == "lumi") {
                        val brandGradient = LocalBrandGradient.current
                        if (brandGradient != null) {
                            Text(
                                text       = titleText,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp,
                                style      = TextStyle(
                                    brush = brandGradient,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 16.sp
                                )
                            )
                        } else {
                            Text(
                                text       = titleText,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text       = titleText,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text     = subtitleText,
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  DAY LABEL — Óvalo animado con slide-up + fade-in
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
    val alpha  = remember(message.id) { Animatable(0f) }
    val offset = remember(message.id) { Animatable(18f) }

    LaunchedEffect(message.id) {
        launch { alpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        launch { offset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
    }

    Box(
        Modifier
            .alpha(alpha.value)
            .graphicsLayer { translationY = offset.value.dp.toPx() }
    ) {
        PremiumChatBubble(message)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PREMIUM CHAT BUBBLE — tarjeta elevada con gradiente + ícono + seen glow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PremiumChatBubble(message: ChatMessageEntity) {
    val primary = MaterialTheme.colorScheme.primary
    val isUser = message.messageType == ChatMessageType.USER

    val isHighlighted = message.messageType in setOf(
        ChatMessageType.SUPPLY_REMINDER,
        ChatMessageType.METHOD_REMINDER,
        ChatMessageType.CUSTOM
    )

    // ── Pulso del indicador "visto" ──────────────────────────────────────
    val infiniteAnim  = rememberInfiniteTransition(label = "seen_pulse")
    val seenGlowAlpha by infiniteAnim.animateFloat(
        initialValue  = 0.25f,
        targetValue   = 0.85f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "seen_glow"
    )
    val seenScale by infiniteAnim.animateFloat(
        initialValue  = 0.85f,
        targetValue   = 1.15f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "seen_scale"
    )

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        // ── Burbuja estilo iOS: fondo sólido, esquinas muy redondeadas ───────
        Surface(
            modifier  = Modifier.widthIn(max = 300.dp),
            shape     = RoundedCornerShape(
                topStart     = if (isUser) 20.dp else 4.dp,
                topEnd       = if (isUser) 4.dp else 20.dp,
                bottomStart  = 20.dp,
                bottomEnd    = 20.dp
            ),
            color     = if (isUser) primary else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {

                // â”€â”€ Etiqueta de tipo resaltado â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                if (isHighlighted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(bottom = 5.dp)
                    ) {
                        Icon(
                            imageVector        = chatTypeIcon(message.messageType),
                            contentDescription = null,
                            modifier           = Modifier.size(13.dp),
                            tint               = primary
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text       = chatTypeLabelRes(message.messageType)
                                ?.let { stringResource(it) } ?: "",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = primary
                        )
                    }
                }

                // â”€â”€ Ícono de Lumi + texto del mensaje â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                Row(verticalAlignment = Alignment.Top) {
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(primary.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint               = primary,
                                modifier           = Modifier.size(14.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    FormattedChatMessage(
                        text       = message.text,
                        isUser     = isUser,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(Modifier.height(6.dp))

                // ──────────────── Timestamp + punto "visto" ────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text     = formatTime(message.timestamp),
                        fontSize = 10.sp,
                        color    = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f) 
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                    if (!isUser) {
                        Spacer(Modifier.width(5.dp))
                        Box(
                            Modifier
                                .size(6.dp)
                                .scale(seenScale)
                                .clip(CircleShape)
                                .background(primary.copy(alpha = seenGlowAlpha))
                        )
                    }
                }
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  EMPTY STATE
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
        val tokenRegex = Regex("""(\*\*(.+?)\*\*|\*(.+?)\*)""")
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
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = if (enabled) primary else Color.Gray
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
