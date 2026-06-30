package com.jeremy.lumi.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.domain.model.CycleInsights
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosModeSheet(
    insights: CycleInsights,
    onDismiss: () -> Unit
) {
    val stats = insights.historicalStats

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera limpia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.MedicalServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Resumen Médico",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Generado el ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}. Este resumen contiene datos de los últimos 6 meses.",
                fontSize = 13.sp,
                color = Color.DarkGray,
                lineHeight = 18.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (stats == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay suficientes datos para generar un reporte.", color = Color.Gray)
                }
            } else {
                // ── SECCIÓN 1: Duración de Ciclos ──
                MedicalSectionHeader("Duración y Regularidad")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MedicalStatBox(
                        title = "Promedio del ciclo",
                        value = "${stats.avgCycleLength.toInt()} días",
                        modifier = Modifier.weight(1f)
                    )
                    MedicalStatBox(
                        title = "Días de sangrado",
                        value = "${stats.avgPeriodLength.toInt()} días",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MedicalStatBox(
                        title = "Ciclo más corto",
                        value = "${stats.shortestCycle} días",
                        modifier = Modifier.weight(1f)
                    )
                    MedicalStatBox(
                        title = "Ciclo más largo",
                        value = "${stats.longestCycle} días",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Variación (Irregularidad)
                val variation = stats.longestCycle - stats.shortestCycle
                if (variation > 9) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF4E5)) // Naranja suave
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Warning, null, tint = Color(0xFFE65100))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Alta variación detectada ($variation días de diferencia entre el ciclo más corto y el más largo).",
                            fontSize = 13.sp,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── SECCIÓN 2: Síntomas Recurrentes ──
                MedicalSectionHeader("Síntomas Frecuentes")
                if (insights.symptomCorrelations.isEmpty()) {
                    Text("No hay síntomas registrados frecuentemente.", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF3F4F6))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        insights.symptomCorrelations.take(4).forEach { corr ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• ${corr.symptomName.replaceFirstChar { it.uppercase() }}", fontSize = 14.sp, color = Color.Black)
                                Text("Fase dominante: ${phaseToName(corr.dominantPhase)}", fontSize = 13.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── SECCIÓN 3: Últimos 6 Ciclos ──
                MedicalSectionHeader("Historial (Últimos 6 ciclos)")
                if (insights.recentCycleStats.isEmpty()) {
                    Text("No hay historial disponible.", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF3F4F6))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header
                        Row(Modifier.fillMaxWidth()) {
                            Text("Duración", modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Sangrado", modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Dolor Prom.", modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                        insights.recentCycleStats.take(6).forEach { cycle ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${cycle.durationDays} d", modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color.Black)
                                Text("${cycle.bleedingDays} d", modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color.Black)
                                Text("${"%.1f".format(cycle.avgPainLevel)}/10", modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun MedicalSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
}

@Composable
private fun MedicalStatBox(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF3F4F6)) // Gris muy claro
            .padding(16.dp)
    ) {
        Text(title, fontSize = 12.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

private fun phaseToName(phase: com.jeremy.lumi.domain.model.CyclePhase): String {
    return when (phase) {
        com.jeremy.lumi.domain.model.CyclePhase.MENSTRUAL -> "Menstrual"
        com.jeremy.lumi.domain.model.CyclePhase.FOLLICULAR -> "Folicular"
        com.jeremy.lumi.domain.model.CyclePhase.OVULATION -> "Ovulación"
        com.jeremy.lumi.domain.model.CyclePhase.LUTEAL -> "Lútea"
        else -> "N/A"
    }
}
