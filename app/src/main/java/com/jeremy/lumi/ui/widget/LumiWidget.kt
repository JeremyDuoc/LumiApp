package com.jeremy.lumi.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.appWidgetBackgroundCornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jeremy.lumi.MainActivity
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.LumiDesignTokens
import com.jeremy.lumi.ui.theme.resolveLumiTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun Preferences.widgetPalette(): LumiDesignTokens {
    val key = this[LumiWidgetPrefs.THEME] ?: "NOIR"
    val palette = runCatching { AppThemePalette.valueOf(key) }.getOrDefault(AppThemePalette.NOIR)
    return resolveLumiTokens(palette)
}

private fun textStyle(color: Color, size: Int, bold: Boolean = false): TextStyle =
    TextStyle(
        color = ColorProvider(color),
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )

@Composable
private fun LumiWidgetFrame(
    palette: LumiDesignTokens,
    inset: Int = 16,
    content: @Composable () -> Unit
) {
    Box(
        modifier = GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            .background(palette.panel)
            .appWidgetBackgroundCornerRadius()
            .clickable(actionStartActivity<MainActivity>())
            .padding(inset.dp)
    ) { content() }
}

@Composable
private fun LumiWidgetBrand(palette: LumiDesignTokens, compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(R.drawable.lumi_widget_flower),
            contentDescription = null,
            modifier = GlanceModifier.size(if (compact) 15.dp else 19.dp)
        )
        Spacer(GlanceModifier.width(6.dp))
        Text("lumi", style = textStyle(palette.text, if (compact) 13 else 16, true))
    }
}

@Composable
private fun PrivacyPrompt(palette: LumiDesignTokens, context: Context) {
    Column {
        Spacer(GlanceModifier.height(13.dp))
        Text(context.getString(R.string.widget_hidden_title), style = textStyle(palette.text, 16, true))
        Spacer(GlanceModifier.height(5.dp))
        Text(context.getString(R.string.widget_hidden_desc), style = textStyle(palette.muted, 11))
    }
}

private fun phaseName(context: Context, name: String): String = when (name) {
    CyclePhase.MENSTRUAL.name -> context.getString(R.string.pm_phase_menstrual)
    CyclePhase.FOLLICULAR.name -> context.getString(R.string.pm_phase_follicular)
    CyclePhase.OVULATION.name -> context.getString(R.string.pm_phase_ovulation)
    CyclePhase.LUTEAL.name -> context.getString(R.string.pm_phase_luteal)
    CyclePhase.PREGNANCY.name -> context.getString(R.string.phase_pregnancy)
    else -> context.getString(R.string.phase_unknown)
}

private fun progressSegments(progress: Int): Int = when {
    progress < 25 -> 1
    progress < 50 -> 2
    progress < 75 -> 3
    else -> 4
}

