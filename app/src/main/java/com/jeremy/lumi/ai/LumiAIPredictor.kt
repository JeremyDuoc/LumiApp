package com.jeremy.lumi.ai

import android.content.Context
import android.util.Log
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.domain.usecase.CycleHistoryV2
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Experimental leak-free residual Ridge. Parameters are disabled by default:
 * grouped participant cross-validation did not prove a meaningful advantage
 * over historical mathematics. No legacy TFLite model is loaded.
 */
interface CycleAIPredictor {
    fun isModelReady(): Boolean
    fun predict(
        closedCycles: List<CycleEntity>,
        currentCycleLen: Float,
        periodLength: Float,
        age: Float? = null,
        height: Float? = null,
        weight: Float? = null
    ): AIPredictionResult
}

@Singleton
class LumiAIPredictor @Inject constructor(
    @ApplicationContext private val context: Context
) : CycleAIPredictor {
    private companion object {
        const val TAG = "LumiAIPredictorV2"
        const val FILE = "lumi_residual_v2.properties"
        val NAMES = listOf("math", "std_raw", "n_clipped", "mean3", "mean5",
            "median", "last", "diff", "trend", "mad")
    }

    private data class Model(
        val means: List<Float>,
        val scales: List<Float>,
        val coefficients: List<Float>,
        val intercept: Float,
        val maxCorrection: Float,
        val enabled: Boolean
    )

    private val model: Model? by lazy {
        try {
            val fields = context.assets.open(FILE).bufferedReader().use { it.readText() }
                .lineSequence().map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") && '=' in it }
                .associate { it.substringBefore('=') to it.substringAfter('=') }
            require(fields["schema"] == "2")
            require(fields["features"]?.split(',') == NAMES)
            fun parse(name: String): List<Float> =
                fields.getValue(name).split(',').map { it.toFloat() }
            val means = parse("means")
            val scales = parse("scales")
            val coefficients = parse("coefficients")
            require(means.size == NAMES.size && scales.size == NAMES.size &&
                coefficients.size == NAMES.size)
            require((means + scales + coefficients).all { it.isFinite() } &&
                scales.all { it > 0f })
            val intercept = fields.getValue("intercept").toFloat()
            val limit = fields.getValue("max_correction_days").toFloat()
            require(intercept.isFinite() && limit.isFinite() && limit in 0f..2f)
            Model(means, scales, coefficients, intercept, limit, fields["enabled"] == "true")
        } catch (e: Exception) {
            Log.w(TAG, "Residual model unavailable: using mathematics only", e)
            null
        }
    }

    override fun isModelReady(): Boolean = model?.enabled == true

    override fun predict(
        closedCycles: List<CycleEntity>,
        currentCycleLen: Float,
        periodLength: Float,
        age: Float?,
        height: Float?,
        weight: Float?
    ): AIPredictionResult {
        val values = CycleHistoryV2.durations(closedCycles)
        val stdDev = CycleHistoryV2.populationStd(values)
        val math = currentCycleLen.takeIf { it.isFinite() && it in 15f..60f } ?: 28f

        fun fallback() = AIPredictionResult(
            predictedCycleLength = math,
            confidenceScore = 0f,
            isIrregular = stdDev > 3.5f,
            predictionRangeDays = 0,
            source = PredictionSource.MATHEMATICAL
        )

        val m = model ?: return fallback()
        if (!m.enabled || values.size < 3) return fallback()

        val median = CycleHistoryV2.median(values)
        val mad = CycleHistoryV2.median(values.map { abs(it - median) })
        val features = listOf(
            math, stdDev, values.size.coerceAtMost(12).toFloat(),
            values.take(3).average().toFloat(), values.take(5).average().toFloat(),
            median, values[0], values[0] - values[1],
            values.take(2).average().toFloat() -
                values.drop(2).take(3).average().toFloat(),
            mad
        )
        if (features.any { !it.isFinite() }) return fallback()
        return try {
            val correction = features.indices.sumOf { i ->
                ((features[i] - m.means[i]) / m.scales[i] * m.coefficients[i]).toDouble()
            }.toFloat() + m.intercept
            if (!correction.isFinite()) return fallback()
            val predicted = (math + correction.coerceIn(-m.maxCorrection, m.maxCorrection))
                .coerceIn(15f, 60f)
            if (!predicted.isFinite()) return fallback()
            AIPredictionResult(predicted, 0f, stdDev > 3.5f, 0, PredictionSource.RIDGE_V2)
        } catch (e: Exception) {
            Log.w(TAG, "Experimental residual inference failed", e)
            fallback()
        }
    }
}
