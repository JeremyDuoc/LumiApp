package com.jeremy.lumi.data.backup

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.SymptomEntity
import org.junit.Assert.*
import org.junit.Test

class BackupSerializerTest {
    private fun validJson() = BackupSerializer.toJson(
        listOf(CycleEntity(1, 100L, null, null)),
        listOf(DailyLogEntity(id = 1, cycleId = 1, date = 100L, notes = "Private test")),
        listOf(SymptomEntity(1, 1, "Test", 3))
    )
    private fun rejects(json: String) {
        assertThrows(CorruptBackupException::class.java) { BackupSerializer.fromJson(json) }
    }
    @Test fun `full snapshot survives round trip`() {
        val restored = BackupSerializer.fromJson(validJson())
        assertEquals(1, restored.cycles.size)
        assertEquals("Private test", restored.dailyLogs.single().notes)
        assertEquals("Test", restored.symptoms.single().name)
    }
    @Test fun `missing table is rejected before any restore`() {
        rejects("""{"formatVersion":1,"dbSchemaVersion":9}""")
    }
    @Test fun `incomplete row is not silently discarded`() {
        rejects(validJson().replace("\"startDate\":100", "\"unused\":100"))
    }
    @Test fun `null row and orphan symptoms are rejected`() {
        rejects("""{"formatVersion":1,"dbSchemaVersion":9,"cycles":[null],"dailyLogs":[],"symptoms":[]}""")
        rejects(validJson().replace("\"dailyLogId\":1", "\"dailyLogId\":999"))
    }
    @Test fun `duplicate days cannot silently overwrite history`() {
        rejects(BackupSerializer.toJson(emptyList(), listOf(
            DailyLogEntity(1, 0, 100), DailyLogEntity(2, 0, 100)
        ), emptyList()))
    }
    @Test fun `future schema is not imported by an older app`() {
        assertThrows(BackupVersionTooNewException::class.java) {
            BackupSerializer.fromJson(validJson().replace("\"dbSchemaVersion\":9", "\"dbSchemaVersion\":10"))
        }
    }
    @Test fun `explicitly empty backup remains valid`() {
        val restored = BackupSerializer.fromJson(BackupSerializer.toJson(emptyList(), emptyList(), emptyList()))
        assertTrue(restored.cycles.isEmpty())
    }
    @Test fun `legacy cloud payload without profile remains compatible`() {
        val restored = BackupSerializer.fromJson(validJson())
        assertNull(restored.profile)
    }

    @Test fun `new cloud backups restore nickname and cycle profile`() {
        val profile = ProfileBackup(
            userName = "Estrella",
            userGoal = "TRACK_CYCLE",
            defaultCycleLength = 31,
            defaultPeriodLength = 5,
            isPregnant = false,
            isOnContraceptive = false
        )
        val json = BackupSerializer.toJson(emptyList(), emptyList(), emptyList(), profile)
        val restored = BackupSerializer.fromJson(json)
        assertEquals("Estrella", restored.profile?.userName)
        assertEquals(31, restored.profile?.defaultCycleLength)
    }

    @Test fun `invalid cloud profile rejects whole payload`() {
        val profile = ProfileBackup(
            userName = "Estrella",
            userGoal = "TRACK_CYCLE",
            defaultCycleLength = 31,
            defaultPeriodLength = 5,
            isPregnant = false,
            isOnContraceptive = false
        )
        val json = BackupSerializer.toJson(emptyList(), emptyList(), emptyList(), profile)
        rejects(json.replace("\"defaultCycleLength\":31", "\"defaultCycleLength\":999"))
    }
}
