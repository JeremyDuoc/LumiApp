package com.jeremy.lumi.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Global clock for the app. Allows time travel debugging in DEV builds.
 */
object LumiClock {
    private val _offsetDays = MutableStateFlow(0L)
    val offsetDays: StateFlow<Long> = _offsetDays.asStateFlow()

    fun advanceDay() {
        _offsetDays.value += 1
    }

    fun regressDay() {
        _offsetDays.value -= 1
    }

    fun today(): LocalDate {
        return LocalDate.now().plusDays(_offsetDays.value)
    }

    fun todayLocalDate(): LocalDate {
        return today()
    }

    fun todayUtcMillis(): Long {
        return today().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    fun nowMillis(): Long {
        return System.currentTimeMillis() + (_offsetDays.value * 24 * 60 * 60 * 1000)
    }
}
