package com.jeremy.lumi

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeremy.lumi.data.backup.BackupManager
import com.jeremy.lumi.data.local.LumiDatabase
import com.jeremy.lumi.data.local.entity.*
import com.jeremy.lumi.data.repository.LumiRepositoryImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class DataIntegrityTest {
    private lateinit var db: LumiDatabase
    private lateinit var backups: BackupManager
    private val day = LocalDate.of(2026, 9, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, LumiDatabase::class.java).build()
        backups = BackupManager(context, db)
    }
    @After fun close() { db.close() }

    @Test fun concurrentDailySavesProduceOneLog() = runBlocking {
        coroutineScope {
            (1..12).map { value -> async {
                db.dao.upsertDailyLogWithSymptoms(DailyLogEntity(cycleId = 0, date = day, notes = "$value"),
                    listOf(SymptomEntity(dailyLogId = 0, name = "Test", intensity = 1)))
            } }.awaitAll()
        }
        assertEquals(1, db.dao.getAllDailyLogsSnapshot().size)
        assertEquals(1, db.dao.getAllSymptomsSnapshot().size)
    }

    @Test fun repeatedPeriodStartsAndUndoPreserveLogs() = runBlocking {
        val repository = LumiRepositoryImpl(db.dao)
        repository.startNewCycle(day, 28, 5)
        val firstId = db.dao.getCurrentActiveCycle()!!.id
        val nextDay = day + 28L * 86_400_000
        db.dao.upsertDailyLogWithSymptoms(DailyLogEntity(cycleId = firstId, date = nextDay), emptyList())
        coroutineScope { List(4) { async { repository.startNewCycle(nextDay, 28, 5) } }.awaitAll() }
        assertEquals(2, db.dao.getAllCyclesSnapshot().size)
        assertEquals(28, db.dao.getClosedCycles().single().closedLengthDays())
        assertEquals(db.dao.getCurrentActiveCycle()!!.id, db.dao.getDailyLogWithSymptoms(nextDay)!!.dailyLog.cycleId)
        assertTrue(repository.undoLastPeriodStart())
        assertEquals(firstId, db.dao.getCurrentActiveCycle()!!.id)
        assertEquals(firstId, db.dao.getDailyLogWithSymptoms(nextDay)!!.dailyLog.cycleId)
    }

    @Test fun zipRoundTripWorksWhileRoomStaysOpen() = runBlocking {
        db.dao.startCycleReplacingActive(CycleEntity(startDate = day, endDate = null, predictedOvulationDate = null))
        db.dao.upsertDailyLogWithSymptoms(DailyLogEntity(cycleId = db.dao.getCurrentActiveCycle()!!.id, date = day, notes = "Original"),
            listOf(SymptomEntity(dailyLogId = 0, name = "Test", intensity = 1)))
        db.chatDao.insertMessage(ChatMessageEntity(text = "Test message", messageType = ChatMessageType.USER))
        val reminderId = db.dao.insertReminder(ReminderEntity(type = ReminderType.CUSTOM, label = "Test reminder")).toInt()
        val before = db.dao.getHealthDataSnapshot()
        val bytes = ByteArrayOutputStream()
        backups.exportDatabaseToZip(bytes).getOrThrow()
        db.dao.clearAllDataForDev()
        backups.importDatabaseFromZip(bytes.toByteArray().inputStream()).getOrThrow()
        assertEquals(before, db.dao.getHealthDataSnapshot())
        assertEquals("Test message", db.chatDao.getAllMessages().first().single().text)
        assertEquals("Test reminder", db.dao.getReminderById(reminderId)!!.label)
        // The restored database remains writable without restarting the app.
        db.dao.upsertDailyLogWithSymptoms(DailyLogEntity(cycleId = 0, date = day + 86_400_000), emptyList())
        assertEquals(2, db.dao.getAllDailyLogsSnapshot().size)
    }

    @Test fun emptyOrCorruptArchivesDoNotModifyExistingData() = runBlocking {
        db.dao.upsertDailyLogWithSymptoms(DailyLogEntity(cycleId = 0, date = day, notes = "Keep"), emptyList())
        val bytes = ByteArrayOutputStream().also { ZipOutputStream(it).close() }.toByteArray()
        assertTrue(backups.importDatabaseFromZip(bytes.inputStream()).isFailure)
        assertTrue(backups.importDatabaseFromZip(byteArrayOf(1, 2, 3).inputStream()).isFailure)
        assertEquals("Keep", db.dao.getAllDailyLogsSnapshot().single().notes)
    }
}
