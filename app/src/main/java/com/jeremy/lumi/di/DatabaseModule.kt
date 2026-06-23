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
            // Estamos en desarrollo: sin usuarias reales todavía, así que
            // priorizamos simplicidad sobre preservar datos de prueba.
            // fallbackToDestructiveMigration() borra y recrea la base cada
            // vez que sube la versión del esquema — no se necesitan
            // Migration objects mientras estemos en esta etapa.
            // ANTES DE PRODUCCIÓN: reemplazar esto por migraciones reales
            // (.addMigrations(...)) para no perder los datos de las usuarias.
            .fallbackToDestructiveMigration()
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
