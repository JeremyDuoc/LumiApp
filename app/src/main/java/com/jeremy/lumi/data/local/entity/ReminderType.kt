package com.jeremy.lumi.data.local.entity

/**
 * Todos los tipos de recordatorio de Lumi.
 *
 * NUEVO: SUPPLY_REMINDER — "lleva tu método" (tampón, copa, toallita, etc.)
 * La usuaria elige CUÁL es su método habitual al activarlo, y Lumi lo usa
 * para personalizar el texto del mensaje ("no olvides tu copa menstrual"
 * en vez de un genérico "lleva tu método").
 */
enum class ReminderType {

    // ── Recordatorios de ciclo (automáticos) ─────────────────────────────────
    PERIOD_SOON,
    OVULATION_SOON,
    LOG_DAILY,

    // ── Recordatorio de insumos — NUEVO ───────────────────────────────────────
    SUPPLY_REMINDER,

    // ── Recordatorios de método anticonceptivo (manuales) ─────────────────────
    PILL_DAILY,
    PATCH_WEEKLY,
    RING_MONTHLY,
    INJECTION_MONTHLY,
    INJECTION_QUARTERLY,

    // ── Recordatorio libre ─────────────────────────────────────────────────────
    CUSTOM;

    fun isAutomatic(): Boolean = this == PERIOD_SOON ||
            this == OVULATION_SOON ||
            this == LOG_DAILY ||
            this == SUPPLY_REMINDER

    fun requiresStartDate(): Boolean = this == PATCH_WEEKLY ||
            this == RING_MONTHLY  ||
            this == INJECTION_MONTHLY ||
            this == INJECTION_QUARTERLY

    fun allowsCustomDate(): Boolean = this == CUSTOM ||
            this == PATCH_WEEKLY ||
            this == RING_MONTHLY ||
            this == INJECTION_MONTHLY ||
            this == INJECTION_QUARTERLY

    fun defaultRepeatIntervalDays(): Long? = when (this) {
        PILL_DAILY            -> 1L
        PATCH_WEEKLY          -> 7L
        RING_MONTHLY          -> 21L
        INJECTION_MONTHLY     -> 30L
        INJECTION_QUARTERLY   -> 90L
        LOG_DAILY             -> 1L
        CUSTOM                -> null
        SUPPLY_REMINDER       -> null   // se reprograma con cada ciclo, igual que PERIOD_SOON
        PERIOD_SOON,
        OVULATION_SOON        -> null
    }
}

/** Opciones de método de protección que la usuaria puede elegir para SUPPLY_REMINDER. */
enum class SupplyType {
    TAMPON, PAD, MENSTRUAL_CUP, PERIOD_UNDERWEAR, OTHER
}