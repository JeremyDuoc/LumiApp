package com.jeremy.lumi.ui.screens.insights

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.AlertSeverity
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.CycleType
import com.jeremy.lumi.domain.model.HistoricalCycleStats
import com.jeremy.lumi.domain.model.LumiObservation
import com.jeremy.lumi.domain.model.PuntoBarraCiclo
import com.jeremy.lumi.domain.model.PuntoBbt
import com.jeremy.lumi.ui.components.AmbientBackgroundLayer
import com.jeremy.lumi.ui.components.LumiBrandFlower
import com.jeremy.lumi.ui.components.LumiBrandLockup
import com.jeremy.lumi.ui.components.LumiThemeSheet
import com.jeremy.lumi.ui.screens.settings.SettingsViewModel
import com.jeremy.lumi.ui.theme.AmbientScreen
import com.jeremy.lumi.ui.theme.LocalPhaseColors
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTheme by settingsViewModel.currentTheme.collectAsStateWithLifecycle()
    val tokens = LumiThemeTokens.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var showSosSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Resumen, 1: Tendencias, 2: Bienestar

    // PDF Export launcher nativo
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bytes = uiState.pdfBytes ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        }
        viewModel.clearPdfBytes()
        Toast.makeText(context, context.getString(R.string.insights_pdf_saved), Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(uiState.pdfBytes) {
        if (uiState.pdfBytes != null) {
            val today = com.jeremy.lumi.utils.LumiClock.today()
            pdfLauncher.launch("Lumi_Reporte_Medico_$today.pdf")
        }
    }

    LaunchedEffect(uiState.pdfError) {
        uiState.pdfError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPdfError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.bg)
    ) {
        AmbientBackgroundLayer(
            screenType = AmbientScreen.CONTENT,
            scrollOffsetProvider = { scrollState.value.toFloat() }
        )

        Scaffold(
            topBar = {
                // Cabecera de marca Lumi aprobada: Logo + Flor + Botones de acción
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LumiBrandLockup(
                        flowerSize = 22,
                        fontSize = 22.sp,
                        starSize = 14.sp,
                        letterSpacing = (-1.2).sp,
                        animated = true
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showThemeSheet = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = stringResource(R.string.insights_cd_atmosphere),
                                tint = tokens.muted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.openSheet(InsightsSheetType.INFO) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(R.string.insights_cd_about_data),
                                tint = tokens.muted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = tokens.accent)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                ) {
                    // Control de rango compacto (Sin encabezado decorativo rechazado)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(tokens.panel)
                                .border(1.dp, tokens.line, RoundedCornerShape(22.dp))
                                .clickable {
                                    val nextRange = when (uiState.cycleRange) {
                                        3 -> 6
                                        6 -> 12
                                        else -> 3
                                    }
                                    viewModel.setCycleRange(nextRange)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = tokens.text,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = stringResource(R.string.insights_last_n_cycles, uiState.cycleRange),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = tokens.text
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = tokens.muted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = if (uiState.availableCyclesCount > 0)
                                stringResource(R.string.insights_n_records_analyzed, uiState.availableCyclesCount.coerceAtMost(uiState.cycleRange))
                            else stringResource(R.string.insights_no_records),
                            fontSize = 10.sp,
                            color = tokens.dim
                        )
                    }

                    // Enlaces de navegación rápida por sección
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        listOf("Resumen", "Tendencias", "Bienestar").forEachIndexed { index, tabTitle ->
                            val isActive = activeTab == index
                            Column(
                                modifier = Modifier
                                    .clickable {
                                        activeTab = index
                                        coroutineScope.launch {
                                            when (index) {
                                                0 -> scrollState.animateScrollTo(0)
                                                1 -> scrollState.animateScrollTo(420)
                                                2 -> scrollState.animateScrollTo(1080)
                                            }
                                        }
                                    }
                                    .padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = tabTitle,
                                    fontSize = 12.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isActive) tokens.text else tokens.dim
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .height(2.dp)
                                        .width(36.dp)
                                        .background(if (isActive) tokens.accent else Color.Transparent)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(26.dp)
                    ) {
                        // Aviso informativo para usuarias con anticonceptivos hormonales
                        if (uiState.isOnContraceptive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(tokens.accentSoft)
                                    .border(1.dp, tokens.line, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Timeline,
                                        contentDescription = null,
                                        tint = tokens.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.insights_pill_mode_note),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = tokens.text
                                    )
                                }
                            }
                        }

                        // ── RESUMEN ESTADÍSTICO (Hero Card) ──────────────────────────
                        val stats = uiState.insights?.historicalStats
                        SectionHeroCard(
                            stats = stats,
                            observationsCount = uiState.observations.size,
                            onViewObservations = { viewModel.openSheet(InsightsSheetType.OBSERVATIONS) }
                        )

                        // ── 01 / EVOLUCIÓN: Así han cambiado ─────────────────────────
                        SectionEvolucion(
                            stats = stats,
                            cycleBars = uiState.cycleBars,
                            selectedCycleIndex = uiState.selectedCycleIndex,
                            onSelectBar = { viewModel.selectCycleBar(it) },
                            onOpenObservations = { viewModel.openSheet(InsightsSheetType.OBSERVATIONS) },
                            isPhaseExpanded = uiState.isPhaseDistributionExpanded,
                            onTogglePhaseExpanded = { viewModel.togglePhaseDistribution() }
                        )

                        // ── 02 / BIENESTAR: Lo que registras ─────────────────────────
                        SectionBienestar(
                            symptomCorrelations = uiState.insights?.symptomCorrelations ?: emptyList(),
                            selectedSymptomNote = uiState.selectedSymptomNote,
                            onSelectSymptomCell = { sym, phase, lvl, cnt ->
                                viewModel.selectSymptomCell(sym, phase, lvl, cnt)
                            },
                            moodDistribution = uiState.insights?.moodDistribution,
                            showAllMoods = uiState.showAllMoods,
                            onToggleShowAllMoods = { viewModel.toggleShowAllMoods() },
                            bbtPoints = uiState.bbtPoints,
                            hayDatosBbt = uiState.hayDatosBbt,
                            onAddDemoTemp = { day, temp -> viewModel.addDemoTemperature(day, temp) },
                            onClearDemoTemp = { viewModel.clearDemoTemperatures() }
                        )

                        // ── 03 / INTERPRETACIÓN: Mirar con calma ─────────────────────
                        SectionInterpretacion(
                            bivariableInsights = uiState.insights?.bivariableInsights ?: emptyList(),
                            onOpenObservations = { viewModel.openSheet(InsightsSheetType.OBSERVATIONS) }
                        )

                        // ── 04 / ACOMPAÑAMIENTO: Cuando quieras hablarlo ─────────────
                        SectionAcompanamiento(
                            onOpenConsult = { showSosSheet = true },
                            onExportPdf = { viewModel.generateMedicalReport() },
                            isGeneratingPdf = uiState.isGeneratingPdf
                        )

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }

            // ── HOJAS MODALES ─────────────────────────────────────────────────────────

            // Modal de Selección de Tema
            if (showThemeSheet) {
                LumiThemeSheet(
                    selectedTheme = currentTheme,
                    onSelectTheme = { settingsViewModel.changeTheme(it) },
                    onDismiss = { showThemeSheet = false }
                )
            }

            // Modal de Preparación Médica (SosModeSheet nativa preservada)
            if (showSosSheet && uiState.insights != null) {
                SosModeSheet(
                    insights = uiState.insights!!,
                    medicalAlerts = uiState.medicalAlerts,
                    isOnContraceptive = uiState.isOnContraceptive,
                    lmpDate = uiState.lmpDate,
                    daysSinceLmp = uiState.daysSinceLmp,
                    onExportPdf = {
                        showSosSheet = false
                        viewModel.generateMedicalReport()
                    },
                    onDismiss = { showSosSheet = false }
                )
            }

            // Modal "Observaciones de Lumi"
            if (uiState.activeSheet == InsightsSheetType.OBSERVATIONS) {
                ObservationsSheet(
                    observations = uiState.observations,
                    onDismiss = { viewModel.closeSheet() },
                    onOpenConsult = {
                        viewModel.closeSheet()
                        showSosSheet = true
                    }
                )
            }

            // Modal "Cómo leer Insights"
            if (uiState.activeSheet == InsightsSheetType.INFO) {
                InfoExplanationSheet(onDismiss = { viewModel.closeSheet() })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  COMPONENTE: RESUMEN ESTADÍSTICO (Hero Card)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeroCard(
    stats: HistoricalCycleStats?,
    observationsCount: Int,
    onViewObservations: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(tokens.glow, Color.Transparent),
                        center = Offset(Float.POSITIVE_INFINITY, 0f),
                        radius = 350f
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .background(tokens.panel)
                .border(1.dp, tokens.line, RoundedCornerShape(20.dp))
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.insights_avg_cycle_length),
                        fontSize = 11.sp,
                        color = tokens.muted
                    )
                    Icon(
                        imageVector = Icons.Rounded.Timeline,
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (stats != null) "${stats.avgCycleLength.roundToInt()}" else "--",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-4).sp,
                        color = tokens.text,
                        lineHeight = 72.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.insights_days_unit),
                        fontSize = 14.sp,
                        color = tokens.muted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = tokens.line, thickness = 1.dp)
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.insights_period_days_caps),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = tokens.dim
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (stats != null) "${stats.avgPeriodLength.roundToInt()}" else "--",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.text
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.insights_days_unit),
                                fontSize = 10.sp,
                                color = tokens.muted,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.insights_observed_range_caps),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = tokens.dim
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (stats != null) "${stats.shortestCycle}–${stats.longestCycle}" else "--",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.text
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.insights_days_unit),
                                fontSize = 10.sp,
                                color = tokens.muted,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Flagline contextual
        val hasVariation = stats != null && (stats.longestCycle - stats.shortestCycle > 9)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(if (hasVariation) Color(0x28ECC78F) else tokens.panel2)
                    .border(1.dp, if (hasVariation) Color(0x60ECC78F) else tokens.line, RoundedCornerShape(30.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(
                        imageVector = if (hasVariation) Icons.Rounded.WarningAmber else Icons.Rounded.Check,
                        contentDescription = null,
                        tint = if (hasVariation) Color(0xFFECC78F) else tokens.accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (hasVariation) stringResource(R.string.insights_variation_to_review) else stringResource(R.string.insights_steady_rhythm),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasVariation) Color(0xFFECC78F) else tokens.accent
                    )
                }
            }

            TextButton(
                onClick = onViewObservations,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.insights_view_n_observations, observationsCount),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.accent
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SECCIÓN 01 / EVOLUCIÓN: Así han cambiado
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionEvolucion(
    stats: HistoricalCycleStats?,
    cycleBars: List<PuntoBarraCiclo>,
    selectedCycleIndex: Int?,
    onSelectBar: (Int) -> Unit,
    onOpenObservations: () -> Unit,
    isPhaseExpanded: Boolean,
    onTogglePhaseExpanded: () -> Unit
) {
    val tokens = LumiThemeTokens.current
    val phaseColors = LocalPhaseColors.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(
                text = stringResource(R.string.insights_badge_evolution),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = tokens.dim
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.insights_evolution_title),
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.8).sp,
                color = tokens.text
            )
            Text(
                text = stringResource(R.string.insights_evolution_desc),
                fontSize = 11.sp,
                color = tokens.muted
            )
        }

        // Contenedor de la gráfica interactiva de barras
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(tokens.panel)
                .border(1.dp, tokens.line, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.insights_cycle_history_title),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.text
                        )
                        Text(
                            text = stringResource(R.string.insights_select_bar_hint),
                            fontSize = 10.sp,
                            color = tokens.muted
                        )
                    }
                    Text(
                        text = if (stats != null) stringResource(R.string.insights_avg_cycle_days_format, stats.avgCycleLength.roundToInt()) else stringResource(R.string.insights_avg_cycle_empty),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.accent
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Área de dibujo de barras
                if (cycleBars.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.insights_not_enough_cycles),
                            fontSize = 11.sp,
                            color = tokens.muted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val maxDays = cycleBars.maxOfOrNull { it.duracionDias }?.toFloat()?.coerceAtLeast(45f) ?: 50f
                    val selectedIdx = selectedCycleIndex ?: (cycleBars.size - 1)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        cycleBars.forEachIndexed { index, bar ->
                            val isSelected = selectedIdx == index
                            val barHeightFraction = (bar.duracionDias / maxDays).coerceIn(0.12f, 1f)

                            val color1 = when (bar.tipoCiclo) {
                                CycleType.LONG -> Color(0xFFAAA5B4)
                                CycleType.SHORT -> Color(0xFFE6A8C6)
                                CycleType.NORMAL -> tokens.rose
                            }
                            val color2 = when (bar.tipoCiclo) {
                                CycleType.LONG -> Color(0xFF777381)
                                CycleType.SHORT -> Color(0xFF7B6382)
                                CycleType.NORMAL -> tokens.rose.copy(alpha = 0.35f)
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectBar(index) }
                                    .padding(horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = "${bar.duracionDias}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) tokens.text else tokens.muted
                                )
                                Spacer(Modifier.height(5.dp))

                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height((barHeightFraction * 115).dp)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                        .background(Brush.verticalGradient(listOf(color1, color2)))
                                        .then(
                                            if (isSelected) Modifier.border(
                                                1.5.dp,
                                                tokens.accent,
                                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                            ) else Modifier
                                        )
                                )

                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = bar.etiqueta,
                                    fontSize = 9.sp,
                                    color = if (isSelected) tokens.text else tokens.dim,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = tokens.line, thickness = 1.dp)
                    Spacer(Modifier.height(10.dp))

                    val activeBar = cycleBars.getOrNull(selectedIdx) ?: cycleBars.last()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.insights_active_bar_info, activeBar.etiqueta, activeBar.duracionDias, stats?.avgCycleLength?.roundToInt() ?: 0),
                            fontSize = 10.sp,
                            color = tokens.text
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(tokens.panel2)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.insights_n_records, cycleBars.size),
                                fontSize = 9.sp,
                                color = tokens.muted
                            )
                        }
                    }
                }
            }
        }

        // Tarjeta de observación contextual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0x28ECC78F), tokens.panel)))
                .border(1.dp, tokens.line, RoundedCornerShape(16.dp))
                .padding(15.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0x35ECC78F)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timeline,
                        contentDescription = null,
                        tint = Color(0xFFECC78F),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.insights_variation_context_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (stats != null)
                            stringResource(R.string.insights_variation_context_desc, stats.shortestCycle, stats.longestCycle)
                        else stringResource(R.string.insights_variation_empty_desc),
                        fontSize = 11.sp,
                        color = tokens.muted,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.insights_understand_observation),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.accent,
                        modifier = Modifier.clickable { onOpenObservations() }
                    )
                }
            }
        }

        // Acordeón: Distribución ilustrativa de fases
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTogglePhaseExpanded() }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.insights_view_phase_distribution),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text
                )
                Icon(
                    imageVector = if (isPhaseExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = tokens.muted,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = isPhaseExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Donut en Canvas
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 14.dp.toPx()
                                val segments = listOf(
                                    Pair(5f, phaseColors.menstrual),
                                    Pair(10f, Color(0xFFECC78F)),
                                    Pair(3f, phaseColors.ovulation),
                                    Pair(14f, tokens.accent)
                                )
                                val totalDays = 32f
                                var currentAngle = -90f

                                segments.forEach { (days, color) ->
                                    val sweep = (days / totalDays) * 360f
                                    drawArc(
                                        color = color,
                                        startAngle = currentAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth)
                                    )
                                    currentAngle += sweep
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${stats?.avgCycleLength?.roundToInt() ?: 32}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.text
                                )
                                Text(
                                    text = stringResource(R.string.unit_days),
                                    fontSize = 9.sp,
                                    color = tokens.muted
                                )
                            }
                        }

                        // Leyenda de fases
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            LegendRow(color = phaseColors.menstrual, text = stringResource(R.string.insights_legend_menstrual))
                            LegendRow(color = Color(0xFFECC78F), text = stringResource(R.string.insights_legend_follicular))
                            LegendRow(color = phaseColors.ovulation, text = stringResource(R.string.insights_legend_ovulation))
                            LegendRow(color = tokens.accent, text = stringResource(R.string.insights_legend_luteal))
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.insights_phase_distribution_note),
                        fontSize = 10.sp,
                        color = tokens.muted,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SECCIÓN 02 / BIENESTAR: Lo que registras
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionBienestar(
    symptomCorrelations: List<com.jeremy.lumi.domain.model.SymptomCorrelation>,
    selectedSymptomNote: String?,
    onSelectSymptomCell: (String, CyclePhase, Int, Int) -> Unit,
    moodDistribution: com.jeremy.lumi.domain.model.MoodDistribution?,
    showAllMoods: Boolean,
    onToggleShowAllMoods: () -> Unit,
    bbtPoints: List<PuntoBbt>,
    hayDatosBbt: Boolean,
    onAddDemoTemp: (Int, Float) -> Unit,
    onClearDemoTemp: () -> Unit
) {
    val tokens = LumiThemeTokens.current
    var demoDay by remember { mutableIntStateOf(12) }
    var demoTemp by remember { mutableFloatStateOf(36.5f) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text(
                text = stringResource(R.string.insights_badge_wellbeing),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = tokens.dim
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.insights_wellbeing_title),
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.8).sp,
                color = tokens.text
            )
            Text(
                text = stringResource(R.string.insights_wellbeing_desc),
                fontSize = 11.sp,
                color = tokens.muted
            )
        }

        // Matriz Síntomas × Fase Estimada
        Text(
            text = stringResource(R.string.insights_symptoms_by_phase_caps),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = tokens.dim
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(17.dp))
                .background(tokens.panel)
                .border(1.dp, tokens.line, RoundedCornerShape(17.dp))
                .padding(16.dp)
        ) {
            Column {
                if (symptomCorrelations.isEmpty()) {
                    Text(
                        text = stringResource(R.string.insights_no_symptoms_to_map),
                        fontSize = 11.sp,
                        color = tokens.muted,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    // Encabezado de columnas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("M", "F", "O", "L").forEach {
                                Text(
                                    text = it,
                                    fontSize = 9.sp,
                                    color = tokens.dim,
                                    modifier = Modifier.width(27.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Filas de síntomas
                    val phases = listOf(CyclePhase.MENSTRUAL, CyclePhase.FOLLICULAR, CyclePhase.OVULATION, CyclePhase.LUTEAL)
                    symptomCorrelations.take(5).forEach { sym ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sym.symptomName,
                                fontSize = 11.sp,
                                color = tokens.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                phases.forEach { phase ->
                                    val count = sym.occurrenceByPhase[phase] ?: 0
                                    val level = when {
                                        count == 0 -> 0
                                        count in 1..2 -> 1
                                        count in 3..4 -> 2
                                        else -> 3
                                    }

                                    val cellBg = when (level) {
                                        1 -> tokens.rose.copy(alpha = 0.25f)
                                        2 -> tokens.mint.copy(alpha = 0.45f)
                                        3 -> tokens.accent.copy(alpha = 0.70f)
                                        else -> tokens.panel2
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(27.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(cellBg)
                                            .clickable { onSelectSymptomCell(sym.symptomName, phase, level, count) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (level == 3) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF724B99))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = tokens.line, thickness = 1.dp)
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = selectedSymptomNote ?: stringResource(R.string.insights_select_cell_hint),
                        fontSize = 10.sp,
                        color = tokens.muted,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Estados de Ánimo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.insights_moods_caps),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = tokens.dim
            )

            Text(
                text = if (showAllMoods) stringResource(R.string.insights_view_less) else stringResource(R.string.insights_view_all),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.accent,
                modifier = Modifier.clickable { onToggleShowAllMoods() }
            )
        }

        if (moodDistribution == null || moodDistribution.totalDays == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(17.dp))
                    .background(tokens.panel)
                    .border(1.dp, tokens.line, RoundedCornerShape(17.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.insights_not_enough_moods),
                    fontSize = 11.sp,
                    color = tokens.muted
                )
            }
        } else {
            val moodList = moodDistribution.distribution.toList()
                .sortedByDescending { it.second }
            val visibleMoods = if (showAllMoods) moodList else moodList.take(4)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleMoods.forEach { (moodName, count) ->
                    val percentage = ((count.toFloat() / moodDistribution.totalDays) * 100).roundToInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = moodName,
                            fontSize = 11.sp,
                            color = tokens.text,
                            modifier = Modifier.width(90.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.line)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(percentage / 100f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tokens.rose)
                            )
                        }

                        Text(
                            text = "$percentage%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.rose,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.insights_mood_percentage_note),
                    fontSize = 10.sp,
                    color = tokens.muted,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Temperatura Basal (BBT)
        Text(
            text = stringResource(R.string.insights_bbt_caps),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = tokens.dim,
            modifier = Modifier.padding(top = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(17.dp))
                .background(tokens.panel)
                .border(1.dp, tokens.line, RoundedCornerShape(17.dp))
                .padding(17.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.insights_thermal_curve_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Icon(
                        imageVector = Icons.Rounded.Thermostat,
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                if (!hayDatosBbt) {
                    // Estado vacío aprobado
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .border(1.dp, tokens.line, RoundedCornerShape(13.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(tokens.panel2),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Thermostat,
                                    contentDescription = null,
                                    tint = tokens.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.insights_no_bbt_measurements),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.text
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.insights_bbt_empty_desc),
                                    fontSize = 10.5.sp,
                                    color = tokens.muted,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                } else {
                    // Curva de temperatura dibujada en Canvas
                    val hasThermalShift = bbtPoints.any { it.esOvulacion }
                    if (hasThermalShift) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(tokens.accentSoft)
                                .border(1.dp, tokens.line, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.insights_possible_thermal_shift),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.accent
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val minTemp = 36.0f
                        val maxTemp = 37.5f

                        // Línea base 36.5°C
                        val yBase = height - ((36.5f - minTemp) / (maxTemp - minTemp)) * height
                        drawLine(
                            color = Color(0x40FFFFFF),
                            start = Offset(0f, yBase),
                            end = Offset(width, yBase),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )

                        if (bbtPoints.size >= 2) {
                            val sorted = bbtPoints.sortedBy { it.diaCiclo }
                            val minX = sorted.first().diaCiclo
                            val maxX = sorted.last().diaCiclo.coerceAtLeast(minX + 1)

                            val path = Path()
                            sorted.forEachIndexed { i, p ->
                                val x = ((p.diaCiclo - minX).toFloat() / (maxX - minX)) * width
                                val y = height - ((p.temperaturaC - minTemp) / (maxTemp - minTemp)) * height
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                                drawCircle(
                                    color = tokens.accent,
                                    radius = if (p.esOvulacion) 6.dp.toPx() else 4.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                            drawPath(
                                path = path,
                                color = tokens.accent,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }

                // Controles de prueba interactiva (fieles al HTML)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.insights_cycle_day_label), fontSize = 10.sp, color = tokens.muted)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = "$demoDay",
                            onValueChange = { demoDay = it.toIntOrNull()?.coerceIn(1, 90) ?: 1 },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = tokens.panel2,
                                unfocusedContainerColor = tokens.panel2,
                                focusedBorderColor = tokens.accent,
                                unfocusedBorderColor = tokens.line,
                                focusedTextColor = tokens.text,
                                unfocusedTextColor = tokens.text
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.insights_temperature_celsius_label), fontSize = 10.sp, color = tokens.muted)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = "%.1f".format(demoTemp),
                            onValueChange = { demoTemp = it.toFloatOrNull()?.coerceIn(35f, 39f) ?: 36.5f },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = tokens.panel2,
                                unfocusedContainerColor = tokens.panel2,
                                focusedBorderColor = tokens.accent,
                                unfocusedBorderColor = tokens.line,
                                focusedTextColor = tokens.text,
                                unfocusedTextColor = tokens.text
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Button(
                        onClick = { onAddDemoTemp(demoDay, demoTemp) },
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.bg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text(stringResource(R.string.btn_add), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (hayDatosBbt) {
                        Button(
                            onClick = onClearDemoTemp,
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.panel2, contentColor = tokens.muted),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text(stringResource(R.string.btn_clear), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.insights_interactive_bbt_notice),
                    fontSize = 9.5.sp,
                    color = tokens.muted,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SECCIÓN 03 / INTERPRETACIÓN: Mirar con calma
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionInterpretacion(
    bivariableInsights: List<com.jeremy.lumi.domain.model.BivariableInsight>,
    onOpenObservations: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(
                text = stringResource(R.string.insights_badge_interpretation),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = tokens.dim
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.insights_interpretation_title),
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.8).sp,
                color = tokens.text
            )
            Text(
                text = stringResource(R.string.insights_interpretation_desc),
                fontSize = 11.sp,
                color = tokens.muted
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(17.dp))
                .background(Brush.linearGradient(listOf(tokens.panel, tokens.panel2)))
                .border(1.dp, tokens.line, RoundedCornerShape(17.dp))
                .padding(20.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(37.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.insights_lumi_discoveries),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.insights_lumi_discoveries_desc),
                    fontSize = 11.sp,
                    color = tokens.muted,
                    lineHeight = 17.sp
                )

                if (bivariableInsights.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    bivariableInsights.take(2).forEach { insight ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.panel)
                                .border(1.dp, tokens.line, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(insight.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tokens.text)
                                Text(insight.message, fontSize = 10.sp, color = tokens.muted, lineHeight = 14.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.insights_view_available_obs),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.accent,
                    modifier = Modifier.clickable { onOpenObservations() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SECCIÓN 04 / ACOMPAÑAMIENTO: Cuando quieras hablarlo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionAcompanamiento(
    onOpenConsult: () -> Unit,
    onExportPdf: () -> Unit,
    isGeneratingPdf: Boolean
) {
    val tokens = LumiThemeTokens.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(
                text = stringResource(R.string.insights_badge_accompaniment),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = tokens.dim
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.insights_accompaniment_title),
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.8).sp,
                color = tokens.text
            )
            Text(
                text = stringResource(R.string.insights_accompaniment_desc),
                fontSize = 11.sp,
                color = tokens.muted
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(19.dp))
                .background(Brush.radialGradient(listOf(tokens.glow, Color.Transparent), radius = 350f))
                .background(tokens.panel)
                .border(1.dp, tokens.line, RoundedCornerShape(19.dp))
                .padding(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.panel2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolunteerActivism,
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.insights_prepare_visit_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.insights_prepare_visit_desc),
                        fontSize = 11.sp,
                        color = tokens.muted,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = onOpenConsult,
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.bg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.insights_prepare_visit_btn), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Botón Exportar PDF
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(tokens.panel)
                .border(1.dp, tokens.line, RoundedCornerShape(13.dp))
                .clickable { onExportPdf() }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = tokens.accent, strokeWidth = 2.dp)
                        Text(stringResource(R.string.insights_generating_summary), fontSize = 11.sp, color = tokens.text)
                    } else {
                        Icon(imageVector = Icons.Rounded.PictureAsPdf, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.insights_save_pdf_btn), fontSize = 11.sp, color = tokens.text)
                    }
                }
                Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = tokens.muted, modifier = Modifier.size(15.dp))
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.insights_prototype_disclaimer),
            fontSize = 9.sp,
            color = tokens.dim,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HOJAS MODALES: Observaciones e Información
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObservationsSheet(
    observations: List<LumiObservation>,
    onDismiss: () -> Unit,
    onOpenConsult: () -> Unit
) {
    val tokens = LumiThemeTokens.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = tokens.panel,
        shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.insights_observations_sheet_title),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.btn_close), tint = tokens.muted)
                }
            }

            Text(
                text = stringResource(R.string.insights_observations_sheet_disclaimer),
                fontSize = 11.sp,
                color = tokens.muted,
                lineHeight = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            if (observations.isEmpty()) {
                Text(
                    text = stringResource(R.string.insights_no_observations),
                    fontSize = 12.sp,
                    color = tokens.muted,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                observations.forEach { obs ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = obs.tag.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFFECC78F)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = obs.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.text
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = obs.description,
                            fontSize = 11.sp,
                            color = tokens.muted,
                            lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.insights_obs_limits_prefix, obs.limitsText),
                            fontSize = 10.sp,
                            color = tokens.dim,
                            lineHeight = 14.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.insights_obs_recommendation_prefix, obs.recommendationText),
                            fontSize = 10.5.sp,
                            color = tokens.muted,
                            lineHeight = 15.sp
                        )
                    }
                    HorizontalDivider(color = tokens.line, thickness = 1.dp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onOpenConsult,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.bg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.insights_prepare_questions_btn), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoExplanationSheet(onDismiss: () -> Unit) {
    val tokens = LumiThemeTokens.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = tokens.panel,
        shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.insights_how_to_read_title),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.btn_close), tint = tokens.muted)
                }
            }

            Text(
                text = stringResource(R.string.insights_how_to_read_desc),
                fontSize = 11.sp,
                color = tokens.muted,
                lineHeight = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            InfoItem(
                title = stringResource(R.string.insights_meaning_numbers_title),
                description = stringResource(R.string.insights_meaning_numbers_desc)
            )

            InfoItem(
                title = stringResource(R.string.insights_symptoms_emotions_title),
                description = stringResource(R.string.insights_symptoms_emotions_desc)
            )

            InfoItem(
                title = stringResource(R.string.insights_predictions_bbt_title),
                description = stringResource(R.string.insights_predictions_bbt_desc)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoItem(title: String, description: String) {
    val tokens = LumiThemeTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tokens.text)
        Spacer(Modifier.height(4.dp))
        Text(text = description, fontSize = 11.sp, color = tokens.muted, lineHeight = 16.sp)
    }
}

@Composable
private fun LegendRow(color: Color, text: String) {
    val tokens = LumiThemeTokens.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = text, fontSize = 9.5.sp, color = tokens.muted)
    }
}
