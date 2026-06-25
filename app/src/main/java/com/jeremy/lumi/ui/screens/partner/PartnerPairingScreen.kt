package com.jeremy.lumi.ui.screens.partner

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.LinkStatus
import com.jeremy.lumi.domain.model.LinkType
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jeremy.lumi.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerPairingScreen(
    initialCode: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: PartnerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showWizard by remember { mutableStateOf(false) }
    var selectedLink by remember { mutableStateOf<PartnerLink?>(null) }

    // FAB entrance animation
    var fabVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        fabVisible = true
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (selectedLink != null) {
        StoryDetailScreen(
            link = selectedLink!!,
            currentUid = uiState.currentUid ?: "",
            onClose = { selectedLink = null },
            onSendCareAction = { action ->
                viewModel.sendCareAction(selectedLink!!.linkId, action)
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Mis Vínculos", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()
            ) {
                FloatingActionButton(
                    onClick = { showWizard = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Rounded.Add, "Agregar vínculo")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CenteredLoadingState(label = "Cargando vínculos...")
            } else if (uiState.activeLinks.isEmpty()) {
                EmptyLinksState()
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    val activeLinks = uiState.activeLinks.filter { it.status == LinkStatus.ACTIVE }
                    if (activeLinks.isNotEmpty()) {
                        Text(
                            text = "Historias Íntimas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(activeLinks, key = { it.linkId }) { link ->
                                val isOwner = link.ownerUid == uiState.currentUid
                                val snapshot = if (isOwner) link.partnerSnapshot else link.ownerSnapshot
                                val phase = snapshot?.currentPhase ?: CyclePhase.UNKNOWN
                                val name = link.relationLabel.takeIf { it.isNotBlank() }
                                    ?: (if (isOwner) "Pareja" else link.ownerDisplayName ?: "Vínculo")
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { selectedLink = link }
                                ) {
                                    StoryAvatar(
                                        displayName = name,
                                        phase = phase,
                                        size = 72.dp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    val pendingLinks = uiState.activeLinks.filter { it.status == LinkStatus.PENDING }
                    if (pendingLinks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Pendientes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                        )
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(pendingLinks, key = { it.linkId }) { link ->
                                LinkCard(
                                    link = link,
                                    currentUid = uiState.currentUid ?: "",
                                    onClick = {}
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    LaunchedEffect(initialCode) {
        if (!initialCode.isNullOrEmpty()) {
            viewModel.joinLink(initialCode)
        }
    }

    if (showWizard) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showWizard = false
                viewModel.clearWizardState()
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            PartnerWizardScreen(
                viewModel = viewModel,
                initialCode = initialCode,
                onClose = {
                    showWizard = false
                    viewModel.clearWizardState()
                }
            )
        }
    }
}

@Composable
private fun LinkCard(link: PartnerLink, currentUid: String, onClick: () -> Unit) {
    val isOwner = link.ownerUid == currentUid
    val isPending = link.status == LinkStatus.PENDING
    val phaseColors = LocalPhaseColors.current
    val primary = MaterialTheme.colorScheme.primary

    // Color según la fase del ciclo que observamos
    val snapshot = if (isOwner) link.partnerSnapshot else link.ownerSnapshot
    val phase = snapshot?.currentPhase ?: CyclePhase.UNKNOWN
    val phaseColor = if (phase != CyclePhase.UNKNOWN) when (phase) {
        CyclePhase.MENSTRUAL  -> phaseColors.menstrual
        CyclePhase.FOLLICULAR -> phaseColors.follicular
        CyclePhase.OVULATION  -> phaseColors.ovulation
        CyclePhase.LUTEAL     -> phaseColors.luteal
        else -> primary
    } else primary

    // Animación pulsante del borde para PENDING
    val infiniteTransition = rememberInfiniteTransition(label = "pending_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "border_alpha"
    )

    val clipboardManager = LocalClipboardManager.current

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending)
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPending) 0.dp else 1.dp),
        border = if (isPending)
            androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)
            )
        else
            androidx.compose.foundation.BorderStroke(1.dp, phaseColor.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val name = link.relationLabel.takeIf { it.isNotBlank() } ?: (if (isOwner) "Pareja" else link.ownerDisplayName ?: "Vínculo")

                StoryAvatar(
                    displayName = name,
                    phase = phase,
                    size = 52.dp,
                    isPending = isPending
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isPending && isOwner -> "Código enviado"
                            isOwner -> "Tu vínculo te observa"
                            else -> if (link.ownerDisplayName != null) "Ciclo de ${link.ownerDisplayName}" else "Ciclo Compartido"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = when {
                            isPending -> "Esperando a que tu pareja se una..."
                            phase != CyclePhase.UNKNOWN -> phaseLabelShort(phase)
                            else -> "Conexión Activa"
                        },
                        fontSize = 13.sp,
                        color = when {
                            isPending -> MaterialTheme.colorScheme.primary
                            phase != CyclePhase.UNKNOWN -> phaseColor
                            else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        }
                    )
                }

                if (!isPending) {
                    Icon(
                        Icons.Rounded.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    )
                }
            }

            // Expandible: mostrar código si está PENDING y el usuario es owner
            if (isPending && isOwner && link.linkCode.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Código de invitación",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = link.linkCode,
                            fontSize = 28.sp,
                            letterSpacing = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(link.linkCode))
                        }
                    ) {
                        Icon(
                            Icons.Rounded.ContentCopy, "Copiar código",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OwnerConnectedScreen(
    link: PartnerLink,
    uiState: PartnerUiState,
    viewModel: PartnerViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi Vínculo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                }
                Text("Vínculo Activo", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Configura qué datos puede ver tu acompañante. Puedes cambiar esto en cualquier momento.",
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                // Tarjeta glassmorphism de privacidad
                GlassCard(phaseColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Privacidad",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        PrivacyToggleRow(icon = Icons.Rounded.Cyclone, label = "Fase actual del ciclo", checked = uiState.sharePhase, onCheckedChange = { viewModel.setSharePhase(it) })
                        PrivacyToggleRow(icon = Icons.Rounded.Timeline, label = "Próximas predicciones", checked = uiState.sharePredictions, onCheckedChange = { viewModel.setSharePredictions(it) })
                        PrivacyToggleRow(icon = Icons.Rounded.Mood, label = "Estado de ánimo", checked = uiState.shareMood, onCheckedChange = { viewModel.setShareMood(it) })
                        PrivacyToggleRow(icon = Icons.Rounded.HealthAndSafety, label = "Síntomas", checked = uiState.shareSymptoms, onCheckedChange = { viewModel.setShareSymptoms(it) }, isLast = true)
                    }
                }

                Spacer(Modifier.weight(1f))

                OutlinedButton(
                    onClick = { viewModel.unlink(link.linkId); onBack() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Rounded.LinkOff, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Desvincular", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PrivacyToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isLast: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
            )
        }
        if (!isLast) HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
    }
}

