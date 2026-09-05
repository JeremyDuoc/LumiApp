package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.dao.LumiDao
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.data.report.ClinicalRedFlag
import com.jeremy.lumi.data.report.FigoCycleClassification
import com.jeremy.lumi.data.report.FigoDuration
import com.jeremy.lumi.data.report.FigoFrequency
import com.jeremy.lumi.data.report.FigoRegularity
import com.jeremy.lumi.data.report.MedicalReportData
import com.jeremy.lumi.data.report.MedicalReportGenerator
import com.jeremy.lumi.domain.repository.InsightsRepository
import com.jeremy.lumi.domain.repository.LumiRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Recopila todos los datos clínicos y biométricos para el reporte médico institucional,
 * incluyendo clasificación FIGO 2018, detección de banderas rojas ginecológicas,
 * correlaciones de Spearman, agrupaciones Jaccard y cálculo de coverline sintotérmico (WHO 3-over-6).
 */
class GenerateMedicalReportUseCase @Inject constructor(
    private val insightsRepo : InsightsRepository,
    private val lumiRepo     : LumiRepository,
    private val lumiDao      : LumiDao,
    private val prefs        : OnboardingPreferenceManager,
    private val generator    : MedicalReportGenerator,
    private val bioEngine    : AdvancedBioCorrelationMatrixEngine,
    private val clusterEngine: SymptomClusterMiningEngine
) {
    /**
     * Genera el reporte clínico de 2 páginas y devuelve los bytes del PDF.
     */
    suspend operator fun invoke(): ByteArray {
        // ── 1. Lectura de datos base y ciclos ────────────────────────────────
        val insights     = insightsRepo.getInsights(cyclesLimit = 12)
        val closedCycles = lumiRepo.getClosedCycles()
        val activeCycle  = lumiDao.getCurrentActiveCycle()
        val allCycles    = closedCycles + listOfNotNull(activeCycle)

        val zone = ZoneOffset.UTC
        val startDates = closedCycles
            .sortedByDescending { it.startDate }
            .take(12)
            .map { Instant.ofEpochMilli(it.startDate).atZone(zone).toLocalDate() }

        // ── 2. Perfil de la usuaria desde preferencias ───────────────────────
        val userName          = prefs.userNameFlow.first()
        val userAge           = prefs.ageFlow.first()
        val userHeight        = prefs.heightFlow.first()
        val userWeight        = prefs.weightFlow.first()
        val isOnContraceptive = prefs.isOnContraceptiveFlow.first()
        val defaultCycleLen   = prefs.cycleLengthFlow.first()
        val defaultPeriodLen  = prefs.periodLengthFlow.first()

        val avgCycleLength = insights.historicalStats?.avgCycleLength?.toInt() ?: defaultCycleLen
        val avgPeriodLength = insights.historicalStats?.avgPeriodLength?.toInt() ?: defaultPeriodLen

        // ── 3. Fecha de Última Menstruación (FUM / LMP) ──────────────────────
        val lmpDate: LocalDate? = activeCycle?.let {
            Instant.ofEpochMilli(it.startDate).atZone(zone).toLocalDate()
        } ?: closedCycles.maxByOrNull { it.startDate }?.let {
            Instant.ofEpochMilli(it.startDate).atZone(zone).toLocalDate()
        }
        val daysSinceLmp: Int? = lmpDate?.let {
            ChronoUnit.DAYS.between(it, LocalDate.now()).toInt().coerceAtLeast(0)
        }

        // ── 4. Clasificación FIGO 2018 / ACOG ────────────────────────────────
        val figoFrequency = when {
            avgCycleLength < 24 -> FigoFrequency.FREQUENT
            avgCycleLength <= 38 -> FigoFrequency.NORMAL
            else -> FigoFrequency.INFREQUENT
        }

        val cycleLengths = insights.recentCycleStats.map { it.durationDays }
        val cycleVariation = if (cycleLengths.size >= 2) {
            (cycleLengths.maxOrNull() ?: avgCycleLength) - (cycleLengths.minOrNull() ?: avgCycleLength)
        } else {
            insights.historicalStats?.let { it.longestCycle - it.shortestCycle } ?: 0
        }

        val figoRegularity = if (cycleVariation <= 9) FigoRegularity.REGULAR else FigoRegularity.IRREGULAR
        val figoDuration = if (avgPeriodLength <= 8) FigoDuration.NORMAL else FigoDuration.PROLONGED

        val figoSummary = buildString {
            append("Ciclo de frecuencia ")
            append(when (figoFrequency) {
                FigoFrequency.NORMAL -> "normal (24 a 38 días)"
                FigoFrequency.FREQUENT -> "frecuente (< 24 días)"
                FigoFrequency.INFREQUENT -> "infrecuente (> 38 días)"
            })
            append(", ")
            append(when (figoRegularity) {
                FigoRegularity.REGULAR -> "regular (variación de ${cycleVariation}d ≤ 9d)"
                FigoRegularity.IRREGULAR -> "irregular (variación de ${cycleVariation}d > 9d)"
            })
            append(" y duración de sangrado ")
            append(when (figoDuration) {
                FigoDuration.NORMAL -> "normal (≤ 8 días)."
                FigoDuration.PROLONGED -> "prolongado (> 8 días)."
            })
        }

        val figoClassification = FigoCycleClassification(
            frequency   = figoFrequency,
            regularity  = figoRegularity,
            duration    = figoDuration,
            summaryText = figoSummary
        )

        // ── 5. Todos los registros diarios para bio-analítica y alertas ───────
        val allLogsWithSymptoms = try {
            lumiDao.getAllLogsAscending().first()
        } catch (e: Exception) {
            emptyList()
        }

        // ── 6. Detección de Banderas Rojas Ginecológicas ──────────────────────
        val redFlags = mutableListOf<ClinicalRedFlag>()

        val maxPainInLogs = allLogsWithSymptoms.maxOfOrNull { it.dailyLog.painLevel } ?: 0
        val maxPainInRecent = insights.recentCycleStats.maxOfOrNull { it.avgPainLevel.toInt() } ?: 0
        val effectiveMaxPain = maxOf(maxPainInLogs, maxPainInRecent)
        if (effectiveMaxPain >= 7) {
            redFlags.add(
                ClinicalRedFlag(
                    title = "Cólicos menstruales intensos (EVA $effectiveMaxPain/10)",
                    description = "Episodios de dolor significativo registrados. Se aconseja valoración ginecológica para descartar causas subyacentes como endometriosis o adenomiosis.",
                    isSevere = true
                )
            )
        }

        val maxBleedInRecent = insights.recentCycleStats.maxOfOrNull { it.bleedingDays } ?: avgPeriodLength
        if (avgPeriodLength > 8 || maxBleedInRecent > 8) {
            redFlags.add(
                ClinicalRedFlag(
                    title = "Sangrado menstrual prolongado (> 8 días)",
                    description = "Duración de sangrado de hasta $maxBleedInRecent días. Se aconseja evaluar causas funcionales o estructurales y vigilar niveles de hierro.",
                    isSevere = true
                )
            )
        }

        if (cycleVariation > 9) {
            redFlags.add(
                ClinicalRedFlag(
                    title = "Variabilidad en la duración del ciclo (Δ $cycleVariation días)",
                    description = "Variación mayor a 9 días entre ciclos. Se sugiere evaluación médica del perfil hormonal, función tiroidea y metabólica.",
                    isSevere = false
                )
            )
        }

        val hasSpotting = allLogsWithSymptoms.any { it.dailyLog.spotting }
        if (hasSpotting) {
            redFlags.add(
                ClinicalRedFlag(
                    title = "Manchado intermenstrual (Spotting)",
                    description = "Goteo o sangrado acíclico registrado fuera de la menstruación. Sugerir valoración ginecológica y ecográfica de la mucosa endometrial.",
                    isSevere = false
                )
            )
        }

        if (daysSinceLmp != null && daysSinceLmp > 45 && !isOnContraceptive) {
            redFlags.add(
                ClinicalRedFlag(
                    title = "Retraso menstrual notable ($daysSinceLmp días desde último periodo)",
                    description = "Ciclo actual extendido sin sangrado reportado. Descartar embarazo y evaluar causas de anovulación funcional.",
                    isSevere = true
                )
            )
        }

        // ── 7. Análisis de Bio-Correlación Spearman y Clusters Jaccard ────────
        val bioInsights = try {
            bioEngine.computeInsights(allLogsWithSymptoms).take(5)
        } catch (e: Exception) {
            emptyList()
        }

        val symptomClusters = try {
            clusterEngine.computeClusters(allLogsWithSymptoms) { dateMs ->
                val logDate = Instant.ofEpochMilli(dateMs).atZone(zone).toLocalDate()
                val ownerCycle = allCycles.firstOrNull { cycle ->
                    val start = Instant.ofEpochMilli(cycle.startDate).atZone(zone).toLocalDate()
                    val end = cycle.endDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: LocalDate.now()
                    !logDate.isBefore(start) && !logDate.isAfter(end)
                }
                if (ownerCycle != null) {
                    val dayInCycle = CyclePredictor.dayInCycleWrapped(
                        Instant.ofEpochMilli(ownerCycle.startDate).atZone(zone).toLocalDate(),
                        ownerCycle.cycleLength,
                        logDate
                    )
                    CyclePredictor.phaseForDay(dayInCycle, ownerCycle.cycleLength, ownerCycle.periodLength)
                } else null
            }.take(4)
        } catch (e: Exception) {
            emptyList()
        }

        // ── 8. Registros del último ciclo y Coverline Sintotérmico ────────────
        val latestLogs = getLatestCycleLogs()
        val latestStartDate = getLatestCycleStartDate()

        val bbtWithDays = latestLogs.mapNotNull { log ->
            val temp = log.basalBodyTemp
            if (temp != null && temp in 35.0f..38.5f) {
                val sDate = latestStartDate ?: lmpDate ?: LocalDate.now()
                val lDate = Instant.ofEpochMilli(log.date).atZone(zone).toLocalDate()
                val day = ChronoUnit.DAYS.between(sDate, lDate).toInt() + 1
                day to temp
            } else null
        }.sortedBy { it.first }

        val bbtCoverline = computeBbtCoverline(bbtWithDays)

        // ── 9. Ensamblar DTO completo ─────────────────────────────────────────
        val data = MedicalReportData(
            userName                = userName,
            userAge                 = userAge,
            userHeightCm            = userHeight,
            userWeightKg            = userWeight,
            isOnContraceptive       = isOnContraceptive,
            avgCycleLength          = avgCycleLength,
            avgPeriodLength         = avgPeriodLength,
            generatedAt             = LocalDate.now(),
            historicalStats         = insights.historicalStats,
            recentCycles            = insights.recentCycleStats,
            cycleStartDates         = startDates,
            symptomCorrelations     = insights.symptomCorrelations,
            moodDistribution        = insights.moodDistribution,
            latestCycleLogs         = latestLogs,
            latestCycleStartDate    = latestStartDate,
            lastMenstrualPeriodDate = lmpDate,
            daysSinceLmp            = daysSinceLmp,
            figoClassification      = figoClassification,
            clinicalRedFlags        = redFlags,
            bioInsights             = bioInsights,
            symptomClusters         = symptomClusters,
            bbtCoverline            = bbtCoverline
        )

        return generator.generate(data)
    }

    private fun computeBbtCoverline(validTemps: List<Pair<Int, Float>>): Float? {
        if (validTemps.size < 9) return null
        for (i in 6..(validTemps.size - 3)) {
            val preDays = validTemps.subList(i - 6, i)
            val postDays = validTemps.subList(i, i + 3)
            val cover = preDays.maxOf { it.second }
            val allHigher = postDays.all { it.second > cover }
            val thirdHigherEnough = (postDays[2].second - cover) >= 0.20f
            if (allHigher && thirdHigherEnough) {
                return cover
            }
        }
        return null
    }

    private suspend fun getLatestCycleLogs(): List<DailyLogEntity> {
        val active = lumiDao.getCurrentActiveCycle()
        if (active != null) return lumiDao.getLogsDelCiclo(active.id)

        val lastClosed = lumiDao.getClosedCycles().firstOrNull()
        if (lastClosed != null) return lumiDao.getLogsDelCiclo(lastClosed.id)

        return emptyList()
    }

    private suspend fun getLatestCycleStartDate(): LocalDate? {
        val active = lumiDao.getCurrentActiveCycle() ?: lumiDao.getClosedCycles().firstOrNull()
        return active?.let {
            Instant.ofEpochMilli(it.startDate).atZone(ZoneOffset.UTC).toLocalDate()
        }
    }
}