/** 2x2 / 4x2: editorial compact summary, no unsupported pregnancy/contraception claims. */
class LumiWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(140.dp, 130.dp), DpSize(260.dp, 130.dp))
    )

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val colors = prefs.widgetPalette()
            val revealed = prefs[LumiWidgetPrefs.REVEALED] ?: false
            val cycle = prefs[LumiWidgetPrefs.HAS_CYCLE] ?: false
            val big = LocalSize.current.width >= 240.dp

            LumiWidgetFrame(colors) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    LumiWidgetBrand(colors)
                    if (!revealed) {
                        PrivacyPrompt(colors, LocalContext.current)
                    } else if (!cycle) {
                        Spacer(GlanceModifier.height(18.dp))
                        Text(context.getString(R.string.widget_empty_title), style = textStyle(colors.text, 16, true))
                        Spacer(GlanceModifier.height(5.dp))
                        Text(context.getString(R.string.widget_empty_desc), style = textStyle(colors.muted, 11))
                    } else if (prefs[LumiWidgetPrefs.IS_PREGNANT] == true) {
                        Spacer(GlanceModifier.height(16.dp))
                        Text(context.getString(R.string.widget_pregnancy_label), style = textStyle(colors.rose, 11, true))
                        Spacer(GlanceModifier.height(5.dp))
                        Text(context.getString(R.string.widget_pregnancy_title), style = textStyle(colors.text, 18, true))
                    } else {
                        Spacer(GlanceModifier.height(if (big) 10.dp else 7.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                context.getString(R.string.widget_day_number, prefs[LumiWidgetPrefs.DAY] ?: 1),
                                style = textStyle(colors.text, if (big) 27 else 25, true)
                            )
                            if (big) {
                                Spacer(GlanceModifier.width(16.dp))
                                Text(
                                    phaseName(context, prefs[LumiWidgetPrefs.PHASE] ?: ""),
                                    style = textStyle(colors.accent, 13, true)
                                )
                            }
                        }
                        if (!big) {
                            Text(
                                phaseName(context, prefs[LumiWidgetPrefs.PHASE] ?: ""),
                                style = textStyle(colors.accent, 11, true)
                            )
                        }
                        Spacer(GlanceModifier.height(9.dp))
                        Row {
                            val active = progressSegments(prefs[LumiWidgetPrefs.PROGRESS] ?: 0)
                            repeat(4) { segment ->
                                Box(
                                    modifier = GlanceModifier
                                        .width(if (big) 40.dp else 24.dp)
                                        .height(5.dp)
                                        .background(if (segment < active) colors.accent else colors.line)
                                ) {}
                                if (segment < 3) Spacer(GlanceModifier.width(4.dp))
                            }
                        }
                        if (big && prefs[LumiWidgetPrefs.NEXT_VALID] == true) {
                            Spacer(GlanceModifier.height(8.dp))
                            val next = prefs[LumiWidgetPrefs.NEXT] ?: 0
                            Text(
                                if (next >= 0) context.getString(R.string.widget_next_estimate, next)
                                else context.getString(R.string.widget_prediction_overdue),
                                style = textStyle(colors.muted, 11)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 4x2: small 7-day cycle overview. Uses the same cycle calculation as Home. */
class LumiWeekWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val colors = prefs.widgetPalette()
            LumiWidgetFrame(colors, 17) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LumiWidgetBrand(colors, true)
                        Spacer(GlanceModifier.width(14.dp))
                        Text(context.getString(R.string.widget_week_title), style = textStyle(colors.muted, 12))
                    }
                    val visible = prefs[LumiWidgetPrefs.REVEALED] == true
                    val hasCycle = prefs[LumiWidgetPrefs.HAS_CYCLE] == true
                    if (!visible) {
                        PrivacyPrompt(colors, context)
                    } else if (!hasCycle) {
                        Spacer(GlanceModifier.height(15.dp))
                        Text(context.getString(R.string.widget_empty_title), style = textStyle(colors.text, 16, true))
                    } else {
                        Spacer(GlanceModifier.height(16.dp))
                        val encoded = prefs[LumiWidgetPrefs.WEEK].orEmpty()
                        val days = encoded.split(",").mapNotNull { item ->
                            val segments = item.split(":")
                            if (segments.size == 2 && segments[0].toIntOrNull() != null)
                                segments[0] to segments[1] else null
                        }.take(7)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            days.forEachIndexed { index, (number, phase) ->
                                val shade = when (phase) {
                                    "M" -> colors.rose
                                    "O" -> colors.peach
                                    "F" -> colors.mint
                                    "L" -> colors.accent
                                    "P" -> colors.rose
                                    else -> colors.muted
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = GlanceModifier.size(30.dp)
                                            .background(if (index == 0) colors.panel2 else colors.panel),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(number, style = textStyle(if (index == 0) colors.text else colors.muted, 12, index == 0))
                                    }
                                    Spacer(GlanceModifier.height(7.dp))
                                    Box(modifier = GlanceModifier.width(23.dp).height(4.dp).background(shade)) {}
                                }
                                if (index < days.lastIndex) Spacer(GlanceModifier.width(8.dp))
                            }
                        }
                        Spacer(GlanceModifier.height(13.dp))
                        Text(context.getString(R.string.widget_estimation_note), style = textStyle(colors.muted, 10))
                    }
                }
            }
        }
    }
}

/** 2x1 discreet card: no cycle details ever, even when global permission is granted. */
class LumiQuietWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val colors = prefs.widgetPalette()
            LumiWidgetFrame(colors, 13) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.lumi_widget_flower),
                        contentDescription = null,
                        modifier = GlanceModifier.size(30.dp)
                    )
                    Spacer(GlanceModifier.width(11.dp))
                    Column {
                        Text("lumi", style = textStyle(colors.text, 18, true))
                        Spacer(GlanceModifier.height(2.dp))
                        Text(context.getString(R.string.widget_quiet_subtitle), style = textStyle(colors.muted, 11))
                    }
                }
            }
        }
    }
}
