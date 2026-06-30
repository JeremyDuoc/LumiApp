package com.jeremy.lumi.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.jeremy.lumi.domain.model.SecondaryGoal
import com.jeremy.lumi.domain.model.UserGoal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona el estado del onboarding y los datos de perfil de usuario en DataStore.
 *
 * Todo permanece en el dispositivo — no se envía ningún dato a servidores.
 * El nombre de usuario es opcional y solo se usa para personalizar el saludo.
 */
@Singleton
class OnboardingPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Reutiliza el mismo DataStore que ThemePreferenceManager (mismo nombre "lumi_prefs")
    // para no crear un segundo archivo innecesario.
    private val dataStore = context.dataStore

    companion object {
        private val ONBOARDING_DONE_KEY = booleanPreferencesKey("onboarding_completed")
        private val IS_OBSERVER_ONLY_KEY= booleanPreferencesKey("is_observer_only")
        private val USER_NAME_KEY       = stringPreferencesKey("user_name")
        private val IS_REGULAR_KEY      = booleanPreferencesKey("is_regular")
        private val USER_GOAL_KEY       = stringPreferencesKey("user_goal")
        private val CYCLE_LENGTH_KEY    = intPreferencesKey("default_cycle_length")
        private val PERIOD_LENGTH_KEY   = intPreferencesKey("default_period_length")
        
        private val IS_PREGNANT_KEY     = booleanPreferencesKey("is_pregnant")
        private val IS_ON_CONTRACEPTIVE_KEY = booleanPreferencesKey("is_on_contraceptive")
        private val IS_HEALTH_CONNECT_ENABLED_KEY = booleanPreferencesKey("is_health_connect_enabled")
        private val IS_DISCREET_KEY     = booleanPreferencesKey("is_discreet_mode")
        private val LOG_CATEGORIES_KEY  = androidx.datastore.preferences.core.stringSetPreferencesKey("active_log_categories")
        // Clave para persistir que la usuaria ya cerró el banner de retraso manualmente.
        // Se guarda el día del ciclo en que se cerró; si el día cambia, vuelve a mostrarse.
        private val DELAY_BANNER_DISMISSED_DAY_KEY = androidx.datastore.preferences.core.intPreferencesKey("delay_banner_dismissed_day")
        
        // Cooldown de abrazos virtuales
        private val HUG_COOLDOWN_UNTIL_KEY = androidx.datastore.preferences.core.longPreferencesKey("hug_cooldown_until")
        
        // --- Partner Sharing Privacy ---
        private val SHARE_PHASE_KEY = booleanPreferencesKey("share_phase")
        private val SHARE_PREDICTIONS_KEY = booleanPreferencesKey("share_predictions")
        private val SHARE_MOOD_KEY = booleanPreferencesKey("share_mood")
        private val SHARE_SYMPTOMS_KEY = booleanPreferencesKey("share_symptoms")

        // --- Perfil físico (opcional, para calibración IA local) ---
        private val USER_AGE_KEY            = intPreferencesKey("user_age")
        private val USER_HEIGHT_KEY         = floatPreferencesKey("user_height_cm")
        private val USER_WEIGHT_KEY         = floatPreferencesKey("user_weight_kg")
        private val SECONDARY_GOALS_KEY     = stringSetPreferencesKey("secondary_goals")
        private val MEDICAL_DISCLAIMER_KEY  = booleanPreferencesKey("medical_disclaimer_accepted")
    }

    // ── Onboarding completado ─────────────────────────────────────────────────

    /** true si la usuaria ya completó (o saltó) el onboarding. */
    val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[ONBOARDING_DONE_KEY] ?: false }

    /** true si la app se instaló solo para observar a alguien más */
    val isObserverOnly: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[IS_OBSERVER_ONLY_KEY] ?: false }

    suspend fun markOnboardingCompleted(isObserver: Boolean = false) {
        dataStore.edit { prefs -> 
            prefs[ONBOARDING_DONE_KEY] = true 
            prefs[IS_OBSERVER_ONLY_KEY] = isObserver
        }
    }

    // ── Datos de perfil ───────────────────────────────────────────────────────

    /** Nombre de la usuaria. Null si lo saltó. */
    val userNameFlow: Flow<String?> = dataStore.data
        .map { prefs -> prefs[USER_NAME_KEY]?.takeIf { it.isNotBlank() } }

    /** Si el ciclo es regular. Null si no lo sabe. */
    val isRegularFlow: Flow<Boolean?> = dataStore.data
        .map { prefs -> prefs[IS_REGULAR_KEY] }

    /** Objetivo de uso de la app. Por defecto TRACK_CYCLE. */
    val userGoalFlow: Flow<UserGoal> = dataStore.data
        .map { prefs ->
            val saved = prefs[USER_GOAL_KEY]
            runCatching { UserGoal.valueOf(saved ?: UserGoal.TRACK_CYCLE.name) }
                .getOrDefault(UserGoal.TRACK_CYCLE)
        }

    val cycleLengthFlow: Flow<Int> = dataStore.data
        .map { prefs -> prefs[CYCLE_LENGTH_KEY] ?: 28 }

    val periodLengthFlow: Flow<Int> = dataStore.data
        .map { prefs -> prefs[PERIOD_LENGTH_KEY] ?: 5 }

    // ── Nuevas Preferencias (Embarazo, Modo Discreto, Categorías de Registro) ──

    val isPregnantFlow: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[IS_PREGNANT_KEY] ?: false }

    val isOnContraceptiveFlow: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[IS_ON_CONTRACEPTIVE_KEY] ?: false }

    val isHealthConnectEnabledFlow: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[IS_HEALTH_CONNECT_ENABLED_KEY] ?: false }

    val isDiscreetModeFlow: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[IS_DISCREET_KEY] ?: false }

    val activeLogCategoriesFlow: Flow<Set<String>> = dataStore.data
        .map { prefs -> 
            prefs[LOG_CATEGORIES_KEY] ?: setOf("physical", "digestive", "mucus", "intercourse") // Por defecto todo activo o personalizar
        }

    suspend fun setUserGoal(goal: UserGoal) {
        dataStore.edit { prefs -> prefs[USER_GOAL_KEY] = goal.name }
    }

    suspend fun setIsPregnant(isPregnant: Boolean) {
        dataStore.edit { prefs -> prefs[IS_PREGNANT_KEY] = isPregnant }
    }

    suspend fun setIsOnContraceptive(isOnContraceptive: Boolean) {
        dataStore.edit { prefs -> prefs[IS_ON_CONTRACEPTIVE_KEY] = isOnContraceptive }
    }

    suspend fun setIsHealthConnectEnabled(isEnabled: Boolean) {
        dataStore.edit { prefs -> prefs[IS_HEALTH_CONNECT_ENABLED_KEY] = isEnabled }
    }

    suspend fun setDiscreetMode(isDiscreet: Boolean) {
        dataStore.edit { prefs -> prefs[IS_DISCREET_KEY] = isDiscreet }
    }

    suspend fun setActiveLogCategories(categories: Set<String>) {
        dataStore.edit { prefs -> prefs[LOG_CATEGORIES_KEY] = categories }
    }

    // --- Partner Privacy Flows & Setters ---
    val sharePhaseFlow: Flow<Boolean> = dataStore.data.map { it[SHARE_PHASE_KEY] ?: true }
    val sharePredictionsFlow: Flow<Boolean> = dataStore.data.map { it[SHARE_PREDICTIONS_KEY] ?: true }
    val shareMoodFlow: Flow<Boolean> = dataStore.data.map { it[SHARE_MOOD_KEY] ?: true }
    val shareSymptomsFlow: Flow<Boolean> = dataStore.data.map { it[SHARE_SYMPTOMS_KEY] ?: true }

    suspend fun setSharePhase(share: Boolean) = dataStore.edit { it[SHARE_PHASE_KEY] = share }
    suspend fun setSharePredictions(share: Boolean) = dataStore.edit { it[SHARE_PREDICTIONS_KEY] = share }
    suspend fun setShareMood(share: Boolean) = dataStore.edit { it[SHARE_MOOD_KEY] = share }
    suspend fun setShareSymptoms(share: Boolean) = dataStore.edit { it[SHARE_SYMPTOMS_KEY] = share }

    // --- Perfil físico (solo local, para calibrar el modelo TFLite) ---
    /** Edad de la usuaria. null si no la ingresó. */
    val ageFlow: Flow<Int?> = dataStore.data.map { it[USER_AGE_KEY] }
    /** Talla en cm. null si no la ingresó. */
    val heightFlow: Flow<Float?> = dataStore.data.map { it[USER_HEIGHT_KEY] }
    /** Peso en kg. null si no la ingresó. */
    val weightFlow: Flow<Float?> = dataStore.data.map { it[USER_WEIGHT_KEY] }
    /** Objetivos secundarios seleccionados. */
    val secondaryGoalsFlow: Flow<Set<SecondaryGoal>> = dataStore.data.map { prefs ->
        prefs[SECONDARY_GOALS_KEY]?.mapNotNull {
            runCatching { SecondaryGoal.valueOf(it) }.getOrNull()
        }?.toSet() ?: emptySet()
    }
    
    val medicalDisclaimerAcceptedFlow: Flow<Boolean> = dataStore.data.map { it[MEDICAL_DISCLAIMER_KEY] ?: false }

    // ── Recordatorio persistido de Banner de retraso ───────────────────────────────────────────────────

    /** El día del ciclo en que se cerró el banner. -1 = nunca cerrado. */
    val delayBannerDismissedDayFlow: kotlinx.coroutines.flow.Flow<Int> = dataStore.data
        .map { prefs -> prefs[DELAY_BANNER_DISMISSED_DAY_KEY] ?: -1 }

    /** Persiste que se cerró el banner en el día [cycleDay] del ciclo actual. */
    suspend fun dismissDelayBannerForDay(cycleDay: Int) {
        dataStore.edit { prefs -> prefs[DELAY_BANNER_DISMISSED_DAY_KEY] = cycleDay }
    }

    /** Resetea el dismiss (p.ej., al iniciar un nuevo ciclo). */
    suspend fun resetDelayBannerDismiss() {
        dataStore.edit { prefs -> prefs.remove(DELAY_BANNER_DISMISSED_DAY_KEY) }
    }

    // ── Persistencia del cooldown de abrazos ──────────────────────────────────
    
    val hugCooldownUntilFlow: Flow<Long> = dataStore.data
        .map { prefs -> prefs[HUG_COOLDOWN_UNTIL_KEY] ?: 0L }

    suspend fun setHugCooldownUntil(timestamp: Long) {
        dataStore.edit { prefs -> prefs[HUG_COOLDOWN_UNTIL_KEY] = timestamp }
    }

    /**
     * Guarda todos los datos del onboarding en una sola transacción atómica.
     * Llamar una vez al finalizar el último paso del onboarding.
     */
    suspend fun saveOnboardingProfile(
        userName       : String?,
        isRegular      : Boolean?,
        cycleLength    : Int,
        periodLength   : Int,
        goal           : UserGoal,
        isObserver     : Boolean = false,
        age            : Int?    = null,
        height         : Float?  = null,
        weight         : Float?  = null,
        secondaryGoals : Set<SecondaryGoal> = emptySet(),
        medicalDisclaimerAccepted: Boolean = false
    ) {
        dataStore.edit { prefs ->
            if (!userName.isNullOrBlank()) prefs[USER_NAME_KEY] = userName.trim()
            if (isRegular != null) prefs[IS_REGULAR_KEY] = isRegular
            prefs[USER_GOAL_KEY]       = goal.name
            prefs[CYCLE_LENGTH_KEY]    = cycleLength
            prefs[PERIOD_LENGTH_KEY]   = periodLength
            prefs[ONBOARDING_DONE_KEY] = true
            prefs[IS_OBSERVER_ONLY_KEY] = isObserver
            // Perfil físico (solo si lo proporcionó)
            if (age != null) prefs[USER_AGE_KEY] = age
            if (height != null) prefs[USER_HEIGHT_KEY] = height
            if (weight != null) prefs[USER_WEIGHT_KEY] = weight
            if (secondaryGoals.isNotEmpty()) {
                prefs[SECONDARY_GOALS_KEY] = secondaryGoals.map { it.name }.toSet()
            }
            prefs[MEDICAL_DISCLAIMER_KEY] = medicalDisclaimerAccepted
        }
    }

    /**
     * Borra TODAS las preferencias guardadas.
     * Usar exclusivamente en el flujo de reset total de datos.
     * Después de llamar esto, [isOnboardingCompleted] devolverá false
     * y la app mostrará el onboarding al reiniciarse.
     */
    suspend fun clearAllPreferences() {
        dataStore.edit { prefs -> prefs.clear() }
    }
}
