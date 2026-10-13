package com.jeremy.lumi.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy

class RoomMigrationDefinitionTest {

    @Test
    fun testMigrationsAreContiguousAndValid() {
        assertEquals(7, LumiDatabase.MIGRATION_7_8.startVersion)
        assertEquals(8, LumiDatabase.MIGRATION_7_8.endVersion)
        assertEquals(8, LumiDatabase.MIGRATION_8_9.startVersion)
        assertEquals(9, LumiDatabase.MIGRATION_8_9.endVersion)

        assertTrue(LumiDatabase.ALL_MIGRATIONS.contains(LumiDatabase.MIGRATION_7_8))
        assertTrue(LumiDatabase.ALL_MIGRATIONS.contains(LumiDatabase.MIGRATION_8_9))
    }

    @Test
    fun testMigration7To8ExecutesExpectedSql() {
        val executedSql = mutableListOf<String>()
        val fakeDb = createFakeSupportSQLiteDatabase(executedSql)

        LumiDatabase.MIGRATION_7_8.migrate(fakeDb)

        assertEquals(5, executedSql.size)
        assertTrue(executedSql.any { it.contains("spotting") && it.contains("daily_logs") })
        assertTrue(executedSql.any { it.contains("energyLevel") && it.contains("daily_logs") })
        assertTrue(executedSql.any { it.contains("sleepHours") && it.contains("daily_logs") })
        assertTrue(executedSql.any { it.contains("stressLevel") && it.contains("daily_logs") })
        assertTrue(executedSql.any { it.contains("basalBodyTemp") && it.contains("daily_logs") })
    }

    @Test
    fun testMigration8To9ExecutesExpectedSql() {
        val executedSql = mutableListOf<String>()
        val fakeDb = createFakeSupportSQLiteDatabase(executedSql)

        LumiDatabase.MIGRATION_8_9.migrate(fakeDb)

        assertEquals(1, executedSql.size)
        assertTrue(executedSql.first().contains("waterIntake") && executedSql.first().contains("daily_logs"))
    }

    @Test
    fun testSchemaExportFileExistsAndMatchesVersion9() {
        // Schema is exported into app/schemas by KSP
        val schemaFile = File("schemas/com.jeremy.lumi.data.local.LumiDatabase/9.json")
            .takeIf { it.exists() }
            ?: File("app/schemas/com.jeremy.lumi.data.local.LumiDatabase/9.json")

        assertTrue("Schema file 9.json must exist at ${schemaFile.absolutePath}", schemaFile.exists())

        val json = com.google.gson.JsonParser.parseString(schemaFile.readText()).asJsonObject
        val database = json.getAsJsonObject("database")
        assertEquals(9, database.get("version").asInt)

        val entities = database.getAsJsonArray("entities")
        val tableNames = entities.map { it.asJsonObject.get("tableName").asString }

        assertTrue(tableNames.contains("cycles"))
        assertTrue(tableNames.contains("daily_logs"))
        assertTrue(tableNames.contains("symptoms"))
        assertTrue(tableNames.contains("reminders"))
        assertTrue(tableNames.contains("chat_messages"))

        val dailyLogsEntity = entities
            .map { it.asJsonObject }
            .first { it.get("tableName").asString == "daily_logs" }

        val fields = dailyLogsEntity.getAsJsonArray("fields")
        val columnNames = fields.map { it.asJsonObject.get("columnName").asString }

        assertTrue(columnNames.contains("waterIntake"))
        assertTrue(columnNames.contains("spotting"))
        assertTrue(columnNames.contains("basalBodyTemp"))
    }

    private fun createFakeSupportSQLiteDatabase(executedSql: MutableList<String>): SupportSQLiteDatabase {
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase
    }
}
