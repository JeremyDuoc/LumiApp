package com.jeremy.lumi.ui.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Refreshes all installed widget types with the same on-device prediction source as Home. */
class WidgetUpdaterWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun widgetUpdater(): WidgetUpdater
    }

    override suspend fun doWork(): Result = try {
        EntryPointAccessors.fromApplication(context, Dependencies::class.java)
            .widgetUpdater().refresh()
        Result.success()
    } catch (e: Exception) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
