package com.jeremy.lumi.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /** Revisa si el SDK está disponible en el dispositivo */
    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    /** Permisos que requiere la app.
     *  FIX P1-5: SleepSessionRecord.PERMISSION_READ es el permiso correcto para
     *  Android 13 (API 33). En API 34+ Health Connect unifica ambos bajo READ_SLEEP.
     *  Declarar ambos garantiza compatibilidad en todos los dispositivos soportados. */
    val permissions = setOf(
        HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    /** Chequea si todos los permisos están concedidos */
    suspend fun hasAllPermissions(): Boolean {
        if (!isAvailable()) return false
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    /** Lee la temperatura basal promedio en un rango de tiempo y la devuelve en Celsius */
    suspend fun readBasalTemperature(start: Instant, end: Instant): Float? = withContext(Dispatchers.IO) {
        if (!hasAllPermissions()) return@withContext null
        try {
            val request = ReadRecordsRequest(
                recordType = BasalBodyTemperatureRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = healthConnectClient.readRecords(request)
            if (response.records.isEmpty()) return@withContext null

            // Calcular un promedio si hay varias lecturas
            val avgCelsius = response.records.map { it.temperature.inCelsius }.average()
            return@withContext avgCelsius.toFloat()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Lee el total de horas de sueño en un rango de tiempo */
    suspend fun readSleepHours(start: Instant, end: Instant): Float? = withContext(Dispatchers.IO) {
        if (!hasAllPermissions()) return@withContext null
        try {
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = healthConnectClient.readRecords(request)
            if (response.records.isEmpty()) return@withContext null

            var totalDurationMs = 0L
            for (record in response.records) {
                val duration = record.endTime.toEpochMilli() - record.startTime.toEpochMilli()
                totalDurationMs += duration
            }
            return@withContext (totalDurationMs / (1000f * 60 * 60)) // Convertir a horas
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Lee el total de pasos en un rango de tiempo */
    suspend fun readTotalSteps(start: Instant, end: Instant): Long? = withContext(Dispatchers.IO) {
        if (!hasAllPermissions()) return@withContext null
        try {
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = healthConnectClient.readRecords(request)
            if (response.records.isEmpty()) return@withContext null

            return@withContext response.records.sumOf { it.count }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Lee el promedio de la frecuencia cardíaca en un rango de tiempo */
    suspend fun readAverageHeartRate(start: Instant, end: Instant): Float? = withContext(Dispatchers.IO) {
        if (!hasAllPermissions()) return@withContext null
        try {
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = healthConnectClient.readRecords(request)
            if (response.records.isEmpty()) return@withContext null

            var totalBeats = 0L
            var totalSamples = 0
            for (record in response.records) {
                for (sample in record.samples) {
                    totalBeats += sample.beatsPerMinute
                    totalSamples++
                }
            }
            if (totalSamples == 0) return@withContext null
            return@withContext (totalBeats.toFloat() / totalSamples)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
