package com.jeremy.lumi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.reminders.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AlarmManager pierde todas las alarmas programadas cuando el dispositivo se reinicia.
 * Este receiver escucha BOOT_COMPLETED y vuelve a programar cada recordatorio activo.
 * Requiere el permiso RECEIVE_BOOT_COMPLETED en el manifest.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: LumiRepository

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val activeReminders = repository.getActiveReminders().first()
                        activeReminders.forEach { reminder ->
                            AlarmScheduler.schedule(context, reminder)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            else -> return
        }
    }
}