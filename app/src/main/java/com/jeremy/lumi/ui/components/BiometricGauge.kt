package com.jeremy.lumi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.HistoricalCycleStats
import com.jeremy.lumi.ui.theme.LocalIsDarkTheme

/**
 * Indicador de arco biométrico circular inspirado en Oura Health y Apple Fitness.
 * Dibuja una pista circular de fondo con un arco de progreso animado con puntas redondeadas.
 */
@Composable
fun BiometricArcGauge(
    progress: Float,
    valueText: String,
    labelText: String,
    statusText: String,
    arcColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 78.dp,
    strokeWidth: Dp = 6.5.dp
) {
    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animated = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "BiometricGaugeProgress"
    )

    val isDark = LocalIsDarkTheme.current
    val trackColor = if (isDark) Color(0x28FFFFFF) else Color(0x18000000)

    Column(
        modifier = modifier.width(IntrinsicSize.Min),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = strokeWidth.toPx()
                val arcPadding = strokePx / 2f
                val arcSize = Size(
                    width = this.size.width - strokePx,
                    height = this.size.height - strokePx
                )
                val topLeft = Offset(arcPadding, arcPadding)

                // Pista de fondo (260 grados)
                val startAngle = 140f
                val totalSweep = 260f

                drawArc(
                    color = trackColor,
                    startAngle = startAngle,
                    sweepAngle = totalSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Arco de progreso
                if (animatedProgress > 0.005f) {
                    drawArc(
                        color = arcColor,
                        startAngle = startAngle,
                        sweepAngle = totalSweep * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }

            // Valor central dentro del arco
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 1.dp)
            ) {
                Text(
                    text = valueText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = statusText,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = arcColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = labelText,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.80f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Fila de 3 arcos biométricos calculados a partir de [HistoricalCycleStats].
 * Utiliza el lenguaje tri-cromático de salud de Oura (Coral, Teal, Lavanda)
 * para garantizar máxima legibilidad y belleza en los 12 temas.
 */
@Composable
fun BiometricGaugesRow(
    stats: HistoricalCycleStats,
    modifier: Modifier = Modifier
) {
    val variability = (stats.longestCycle - stats.shortestCycle).coerceAtLeast(0)
    val regularityScore = ((1f - (variability.coerceIn(0, 16) / 20f)) * 100f).coerceIn(45f, 98f).toInt()
    
    val regularityStatus = when {
        regularityScore >= 85 -> stringResource(R.string.insights_regularity_high)
        regularityScore >= 70 -> stringResource(R.string.insights_regularity_moderate)
        else                  -> stringResource(R.string.insights_regularity_variable)
    }

    // Estado breve de 1 palabra para el ciclo medio para evitar cualquier desbordamiento
    val cycleStatus = when {
        stats.avgCycleLength in 26f..30f -> stringResource(R.string.insights_luteal_optimal)   // "Óptima"
        stats.avgCycleLength in 24f..35f -> stringResource(R.string.insights_luteal_sufficient) // "Normal"
        stats.avgCycleLength < 24f       -> stringResource(R.string.insights_luteal_short)      // "Corta"
        else                             -> stringResource(R.string.insights_regularity_variable) // "Variable"
    }

    val lutealStatus = when {
        stats.personalLutealLength in 12..16 -> stringResource(R.string.insights_luteal_optimal)
        stats.personalLutealLength in 10..11 -> stringResource(R.string.insights_luteal_sufficient)
        else                                 -> stringResource(R.string.insights_luteal_short)
    }

    val isDark = LocalIsDarkTheme.current
    
    // Paleta tri-cromática Oura/Flo para salud femenina (vibrante e inmune a temas monocromáticos)
    val regularityColor = if (isDark) Color(0xFFFF6B81) else Color(0xFFE85870) // Coral
    val cycleColor      = if (isDark) Color(0xFF4FD1C5) else Color(0xFF20A396) // Teal
    val lutealColor     = if (isDark) Color(0xFFA78BFA) else Color(0xFF8B5CF6) // Lavanda

    val cardBg = if (isDark) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.surface
    val cardBorder = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.55f) else Color(0x1A000000)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
            .padding(vertical = 18.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gauge 1: Regularidad
            BiometricArcGauge(
                progress = regularityScore / 100f,
                valueText = "$regularityScore%",
                labelText = stringResource(R.string.insights_regularity_title),
                statusText = regularityStatus,
                arcColor = regularityColor,
                modifier = Modifier.weight(1f)
            )

            // Gauge 2: Promedio de ciclo (escala 20 a 40 días, óptimo 28)
            val cycleNormProgress = ((stats.avgCycleLength - 20f) / 20f).coerceIn(0.1f, 1f)
            BiometricArcGauge(
                progress = cycleNormProgress,
                valueText = "${stats.avgCycleLength.toInt()}d",
                labelText = stringResource(R.string.insights_cycle_average),
                statusText = cycleStatus,
                arcColor = cycleColor,
                modifier = Modifier.weight(1f)
            )

            // Gauge 3: Fase Lútea (escala 0 a 16 días, óptimo ~14)
            val lutealNormProgress = (stats.personalLutealLength / 16f).coerceIn(0.1f, 1f)
            BiometricArcGauge(
                progress = lutealNormProgress,
                valueText = "${stats.personalLutealLength}d",
                labelText = stringResource(R.string.insights_luteal_title),
                statusText = lutealStatus,
                arcColor = lutealColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
