package com.jeremy.lumi.ui.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.data.preferences.ThemePreferenceManager
import com.jeremy.lumi.data.preferences.WidgetPreferenceManager
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.usecase.CyclePredictor
import com.jeremy.lumi.domain.usecase.GetEffectiveCyclePredictionUseCase
import com.jeremy.lumi.utils.LumiClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

internal data class WidgetSnapshot(
    val revealed: Boolean = false,
    val hasCycle: Boolean = false,
    val pregnant: Boolean = false,
    val day: Int = 0,
    val phase: String = "",
    val next: Int = 0,
    val nextValid: Boolean = false,
    val progress: Int = 0,
    val week: String = "",
    val date: String = "",
    val theme: String = "NOIR"
)

/** One source of truth for all launcher widgets. Never compute an independent cycle forecast. */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LumiRepository,
    private val prefs: OnboardingPreferenceManager,
    private val widgetPrefs: WidgetPreferenceManager,
    private val themePrefs: ThemePreferenceManager,
    private val effectivePrediction: GetEffectiveCyclePredictionUseCase
) {
    suspend fun refresh() {
        val manager = GlanceAppWidgetManager(context)
        val variants: List<GlanceAppWidget> = listOf(LumiWidget(), LumiWeekWidget(), LumiQuietWidget())
        val installed = variants.map { widget -> widget to manager.getGlanceIds(widget.javaClass) }
        if (installed.all { it.second.isEmpty() }) return

        val theme = themePrefs.selectedThemeFlow.first()
        val today = LumiClock.today()
        // Default private; observer-only installations also never reveal menstrual information.
        val revealed = WidgetPrivacyPolicy.canReveal(
            optedIn = widgetPrefs.revealCycleDetails.first(),
            completedOnboarding = prefs.isOnboardingCompleted.first(),
            observerOnly = prefs.isObserverOnly.first()
        )

        val snapshot = if (revealed) {
            val cycle = repository.getCurrentActiveCycle()
            if (cycle == null) {
                WidgetSnapshot(revealed = true, date = today.toString(), theme = theme.name)
            } else {
                val closed = repository.getClosedCycles()
                val pregnant = prefs.isPregnantFlow.first()
                val contraceptive = prefs.isOnContraceptiveFlow.first()
                val effective = effectivePrediction(
                    activeCycle = cycle,
                    closedCycles = closed,
                    isPregnant = pregnant,
                    isOnContraceptive = contraceptive,
                    age = prefs.ageFlow.first()?.toFloat(),
                    height = prefs.heightFlow.first(),
                    weight = prefs.weightFlow.first(),
                    targetDate = today
                )
                val prediction = effective.prediction
                val start = Instant.ofEpochMilli(cycle.startDate).atZone(ZoneOffset.UTC).toLocalDate()
                val day = prediction?.currentDayOfCycle
                    ?: (ChronoUnit.DAYS.between(start, today).toInt() + 1).coerceAtLeast(1)
                val phase = prediction?.currentPhase ?: CyclePhase.UNKNOWN
                val week = (0..6).joinToString(",") { offset ->
                    val target = today.plusDays(offset.toLong())
                    val forecast = CyclePredictor.phaseForDateInActiveCycle(
                        startDate = start,
                        cycleLength = effective.effectiveCycleLength,
                        periodLength = effective.periodLength,
                        date = target,
                        today = today,
                        isPregnant = pregnant,
                        isOnContraceptive = contraceptive
                    ).first
                    "${target.dayOfMonth}:${forecast.name.first()}"
                }
                WidgetSnapshot(
                    revealed = true,
                    hasCycle = true,
                    pregnant = pregnant,
                    day = day,
                    phase = phase.name,
                    next = prediction?.daysUntilNextPeriod ?: 0,
                    nextValid = !pregnant && prediction != null,
                    progress = ((day.coerceAtMost(effective.effectiveCycleLength) * 100) /
                        effective.effectiveCycleLength.coerceAtLeast(1)).coerceIn(0, 100),
                    week = week,
                    date = today.toString(),
                    theme = theme.name
                )
            }
        } else {
            // Overwrite all sensitive keys: switching privacy off clears cached launcher data.
            WidgetSnapshot(date = today.toString(), theme = theme.name)
        }

        installed.forEach { (widget, ids) ->
            ids.forEach { id ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { existing ->
                    existing.toMutablePreferences().apply { putSnapshot(snapshot) }
                }
                widget.update(context, id)
            }
        }
    }

    private fun MutablePreferences.putSnapshot(value: WidgetSnapshot) {
        this[LumiWidgetPrefs.REVEALED] = value.revealed
        this[LumiWidgetPrefs.HAS_CYCLE] = value.hasCycle
        this[LumiWidgetPrefs.IS_PREGNANT] = value.pregnant
        this[LumiWidgetPrefs.DAY] = value.day
        this[LumiWidgetPrefs.PHASE] = value.phase
        this[LumiWidgetPrefs.NEXT] = value.next
        this[LumiWidgetPrefs.NEXT_VALID] = value.nextValid
        this[LumiWidgetPrefs.PROGRESS] = value.progress
        this[LumiWidgetPrefs.WEEK] = value.week
        this[LumiWidgetPrefs.DATE] = value.date
        this[LumiWidgetPrefs.THEME] = value.theme
    }
}
