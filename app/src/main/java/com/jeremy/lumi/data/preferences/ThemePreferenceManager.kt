package com.jeremy.lumi.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.graphics.Color
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.BackgroundAmbient
import com.jeremy.lumi.ui.theme.DialStyle
import com.jeremy.lumi.ui.theme.PhaseColorPalette
import com.jeremy.lumi.ui.theme.PhaseColors
import com.jeremy.lumi.ui.theme.PhaseDefaultLight
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lumi_prefs")

@Singleton
class ThemePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("selected_theme")
        // Paleta por defecto si nunca se ha guardado nada
        private val DEFAULT_THEME = AppThemePalette.NOIR


        // ── Colores de fase ────────────────────────────────────────────────
        private val PHASE_PALETTE_KEY = stringPreferencesKey("phase_color_palette")
        private val DEFAULT_PHASE_PALETTE = PhaseColorPalette.AUTO_HARMONY

        // ── Estilo del aro (Home dial) ─────────────────────────────────────
        private val DIAL_STYLE_KEY = stringPreferencesKey("home_dial_style")
        private val DEFAULT_DIAL_STYLE = DialStyle.GRADIENT

        // ── Ambiente de fondo ───────────────────────────────────────────────
        private val AMBIENT_KEY = stringPreferencesKey("background_ambient")
        private val DEFAULT_AMBIENT = BackgroundAmbient.FLAT

        // Overrides individuales — solo se leen cuando PHASE_PALETTE_KEY == CUSTOM.
        // Se guardan como Long (ARGB) porque Color no es serializable directamente.
        private val CUSTOM_MENSTRUAL_KEY  = longPreferencesKey("phase_custom_menstrual")
        private val CUSTOM_FOLLICULAR_KEY = longPreferencesKey("phase_custom_follicular")
        private val CUSTOM_OVULATION_KEY  = longPreferencesKey("phase_custom_ovulation")
        private val CUSTOM_LUTEAL_KEY     = longPreferencesKey("phase_custom_luteal")
    }

    // ── Tema general de la app ────────────────────────────────────────────────

    val selectedThemeFlow: Flow<AppThemePalette> = context.dataStore.data
        .map { prefs ->
            val saved = prefs[THEME_KEY]
            val resolved = runCatching { AppThemePalette.valueOf(saved ?: DEFAULT_THEME.name) }
                .getOrDefault(DEFAULT_THEME)
            if (resolved == AppThemePalette.PETALO) AppThemePalette.PETALO else AppThemePalette.NOIR
        }

    suspend fun saveTheme(theme: AppThemePalette) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }

    // ── Estilo de aro (DialStyle) ─────────────────────────────────────────────

    val dialStyleFlow: Flow<DialStyle> = context.dataStore.data
        .map { prefs ->
            val saved = prefs[DIAL_STYLE_KEY]
            runCatching { DialStyle.valueOf(saved ?: DEFAULT_DIAL_STYLE.name) }
                .getOrDefault(DEFAULT_DIAL_STYLE)
        }

    suspend fun saveDialStyle(style: DialStyle) {
        context.dataStore.edit { prefs ->
            prefs[DIAL_STYLE_KEY] = style.name
        }
    }

    // ── Ambiente de fondo ─────────────────────────────────────────────────

    val backgroundAmbientFlow: Flow<BackgroundAmbient> = context.dataStore.data
        .map { prefs ->
            val saved = prefs[AMBIENT_KEY]
            val resolved = runCatching { BackgroundAmbient.valueOf(saved ?: DEFAULT_AMBIENT.name) }
                .getOrDefault(DEFAULT_AMBIENT)
            if (resolved == BackgroundAmbient.CELESTIAL) BackgroundAmbient.CELESTIAL else BackgroundAmbient.FLAT
        }

    suspend fun saveBackgroundAmbient(ambient: BackgroundAmbient) {
        context.dataStore.edit { prefs ->
            prefs[AMBIENT_KEY] = ambient.name
        }
    }

    // ── Paleta de colores de fase ───────────────────────────────────────────

    val selectedPhasePaletteFlow: Flow<PhaseColorPalette> = context.dataStore.data
        .map { prefs ->
            val saved = prefs[PHASE_PALETTE_KEY]
            val resolved = runCatching { PhaseColorPalette.valueOf(saved ?: DEFAULT_PHASE_PALETTE.name) }
                .getOrDefault(DEFAULT_PHASE_PALETTE)
            when (resolved) {
                PhaseColorPalette.PASTEL -> PhaseColorPalette.PASTEL
                PhaseColorPalette.MINIMALISTA -> PhaseColorPalette.MINIMALISTA
                else -> PhaseColorPalette.AUTO_HARMONY
            }
        }

    suspend fun savePhasePalette(palette: PhaseColorPalette) {
        context.dataStore.edit { prefs ->
            prefs[PHASE_PALETTE_KEY] = palette.name
        }
    }

    // Colores custom — null si la usuaria no ha guardado nunca un set personalizado.
    // Cuando selectedPhasePaletteFlow emite CUSTOM, este flow es el que se debe usar.
    val customPhaseColorsFlow: Flow<PhaseColors?> = context.dataStore.data
        .map { prefs ->
            val m = prefs[CUSTOM_MENSTRUAL_KEY]
            val f = prefs[CUSTOM_FOLLICULAR_KEY]
            val o = prefs[CUSTOM_OVULATION_KEY]
            val l = prefs[CUSTOM_LUTEAL_KEY]
            if (m == null || f == null || o == null || l == null) {
                null
            } else {
                PhaseColors(
                    menstrual  = Color(m.toULong()),
                    follicular = Color(f.toULong()),
                    ovulation  = Color(o.toULong()),
                    luteal     = Color(l.toULong())
                )
            }
        }

    /**
     * Guarda un set completo de colores personalizados y activa el preset CUSTOM
     * automáticamente — así la pantalla de Settings no tiene que orquestar dos
     * llamadas separadas cada vez que la usuaria toca un color.
     */
    suspend fun saveCustomPhaseColors(colors: PhaseColors) {
        context.dataStore.edit { prefs ->
            prefs[CUSTOM_MENSTRUAL_KEY]  = colors.menstrual.value.toLong()
            prefs[CUSTOM_FOLLICULAR_KEY] = colors.follicular.value.toLong()
            prefs[CUSTOM_OVULATION_KEY]  = colors.ovulation.value.toLong()
            prefs[CUSTOM_LUTEAL_KEY]     = colors.luteal.value.toLong()
            prefs[PHASE_PALETTE_KEY]     = PhaseColorPalette.CUSTOM.name
        }
    }

    /**
     * Cambia solo el color de UNA fase, manteniendo las otras tres tal cual estén
     * (toma como base la paleta actualmente activa si todavía no hay custom guardado).
     * Útil para el flujo "personalización individual" donde se toca una fase a la vez.
     */
    suspend fun saveSinglePhaseColor(
        phase: PhaseSlot,
        color: Color,
        currentColors: PhaseColors
    ) {
        val updated = when (phase) {
            PhaseSlot.MENSTRUAL  -> currentColors.copy(menstrual  = color)
            PhaseSlot.FOLLICULAR -> currentColors.copy(follicular = color)
            PhaseSlot.OVULATION  -> currentColors.copy(ovulation  = color)
            PhaseSlot.LUTEAL     -> currentColors.copy(luteal     = color)
        }
        saveCustomPhaseColors(updated)
    }
}

enum class PhaseSlot { MENSTRUAL, FOLLICULAR, OVULATION, LUTEAL }