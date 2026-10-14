package com.jeremy.lumi.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideChatStringResolver(@ApplicationContext context: Context): com.jeremy.lumi.domain.usecase.ChatStringResolver =
        com.jeremy.lumi.domain.usecase.AndroidChatStringResolver(context)
}