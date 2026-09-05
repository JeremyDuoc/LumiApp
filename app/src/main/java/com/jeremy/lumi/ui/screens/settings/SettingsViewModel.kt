package com.jeremy.lumi.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.preferences.ChatPreferenceManager
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.data.health.HealthConnectManager
import com.jeremy.lumi.data.preferences.PhaseSlot
import com.jeremy.lumi.data.preferences.ThemePreferenceManager
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.BackgroundAmbient
import com.jeremy.lumi.ui.theme.DialStyle
import com.jeremy.lumi.domain.model.UserGoal
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.ui.theme.PhaseColorPalette
import com.jeremy.lumi.ui.theme.PhaseColors
import com.jeremy.lumi.ui.theme.PhaseDefaultLight
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
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
    private val repository: LumiRepository,
    // FIX P3-2: El manager es privado — el Screen solo accede a propiedades
    // expuestas por el ViewModel, no al objeto completo (patrón MVVM correcto).
    private val healthConnectManager: HealthConnectManager,
    private val mockDataGeneratorUseCase: com.jeremy.lumi.domain.usecase.MockDataGeneratorUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** ¿Está el SDK de Health Connect disponible en este dispositivo? */
    val isHealthConnectAvailable: Boolean
        get() = healthConnectManager.isAvailable()

    /** Conjunto de permisos que la app necesita solicitar a Health Connect. */
    val healthConnectPermissions: Set<String>
        get() = healthConnectManager.permissions

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

    val isOnContraceptive: StateFlow<Boolean> = onboardingPreferenceManager.isOnContraceptiveFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isHealthConnectEnabled: StateFlow<Boolean> = onboardingPreferenceManager.isHealthConnectEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setUserGoal(goal: UserGoal) {
        viewModelScope.launch { onboardingPreferenceManager.setUserGoal(goal) }
    }

    fun setIsPregnant(isPregnant: Boolean) {
        viewModelScope.launch { onboardingPreferenceManager.setIsPregnant(isPregnant) }
    }

    fun setIsOnContraceptive(isOnContraceptive: Boolean) {
        viewModelScope.launch { onboardingPreferenceManager.setIsOnContraceptive(isOnContraceptive) }
    }

    fun setIsHealthConnectEnabled(isEnabled: Boolean) {
        viewModelScope.launch { onboardingPreferenceManager.setIsHealthConnectEnabled(isEnabled) }
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

    // ── Estilo de aro (DialStyle) ─────────────────────────────────────────

    val dialStyle: StateFlow<DialStyle> = themePreferenceManager.dialStyleFlow
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = DialStyle.GRADIENT
        )

    fun changeDialStyle(style: DialStyle) {
        viewModelScope.launch {
            themePreferenceManager.saveDialStyle(style)
        }
    }

    // ── Ambiente de fondo ─────────────────────────────────────────────────

    val backgroundAmbient: StateFlow<BackgroundAmbient> = themePreferenceManager.backgroundAmbientFlow
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = BackgroundAmbient.AURA
        )

    fun changeBackgroundAmbient(ambient: BackgroundAmbient) {
        viewModelScope.launch {
            themePreferenceManager.saveBackgroundAmbient(ambient)
        }
    }

    // ── Paleta de colores de fase ─────────────────────────────────────────

    val currentPhasePalette: StateFlow<PhaseColorPalette> =
        themePreferenceManager.selectedPhasePaletteFlow.stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = PhaseColorPalette.AUTO_HARMONY
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
                ?: currentPhasePalette.value.toPhaseColors(isDark, currentTheme.value)
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
     * Marca el onboarding como no completado y relanza la app en una tarea limpia.
     */
    fun resetAllData() {
        viewModelScope.launch {
            // 1. Borrar base de datos Room (ciclos, logs, síntomas, recordatorios, chat)
            repository.deleteAllData()
            // 2. Limpiar preferencias de onboarding
            onboardingPreferenceManager.clearAllPreferences()
            // 3. Limpiar preferencias de chat
            chatPreferenceManager.clearAllPreferences()
            // 4. Relanzar limpiamente la aplicación eliminando la pila de actividades anterior
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            if (intent != null) {
                context.startActivity(intent)
                delay(150)
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    /** 
     * Inyecta 4 meses de historial (solo para Dev/Testing).
     * Luego reinicia la app limpiamente para forzar recarga en todos los ViewModels. 
     */
    fun injectMockData() {
        viewModelScope.launch {
            mockDataGeneratorUseCase()
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            if (intent != null) {
                context.startActivity(intent)
                delay(150)
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
}