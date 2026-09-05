package com.jeremy.lumi.data.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jeremy.lumi.data.backup.DriveBackupManager
import com.jeremy.lumi.data.backup.drive.DriveApiException
import com.jeremy.lumi.data.backup.drive.DriveAuthManager
import com.jeremy.lumi.data.backup.drive.DriveAuthResult
import com.jeremy.lumi.data.preferences.BackupPreferenceManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Respaldo automático diario en el Drive de la usuaria.
 *
 * Corre en segundo plano SOLO si se cumplen las tres condiciones:
 *  1. La usuaria activó el interruptor de respaldo automático.
 *  2. Hay clave de cifrado cacheada (creada al hacer el primer respaldo manual).
 *  3. Google concede el token en silencio (el permiso ya fue otorgado antes).
 *
 * Si falta cualquiera, el worker termina en éxito SIN hacer nada: nunca
 * muestra UI ni se queda reintentando para siempre.
 *
 * Usa el mismo patrón EntryPoint que WidgetUpdaterWorker para no añadir la
 * dependencia hilt-work al proyecto.
 */
class AutoBackupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AutoBackupEntryPoint {
        fun backupManager(): DriveBackupManager
        fun authManager(): DriveAuthManager
        fun backupPrefs(): BackupPreferenceManager
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AutoBackupEntryPoint::class.java
        )

        // La usuaria pudo desactivar el respaldo automático después de programarlo
        if (!entryPoint.backupPrefs().autoBackupFlow.first()) return Result.success()

        // Sin clave local no podemos cifrar; el primer respaldo manual la crea
        val key = entryPoint.backupManager().getCachedKey() ?: return Result.success()

        return when (val auth = entryPoint.authManager().requestAuthorization()) {
            is DriveAuthResult.Authorized -> try {
                entryPoint.backupManager().createBackup(auth.accessToken, key)
                Log.i(TAG, "Respaldo automático completado")
                Result.success()
            } catch (e: DriveApiException) {
                // Error del servidor: reintentar con backoff, pero con tope —
                // un fallo determinista no debe reintentarse eternamente.
                Log.w(TAG, "Falló el respaldo automático (HTTP ${e.httpCode}): ${e.message}")
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            } catch (e: IOException) {
                // Sin red a mitad de subida: WorkManager reintentará
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en el respaldo automático", e)
                Result.failure()
            }

            // Google exige pantalla de consentimiento: imposible desde un worker.
            // Se resolverá la próxima vez que la usuaria abra la pantalla de respaldo.
            else -> Result.success()
        }
    }

    companion object {
        private const val TAG = "AutoBackupWorker"
        private const val MAX_RETRIES = 3
        const val UNIQUE_WORK_NAME = "respaldo_automatico_drive"

        /** Programa el respaldo diario. KEEP: no reinicia el ciclo si ya existía. */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
