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

// Sin Migration objects por ahora: DatabaseModule usa
// fallbackToDestructiveMigration() mientras estamos en desarrollo.
// Cuando haya usuarias reales con datos que preservar, aquí es donde
// vuelven a vivir las Migration 1→2→3→4→5, bien encadenadas y probadas.
@Database(
    entities = [
        CycleEntity::class,
        DailyLogEntity::class,
        SymptomEntity::class,
        ReminderEntity::class,
        ChatMessageEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class LumiDatabase : RoomDatabase() {
    abstract val dao: LumiDao
    abstract val chatDao: ChatDao   // ← faltaba: DatabaseModule.provideChatDao() lo necesita
}