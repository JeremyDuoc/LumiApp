package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.domain.repository.LumiRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class SymptomPattern(
    val symptomName: String,
    val daysBeforePeriod: Int,
    val occurrences: Int
)

class SymptomCorrelationEngine @Inject constructor(
    private val repository: LumiRepository
) {

    /**
     * Analiza los últimos [monthsToAnalyze] meses de datos para encontrar patrones recurrentes.
     * Un patrón es un síntoma que ocurre al menos [minOccurrences] veces en el mismo momento del ciclo
     * (con un margen exacto o muy cercano al próximo periodo).
     */
    suspend fun findPatterns(
        monthsToAnalyze: Int = 3,
        minOccurrences: Int = 2
    ): List<SymptomPattern> {
        val closedCycles = repository.getClosedCycles()
        if (closedCycles.size < minOccurrences) return emptyList()

        // Tomar solo los ciclos de los últimos N meses
        val cutoffDate = Instant.now().atZone(ZoneOffset.UTC).toLocalDate().minusMonths(monthsToAnalyze.toLong())
        val recentCycles = closedCycles.filter {
            Instant.ofEpochMilli(it.endDate ?: it.startDate).atZone(ZoneOffset.UTC).toLocalDate().isAfter(cutoffDate)
        }

        if (recentCycles.isEmpty()) return emptyList()

        val logsList = repository.getAllLogs(descending = true).firstOrNull() ?: emptyList()
        if (logsList.isEmpty()) return emptyList()

        // Map de (NombreSintoma -> Lista de DaysBeforePeriod)
        val occurrencesMap = mutableMapOf<String, MutableList<Pair<Int, Int>>>()

        recentCycles.forEach { cycle ->
            if (cycle.endDate != null) {
                val cycleEndLocal = Instant.ofEpochMilli(cycle.endDate).atZone(ZoneOffset.UTC).toLocalDate()

                // Filtrar los logs que pertenecen a este ciclo
                val cycleLogs = logsList.filter { it.dailyLog.cycleId == cycle.id }

                cycleLogs.forEach { logWithSymptoms ->
                    val logDateLocal = Instant.ofEpochMilli(logWithSymptoms.dailyLog.date).atZone(ZoneOffset.UTC).toLocalDate()
                    val daysBefore = ChronoUnit.DAYS.between(logDateLocal, cycleEndLocal).toInt()

                    logWithSymptoms.symptoms.forEach { symptom ->
                        val name = symptom.name.lowercase().trim()
                        if (name.isNotEmpty()) {
                            occurrencesMap.putIfAbsent(name, mutableListOf())
                            // Remember the cycle ID so multiple logs in the same cycle do not count as replication.
                            if (daysBefore in 0..30) occurrencesMap[name]!!.add(cycle.id to daysBefore)
                        }
                    }
                }
            }
        }

        val patterns = mutableListOf<SymptomPattern>()

        // Require repetition in distinct cycles, not two observations from one cycle.
        for ((symptomName, observations) in occurrencesMap) {
            val candidates = observations.map { it.second }.distinct().sorted()
            for (dayBefore in candidates) {
                val supportingCycles = observations
                    .filter { kotlin.math.abs(it.second - dayBefore) <= 1 }
                    .map { it.first }
                    .distinct()
                    .size
                if (supportingCycles >= minOccurrences) {
                    patterns.add(
                        SymptomPattern(
                            symptomName = symptomName,
                            daysBeforePeriod = dayBefore,
                            occurrences = supportingCycles
                        )
                    )
                    break
                }
            }
        }

        return patterns.sortedByDescending { it.occurrences }
    }
}
