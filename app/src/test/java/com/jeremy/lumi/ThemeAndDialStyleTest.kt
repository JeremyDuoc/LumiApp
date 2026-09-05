package com.jeremy.lumi

import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.DialStyle
import com.jeremy.lumi.ui.theme.PhaseColorPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeAndDialStyleTest {

    @Test
    fun testDialStyleEntries() {
        val entries = DialStyle.entries
        assertEquals(3, entries.size)
        assertTrue(entries.contains(DialStyle.GRADIENT))
        assertTrue(entries.contains(DialStyle.CYCLE_MAP))
        assertTrue(entries.contains(DialStyle.MINIMALIST))
    }

    @Test
    fun testAutoHarmonyResolvesForDifferentThemes() {
        val oceanColors = PhaseColorPalette.AUTO_HARMONY.toPhaseColors(
            isDark = false,
            currentTheme = AppThemePalette.LUMI_OCEAN
        )
        assertNotNull(oceanColors.menstrual)
        assertNotNull(oceanColors.follicular)
        assertNotNull(oceanColors.ovulation)
        assertNotNull(oceanColors.luteal)

        val forestDarkColors = PhaseColorPalette.AUTO_HARMONY.toPhaseColors(
            isDark = true,
            currentTheme = AppThemePalette.LUMI_FOREST_DARK
        )
        assertNotNull(forestDarkColors.menstrual)
        assertNotNull(forestDarkColors.follicular)
        assertNotNull(forestDarkColors.ovulation)
        assertNotNull(forestDarkColors.luteal)
    }

    @Test
    fun testAllPhasePalettesResolveWithoutCrash() {
        for (palette in PhaseColorPalette.entries) {
            val light = palette.toPhaseColors(isDark = false, currentTheme = AppThemePalette.LUMI_SPARK)
            val dark = palette.toPhaseColors(isDark = true, currentTheme = AppThemePalette.LUMI_SPARK_DARK)
            assertNotNull(light)
            assertNotNull(dark)
        }
    }
}
