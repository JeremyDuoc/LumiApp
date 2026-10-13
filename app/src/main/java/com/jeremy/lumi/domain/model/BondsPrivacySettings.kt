package com.jeremy.lumi.domain.model

/**
 * Permisos individuales y granulares por vínculo en Bonds.
 * Cada dimensión es completamente voluntaria y revocable en cualquier momento.
 */
data class BondsPrivacySettings(
    val shareCyclePhase: Boolean = true,
    val shareMood: Boolean = true,
    val shareSymptoms: Boolean = false,
    val sharePredictions: Boolean = true
) {
    fun toSharePreferences(): SharePreferences = SharePreferences(
        phase = shareCyclePhase,
        mood = shareMood,
        symptoms = shareSymptoms,
        predictions = sharePredictions
    )

    companion object {
        fun fromSharePreferences(prefs: SharePreferences): BondsPrivacySettings =
            BondsPrivacySettings(
                shareCyclePhase = prefs.phase,
                shareMood = prefs.mood,
                shareSymptoms = prefs.symptoms,
                sharePredictions = prefs.predictions
            )
    }
}
