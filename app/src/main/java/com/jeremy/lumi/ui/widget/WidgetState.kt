package com.jeremy.lumi.ui.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Render-only values, never used as the source of medical truth. */
object LumiWidgetPrefs {
    val REVEALED = booleanPreferencesKey("v2_revealed")
    val HAS_CYCLE = booleanPreferencesKey("v2_has_cycle")
    val IS_PREGNANT = booleanPreferencesKey("v2_pregnant")
    val DAY = intPreferencesKey("v2_day")
    val PHASE = stringPreferencesKey("v2_phase")
    val NEXT = intPreferencesKey("v2_next_days")
    val NEXT_VALID = booleanPreferencesKey("v2_next_valid")
    val PROGRESS = intPreferencesKey("v2_progress")
    val WEEK = stringPreferencesKey("v2_week")
    val DATE = stringPreferencesKey("v2_date")
    val THEME = stringPreferencesKey("v2_theme")
}

enum class LumiWidgetKind { CYCLE, WEEK, QUIET }
