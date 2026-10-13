package com.jeremy.lumi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jeremy.lumi.data.local.dao.ChatDao
import com.jeremy.lumi.data.local.dao.LumiDao
import com.jeremy.lumi.data.local.entity.ChatMessageEntity
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.ReminderEntity
import com.jeremy.lumi.data.local.entity.SymptomEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CycleEntity::class,
        DailyLogEntity::class,
        SymptomEntity::class,
        ReminderEntity::class,
        ChatMessageEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class LumiDatabase : RoomDatabase() {
    abstract val dao: LumiDao
    abstract val chatDao: ChatDao

    companion object {
        /**
         * Migración de esquema versión 7 a 8:
         * Añade a la tabla `daily_logs` los campos de spotting, energyLevel,
         * sleepHours, stressLevel y basalBodyTemp.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_logs` ADD COLUMN `spotting` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_logs` ADD COLUMN `energyLevel` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `daily_logs` ADD COLUMN `sleepHours` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `daily_logs` ADD COLUMN `stressLevel` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `daily_logs` ADD COLUMN `basalBodyTemp` REAL DEFAULT NULL")
            }
        }

        /**
         * Migración de esquema versión 8 a 9:
         * Añade a la tabla `daily_logs` el campo waterIntake (litros de agua).
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_logs` ADD COLUMN `waterIntake` REAL DEFAULT NULL")
            }
        }

        /**
         * Colección de todas las migraciones registradas para LumiDatabase.
         */
        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_7_8,
            MIGRATION_8_9
        )
    }
}