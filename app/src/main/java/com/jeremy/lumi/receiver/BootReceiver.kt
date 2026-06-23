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
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val activeReminders = repository.getActiveReminders().first()
            activeReminders.forEach { reminder ->
                AlarmScheduler.schedule(context, reminder)
            }
        }
    }
}