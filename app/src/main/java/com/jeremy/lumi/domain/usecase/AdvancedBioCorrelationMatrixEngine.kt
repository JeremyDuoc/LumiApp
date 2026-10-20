package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.domain.model.BioVariable
import com.jeremy.lumi.domain.model.CorrelationDirection
import com.jeremy.lumi.domain.model.CorrelationInsight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class AdvancedBioCorrelationMatrixEngine @Inject constructor() {

    suspend fun computeInsights(logs: List<DailyLogWithSymptoms>): List<CorrelationInsight> = withContext(Dispatchers.Default) {
        val insights = mutableListOf<CorrelationInsight>()
        if (logs.isEmpty()) return@withContext insights

        try {
            val logMap = logs.associateBy { it.dailyLog.date / 86400000L }
            val days = logMap.keys.sorted()

            val vars = BioVariable.values()
            for (i in vars.indices) {
                for (j in i + 1 until vars.size) {
                    val varA = vars[i]
                    val varB = vars[j]

                    // Lag 0
                    evaluatePair(varA, varB, 0, logMap, days)?.let { insights.add(it) }

                    // Lag 1 (A shifted earlier by 1 day)
                    evaluatePair(varA, varB, 1, logMap, days)?.let { insights.add(it) }

                    // Lag 1 (B shifted earlier by 1 day)
                    evaluatePair(varB, varA, 1, logMap, days)?.let { insights.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Conservative family-wise error control across all variable pairs and lags.
        val variableCount = BioVariable.values().size
        val hypothesisCount = (variableCount * (variableCount - 1) / 2) * 3
        val adjustedAlpha = 0.05f / hypothesisCount
        return@withContext insights
            .filter { it.pValue.isFinite() && it.pValue < adjustedAlpha }
            .sortedByDescending { abs(it.correlationScore) }
    }

    private fun evaluatePair(
        varX: BioVariable,
        varY: BioVariable,
        lagDays: Int,
        logMap: Map<Long, DailyLogWithSymptoms>,
        days: List<Long>
    ): CorrelationInsight? {
        val xSeries = mutableListOf<Float>()
        val ySeries = mutableListOf<Float>()

        for (day in days) {
            val logY = logMap[day] ?: continue
            val logX = logMap[day - lagDays] ?: continue

            val valX = extractVar(logX.dailyLog, varX)
            val valY = extractVar(logY.dailyLog, varY)

            if (valX != null && valY != null) {
                xSeries.add(valX)
                ySeries.add(valY)
            }
        }

        val n = xSeries.size
        if (n < 8) return null

        if (variance(xSeries) <= 0f || variance(ySeries) <= 0f) return null

        val pearsonR = pearson(xSeries, ySeries)
        val rho = spearman(xSeries, ySeries)

        val t = if (abs(rho) >= 0.999f) {
            1000.0
        } else {
            rho * sqrt((n - 2).toDouble() / (1.0 - rho * rho))
        }

        val pValue = studentTPValue(t, n - 2).toFloat()

        if (abs(rho) >= 0.35f && pValue < 0.05f) {
            val direction = if (rho > 0) CorrelationDirection.POSITIVE else CorrelationDirection.NEGATIVE
            return CorrelationInsight(
                variableA = varX,
                variableB = varY,
                correlationScore = rho,
                pearsonR = pearsonR,
                isLagged = lagDays > 0,
                lagDays = lagDays,
                pValue = pValue,
                sampleSize = n,
                direction = direction,
                clinicalInterpretationKey = getClinicalKey(varX, varY, lagDays > 0)
            )
        }

        return null
    }

    private fun extractVar(log: DailyLogEntity, variable: BioVariable): Float? {
        return when (variable) {
            BioVariable.SLEEP_HOURS -> log.sleepHours
            BioVariable.PAIN_LEVEL -> log.painLevel.toFloat()
            BioVariable.STRESS_LEVEL -> log.stressLevel?.toFloat()
            BioVariable.ENERGY_LEVEL -> log.energyLevel?.toFloat()
            BioVariable.WATER_INTAKE -> log.waterIntake
            BioVariable.BASAL_BODY_TEMP -> log.basalBodyTemp
        }
    }

    private fun pearson(x: List<Float>, y: List<Float>): Float {
        val n = x.size
        val meanX = x.average().toFloat()
        val meanY = y.average().toFloat()
        var num = 0f
        var denX = 0f
        var denY = 0f
        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }
        if (denX == 0f || denY == 0f) return 0f
        return num / sqrt(denX * denY)
    }

    private fun rank(x: List<Float>): List<Float> {
        val sorted = x.mapIndexed { index, value -> Pair(index, value) }.sortedBy { it.second }
        val ranks = FloatArray(x.size)
        var i = 0
        while (i < sorted.size) {
            var j = i
            while (j < sorted.size && sorted[j].second.compareTo(sorted[i].second) == 0) {
                j++
            }
            val avgRank = (i + 1 + j).toFloat() / 2f
            for (k in i until j) {
                ranks[sorted[k].first] = avgRank
            }
            i = j
        }
        return ranks.toList()
    }

    private fun spearman(x: List<Float>, y: List<Float>): Float {
        return pearson(rank(x), rank(y))
    }

    private fun variance(list: List<Float>): Float {
        if (list.size < 2) return 0f
        val mean = list.average().toFloat()
        var sumSq = 0f
        for (v in list) {
            sumSq += (v - mean) * (v - mean)
        }
        return sumSq / (list.size - 1)
    }

    private fun studentTPValue(t: Double, df: Int): Double {
        val tAbs = abs(t)
        val theta = atan(tAbs / sqrt(df.toDouble()))
        val sinTheta = sin(theta)
        val cosTheta = cos(theta)

        if (df == 1) {
            return 1.0 - (2.0 / Math.PI) * theta
        }

        if (df % 2 == 1) {
            var sum = cosTheta
            var term = cosTheta
            for (i in 2 until df step 2) {
                term *= (i.toDouble() / (i + 1.0)) * sinTheta * sinTheta
                sum += term
            }
            return 1.0 - (2.0 / Math.PI) * (theta + sinTheta * sum)
        } else {
            var sum = 1.0
            var term = 1.0
            for (i in 1 until df - 1 step 2) {
                term *= (i.toDouble() / (i + 1.0)) * sinTheta * sinTheta
                sum += term
            }
            return 1.0 - sinTheta * sum
        }
    }

    private fun getClinicalKey(var1: BioVariable, var2: BioVariable, isLagged: Boolean): String {
        val set = setOf(var1, var2)
        val baseKey = when {
            set == setOf(BioVariable.SLEEP_HOURS, BioVariable.PAIN_LEVEL) -> "correlation_sleep_pain"
            set == setOf(BioVariable.SLEEP_HOURS, BioVariable.STRESS_LEVEL) -> "correlation_sleep_stress"
            set == setOf(BioVariable.SLEEP_HOURS, BioVariable.ENERGY_LEVEL) -> "correlation_sleep_energy"
            set == setOf(BioVariable.STRESS_LEVEL, BioVariable.ENERGY_LEVEL) -> "correlation_stress_energy"
            set == setOf(BioVariable.STRESS_LEVEL, BioVariable.PAIN_LEVEL) -> "correlation_stress_pain"
            set == setOf(BioVariable.WATER_INTAKE, BioVariable.PAIN_LEVEL) -> "correlation_water_pain"
            BioVariable.BASAL_BODY_TEMP in set -> "correlation_bbt_general"
            else -> "correlation_general"
        }
        return if (isLagged) "${baseKey}_lag" else baseKey
    }
}
