package com.jeremy.lumi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un mensaje en el chat de "Lumi" — feed de solo lectura.
 *
 * Cada vez que dispara una notificación del sistema (ReminderAlarmReceiver),
 * también se inserta una fila aquí con el mismo contenido. Así la usuaria
 * tiene un historial navegable de todo lo que Lumi le avisó, aunque haya
 * deslizado la notificación sin leerla.
 *
 * No es conversacional: la usuaria no responde, solo lee. Por eso no hay
 * campo "isFromUser" ni nada de hilos — es un feed cronológico simple.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id          : Int    = 0,

    // Texto que se muestra en el chat (puede ser más largo/cálido que el de la notificación)
    val text        : String,

    // Tipo de mensaje — determina el ícono y si se muestra como "recordatorio" destacado
    val messageType : ChatMessageType,

    // Vínculo opcional al recordatorio que lo generó (null si es un mensaje
    // "suelto" de Lumi, ej. un saludo o un dato educativo)
    val reminderId  : Int?   = null,

    // Cuándo se generó (epoch millis) — se usa para ordenar y agrupar por día
    val timestamp   : Long   = System.currentTimeMillis(),

    // Si la usuaria ya entró al chat y lo vio (para el badge de "no leído")
    val isRead      : Boolean = false
)

enum class ChatMessageType {
    GREETING,          // saludo / mensaje informal de Lumi
    PERIOD_SOON,       // aviso de regla próxima
    OVULATION_SOON,    // aviso de ovulación
    SUPPLY_REMINDER,   // "lleva tampón/copa/toallitas" — nuevo
    METHOD_REMINDER,   // pastilla/parche/anillo/inyección
    LOG_DAILY,         // recordatorio de registro diario
    CUSTOM,            // recordatorio personalizado de la usuaria
    EDUCATIONAL,       // dato/tip educativo
    INSIGHT,           // patrón detectado por el motor de correlación
    USER               // Pregunta hecha por la usuaria
}