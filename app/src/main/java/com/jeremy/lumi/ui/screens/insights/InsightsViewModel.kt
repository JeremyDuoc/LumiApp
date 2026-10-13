package com.jeremy.lumi.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.closedLengthDays
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.domain.model.*
import com.jeremy.lumi.domain.repository.InsightsRepository
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.usecase.GenerateMedicalReportUseCase
import com.jeremy.lumi.domain.usecase.MedicalPatternDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

enum class InsightsSheetType {
    THEMES,
    OBSERVATIONS,
    CONSULT,
    INFO
}

data class InsightsUiState(
    val isLoading                 : Boolean                  = true,
    val insights                  : CycleInsights?           = null,
    val isOnContraceptive         : Boolean                  = false,
    val isGeneratingPdf           : Boolean                  = false,
    val pdfBytes                  : ByteArray?               = null,
    val pdfError                  : String?                  = null,
    val medicalAlerts             : List<MedicalAlert>       = emptyList(),
    val observations              : List<LumiObservation>   = emptyList(),
    val lmpDate                   : LocalDate?               = null,
    val daysSinceLmp              : Int?                     = null,
    // Selector de rango: 3, 6, 12 ciclos
    val cycleRange                : Int                      = 6,
    val availableCyclesCount      : Int                      = 0,
    val cycleBars                 : List<PuntoBarraCiclo>    = emptyList(),
    val selectedCycleIndex        : Int?                     = null,
    // Bienestar & interactividad
    val showAllMoods              : Boolean                  = false,
    val isPhaseDistributionExpanded : Boolean                = false,
    val selectedSymptomNote       : String?                  = null,
    val bbtPoints                 : List<PuntoBbt>           = emptyList(),
    val demoBbtPoints             : List<PuntoBbt>           = emptyList(),
    val hayDatosBbt               : Boolean                  = false,
    // Hoja activa
    val activeSheet               : InsightsSheetType?       = null
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insightsRepo          : InsightsRepository,
    private val prefsManager          : OnboardingPreferenceManager,
    private val generateReportUseCase : GenerateMedicalReportUseCase,
    private val lumiRepository        : LumiRepository,
    private val medicalPatternDetector: MedicalPatternDetector
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private val zona = ZoneOffset.UTC
    private val monthFormat = DateTimeFormatter.ofPattern("MMM", Locale("es"))
    private val dayMonthFormat = DateTimeFormatter.ofPattern("d MMM", Locale("es"))

    init {
        // Observación reactiva de ciclos y logs
        viewModelScope.launch {
            lumiRepository.getAllCycles().collect {
                load()
            }
        }
    }

    fun setCycleRange(range: Int) {
        if (_uiState.value.cycleRange == range) return
        _uiState.update { it.copy(cycleRange = range, selectedCycleIndex = null) }
        load()
    }

    fun selectCycleBar(index: Int?) {
        _uiState.update { it.copy(selectedCycleIndex = index) }
    }

    fun toggleShowAllMoods() {
        _uiState.update { it.copy(showAllMoods = !it.showAllMoods) }
    }

    fun togglePhaseDistribution() {
        _uiState.update { it.copy(isPhaseDistributionExpanded = !it.isPhaseDistributionExpanded) }
    }

    fun selectSymptomCell(symptomName: String, phase: CyclePhase, level: Int, count: Int) {
        val phaseName = when (phase) {
            CyclePhase.MENSTRUAL -> "Fase menstrual"
            CyclePhase.FOLLICULAR -> "Fase folicular"
            CyclePhase.OVULATION -> "Ventana ovulatoria (est.)"
            CyclePhase.LUTEAL -> "Fase lútea"
            else -> "Fase estimada"
        }
        val note = if (level > 0) {
            "$symptomName · $phaseName. Nivel $level/3 ($count días observados); no es una probabilidad ni una medida clínica."
        } else {
            "$symptomName · $phaseName. Sin registros en esta celda. No significa ausencia del síntoma."
        }
        _uiState.update { it.copy(selectedSymptomNote = note) }
    }

    fun openSheet(type: InsightsSheetType) {
        _uiState.update { it.copy(activeSheet = type) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(activeSheet = null) }
    }

    fun addDemoTemperature(day: Int, temp: Float) {
        val newPoint = PuntoBbt(
            diaCiclo = day,
            temperaturaC = temp,
            fecha = "Día $day"
        )
        val currentDemo = _uiState.value.demoBbtPoints.filter { it.diaCiclo != day } + newPoint
        _uiState.update { state ->
            val merged = (state.bbtPoints + currentDemo).distinctBy { it.diaCiclo }
            val processed = detectarCambioTermico(merged)
            state.copy(
                demoBbtPoints = currentDemo,
                bbtPoints = processed,
                hayDatosBbt = processed.isNotEmpty()
            )
        }
    }

    fun clearDemoTemperatures() {
        _uiState.update { state ->
            state.copy(
                demoBbtPoints = emptyList()
            )
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentRange = _uiState.value.cycleRange
            val insights = insightsRepo.getInsights(cyclesLimit = currentRange)
            val isOnContraceptive = prefsManager.isOnContraceptiveFlow.first()

            val allCycles = lumiRepository.getAllCycles().first()
            val closedCycles = allCycles.filter { it.endDate != null }
            val activeCycle = allCycles.firstOrNull { it.endDate == null }
            val mostRecentCycle = activeCycle ?: allCycles.maxByOrNull { it.startDate }

            val lmpDate = mostRecentCycle?.let {
                Instant.ofEpochMilli(it.startDate).atZone(zona).toLocalDate()
            }
            val daysSinceLmp = lmpDate?.let {
                ChronoUnit.DAYS.between(it, com.jeremy.lumi.utils.LumiClock.today()).toInt().coerceAtLeast(0)
            }

            // Construir barras de historial para los ciclos del rango
            val cyclesToDisplay = closedCycles.take(currentRange).reversed()
            val bars = cyclesToDisplay.mapIndexed { index, cycle ->
                val duration = cycle.closedLengthDays()?.coerceIn(1, 90) ?: cycle.cycleLength
                val type = when {
                    duration < 24 -> CycleType.SHORT
                    duration > 35 -> CycleType.LONG
                    else          -> CycleType.NORMAL
                }
                val startDay = Instant.ofEpochMilli(cycle.startDate).atZone(zona).toLocalDate()
                val monthLabel = startDay.format(monthFormat).uppercase().replace(".", "")
                PuntoBarraCiclo(
                    etiqueta = monthLabel,
                    duracionDias = duration,
                    tipoCiclo = type,
                    fechaInicio = startDay.format(dayMonthFormat).replaceFirstChar { it.uppercase() }
                )
            }

            // Detección de patrones médicos
            val logsWithSymptoms = lumiRepository.getAllLogs().first()
            val logs = logsWithSymptoms.map { it.dailyLog }
            val alerts = withContext(Dispatchers.Default) {
                medicalPatternDetector.analyzePatterns(closedCycles, logs)
            }

            // BBT real desde logs
            val targetCycleForBbt = activeCycle ?: closedCycles.firstOrNull()
            val bbtFromLogs = if (targetCycleForBbt != null) {
                val cycleStart = Instant.ofEpochMilli(targetCycleForBbt.startDate).atZone(zona).toLocalDate()
                logs.filter { it.basalBodyTemp != null && it.basalBodyTemp > 35f }
                    .map { log ->
                        val logDate = Instant.ofEpochMilli(log.date).atZone(zona).toLocalDate()
                        val dayInCycle = ChronoUnit.DAYS.between(cycleStart, logDate).toInt() + 1
                        PuntoBbt(
                            diaCiclo = dayInCycle.coerceAtLeast(1),
                            temperaturaC = log.basalBodyTemp!!,
                            fecha = logDate.format(dayMonthFormat)
                        )
                    }
            } else emptyList()

            val combinedBbt = (_uiState.value.demoBbtPoints + bbtFromLogs).distinctBy { it.diaCiclo }
            val processedBbt = detectarCambioTermico(combinedBbt)

            // Construcción estructurada de observaciones clínicas prudentes
            val observationsList = buildObservations(
                stats = insights.historicalStats,
                alerts = alerts,
                recentCycles = insights.recentCycleStats,
                cycleCount = bars.size
            )

            val result = if (insights.historicalStats == null &&
                insights.symptomCorrelations.isEmpty() &&
                insights.moodDistribution?.totalDays == 0) null
            else insights

            val initialSelected = if (bars.isNotEmpty()) {
                val prev = _uiState.value.selectedCycleIndex
                if (prev != null && prev in bars.indices) prev else bars.size - 1
            } else null

            _uiState.update {
                it.copy(
                    isLoading = false,
                    insights = result,
                    isOnContraceptive = isOnContraceptive,
                    medicalAlerts = alerts,
                    observations = observationsList,
                    lmpDate = lmpDate,
                    daysSinceLmp = daysSinceLmp,
                    availableCyclesCount = closedCycles.size,
                    cycleBars = bars,
                    selectedCycleIndex = initialSelected,
                    bbtPoints = processedBbt,
                    hayDatosBbt = processedBbt.isNotEmpty()
                )
            }
        }
    }

    private fun detectarCambioTermico(puntos: List<PuntoBbt>): List<PuntoBbt> {
        val puntosOrdenados = puntos.sortedBy { it.diaCiclo }
        if (puntosOrdenados.size < 4) return puntosOrdenados

        val umbralSubida = 0.18f
        var indiceCambio = -1

        for (i in 1 until puntosOrdenados.size - 1) {
            val prevTemp = puntosOrdenados[i - 1].temperaturaC
            val currTemp = puntosOrdenados[i].temperaturaC
            val nextTemp = if (i + 1 < puntosOrdenados.size) puntosOrdenados[i + 1].temperaturaC else currTemp

            if (currTemp < prevTemp && nextTemp - currTemp >= umbralSubida) {
                indiceCambio = i + 1
                break
            }
        }

        return puntosOrdenados.mapIndexed { indice, punto ->
            punto.copy(esOvulacion = indice == indiceCambio)
        }
    }

    private fun buildObservations(
        stats: HistoricalCycleStats?,
        alerts: List<MedicalAlert>,
        recentCycles: List<CycleStats>,
        cycleCount: Int
    ): List<LumiObservation> {
        val result = mutableListOf<LumiObservation>()

        if (stats != null) {
            val variation = stats.longestCycle - stats.shortestCycle
            if (variation > 9) {
                result.add(
                    LumiObservation(
                        id = "obs_variation",
                        tag = "Duración",
                        title = "Variación entre ciclos",
                        description = "En los $cycleCount ciclos analizados, la duración va de ${stats.shortestCycle} a ${stats.longestCycle} días. Esto indica una diferencia entre observaciones, pero no identifica una enfermedad ni establece por qué cambian.",
                        sampleText = "Basado en $cycleCount ciclos cerrados",
                        limitsText = "La variación, por sí sola, no permite establecer una causa médica ni predecir anomalías.",
                        recommendationText = "Si la variación se mantiene o supera los 45 días repetidamente, es aconsejable conversarlo en tu próxima visita ginecológica.",
                        severity = AlertSeverity.WARNING
                    )
                )
            } else {
                result.add(
                    LumiObservation(
                        id = "obs_regularity",
                        tag = "Duración",
                        title = "Ritmo constante en tus ciclos",
                        description = "Tus registros muestran una variación moderada de $variation días entre tu ciclo más corto (${stats.shortestCycle} d) y el más largo (${stats.longestCycle} d).",
                        sampleText = "Basado en $cycleCount ciclos analizados",
                        limitsText = "La regularidad matemática no confirma ovulación en todos los periodos.",
                        recommendationText = "Continúa registrando para mantener la precisión de tus predicciones.",
                        severity = AlertSeverity.WARNING
                    )
                )
            }
        }

        alerts.forEach { alert ->
            when (alert.type) {
                MedicalAlertType.ENDOMETRIOSIS_SUSPICION -> {
                    result.add(
                        LumiObservation(
                            id = "obs_endo",
                            tag = "Síntomas",
                            title = "Dolor menstrual recurrente registrado",
                            description = "En tus registros figura una frecuencia reiterada de dolor moderado a intenso asociado a tu periodo.",
                            sampleText = "Basado en registros de dolor en el diario",
                            limitsText = "No constituye un diagnóstico de endometriosis ni de dismenorrea secundaria.",
                            recommendationText = "Si el dolor no cede con analgésicos o dificulta tus tareas cotidianas, es muy recomendable conversarlo con un profesional de la salud.",
                            severity = AlertSeverity.CRITICAL
                        )
                    )
                }
                MedicalAlertType.PCOS_SUSPICION, MedicalAlertType.SEVERE_IRREGULARITY -> {
                    result.add(
                        LumiObservation(
                            id = "obs_pcos",
                            tag = "Ritmo",
                            title = "Ciclos con duración prolongada o variable",
                            description = "Se observan ciclos con duración superior al promedio habitual de la población.",
                            sampleText = "Basado en las fechas registradas de tus ciclos",
                            limitsText = "No diagnostica síndrome de ovario poliquístico ni alteraciones hormonales.",
                            recommendationText = "Un profesional puede solicitar análisis de laboratorio si deseas profundizar en tu patrón ovulatorio.",
                            severity = AlertSeverity.WARNING
                        )
                    )
                }
                MedicalAlertType.LUTEAL_PHASE_DEFECT -> {
                    result.add(
                        LumiObservation(
                            id = "obs_luteal",
                            tag = "Fase lútea",
                            title = "Fase lútea de duración corta observada",
                            description = "El intervalo estimado entre la posible ovulación y la menstruación se presenta menor a 10 días en los registros.",
                            sampleText = "Basado en estimaciones de fase y registros",
                            limitsText = "Las estimaciones de fase son aproximadas y no reemplazan mediciones hormonales en sangre.",
                            recommendationText = "Conversa con tu especialista si buscas concebir o si observas sangrado intermenstrual.",
                            severity = AlertSeverity.WARNING
                        )
                    )
                }
                else -> {
                    result.add(
                        LumiObservation(
                            id = "obs_${alert.type.name}",
                            tag = "Bienestar",
                            title = "Patrón identificado en registros",
                            description = "Tus registros indican un patrón que conviene revisar con tranquilidad.",
                            sampleText = "Basado en observaciones registradas",
                            limitsText = "Lumi genera observaciones basadas en datos; no realiza diagnósticos clínicos.",
                            recommendationText = "Comparte este resumen en tu próxima consulta médica.",
                            severity = alert.severity
                        )
                    )
                }
            }
        }

        return result
    }

    fun generateMedicalReport() {
        if (_uiState.value.isGeneratingPdf) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPdf = true, pdfError = null) }
            try {
                val bytes = withContext(Dispatchers.IO) {
                    generateReportUseCase()
                }
                _uiState.update { it.copy(isGeneratingPdf = false, pdfBytes = bytes) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGeneratingPdf = false,
                        pdfError = "No se pudo generar el reporte: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearPdfBytes() {
        _uiState.update { it.copy(pdfBytes = null) }
    }

    fun clearPdfError() {
        _uiState.update { it.copy(pdfError = null) }
    }

    fun dismissAlert(alert: MedicalAlert) {
        _uiState.update { state ->
            state.copy(medicalAlerts = state.medicalAlerts.filter { it != alert })
        }
    }
}

