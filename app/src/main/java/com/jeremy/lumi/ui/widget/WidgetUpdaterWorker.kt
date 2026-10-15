package com.jeremy.lumi.ui.widget

import android.content.Context
import com.jeremy.lumi.R
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
import java.time.ZoneOffset

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
                val startDate = Instant.ofEpochMilli(cycle.startDate).atZone(ZoneOffset.UTC).toLocalDate()
                val mathCycleLen = if (closedCycles.size >= 3) {
                    CyclePredictor.weightedAverageCycleLength(closedCycles).toInt().coerceIn(15, 60)
                } else {
                    cycle.cycleLength.coerceAtLeast(15)
                }
                
                val date = com.jeremy.lumi.utils.LumiClock.todayLocalDate()
                val day = CyclePredictor.dayInCycle(startDate, mathCycleLen, date)
                val phaseEnum = CyclePredictor.phaseForDay(day, mathCycleLen, cycle.periodLength, isPregnant = false, isOnContraceptive = false)
                
                val phaseName = when (phaseEnum) {
                    com.jeremy.lumi.domain.model.CyclePhase.MENSTRUAL -> context.getString(R.string.pm_phase_menstrual)
                    com.jeremy.lumi.domain.model.CyclePhase.FOLLICULAR -> context.getString(R.string.pm_phase_follicular)
                    com.jeremy.lumi.domain.model.CyclePhase.OVULATION -> context.getString(R.string.pm_phase_ovulation)
                    com.jeremy.lumi.domain.model.CyclePhase.LUTEAL -> context.getString(R.string.pm_phase_luteal)
                    com.jeremy.lumi.domain.model.CyclePhase.PREGNANCY -> context.getString(R.string.phase_pregnancy)
                    com.jeremy.lumi.domain.model.CyclePhase.UNKNOWN -> context.getString(R.string.phase_unknown)
                }
                
                // For a simpler prediction message in background
                val nextPeriodDays = mathCycleLen - day
                val msg = if (nextPeriodDays < 0) {
                    context.getString(R.string.home_period_delayed_by, -nextPeriodDays)
                } else {
                    context.getString(R.string.home_period_next_in, nextPeriodDays)
                }
                
                updater.updateWidget(day, phaseName, msg, "#9B72C0")
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
