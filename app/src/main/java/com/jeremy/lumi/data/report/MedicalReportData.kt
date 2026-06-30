package com.jeremy.lumi.data.report

import com.jeremy.lumi.domain.model.CycleStats
import com.jeremy.lumi.domain.model.HistoricalCycleStats
import com.jeremy.lumi.domain.model.MoodDistribution
import com.jeremy.lumi.domain.model.SymptomCorrelation
import java.time.LocalDate

/**
 * Contenedor de todos los datos necesarios para generar el reporte médico en PDF.
 * Se construye en [GenerateMedicalReportUseCase] y se pasa al [MedicalReportGenerator].
 */
data class MedicalReportData(
    val userName           : String?,
    val userAge            : Int?,
    val userHeightCm       : Float?,
    val userWeightKg       : Float?,
    val isOnContraceptive  : Boolean,
    val avgCycleLength     : Int,         // from DataStore default
    val avgPeriodLength    : Int,
    val generatedAt        : LocalDate,
    val historicalStats    : HistoricalCycleStats?,
    val recentCycles       : List<CycleStats>,          // last ≤12 closed cycles
    val cycleStartDates    : List<LocalDate>,            // parallel to recentCycles
    val symptomCorrelations: List<SymptomCorrelation>,
    val moodDistribution   : MoodDistribution?,
    
    // Novedad: datos del ciclo actual (o el más reciente) para graficar BBT y síntomas.
    val latestCycleLogs      : List<com.jeremy.lumi.data.local.entity.DailyLogEntity>,
    val latestCycleStartDate : LocalDate?
)
