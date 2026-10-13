package com.jeremy.lumi.ui.screens.bonds

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.usecase.CyclePredictor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Calendario Compartido de BONDS — Admite múltiples conexiones en el círculo.
 *
 * Características:
 * - Vista mensual con navegación de meses
 * - Selector / filtro de personas en el círculo
 * - Indicadores discretos sin saturar las celdas
 * - Detalle contextual del día seleccionado al pulsar
 * - Distingue datos autorizados de proyecciones
 */
@Composable
fun BondsSharedCalendarScreen(
    members: List<BondMember>,
    onNavigateBack: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var filterMemberId by remember { mutableStateOf<String?>(null) } // null = Todos

    // Solo personas que hayan autorizado compartir fases o predicciones
    val authorizedMembers = remember(members) {
        members.filter { it.otherSharePrefs.phase || it.otherSharePrefs.predictions }
    }

    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    }

    Scaffold(
        containerColor = BondsTheme.WarmBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.pm_back),
                        tint = BondsTheme.DeepNavy
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = stringResource(R.string.bonds_shared_calendar_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BondsTheme.DeepNavy
                    )
                    Text(
                        text = stringResource(R.string.bonds_shared_calendar_subtitle),
                        fontSize = 12.sp,
                        color = BondsTheme.TextSecondary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // ── Filtro de personas (Pills) ──────────────────────────────────────
            if (authorizedMembers.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        CalendarFilterPill(
                            title = stringResource(R.string.bonds_calendar_filter_all),
                            isSelected = filterMemberId == null,
                            color = BondsTheme.DeepNavy,
                            onClick = { filterMemberId = null }
                        )
                    }

                    items(authorizedMembers) { member ->
                        val isChosen = filterMemberId == member.linkId
                        val memberColor = getMemberColor(member.displayName)
                        CalendarFilterPill(
                            title = member.displayName,
                            isSelected = isChosen,
                            color = memberColor,
                            onClick = { filterMemberId = member.linkId }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Cabecera del mes con flechas ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Mes anterior",
                        tint = BondsTheme.DeepNavy
                    )
                }

                Text(
                    text = currentMonth.format(monthFormatter).replaceFirstChar { it.uppercase() },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BondsTheme.DeepNavy
                )

                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Mes siguiente",
                        tint = BondsTheme.DeepNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Rejilla del calendario ──────────────────────────────────────────
            Surface(
                shape = BondsTheme.CardShapeLarge,
                color = BondsTheme.WarmWhite,
                border = BorderStroke(1.dp, BondsTheme.BorderSubtle),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Días de la semana (L M X J V S D)
                    val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysOfWeek.forEach { dayName ->
                            Text(
                                text = dayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BondsTheme.TextTertiary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Días del mes
                    val firstDayOfMonth = currentMonth.atDay(1)
                    val daysInMonth = currentMonth.lengthOfMonth()
                    val leadingEmptyDays = (firstDayOfMonth.dayOfWeek.value - 1) // 0 para lunes
                    val totalSlots = leadingEmptyDays + daysInMonth
                    val rowsCount = (totalSlots + 6) / 7

                    val activeFiltered = if (filterMemberId == null) {
                        authorizedMembers
                    } else {
                        authorizedMembers.filter { it.linkId == filterMemberId }
                    }

                    for (rowIndex in 0 until rowsCount) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (colIndex in 0 until 7) {
                                val slotIndex = rowIndex * 7 + colIndex
                                val dayNumber = slotIndex - leadingEmptyDays + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val date = currentMonth.atDay(dayNumber)
                                    val isSelected = date == selectedDate
                                    val isToday = date == LocalDate.now()

                                    // Calcular indicadores de miembros para este día
                                    val indicators = activeFiltered.mapNotNull { member ->
                                        getMemberPhaseOnDate(member, date)?.let { phase ->
                                            MemberIndicator(
                                                member = member,
                                                phase = phase,
                                                color = getMemberColor(member.displayName)
                                            )
                                        }
                                    }

                                    CalendarDayCell(
                                        date = date,
                                        dayNumber = dayNumber,
                                        isSelected = isSelected,
                                        isToday = isToday,
                                        indicators = indicators,
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clickable { selectedDate = date }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Leyenda de Fases ────────────────────────────────────────────────
            CalendarLegendSection(modifier = Modifier.padding(horizontal = 24.dp))

            Spacer(modifier = Modifier.height(20.dp))

            // ── Detalle del día seleccionado ────────────────────────────────────
            SelectedDayDetailCard(
                date = selectedDate,
                members = authorizedMembers,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Componentes auxiliares del calendario
// ─────────────────────────────────────────────────────────────────────────────

private data class MemberIndicator(
    val member: BondMember,
    val phase: CyclePhase,
    val color: Color
)

@Composable
private fun CalendarFilterPill(
    title: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = BondsTheme.PillShape,
        color = if (isSelected) color else BondsTheme.WarmWhite,
        border = if (isSelected) null else BorderStroke(1.dp, BondsTheme.BorderSubtle),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) (if (LumiThemeTokens.current.isDark) Color(0xFF0B0B11) else Color.White) else BondsTheme.DeepNavy
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    indicators: List<MemberIndicator>,
    modifier: Modifier = Modifier
) {
    val isDark = LumiThemeTokens.current.isDark
    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> BondsTheme.DeepNavy
                    isToday -> BondsTheme.PeachLight
                    else -> Color.Transparent
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayNumber.toString(),
            fontSize = 13.sp,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> if (isDark) Color(0xFF0B0B11) else Color.White
                isToday -> BondsTheme.DeepNavy
                else -> BondsTheme.TextPrimary
            }
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Puntos indicadores discretos de las personas
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            indicators.take(3).forEach { ind ->
                Box(
                    modifier = Modifier
                        .size(4.5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) (if (isDark) Color(0xFF0B0B11).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f)) else ind.color)
                )
            }
            if (indicators.size > 3) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) (if (isDark) Color(0xFF0B0B11) else Color.White) else BondsTheme.TextTertiary)
                )
            }
        }
    }
}

