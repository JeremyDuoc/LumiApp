package com.jeremy.lumi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "symptoms")
data class SymptomEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dailyLogId: Int, // Esta es la clave foránea que lo conecta con el día
    val name: String, // Ej: "Acné", "Dolor de cabeza", "Fatiga"
    val intensity: Int // Escala de dolor o molestia (1 al 10)
)