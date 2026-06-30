package com.jeremy.lumi.domain.usecase

import android.content.Context
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.domain.model.BivariableInsight
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.roundToInt

class BivariableCorrelationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun computeInsights(logs: List<DailyLogWithSymptoms>): List<BivariableInsight> {
        val insights = mutableListOf<BivariableInsight>()

        // 1. Sueño vs Dolor
        val sleepVsPain = computeSleepVsPain(logs)
        if (sleepVsPain != null) insights.add(sleepVsPain)

        // 2. Estrés vs Energía
        val stressVsEnergy = computeStressVsEnergy(logs)
        if (stressVsEnergy != null) insights.add(stressVsEnergy)

        return insights.sortedByDescending { it.importance }
    }

    private fun getRandomString(resId: Int, vararg formatArgs: Any): String {
        val array = context.resources.getStringArray(resId)
        return String.format(array.random(), *formatArgs)
    }

    private fun computeSleepVsPain(logs: List<DailyLogWithSymptoms>): BivariableInsight? {
        // Filtrar días que tengan tanto horas de sueño como nivel de dolor reportado
        val validLogs = logs.filter { it.dailyLog.sleepHours != null && it.dailyLog.painLevel > 0 }
        
        // Necesitamos suficientes datos para que sea estadísticamente relevante
        if (validLogs.size < 10) return null

        val shortSleepLogs = validLogs.filter { it.dailyLog.sleepHours!! < 6f }
        val goodSleepLogs = validLogs.filter { it.dailyLog.sleepHours!! >= 6f }

        if (shortSleepLogs.size < 3 || goodSleepLogs.size < 3) return null

        val avgPainShortSleep = shortSleepLogs.map { it.dailyLog.painLevel }.average()
        val avgPainGoodSleep = goodSleepLogs.map { it.dailyLog.painLevel }.average()

        if (avgPainGoodSleep == 0.0) return null

        val increaseRatio = (avgPainShortSleep - avgPainGoodSleep) / avgPainGoodSleep

        // Si el dolor aumenta más de un 20% cuando duerme poco
        if (increaseRatio > 0.20) {
            val percentage = (increaseRatio * 100).roundToInt()
            val title = context.getString(R.string.insight_title_sleep_pain)
            val message = getRandomString(R.array.insight_msg_sleep_pain, percentage)
            return BivariableInsight(
                title = title,
                message = message,
                importance = increaseRatio.toFloat()
            )
        }
        return null
    }

    private fun computeStressVsEnergy(logs: List<DailyLogWithSymptoms>): BivariableInsight? {
        val validLogs = logs.filter { it.dailyLog.stressLevel != null && it.dailyLog.energyLevel != null }
        if (validLogs.size < 10) return null

        val highStressLogs = validLogs.filter { it.dailyLog.stressLevel!! >= 7 }
        val lowStressLogs = validLogs.filter { it.dailyLog.stressLevel!! <= 4 }

        if (highStressLogs.size < 3 || lowStressLogs.size < 3) return null

        val avgEnergyHighStress = highStressLogs.map { it.dailyLog.energyLevel!! }.average()
        val avgEnergyLowStress = lowStressLogs.map { it.dailyLog.energyLevel!! }.average()

        if (avgEnergyLowStress == 0.0) return null

        val dropRatio = (avgEnergyLowStress - avgEnergyHighStress) / avgEnergyLowStress

        // Si la energía cae más de un 20% en días de alto estrés
        if (dropRatio > 0.20 && avgEnergyHighStress < avgEnergyLowStress) {
            val percentage = (dropRatio * 100).roundToInt()
            val title = context.getString(R.string.insight_title_stress_energy)
            val message = getRandomString(R.array.insight_msg_stress_energy, percentage)
            return BivariableInsight(
                title = title,
                message = message,
                importance = dropRatio.toFloat()
            )
        }
        return null
    }
}
