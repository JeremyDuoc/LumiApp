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
import androidx.compose.ui.res.stringResource
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
        val link = selectedLink ?: return
        StoryDetailScreen(
            link = link,
            currentUid = uiState.currentUid ?: "",
            onClose = { selectedLink = null },
            onSendCareAction = { action ->
                viewModel.sendCareAction(link.linkId, action)
            },
            isOnCooldown = uiState.isCareActionOnCooldown()
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.partner_links_title),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Color.White)
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
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF7B2FBE), Color(0xFFE91E8C))
                            ),
                            shape = CircleShape
                        ),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.partner_add_link), tint = Color.White)
                }
            }
        },
        containerColor = Color(0xFF0E0A1A)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CenteredLoadingState(label = stringResource(R.string.partner_loading_links))
            } else if (uiState.activeLinks.isEmpty()) {
                EmptyLinksState(onAddLink = { showWizard = true })
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    val activeLinks = uiState.activeLinks.filter { it.status == LinkStatus.ACTIVE }
                    if (activeLinks.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.partner_intimate_stories),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
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
                                    ?: (if (isOwner) stringResource(R.string.partner_default_name) else link.ownerDisplayName ?: stringResource(R.string.partner_default_link_name))

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { selectedLink = link }
                                ) {
                                    StoryAvatar(
                                        displayName = name,
                                        phase = phase,
                                        size = 64.dp,
                                        animated = true
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = name,
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    val pendingLinks = uiState.activeLinks.filter { it.status == LinkStatus.PENDING }
                    if (pendingLinks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.partner_pending_links),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
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

    var initialCodeConsumed by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialCode) {
        if (!initialCode.isNullOrEmpty() && !initialCodeConsumed) {
            viewModel.joinLink(initialCode)
            initialCodeConsumed = true
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isPending) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else Color(0xFF1A1025))
            .border(
                if (isPending) 1.5.dp else 1.dp,
                if (isPending) MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha) else phaseColor.copy(alpha = 0.45f),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val name = link.relationLabel.takeIf { it.isNotBlank() } ?: (if (isOwner) stringResource(R.string.partner_default_name) else link.ownerDisplayName ?: stringResource(R.string.partner_default_link_name))

                StoryAvatar(
                    displayName = name,
                    phase = phase,
                    size = 52.dp,
                    isPending = isPending,
                    animated = true
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isPending && isOwner -> stringResource(R.string.partner_code_sent)
                            isOwner -> stringResource(R.string.partner_observing_you)
                            else -> if (link.ownerDisplayName != null) stringResource(R.string.partner_cycle_of, link.ownerDisplayName) else stringResource(R.string.partner_shared_cycle)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = when {
                            isPending -> stringResource(R.string.partner_waiting_join)
                            phase != CyclePhase.UNKNOWN -> phaseLabelShort(phase)
                            else -> stringResource(R.string.partner_active_connection)
                        },
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f)
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
                            stringResource(R.string.partner_invite_code),
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
                            Icons.Rounded.ContentCopy, stringResource(R.string.partner_copy_code),
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
                title = { Text(stringResource(R.string.partner_my_link), fontWeight = FontWeight.Bold) },
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
                Text(stringResource(R.string.partner_active_link), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.partner_privacy_desc),
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                // Tarjeta glassmorphism de privacidad
                GlassCard(phaseColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            stringResource(R.string.partner_privacy_label),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        PrivacyToggleRow(icon = Icons.Rounded.Cyclone, label = stringResource(R.string.partner_privacy_phase), checked = uiState.sharePhase, onCheckedChange = { viewModel.setSharePhase(it) })
                        PrivacyToggleRow(icon = Icons.Rounded.Timeline, label = stringResource(R.string.partner_privacy_predictions), checked = uiState.sharePredictions, onCheckedChange = { viewModel.setSharePredictions(it) })
                        PrivacyToggleRow(icon = Icons.Rounded.Mood, label = stringResource(R.string.partner_privacy_mood), checked = uiState.shareMood, onCheckedChange = { viewModel.setShareMood(it) })
                        PrivacyToggleRow(icon = Icons.Rounded.HealthAndSafety, label = stringResource(R.string.partner_privacy_symptoms), checked = uiState.shareSymptoms, onCheckedChange = { viewModel.setShareSymptoms(it) }, isLast = true)
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
                    Text(stringResource(R.string.partner_unlink), fontWeight = FontWeight.Bold)
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
private fun EmptyLinksState(onAddLink: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color(0xFF7B2FBE).copy(alpha = 0.15f))
                .border(1.dp, Color(0xFF7B2FBE).copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🌸", fontSize = 40.sp)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.partner_empty_state_title),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.partner_empty_state_desc),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onAddLink,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7B2FBE)
            ),
            shape = RoundedCornerShape(50)
        ) {
            Text(stringResource(R.string.partner_empty_state_btn), color = Color.White, fontWeight = FontWeight.SemiBold)
        }
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

@Composable
private fun phaseLabelShort(phase: CyclePhase): String = when (phase) {
    CyclePhase.MENSTRUAL  -> stringResource(R.string.phase_name_menstrual)
    CyclePhase.FOLLICULAR -> stringResource(R.string.phase_name_follicular)
    CyclePhase.OVULATION  -> stringResource(R.string.phase_name_ovulation)
    CyclePhase.LUTEAL     -> stringResource(R.string.phase_name_luteal)
    CyclePhase.PREGNANCY  -> stringResource(R.string.phase_name_pregnancy)
    else                  -> stringResource(R.string.phase_name_active)
}