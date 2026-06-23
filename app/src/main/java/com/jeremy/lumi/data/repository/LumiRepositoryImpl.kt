package com.jeremy.lumi.data.repository

import com.jeremy.lumi.data.local.dao.LumiDao
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.local.entity.ReminderEntity
import com.jeremy.lumi.data.local.entity.SymptomEntity
import com.jeremy.lumi.domain.repository.LumiRepository
import kotlinx.coroutines.flow.Flow

class LumiRepositoryImpl(
    private val dao: LumiDao
) : LumiRepository {

    override fun getAllCycles(): Flow<List<CycleEntity>> {
        return dao.getAllCycles()
    }

    override suspend fun startNewCycle(startDate: Long, cycleLength: Int, periodLength: Int) {
        val dayMs = 86_400_000L

        // La ovulación se predice como: fin_esperado - fase_lútea_estándar (14 días)
        // El predictor histórico refinará esto cuando tenga ≥3 ciclos cerrados.
        val expectedEnd   = startDate + (cycleLength * dayMs)
        val ovulationDate = expectedEnd - (14 * dayMs)

        dao.insertCycle(
            CycleEntity(
                startDate              = startDate,
                endDate                = null,       // ciclo activo
                predictedOvulationDate = ovulationDate,
                cycleLength            = cycleLength,
                periodLength           = periodLength
            )
        )
    }

    override suspend fun endCurrentCycle(endDate: Long, realCycleLength: Int) {
        val currentCycle = dao.getCurrentActiveCycle() ?: return
        // Guardar la duración REAL del ciclo en la entidad para que el historial sea correcto.
        dao.insertCycle(
            currentCycle.copy(
                endDate     = endDate,
                cycleLength = realCycleLength
            )
        )
    }

    override suspend fun getCurrentActiveCycle(): CycleEntity? {
        return dao.getCurrentActiveCycle()
    }

    override suspend fun getClosedCycles(): List<CycleEntity> {
        return dao.getClosedCycles()
    }

    override suspend fun saveDailyLogWithSymptoms(dailyLog: DailyLogEntity, symptoms: List<SymptomEntity>) {
        // Guardamos el día primero para obtener su ID autogenerado (o actualizar si ya existía)
        val dailyLogId = dao.insertDailyLog(dailyLog)
        val resolvedLogId = if (dailyLog.id != 0) dailyLog.id else dailyLogId.toInt()

        // FIX: si estamos editando un registro existente, primero limpiamos sus síntomas
        // anteriores. Sin esto, un síntoma desmarcado por la usuaria se quedaría guardado
        // para siempre (el REPLACE del insert solo pisa los que coinciden por id).
        dao.deleteSymptomsForLog(resolvedLogId)

        if (symptoms.isNotEmpty()) {
            val symptomsToInsert = symptoms.map { it.copy(dailyLogId = resolvedLogId) }
            dao.insertSymptoms(symptomsToInsert)
        }
    }

    override suspend fun getDailyLog(date: Long): DailyLogWithSymptoms? {
        return dao.getDailyLogWithSymptoms(date)
    }

    override fun getAllLogs(descending: Boolean): Flow<List<DailyLogWithSymptoms>> {
        return if (descending) dao.getAllLogsDescending() else dao.getAllLogsAscending()
    }

    // --- RECORDATORIOS ---
    override suspend fun saveReminder(reminder: ReminderEntity): Long {
        return dao.insertReminder(reminder)
    }

    override fun getActiveReminders(): Flow<List<ReminderEntity>> {
        return dao.getActiveReminders()
    }

    override suspend fun getReminderById(id: Int): ReminderEntity? {
        return dao.getReminderById(id)
    }

    override suspend fun deactivateReminder(id: Int) {
        dao.deactivateReminder(id)
    }

    override suspend fun updateNextTrigger(id: Int, nextTriggerAt: Long) {
        dao.updateNextTrigger(id, nextTriggerAt)
    }

    override suspend fun deleteReminder(reminder: ReminderEntity) {
        dao.deleteReminder(reminder)
    }

    override suspend fun deleteAllData() {
        dao.deleteAllCycles()
        dao.deleteAllLogs()
        dao.deleteAllSymptoms()
        dao.deleteAllReminders()
        dao.deleteAllChatMessages()
    }
}