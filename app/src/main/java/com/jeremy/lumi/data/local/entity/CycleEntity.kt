package com.jeremy.lumi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycles")
data class CycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDate: Long,
    val endDate: Long?,
    val predictedOvulationDate: Long?,
    val cycleLength: Int = 28,   // ← nuevo
    val periodLength: Int = 5    // ← nuevo
)

/** Persisted endDate is the last day of the cycle, inclusive (UTC date keys). */
fun CycleEntity.closedLengthDays(): Int? {
    val end = endDate ?: return null
    val startDay = java.time.Instant.ofEpochMilli(startDate).atZone(java.time.ZoneOffset.UTC).toLocalDate()
    val endDay = java.time.Instant.ofEpochMilli(end).atZone(java.time.ZoneOffset.UTC).toLocalDate()
    val length = java.time.temporal.ChronoUnit.DAYS.between(startDay, endDay) + 1
    return length.takeIf { it in 1..Int.MAX_VALUE.toLong() }?.toInt()
}
