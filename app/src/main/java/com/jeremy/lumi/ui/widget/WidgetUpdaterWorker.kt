package com.jeremy.lumi.ui.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.usecase.CyclePredictor
import com.jeremy.lumi.di.DatabaseModule
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WidgetUpdaterWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetWorkerEntryPoint {
        fun repository(): LumiRepository
        fun widgetUpdater(): WidgetUpdater
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(context, WidgetWorkerEntryPoint::class.java)
            val repository = entryPoint.repository()
            val updater = entryPoint.widgetUpdater()
            
            val cycle = repository.getCurrentActiveCycle()
            val closedCycles = repository.getClosedCycles()
            
            if (cycle != null) {
                val startDate = Instant.ofEpochMilli(cycle.startDate).atZone(ZoneId.of("UTC")).toLocalDate()
                val mathCycleLen = if (closedCycles.size >= 3) {
                    CyclePredictor.weightedAverageCycleLength(closedCycles).toInt().coerceIn(15, 60)
                } else {
                    cycle.cycleLength.coerceAtLeast(15)
                }
                
                val date = LocalDate.now()
                val day = CyclePredictor.dayInCycleWrapped(startDate, mathCycleLen, date)
                val phaseEnum = CyclePredictor.phaseForDay(day, mathCycleLen, cycle.periodLength, isPregnant = false, isOnContraceptive = false)
                
                val phaseName = when (phaseEnum) {
                    com.jeremy.lumi.domain.model.CyclePhase.MENSTRUAL -> "Menstruación"
                    com.jeremy.lumi.domain.model.CyclePhase.FOLLICULAR -> "Fase Folicular"
                    com.jeremy.lumi.domain.model.CyclePhase.OVULATION -> "Ovulación"
                    com.jeremy.lumi.domain.model.CyclePhase.LUTEAL -> "Fase Lútea"
                    com.jeremy.lumi.domain.model.CyclePhase.PREGNANCY -> "Embarazo"
                    com.jeremy.lumi.domain.model.CyclePhase.UNKNOWN -> "Desconocido"
                }
                
                // For a simpler prediction message in background
                val nextPeriodDays = mathCycleLen - day
                val msg = if (nextPeriodDays < 0) "Retraso de ${-nextPeriodDays} días" else "Próximo en $nextPeriodDays días"
                
                updater.updateWidget(day, phaseName, msg, "#9B72C0")
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
