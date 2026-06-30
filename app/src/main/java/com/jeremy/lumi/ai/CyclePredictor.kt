package com.jeremy.lumi.ai

import android.content.Context
import android.util.Log
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.domain.usecase.CyclePredictor
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Motor de predicción TFLite de LumiApp.
 *
 * Este predictor es el "cerebro profundo" que complementa al predictor matemático.
 * Se usa cuando hay suficientes datos de entrada; de lo contrario, devuelve null
 * y el HomeViewModel cae de vuelta al predictor matemático (degradación graciosa).
 *
 * Features que espera el modelo (orden EXACTO del scaler_params.txt — 11 features):
 *   [0] MeanCycleLength       — longitud media del ciclo de la usuaria
 *   [1] EstimatedDayofOvulation — estimación: cycleLen - 14
 *   [2] LengthofLutealPhase   — constante clínica: 14 días (sin BBT real)
 *   [3] LengthofMenses        — duración del sangrado
 *   [4] TotalMensesScore      — aproximación: mensesLen × 2
 *   [5] Age                   — edad (opcional, fallback a media del dataset)
 *   [6] Height                — talla en cm (opcional)
 *   [7] Weight                — peso en kg (opcional)
 *   [8] BMI                   — calculado o fallback a media
 *   [9] cycle_variance        — std dev de duraciones históricas (personalización on-device)
 *   [10] rolling_mean_3       — media de los últimos 3 ciclos cerrados
 *
 * Inyectado como @Singleton vía Hilt. Se inicializa lazily al primer uso.
 */
