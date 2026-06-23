package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.domain.repository.LumiRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.ZoneId
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
        val cutoffDate = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().minusMonths(monthsToAnalyze.toLong())
        val recentCycles = closedCycles.filter {
            Instant.ofEpochMilli(it.endDate ?: it.startDate).atZone(ZoneId.systemDefault()).toLocalDate().isAfter(cutoffDate)
        }

        if (recentCycles.isEmpty()) return emptyList()

        val logsList = repository.getAllLogs(descending = true).firstOrNull() ?: emptyList()
        if (logsList.isEmpty()) return emptyList()

        // Map de (NombreSintoma -> Lista de DaysBeforePeriod)
        val occurrencesMap = mutableMapOf<String, MutableList<Int>>()

        recentCycles.forEach { cycle ->
            if (cycle.endDate != null) {
                val cycleEndLocal = Instant.ofEpochMilli(cycle.endDate).atZone(ZoneId.systemDefault()).toLocalDate()

                // Filtrar los logs que pertenecen a este ciclo
                val cycleLogs = logsList.filter { it.log.cycleId == cycle.id }

                cycleLogs.forEach { logWithSymptoms ->
                    val logDateLocal = Instant.ofEpochMilli(logWithSymptoms.log.date).atZone(ZoneId.systemDefault()).toLocalDate()
                    val daysBefore = ChronoUnit.DAYS.between(logDateLocal, cycleEndLocal).toInt()

                    logWithSymptoms.symptoms.forEach { symptom ->
                        val name = symptom.name.lowercase().trim()
                        if (name.isNotEmpty()) {
                            occurrencesMap.putIfAbsent(name, mutableListOf())
                            occurrencesMap[name]!!.add(daysBefore)
                        }
                    }
                }
            }
        }

        val patterns = mutableListOf<SymptomPattern>()

        // Buscar concentraciones
        // Si un síntoma aparece minOccurrences veces dentro de un margen de +/- 1 día antes de la regla
        for ((symptomName, daysList) in occurrencesMap) {
            if (daysList.size >= minOccurrences) {
                // Agrupar por días aproximados (+/- 1 día)
                val groupedDays = daysList.groupBy { it }
                for ((dayBefore, count) in groupedDays) {
                    val totalOccurrences = daysList.count { it in (dayBefore - 1)..(dayBefore + 1) }
                    if (totalOccurrences >= minOccurrences) {
                        patterns.add(
                            SymptomPattern(
                                symptomName = symptomName,
                                daysBeforePeriod = dayBefore,
                                occurrences = totalOccurrences
                            )
                        )
                        break // Encontramos un patrón para este síntoma, pasamos al siguiente
                    }
                }
            }
        }

        return patterns.sortedByDescending { it.occurrences }
    }
}
