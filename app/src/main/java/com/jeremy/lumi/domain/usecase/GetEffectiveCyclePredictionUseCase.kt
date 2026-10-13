package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.ai.CycleAIPredictor
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.utils.LumiClock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

enum class PredictionSourceType {
    NONE,
    PROFILE_DEFAULT,
    HISTORICAL_MATH,
    AI_BLENDED,
    CONTRACEPTIVE
}

data class EffectiveCyclePredictionResult(
    val prediction: CyclePrediction?,
    val effectiveCycleLength: Int,
    val periodLength: Int,
    val source: PredictionSourceType,
    val aiConfidence: Float? = null
)

/**
 * Fuente única de verdad para la predicción efectiva del ciclo menstrual.
 *
 * Unifica el cálculo de longitud estimada, blending de IA (LiteRT / TFLite),
 * reglas de anticoncepción hormonal y estado de fase para todos los consumidores:
 * HomeScreen (Dial y cabecera), CalendarScreen (mes y tarjeta de estado),
 * recordatorios locales y sincronización con pareja.
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
            return EffectiveCyclePredictionResult(
                prediction = null,
                effectiveCycleLength = 28,
                periodLength = 5,
                source = PredictionSourceType.NONE
            )
        }

        val startDate = Instant.ofEpochMilli(activeCycle.startDate)
            .atZone(ZoneId.of("UTC")).toLocalDate()
        val periodLength = activeCycle.periodLength.coerceAtLeast(1)

        // 1. Longitud de ciclo base matemática
        val mathCycleLen = if (isOnContraceptive) {
            28
        } else if (closedCycles.size >= 3) {
            CyclePredictor.weightedAverageCycleLength(closedCycles).roundToInt().coerceIn(15, 60)
        } else {
            activeCycle.cycleLength.coerceIn(15, 60)
        }

        // 2. Modelo de Machine Learning (LiteRT / TFLite)
        val aiResult = if (closedCycles.size >= 3 && !isOnContraceptive) {
            aiPredictor.predict(
                closedCycles = closedCycles,
                currentCycleLen = mathCycleLen.toFloat(),
                periodLength = periodLength.toFloat(),
                age = age,
                height = height,
                weight = weight
            )
        } else null

        val effectiveCycleLen = aiResult?.predictedCycleLength?.roundToInt()?.coerceIn(15, 60)
            ?: mathCycleLen

        // 3. Predicción clínica integral
        val prediction = CyclePredictor.predict(
            startDate = startDate,
            cycleLength = effectiveCycleLen,
            periodLength = periodLength,
            today = targetDate,
            closedCycles = closedCycles,
            isPregnant = isPregnant,
            isOnContraceptive = isOnContraceptive,
            effectiveCycleLength = effectiveCycleLen,
            predictionConfidence = aiResult?.confidenceScore
        )

        val source = when {
            isOnContraceptive -> PredictionSourceType.CONTRACEPTIVE
            aiResult != null && aiResult.source == com.jeremy.lumi.ai.PredictionSource.BLENDED -> PredictionSourceType.AI_BLENDED
            closedCycles.size >= 3 -> PredictionSourceType.HISTORICAL_MATH
            else -> PredictionSourceType.PROFILE_DEFAULT
        }

        return EffectiveCyclePredictionResult(
            prediction = prediction,
            effectiveCycleLength = effectiveCycleLen,
            periodLength = periodLength,
            source = source,
            aiConfidence = aiResult?.confidenceScore
        )
    }
}
