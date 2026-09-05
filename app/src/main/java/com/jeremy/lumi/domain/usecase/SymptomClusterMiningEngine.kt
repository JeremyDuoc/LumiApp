package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.SymptomCluster
import com.jeremy.lumi.domain.model.SymptomPhaseRisk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SymptomClusterMiningEngine @Inject constructor() {

    suspend fun computeClusters(
        logs: List<DailyLogWithSymptoms>,
        cyclePhaseFn: (Long) -> CyclePhase?
    ): List<SymptomCluster> = withContext(Dispatchers.Default) {
        if (logs.isEmpty()) return@withContext emptyList()

        val symptomDays = mutableMapOf<String, MutableSet<Long>>()
        val phaseDays = mutableMapOf<CyclePhase, Int>()
        var totalDays = 0

        for (log in logs) {
            val date = log.dailyLog.date
            val phase = cyclePhaseFn(date)
            if (phase != null && phase != CyclePhase.UNKNOWN) {
                phaseDays[phase] = phaseDays.getOrDefault(phase, 0) + 1
                totalDays++
            }

            val dailySymptoms = log.symptoms
                .map { it.name.lowercase().trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            for (symp in dailySymptoms) {
                symptomDays.getOrPut(symp) { mutableSetOf() }.add(date)
            }
        }

        val validSymptoms = symptomDays.filter { it.value.size >= 5 }.keys.toList()
        val clusters = mutableListOf<SymptomCluster>()

        for (i in validSymptoms.indices) {
            for (j in i + 1 until validSymptoms.size) {
                val sympA = validSymptoms[i]
                val sympB = validSymptoms[j]

                val daysA = symptomDays[sympA]!!
                val daysB = symptomDays[sympB]!!

                val intersection = daysA.intersect(daysB)
                val union = daysA.union(daysB)

                if (union.isEmpty()) continue

                val jaccard = intersection.size.toFloat() / union.size.toFloat()
                if (jaccard >= 0.40f) {
                    val intersectionWithPhase = intersection.filter { 
                        val p = cyclePhaseFn(it)
                        p != null && p != CyclePhase.UNKNOWN 
                    }

                    var bestPhase: CyclePhase? = null
                    var bestRR = 0f

                    if (intersectionWithPhase.isNotEmpty()) {
                        for (phase in CyclePhase.values()) {
                            if (phase == CyclePhase.UNKNOWN) continue
                            
                            val occurrencesInPhase = intersectionWithPhase.count { cyclePhaseFn(it) == phase }
                            val totalInPhase = phaseDays[phase] ?: 0

                            val occurrencesNotInPhase = intersectionWithPhase.size - occurrencesInPhase
                            val totalNotInPhase = totalDays - totalInPhase

                            val pSymptomGivenPhase = if (totalInPhase > 0) occurrencesInPhase.toFloat() / totalInPhase else 0f
                            val pSymptomGivenOther = if (totalNotInPhase > 0) occurrencesNotInPhase.toFloat() / totalNotInPhase else 0f

                            val rr = if (pSymptomGivenOther > 0f) pSymptomGivenPhase / pSymptomGivenOther else 0f

                            if (bestPhase == null || rr > bestRR) {
                                bestRR = rr
                                bestPhase = phase
                            }
                        }
                    }

                    clusters.add(
                        SymptomCluster(
                            symptomA = sympA,
                            symptomB = sympB,
                            jaccardScore = jaccard,
                            coOccurrenceDays = intersection.size,
                            dominantPhase = bestPhase,
                            relativeRiskInDominantPhase = bestRR
                        )
                    )
                }
            }
        }

        return@withContext clusters.sortedByDescending { it.jaccardScore }
    }

    suspend fun computeSymptomPhaseRisks(
        logs: List<DailyLogWithSymptoms>,
        cyclePhaseFn: (Long) -> CyclePhase?
    ): List<SymptomPhaseRisk> = withContext(Dispatchers.Default) {
        if (logs.isEmpty()) return@withContext emptyList()

        val phaseDays = mutableMapOf<CyclePhase, Int>()
        var totalDays = 0
        val symptomPhaseCounts = mutableMapOf<String, MutableMap<CyclePhase, Int>>()
        val symptomTotalOccurrences = mutableMapOf<String, Int>()

        for (log in logs) {
            val date = log.dailyLog.date
            val phase = cyclePhaseFn(date)
            
            if (phase != null && phase != CyclePhase.UNKNOWN) {
                phaseDays[phase] = phaseDays.getOrDefault(phase, 0) + 1
                totalDays++
            }

            val dailySymptoms = log.symptoms
                .map { it.name.lowercase().trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            for (symp in dailySymptoms) {
                if (phase != null && phase != CyclePhase.UNKNOWN) {
                    val counts = symptomPhaseCounts.getOrPut(symp) { mutableMapOf() }
                    counts[phase] = counts.getOrDefault(phase, 0) + 1
                    symptomTotalOccurrences[symp] = symptomTotalOccurrences.getOrDefault(symp, 0) + 1
                }
            }
        }

        val risks = mutableListOf<SymptomPhaseRisk>()
        for ((symp, counts) in symptomPhaseCounts) {
            for (phase in CyclePhase.values()) {
                if (phase == CyclePhase.UNKNOWN) continue

                val occurrencesInPhase = counts[phase] ?: 0
                val totalInPhase = phaseDays[phase] ?: 0

                val occurrencesNotInPhase = symptomTotalOccurrences.getOrDefault(symp, 0) - occurrencesInPhase
                val totalNotInPhase = totalDays - totalInPhase

                val pSymptomGivenPhase = if (totalInPhase > 0) occurrencesInPhase.toFloat() / totalInPhase else 0f
                val pSymptomGivenOther = if (totalNotInPhase > 0) occurrencesNotInPhase.toFloat() / totalNotInPhase else 0f

                // When symptom exclusively appears in this phase (pSymptomGivenOther==0 and pSymptomGivenPhase>0),
                // RR is theoretically infinite → use 99f as a sentinel high value
                val rr = when {
                    pSymptomGivenOther > 0f -> pSymptomGivenPhase / pSymptomGivenOther
                    pSymptomGivenPhase > 0f -> 99f  // exclusive to this phase
                    else -> 0f
                }

                risks.add(
                    SymptomPhaseRisk(
                        symptomName = symp,
                        phase = phase,
                        relativeRisk = rr,
                        occurrencesInPhase = occurrencesInPhase,
                        totalOccurrences = symptomTotalOccurrences.getOrDefault(symp, 0)
                    )
                )
            }
        }
        return@withContext risks
    }
}
