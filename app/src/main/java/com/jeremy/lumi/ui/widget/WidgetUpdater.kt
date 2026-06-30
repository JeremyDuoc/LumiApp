package com.jeremy.lumi.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun updateWidget(day: Int, phase: String, message: String, colorHex: String) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(LumiWidget::class.java)
        
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[LumiWidgetPrefs.CYCLE_DAY] = day
                    this[LumiWidgetPrefs.CYCLE_PHASE] = phase
                    this[LumiWidgetPrefs.MESSAGE] = message
                    this[LumiWidgetPrefs.PHASE_COLOR] = colorHex
                }
            }
            LumiWidget().update(context, glanceId)
        }
    }
}
