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

    @Query("SELECT * FROM cycles WHERE endDate IS NULL LIMIT 1")
    suspend fun getCurrentActiveCycle(): CycleEntity?

    // --- REGISTROS DIARIOS Y SÍNTOMAS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(dailyLog: DailyLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptoms(symptoms: List<SymptomEntity>)

    // Borra los síntomas previos de un día antes de re-insertar (evita duplicados al editar)
    @Query("DELETE FROM symptoms WHERE dailyLogId = :dailyLogId")
    suspend fun deleteSymptomsForLog(dailyLogId: Int)

    // Trae el registro de un día específico con todos sus síntomas
    @Transaction
    @Query("SELECT * FROM daily_logs WHERE date = :targetDate")
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
     * Todos los ciclos cerrados (endDate != null), ordenados del más reciente
     * al más antiguo. Necesario para el predictor de ciclo mejorado con
     * promedio ponderado y fase lútea personal.
     */
    @Query("SELECT * FROM cycles WHERE endDate IS NOT NULL ORDER BY startDate DESC")
    suspend fun getClosedCycles(): List<CycleEntity>

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
}