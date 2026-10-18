package com.jeremy.lumi.ai

/**
 * Resultado auxiliar del predictor local de ciclos. El modelo experimental está desactivado.
 *
 * @param predictedCycleLength  Duración predicha del próximo ciclo (días).
 * @param confidenceScore       Índice descriptivo histórico; NO probabilidad clínica.
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
    BLENDED,            // Legacy: no se usa en la versión de producción
    RIDGE_V2,           // Modelo residual experimental solo si se valida y habilita
    DEFAULT             // Primera vez, sin historial — usa media del dataset de entrenamiento
}
