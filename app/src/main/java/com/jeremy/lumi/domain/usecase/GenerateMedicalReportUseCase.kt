package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.data.report.MedicalReportData
import com.jeremy.lumi.data.report.MedicalReportGenerator
import com.jeremy.lumi.domain.repository.InsightsRepository
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.data.local.dao.LumiDao
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Recopila todos los datos necesarios para el reporte médico desde los distintos
 * repositorios y preferencias, los empaqueta en [MedicalReportData] y delega
 * la generación del PDF a [MedicalReportGenerator].
 *
 * Es un Use Case puro: no tiene estado propio y puede invocarse desde cualquier ViewModel.
 */
class GenerateMedicalReportUseCase @Inject constructor(
    private val insightsRepo : InsightsRepository,
    private val lumiRepo     : LumiRepository,
    private val lumiDao      : LumiDao,
    private val prefs        : OnboardingPreferenceManager,
    private val generator    : MedicalReportGenerator
) {
    /**
     * Genera el reporte y devuelve los bytes del PDF.
     * Lanza excepciones si alguna fuente de datos falla — el ViewModel debe manejarlas.
     */
    suspend operator fun invoke(): ByteArray {
        // ── Leer datos en paralelo (ambas suspending) ────────────────────────
        val insights     = insightsRepo.getInsights(cyclesLimit = 12)
        val closedCycles = lumiRepo.getClosedCycles()

        // ── Fechas de inicio de cada ciclo cerrado (para la tabla) ────────────
        val zone        = ZoneId.systemDefault()
        val startDates  = closedCycles
            .sortedByDescending { it.startDate }
            .take(12)
            .map { Instant.ofEpochMilli(it.startDate).atZone(zone).toLocalDate() }

        // ── Perfil de la usuaria desde DataStore ──────────────────────────────
        val userName          = prefs.userNameFlow.first()
        val userAge           = prefs.ageFlow.first()
        val userHeight        = prefs.heightFlow.first()
        val userWeight        = prefs.weightFlow.first()
        val isOnContraceptive = prefs.isOnContraceptiveFlow.first()
        val defaultCycleLen   = prefs.cycleLengthFlow.first()
        val defaultPeriodLen  = prefs.periodLengthFlow.first()

        val data = MedicalReportData(
            userName            = userName,
            userAge             = userAge,
            userHeightCm        = userHeight,
            userWeightKg        = userWeight,
            isOnContraceptive   = isOnContraceptive,
            avgCycleLength      = insights.historicalStats?.avgCycleLength?.toInt()
                                  ?: defaultCycleLen,
            avgPeriodLength     = insights.historicalStats?.avgPeriodLength?.toInt()
                                  ?: defaultPeriodLen,
            generatedAt         = LocalDate.now(),
            historicalStats     = insights.historicalStats,
            recentCycles        = insights.recentCycleStats,
            cycleStartDates     = startDates,
            symptomCorrelations = insights.symptomCorrelations,
            moodDistribution    = insights.moodDistribution,
            
            // Buscar logs del ciclo activo o el último cerrado
            latestCycleLogs      = getLatestCycleLogs(),
            latestCycleStartDate = getLatestCycleStartDate()
        )

        return generator.generate(data)
    }
    
    private suspend fun getLatestCycleLogs(): List<com.jeremy.lumi.data.local.entity.DailyLogEntity> {
        val active = lumiDao.getCurrentActiveCycle()
        if (active != null) return lumiDao.getLogsDelCiclo(active.id)
        
        val lastClosed = lumiDao.getClosedCycles().firstOrNull()
        if (lastClosed != null) return lumiDao.getLogsDelCiclo(lastClosed.id)
        
        return emptyList()
    }
    
    private suspend fun getLatestCycleStartDate(): LocalDate? {
        val active = lumiDao.getCurrentActiveCycle() ?: lumiDao.getClosedCycles().firstOrNull()
        return active?.let { 
            Instant.ofEpochMilli(it.startDate).atZone(ZoneId.systemDefault()).toLocalDate() 
        }
    }
}
