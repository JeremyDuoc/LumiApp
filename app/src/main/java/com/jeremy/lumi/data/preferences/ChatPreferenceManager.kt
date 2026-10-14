package com.jeremy.lumi.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.chatDataStore by preferencesDataStore(name = "chat_prefs")

@Singleton
class ChatPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SAVE_REMINDERS = booleanPreferencesKey("save_reminders_in_chat")
        private val LAST_INSIGHT_DATE = stringPreferencesKey("last_insight_date")
    }

    private val safeData: Flow<androidx.datastore.preferences.core.Preferences> =
        context.chatDataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val saveRemindersFlow: Flow<Boolean> = safeData.map { it[SAVE_REMINDERS] ?: true }

    suspend fun setSaveReminders(enabled: Boolean) {
        context.chatDataStore.edit { it[SAVE_REMINDERS] = enabled }
    }

    val lastInsightDateFlow: Flow<String> = safeData.map { it[LAST_INSIGHT_DATE] ?: "" }

    suspend fun setLastInsightDate(dateStr: String) {
        context.chatDataStore.edit { it[LAST_INSIGHT_DATE] = dateStr }
    }

    /** Limpia todas las preferencias del chat (usado en el reset total de datos). */
    suspend fun clearAllPreferences() {
        context.chatDataStore.edit { it.clear() }
    }
}
