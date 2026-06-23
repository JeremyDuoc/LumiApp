package com.jeremy.lumi.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.ReminderType

/**
 * Define un canal de notificación por cada "familia" de sonido que quieras
 * ofrecer. En Android 8+, el sonido se fija al CREAR el canal y no se puede
 * cambiar después sin crear un canal con ID nuevo — por eso aquí está todo
 * centralizado y documentado.
 *
 * Si mañana quieres añadir un sonido más, solo agregas una entrada nueva
 * a este enum con un channelId distinto.
 */
enum class LumiNotificationChannel(
    val channelId    : String,
    val nameRes      : Int,
    val descRes      : Int,
    /** null = sonido por defecto del sistema. */
    val soundResName : String?
) {
    // Canal general — sonido por defecto del sistema (el que ya tenías)
    DEFAULT(
        channelId    = "lumi_reminders_default",
        nameRes      = R.string.notif_channel_name,
        descRes      = R.string.notif_channel_desc,
        soundResName = null
    ),

    // Canal suave — para recordatorios de bienestar (registro diario, ciclo)
    // Requiere un archivo en res/raw/gentle_chime.mp3 (o .ogg/.wav)
    GENTLE(
        channelId    = "lumi_reminders_gentle",
        nameRes      = R.string.notif_channel_gentle_name,
        descRes      = R.string.notif_channel_gentle_desc,
        soundResName = "gentle_chime"
    ),

    // Canal urgente — para pastilla/método anticonceptivo (no se debe ignorar)
    // Requiere un archivo en res/raw/pill_alert.mp3
    URGENT(
        channelId    = "lumi_reminders_urgent",
        nameRes      = R.string.notif_channel_urgent_name,
        descRes      = R.string.notif_channel_urgent_desc,
        soundResName = "pill_alert"
    );

    companion object {
        /** Decide qué canal usar según el tipo de recordatorio. */
        fun forType(type: ReminderType): LumiNotificationChannel = when (type) {
            ReminderType.PILL_DAILY,
            ReminderType.PATCH_WEEKLY,
            ReminderType.RING_MONTHLY,
            ReminderType.INJECTION_MONTHLY,
            ReminderType.INJECTION_QUARTERLY -> URGENT

            ReminderType.LOG_DAILY,
            ReminderType.PERIOD_SOON,
            ReminderType.OVULATION_SOON      -> GENTLE

            ReminderType.CUSTOM              -> GENTLE
            ReminderType.SUPPLY_REMINDER -> TODO()
        }
    }
}

object NotificationChannels {

    /**
     * Crea todos los canales si no existen. Llamar una vez al iniciar la app
     * (ej. en Application.onCreate()) — es idempotente, no falla si ya existen.
     */
    fun ensureAllChannelsExist(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        LumiNotificationChannel.entries.forEach { def ->
            // Si ya existe, no lo tocamos — el sonido no se puede cambiar en caliente
            if (manager.getNotificationChannel(def.channelId) != null) return@forEach

            val channel = NotificationChannel(
                def.channelId,
                context.getString(def.nameRes),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(def.descRes)

                // Sonido personalizado, si lo tiene definido
                def.soundResName?.let { resName ->
                    val soundUri: Uri = "android.resource://${context.packageName}/raw/$resName".toUri()
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setSound(soundUri, audioAttributes)
                }
                // Si soundResName es null, el canal usa el sonido por defecto
                // del sistema automáticamente (no hace falta hacer nada más)
            }
            manager.createNotificationChannel(channel)
        }
    }
}