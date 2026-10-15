package com.jeremy.lumi.ui.screens.bonds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import com.jeremy.lumi.ui.components.LumiBrandLockup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.screens.partner.AvatarCatalog

/**
 * Pantalla principal de BONDS — Diseñada con máxima fidelidad al Teléfono 1 de la referencia.
 *
 * Estética:
 * - Fondo blanco cálido (#FFFDFB / #FAF7F5)
 * - Tipografía Deep Navy (#193458)
 * - Carrusel de personas conectadas con aros degradados
 * - Tarjeta contextual "How she's feeling today" con aura pastel suave
 * - Carrusel de gestos afectivos ("Send a caring gesture")
 */
@Composable
fun BondsHomeScreen(
    uiState: BondsUiState,
    onSelectMember: (BondMember) -> Unit,
    onOpenMemberDetail: (BondMember) -> Unit,
    onOpenSendGesture: (BondMember?, CareAction) -> Unit,
    onOpenPairingPrivacy: () -> Unit,
    onOpenSharedCalendar: () -> Unit,
    onToggleDemoMode: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val selected = uiState.selectedMember

    Scaffold(
        containerColor = BondsTheme.WarmBackground,
        topBar = {
            BondsTopHeader(
                isDemoMode = uiState.isDemoMode,
                onToggleDemoMode = onToggleDemoMode,
                onOpenPairing = onOpenPairingPrivacy,
                onOpenCalendar = onOpenSharedCalendar,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Título y subtítulo principal ──────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = stringResource(R.string.bonds_title),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = BondsTheme.DeepNavy,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.bonds_subtitle),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = BondsTheme.TextSecondary
                )
            }

            // ── Banner interactivo de Modo Demostración ───────────────────────────
            AnimatedVisibility(
                visible = uiState.isDemoMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    BondsDemoModeBanner(
                        onExitDemo = onToggleDemoMode
                    )
                }
            }

            if (uiState.members.isEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                BondsEmptyStateView(
                    onPairClick = onOpenPairingPrivacy,
                    onToggleDemoMode = onToggleDemoMode
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))

                // ── 4.2 Carrusel horizontal de personas ──────────────────────────────
                BondsCircleCarousel(
                    members = uiState.members,
                    selectedMember = selected,
                    onSelectMember = onSelectMember,
                    onAddClick = onOpenPairingPrivacy
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── 4.3 Tarjeta contextual: "Cómo se siente hoy" ────────────────
                if (selected != null) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        BondsFeelingCard(
                            member = selected,
                            onClick = { onOpenMemberDetail(selected) }
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // ── SECCIÓN 1: Calendario compartido (independiente y separada) ───
                BondsSharedCalendarBanner(
                    onOpenCalendar = onOpenSharedCalendar
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── SECCIÓN 2: Gestos afectivos (independiente) ────────────────────
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.bonds_send_caring_gesture),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BondsTheme.DeepNavy
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (selected != null) stringResource(R.string.bonds_comfort_gesture_for_person, selected.displayName)
                                   else stringResource(R.string.bonds_comfort_gesture_for_circle),
                            fontSize = 12.5.sp,
                            color = BondsTheme.TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    BondsGesturesCarousel(
                        onGestureClick = { action ->
                            onOpenSendGesture(selected, action)
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Componentes del Home
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BondsTopHeader(
    isDemoMode: Boolean = false,
    onToggleDemoMode: () -> Unit = {},
    onOpenPairing: () -> Unit,
    onOpenCalendar: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo Lumi minimalista a la izquierda con botón atrás explícito
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back),
                    tint = BondsTheme.DeepNavy,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            LumiBrandLockup(
                flowerSize = 22,
                fontSize = 21.sp,
                starSize = 13.sp,
                letterSpacing = (-1.2).sp,
                textColor = BondsTheme.DeepNavy,
                animated = true
            )
        }

        // Iconos de acción a la derecha
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Acceso rápido a Modo Demostración
            IconButton(
                onClick = onToggleDemoMode,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = if (isDemoMode) stringResource(R.string.bonds_demo_mode_exit) else stringResource(R.string.bonds_demo_mode),
                    tint = if (isDemoMode) BondsTheme.SoftViolet else BondsTheme.DeepNavy.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            IconButton(
                onClick = onOpenCalendar,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = stringResource(R.string.bonds_shared_calendar_title),
                    tint = BondsTheme.DeepNavy,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            IconButton(
                onClick = onOpenPairing,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PersonOutline,
                    contentDescription = stringResource(R.string.bonds_pairing_privacy_title),
                    tint = BondsTheme.DeepNavy,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun BondsCircleCarousel(
    members: List<BondMember>,
    selectedMember: BondMember?,
    onSelectMember: (BondMember) -> Unit,
    onAddClick: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Personas vinculadas
        items(members, key = { it.linkId }) { member ->
            val isSelected = selectedMember?.linkId == member.linkId
            BondsAvatarItem(
                member = member,
                isSelected = isSelected,
                onClick = { onSelectMember(member) }
            )
        }

        // Botón "+1" o añadir conexión al final
        item {
            BondsAddCircleItem(onClick = onAddClick)
        }
    }
}

@Composable
private fun BondsAvatarItem(
    member: BondMember,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val spec = AvatarCatalog.byId(member.avatarId)

    val defaultRingGradient = BondsTheme.AvatarRingGradient
    val peachColor = BondsTheme.Peach
    val blushColor = BondsTheme.Blush
    val lavenderColor = BondsTheme.Lavender
    // Gradiente exclusivo por persona para coincidir con la riqueza visual de la referencia
    val ringGradient = remember(member.displayName, isSelected, defaultRingGradient, peachColor, blushColor, lavenderColor) {
        when (member.displayName.lowercase()) {
            "camila" -> Brush.sweepGradient(listOf(Color(0xFFFF9E7D), Color(0xFFF37CA5), Color(0xFFC8B5F1), Color(0xFFFF9E7D)))
            "sofia" -> Brush.sweepGradient(listOf(Color(0xFFFFB75E), Color(0xFFED8F03), Color(0xFFFFE259), Color(0xFFFFB75E)))
            "valentina" -> Brush.sweepGradient(listOf(Color(0xFF48C6EF), Color(0xFF2F80ED), Color(0xFF89F7FE), Color(0xFF48C6EF)))
            "anto" -> Brush.sweepGradient(listOf(Color(0xFFFF8FA3), Color(0xFFE84393), Color(0xFFF857A6), Color(0xFFFF8FA3)))
            else -> if (isSelected) defaultRingGradient else Brush.sweepGradient(
                listOf(peachColor, blushColor, lavenderColor, peachColor)
            )
        }
    }

    val avatarInnerGradient = remember(member.displayName) {
        when (member.displayName.lowercase()) {
            "camila" -> listOf(Color(0xFFFF8A65), Color(0xFFBA68C8))
            "sofia" -> listOf(Color(0xFFFFB74D), Color(0xFFFF8A65))
            "valentina" -> listOf(Color(0xFF4DD0E1), Color(0xFF4FC3F7))
            "anto" -> listOf(Color(0xFFF06292), Color(0xFFBA68C8))
            else -> spec?.gradient ?: AvatarCatalog.fallbackGradient(member.displayName)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(BondsTheme.CardShapeMedium)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        // Anillo degradado alrededor del avatar
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(ringGradient)
                .padding(if (isSelected) 3.5.dp else 2.5.dp),
            contentAlignment = Alignment.Center
        ) {
            // Separador fino armónico con el tema
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(BondsTheme.WarmWhite)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                // Círculo coloreado interior
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Brush.linearGradient(avatarInnerGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    if (spec != null) {
                        Icon(
                            imageVector = spec.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        Text(
                            text = member.displayName.trim().take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Nombre
        Text(
            text = member.displayName,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) BondsTheme.DeepNavy else BondsTheme.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Día del ciclo y dot solo si está autorizado
        if (member.cycleDay != null && member.otherSharePrefs.phase) {
            val dotColor = when (member.displayName.lowercase()) {
                "camila" -> Color(0xFF9070D8) // Violeta (Day 12 en referencia)
                "sofia" -> Color(0xFFFF9800)  // Naranja (Day 18 en referencia)
                "valentina" -> Color(0xFF00BCD4) // Cyan (Day 6 en referencia)
                "anto" -> Color(0xFFE91E63)   // Rosa (Day 22 en referencia)
                else -> phaseDotColor(member.phase)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.bonds_day_format, member.cycleDay),
                    fontSize = 11.5.sp,
                    color = BondsTheme.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Text(
                text = "Conectada",
                fontSize = 11.sp,
                color = BondsTheme.TextTertiary,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun BondsAddCircleItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(BondsTheme.CardShapeMedium)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(BondsTheme.PureWhite)
                .border(1.5.dp, BondsTheme.BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.bonds_add_connection),
                tint = BondsTheme.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = stringResource(R.string.bonds_add_connection),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = BondsTheme.TextPrimary
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = stringResource(R.string.bonds_link),
            fontSize = 11.sp,
            color = BondsTheme.TextTertiary
        )
    }
}

/**
 * Tarjeta contextual "How she's feeling today" (Teléfono 1)
 */
@Composable
private fun BondsFeelingCard(
    member: BondMember,
    onClick: () -> Unit
) {
    Card(
        shape = BondsTheme.CardShapeLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = BondsTheme.CardShapeLarge,
                spotColor = BondsTheme.Peach.copy(alpha = 0.4f),
                ambientColor = BondsTheme.Blush.copy(alpha = 0.2f)
            )
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BondsTheme.FeelingCardGradient)
                .padding(22.dp)
        ) {
            Column {
                // Cabecera de la tarjeta: Icono sol + Título + Flecha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BondsTheme.WarmWhite.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WbSunny,
                                contentDescription = null,
                                tint = BondsTheme.DeepNavy,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.bonds_how_person_feeling_today, member.displayName),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BondsTheme.DeepNavy
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = BondsTheme.DeepNavy.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Estado de ánimo compartido o mensaje amigable
                val moodText = member.mood ?: stringResource(R.string.bonds_feeling_sample_status)
                Text(
                    text = moodText,
                    fontSize = 15.sp,
                    color = BondsTheme.DeepNavy.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Subtarjeta translúcida con sugerencia de apoyo (Phone 1)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = BondsTheme.WarmWhite.copy(alpha = 0.82f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(BondsTheme.Lavender.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = BondsTheme.SoftViolet,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.bonds_feeling_default_quote),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = BondsTheme.DeepNavy.copy(alpha = 0.85f),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BondsEmptyStateView(
    onPairClick: () -> Unit,
    onToggleDemoMode: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = BondsTheme.CardShapeLarge,
            colors = CardDefaults.cardColors(containerColor = BondsTheme.WarmWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BondsTheme.BorderSubtle, BondsTheme.CardShapeLarge)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    BondsTheme.Peach.copy(alpha = 0.6f),
                                    BondsTheme.Blush.copy(alpha = 0.6f),
                                    BondsTheme.Lavender.copy(alpha = 0.6f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.bonds_empty_state_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BondsTheme.DeepNavy,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.bonds_empty_state_desc),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = BondsTheme.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onPairClick,
                    shape = BondsTheme.PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BondsTheme.SoftViolet,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.bonds_link_code_qr),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onToggleDemoMode,
                    shape = BondsTheme.PillShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BondsTheme.SoftViolet
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BondsTheme.SoftViolet.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = BondsTheme.SoftViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.bonds_explore_demo),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.bonds_privacy_always_control),
                    fontSize = 11.5.sp,
                    color = BondsTheme.TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BondsDemoModeBanner(onExitDemo: () -> Unit) {
    Card(
        shape = BondsTheme.CardShapeMedium,
        colors = CardDefaults.cardColors(containerColor = BondsTheme.SoftViolet.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, BondsTheme.SoftViolet.copy(alpha = 0.35f), BondsTheme.CardShapeMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = BondsTheme.SoftViolet,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.bonds_demo_active),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BondsTheme.DeepNavy
                    )
                    Text(
                        text = stringResource(R.string.bonds_demo_exploring_desc),
                        fontSize = 11.sp,
                        color = BondsTheme.TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onExitDemo,
                shape = BondsTheme.PillShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BondsTheme.SoftViolet
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, BondsTheme.SoftViolet.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "Salir",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BondsSharedCalendarBanner(onOpenCalendar: () -> Unit) {
    Card(
        shape = BondsTheme.CardShapeMedium,
        colors = CardDefaults.cardColors(containerColor = BondsTheme.WarmWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, BondsTheme.BorderSubtle, BondsTheme.CardShapeMedium)
            .clickable { onOpenCalendar() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BondsTheme.LavenderLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = BondsTheme.SoftViolet,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.bonds_shared_calendar_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BondsTheme.DeepNavy
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.bonds_shared_calendar_subtitle),
                        fontSize = 12.sp,
                        color = BondsTheme.TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(BondsTheme.PillShape)
                    .background(BondsTheme.LavenderLight)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.bonds_view_detail),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BondsTheme.SoftViolet
                )
            }
        }
    }
}

/**
 * Carrusel de gestos afectivos ("Send a caring gesture") — Estética exacta de Phone 1
 */
@Composable
private fun BondsGesturesCarousel(
    onGestureClick: (CareAction) -> Unit
) {
    val gestures = listOf(
        GestureItem(CareAction.HUG, R.string.bonds_gesture_hug_title, R.string.bonds_gesture_hug_desc, BondsTheme.Peach, isHighlighted = true),
        GestureItem(CareAction.TEA, R.string.bonds_gesture_tea_title, R.string.bonds_gesture_tea_desc, BondsTheme.Lavender, isHighlighted = false),
        GestureItem(CareAction.COFFEE, R.string.bonds_gesture_coffee_title, R.string.bonds_gesture_coffee_desc, BondsTheme.PeachLight, isHighlighted = false),
        GestureItem(CareAction.CHOCOLATE, R.string.bonds_gesture_chocolate_title, R.string.bonds_gesture_chocolate_desc, BondsTheme.Blush, isHighlighted = false),
        GestureItem(CareAction.HEATING_PAD, R.string.bonds_gesture_heating_pad_title, R.string.bonds_gesture_heating_pad_desc, BondsTheme.LavenderLight, isHighlighted = false),
        GestureItem(CareAction.PHARMACY, R.string.bonds_gesture_pharmacy_title, R.string.bonds_gesture_pharmacy_desc, BondsTheme.BlushLight, isHighlighted = false)
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(gestures) { item ->
            GestureButtonView(
                item = item,
                onClick = { onGestureClick(item.action) }
            )
        }
    }
}

private data class GestureItem(
    val action: CareAction,
    val titleRes: Int,
    val descRes: Int,
    val accentColor: Color,
    val isHighlighted: Boolean = false
)

@Composable
private fun GestureButtonView(
    item: GestureItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(82.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Botón circular / squircle iluminado del gesto
        Box(
            modifier = Modifier
                .size(62.dp)
                .shadow(
                    elevation = if (item.isHighlighted) 6.dp else 2.dp,
                    shape = if (item.isHighlighted) RoundedCornerShape(22.dp) else CircleShape,
                    spotColor = if (item.isHighlighted) BondsTheme.Peach.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.05f),
                    ambientColor = if (item.isHighlighted) BondsTheme.Blush.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.03f)
                )
                .clip(if (item.isHighlighted) RoundedCornerShape(22.dp) else CircleShape)
                .background(
                    if (item.isHighlighted) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFFD4C5),
                                Color(0xFFFFBBA5)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                BondsTheme.PureWhite,
                                BondsTheme.WarmWhite
                            )
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (item.isHighlighted) BondsTheme.PeachLight.copy(alpha = 0.8f) else BondsTheme.BorderSubtle,
                    shape = if (item.isHighlighted) RoundedCornerShape(22.dp) else CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            BondsGestureIcon(
                action = item.action,
                tint = BondsTheme.DeepNavy,
                size = 28.dp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Título del gesto
        Text(
            text = stringResource(item.titleRes),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = BondsTheme.DeepNavy,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Subtítulo breve
        Text(
            text = stringResource(item.descRes),
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = BondsTheme.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 12.5.sp
        )
    }
}

@Composable
private fun phaseDotColor(phase: CyclePhase?): Color = when (phase) {
    CyclePhase.MENSTRUAL -> BondsTheme.DotMenstrual
    CyclePhase.FOLLICULAR -> BondsTheme.DotFollicular
    CyclePhase.OVULATION -> BondsTheme.DotOvulation
    CyclePhase.LUTEAL -> BondsTheme.DotLuteal
    else -> BondsTheme.TextTertiary
}
