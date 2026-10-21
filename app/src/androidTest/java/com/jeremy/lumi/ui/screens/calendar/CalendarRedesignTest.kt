package com.jeremy.lumi.ui.screens.calendar

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.local.entity.SymptomEntity
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.components.LumiWeekTimeline
import com.jeremy.lumi.ui.screens.home.CycleDayUi
import com.jeremy.lumi.ui.theme.*
import com.jeremy.lumi.utils.LumiClock
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

/** Uses synthetic entries, never the repository or the user's records. */
@RunWith(AndroidJUnit4::class)
class CalendarRedesignTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.of(2026, 10, 20)
    private val selected = mutableStateOf(today)
    private val mode = mutableStateOf(CalendarMode.MONTH)
    private val theme = mutableStateOf(AppThemePalette.NOIR)
    private val scale = mutableFloatStateOf(1f)
    private var editedDate: LocalDate? = null
    private var openedMonth: Int? = null

    private fun days(year: Int = 2026, month: Int = 10): List<CalendarDay> {
        val first = LocalDate.of(year, month, 1)
        val saved = DailyLogWithSymptoms(DailyLogEntity(cycleId = 0, date = 0, mood = "Calmada", painLevel = 3),
            listOf(SymptomEntity(dailyLogId = 0, name = "Dolor de cabeza", intensity = 2)))
        return List(first.dayOfWeek.value - 1) { CalendarDay(0, isEmptyOffset = true) } +
            (1..first.lengthOfMonth()).map {
                val date = first.withDayOfMonth(it)
                CalendarDay(it, when (it) {
                    in 16..20 -> CyclePhase.MENSTRUAL
                    in 24..28 -> CyclePhase.OVULATION
                    else -> CyclePhase.FOLLICULAR
                }, isToday = date == today, hasLog = it == 19 || it == 20, isPrediction = date > today,
                    isFuture = date > today, date = date, record = if (it == 19 || it == 20) saved else null)
            }
    }

    @Composable private fun Host() {
        LumiTheme(selectedTheme = theme.value) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale.floatValue)) {
                Box(Modifier.fillMaxSize().background(LumiThemeTokens.current.bg)) {
                    CalendarContent(
                        CalendarUiState(selectedDate = selected.value, days = days(), displayYear = 2026, displayMonth = 10),
                        mode.value, 2026, (1..12).map { "" to days(month = it) },
                        onModeChange = { mode.value = it }, onToday = { selected.value = today; mode.value = CalendarMode.MONTH },
                        onPrevious = {}, onNext = {}, onPickDate = {}, onSelectDate = { selected.value = it },
                        onSelectMonth = { openedMonth = it; mode.value = CalendarMode.MONTH }, onEdit = { editedDate = it },
                        onLegend = {}, onHistory = {}, onCycles = {}
                    )
                }
            }
        }
    }

    private fun scroll(tag: String) = compose.onNodeWithTag("calendar_scroll").performScrollToNode(hasTestTag(tag))
    private fun capture(name: String, tag: String? = null) {
        compose.waitForIdle()
        val file = File(context.getExternalFilesDir(null), "calendar-qa/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { (if (tag == null) compose.onRoot() else compose.onNodeWithTag(tag))
            .captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun selectionShowsDetailsBeforeEditingAndPreservesTheExactDate() {
        compose.setContent { Host() }
        compose.onNodeWithTag("calendar_day_2026-10-19").performClick().assertIsSelected()
        compose.runOnIdle { assertNull(editedDate) }
        scroll("calendar_day_detail")
        compose.onNodeWithText(context.getString(R.string.mood_calm)).assertExists()
        compose.onNodeWithTag("calendar_edit").performClick()
        compose.runOnIdle { assertEquals(LocalDate.of(2026, 10, 19), editedDate) }
        capture("selected-record")
    }

    @Test fun futureSelectionExplainsPredictionWithoutOfferingToLog() {
        compose.setContent { Host() }
        compose.onNodeWithTag("calendar_day_2026-10-25").performClick().assertIsSelected()
        scroll("calendar_day_detail")
        compose.onNodeWithText(context.getString(R.string.calendar_future_title)).assertExists()
        compose.onNodeWithTag("calendar_edit").assertDoesNotExist()
        capture("future-estimate")
    }

    @Test fun annualOverviewOpensTheChosenMonth() {
        compose.setContent { Host() }
        compose.onNodeWithText(context.getString(R.string.cal_mode_year)).performClick()
        scroll("calendar_month_10")
        capture("year-overview")
        compose.onNodeWithTag("calendar_month_10").performClick()
        compose.runOnIdle { assertEquals(9, openedMonth) }
        compose.onNodeWithTag("calendar_grid").assertIsDisplayed()
    }

    @Test fun themesAndLargeTextKeepTheSelectedDayActionReachable() {
        compose.setContent { Host() }
        for (palette in listOf(AppThemePalette.NOIR, AppThemePalette.CIRUELA, AppThemePalette.BOSQUE,
            AppThemePalette.INDIGO, AppThemePalette.PETALO, AppThemePalette.MARFIL,
            AppThemePalette.SALVIA, AppThemePalette.LAVANDA)) {
            compose.runOnIdle { theme.value = palette }
            capture("month-${palette.name.lowercase()}")
        }
        compose.runOnIdle { scale.floatValue = 1.6f }
        compose.onNodeWithTag("calendar_edit").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(today, editedDate) }
        capture("large-text")
    }

    @Test fun weekCanReturnToTodayAfterBrowsingAndOpensSelectedDate() {
        val actualToday = LumiClock.today()
        val monday = actualToday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
        val weekSelection = mutableStateOf<LocalDate?>(null)
        var openedDate: LocalDate? = null
        val weekDays = (0L..20L).map {
            val date = monday.plusDays(it)
            CycleDayUi(date, date.dayOfMonth, "", CyclePhase.FOLLICULAR, date == actualToday,
                isPrediction = date > actualToday, hasLog = date == actualToday)
        }
        compose.setContent {
            LumiTheme(selectedTheme = AppThemePalette.NOIR) {
                Column(Modifier.fillMaxWidth().background(LumiThemeTokens.current.bg).padding(vertical = 24.dp)) {
                    LumiWeekTimeline(weekDays, weekSelection.value,
                        onSelectDate = { weekSelection.value = it }, onViewCalendarClick = {},
                        onResetToToday = { weekSelection.value = null }, onOpenDay = { openedDate = it })
                }
            }
        }
        capture("home-week", "home_week")
        compose.onNodeWithContentDescription(context.getString(R.string.calendar_next_week)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("week_today").assertExists().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("calendar_day_$actualToday").assertIsSelected()
        compose.onNodeWithTag("week_open_day").performClick()
        compose.runOnIdle { assertEquals(actualToday, openedDate) }
        val other = actualToday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .let { if (it == actualToday) it.plusDays(1) else it }
        compose.onNodeWithTag("calendar_day_$other").performClick().assertIsSelected()
        compose.onNodeWithTag("week_open_day").performClick()
        compose.runOnIdle { assertEquals(other, openedDate) }
    }
}