@Singleton
class LumiAIPredictor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "LumiAIPredictor"
        private const val MODEL_FILE   = "modelo_lumi.tflite"
        private const val SCALER_FILE  = "scaler_params.txt"
        private const val NUM_FEATURES = 11    // ← actualizado con el modelo retrenado

        /** Umbral de cycle_variance para considerar el ciclo como irregular */
        private const val IRREGULAR_VARIANCE_THRESHOLD = 3.5f
        /** Mínimo de ciclos cerrados para activar el modo TFLite */
        private const val MIN_CYCLES_FOR_AI = 3
        /**
         * Peso del TFLite en el blend final.
         * 0.40 = TFLite 40 %, predictor matemático 60 %.
         * Se puede subir a 0.6 cuando haya 6+ ciclos.
         */
        private const val TFLITE_BLEND_WEIGHT_LOW  = 0.40f   // 3–5 ciclos
        private const val TFLITE_BLEND_WEIGHT_HIGH = 0.60f   // 6+ ciclos
    }

    private var interpreter: Interpreter? = null
    private var means: FloatArray  = FloatArray(NUM_FEATURES)
    private var scales: FloatArray = FloatArray(NUM_FEATURES)
    private var isInitialized = false

    init {
        try {
            interpreter = Interpreter(loadModelFile())
            loadScalerParams()
            isInitialized = true
            Log.d(TAG, "Modelo TFLite (11 features) inicializado correctamente.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar el modelo TFLite: ${e.message}")
            isInitialized = false
        }
    }

    /** true si el modelo cargó correctamente y puede hacer inferencias. */
    fun isModelReady(): Boolean = isInitialized && interpreter != null

    // ─────────────────────────────────────────────────────────────────────────
    //  API pública
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Predicción combinada TFLite + matemática.
     *
     * @param closedCycles     Historial de ciclos cerrados (más reciente primero).
     * @param currentCycleLen  Longitud del ciclo actual (del predictor matemático).
     * @param periodLength     Duración del sangrado.
     * @param age              Edad de la usuaria (opcional).
     * @param height           Talla en cm (opcional).
     * @param weight           Peso en kg (opcional).
     * @return [AIPredictionResult] con la predicción final y metadatos de confianza.
     */
    fun predict(
        closedCycles    : List<CycleEntity>,
        currentCycleLen : Float,
        periodLength    : Float,
        age             : Float? = null,
        height          : Float? = null,
        weight          : Float? = null
    ): AIPredictionResult {

        // ── Métricas del historial ───────────────────────────────────────────
        val durations     = extractDurations(closedCycles)
        val cycleVariance = calculateStdDev(durations)
        val rollingMean3  = durations.take(3).let { if (it.isEmpty()) currentCycleLen else it.average().toFloat() }
        val isIrregular   = cycleVariance > IRREGULAR_VARIANCE_THRESHOLD
        val rangeDays     = if (isIrregular) (cycleVariance.toInt() + 1).coerceIn(2, 7) else 2

        // ── Sin modelo o sin datos suficientes → predictor matemático puro ───
        if (!isModelReady() || closedCycles.size < MIN_CYCLES_FOR_AI) {
            return AIPredictionResult(
                predictedCycleLength = currentCycleLen,
                confidenceScore      = if (closedCycles.isEmpty()) 0.1f else 0.4f,
                isIrregular          = isIrregular,
                predictionRangeDays  = rangeDays,
                source               = if (closedCycles.isEmpty()) PredictionSource.DEFAULT
                                       else PredictionSource.MATHEMATICAL
            )
        }

        // ── Construir el vector de features (orden idéntico al scaler_params.txt) ──
        val meanCycleLen       = rollingMean3                              // [0] MeanCycleLength
        val estimatedOvulation = (currentCycleLen - 14f).coerceIn(8f, 28f) // [1]
        val lutealPhaseLen     = 14f                                        // [2] constante clínica
        val mensesLen          = periodLength                               // [3]
        val totalMensesScore   = periodLength * 2f                          // [4] aproximación
        val ageVal             = age    ?: means[5]                         // [5] fallback a media
        val heightVal          = height ?: means[6]                         // [6]
        val weightVal          = weight ?: means[7]                         // [7]
        val bmi = if (height != null && weight != null && height > 0f) {
            val hm = height / 100f
            weight / (hm * hm)
        } else means[8]                                                     // [8] fallback a media
        // [9]  cycle_variance — clave para detectar irregularidad
        // [10] rolling_mean_3 — la media ponderada más reciente

        val rawFeatures = floatArrayOf(
            meanCycleLen, estimatedOvulation, lutealPhaseLen,
            mensesLen, totalMensesScore, ageVal, heightVal, weightVal, bmi,
            cycleVariance, rollingMean3
        )

        // ── Escalar (Z-score idéntico al entrenamiento en Python) ─────────────
        val scaledInput = FloatArray(NUM_FEATURES) { i ->
            val s = if (scales[i] != 0f) scales[i] else 1f
            (rawFeatures[i] - means[i]) / s
        }

        // ── Inferencia TFLite ─────────────────────────────────────────────────
        val input  = arrayOf(scaledInput)
        val output = arrayOf(FloatArray(1))

        return try {
            interpreter!!.run(input, output)
            val tflitePrediction = output[0][0].coerceIn(15f, 60f)

            // ── Blending adaptativo ───────────────────────────────────────────
            // Con más historial y datos biométricos reales, confiamos más en TFLite
            val blendWeight = if (closedCycles.size >= 6) TFLITE_BLEND_WEIGHT_HIGH
                              else TFLITE_BLEND_WEIGHT_LOW
            val blended = blendWeight * tflitePrediction + (1f - blendWeight) * currentCycleLen

            // ── Puntuación de confianza ───────────────────────────────────────
            val hasBiometrics = age != null && height != null && weight != null
            val cycleBonus    = (closedCycles.size.coerceAtMost(12) / 12f) * 0.30f
            val bioBonus      = if (hasBiometrics) 0.20f else 0f
            val confidence    = (0.50f + cycleBonus + bioBonus).coerceIn(0f, 1f)

            AIPredictionResult(
                predictedCycleLength = blended,
                confidenceScore      = confidence,
                isIrregular          = isIrregular,
                predictionRangeDays  = rangeDays,
                source               = PredictionSource.BLENDED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la inferencia TFLite: ${e.message}")
            AIPredictionResult(
                predictedCycleLength = currentCycleLen,
                confidenceScore      = 0.3f,
                isIrregular          = isIrregular,
                predictionRangeDays  = rangeDays,
                source               = PredictionSource.MATHEMATICAL
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers privados
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extrae las duraciones válidas de los ciclos cerrados (más reciente primero).
     * Filtra outliers biológicos (<15 o >60 días).
     */
    private fun extractDurations(closedCycles: List<CycleEntity>): List<Float> =
        closedCycles.mapNotNull { cycle ->
            if (cycle.endDate == null) return@mapNotNull null
            val start = Instant.ofEpochMilli(cycle.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
            val end   = Instant.ofEpochMilli(cycle.endDate).atZone(ZoneId.systemDefault()).toLocalDate()
            val d     = ChronoUnit.DAYS.between(start, end).toFloat()
            if (d in 15f..60f) d else null
        }

    /** Desviación estándar de una lista de duraciones. */
    private fun calculateStdDev(durations: List<Float>): Float {
        if (durations.size < 2) return 0f
        val mean     = durations.average().toFloat()
        val variance = durations.map { (it - mean) * (it - mean) }.average().toFloat()
        return sqrt(variance)
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fd = context.assets.openFd(MODEL_FILE)
        return FileInputStream(fd.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    private fun loadScalerParams() {
        val content = context.assets.open(SCALER_FILE).bufferedReader().use { it.readText() }
        content.lines().forEach { line ->
            when {
                line.startsWith("Means:") -> {
                    val vals = line.substringAfter("Means:").trim().split(",")
                    means = FloatArray(NUM_FEATURES) { i -> vals.getOrNull(i)?.trim()?.toFloatOrNull() ?: 0f }
                }
                line.startsWith("Scales:") -> {
                    val vals = line.substringAfter("Scales:").trim().split(",")
                    scales = FloatArray(NUM_FEATURES) { i -> vals.getOrNull(i)?.trim()?.toFloatOrNull() ?: 1f }
                }
            }
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }
}
