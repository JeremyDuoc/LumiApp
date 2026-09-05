package com.jeremy.lumi.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.domain.model.CycleInsights
import com.jeremy.lumi.domain.repository.InsightsRepository
import com.jeremy.lumi.domain.usecase.GenerateMedicalReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.usecase.MedicalPatternDetector
import com.jeremy.lumi.domain.model.MedicalAlert
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class InsightsUiState(
    val isLoading        : Boolean        = true,
    val insights         : CycleInsights? = null,
    // FIX P2-5: Exponer el modo pastilla para que InsightsScreen
    // muestre un aviso en lugar de estadísticas de ovulación erróneas.
    val isOnContraceptive: Boolean        = false,
    // PDF report states
    val isGeneratingPdf  : Boolean        = false,
    val pdfBytes         : ByteArray?     = null,
    val pdfError         : String?        = null,
    // Medical Pattern Alerts
    val medicalAlerts    : List<MedicalAlert> = emptyList(),
    // F.U.M. clínica
    val lmpDate          : LocalDate?     = null,
    val daysSinceLmp     : Int?           = null
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insightsRepo         : InsightsRepository,
    // FIX P2-5: Inyectar prefs para leer el modo anticonceptivo.
    private val prefsManager         : OnboardingPreferenceManager,
    // PDF medical report use case
    private val generateReportUseCase: GenerateMedicalReportUseCase,
    private val lumiRepository       : LumiRepository,
    private val medicalPatternDetector: MedicalPatternDetector
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val insights = insightsRepo.getInsights()
            val isOnContraceptive = prefsManager.isOnContraceptiveFlow.first()
            // Si no hay datos suficientes (0 ciclos cerrados) devolvemos null
            val result = if (insights.historicalStats == null &&
                insights.symptomCorrelations.isEmpty() &&
                insights.moodDistribution?.totalDays == 0) null
            else insights

            // Detección de F.U.M.
            val allCycles = lumiRepository.getAllCycles().first()
            val activeCycle = allCycles.firstOrNull { it.endDate == null }
            val mostRecentCycle = activeCycle ?: allCycles.maxByOrNull { it.startDate }
            val lmpDate = mostRecentCycle?.let {
                Instant.ofEpochMilli(it.startDate).atZone(ZoneOffset.UTC).toLocalDate()
            }
            val daysSinceLmp = lmpDate?.let {
                ChronoUnit.DAYS.between(it, com.jeremy.lumi.utils.LumiClock.today()).toInt().coerceAtLeast(0)
            }

            // Ejecutar la detección de patrones médicos en segundo plano
            val closedCycles = allCycles.filter { it.endDate != null }
            val logsWithSymptoms = lumiRepository.getAllLogs().first()
            val logs = logsWithSymptoms.map { it.dailyLog }
            val alerts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                medicalPatternDetector.analyzePatterns(closedCycles, logs)
            }

            _uiState.update { 
                it.copy(
                    isLoading = false, 
                    insights = result, 
                    isOnContraceptive = isOnContraceptive,
                    medicalAlerts = alerts,
                    lmpDate = lmpDate,
                    daysSinceLmp = daysSinceLmp
                ) 
            }
        }
    }

    /**
     * Lanza la generación del PDF médico en el hilo de IO.
     * Cuando termina, [uiState.pdfBytes] contiene los bytes listos para escribir al archivo.
     * El Screen debe llamar a [clearPdfBytes] después de guardar el archivo.
     */
    fun generateMedicalReport() {
        if (_uiState.value.isGeneratingPdf) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPdf = true, pdfError = null) }
            try {
                val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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

    /** Llamar desde el Screen una vez que el archivo PDF fue escrito correctamente. */
    fun clearPdfBytes() {
        _uiState.update { it.copy(pdfBytes = null) }
    }

    /** Llamar desde el Screen para descartar el error. */
    fun clearPdfError() {
        _uiState.update { it.copy(pdfError = null) }
    }

    /** Descarta una alerta médica de la lista */
    fun dismissAlert(alert: MedicalAlert) {
        _uiState.update { state ->
            state.copy(medicalAlerts = state.medicalAlerts.filter { it != alert })
        }
    }
}