@Composable
private fun CalendarLegendSection(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(color = BondsTheme.DotMenstrual, label = "Menstruación")
        LegendDot(color = BondsTheme.DotFollicular, label = "Folicular")
        LegendDot(color = BondsTheme.DotOvulation, label = "Ovulación")
        LegendDot(color = BondsTheme.DotLuteal, label = "Lútea")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = BondsTheme.TextSecondary,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun SelectedDayDetailCard(
    date: LocalDate,
    members: List<BondMember>,
    modifier: Modifier = Modifier
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.getDefault()) }

    Card(
        shape = BondsTheme.CardShapeLarge,
        colors = CardDefaults.cardColors(containerColor = BondsTheme.WarmWhite),
        border = BorderStroke(1.dp, BondsTheme.BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = date.format(formatter).replaceFirstChar { it.uppercase() },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BondsTheme.DeepNavy
            )

            Spacer(modifier = Modifier.height(14.dp))

            val itemsForDate = members.mapNotNull { m ->
                getMemberPhaseOnDate(m, date)?.let { phase -> Pair(m, phase) }
            }

            if (itemsForDate.isEmpty()) {
                Text(
                    text = stringResource(R.string.bonds_calendar_no_data),
                    fontSize = 13.sp,
                    color = BondsTheme.TextTertiary
                )
            } else {
                itemsForDate.forEach { (member, phase) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(getMemberColor(member.displayName))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = member.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BondsTheme.DeepNavy,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = phaseLocalized(phase),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = phaseColor(phase)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Funciones de cálculo de proyecciones
// ─────────────────────────────────────────────────────────────────────────────

private fun getMemberPhaseOnDate(member: BondMember, date: LocalDate): CyclePhase? {
    val startMs = member.cycleStartDateMs ?: return null
    val start = java.time.Instant.ofEpochMilli(startMs).atZone(java.time.ZoneOffset.UTC).toLocalDate()
    val cycleLength = if (member.cycleLengthDays in 20..45) member.cycleLengthDays else 28
    val periodLength = if (member.periodLengthDays in 2..10) member.periodLengthDays else 5

    val dayInCycle = CyclePredictor.dayInCycleWrapped(start, cycleLength, date)
    return CyclePredictor.phaseForDay(dayInCycle, cycleLength, periodLength)
}

private fun getMemberColor(name: String): Color {
    val colors = listOf(
        Color(0xFFE87A90),
        Color(0xFF7A9FE8),
        Color(0xFF5ABEA0),
        Color(0xFFE5B54F),
        Color(0xFF8F80DE),
        Color(0xFFE8907A)
    )
    val hash = kotlin.math.abs(name.hashCode())
    return colors[hash % colors.size]
}

@Composable
private fun phaseColor(phase: CyclePhase): Color = when (phase) {
    CyclePhase.MENSTRUAL -> BondsTheme.DotMenstrual
    CyclePhase.FOLLICULAR -> BondsTheme.DotFollicular
    CyclePhase.OVULATION -> BondsTheme.DotOvulation
    CyclePhase.LUTEAL -> BondsTheme.DotLuteal
    else -> BondsTheme.TextSecondary
}

private fun phaseLocalized(phase: CyclePhase): String = when (phase) {
    CyclePhase.MENSTRUAL -> "Menstruación"
    CyclePhase.FOLLICULAR -> "Fase folicular"
    CyclePhase.OVULATION -> "Ovulación"
    CyclePhase.LUTEAL -> "Fase lútea"
    CyclePhase.PREGNANCY -> "Embarazo"
    CyclePhase.UNKNOWN -> "Sin datos"
}
