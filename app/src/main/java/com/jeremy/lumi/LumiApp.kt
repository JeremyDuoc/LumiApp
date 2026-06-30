package com.jeremy.lumi

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LumiApp : Application(){
    override fun onCreate() {
        super.onCreate()
        com.jeremy.lumi.notifications.NotificationChannels.ensureAllChannelsExist(this)
        
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.jeremy.lumi.ui.widget.WidgetUpdaterWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        ).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "widget_updater",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
