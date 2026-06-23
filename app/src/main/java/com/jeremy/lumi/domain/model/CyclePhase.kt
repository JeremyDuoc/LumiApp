package com.jeremy.lumi.domain.model

import androidx.annotation.StringRes
import com.jeremy.lumi.R

enum class CyclePhase(
    @StringRes val phaseNameRes: Int,
    @StringRes val descriptionRes: Int
) {
    MENSTRUAL(
        phaseNameRes = R.string.phase_menstrual,
        descriptionRes = R.string.desc_menstrual
    ),
    FOLLICULAR(
        phaseNameRes = R.string.phase_follicular,
        descriptionRes = R.string.desc_follicular
    ),
    OVULATION(
        phaseNameRes = R.string.phase_ovulation,
        descriptionRes = R.string.desc_ovulation
    ),
    LUTEAL(
        phaseNameRes = R.string.phase_luteal,
        descriptionRes = R.string.desc_luteal
    ),
    PREGNANCY(
        phaseNameRes = R.string.phase_pregnancy,
        descriptionRes = R.string.desc_pregnancy
    ),
    UNKNOWN(
        phaseNameRes = R.string.phase_unknown,
        descriptionRes = R.string.desc_unknown
    )
}