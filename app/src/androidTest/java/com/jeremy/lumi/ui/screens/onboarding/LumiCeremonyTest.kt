package com.jeremy.lumi.ui.screens.onboarding

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.theme.LumiTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Integration checks for the real frame-clock-driven overlay, not a copied timeline. */
@RunWith(AndroidJUnit4::class)
class LumiCeremonyTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fullCeremonyKeepsLastMessageReadableAndCompletesOnce() {
        var finished = 0
        var mounted = 0
        var revealed = 0
        var progress = 0f
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LumiTheme {
                LumiAtmosphericTransition(
                    reducedMotion = false,
                    onCurtainOpaque = { mounted++ },
                    onRevealStart = { revealed++ },
                    onRevealProgress = { progress = it },
                    onFinished = { finished++ }
                )
            }
        }
        compose.mainClock.advanceTimeBy(11000)
        val lastTitle = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.atmospheric_title_5)
        compose.onNodeWithText(lastTitle).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, mounted)
            assertEquals(0, revealed)
            assertEquals(0, finished)
        }
        compose.mainClock.advanceTimeBy(2000)
        compose.runOnIdle {
            assertEquals(1, revealed)
            assertTrue(progress > 0f && progress < 1f)
            assertEquals(0, finished)
        }
        compose.mainClock.advanceTimeBy(3000)
        compose.runOnIdle {
            assertEquals(1, finished)
            assertEquals(1f, progress)
        }
    }

    @Test fun slowHomeStaysCoveredAndResumesTheLandingWhenReady() {
        val ready = mutableStateOf(false)
        var revealed = 0
        var finished = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LumiTheme {
                LumiAtmosphericTransition(
                    reducedMotion = false,
                    homeReady = ready.value,
                    onRevealStart = { revealed++ },
                    onFinished = { finished++ }
                )
            }
        }
        compose.mainClock.advanceTimeBy(18000)
        compose.runOnIdle {
            assertEquals(0, revealed)
            assertEquals(0, finished)
            ready.value = true
        }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle {
            assertEquals(1, revealed)
            assertEquals(0, finished)
        }
        compose.mainClock.advanceTimeBy(2000)
        compose.runOnIdle { assertEquals(1, finished) }
    }

    @Test fun backAtTheBeginningStillMountsHomeBeforeFinishing() {
        val skip = mutableStateOf(false)
        val events = mutableListOf<String>()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LumiTheme {
                LumiAtmosphericTransition(
                    reducedMotion = false,
                    skipRequested = skip.value,
                    onCurtainOpaque = { events += "mounted" },
                    onRevealStart = { events += "revealed" },
                    onFinished = { events += "finished" }
                )
            }
        }
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle { skip.value = true }
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle { assertEquals(listOf("mounted"), events) }
        compose.mainClock.advanceTimeBy(3000)
        compose.runOnIdle { assertEquals(listOf("mounted", "revealed", "finished"), events) }
    }

    @Test fun reducedMotionWaitsForHomeWithoutPlayingTheCeremony() {
        val ready = mutableStateOf(false)
        var mounted = 0
        var finished = 0
        var progress = 0f
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LumiTheme {
                LumiAtmosphericTransition(
                    reducedMotion = true,
                    homeReady = ready.value,
                    onCurtainOpaque = { mounted++ },
                    onRevealProgress = { progress = it },
                    onFinished = { finished++ }
                )
            }
        }
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle {
            assertEquals(1, mounted)
            assertEquals(0, finished)
            ready.value = true
        }
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle {
            assertEquals(1, finished)
            assertEquals(1f, progress)
        }
    }

    @Test fun enterNowButtonUsesTheLandingInsteadOfAnAbruptCut() {
        var finished = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LumiTheme {
                LumiAtmosphericTransition(reducedMotion = false, onFinished = { finished++ })
            }
        }
        compose.mainClock.advanceTimeBy(2500)
        val enter = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.atmospheric_enter_now)
        compose.onNodeWithText(enter).performClick()
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle { assertEquals(0, finished) }
        compose.mainClock.advanceTimeBy(1800)
        compose.runOnIdle { assertEquals(1, finished) }
    }

    @Test fun lightThemeAndLargeTextKeepThePhraseAboveTheProgressLights() {
        val theme = mutableStateOf(LumiOnboardingTheme.NOIR)
        val fontScale = mutableStateOf(1f)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LumiTheme(selectedTheme = theme.value.appThemePalette) {
                CompositionLocalProvider(
                    LocalLumiOnboardingTokens provides getThemeTokensV7(theme.value),
                    LocalDensity provides Density(LocalDensity.current.density, fontScale.value)
                ) {
                    LumiAtmosphericTransition(reducedMotion = false, onFinished = {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(4200)
        saveScreenshot("ceremony-noir.png")
        compose.runOnIdle {
            theme.value = LumiOnboardingTheme.PETALO
            fontScale.value = 1.4f
        }
        compose.mainClock.advanceTimeBy(600)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val title = compose.onNodeWithText(context.getString(R.string.atmospheric_title_2))
        val subtitle = compose.onNodeWithText(context.getString(R.string.atmospheric_sub_2))
        title.assertIsDisplayed()
        subtitle.assertIsDisplayed()
        val lights = compose.onNodeWithTag("ceremony_progress")
        assertTrue(subtitle.fetchSemanticsNode().boundsInRoot.bottom < lights.fetchSemanticsNode().boundsInRoot.top)
        saveScreenshot("ceremony-petalo-large-text.png")
    }

    private fun saveScreenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = requireNotNull(context.getExternalFilesDir("transition-review"))
        File(directory, name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
