package com.jeremy.lumi.di

import com.jeremy.lumi.data.repository.InsightsRepositoryImpl
import com.jeremy.lumi.domain.repository.InsightsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InsightsModule {

    @Binds
    @Singleton
    abstract fun bindInsightsRepository(
        impl: InsightsRepositoryImpl
    ): InsightsRepository
}
