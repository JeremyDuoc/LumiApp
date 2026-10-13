package com.jeremy.lumi.domain.model

/**
 * Permisos individuales y granulares por vínculo en Bonds.
 * Cada dimensión es completamente voluntaria y revocable en cualquier momento.
 *
 * Principio de Privacidad por Defecto (Privacy by Default - GDPR Art. 25):
 * Todas las banderas de compartición médica y emocional inicializan estrictamente en false (Opt-in).
 */
data class BondsPrivacySettings(
    val shareCyclePhase: Boolean = false,
    val shareMood: Boolean = false,
    val shareSymptoms: Boolean = false,
    val sharePredictions: Boolean = false,
    val consentExplicitlyGranted: Boolean = false
) {
    fun toSharePreferences(): SharePreferences = SharePreferences(
        phase = shareCyclePhase,
        mood = shareMood,
        symptoms = shareSymptoms,
        predictions = sharePredictions,
        consentExplicitlyGranted = consentExplicitlyGranted
    )

    companion object {
        fun fromSharePreferences(prefs: SharePreferences): BondsPrivacySettings =
            BondsPrivacySettings(
                shareCyclePhase = prefs.phase,
                shareMood = prefs.mood,
                shareSymptoms = prefs.symptoms,
                sharePredictions = prefs.predictions,
                consentExplicitlyGranted = prefs.consentExplicitlyGranted
            )
    }
}
