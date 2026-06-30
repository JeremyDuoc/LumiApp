package com.jeremy.lumi.ai

/**
 * Resultado de la predicción combinada (TFLite + matemática).
 *
 * @param predictedCycleLength  Duración predicha del próximo ciclo (días).
 * @param confidenceScore       0.0–1.0. Alta si hay historial suficiente + datos biométricos.
 * @param isIrregular           true si cycle_variance > 3.5 días (ciclo impredecible).
 * @param predictionRangeDays   Margen de incertidumbre en días. Ej: 2 → "entre martes y jueves".
 * @param source                De dónde provino la predicción final.
 */
data class AIPredictionResult(
    val predictedCycleLength : Float,
    val confidenceScore      : Float,       // 0.0 = sin datos, 1.0 = máxima confianza
    val isIrregular          : Boolean,
    val predictionRangeDays  : Int,         // Margen ± en días para mostrar en UI
    val source               : PredictionSource
)

enum class PredictionSource {
    TFLITE_MODEL,       // Usó el modelo de Deep Learning (tiene datos biométricos)
    MATHEMATICAL,       // Solo el promedio ponderado del historial (sin biométricos)
    BLENDED,            // Combinación ponderada de ambos
    DEFAULT             // Primera vez, sin historial — usa media del dataset de entrenamiento
}
