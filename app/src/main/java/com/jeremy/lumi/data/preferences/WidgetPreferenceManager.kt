package com.jeremy.lumi.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Explicit consent for revealing cycle information on the device launcher.
 * Defaults to false even if an older widget was installed.
 * Backups intentionally do not include this local device consent.
 */
@Singleton
class WidgetPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val showKey = booleanPreferencesKey("widget_reveal_cycle_details_v2")

    val revealCycleDetails: Flow<Boolean> = context.dataStore.data.map { it[showKey] ?: false }

    suspend fun setRevealCycleDetails(reveal: Boolean) {
        context.dataStore.edit { it[showKey] = reveal }
    }
}
