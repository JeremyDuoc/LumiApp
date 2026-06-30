package com.jeremy.lumi.ui.screens.partner

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// Representa la fase de ambas personas en un día concreto
data class DualDayInfo(
    val date: LocalDate,
    val myPhase: CyclePhase,
    val partnerPhase: CyclePhase
)

// Semanas donde ambas tienen alta energía simultánea
fun List<DualDayInfo>.highEnergyWeeks(): Set<LocalDate> =
    filter {
        it.myPhase in setOf(CyclePhase.FOLLICULAR, CyclePhase.OVULATION) &&
        it.partnerPhase in setOf(CyclePhase.FOLLICULAR, CyclePhase.OVULATION)
    }.map { it.date }.toSet()

private fun CyclePhase.toColor(phaseColors: com.jeremy.lumi.ui.theme.PhaseColors): Color = when (this) {
    CyclePhase.MENSTRUAL  -> phaseColors.menstrual
    CyclePhase.FOLLICULAR -> phaseColors.follicular
    CyclePhase.OVULATION  -> phaseColors.ovulation
    CyclePhase.LUTEAL     -> phaseColors.luteal
    else                  -> Color(0xFF1A1025)
}

@Composable
fun DualCalendarScreen(
    myName: String,
    partnerName: String,
    days: List<DualDayInfo>,          // viene del ViewModel
    onBack: () -> Unit
) {
    val phaseColors = LocalPhaseColors.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val highEnergy = remember(days) { days.highEnergyWeeks() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0A1A))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.dual_calendar_back), tint = Color.White)
            }
            Text(
                text = stringResource(R.string.dual_calendar_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.3.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Leyenda de personas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendChip(name = myName, isLeft = true)
            LegendChip(name = partnerName, isLeft = false)
            Spacer(modifier = Modifier.weight(1f))
            // Chip de alta energía compartida
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF7B2FBE).copy(alpha = 0.25f))
                    .border(1.dp, Color(0xFF7B2FBE).copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.dual_calendar_energy_chip), color = Color(0xFFCE93D8), fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Navegación de mes
        MonthNavigator(
            currentMonth = currentMonth,
            onPrev = { currentMonth = currentMonth.minusMonths(1) },
            onNext = { currentMonth = currentMonth.plusMonths(1) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Días de la semana
        val daysOfWeek = listOf(
            stringResource(R.string.cal_mon),
            stringResource(R.string.cal_tue),
            stringResource(R.string.cal_wed),
            stringResource(R.string.cal_thu),
            stringResource(R.string.cal_fri),
            stringResource(R.string.cal_sat),
            stringResource(R.string.cal_sun)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid del calendario
        DualCalendarGrid(
            month = currentMonth,
            days = days,
            highEnergyDays = highEnergy,
            phaseColors = phaseColors
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Banner semanas de alta energía
        if (highEnergy.isNotEmpty()) {
            HighEnergyBanner()
        }
    }
}

@Composable
private fun LegendChip(name: String, isLeft: Boolean) {
    val color = if (isLeft) Color(0xFF7B2FBE) else Color(0xFFE91E8C)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = name, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
    }
}

@Composable
private fun MonthNavigator(
    currentMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
        }
        Text(
            text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale("es"))
                .replaceFirstChar { it.uppercase() } + " ${currentMonth.year}",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun DualCalendarGrid(
    month: YearMonth,
    days: List<DualDayInfo>,
    highEnergyDays: Set<LocalDate>,
    phaseColors: com.jeremy.lumi.ui.theme.PhaseColors
) {
    val daysMap = remember(days) { days.associateBy { it.date } }
    val firstDay = month.atDay(1)
    // Ajuste lunes=0
    val startOffset = (firstDay.dayOfWeek.value - 1) % 7
    val totalDays = month.lengthOfMonth()
    val cells = startOffset + totalDays
    val rows = (cells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val index = row * 7 + col
                    val dayNumber = index - startOffset + 1
                    if (dayNumber < 1 || dayNumber > totalDays) {
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        val date = month.atDay(dayNumber)
                        val info = daysMap[date]
                        val isHighEnergy = date in highEnergyDays
                        DualDayCell(
                            day = dayNumber,
                            info = info,
                            isHighEnergy = isHighEnergy,
                            phaseColors = phaseColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DualDayCell(
    day: Int,
    info: DualDayInfo?,
    isHighEnergy: Boolean,
    phaseColors: com.jeremy.lumi.ui.theme.PhaseColors,
    modifier: Modifier = Modifier
) {
    val (color1, color2) = if (info != null) {
        info.myPhase.toColor(phaseColors) to info.partnerPhase.toColor(phaseColors)
    } else {
        Color.Transparent to Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isHighEnergy) Color(0xFF7B2FBE).copy(alpha = 0.15f)
                else Color.Transparent
            )
            .then(
                if (isHighEnergy) Modifier.border(
                    1.dp, Color(0xFF7B2FBE).copy(alpha = 0.4f), RoundedCornerShape(10.dp)
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                color = if (isHighEnergy) Color.White else Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = if (isHighEnergy) FontWeight.SemiBold else FontWeight.Normal
            )
            if (info != null) {
                Spacer(modifier = Modifier.height(3.dp))
                // Dos puntos: izquierdo=yo, derecho=pareja
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(info.myPhase.toColor(phaseColors).copy(alpha = 0.85f))
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(info.partnerPhase.toColor(phaseColors).copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}

@Composable
private fun HighEnergyBanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "banner_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF7B2FBE).copy(alpha = 0.25f),
                        Color(0xFFE91E8C).copy(alpha = 0.25f)
                    )
                )
            )
            .border(
                1.dp,
                Color(0xFF7B2FBE).copy(alpha = glowAlpha * 0.6f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.dual_calendar_high_energy_banner),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}
