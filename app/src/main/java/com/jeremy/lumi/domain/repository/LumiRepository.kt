package com.jeremy.lumi.domain.repository

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.local.entity.ReminderEntity
import com.jeremy.lumi.data.local.entity.SymptomEntity
import kotlinx.coroutines.flow.Flow

interface LumiRepository {
    // Ciclos
    fun getAllCycles(): Flow<List<CycleEntity>>

    /**
     * Crea un nuevo ciclo activo en Room.
     * [cycleLength] y [periodLength] se guardan en la entidad para que el
     * predictor histórico pueda leerlos desde el primer ciclo.
     */
    suspend fun startNewCycle(
        startDate    : Long,
        cycleLength  : Int = 28,
        periodLength : Int = 5
    )

    suspend fun endCurrentCycle(endDate: Long, realCycleLength: Int)
    suspend fun getCurrentActiveCycle(): CycleEntity?

    /** Ciclos cerrados ordenados del más reciente al más antiguo. */
    suspend fun getClosedCycles(): List<CycleEntity>

    // Registros Diarios
    suspend fun saveDailyLogWithSymptoms(dailyLog: DailyLogEntity, symptoms: List<SymptomEntity>)
    suspend fun getDailyLog(date: Long): DailyLogWithSymptoms?
    fun getAllLogs(descending: Boolean = true): Flow<List<DailyLogWithSymptoms>>

    // Recordatorios
    suspend fun saveReminder(reminder: ReminderEntity): Long
    fun getActiveReminders(): Flow<List<ReminderEntity>>
    suspend fun getReminderById(id: Int): ReminderEntity?
    suspend fun deactivateReminder(id: Int)
    suspend fun updateNextTrigger(id: Int, nextTriggerAt: Long)
    suspend fun deleteReminder(reminder: ReminderEntity)

    /** Borra TODOS los datos: ciclos, registros, síntomas y recordatorios. */
    suspend fun deleteAllData()
}