package com.jeremy.lumi.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.screens.home.PredictionFactorsUi
import com.jeremy.lumi.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tarjeta interactiva de factores de predicción transparente.
 * Inicialmente se presenta como "¿Cómo calculamos tu próximo periodo?".
 * Al tocarla, ejecuta una animación suave de "Calculando..." y revela
 * las estadísticas y barras de certeza del modelo.
 */
@Composable
fun PredictionFactorsCard(
    factors: PredictionFactorsUi,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isCalculating by remember { mutableStateOf(false) }
    var hasCalculatedOnce by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val isDark = LocalIsDarkTheme.current
    val surfaceColor = if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000)
    val borderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                      else Color(0x1A000000)
    
    val accentColor = MaterialTheme.colorScheme.primary
    val trackColor = if (isDark) Color(0x22FFFFFF) else Color(0x14000000)

    val shape = RoundedCornerShape(24.dp)

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ArrowRotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor)
            ) {
                if (!isExpanded) {
                    isExpanded = true
                    isCalculating = true
                    coroutineScope.launch {
                        delay(850) // Animación pausada de análisis
                        isCalculating = false
                        hasCalculatedOnce = true
                    }
                } else {
                    isExpanded = false
                }
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .padding(horizontal = 20.dp, vertical = if (isExpanded) 20.dp else 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Fila de Encabezado / Disparador ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = if (isExpanded) stringResource(R.string.home_factors_title) 
                                   else stringResource(R.string.home_factors_trigger_title),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isExpanded) stringResource(R.string.home_factors_subtitle) 
                                   else stringResource(R.string.home_factors_trigger_subtitle),
                            fontSize = 11.sp,
                            fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.Normal,
                            letterSpacing = if (isExpanded) 0.6.sp else 0.sp,
                            color = if (isExpanded) accentColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Flecha indicadora de acordeón
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotationAngle)
                )
            }

            // ── Contenido Expandido ───────────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(300)) + expandVertically(spring(stiffness = Spring.StiffnessLow)),
                exit = fadeOut(tween(200)) + shrinkVertically(spring(stiffness = Spring.StiffnessMedium))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isCalculating) {
                        // ── Estado 1: Animación fluida de cálculo ─────────────
                        CalculatingStateView(accentColor = accentColor)
                    } else {
                        // ── Estado 2: Resultados revelados ────────────────────
                        RevealedFactorsView(
                            factors = factors,
                            accentColor = accentColor,
                            trackColor = trackColor,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vista de carga suave mientras el motor analiza las estadísticas.
 */
@Composable
private fun CalculatingStateView(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "CalculatingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.08f))
            .padding(vertical = 18.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.5.dp,
            color = accentColor,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.size(22.dp).scale(pulseScale)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = stringResource(R.string.home_factors_calculating_title),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.home_factors_calculating_subtitle),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f)
            )
        }
    }
}

/**
 * Vista con las estadísticas, chip de certeza (sin saltos de línea) y barras animadas.
 */
@Composable
private fun RevealedFactorsView(
    factors: PredictionFactorsUi,
    accentColor: Color,
    trackColor: Color,
    isDark: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        
        // Fila con el chip de certeza (con espacio suficiente en una sola línea)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_factors_model_certainty),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )

            // Chip de Certeza en UNA SOLA LÍNEA (sin cortes feos)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = factors.confidenceLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 3 Barras de Contribución ───────────────────────────────────────
        FactorBarRow(
            label = stringResource(R.string.home_factors_regularity),
            score = factors.regularityScore,
            barColor = accentColor,
            trackColor = trackColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        FactorBarRow(
            label = stringResource(R.string.home_factors_history),
            score = factors.historyScore,
            barColor = accentColor,
            trackColor = trackColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        FactorBarRow(
            label = stringResource(R.string.home_factors_symptoms),
            score = factors.symptomsScore,
            barColor = accentColor,
            trackColor = trackColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Línea divisoria y nota de pie transparente ─────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(if (isDark) Color(0x1FFFFFFF) else Color(0x12000000))
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val marginNote = stringResource(R.string.home_factors_margin_note, factors.marginDaysText)
            val cyclesNote = stringResource(R.string.home_factors_cycles_count, factors.cycleCount)
            Text(
                text = "$marginNote · $cyclesNote",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun FactorBarRow(
    label: String,
    score: Int,
    barColor: Color,
    trackColor: Color
) {
    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animated = true
    }

    val progress = (score / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) progress else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "FactorBarAnim"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            modifier = Modifier.width(120.dp)
        )

        // Barra con cápsula redondeada
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.5.dp)
                .clip(CircleShape)
                .background(trackColor)
        ) {
            if (animatedProgress > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(barColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$score%",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.90f),
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}
