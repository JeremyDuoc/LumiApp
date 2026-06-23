package com.jeremy.lumi.domain.model

// ─────────────────────────────────────────────────────────────────────────────
//  MODELOS DE AGREGACIÓN E INSIGHTS
//
//  Estas clases son datos "calculados" — no van en Room, solo en el dominio.
//  Las produce InsightsRepository y las consumen los ViewModels de los gráficos.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Estadísticas de un ciclo individual o del promedio histórico.
 *
 * @param durationDays       Duración total del ciclo (startDate→endDate).
 * @param bleedingDays       Días donde flowIntensity != null (días con regla registrada).
 * @param cycleType          Clasificación según duración: CORTO (<21d), NORMAL, LARGO (>35d).
 * @param avgPainLevel       Promedio de nivel de dolor durante la menstruación.
 */
data class CycleStats(
    val durationDays : Int,
    val bleedingDays : Int,
    val cycleType    : CycleType,
    val avgPainLevel : Float
)

enum class CycleType { SHORT, NORMAL, LONG }

/**
 * Correlación entre un síntoma y la fase del ciclo en la que ocurre con más frecuencia.
 *
 * El porcentaje representa: (días con ese síntoma en esa fase) / (total días con ese síntoma)
 * a lo largo de todos los ciclos analizados. Un valor alto indica patrón claro.
 *
 * @param symptomName       Nombre del síntoma (ej. "Dolor de cabeza").
 * @param dominantPhase     Fase donde ocurre más veces.
 * @param occurrenceByPhase Distribución completa por fase (para el gráfico de barras).
 * @param totalOccurrences  Total de días donde se registró este síntoma.
 */
data class SymptomCorrelation(
    val symptomName       : String,
    val dominantPhase     : CyclePhase,
    val occurrenceByPhase : Map<CyclePhase, Int>,   // fase → nº de días
    val totalOccurrences  : Int
) {
    /** Porcentaje (0..1) de ocurrencia en la fase dominante. */
    val dominantPhaseRatio: Float
        get() = if (totalOccurrences == 0) 0f
                else (occurrenceByPhase[dominantPhase] ?: 0) / totalOccurrences.toFloat()
}

/**
 * Distribución de humor en un período de tiempo.
 *
 * @param distribution  Mapa mood-string → nº de días registrados con ese humor.
 * @param totalDays     Total de días con mood registrado en el período.
 * @param dominantMood  El humor más frecuente (null si no hay datos).
 */
data class MoodDistribution(
    val distribution : Map<String, Int>,   // mood → count
    val totalDays    : Int,
    val dominantMood : String?
) {
    /** Porcentaje (0..1) de un mood específico sobre el total. */
    fun ratioOf(mood: String): Float =
        if (totalDays == 0) 0f else (distribution[mood] ?: 0) / totalDays.toFloat()
}

/**
 * Estadísticas resumidas del historial completo de ciclos.
 *
 * @param cycleCount            Número de ciclos cerrados analizados.
 * @param avgCycleLength        Promedio ponderado (más peso a ciclos recientes).
 * @param avgPeriodLength       Promedio de días de menstruación.
 * @param shortestCycle         Duración del ciclo más corto registrado.
 * @param longestCycle          Duración del ciclo más largo registrado.
 * @param personalLutealLength  Fase lútea personal calculada desde historial.
 *                              Si no hay suficientes datos, usa 14 (estándar).
 * @param maxHistoricalDelay    Máximo retraso histórico observado — se usa para
 *                              decidir cuándo mostrar una alerta de retraso real.
 */
data class HistoricalCycleStats(
    val cycleCount           : Int,
    val avgCycleLength       : Float,
    val avgPeriodLength      : Float,
    val shortestCycle        : Int,
    val longestCycle         : Int,
    val personalLutealLength : Int,
    val maxHistoricalDelay   : Int
)

/**
 * Contenedor raíz que agrega todos los insights producidos por [InsightsRepository].
 * El ViewModel lo expone como un único StateFlow y la UI lo desestructura por sección.
 */
data class CycleInsights(
    val historicalStats      : HistoricalCycleStats?,
    val symptomCorrelations  : List<SymptomCorrelation>,
    val moodDistribution     : MoodDistribution?,
    val recentCycleStats     : List<CycleStats>         // últimos N ciclos para el gráfico de barras
)
