package com.jeremy.lumi.di

import android.app.Application
import androidx.room.Room
import com.jeremy.lumi.data.local.LumiDatabase
import com.jeremy.lumi.data.local.dao.LumiDao
import com.jeremy.lumi.data.local.dao.ChatDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLumiDatabase(app: Application): LumiDatabase {
        return Room.databaseBuilder(
            app,
            LumiDatabase::class.java,
            "lumi_db"
        )
            // Migraciones explícitas para preservar los datos de salud de las usuarias
            // entre las versiones documentadas (7 → 8 → 9).
            .addMigrations(*LumiDatabase.ALL_MIGRATIONS)
            // Versiones pre-alfa / desarrollo iniciales (1..6) anteriores al historial de Git
            // se descartan destructivamente si se encontrasen, pero las versiones 7+
            // requieren migración explícita para evitar pérdida de datos accidental.
            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    @Singleton
    fun provideLumiDao(db: LumiDatabase): LumiDao {
        return db.dao
    }
    @Provides
    @Singleton
    fun provideChatDao(db: LumiDatabase): ChatDao = db.chatDao
}
