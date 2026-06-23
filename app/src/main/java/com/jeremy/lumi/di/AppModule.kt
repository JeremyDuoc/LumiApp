package com.jeremy.lumi.di

import android.content.Context
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.data.preferences.ThemePreferenceManager
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
    fun provideThemePreferenceManager(@ApplicationContext context: Context): ThemePreferenceManager =
        ThemePreferenceManager(context)

    @Provides
    @Singleton
    fun provideOnboardingPreferenceManager(@ApplicationContext context: Context): OnboardingPreferenceManager =
        OnboardingPreferenceManager(context)
}