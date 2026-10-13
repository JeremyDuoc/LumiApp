package com.jeremy.lumi.data.backup

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import com.jeremy.lumi.data.local.LumiDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/** Consistent snapshots; imports never overwrite files used by a live Room connection. */
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LumiDatabase
) {
    private val tables = listOf("cycles", "daily_logs", "symptoms", "reminders", "chat_messages")

    suspend fun exportDatabaseToZip(outputStream: OutputStream): Result<Unit> = archiveOperation { directory ->
        val snapshotFile = File(directory, "lumi_db")
        SQLiteDatabase.openOrCreateDatabase(snapshotFile, null).use { snapshot ->
            database.withTransaction {
                snapshot.beginTransaction()
                try {
                    val source = database.openHelper.writableDatabase
                    // Schema comes from our own database, never from an untrusted archive.
                    source.query("SELECT sql FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'").use { cursor ->
                        while (cursor.moveToNext()) snapshot.execSQL(cursor.getString(0))
                    }
                    for (table in tables + "room_master_table") {
                        source.query("SELECT * FROM `$table`").use { cursor ->
                            val sql = insertSql(table, cursor.columnNames.toList())
                            while (cursor.moveToNext()) snapshot.execSQL(sql, cursor.rowValues())
                        }
                    }
                    source.query("SELECT sql FROM sqlite_master WHERE type = 'index' AND sql IS NOT NULL").use { cursor ->
                        while (cursor.moveToNext()) snapshot.execSQL(cursor.getString(0))
                    }
                    snapshot.version = source.version
                    snapshot.setTransactionSuccessful()
                } finally {
                    snapshot.endTransaction()
                }
            }
        }
        ZipOutputStream(outputStream).use { zip ->
            zip.putNextEntry(ZipEntry("lumi_db"))
            snapshotFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }

    suspend fun importDatabaseFromZip(inputStream: InputStream): Result<Unit> = archiveOperation { directory ->
        val allowed = setOf("lumi_db", "lumi_db-wal", "lumi_db-shm")
        val found = mutableSetOf<String>()
        var extractedBytes = 0L
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.isDirectory || entry.name !in allowed || !found.add(entry.name)) {
                    throw CorruptBackupException("Archivo inesperado o duplicado en el respaldo")
                }
                File(directory, entry.name).outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var count = zip.read(buffer)
                    while (count != -1) {
                        extractedBytes += count
                        if (extractedBytes > 25L * 1024 * 1024) throw CorruptBackupException("Respaldo demasiado grande")
                        output.write(buffer, 0, count)
                        count = zip.read(buffer)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        if ("lumi_db" !in found) throw CorruptBackupException("El respaldo no contiene una base de datos")
        // Older archives may include a WAL; recovery happens only in this staging database.
        SQLiteDatabase.openDatabase(File(directory, "lumi_db").path, null, SQLiteDatabase.OPEN_READWRITE).use { source ->
            source.rawQuery("PRAGMA quick_check", null).use { cursor ->
                if (!cursor.moveToFirst() || cursor.getString(0) != "ok") throw CorruptBackupException("Base de datos dañada")
            }
            database.withTransaction {
                val target = database.openHelper.writableDatabase
                if (source.version != target.version) throw CorruptBackupException("Versión de base de datos incompatible")
                for (table in tables) {
                    val expected = target.query("PRAGMA table_info(`$table`)").use { it.columnSchema() }
                    val actual = source.rawQuery("PRAGMA table_info(`$table`)", null).use { it.columnSchema() }
                    if (expected.isEmpty() || actual != expected) throw CorruptBackupException("Esquema de respaldo incompatible")
                }
                // All tables (including reminders and chat) are replaced together, or rolled back.
                for (table in tables.asReversed()) target.execSQL("DELETE FROM `$table`")
                for (table in tables) {
                    source.rawQuery("SELECT * FROM `$table`", null).use { cursor ->
                        val statement = target.compileStatement(insertSql(table, cursor.columnNames.toList()))
                        try {
                            while (cursor.moveToNext()) {
                                statement.clearBindings()
                                cursor.rowValues().forEachIndexed { index, value ->
                                    when (value) {
                                        null -> statement.bindNull(index + 1)
                                        is Long -> statement.bindLong(index + 1, value)
                                        is Double -> statement.bindDouble(index + 1, value)
                                        is ByteArray -> statement.bindBlob(index + 1, value)
                                        else -> statement.bindString(index + 1, value.toString())
                                    }
                                }
                                statement.executeInsert()
                            }
                        } finally {
                            statement.close()
                        }
                    }
                }
            }
        }
    }

    private suspend fun archiveOperation(block: suspend (File) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        val directory = File.createTempFile("lumi-archive-", "", context.cacheDir)
        check(directory.delete() && directory.mkdir())
        try {
            block(directory)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun insertSql(table: String, columns: List<String>): String =
        "INSERT INTO `$table` (${columns.joinToString { "`$it`" }}) VALUES (${columns.joinToString { "?" }})"

    private fun Cursor.rowValues(): Array<Any?> = Array(columnCount) { index ->
        when (getType(index)) {
            Cursor.FIELD_TYPE_NULL -> null
            Cursor.FIELD_TYPE_INTEGER -> getLong(index)
            Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
            Cursor.FIELD_TYPE_BLOB -> getBlob(index)
            else -> getString(index)
        }
    }

    private fun Cursor.columnSchema(): List<List<String>> = buildList {
        while (moveToNext()) add(listOf(getString(1), getString(2), getInt(3).toString(), getInt(5).toString()))
    }
}
