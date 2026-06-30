package com.jeremy.lumi.domain.model

/**
 * Objetivo principal de uso de la app, recogido en el onboarding.
 * Es excluyente (solo uno). Dicta la "matemática" de la app:
 * - AVOID_PREGNANCY → ventanas fértiles más amplias (modo conservador)
 * - SEEK_PREGNANCY  → máxima precisión en ovulación
 * - TRACK_CYCLE     → seguimiento general
 * - HEALTH_MONITORING → enfoque en síntomas y bienestar
 */
enum class UserGoal {
    TRACK_CYCLE,       // Solo llevar el control del ciclo
    AVOID_PREGNANCY,   // Evitar el embarazo
    SEEK_PREGNANCY,    // Buscar el embarazo
    HEALTH_MONITORING  // Seguimiento de salud general
}

/**
 * Objetivos secundarios de la usuaria (múltiples).
 * Alimentan al Chat de Lumi, diciéndole en qué temas enfocarse.
 * No son excluyentes entre sí.
 */
enum class SecondaryGoal {
    MANAGE_CRAMPS,      // Manejar mis cólicos
    IMPROVE_SLEEP,      // Mejorar mi sueño
    UNDERSTAND_MOOD,    // Entender mis cambios de humor
    PARTNER_MODE,       // Usar el modo pareja
    TRACK_FERTILITY,    // Seguimiento detallado de fertilidad (BBT, flujo)
    REDUCE_STRESS       // Manejo del estrés y su impacto en el ciclo
}

/**
 * Datos recopilados durante el onboarding.
 * Se usa como contenedor temporal en el ViewModel antes de persistirlos.
 *
 * Los campos físicos (age, height, weight) son 100% opcionales.
 * Solo se usan para calibrar el modelo TFLite — nunca se suben a la nube.
 */
data class OnboardingData(
    val userName         : String?              = null,
    val isRegular        : Boolean?             = null,
    val cycleLength      : Int                  = 28,
    val lastPeriodKnown  : Boolean              = true,
    val lastPeriodDate   : Long                 = 0L,
    val periodLength     : Int                  = 5,
    val userGoal         : UserGoal             = UserGoal.TRACK_CYCLE,
    // --- Perfil físico (opcional, solo para calibración IA local) ---
    val age              : Int?                 = null,
    val height           : Float?               = null,   // cm
    val weight           : Float?               = null,   // kg
    // --- Objetivos secundarios (múltiples) ---
    val secondaryGoals   : Set<SecondaryGoal>   = emptySet(),
    // FIX P3-3: Modo anticonceptivo recogido durante el onboarding.
    // Persiste en DataStore al completar el wizard y afecta predictor + chat.
    val isOnContraceptive: Boolean              = false,
    
    // Disclaimer Médico obligatorio (cumplimiento legal)
    val medicalDisclaimerAccepted: Boolean      = false
)
