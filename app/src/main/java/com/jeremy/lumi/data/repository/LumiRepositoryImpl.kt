package com.jeremy.lumi.data.repository

import com.jeremy.lumi.data.local.dao.LumiDao
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.local.entity.ReminderEntity
import com.jeremy.lumi.data.local.entity.SymptomEntity
import com.jeremy.lumi.domain.repository.LumiRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LumiRepositoryImpl(
    private val dao: LumiDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LumiRepository {

    override fun getAllCycles(): Flow<List<CycleEntity>> {
        return dao.getAllCycles()
    }

    override suspend fun startNewCycle(startDate: Long, cycleLength: Int, periodLength: Int) = withContext(ioDispatcher) {
        val zone = java.time.ZoneId.of("UTC")
        val startLocalDate = java.time.Instant.ofEpochMilli(startDate).atZone(zone).toLocalDate()

        // La ovulación se predice como: fin_esperado - fase_lútea_estándar (14 días)
        val expectedEnd = startLocalDate.plusDays(cycleLength.toLong())
        val ovulationLocalDate = expectedEnd.minusDays(15)
        
        val ovulationDate = ovulationLocalDate.atStartOfDay(zone).toInstant().toEpochMilli()

        dao.startCycleReplacingActive(
            CycleEntity(
                startDate              = startDate,
                endDate                = null,       // ciclo activo
                predictedOvulationDate = ovulationDate,
                cycleLength            = cycleLength,
                periodLength           = periodLength
            )
        )
    }

    override suspend fun endCurrentCycle(endDate: Long, realCycleLength: Int) = withContext(ioDispatcher) {
        dao.closeActiveCycle(endDate, realCycleLength)
    }

    override suspend fun getCurrentActiveCycle(): CycleEntity? = withContext(ioDispatcher) {
        dao.getCurrentActiveCycle()
    }

    override suspend fun getClosedCycles(): List<CycleEntity> = withContext(ioDispatcher) {
        dao.getClosedCycles()
    }

    override suspend fun getCycleForDate(dateMs: Long): CycleEntity? = withContext(ioDispatcher) {
        dao.getCycleForDate(dateMs)
    }

    override suspend fun updateCycle(cycle: CycleEntity) = withContext(ioDispatcher) {
        dao.updateCycle(cycle)
    }

    override suspend fun deleteCycle(cycleId: Int) = withContext(ioDispatcher) {
        dao.deleteCycleById(cycleId)
    }

    override suspend fun undoLastPeriodStart(): Boolean = withContext(ioDispatcher) {
        dao.undoLastPeriodStart()
    }

    override suspend fun saveDailyLogWithSymptoms(dailyLog: DailyLogEntity, symptoms: List<SymptomEntity>) = withContext(ioDispatcher) {
        dao.upsertDailyLogWithSymptoms(dailyLog, symptoms)
    }

    override suspend fun getDailyLog(date: Long): DailyLogWithSymptoms? = withContext(ioDispatcher) {
        dao.getDailyLogWithSymptoms(date)
    }

    override fun getAllLogs(descending: Boolean): Flow<List<DailyLogWithSymptoms>> {
        return if (descending) dao.getAllLogsDescending() else dao.getAllLogsAscending()
    }

    // --- RECORDATORIOS ---
    override suspend fun saveReminder(reminder: ReminderEntity): Long = withContext(ioDispatcher) {
        dao.insertReminder(reminder)
    }

    override fun getActiveReminders(): Flow<List<ReminderEntity>> {
        return dao.getActiveReminders()
    }

    override suspend fun getReminderById(id: Int): ReminderEntity? = withContext(ioDispatcher) {
        dao.getReminderById(id)
    }

    override suspend fun deactivateReminder(id: Int) = withContext(ioDispatcher) {
        dao.deactivateReminder(id)
    }

    override suspend fun updateNextTrigger(id: Int, nextTriggerAt: Long) = withContext(ioDispatcher) {
        dao.updateNextTrigger(id, nextTriggerAt)
    }

    override suspend fun deleteReminder(reminder: ReminderEntity) = withContext(ioDispatcher) {
        dao.deleteReminder(reminder)
    }

    override suspend fun deleteAllData() = withContext(ioDispatcher) {
        dao.clearAllDataForDev()
    }
}
