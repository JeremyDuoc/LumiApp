package com.jeremy.lumi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id               : Int     = 0,

    val type             : ReminderType,

    // Hora de disparo — para CUSTOM, esta es la hora elegida junto a la fecha;
    // para los demás tipos, es la hora diaria del recordatorio.
    val hourOfDay        : Int     = 20,
    val minute           : Int     = 0,

    val createdAt        : Long    = System.currentTimeMillis(),

    // Fecha real de colocación del método (parche, anillo, inyección)
    val methodStartDate  : Long?   = null,

    // Cuando isCustomDate = true, nextTriggerAt se calculó directamente desde
    // la fecha+hora que la usuaria eligió en el DatePicker/TimePicker, sin
    // pasar por defaultRepeatIntervalDays(). Útil para "cambiar el parche en
    // 3 meses" en vez del intervalo sugerido de 3 semanas.
    val isCustomDate     : Boolean = false,

    // Si la usuaria quiere que el recordatorio personalizado se repita
    // (ej. "cada 3 meses" en vez de una sola vez)
    val customRepeatDays : Long?   = null,

    // ── NUEVO: solo para SUPPLY_REMINDER ───────────────────────────────────
    // Qué método usa habitualmente la usuaria (tampón, copa, toallita...),
    // para personalizar el mensaje del chat y de la notificación.
    val supplyType       : SupplyType? = null,

    val label            : String? = null,

    val isActive         : Boolean = true,
    val nextTriggerAt    : Long    = 0L
)