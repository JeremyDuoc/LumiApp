package com.jeremy.lumi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cycleId: Int, // Para saber a qué ciclo pertenece este día
    val date: Long, // Fecha exacta del registro
    val flowIntensity: String?, // Ej: "Ligero", "Medio", "Abundante"
    val painLevel: Int, // Escala del 1 al 10 (Aquí lanzaremos la alerta médica si es muy alto)
    val mood: String?, // Ej: "Irritable", "Triste", "Con energía"
    val cervicalMucus: String? = null, // Ej: "Seco", "Pegajoso", "Cremoso", "Clara de Huevo"
    val notes: String?, // El campo de texto libre que tanto buscan

    // --- RELACIONES SEXUALES ---
    val hadIntercourse: Boolean = false,
    val protectionUsed: Boolean? = null, // null = no aplica (no hubo relación)
    val contraceptionMethod: String? = null, // Ej: "Condón", "Píldora", "Ninguno"
    val intercourseNotes: String? = null, // Notas libres específicas de la relación
    val showIntercourseOnCalendar: Boolean = true // La usuaria decide si el corazón aparece en el calendario
)