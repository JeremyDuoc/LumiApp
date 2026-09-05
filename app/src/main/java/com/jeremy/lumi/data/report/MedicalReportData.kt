package com.jeremy.lumi.data.report

import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.domain.model.CorrelationInsight
import com.jeremy.lumi.domain.model.CycleStats
import com.jeremy.lumi.domain.model.HistoricalCycleStats
import com.jeremy.lumi.domain.model.MoodDistribution
import com.jeremy.lumi.domain.model.SymptomCluster
import com.jeremy.lumi.domain.model.SymptomCorrelation
import java.time.LocalDate

/**
 * Clasificación clínica del ciclo según los criterios de la Federación Internacional
 * de Ginecología y Obstetricia (FIGO 2018 / ACOG).
 */
data class FigoCycleClassification(
    val frequency: FigoFrequency,
    val regularity: FigoRegularity,
    val duration: FigoDuration,
    val summaryText: String
)

enum class FigoFrequency(val label: String) {
    FREQUENT("Frecuente (< 24 días)"),
    NORMAL("Normal (24 a 38 días)"),
    INFREQUENT("Infrecuente (> 38 días)")
}

enum class FigoRegularity(val label: String) {
    REGULAR("Regular (variación ≤ 9 días)"),
    IRREGULAR("Irregular (variación > 9 días)")
}

enum class FigoDuration(val label: String) {
    NORMAL("Normal (≤ 8 días)"),
    PROLONGED("Prolongado (> 8 días)")
}

/**
 * Banderas rojas y alertas ginecológicas para orientación diagnóstica en consulta.
 */
data class ClinicalRedFlag(
    val title: String,
    val description: String,
    val isSevere: Boolean = false
)

/**
 * Contenedor completo de datos para generar el reporte médico de alta fidelidad en PDF.
 * Construido en [com.jeremy.lumi.domain.usecase.GenerateMedicalReportUseCase] y consumido por [MedicalReportGenerator].
 */
data class MedicalReportData(
    val userName             : String?,
    val userAge              : Int?,
    val userHeightCm         : Float?,
    val userWeightKg         : Float?,
    val isOnContraceptive    : Boolean,
    val avgCycleLength       : Int,
    val avgPeriodLength      : Int,
    val generatedAt          : LocalDate,
    val historicalStats      : HistoricalCycleStats?,
    val recentCycles         : List<CycleStats>,
    val cycleStartDates      : List<LocalDate>,
    val symptomCorrelations  : List<SymptomCorrelation>,
    val moodDistribution     : MoodDistribution?,
    val latestCycleLogs      : List<DailyLogEntity>,
    val latestCycleStartDate : LocalDate?,

    // Datos clínicos avanzados
    val lastMenstrualPeriodDate : LocalDate? = null,
    val daysSinceLmp            : Int? = null,
    val figoClassification      : FigoCycleClassification? = null,
    val clinicalRedFlags        : List<ClinicalRedFlag> = emptyList(),
    val bioInsights             : List<CorrelationInsight> = emptyList(),
    val symptomClusters         : List<SymptomCluster> = emptyList(),
    val bbtCoverline            : Float? = null
)
