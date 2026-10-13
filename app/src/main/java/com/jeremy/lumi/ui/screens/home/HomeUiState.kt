package com.jeremy.lumi.ui.screens.home

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.PartnerLink
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
    val userName             : String?          = null,
    val isPregnant           : Boolean          = false,
    val isOnContraceptive    : Boolean          = false,
    val userGoal             : String           = "health",
    val logStreakDays        : Int              = 0,

    // ── Animaciones In-App ────────────────────────────────────────────────
    val showHugAnimation     : Boolean          = false,

    // ── Modo Vínculo ──────────────────────────────────────────────────────
    val isObserverOnly       : Boolean          = false,
    val homeMode             : HomeMode         = HomeMode.NORMAL,
    val linkedCycles         : List<PartnerLink> = emptyList(),
    
    // ── Errores ───────────────────────────────────────────────────────────
    val syncError            : Boolean          = false,

    // ── Fecha seleccionada en el Timeline (null = hoy) ────────────────────
    val selectedDate         : java.time.LocalDate? = null,

    // ── Factores de Predicción "Basado en:" ────────────────────────────────
    val predictionFactors    : PredictionFactorsUi? = null
)

data class PredictionFactorsUi(
    val regularityScore: Int,      // 0-100%
    val historyScore: Int,         // 0-100%
    val symptomsScore: Int,        // 0-100%
    val cycleCount: Int,           // Número de ciclos completados
    val confidenceLabel: String,   // e.g. "Certeza alta", "Certeza moderada"
    val marginDaysText: String     // e.g. "±1.5 días"
)

data class CycleDayUi(
    val date         : java.time.LocalDate,
    val dayOfMonth   : Int,
    val weekdayLabel : String,
    val phase        : CyclePhase,
    val isToday      : Boolean,
    val isPrediction : Boolean = false,
    val isSelected   : Boolean = false,
    val hasLog       : Boolean = false
)