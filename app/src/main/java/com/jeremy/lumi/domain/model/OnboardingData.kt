package com.jeremy.lumi.domain.model

/**
 * Objetivo de uso de la app, recogido en el onboarding.
 * Se guarda en DataStore y se usa para personalizar el tono del Chat.
 * No modifica la lógica de predicción (por ahora).
 */
enum class UserGoal {
    TRACK_CYCLE,       // Solo llevar el control del ciclo
    AVOID_PREGNANCY,   // Evitar el embarazo
    SEEK_PREGNANCY,    // Buscar el embarazo
    HEALTH_MONITORING  // Seguimiento de salud general
}

/**
 * Datos recopilados durante el onboarding.
 * Se usa como contenedor temporal en el ViewModel antes de persistirlos.
 */
data class OnboardingData(
    val userName      : String?   = null,       // Opcional — null si lo saltó
    val lastPeriodDate: Long      = 0L,          // Epoch millis del inicio del último ciclo
    val cycleLength   : Int       = 28,
    val periodLength  : Int       = 5,
    val userGoal      : UserGoal  = UserGoal.TRACK_CYCLE
)
