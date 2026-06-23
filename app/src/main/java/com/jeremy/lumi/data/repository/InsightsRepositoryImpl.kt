package com.jeremy.lumi.data.repository

import com.jeremy.lumi.data.local.dao.LumiDao
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.domain.model.*
import com.jeremy.lumi.domain.repository.InsightsRepository
import com.jeremy.lumi.domain.usecase.CyclePredictor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Implementación de [InsightsRepository].
 *
 * Aquí vive toda la matemática de agregación:
 *  - Promedios ponderados de duración de ciclo
 *  - Cruce síntoma × fase del ciclo
 *  - Distribución de humor
 *  - Cálculo de fase lútea personal y máximo retraso histórico
 *
 * Es la única clase donde se "piensa" — el resto del código solo consume
 * los resultados limpios que esta clase produce.
 */
class InsightsRepositoryImpl @Inject constructor(
    private val dao: LumiDao
) : InsightsRepository {

    // ─────────────────────────────────────────────────────────────────────────
    //  INSIGHTS RAÍZ
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun getInsights(cyclesLimit: Int): CycleInsights {
        val closedCycles = dao.getClosedCycles().take(cyclesLimit)
        val allLogs      = dao.getLogsInRange(0L, Long.MAX_VALUE)

        return CycleInsights(
            historicalStats     = computeHistoricalStats(closedCycles, allLogs),
            symptomCorrelations = computeSymptomCorrelations(allLogs, closedCycles, minOccurrences = 2),
            moodDistribution    = computeMoodDistribution(allLogs),
            recentCycleStats    = computeRecentCycleStats(closedCycles.take(6), allLogs)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HISTORIAL DE CICLOS
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun getHistoricalCycleStats(): HistoricalCycleStats? {
        val closedCycles = dao.getClosedCycles()
        val allLogs      = dao.getLogsInRange(0L, Long.MAX_VALUE)
        return computeHistoricalStats(closedCycles, allLogs)
    }

    private fun computeHistoricalStats(
        closedCycles : List<CycleEntity>,
        allLogs      : List<DailyLogWithSymptoms>
    ): HistoricalCycleStats? {
        if (closedCycles.isEmpty()) return null

        val durations = closedCycles.mapNotNull { cycle ->
            if (cycle.endDate == null) null
            else ChronoUnit.DAYS.between(
                cycle.startDate.toLocalDate(),
                cycle.endDate.toLocalDate()
            ).toInt().takeIf { it in 15..90 }
        }
        if (durations.isEmpty()) return null

        // Promedio de días de menstruación: contamos logs con flowIntensity != null
        // y que pertenecen a la fase menstrual según su fecha dentro del ciclo
        val avgPeriodLen = closedCycles.mapNotNull { cycle ->
            if (cycle.endDate == null) return@mapNotNull null
            val start = cycle.startDate.toLocalDate()
            val end   = cycle.endDate.toLocalDate()
            allLogs.count { log ->
                val logDate = log.dailyLog.date.toLocalDate()
                !logDate.isBefore(start) && !logDate.isAfter(end)
                    && log.dailyLog.flowIntensity != null
            }.toFloat().takeIf { it > 0f }
        }.average().toFloat().takeIf { !it.isNaN() } ?: 5f

        return HistoricalCycleStats(
            cycleCount           = closedCycles.size,
            avgCycleLength       = CyclePredictor.weightedAverageCycleLength(closedCycles),
            avgPeriodLength      = avgPeriodLen,
            shortestCycle        = durations.min(),
            longestCycle         = durations.max(),
            personalLutealLength = CyclePredictor.personalLutealLength(closedCycles),
            maxHistoricalDelay   = CyclePredictor.maxHistoricalDelay(closedCycles)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CORRELACIONES SÍNTOMA × FASE
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun getSymptomCorrelations(minOccurrences: Int): List<SymptomCorrelation> {
        val closedCycles = dao.getClosedCycles()
        val allLogs      = dao.getLogsInRange(0L, Long.MAX_VALUE)
        return computeSymptomCorrelations(allLogs, closedCycles, minOccurrences)
    }

    /**
     * Para cada log con síntomas:
     *   1. Determina en qué fase del ciclo cae ese día
     *   2. Suma +1 al contador (síntoma, fase)
     * Al final calcula el porcentaje y la fase dominante.
     *
     * Este es el "wow factor" de Lumi: "el 80% de tus dolores de cabeza
     * ocurren 2 días antes de tu regla".
     */
    private fun computeSymptomCorrelations(
        allLogs      : List<DailyLogWithSymptoms>,
        closedCycles : List<CycleEntity>,
        minOccurrences: Int
    ): List<SymptomCorrelation> {
        // Mapa: symptomName → Map<CyclePhase, Int>
        val counts = mutableMapOf<String, MutableMap<CyclePhase, Int>>()

        allLogs.forEach { logWithSymptoms ->
            if (logWithSymptoms.symptoms.isEmpty()) return@forEach

            val logDate = logWithSymptoms.dailyLog.date.toLocalDate()

            // Encontrar el ciclo al que pertenece este día
            val ownerCycle = closedCycles.firstOrNull { cycle ->
                val start = cycle.startDate.toLocalDate()
                val end   = cycle.endDate?.toLocalDate() ?: LocalDate.now()
                !logDate.isBefore(start) && !logDate.isAfter(end)
            } ?: return@forEach     // log sin ciclo asignado, ignorar

            val dayInCycle = CyclePredictor.dayInCycleWrapped(
                ownerCycle.startDate.toLocalDate(),
                ownerCycle.cycleLength,
                logDate
            )
            val phase = CyclePredictor.phaseForDay(
                dayInCycle,
                ownerCycle.cycleLength,
                ownerCycle.periodLength
            )

            logWithSymptoms.symptoms.forEach { symptom ->
                val phaseMap = counts.getOrPut(symptom.name) { mutableMapOf() }
                phaseMap[phase] = (phaseMap[phase] ?: 0) + 1
            }
        }

        return counts
            .map { (name, phaseMap) ->
                val total         = phaseMap.values.sum()
                val dominantPhase = phaseMap.maxByOrNull { it.value }?.key ?: CyclePhase.UNKNOWN
                SymptomCorrelation(
                    symptomName       = name,
                    dominantPhase     = dominantPhase,
                    occurrenceByPhase = phaseMap,
                    totalOccurrences  = total
                )
            }
            .filter { it.totalOccurrences >= minOccurrences }
            .sortedByDescending { it.totalOccurrences }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DISTRIBUCIÓN DE HUMOR
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun getMoodDistribution(from: Long, to: Long): MoodDistribution {
        val logs = dao.getLogsInRange(from, to)
        return computeMoodDistribution(logs)
    }

    private fun computeMoodDistribution(logs: List<DailyLogWithSymptoms>): MoodDistribution {
        val moodCount = mutableMapOf<String, Int>()
        logs.forEach { log ->
            val mood = log.dailyLog.mood ?: return@forEach
            moodCount[mood] = (moodCount[mood] ?: 0) + 1
        }
        val total        = moodCount.values.sum()
        val dominantMood = moodCount.maxByOrNull { it.value }?.key
        return MoodDistribution(
            distribution = moodCount,
            totalDays    = total,
            dominantMood = dominantMood
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STATS POR CICLO RECIENTE (para gráfico de barras)
    // ─────────────────────────────────────────────────────────────────────────

    private fun computeRecentCycleStats(
        recentCycles : List<CycleEntity>,
        allLogs      : List<DailyLogWithSymptoms>
    ): List<CycleStats> {
        return recentCycles.mapNotNull { cycle ->
            if (cycle.endDate == null) return@mapNotNull null
            val start    = cycle.startDate.toLocalDate()
            val end      = cycle.endDate.toLocalDate()
            val duration = ChronoUnit.DAYS.between(start, end).toInt().takeIf { it in 15..90 }
                ?: return@mapNotNull null

            // Días con registro de flujo durante este ciclo
            val bleedingDays = allLogs.count { log ->
                val logDate = log.dailyLog.date.toLocalDate()
                !logDate.isBefore(start) && !logDate.isAfter(end)
                    && log.dailyLog.flowIntensity != null
            }

            // Promedio de dolor en días de menstruación
            val painLogs = allLogs.filter { log ->
                val logDate = log.dailyLog.date.toLocalDate()
                !logDate.isBefore(start) && !logDate.isAfter(end)
                    && log.dailyLog.flowIntensity != null
                    && log.dailyLog.painLevel > 0
            }
            val avgPain = if (painLogs.isEmpty()) 0f
            else painLogs.map { it.dailyLog.painLevel }.average().toFloat()

            CycleStats(
                durationDays = duration,
                bleedingDays = bleedingDays,
                cycleType    = when {
                    duration < 21 -> CycleType.SHORT
                    duration > 35 -> CycleType.LONG
                    else          -> CycleType.NORMAL
                },
                avgPainLevel = avgPain
            )
        }
    }

    // ── Extension helpers ────────────────────────────────────────────────────
    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}
