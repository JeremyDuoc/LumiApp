package com.jeremy.lumi.di

import com.jeremy.lumi.ai.CycleAIPredictor
import com.jeremy.lumi.ai.LumiAIPredictor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para la capa de IA (LiteRT / TFLite).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {

    @Binds
    @Singleton
    abstract fun bindCycleAIPredictor(
        predictor: LumiAIPredictor
    ): CycleAIPredictor
}
