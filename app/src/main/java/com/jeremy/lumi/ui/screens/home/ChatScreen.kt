package com.jeremy.lumi.ui.screens.chat

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.ChatMessageEntity
import com.jeremy.lumi.data.local.entity.ChatMessageType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.jeremy.lumi.ui.theme.LocalBrandGradient
import com.jeremy.lumi.ui.theme.LocalBrandBackgroundGradient
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
    
    var searchQuery by remember { mutableStateOf("") }

    val messages = remember(allMessages, thread, searchQuery) {
        val filteredByThread = if (thread == "lumi") {
            allMessages.filter { it.messageType == ChatMessageType.GREETING }
        } else {
            allMessages.filter { it.messageType != ChatMessageType.GREETING }
        }
        
        if (searchQuery.isBlank()) {
            filteredByThread
        } else {
            filteredByThread.filter { it.text.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    LaunchedEffect(Unit) { viewModel.markAllAsRead() }



    val brandBgGradient = LocalBrandBackgroundGradient.current
    val containerModifier = if (brandBgGradient != null) {
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).background(brandBgGradient)
    } else {
        Modifier.fillMaxSize()
    }

    Scaffold(
        topBar  = { LumiChatTopBar(thread = thread, onBack = onBack) },
        containerColor = if (brandBgGradient != null) Color.Transparent else MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = containerModifier.padding(paddingValues)) {
            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(50.dp),
                placeholder = { Text("Buscar mensajes...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Buscar") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            if (messages.isEmpty()) {
                if (searchQuery.isNotBlank()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron resultados", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                } else {
                    EmptyChatState(Modifier.weight(1f))
                }
            } else {
                val grouped = remember(messages) { groupByDay(messages) }

                LazyColumn(
                    modifier            = Modifier.weight(1f),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    reverseLayout       = true
                ) {
                    grouped.asReversed().forEach { (dayLabel, dayMessages) ->
                        items(dayMessages.asReversed(), key = { it.id }) { msg ->
                            AnimatedChatBubble(msg)
                        }
                        item(key = "label_$dayLabel") { AnimatedDayLabel(dayLabel) }
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

    val titleText = if (thread == "lumi") "Lumi" else "Recordatorios"
    val subtitleText = if (thread == "lumi") stringResource(R.string.chat_status_online) else "Historial"
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
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Atrás", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {

                if (thread == "lumi") {
                    // â”€â”€ Avatar con logo real de Lumi â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    Image(
                        painter = painterResource(id = R.drawable.lumi_logo),
                        contentDescription = "Avatar de Lumi",
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  CHAT BUBBLE ANIMADA â€” slide-up + fade-in por mensaje
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  PREMIUM CHAT BUBBLE â€” tarjeta elevada con gradiente + ícono + seen glow
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun PremiumChatBubble(message: ChatMessageEntity) {
    val primary = MaterialTheme.colorScheme.primary

    val isHighlighted = message.messageType in setOf(
        ChatMessageType.SUPPLY_REMINDER,
        ChatMessageType.METHOD_REMINDER,
        ChatMessageType.CUSTOM
    )

    // â”€â”€ Pulso del indicador "visto" â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
        horizontalArrangement = Arrangement.Start
    ) {
        // â”€â”€ Burbuja estilo iOS: fondo sólido, esquinas muy redondeadas â”€â”€â”€â”€â”€â”€â”€
        Surface(
            modifier  = Modifier.widthIn(max = 300.dp),
            shape     = RoundedCornerShape(
                topStart     = 4.dp,   // "cola" sutil en la esquina del emisor
                topEnd       = 20.dp,
                bottomStart  = 20.dp,
                bottomEnd    = 20.dp
            ),
            color     = MaterialTheme.colorScheme.surfaceVariant,
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
                    Text(
                        text       = message.text,
                        fontSize   = 15.sp,
                        lineHeight = 21.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(Modifier.height(6.dp))

                // â”€â”€ Timestamp + punto "visto" â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text     = formatTime(message.timestamp),
                        fontSize = 10.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
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

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun groupByDay(
    messages: List<ChatMessageEntity>
): List<Pair<String, List<ChatMessageEntity>>> {
    val today     = LocalDate.now()
    val yesterday = today.minusDays(1)
    return messages
        .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() }
        .toSortedMap()
        .map { (date, msgs) ->
            val label = when (date) {
                today     -> "Hoy"
                yesterday -> "Ayer"
                else      -> date.toString()
            }
            label to msgs.sortedBy { it.timestamp }
        }
}
