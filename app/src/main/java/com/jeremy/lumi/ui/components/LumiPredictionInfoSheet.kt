package com.jeremy.lumi.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.domain.usecase.CyclePrediction
import com.jeremy.lumi.ui.screens.home.PredictionFactorsUi
import com.jeremy.lumi.ui.theme.LumiThemeTokens

/**
 * Modal BottomSheet que explica con transparencia clínica y métricas reales
 * cómo calcula Lumi las predicciones del ciclo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumiPredictionInfoSheet(
    predictionFactors: PredictionFactorsUi? = null,
    prediction: CyclePrediction? = null,
    onDismiss: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = tokens.panel,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(tokens.dim.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cómo interpreta Lumi tu ciclo",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Transparencia clínica y calibración basada en tus datos",
                        fontSize = 11.5.sp,
                        color = tokens.muted
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tokens.pill)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar",
                        tint = tokens.text,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tarjeta de métricas reales calculadas
            if (predictionFactors != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(tokens.panel2)
                        .border(1.dp, tokens.line, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    // Fila de Certeza del Modelo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Certeza del modelo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.text
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.accentSoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(tokens.accent)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = predictionFactors.confidenceLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.accent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Barras de progreso de los 3 pilares
                    FactorProgressRow(
                        label = "Regularidad de ciclos",
                        score = predictionFactors.regularityScore,
                        barColor = tokens.accent,
                        trackColor = tokens.pill
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FactorProgressRow(
                        label = "Historial acumulado (${predictionFactors.cycleCount} ciclos)",
                        score = predictionFactors.historyScore,
                        barColor = tokens.accent,
                        trackColor = tokens.pill
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FactorProgressRow(
                        label = "Registro de síntomas y estado",
                        score = predictionFactors.symptomsScore,
                        barColor = tokens.accent,
                        trackColor = tokens.pill
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(tokens.line)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = tokens.dim,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Margen de variabilidad: ${predictionFactors.marginDaysText} · ${predictionFactors.cycleCount} ciclos completados",
                            fontSize = 11.sp,
                            color = tokens.muted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Explicación metodológica
            Text(
                text = "Metodología honesta",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.text
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "En Lumi tu fecha estimada de periodo y fases se determinan mediante una media ponderada que da mayor peso a tus ciclos más recientes, calibrada con tus síntomas físicos diarios.\n\nNo utilizamos cajas negras ni inventamos certezas: cuando hay pocos ciclos registrados, Lumi te lo indica con honestidad hasta que tu patrón biológico se consolide.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = tokens.muted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Aviso médico
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.pill)
                    .padding(14.dp)
            ) {
                Text(
                    text = "Aviso médico importante: Estas estimaciones tienen propósitos informativos y de bienestar. No constituyen un método anticonceptivo ni reemplazan el asesoramiento ginecológico profesional.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = tokens.dim
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de confirmación / cerrar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.accent)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Entendido",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tokens.isDark) tokens.bg else Color.White
                )
            }
        }
    }
}

@Composable
private fun FactorProgressRow(
    label: String,
    score: Int,
    barColor: Color,
    trackColor: Color
) {
    val tokens = LumiThemeTokens.current
    val progressFraction = (score.coerceIn(0, 100) / 100f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 600),
        label = "FactorBarProgress"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                color = tokens.text
            )
            Text(
                text = "$score%",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.accent
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}
