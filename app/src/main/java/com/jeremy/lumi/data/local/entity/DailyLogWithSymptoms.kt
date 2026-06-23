package com.jeremy.lumi.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

// Esta clase no es una tabla, es una estructura que junta el día con su lista de síntomas
data class DailyLogWithSymptoms(
    @Embedded val dailyLog: DailyLogEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "dailyLogId"
    )
    val symptoms: List<SymptomEntity>
)