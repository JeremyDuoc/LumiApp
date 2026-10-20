package com.jeremy.lumi

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jeremy.lumi.notification.NotificationChannels
import com.jeremy.lumi.ui.widget.WidgetUpdaterWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class LumiApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureAllChannelsExist(this)

        // Encolado en segundo plano para evitar contención de SQLite y bloqueo del hilo
        // principal durante el arranque en frío (previene ANR en SystemJobService.onStartJob).
        appScope.launch {
            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdaterWorker>(
                6, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(this@LumiApp).enqueueUniquePeriodicWork(
                "widget_updater",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
