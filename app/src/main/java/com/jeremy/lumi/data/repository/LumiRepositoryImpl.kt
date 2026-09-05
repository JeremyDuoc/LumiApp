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
        // FIX: Reemplazar suma fija de ms (falla en Daylight Saving Time) por java.time
        // FIX (bug de zona horaria): [startDate] llega normalizado a medianoche UTC
        // (convención de toda la app: QuickLog, Calendar, Onboarding). Interpretarlo
        // con la zona local corría la fecha un día hacia atrás en zonas UTC- (p. ej.
        // Chile), desplazando la ovulación predicha. Usamos UTC de forma consistente.
        val zone = java.time.ZoneId.of("UTC")
        val startLocalDate = java.time.Instant.ofEpochMilli(startDate).atZone(zone).toLocalDate()

        // La ovulación se predice como: fin_esperado - fase_lútea_estándar (14 días)
        val expectedEnd = startLocalDate.plusDays(cycleLength.toLong())
        val ovulationLocalDate = expectedEnd.minusDays(14)
        
        val ovulationDate = ovulationLocalDate.atStartOfDay(zone).toInstant().toEpochMilli()

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

    override suspend fun getCycleForDate(dateMs: Long): CycleEntity? {
        return dao.getCycleForDate(dateMs)
    }

    override suspend fun updateCycle(cycle: CycleEntity) {
        dao.updateCycle(cycle)
    }

    override suspend fun deleteCycle(cycleId: Int) {
        dao.deleteCycleById(cycleId)
    }

    override suspend fun undoLastPeriodStart(): Boolean {
        val activeCycle = dao.getCurrentActiveCycle() ?: return false
        val closedCycles = dao.getClosedCycles()
        val previousCycle = closedCycles.firstOrNull() ?: return false

        // 1. Reasignar cualquier log asociado al ciclo cancelado hacia el ciclo reabierto
        dao.reassignDailyLogs(sourceCycleId = activeCycle.id, targetCycleId = previousCycle.id)

        // 2. Eliminar el ciclo activo erróneo
        dao.deleteCycleById(activeCycle.id)

        // 3. Reabrir el ciclo previo (limpiando endDate)
        dao.updateCycle(previousCycle.copy(endDate = null))
        return true
    }

    override suspend fun saveDailyLogWithSymptoms(dailyLog: DailyLogEntity, symptoms: List<SymptomEntity>) {
        dao.upsertDailyLogWithSymptoms(dailyLog, symptoms)
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