//  Empty & Loading States


@Composable
private fun EmptyLinksState() {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "empty_float"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_empty))
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(140.dp).graphicsLayer { translationY = offsetY }
            )
        } else {
            Text(
                "💫",
                fontSize = 64.sp,
                modifier = Modifier.graphicsLayer { translationY = offsetY }
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Aún no tienes vínculos",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Toca el botón + para compartir tu ciclo con alguien especial, o para acompañar a quien te importa.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun CenteredLoadingState(label: String?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "loading_alpha"
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_firefly))
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                com.jeremy.lumi.ui.components.LumiLoader(
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        if (label != null) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                textAlign = TextAlign.Center
            )
        }
    }
}

//  Helpers compartidos


internal fun phaseEmoji(phase: CyclePhase): String = when (phase) {
    CyclePhase.MENSTRUAL  -> "🌺"
    CyclePhase.FOLLICULAR -> "🌱"
    CyclePhase.OVULATION  -> "✨"
    CyclePhase.LUTEAL     -> "🌙"
    CyclePhase.PREGNANCY  -> "🤰"
    else                  -> "🌸"
}

private fun phaseLabelShort(phase: CyclePhase): String = when (phase) {
    CyclePhase.MENSTRUAL  -> "Fase menstrual"
    CyclePhase.FOLLICULAR -> "Fase folicular"
    CyclePhase.OVULATION  -> "Ovulación"
    CyclePhase.LUTEAL     -> "Fase lútea"
    CyclePhase.PREGNANCY  -> "Embarazo"
    else                  -> "Activo"
}
