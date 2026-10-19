package com.jeremy.lumi.ui.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/** Debounce bursts from lifecycle, new cycles, preferences and widget placement. */
object WidgetRefreshScheduler {
    fun requestNow(context: Context) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "lumi_widget_refresh_now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetUpdaterWorker>().build()
        )
    }
}
