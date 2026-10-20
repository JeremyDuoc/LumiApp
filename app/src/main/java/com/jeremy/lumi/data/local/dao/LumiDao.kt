package com.jeremy.lumi.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.local.entity.ReminderEntity
import com.jeremy.lumi.data.local.entity.SymptomEntity
import com.jeremy.lumi.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LumiDao {

    // --- CICLOS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: CycleEntity): Long

    @Query("SELECT * FROM cycles ORDER BY startDate DESC")
    fun getAllCycles(): Flow<List<CycleEntity>>

    @Query("SELECT * FROM cycles WHERE endDate IS NULL ORDER BY startDate DESC LIMIT 1")
    suspend fun getCurrentActiveCycle(): CycleEntity?

    @androidx.room.Update
    suspend fun updateCycle(cycle: CycleEntity)

    @androidx.room.Delete
    suspend fun deleteCycle(cycle: CycleEntity)

    @Query("DELETE FROM cycles WHERE id = :cycleId")
    suspend fun deleteCycleRowById(cycleId: Int)

    @Transaction
    suspend fun deleteCycleById(cycleId: Int) {
        reassignDailyLogs(sourceCycleId = cycleId, targetCycleId = 0)
        deleteCycleRowById(cycleId)
    }

    @Query("SELECT * FROM cycles WHERE id = :cycleId LIMIT 1")
    suspend fun getCycleById(cycleId: Int): CycleEntity?

    @Query("UPDATE daily_logs SET cycleId = :targetCycleId WHERE cycleId = :sourceCycleId")
    suspend fun reassignDailyLogs(sourceCycleId: Int, targetCycleId: Int)

    @Query("UPDATE daily_logs SET cycleId = :targetCycleId WHERE date >= :startDate AND (cycleId = :sourceCycleId OR cycleId = 0)")
    suspend fun reassignLogsFromDate(sourceCycleId: Int, targetCycleId: Int, startDate: Long)

    @Query("SELECT * FROM cycles WHERE endDate IS NOT NULL ORDER BY startDate DESC LIMIT 1")
    suspend fun getPreviousClosedCycle(): CycleEntity?

    @Transaction
    suspend fun closeActiveCycle(endDate: Long, realCycleLength: Int) {
        val currentCycle = getCurrentActiveCycle() ?: return
        updateCycle(
            currentCycle.copy(
                endDate = endDate,
                cycleLength = realCycleLength
            )
        )
    }

    /** Close and replace the active cycle atomically; repeated taps are idempotent. */
    @Transaction
    suspend fun startCycleReplacingActive(cycle: CycleEntity) {
        val current = getCurrentActiveCycle()
        if (current != null) {
            val start = java.time.Instant.ofEpochMilli(current.startDate).atZone(java.time.ZoneOffset.UTC).toLocalDate()
            val next = java.time.Instant.ofEpochMilli(cycle.startDate).atZone(java.time.ZoneOffset.UTC).toLocalDate()
            if (!next.isAfter(start)) return
            updateCycle(current.copy(
                endDate = next.minusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
                cycleLength = java.time.temporal.ChronoUnit.DAYS.between(start, next).toInt()
            ))
        }
        val id = insertCycle(cycle).toInt()
        reassignLogsFromDate(current?.id ?: 0, id, cycle.startDate)
    }

    @Transaction
    suspend fun undoLastPeriodStart(): Boolean {
        val active = getCurrentActiveCycle() ?: return false
        val previous = getPreviousClosedCycle() ?: return false
        reassignDailyLogs(active.id, previous.id)
        deleteCycleById(active.id)
        updateCycle(previous.copy(endDate = null))
        return true
    }

    // --- REGISTROS DIARIOS Y SÍNTOMAS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(dailyLog: DailyLogEntity): Long

    @Query("SELECT id FROM daily_logs WHERE date = :date LIMIT 1")
    suspend fun getDailyLogIdByDate(date: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptoms(symptoms: List<SymptomEntity>)

    // Borra los síntomas previos de un día antes de re-insertar (evita duplicados al editar)
    @Query("DELETE FROM symptoms WHERE dailyLogId = :dailyLogId")
    suspend fun deleteSymptomsForLog(dailyLogId: Int)

    /**
     * Operación atómica de guardado de registro diario y sus síntomas.
     * Si ocurre algún fallo o cancelación de corrutina a la mitad, Room revierte
     * los cambios evitando que se borren síntomas previos sin guardar los nuevos.
     */
    @Transaction
    suspend fun upsertDailyLogWithSymptoms(dailyLog: DailyLogEntity, symptoms: List<SymptomEntity>) {
        val existingId = getDailyLogIdByDate(dailyLog.date) ?: 0
        val resolvedLogId = insertDailyLog(dailyLog.copy(id = existingId)).toInt()
        deleteSymptomsForLog(resolvedLogId)
        if (symptoms.isNotEmpty()) {
            insertSymptoms(symptoms.map { it.copy(id = 0, dailyLogId = resolvedLogId) })
        }
    }

    // Trae el registro de un día específico con todos sus síntomas
    @Transaction
    @Query("SELECT * FROM daily_logs WHERE date = :targetDate ORDER BY id DESC LIMIT 1")
    suspend fun getDailyLogWithSymptoms(targetDate: Long): DailyLogWithSymptoms?

    // Historial completo, ordenado por fecha. El ViewModel decide asc/desc en memoria,
    // pero dejamos ambas queries explícitas para que la DB haga el trabajo pesado.
    @Transaction
    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    fun getAllLogsDescending(): Flow<List<DailyLogWithSymptoms>>

    @Transaction
    @Query("SELECT * FROM daily_logs ORDER BY date ASC")
    fun getAllLogsAscending(): Flow<List<DailyLogWithSymptoms>>

    /**
     * Logs dentro de un rango de fechas (epoch millis, extremos incluidos).
     * Usar para gráficos que solo necesitan un período concreto — evita
     * cargar todo el historial en memoria.
     */
    @Transaction
    @Query("SELECT * FROM daily_logs WHERE date >= :from AND date <= :to ORDER BY date ASC")
    suspend fun getLogsInRange(from: Long, to: Long): List<DailyLogWithSymptoms>

    /**
     * Todos los registros diarios que pertenecen a un ciclo concreto,
     * ordenados por fecha ascendente.
     * Se usa en GraficasViewModel para trazar la curva de temperatura basal
     * y las tendencias de dolor/estrés a lo largo del ciclo activo.
     */
    @Query("SELECT * FROM daily_logs WHERE cycleId = :cicloId ORDER BY date ASC")
    suspend fun getLogsDelCiclo(cicloId: Int): List<DailyLogEntity>

    /**
     * Todos los ciclos cerrados (endDate != null), ordenados del más reciente
     * al más antiguo. Necesario para el predictor de ciclo mejorado con
     * promedio ponderado y fase lútea personal.
     */
    @Query("SELECT * FROM cycles WHERE endDate IS NOT NULL ORDER BY startDate DESC")
    suspend fun getClosedCycles(): List<CycleEntity>

    @Query("SELECT * FROM cycles WHERE startDate <= :dateMs AND (endDate IS NULL OR endDate >= :dateMs) LIMIT 1")
    suspend fun getCycleForDate(dateMs: Long): CycleEntity?

    // --- RECORDATORIOS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY nextTriggerAt ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): ReminderEntity?

    @Query("UPDATE reminders SET isActive = 0 WHERE id = :id")
    suspend fun deactivateReminder(id: Int)

    @Query("UPDATE reminders SET nextTriggerAt = :nextTriggerAt WHERE id = :id")
    suspend fun updateNextTrigger(id: Int, nextTriggerAt: Long)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    // --- RESET TOTAL ---
    @Query("DELETE FROM cycles")
    suspend fun deleteAllCycles()

    @Query("DELETE FROM daily_logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM symptoms")
    suspend fun deleteAllSymptoms()

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllChatMessages()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: SymptomEntity)

    @Transaction
    suspend fun clearAllDataForDev() {
        deleteAllCycles()
        deleteAllLogs()
        deleteAllSymptoms()
        deleteAllReminders()
        deleteAllChatMessages()
    }

    // --- RESPALDO PRIVADO EN GOOGLE DRIVE ---

    /** Instantánea completa de ciclos para serializar en el respaldo. */
    @Query("SELECT * FROM cycles ORDER BY id ASC")
    suspend fun getAllCyclesSnapshot(): List<CycleEntity>

    /** Instantánea completa de registros diarios (planos, sin relaciones). */
    @Query("SELECT * FROM daily_logs ORDER BY id ASC")
    suspend fun getAllDailyLogsSnapshot(): List<DailyLogEntity>

    /** Instantánea completa de síntomas. */
    @Query("SELECT * FROM symptoms ORDER BY id ASC")
    suspend fun getAllSymptomsSnapshot(): List<SymptomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycles(cycles: List<CycleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLogs(logs: List<DailyLogEntity>)

    /**
     * Lee las tres tablas de salud dentro de UNA transacción para que el
     * respaldo sea internamente coherente (nunca un ciclo sin sus registros
     * porque alguien escribió entre lectura y lectura).
     */
    @Transaction
    suspend fun getHealthDataSnapshot(): HealthSnapshot {
        return HealthSnapshot(
            cycles   = getAllCyclesSnapshot(),
            logs     = getAllDailyLogsSnapshot(),
            symptoms = getAllSymptomsSnapshot()
        )
    }

    /**
     * Restauración atómica desde un respaldo: borra los datos de salud actuales
     * e inserta los del respaldo EN UNA SOLA TRANSACCIÓN. Si algo falla a mitad
     * de camino, Room revierte todo — nunca queda una base a medias.
     * Recordatorios y chat NO forman parte del respaldo y no se tocan.
     */
    @Transaction
    suspend fun restoreHealthDataSnapshot(
        cycles: List<CycleEntity>,
        logs: List<DailyLogEntity>,
        symptoms: List<SymptomEntity>
    ) {
        deleteAllCycles()
        deleteAllLogs()
        deleteAllSymptoms()
        insertCycles(cycles)
        insertDailyLogs(logs)
        insertSymptoms(symptoms)
    }
}

/** Las tres tablas de salud leídas de forma coherente para el respaldo. */
data class HealthSnapshot(
    val cycles: List<CycleEntity>,
    val logs: List<DailyLogEntity>,
    val symptoms: List<SymptomEntity>
)
