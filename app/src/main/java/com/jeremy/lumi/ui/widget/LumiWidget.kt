package com.jeremy.lumi.ui.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.state.GlanceStateDefinition
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jeremy.lumi.MainActivity
import com.jeremy.lumi.R

object LumiWidgetPrefs {
    val CYCLE_DAY = intPreferencesKey("cycle_day")
    val CYCLE_PHASE = stringPreferencesKey("cycle_phase")
    val MESSAGE = stringPreferencesKey("message")
    val PHASE_COLOR = stringPreferencesKey("phase_color")
}

class LumiWidget : GlanceAppWidget() {
    
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val cycleDay = prefs[LumiWidgetPrefs.CYCLE_DAY] ?: 0
                val phase = prefs[LumiWidgetPrefs.CYCLE_PHASE] ?: "Sincronizando..."
                val message = prefs[LumiWidgetPrefs.MESSAGE] ?: "Abre la app para actualizar."
                val colorHex = prefs[LumiWidgetPrefs.PHASE_COLOR] ?: "#9B72C0" // Default Lumi color

                val primaryColor = try {
                    Color(android.graphics.Color.parseColor(colorHex))
                } catch (e: Exception) {
                    Color(0xFF9B72C0)
                }

                // Premium Card Design
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .padding(16.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Día actual
                        Text(
                            text = if (cycleDay > 0) "Día $cycleDay" else "—",
                            style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                        
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        
                        // Fase actual
                        Text(
                            text = phase.uppercase(),
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlanceTheme.colors.primary
                            )
                        )
                        
                        Spacer(modifier = GlanceModifier.height(12.dp))
                        
                        // Banner de mensaje
                        Box(
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.secondaryContainer)
                                .padding(8.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = message,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = GlanceTheme.colors.onSecondaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
