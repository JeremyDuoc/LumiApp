package com.jeremy.lumi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cycleId: Int,         // Para saber a qué ciclo pertenece este día
    val date: Long,           // Fecha exacta del registro (epoch ms, inicio del día UTC)

    // --- FLUJO Y SÍNTOMAS FÍSICOS ---
    val flowIntensity: String? = null,  // "Ligero", "Medio", "Abundante", "Sin flujo"
    val painLevel: Int = 0,             // Cólicos: 0–10
    val spotting: Boolean = false,      // Manchado intermenstrual

    // --- BIENESTAR ---
    val mood: String? = null,           // Ej: "Irritable", "Triste", "Con energía"
    val energyLevel: Int? = null,       // 0–10 (0 = agotada, 10 = con mucha energía)
    val sleepHours: Float? = null,      // Horas de sueño (ej. 6.5)
    val stressLevel: Int? = null,       // 0–10

    // --- DATOS BIOLÓGICOS AVANZADOS (opcionales, para predicción fina) ---
    val basalBodyTemp: Float? = null,   // Temperatura basal corporal en °C (ej. 36.7)
    val cervicalMucus: String? = null,  // "Seco", "Pegajoso", "Cremoso", "Clara de Huevo"

    // --- NOTAS ---
    val notes: String? = null,

    // --- RELACIONES SEXUALES ---
    val hadIntercourse: Boolean = false,
    val protectionUsed: Boolean? = null,
    val contraceptionMethod: String? = null,
    val intercourseNotes: String? = null,
    val showIntercourseOnCalendar: Boolean = true
)