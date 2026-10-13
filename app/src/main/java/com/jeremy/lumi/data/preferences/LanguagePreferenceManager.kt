package com.jeremy.lumi.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguagePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("app_language")
        const val SYSTEM_DEFAULT = "system"
        const val SPANISH = "es"
        const val ENGLISH = "en"
    }

    val selectedLanguageFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[LANGUAGE_KEY] ?: SYSTEM_DEFAULT }

    suspend fun getSelectedLanguage(): String {
        return context.dataStore.data.map { it[LANGUAGE_KEY] ?: SYSTEM_DEFAULT }.firstOrNull() ?: SYSTEM_DEFAULT
    }

    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = languageCode
        }
    }
}
