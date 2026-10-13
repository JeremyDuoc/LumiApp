package com.jeremy.lumi.domain.model

/** Apply each link's consent before writing any health data to Firestore. */
fun CycleSnapshot.filteredBy(privacy: SharePreferences): CycleSnapshot = copy(
    currentPhase = if (privacy.phase) currentPhase else CyclePhase.UNKNOWN,
    currentMood = if (privacy.mood) currentMood else null,
    topSymptoms = if (privacy.symptoms) topSymptoms else emptyList(),
    daysUntilNextPhase = if (privacy.predictions) daysUntilNextPhase else 0,
    isLate = privacy.predictions && isLate,
    delayDays = if (privacy.predictions) delayDays else 0,
    cycleStartDateMs = if (privacy.predictions) cycleStartDateMs else null,
    cycleLengthDays = if (privacy.predictions) cycleLengthDays else 0,
    periodLengthDays = if (privacy.predictions) periodLengthDays else 0
)
