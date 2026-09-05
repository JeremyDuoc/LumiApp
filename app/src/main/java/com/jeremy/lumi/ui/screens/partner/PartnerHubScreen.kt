package com.jeremy.lumi.ui.screens.partner

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.LinkType
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.model.otherAvatarId
import com.jeremy.lumi.domain.model.otherDisplayName
import com.jeremy.lumi.domain.model.otherSharePreferences
import com.jeremy.lumi.domain.model.otherSnapshot

/**
 * Hub del Modo Pareja: constelación de vínculos con fila de historias,
 * tarjetas por vínculo, invitación pendiente y envío de Detalles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerHubScreen(
    uiState: PartnerUiState,
    onOpenStory: (PartnerLink) -> Unit,
    onSendCareAction: (String, CareAction) -> Unit,
    onOpenAddPartner: () -> Unit,
    onOpenDiary: (PartnerLink) -> Unit,
    onOpenDualCalendar: (PartnerLink) -> Unit,
    onUnlink: (PartnerLink) -> Unit,
    onNavigateBack: () -> Unit
) {
    var careSheetLink by remember { mutableStateOf<PartnerLink?>(null) }
    var unlinkCandidate by remember { mutableStateOf<PartnerLink?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.pm_hub_title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.pm_hub_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.pm_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.activeLinks.isEmpty() && uiState.pendingCode == null) {
            EmptyHub(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onAdd = onOpenAddPartner
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 32.dp, top = 4.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Fila de historias ──
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(uiState.activeLinks) { link ->
                            StoryBubble(
                                link = link,
                                myUid = uiState.currentUid,
                                onClick = { onOpenStory(link) }
                            )
                        }
                        item { AddBubble(onClick = onOpenAddPartner) }
                    }
                }

                // ── Invitación pendiente ──
                uiState.pendingCode?.let { code ->
                    item { PendingInvitationCard(code) }
                }

                // ── Tarjetas de vínculo ──
                items(uiState.activeLinks, key = { it.linkId }) { link ->
                    LinkCard(
                        link = link,
                        myUid = uiState.currentUid,
                        onOpenStory = { onOpenStory(link) },
                        onOpenDiary = { onOpenDiary(link) },
                        onOpenDualCalendar = { onOpenDualCalendar(link) },
                        onOpenCareSheet = { careSheetLink = link },
                        onRequestUnlink = { unlinkCandidate = link }
                    )
                }
            }
        }
    }

    // ── Hoja de Detalles ──
    careSheetLink?.let { link ->
        CareActionsSheet(
            uiState = uiState,
            onSend = { action ->
                onSendCareAction(link.linkId, action)
                careSheetLink = null
            },
            onDismiss = { careSheetLink = null }
        )
    }

    // ── Confirmación de desvinculación ──
    unlinkCandidate?.let { link ->
        AlertDialog(
            onDismissRequest = { unlinkCandidate = null },
            icon = { Icon(Icons.Rounded.LinkOff, contentDescription = null) },
            title = { Text(stringResource(R.string.pm_hub_unlink_confirm_title)) },
            text = { Text(stringResource(R.string.pm_hub_unlink_confirm_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    onUnlink(link)
                    unlinkCandidate = null
                }) {
                    Text(
                        text = stringResource(R.string.pm_hub_unlink_confirm_yes),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { unlinkCandidate = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Burbujas de historias
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StoryBubble(link: PartnerLink, myUid: String?, onClick: () -> Unit) {
    val snapshot = link.otherSnapshot(myUid)
    val phase = snapshot?.currentPhase
    val ringColors = if (phase != null && link.otherSharePreferences(myUid).phase) {
        listOf(phaseColor(phase), phaseColor(phase).copy(alpha = 0.35f), phaseColor(phase))
    } else {
        listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD), Color(0xFF9E9E9E))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(2.5.dp, Brush.sweepGradient(ringColors), CircleShape)
                .padding(6.dp)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            PartnerAvatarView(
                avatarId = link.otherAvatarId(myUid),
                fallbackName = link.otherDisplayName(myUid),
                size = 58.dp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = link.otherDisplayName(myUid)
                ?: stringResource(R.string.pm_dual_partner_fallback),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun AddBubble(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    CircleShape
                )
                .padding(6.dp)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(58.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.pm_hub_add),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.pm_hub_add),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tarjeta de vínculo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LinkCard(
    link: PartnerLink,
    myUid: String?,
    onOpenStory: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenDualCalendar: () -> Unit,
    onOpenCareSheet: () -> Unit,
    onRequestUnlink: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val snapshot = link.otherSnapshot(myUid)
    val sharesPhase = link.otherSharePreferences(myUid).phase

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PartnerAvatarView(
                    avatarId = link.otherAvatarId(myUid),
                    fallbackName = link.otherDisplayName(myUid),
                    size = 46.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.otherDisplayName(myUid)
                            ?: stringResource(R.string.pm_dual_partner_fallback),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = stringResource(
                                    if (link.linkType == LinkType.CYCLE_SYNC)
                                        R.string.pm_link_type_sync
                                    else
                                        R.string.pm_link_type_observer
                                ),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (sharesPhase && snapshot != null) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(phaseColor(snapshot.currentPhase), CircleShape)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = stringResource(phaseLabelRes(snapshot.currentPhase)),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pm_hub_unlink)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.LinkOff, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                onRequestUnlink()
                            }
                        )
                    }
                }
            }

            snapshot?.let {
                if (it.lastUpdatedAt > 0L) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.pm_story_updated,
                            DateUtils.getRelativeTimeSpanString(
                                it.lastUpdatedAt,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS
                            ).toString()
                        ),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HubAction(
                    icon = Icons.Rounded.PlayArrow,
                    label = stringResource(R.string.pm_hub_view_story),
                    onClick = onOpenStory
                )
                HubAction(
                    icon = Icons.Rounded.EditNote,
                    label = stringResource(R.string.pm_diary_title_short),
                    onClick = onOpenDiary
                )
                if (link.linkType == LinkType.CYCLE_SYNC) {
                    HubAction(
                        icon = Icons.Rounded.CalendarMonth,
                        label = stringResource(R.string.pm_hub_dual_calendar),
                        onClick = onOpenDualCalendar
                    )
                }
                HubAction(
                    icon = Icons.Rounded.VolunteerActivism,
                    label = stringResource(R.string.pm_hub_send_care_short),
                    onClick = onOpenCareSheet
                )
            }
        }
    }
}

@Composable
private fun HubAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Invitación pendiente / estado vacío
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PendingInvitationCard(code: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.HourglassEmpty,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.pm_hub_pending_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.pm_hub_pending_desc, code),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun EmptyHub(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(22.dp)
                    .size(40.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.pm_hub_empty_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.pm_hub_empty_desc),
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onAdd,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.pm_hub_add), fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hoja de Detalles (Care Actions)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareActionsSheet(
    uiState: PartnerUiState,
    onSend: (CareAction) -> Unit,
    onDismiss: () -> Unit
) {
    val onCooldown = uiState.isCareActionOnCooldown()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.pm_care_sheet_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.pm_care_sheet_subtitle),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                visibleCareActions.forEach { action ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(62.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = careActionTint(action).copy(alpha = if (onCooldown) 0.08f else 0.15f),
                            modifier = Modifier.clickable(enabled = !onCooldown) { onSend(action) }
                        ) {
                            Icon(
                                imageVector = careActionIcon(action),
                                contentDescription = stringResource(careActionLabelRes(action)),
                                tint = careActionTint(action).copy(alpha = if (onCooldown) 0.4f else 1f),
                                modifier = Modifier
                                    .padding(14.dp)
                                    .size(26.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(careActionLabelRes(action)),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (onCooldown) {
                Spacer(Modifier.height(16.dp))
                val seconds = uiState.careActionCooldownSeconds()
                Text(
                    text = stringResource(
                        R.string.pm_care_cooldown,
                        String.format("%d:%02d", seconds / 60, seconds % 60)
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
