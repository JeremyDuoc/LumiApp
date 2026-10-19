package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.closedLengthDays
import kotlin.math.sqrt

/** Causal history: valid closed cycles before the active cycle, newest first. */
object CycleHistoryV2 {
    fun validCycles(cycles: List<CycleEntity>, beforeStartMillis: Long? = null): List<CycleEntity> =
        cycles.asSequence()
            .filter { it.endDate?.let { end -> beforeStartMillis == null || end < beforeStartMillis } == true }
            .filter { (it.closedLengthDays() ?: 0) in 15..60 }
            .sortedByDescending { it.startDate }
            .distinctBy { it.startDate }
            .toList()

    fun durations(cycles: List<CycleEntity>): List<Float> =
        validCycles(cycles).mapNotNull { it.closedLengthDays()?.toFloat() }

    fun median(values: List<Float>): Float {
        require(values.isNotEmpty())
        val x = values.sorted()
        val m = x.size / 2
        return if (x.size % 2 == 0) (x[m - 1] + x[m]) / 2f else x[m]
    }

    fun populationStd(values: List<Float>): Float {
        if (values.size < 2) return 0f
        val avg = values.average()
        return sqrt(values.sumOf { (it - avg) * (it - avg) } / values.size).toFloat()
    }
}