package com.jeremy.lumi.domain.model

import androidx.annotation.StringRes

enum class MedicalAlertType {
    PCOS_SUSPICION,
    ENDOMETRIOSIS_SUSPICION,
    SEVERE_IRREGULARITY,
    PMDD_SUSPICION,
    PERIMENOPAUSE_TRANSITION,
    LUTEAL_PHASE_DEFECT
}

enum class AlertSeverity {
    WARNING,
    CRITICAL
}

data class MedicalAlert(
    val type: MedicalAlertType,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val severity: AlertSeverity,
    val requiresDisclaimer: Boolean = true
)
