package com.jeremy.lumi.ui.screens.calendar

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.local.entity.SymptomEntity
import com.jeremy.lumi.data.preferences.DefaultLogFavorites
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.LumiTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real Compose interactions; the test host does not read or change the user's health records. */
@RunWith(AndroidJUnit4::class)
class DailyLogSheetTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun label(id: Int) = context.getString(id)
    private var savedSymptoms: Map<String, Int>? = null
    private var savedMood: String? = null
    private var savedPain: Int? = null
    private var dismissals = 0
    private val saving = mutableStateOf(false)
    private val failed = mutableStateOf(false)
    private val favorites = mutableStateOf(DefaultLogFavorites)
    private val custom = mutableStateOf(emptySet<String>())
    private val theme = mutableStateOf(AppThemePalette.NOIR)
    private val fontScale = mutableFloatStateOf(1f)

    @Composable
    private fun Host(saved: DailyLogWithSymptoms? = null) {
        LumiTheme(selectedTheme = theme.value) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.floatValue)) {
                DailyLogSheet(
                    day = 20, month = 10, year = 2026, savedLog = saved,
                    activeCategories = setOf("mucus", "intercourse"), onActiveCategoriesChange = {},
                    onDismiss = { dismissals++ }, isSaving = saving.value, saveFailed = failed.value,
                    customSymptoms = custom.value, onCustomSymptomsChange = { custom.value = it },
                    favoriteSymptoms = favorites.value, onFavoriteSymptomsChange = { favorites.value = it },
                    onSave = { _, pain, mood, _, _, _, _, _, _, _, _, _, _, _, _, _, _, intensities ->
                        savedMood = mood
                        savedPain = pain
                        savedSymptoms = intensities
                    }
                )
            }
        }
    }

    private fun scrollTo(id: Int) {
        compose.onNodeWithTag("daily_scroll").performScrollToNode(hasText(label(id)))
    }

    @Test fun oneMoodIsEnoughToSaveAndDraftSurvivesRestoration() {
        val restore = StateRestorationTester(compose)
        restore.setContent { Host() }
        compose.onNodeWithTag("daily_save").assertIsNotEnabled()
        compose.onNodeWithText(label(R.string.mood_calm)).performClick().assertIsSelected()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(label(R.string.mood_calm)).assertIsSelected()
        compose.onNodeWithTag("daily_save").performClick()
        compose.runOnIdle {
            assertEquals("Calmada", savedMood)
            assertEquals(emptyMap<String, Int>(), savedSymptoms)
        }
    }

    @Test fun editingCanRemoveCustomSymptomsWithoutLosingOtherIntensities() {
        val saved = DailyLogWithSymptoms(
            DailyLogEntity(id = 5, cycleId = 1, date = 0, painLevel = 7),
            listOf(SymptomEntity(dailyLogId = 5, name = "Dolor de cabeza", intensity = 3),
                SymptomEntity(dailyLogId = 5, name = "Mi síntoma", intensity = 9))
        )
        compose.setContent { Host(saved) }
        scrollTo(R.string.daily_selected)
        compose.onNodeWithContentDescription(context.getString(R.string.daily_remove, "Mi síntoma"))
            .performScrollTo().performClick()
        compose.onNodeWithTag("daily_save").performClick()
        compose.runOnIdle {
            assertEquals(mapOf("Dolor de cabeza" to 3), savedSymptoms)
            assertEquals(7, savedPain)
        }
    }

    @Test fun customSymptomCanBeCreatedSelectedAndGivenItsOwnIntensity() {
        compose.setContent { Host() }
        scrollTo(R.string.daily_all_symptoms)
        compose.onNodeWithText(label(R.string.daily_all_symptoms)).performClick()
        compose.onNodeWithText(label(R.string.daily_custom_add)).performClick()
        compose.onNodeWithText(label(R.string.daily_custom_name)).performTextInput("Mi sensación")
        compose.onNodeWithText(label(R.string.daily_custom_save)).performClick()
        compose.onNodeWithText(label(R.string.daily_done)).performClick()
        scrollTo(R.string.daily_add_intensity)
        compose.onNodeWithText(label(R.string.daily_add_intensity)).performClick()
        compose.onNode(hasProgressBarRangeInfo(androidx.compose.ui.semantics.ProgressBarRangeInfo(0f, 0f..10f, 9)))
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(8f) }
        compose.onNodeWithText(label(R.string.daily_done)).performClick()
        compose.onNodeWithTag("daily_save").performClick()
        compose.runOnIdle {
            assertEquals(setOf("Mi sensación"), custom.value)
            assertEquals(mapOf("Mi sensación" to 8), savedSymptoms)
        }
    }

    @Test fun favoritePickerDoesNotLogSymptomsAndUnsavedCloseCanBeCancelled() {
        compose.setContent { Host() }
        compose.onNodeWithTag("daily_customize").performClick()
        compose.onNodeWithText(label(R.string.daily_favorites_edit)).performClick()
        compose.onNodeWithTag("daily_catalog_scroll").performScrollToNode(hasText(label(R.string.symp_migraine)))
        compose.onNodeWithText(label(R.string.symp_migraine)).performClick()
        compose.onNodeWithText(label(R.string.daily_done)).performClick()
        compose.onNodeWithTag("daily_save").assertIsNotEnabled()
        compose.runOnIdle { assertTrue("Migraña" in favorites.value) }
        compose.onNodeWithText(label(R.string.mood_happy)).performClick()
        compose.onNodeWithTag("daily_close").performClick()
        compose.onNodeWithText(label(R.string.daily_keep)).performClick()
        compose.onNodeWithText(label(R.string.mood_happy)).assertIsSelected()
        compose.runOnIdle { assertEquals(0, dismissals) }
    }

    @Test fun saveFailureKeepsDraftAndSavingPreventsDuplicateSubmission() {
        compose.setContent { Host() }
        compose.onNodeWithText(label(R.string.mood_happy)).performClick()
        compose.runOnIdle { failed.value = true }
        compose.onNodeWithText(label(R.string.daily_save_error)).assertIsDisplayed()
        compose.onNodeWithTag("daily_save").assertIsEnabled()
        compose.onNodeWithText(label(R.string.mood_happy)).assertIsSelected()
        compose.runOnIdle { saving.value = true }
        compose.onNodeWithTag("daily_save").assertIsNotEnabled()
        compose.onNodeWithTag("daily_close").assertIsNotEnabled()
    }

    @Test fun visualEvidenceAcrossThemesAndLargeText() {
        compose.setContent { Host() }
        val themes = listOf(AppThemePalette.NOIR, AppThemePalette.CIRUELA, AppThemePalette.BOSQUE,
            AppThemePalette.INDIGO, AppThemePalette.PETALO, AppThemePalette.MARFIL,
            AppThemePalette.SALVIA, AppThemePalette.LAVANDA)
        themes.forEach {
            compose.runOnIdle { theme.value = it }
            capture("daily_${it.name.lowercase()}")
        }
        compose.runOnIdle { theme.value = AppThemePalette.NOIR }
        scrollTo(R.string.daily_all_symptoms)
        capture("daily_symptoms")
        compose.onNodeWithText(label(R.string.symp_cramps)).performClick()
        capture("daily_selected")
        scrollTo(R.string.daily_wellbeing)
        compose.onNodeWithText(label(R.string.daily_wellbeing)).performClick()
        scrollTo(R.string.daily_sleep)
        capture("daily_details")
        compose.onNodeWithTag("daily_scroll").performScrollToIndex(0)
        compose.onNodeWithTag("daily_customize").performClick()
        capture("daily_customize")
        compose.onNodeWithText(label(R.string.daily_favorites_edit)).performClick()
        capture("daily_catalog")
        compose.onNodeWithText(label(R.string.daily_done)).performClick()
        compose.runOnIdle { fontScale.floatValue = 1.6f }
        compose.onNodeWithTag("daily_scroll").performScrollToIndex(0)
        capture("daily_large_text")
        scrollTo(R.string.mood_calm)
        compose.onNodeWithText(label(R.string.mood_calm)).performClick()
        compose.onNodeWithTag("daily_save").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("Calmada", savedMood) }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onNodeWithTag("daily_editor").captureToImage().asAndroidBitmap()
        val folder = File(context.getExternalFilesDir(null), "daily-log-qa").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
