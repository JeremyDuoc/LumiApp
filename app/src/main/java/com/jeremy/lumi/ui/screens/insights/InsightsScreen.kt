package com.jeremy.lumi.ui.screens.insights

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.foundation.border
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  INSIGHTS SCREEN
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSosSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // PDF Export: cuando el ViewModel entrega bytes, abrimos el selector de destino
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bytes = uiState.pdfBytes ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        }
        viewModel.clearPdfBytes()
    }

    LaunchedEffect(uiState.pdfBytes) {
        if (uiState.pdfBytes != null) {
            val today = java.time.LocalDate.now()
            pdfLauncher.launch("Lumi_Reporte_Medico_$today.pdf")
        }
    }

    LaunchedEffect(uiState.pdfError) {
        uiState.pdfError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPdfError()
        }
    }

    var screenReady by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) { delay(60); screenReady = true }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack, 
                            contentDescription = "Atrás", 
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = stringResource(R.string.insights_title),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 17.sp,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text     = stringResource(R.string.insights_subtitle),
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.insights == null) {
            // Estado vacío â€” todavía no hay ciclos suficientes
            InsightsEmptyState(Modifier.padding(paddingValues))
        } else {
            val insights = uiState.insights!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // FIX P2-5: Aviso para usuarias con anticonceptivos hormonales.
                // Las estadísticas de ovulación y fase lúteal no les aplican.
                if (uiState.isOnContraceptive) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = androidx.compose.ui.Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.insights_pill_mode_note),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // â”€â”€ Resumen de ciclos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                insights.historicalStats?.let { stats ->
                    InsightsFadeIn(screenReady, 0) {
                        CycleStatsCard(stats)
                    }
                }

                // -- Graficas Avanzadas (Canvas premium) --
                // Seccion con su propio GraficasViewModel: historial detallado
                // de ciclos, tendencia de dolor/estres por dia y curva BBT.
                InsightsFadeIn(screenReady, 100) {
                    GraficasAvanzadasSection()
                }

                // â”€â”€ Donut Chart: Distribución de Fases (Canvas) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                insights.historicalStats?.let { stats ->
                    InsightsFadeIn(screenReady, 120) {
                        PhaseDistributionDonutCard(stats)
                    }
                }

                // ── Síntomas × Fase ──────────────────────────────────────
                if (insights.symptomCorrelations.isNotEmpty()) {
                    InsightsFadeIn(screenReady, 160) {
                        SymptomCorrelationCard(insights.symptomCorrelations.take(5))
                    }
                }

                // â”€â”€ Distribución de humor â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                insights.moodDistribution?.let { mood ->
                    if (mood.totalDays > 0) {
                        InsightsFadeIn(screenReady, 240) {
                            MoodDistributionCard(mood)
                        }
                    }
                }

                // ── Descubrimientos Ocultos (Bivariables) ────────────────────────────────
                if (insights.bivariableInsights.isNotEmpty()) {
                    InsightsFadeIn(screenReady, 280) {
                        BivariableInsightsCard(insights.bivariableInsights)
                    }
                }

                // ── Botón Modo SOS (Resumen Médico) ──────────────────────────────────────
                InsightsFadeIn(screenReady, 300) {
                    Button(
                        onClick = { showSosSheet = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.MonitorHeart, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Preparar visita médica", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Exportar PDF para el médico ──────────────────────────────────────────
                InsightsFadeIn(screenReady, 360) {
                    OutlinedButton(
                        onClick  = { viewModel.generateMedicalReport() },
                        enabled  = !uiState.isGeneratingPdf,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(18.dp),
                        border   = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        if (uiState.isGeneratingPdf) {
                            CircularProgressIndicator(
                                modifier  = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color     = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Generando reporte...", fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(
                                androidx.compose.material.icons.Icons.Rounded.Description,
                                contentDescription = null,
                                tint   = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Exportar para mi Doctor \uD83D\uDCC4",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
            
            if (showSosSheet) {
                SosModeSheet(insights = insights) {
                    showSosSheet = false
                }
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  TARJETA 1 â€” Estadísticas históricas de ciclos
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun CycleStatsCard(stats: HistoricalCycleStats) {
    val primary = MaterialTheme.colorScheme.primary

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader(
                icon  = Icons.Rounded.AutoGraph,
                title = stringResource(R.string.insights_section_cycle)
            )

            // Grid 2×2 de métricas
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricChip(
                    label = stringResource(R.string.insights_avg_cycle),
                    value = "${stats.avgCycleLength.toInt()}",
                    unit  = stringResource(R.string.insights_days_suffix),
                    color = primary,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = stringResource(R.string.insights_avg_period),
                    value = "${stats.avgPeriodLength.toInt()}",
                    unit  = stringResource(R.string.insights_days_suffix),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricChip(
                    label = stringResource(R.string.insights_shortest),
                    value = "${stats.shortestCycle}",
                    unit  = stringResource(R.string.insights_days_suffix),
                    color = primary.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = stringResource(R.string.insights_longest),
                    value = "${stats.longestCycle}",
                    unit  = stringResource(R.string.insights_days_suffix),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Fase lútea personal
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(primary.copy(alpha = 0.07f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text      = "Fase lútea personal",
                    fontSize  = 13.sp,
                    color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text       = "${stats.personalLutealLength} días",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = primary
                )
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  TARJETA 2 â€” Gráfico de barras de ciclos recientes (Canvas propio)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun RecentCyclesBarChart(cycles: List<CycleStats>) {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val maxDur    = cycles.maxOf { it.durationDays }.toFloat().coerceAtLeast(1f)

    // Animación de entrada de barras
    val barProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        barProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader(
                icon  = Icons.Rounded.Analytics,
                title = "Últimos ${cycles.size} ciclos"
            )

            Row(
                modifier              = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                cycles.forEachIndexed { index, cycle ->
                    val heightFraction = (cycle.durationDays / maxDur) * barProgress.value
                    val barColor = when (cycle.cycleType) {
                        CycleType.SHORT  -> secondary
                        CycleType.NORMAL -> primary
                        CycleType.LONG   -> MaterialTheme.colorScheme.tertiary
                    }
                    Column(
                        modifier            = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text     = "${cycle.durationDays}",
                            fontSize = 10.sp,
                            color    = barColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(heightFraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(barColor, barColor.copy(alpha = 0.5f))
                                    )
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text     = "C${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Leyenda
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(primary,   "Normal")
                LegendDot(secondary, "Corto")
                LegendDot(MaterialTheme.colorScheme.tertiary, "Largo")
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  TARJETA 3 — Heatmap de Correlaciones síntoma × fase
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun SymptomCorrelationCard(correlations: List<SymptomCorrelation>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                icon  = Icons.Rounded.WaterDrop,
                title = "Heatmap de Síntomas"
            )

            // Leyenda de columnas para el heatmap
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("M", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f), modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("F", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f), modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("O", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f), modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("L", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f), modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                }
            }

            correlations.forEach { corr ->
                SymptomCorrelationRow(corr)
            }
        }
    }
}

@Composable
private fun SymptomCorrelationRow(corr: SymptomCorrelation) {
    val phaseColors = com.jeremy.lumi.ui.theme.LocalPhaseColors.current

    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(corr.symptomName, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val phases = listOf(
                CyclePhase.MENSTRUAL to phaseColors.menstrual,
                CyclePhase.FOLLICULAR to phaseColors.follicular,
                CyclePhase.OVULATION to phaseColors.ovulation,
                CyclePhase.LUTEAL to phaseColors.luteal
            )
            
            phases.forEach { (phase, baseColor) ->
                val ratio = if (corr.totalOccurrences == 0) 0f else (corr.occurrenceByPhase[phase] ?: 0) / corr.totalOccurrences.toFloat()
                
                // Animación de opacidad para el Heatmap
                val alphaAnim = remember { Animatable(0.02f) }
                LaunchedEffect(corr.symptomName) {
                    alphaAnim.animateTo(ratio.coerceAtLeast(0.05f), tween(800, easing = FastOutSlowInEasing))
                }
                
                // La fase dominante se determina buscando el máximo (no comparando floats con ==)
                val dominantPhase = corr.occurrenceByPhase.maxByOrNull { it.value }?.key
                val isDominant = dominantPhase == phase && ratio > 0f
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(baseColor.copy(alpha = alphaAnim.value))
                ) {
                    if (isDominant) {
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  TARJETA 4 â€” Distribución de humor
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun MoodDistributionCard(mood: MoodDistribution) {
    val primary = MaterialTheme.colorScheme.primary

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(
                icon  = Icons.Rounded.Favorite,
                title = stringResource(R.string.insights_section_mood)
            )

            mood.distribution.entries
                .sortedByDescending { it.value }
                .forEach { (moodLabel, count) ->
                    val ratio   = mood.ratioOf(moodLabel)
                    val barAnim = remember(moodLabel) { Animatable(0f) }
                    LaunchedEffect(moodLabel) {
                        barAnim.animateTo(ratio, tween(700, easing = FastOutSlowInEasing))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text     = moodLabel,
                            fontSize = 13.sp,
                            color    = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.width(80.dp)
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(primary.copy(alpha = 0.12f))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(barAnim.value)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(primary)
                            )
                        }
                        Text(
                            text     = "${(ratio * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color    = primary,
                            modifier = Modifier.width(32.dp)
                        )
                    }
                }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  EMPTY STATE
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun InsightsEmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Analytics, null,
                    Modifier.size(36.dp), MaterialTheme.colorScheme.primary)
            }
            Text(
                text       = stringResource(R.string.insights_no_data),
                fontSize   = 14.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp,
                color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier   = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  HELPERS VISUALES
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun InsightsFadeIn(visible: Boolean, delayMs: Int, content: @Composable () -> Unit) {
    val alpha  = remember { Animatable(0f) }
    val offset = remember { Animatable(14f) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMs.toLong())
            val j1 = launch { alpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
            val j2 = launch { offset.animateTo(0f,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            j1.join(); j2.join()
        }
    }
    Box(Modifier.alpha(alpha.value).graphicsLayer { translationY = offset.value.dp.toPx() }) {
        content()
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun MetricChip(
    label    : String,
    value    : String,
    unit     : String,
    color    : androidx.compose.ui.graphics.Color,
    modifier : Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.width(3.dp))
            Text(unit, fontSize = 12.sp, color = color.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    }
}

@Composable
private fun phaseColor(phase: com.jeremy.lumi.domain.model.CyclePhase): androidx.compose.ui.graphics.Color {
    val phaseColors = com.jeremy.lumi.ui.theme.LocalPhaseColors.current
    return when (phase) {
        com.jeremy.lumi.domain.model.CyclePhase.MENSTRUAL  -> phaseColors.menstrual
        com.jeremy.lumi.domain.model.CyclePhase.FOLLICULAR -> phaseColors.follicular
        com.jeremy.lumi.domain.model.CyclePhase.OVULATION  -> phaseColors.ovulation
        com.jeremy.lumi.domain.model.CyclePhase.LUTEAL     -> phaseColors.luteal
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    }
}

private fun phaseName(phase: com.jeremy.lumi.domain.model.CyclePhase): String = when (phase) {
    com.jeremy.lumi.domain.model.CyclePhase.MENSTRUAL  -> "Menstrual"
    com.jeremy.lumi.domain.model.CyclePhase.FOLLICULAR -> "Folicular"
    com.jeremy.lumi.domain.model.CyclePhase.OVULATION  -> "Ovulación"
    com.jeremy.lumi.domain.model.CyclePhase.LUTEAL     -> "Lútea"
    else -> ""
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  TARJETA 5 â€” Donut Chart: Distribución Promedio de Fases (Canvas)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun PhaseDistributionDonutCard(stats: HistoricalCycleStats) {
    val phaseColors = com.jeremy.lumi.ui.theme.LocalPhaseColors.current
    
    // Cálculo de duraciones promedio
    val total = stats.avgCycleLength.coerceAtLeast(15f)
    val menstrual = stats.avgPeriodLength
    val luteal = stats.personalLutealLength.toFloat()
    val ovulation = 3f // Consistente con los 3 días de la ventana fértil en Home
    val follicular = (total - menstrual - luteal - ovulation).coerceAtLeast(1f)
    
    // Animación de llenado del donut
    val sweepAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        sweepAnim.animateTo(360f, tween(1200, easing = FastOutSlowInEasing))
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionHeader(
                icon  = Icons.Rounded.Analytics, 
                title = "Distribución Promedio"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // â”€â”€ Donut en Canvas â”€â”€
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
                        val strokeWidth = 24.dp.toPx()
                        
                        var currentStartAngle = -90f // Empezamos en las 12 en punto
                        
                        // Lista de segmentos ordenados lógicamente
                        val segments = listOf(
                            Pair(menstrual, phaseColors.menstrual),
                            Pair(follicular, phaseColors.follicular),
                            Pair(ovulation, phaseColors.ovulation),
                            Pair(luteal, phaseColors.luteal)
                        )
                        val actualTotal = menstrual + follicular + ovulation + luteal
                        
                        segments.forEach { (days, color) ->
                            val sweepAngle = (days / actualTotal) * sweepAnim.value
                            drawArc(
                                color = color,
                                startAngle = currentStartAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidth,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Butt
                                )
                            )
                            currentStartAngle += sweepAngle
                        }
                    }
                    
                    // Texto en el centro
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${total.toInt()}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "días",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            // Leyenda de 4 columnas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendDot(phaseColors.menstrual, "Mens.")
                LegendDot(phaseColors.follicular, "Folíc.")
                LegendDot(phaseColors.ovulation, "Ovul.")
                LegendDot(phaseColors.luteal, "Lútea")
            }
        }
    }
}



// -----------------------------------------------------------------------------
//  TARJETA: DESCUBRIMIENTOS OCULTOS (Bivariables)
// -----------------------------------------------------------------------------

@Composable
fun BivariableInsightsCard(insights: List<com.jeremy.lumi.domain.model.BivariableInsight>) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icono mágico con fondo brillante
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(primary.copy(alpha = 0.2f), secondary.copy(alpha = 0.2f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star, // Changed from AutoAwesome to Star which is definitely in core
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.insights_lumi_discoveries),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.height(16.dp))

            insights.forEachIndexed { index, insight ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    primary.copy(alpha = 0.12f),
                                    secondary.copy(alpha = 0.04f)
                                )
                            )
                        )
                        .then(
                            Modifier.border(
                                1.dp,
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(primary.copy(alpha = 0.3f), Color.Transparent)
                                ),
                                RoundedCornerShape(16.dp)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(tertiary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = insight.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = insight.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                    }
                }
                if (index < insights.size - 1) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

