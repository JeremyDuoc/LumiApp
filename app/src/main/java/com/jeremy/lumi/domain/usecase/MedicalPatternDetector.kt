package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.domain.model.AlertSeverity
import com.jeremy.lumi.domain.model.MedicalAlert
import com.jeremy.lumi.domain.model.MedicalAlertType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class MedicalPatternDetector @Inject constructor() {

    companion object {
        // Umbrales clínicos heurísticos para SOP
        private const val MIN_CYCLES_FOR_PCOS_CHECK = 3
        private const val PCOS_HIGH_VARIANCE_THRESHOLD = 8.0 // días
        private const val PCOS_LONG_CYCLE_THRESHOLD = 35 // días
        
        // Umbrales clínicos heurísticos para Endometriosis
        private const val MIN_PAIN_THRESHOLD = 7
        private const val ENDO_MIN_DAYS_HIGH_PAIN_PER_CYCLE = 2
        private const val ENDO_MIN_CYCLES_WITH_PAIN = 2
    }

    /**
     * Analiza el historial local de la usuaria y devuelve una lista de alertas si encuentra
     * patrones anómalos. La ejecución debe hacerse en un dispatcher de background.
     */
    fun analyzePatterns(
        closedCycles: List<CycleEntity>,
        logs: List<DailyLogEntity>,
        logsWithSymptoms: List<com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms> = emptyList(),
        userAge: Int? = null,
        observedLutealLengths: List<Int> = emptyList()
    ): List<MedicalAlert> {
        val alerts = mutableListOf<MedicalAlert>()

        // 1. Detección de patrones de SOP (Irregularidad)
        val pcosAlert = checkPcosPatterns(closedCycles)
        if (pcosAlert != null) {
            alerts.add(pcosAlert)
        }

        // 2. Detección de patrones de Endometriosis (Dolor Severo)
        val endoAlert = checkEndometriosisPatterns(logs, closedCycles)
        if (endoAlert != null) {
            alerts.add(endoAlert)
        }

        val pmddAlert = checkPmddPatterns(closedCycles, logsWithSymptoms)
        if (pmddAlert != null) alerts.add(pmddAlert)

        val periAlert = checkPerimenopausalTransition(closedCycles, logsWithSymptoms, userAge)
        if (periAlert != null) alerts.add(periAlert)

        val lpdAlert = checkLutealPhaseDefect(observedLutealLengths)
        if (lpdAlert != null) alerts.add(lpdAlert)

        return alerts
    }

    private fun checkPcosPatterns(cycles: List<CycleEntity>): MedicalAlert? {
        if (cycles.size < MIN_CYCLES_FOR_PCOS_CHECK) return null

        // Consideramos solo los ciclos cerrados más recientes para mayor relevancia
        val recentCycles = cycles.sortedByDescending { it.startDate }.take(6)
        
        // FIX CLÍNICO: Calcular la duración BIOLÓGICA real en días entre startDate y endDate,
        // no el valor estático por defecto cycle.cycleLength (que casi siempre es 28).
        val durations = recentCycles.mapNotNull { cycle ->
            if (cycle.endDate != null) {
                val start = java.time.Instant.ofEpochMilli(cycle.startDate)
                    .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                val end = java.time.Instant.ofEpochMilli(cycle.endDate)
                    .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                val d = java.time.temporal.ChronoUnit.DAYS.between(start, end).toDouble()
                if (d in 15.0..120.0) d else null
            } else null
        }

        if (durations.size < MIN_CYCLES_FOR_PCOS_CHECK) return null

        val avgLength = durations.average()
        
        // Calcular la desviación estándar (varianza)
        val variance = durations.map { (it - avgLength).pow(2) }.average()
        val standardDeviation = sqrt(variance)

        // Comprobar condiciones
        val hasHighVariance = standardDeviation >= PCOS_HIGH_VARIANCE_THRESHOLD
        val hasConsistentlyLongCycles = avgLength >= PCOS_LONG_CYCLE_THRESHOLD

        if (hasHighVariance || hasConsistentlyLongCycles) {
            return MedicalAlert(
                type = MedicalAlertType.PCOS_SUSPICION,
                titleRes = R.string.alert_pcos_title,
                descriptionRes = R.string.alert_pcos_desc,
                severity = if (hasHighVariance && hasConsistentlyLongCycles) AlertSeverity.CRITICAL else AlertSeverity.WARNING
            )
        }
        return null
    }

    private fun checkEndometriosisPatterns(
        logs: List<DailyLogEntity>,
        cycles: List<CycleEntity>
    ): MedicalAlert? {
        if (cycles.size < ENDO_MIN_CYCLES_WITH_PAIN || logs.isEmpty()) return null

        // Agrupar los logs por cicloId
        val logsByCycle = logs.groupBy { it.cycleId }
        
        var cyclesWithSeverePain = 0

        for (cycle in cycles) {
            val cycleLogs = logsByCycle[cycle.id] ?: emptyList()
            
            // Contar cuántos días en este ciclo el dolor fue >= 7
            val severePainDays = cycleLogs.count { (it.painLevel ?: 0) >= MIN_PAIN_THRESHOLD }
            
            if (severePainDays >= ENDO_MIN_DAYS_HIGH_PAIN_PER_CYCLE) {
                cyclesWithSeverePain++
            }
        }

        if (cyclesWithSeverePain >= ENDO_MIN_CYCLES_WITH_PAIN) {
            return MedicalAlert(
                type = MedicalAlertType.ENDOMETRIOSIS_SUSPICION,
                titleRes = R.string.alert_endo_title,
                descriptionRes = R.string.alert_endo_desc,
                severity = AlertSeverity.CRITICAL
            )
        }
        return null
    }

    private fun checkPmddPatterns(
        cycles: List<CycleEntity>,
        logsWithSymptoms: List<com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms>
    ): MedicalAlert? {
        val closedCycles = cycles.filter { it.endDate != null }
        if (closedCycles.size < 2) return null

        val negativeMoods = listOf("ansiosa", "irritable", "triste", "abrumada")
        var cyclesWithPmddPattern = 0

        for (cycle in closedCycles) {
            val endDate = java.time.Instant.ofEpochMilli(cycle.endDate!!).atZone(java.time.ZoneOffset.UTC).toLocalDate()
            val premenstrualStart = endDate.minusDays(4)

            var premenstrualNegativeMoodDays = 0
            var premenstrualLoggedMoodDays = 0
            
            var postStartNegativeMoodDays = 0
            var postStartLoggedMoodDays = 0

            for (logWithSymptoms in logsWithSymptoms) {
                val logDate = java.time.Instant.ofEpochMilli(logWithSymptoms.dailyLog.date).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                
                if (!logDate.isBefore(premenstrualStart) && !logDate.isAfter(endDate)) {
                    val mood = logWithSymptoms.dailyLog.mood?.lowercase()
                    if (mood != null) {
                        premenstrualLoggedMoodDays++
                        if (negativeMoods.any { mood.contains(it) }) {
                            premenstrualNegativeMoodDays++
                        }
                    }
                }
                
                if (logDate == endDate.plusDays(1) || logDate == endDate.plusDays(2)) {
                    val mood = logWithSymptoms.dailyLog.mood?.lowercase()
                    if (mood != null) {
                        postStartLoggedMoodDays++
                        if (negativeMoods.any { mood.contains(it) }) {
                            postStartNegativeMoodDays++
                        }
                    }
                }
            }

            val premenstrualNegativeRate = premenstrualNegativeMoodDays.toFloat() / maxOf(premenstrualLoggedMoodDays, 1)
            val menstrualNegativeRate = postStartNegativeMoodDays.toFloat() / maxOf(postStartLoggedMoodDays, 1)

            if (premenstrualNegativeRate > 0.60f && menstrualNegativeRate < 0.30f) {
                cyclesWithPmddPattern++
            }
        }

        if (cyclesWithPmddPattern >= 2) {
            return MedicalAlert(
                type = MedicalAlertType.PMDD_SUSPICION,
                titleRes = R.string.alert_pmdd_title,
                descriptionRes = R.string.alert_pmdd_desc,
                severity = AlertSeverity.WARNING
            )
        }
        return null
    }

    private fun checkPerimenopausalTransition(
        cycles: List<CycleEntity>,
        logsWithSymptoms: List<com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms>,
        userAge: Int?
    ): MedicalAlert? {
        if (userAge == null || userAge < 40) return null

        val recentCycles = cycles.filter { it.endDate != null }.sortedBy { it.startDate }.takeLast(6)
        val differences = mutableListOf<Int>()
        
        for (i in 0 until recentCycles.size - 1) {
            val d1 = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.Instant.ofEpochMilli(recentCycles[i].startDate).atZone(java.time.ZoneOffset.UTC).toLocalDate(),
                java.time.Instant.ofEpochMilli(recentCycles[i].endDate!!).atZone(java.time.ZoneOffset.UTC).toLocalDate()
            ).toInt()
            val d2 = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.Instant.ofEpochMilli(recentCycles[i + 1].startDate).atZone(java.time.ZoneOffset.UTC).toLocalDate(),
                java.time.Instant.ofEpochMilli(recentCycles[i + 1].endDate!!).atZone(java.time.ZoneOffset.UTC).toLocalDate()
            ).toInt()
            differences.add(kotlin.math.abs(d2 - d1))
        }

        val largeDiffCount = differences.takeLast(5).count { it >= 7 }

        if (largeDiffCount >= 3) {
            // Use last cycle end date as reference for recency — makes the detector dataset-relative
            // and independent of wall-clock time (testable and clinically sound)
            val lastCycleEndMs = recentCycles.last().endDate ?: System.currentTimeMillis()
            val recentThreshold = lastCycleEndMs - (60L * 24 * 60 * 60 * 1000) // 60 days before last cycle end
            val potentiatingSymptoms = listOf("sofoco", "bochorno", "hot_flash", "insomnio", "insomnia")
            var hasPotentiatingSymptoms = false

            for (log in logsWithSymptoms) {
                if (log.dailyLog.date >= recentThreshold) {
                    if (log.symptoms.any { symptom ->
                        potentiatingSymptoms.any { symptom.name.lowercase().contains(it) }
                    }) {
                        hasPotentiatingSymptoms = true
                        break
                    }
                }
            }

            return MedicalAlert(
                type = MedicalAlertType.PERIMENOPAUSE_TRANSITION,
                titleRes = R.string.alert_perimenopause_title,
                descriptionRes = R.string.alert_perimenopause_desc,
                severity = if (hasPotentiatingSymptoms) AlertSeverity.CRITICAL else AlertSeverity.WARNING
            )
        }
        return null
    }

    private fun checkLutealPhaseDefect(observedLutealLengths: List<Int>): MedicalAlert? {
        val validEntries = observedLutealLengths.filter { it in 4..18 }
        if (validEntries.size < 2) return null

        val average = validEntries.average()
        if (average < 10.0) {
            return MedicalAlert(
                type = MedicalAlertType.LUTEAL_PHASE_DEFECT,
                titleRes = R.string.alert_lpd_title,
                descriptionRes = R.string.alert_lpd_desc,
                severity = AlertSeverity.WARNING
            )
        }
        return null
    }
}
