package com.jeremy.lumi.di

import android.content.Context
import com.jeremy.lumi.ai.LumiAIPredictor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para la capa de IA (TFLite).
 *
 * LumiAIPredictor es @Singleton porque cargar el modelo .tflite en memoria
 * es una operación costosa (~5ms) que solo debe hacerse una vez.
 */
@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideLumiAIPredictor(
        @ApplicationContext context: Context
    ): LumiAIPredictor = LumiAIPredictor(context)
}
