package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.ai.CycleAIPredictor
import com.jeremy.lumi.ai.PredictionSource
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.utils.LumiClock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

enum class PredictionSourceType {
    NONE, PROFILE_DEFAULT, HISTORICAL_MATH, AI_BLENDED, CONTRACEPTIVE
}

data class EffectiveCyclePredictionResult(
    val prediction: CyclePrediction?,
    val effectiveCycleLength: Int,
    val periodLength: Int,
    val source: PredictionSourceType,
    val aiConfidence: Float? = null
)

/**
 * Single source of truth for Home/Calendar/reminders.
 * Prior cycles only; no information from the cycle being predicted.
 * The experimental Ridge candidate is disabled in bundled assets until a
 * prospective participant-held-out study establishes actual benefit.
 */
@Singleton
class GetEffectiveCyclePredictionUseCase @Inject constructor(
    private val aiPredictor: CycleAIPredictor
) {
    operator fun invoke(
        activeCycle: CycleEntity?,
        closedCycles: List<CycleEntity>,
        isPregnant: Boolean = false,
        isOnContraceptive: Boolean = false,
        age: Float? = null,
        height: Float? = null,
        weight: Float? = null,
        targetDate: LocalDate = LumiClock.today()
    ): EffectiveCyclePredictionResult {
        if (activeCycle == null) {
            return EffectiveCyclePredictionResult(null, 28, 5, PredictionSourceType.NONE)
        }

        val startDate = Instant.ofEpochMilli(activeCycle.startDate)
            .atZone(ZoneOffset.UTC).toLocalDate()
        val periodLength = activeCycle.periodLength.takeIf { it in 1..10 } ?: 5
        val priorCycles = CycleHistoryV2.validCycles(closedCycles, activeCycle.startDate)

        val mathCycleLen = when {
            isOnContraceptive -> 28 // Historical UI behavior; see clinical caveat.
            priorCycles.size >= 3 -> CyclePredictor.weightedAverageCycleLength(priorCycles)
                .roundToInt().coerceIn(15, 60)
            else -> activeCycle.cycleLength.coerceIn(15, 60)
        }

        val ai = if (priorCycles.size >= 3 && !isOnContraceptive && !isPregnant &&
            aiPredictor.isModelReady()) {
            aiPredictor.predict(
                closedCycles = priorCycles, currentCycleLen = mathCycleLen.toFloat(),
                periodLength = periodLength.toFloat(),
                age = age, height = height, weight = weight
            )
        } else null

        // Never trust unchecked NaN/infinite/out-of-range outputs.
        val verifiedModelLength = ai?.takeIf {
            it.source == PredictionSource.RIDGE_V2 &&
                it.predictedCycleLength.isFinite() &&
                it.predictedCycleLength in 15f..60f
        }?.predictedCycleLength
        val effectiveCycleLength = verifiedModelLength?.roundToInt() ?: mathCycleLen

        val prediction = CyclePredictor.predict(
            startDate = startDate,
            cycleLength = effectiveCycleLength,
            periodLength = periodLength,
            today = targetDate,
            closedCycles = priorCycles,
            isPregnant = isPregnant,
            isOnContraceptive = isOnContraceptive,
            effectiveCycleLength = effectiveCycleLength,
            predictionConfidence = null
        )

        val source = when {
            isOnContraceptive -> PredictionSourceType.CONTRACEPTIVE
            verifiedModelLength != null -> PredictionSourceType.AI_BLENDED // Legacy enum name
            priorCycles.size >= 3 -> PredictionSourceType.HISTORICAL_MATH
            else -> PredictionSourceType.PROFILE_DEFAULT
        }
        return EffectiveCyclePredictionResult(
            prediction = prediction,
            effectiveCycleLength = effectiveCycleLength,
            periodLength = periodLength,
            source = source,
            aiConfidence = null // no unsupported claims of probability
        )
    }
}
