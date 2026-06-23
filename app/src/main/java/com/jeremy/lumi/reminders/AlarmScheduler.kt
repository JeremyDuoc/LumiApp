package com.jeremy.lumi.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jeremy.lumi.data.local.entity.ReminderEntity
import com.jeremy.lumi.data.local.entity.ReminderType
import com.jeremy.lumi.receiver.ReminderAlarmReceiver
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

object AlarmScheduler {

    private const val EXTRA_REMINDER_ID = "extra_reminder_id"

    // ─────────────────────────────────────────────────────────────────────
    //  NUEVO — esta es la función que faltaba y rompía RemindersViewModel.
    //  Convierte una LocalDate + hora:minuto en epoch millis, en la zona
    //  horaria del dispositivo. La usa syncCycleReminders() para calcular
    //  el trigger de PERIOD_SOON / OVULATION_SOON a partir de la predicción.
    // ─────────────────────────────────────────────────────────────────────
    fun epochMillisAt(date: LocalDate, hourOfDay: Int, minute: Int): Long {
        return date
            .atTime(hourOfDay, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Calcula el primer trigger para un recordatorio recién creado, a la hora indicada.
     * Si baseDate es null, se usa hoy. Si la hora ya pasó para esa fecha, salta al día siguiente.
     */
    fun computeNextTrigger(type: ReminderType, hourOfDay: Int, minute: Int, baseDate: LocalDate? = null): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            if (baseDate != null) {
                set(Calendar.YEAR, baseDate.year)
                set(Calendar.MONTH, baseDate.monthValue - 1)
                set(Calendar.DAY_OF_MONTH, baseDate.dayOfMonth)
            }
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // Si no dimos una fecha base explícita y la hora ya pasó, pasa al día siguiente
            if (baseDate == null && before(now)) {
                add(Calendar.DATE, 1)
            }
        }
        return target.timeInMillis
    }

    /**
     * Calcula el siguiente trigger después de que una alarma ya disparó,
     * según el intervalo de repetición de ese tipo.
     */
    fun computeFollowingTrigger(type: ReminderType, previousTriggerAt: Long): Long {
        val intervalDays = type.defaultRepeatIntervalDays() ?: 1L
        return previousTriggerAt + (intervalDays * 24L * 60L * 60L * 1000L)
    }

    /** Programa (o reprograma) la alarma exacta para un recordatorio. */
    fun schedule(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, reminder.id)

        if (reminder.nextTriggerAt <= System.currentTimeMillis()) return

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminder.nextTriggerAt,
            pendingIntent
        )
    }

    fun cancel(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context, reminderId))
    }

    fun extractReminderId(intent: Intent): Int =
        intent.getIntExtra(EXTRA_REMINDER_ID, -1)

    private fun buildPendingIntent(context: Context, reminderId: Int): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId,                    // requestCode único por recordatorio
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}