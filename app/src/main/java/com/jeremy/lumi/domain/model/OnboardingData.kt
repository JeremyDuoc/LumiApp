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
    val mode             : String               = "track", // "track" | "support"
    val isRegular        : Boolean?             = null,
    val regularityChoice : String?              = null,    // "yes" | "no" | "unknown"
    val cycleLengthKnown : Boolean              = false,
    val cycleLength      : Int                  = 28,
    val lastPeriodKnown  : Boolean              = false,
    val lastPeriodDate   : Long                 = 0L,
    val periodLengthKnown: Boolean              = false,
    val periodLength     : Int                  = 5,
    val userGoal         : UserGoal?            = null,
    val contraceptionChoice: String             = "unsure", // "yes" | "no" | "unsure"
    val isOnContraceptive: Boolean              = false,
    // --- Objetivos secundarios (múltiples) ---
    val secondaryGoals   : Set<SecondaryGoal>   = emptySet(),
    // --- Perfil físico (opcional, solo para calibración IA local) ---
    val age              : Int?                 = null,
    val height           : Float?               = null,   // cm
    val weight           : Float?               = null,   // kg
    // Cumplimiento legal y consentimientos
    val medicalDisclaimerAccepted: Boolean      = false,
    val privacyAccepted  : Boolean              = false
)
