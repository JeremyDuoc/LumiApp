package com.jeremy.lumi.di

import com.jeremy.lumi.data.local.dao.LumiDao
import com.jeremy.lumi.data.repository.LumiRepositoryImpl
import com.jeremy.lumi.domain.repository.LumiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideLumiRepository(dao: LumiDao): LumiRepository {
        return LumiRepositoryImpl(dao)
    }
}