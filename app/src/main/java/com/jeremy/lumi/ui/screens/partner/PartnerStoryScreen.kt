package com.jeremy.lumi.ui.screens.partner

import android.text.format.DateUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.CycleSnapshot
import com.jeremy.lumi.domain.model.PartnerLink
import com.jeremy.lumi.domain.model.otherAvatarId
import com.jeremy.lumi.domain.model.otherDisplayName
import com.jeremy.lumi.domain.model.otherSharePreferences
import com.jeremy.lumi.domain.model.otherSnapshot
import com.jeremy.lumi.domain.usecase.CyclePredictor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Páginas posibles de la historia, según la privacidad de la otra persona. */
private enum class StoryPage { PHASE, MOOD, SYMPTOMS, PREDICTION, NO_DATA, PRIVATE }

private const val PAGE_DURATION_MS = 6000

/**
 * Dashboard estilo "historias": pantalla inmersiva a color de fase, con
 * barras de progreso segmentadas, avance automático, navegación por toques
 * (mitad izquierda/derecha) y Detalles en la parte inferior.
 *
 * Solo muestra lo que la otra persona decidió compartir: cada página se
 * construye a partir de sus toggles de privacidad.
 */
@Composable
fun PartnerStoryScreen(
    link: PartnerLink,
    myUid: String,
    uiState: PartnerUiState,
    onSendCareAction: (CareAction) -> Unit,
    onClose: () -> Unit
) {
    val snapshot = link.otherSnapshot(myUid)
    val shares = link.otherSharePreferences(myUid)

    val pages = remember(link, myUid) {
        if (snapshot == null) {
            listOf(StoryPage.NO_DATA)
        } else {
            buildList {
                if (shares.phase) add(StoryPage.PHASE)
                if (shares.mood) add(StoryPage.MOOD)
                if (shares.symptoms) add(StoryPage.SYMPTOMS)
                if (shares.predictions) add(StoryPage.PREDICTION)
                if (isEmpty()) add(StoryPage.PRIVATE)
            }
        }
    }

    var currentPage by remember { mutableIntStateOf(0) }
    val progress = remember(currentPage) { Animatable(0f) }
    val reactionState = rememberCareReactionState()

    // Avance automático de la página actual
    LaunchedEffect(currentPage) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(PAGE_DURATION_MS, easing = LinearEasing))
        if (currentPage < pages.lastIndex) currentPage++
    }

    val phase = snapshot?.currentPhase?.takeIf { shares.phase }
    val gradient = phaseGradient(phase ?: com.jeremy.lumi.domain.model.CyclePhase.UNKNOWN)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradient))
    ) {
        // Zonas táctiles: mitad izquierda retrocede, mitad derecha avanza
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (currentPage > 0) currentPage-- }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (currentPage < pages.lastIndex) currentPage++ else onClose() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            // ── Barras de progreso segmentadas ──
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                pages.forEachIndexed { index, _ ->
                    val fill = when {
                        index < currentPage -> 1f
                        index == currentPage -> progress.value
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fill)
                                .background(Color.White, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Cabecera: avatar + nombre + cierre ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                PartnerAvatarView(
                    avatarId = link.otherAvatarId(myUid),
                    fallbackName = link.otherDisplayName(myUid),
                    size = 40.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.otherDisplayName(myUid)
                            ?: stringResource(R.string.pm_dual_partner_fallback),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    snapshot?.takeIf { it.lastUpdatedAt > 0 }?.let {
                        Text(
                            text = DateUtils.getRelativeTimeSpanString(
                                it.lastUpdatedAt,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS
                            ).toString(),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.pm_story_close),
                        tint = Color.White
                    )
                }
            }

            // ── Contenido de la página ──
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when (pages[currentPage]) {
                    StoryPage.PHASE -> PhasePage(snapshot!!)
                    StoryPage.MOOD -> MoodPage(snapshot!!)
                    StoryPage.SYMPTOMS -> SymptomsPage(snapshot!!)
                    StoryPage.PREDICTION -> PredictionPage(snapshot!!)
                    StoryPage.NO_DATA -> InfoPage(
                        title = stringResource(R.string.pm_story_no_data),
                        subtitle = stringResource(R.string.pm_story_no_data_desc)
                    )
                    StoryPage.PRIVATE -> InfoPage(
                        title = stringResource(R.string.pm_story_private),
                        subtitle = stringResource(R.string.pm_story_private_desc),
                        icon = true
                    )
                }
            }

            // ── Detalles (Care Actions) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val onCooldown = uiState.isCareActionOnCooldown()
                visibleCareActions.forEach { action ->
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = if (onCooldown) 0.08f else 0.18f),
                        modifier = Modifier.clickable(enabled = !onCooldown) {
                            onSendCareAction(action)
                            reactionState.fire(action)
                        }
                    ) {
                        Icon(
                            imageVector = careActionIcon(action),
                            contentDescription = stringResource(careActionLabelRes(action)),
                            tint = Color.White.copy(alpha = if (onCooldown) 0.4f else 1f),
                            modifier = Modifier
                                .padding(13.dp)
                                .size(24.dp)
                        )
                    }
                }
            }
        }

        // Animación del detalle enviado, por encima de todo
        CareReactionOverlay(state = reactionState)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Páginas
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhasePage(snapshot: CycleSnapshot) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StoryOverline(stringResource(R.string.pm_story_phase_title))
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(phaseLabelRes(snapshot.currentPhase)),
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 46.sp
        )

        // Día del ciclo si la proyección está disponible
        val dayOfCycle = snapshot.cycleStartDateMs
            ?.takeIf { it > 0 && snapshot.cycleLengthDays >= 15 }
            ?.let {
                val start = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                CyclePredictor.dayInCycleWrapped(start, snapshot.cycleLengthDays, com.jeremy.lumi.utils.LumiClock.today())
            }
        dayOfCycle?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.pm_story_day_label, it),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 15.sp
            )
        }

        if (snapshot.isLate && snapshot.delayDays > 0) {
            Spacer(Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Text(
                    text = stringResource(R.string.pm_story_late_banner, snapshot.delayDays),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun MoodPage(snapshot: CycleSnapshot) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StoryOverline(stringResource(R.string.pm_story_mood_title))
        Spacer(Modifier.height(10.dp))
        Text(
            text = snapshot.currentMood ?: stringResource(R.string.pm_story_mood_empty),
            color = Color.White,
            fontSize = if (snapshot.currentMood != null) 34.sp else 18.sp,
            fontWeight = if (snapshot.currentMood != null) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomsPage(snapshot: CycleSnapshot) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        StoryOverline(stringResource(R.string.pm_story_symptoms_title))
        Spacer(Modifier.height(16.dp))
        if (snapshot.topSymptoms.isEmpty()) {
            Text(
                text = stringResource(R.string.pm_story_symptoms_empty),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                snapshot.topSymptoms.forEach { symptom ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = symptom,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionPage(snapshot: CycleSnapshot) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StoryOverline(stringResource(R.string.pm_story_prediction_title))
        Spacer(Modifier.height(10.dp))
        Text(
            text = when {
                snapshot.isLate && snapshot.delayDays > 0 ->
                    stringResource(R.string.pm_story_late_banner, snapshot.delayDays)
                snapshot.daysUntilNextPhase <= 0 ->
                    stringResource(R.string.pm_story_prediction_today)
                else ->
                    stringResource(R.string.pm_story_prediction_days, snapshot.daysUntilNextPhase)
            },
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp
        )
    }
}

@Composable
private fun InfoPage(title: String, subtitle: String, icon: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        if (icon) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StoryOverline(text: String) {
    Text(
        text = text.uppercase(),
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 2.sp
    )
}
