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