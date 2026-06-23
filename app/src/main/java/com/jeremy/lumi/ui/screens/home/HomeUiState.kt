package com.jeremy.lumi.ui.screens.home

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.usecase.CyclePrediction
import com.jeremy.lumi.domain.usecase.DelayState

data class HomeUiState(
    val isLoading            : Boolean          = true,
    val activeCycle          : CycleEntity?     = null,
    val currentPhase         : CyclePhase       = CyclePhase.UNKNOWN,
    val currentDayOfCycle    : Int              = 0,
    val prediction           : CyclePrediction? = null,
    val weekDays             : List<CycleDayUi> = emptyList(),
    // ── Estado de retraso ─────────────────────────────────────────────────
    val delayState           : DelayState       = DelayState.ON_TIME,
    val delayDays            : Int              = 0,
    val isLate               : Boolean          = false,
    
    // ── Preferencias ──────────────────────────────────────────────────────
    val isDiscreetMode       : Boolean          = false,
    val isPregnant           : Boolean          = false,
    val userGoal             : String           = "health"
)

data class CycleDayUi(
    val date         : java.time.LocalDate,
    val dayOfMonth   : Int,
    val weekdayLabel : String,
    val phase        : CyclePhase,
    val isToday      : Boolean
)