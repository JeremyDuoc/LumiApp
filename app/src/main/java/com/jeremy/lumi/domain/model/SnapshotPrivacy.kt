package com.jeremy.lumi.domain.model

/**
 * Aplica el consentimiento granular de cada vínculo antes de transmitir cualquier snapshot de salud a Firestore.
 *
 * Protección contra inferencia indirecta de fases (P1-6):
 * - Si privacy.phase == false: currentPhase es UNKNOWN y se eliminan cycleStartDateMs, cycleLengthDays
 *   y periodLengthDays, imposibilitando la reconstrucción matemática del día del ciclo.
 * - Las predicciones de retraso solo transmiten métricas relativas (isLate, delayDays, daysUntilNextPhase).
 */
fun CycleSnapshot.filteredBy(privacy: SharePreferences): CycleSnapshot = copy(
    currentPhase = if (privacy.phase) currentPhase else CyclePhase.UNKNOWN,
    currentMood = if (privacy.mood) currentMood else null,
    topSymptoms = if (privacy.symptoms) topSymptoms else emptyList(),
    daysUntilNextPhase = if (privacy.predictions) daysUntilNextPhase else 0,
    isLate = privacy.predictions && isLate,
    delayDays = if (privacy.predictions) delayDays else 0,
    cycleStartDateMs = if (privacy.phase && privacy.predictions) cycleStartDateMs else null,
    cycleLengthDays = if (privacy.phase) cycleLengthDays else 0,
    periodLengthDays = if (privacy.phase) periodLengthDays else 0
)
