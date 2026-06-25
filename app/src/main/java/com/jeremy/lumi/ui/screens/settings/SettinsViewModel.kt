package com.jeremy.lumi.ui.screens.settings

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.preferences.ChatPreferenceManager
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.data.preferences.PhaseSlot
import com.jeremy.lumi.data.preferences.ThemePreferenceManager
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.domain.model.UserGoal
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.ui.theme.PhaseColorPalette
import com.jeremy.lumi.ui.theme.PhaseColors
import com.jeremy.lumi.ui.theme.PhaseDefaultLight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferenceManager: ThemePreferenceManager,
    private val chatPreferenceManager: ChatPreferenceManager,
    private val onboardingPreferenceManager: OnboardingPreferenceManager,
    private val repository: LumiRepository
) : ViewModel() {

    // ── Ajustes de Chat ────────────────────────────────────────────────────
    val saveRemindersInChat: StateFlow<Boolean> = chatPreferenceManager.saveRemindersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setSaveRemindersInChat(enabled: Boolean) {
        viewModelScope.launch { chatPreferenceManager.setSaveReminders(enabled) }
    }

    // ── Salud y Objetivos ──────────────────────────────────────────────────
    
    val userGoal: StateFlow<UserGoal> = onboardingPreferenceManager.userGoalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserGoal.TRACK_CYCLE)

    val isPregnant: StateFlow<Boolean> = onboardingPreferenceManager.isPregnantFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setUserGoal(goal: UserGoal) {
        viewModelScope.launch { onboardingPreferenceManager.setUserGoal(goal) }
    }

    fun setIsPregnant(isPregnant: Boolean) {
        viewModelScope.launch { onboardingPreferenceManager.setIsPregnant(isPregnant) }
    }

    // ── Tema general ───────────────────────────────────────────────────────

    val currentTheme: StateFlow<AppThemePalette> = themePreferenceManager.selectedThemeFlow
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = AppThemePalette.LUMI_SPARK_DARK
        )

    fun changeTheme(newTheme: AppThemePalette) {
        viewModelScope.launch {
            themePreferenceManager.saveTheme(newTheme)
        }
    }

    // ── Paleta de colores de fase ─────────────────────────────────────────

    val currentPhasePalette: StateFlow<PhaseColorPalette> =
        themePreferenceManager.selectedPhasePaletteFlow.stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = PhaseColorPalette.DEFAULT
        )

    val customPhaseColors: StateFlow<PhaseColors?> =
        themePreferenceManager.customPhaseColorsFlow.stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Cambia a un preset completo (DEFAULT, PASTEL, VIVID, etc.) — las 4 fases juntas. */
    fun changePhasePalette(newPalette: PhaseColorPalette) {
        viewModelScope.launch {
            themePreferenceManager.savePhasePalette(newPalette)
        }
    }

    /**
     * Cambia el color de UNA sola fase, activando CUSTOM automáticamente.
     * Si todavía no había un set custom guardado, parte de la paleta visible
     * actualmente (preset o custom previo) para no perder las otras 3 fases.
     */
    fun changeSinglePhaseColor(slot: PhaseSlot, color: Color, isDark: Boolean) {
        viewModelScope.launch {
            val baseline = customPhaseColors.value
                ?: currentPhasePalette.value.toPhaseColors(isDark)
            themePreferenceManager.saveSinglePhaseColor(slot, color, baseline)
        }
    }

    /** Guarda los 4 colores a la vez — usado por el flujo "personalización completa". */
    fun saveCustomPhaseColors(colors: PhaseColors) {
        viewModelScope.launch {
            themePreferenceManager.saveCustomPhaseColors(colors)
        }
    }

    // ── Reset de datos ────────────────────────────────────────────────────────

    /**
     * Borra TODOS los datos de la app: ciclos, registros y preferencias.
     * Marca el onboarding como no completado y mata el proceso para un reinicio limpio.
     */
    fun resetAllData() {
        viewModelScope.launch {
            // 1. Borrar base de datos Room (ciclos, logs, síntomas, recordatorios, chat)
            repository.deleteAllData()
            // 2. Limpiar preferencias de onboarding
            onboardingPreferenceManager.clearAllPreferences()
            // 3. Limpiar preferencias de chat
            chatPreferenceManager.clearAllPreferences()
            // 4. Matar el proceso — Android lo relanzará desde cero (onboarding)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}