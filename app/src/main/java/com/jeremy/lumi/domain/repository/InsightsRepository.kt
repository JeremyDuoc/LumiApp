package com.jeremy.lumi.domain.repository

import com.jeremy.lumi.domain.model.CycleInsights
import com.jeremy.lumi.domain.model.HistoricalCycleStats
import com.jeremy.lumi.domain.model.MoodDistribution
import com.jeremy.lumi.domain.model.SymptomCorrelation

/**
 * Contrato de la capa de agregación de insights.
 *
 * Separa la matemática de agregación del repositorio de datos crudos
 * ([LumiRepository]), siguiendo el principio de responsabilidad única.
 * La implementación ([InsightsRepositoryImpl]) vive en la capa de datos.
 */
interface InsightsRepository {

    /**
     * Devuelve el objeto raíz con todos los insights calculados.
     * Consume datos de Room a través del DAO y aplica toda la matemática
     * internamente — el ViewModel solo necesita llamar a este método.
     *
     * @param cyclesLimit Máximo de ciclos a analizar (por defecto los últimos 12).
     */
    suspend fun getInsights(cyclesLimit: Int = 12): CycleInsights

    /**
     * Estadísticas del historial de ciclos cerrados.
     * Incluye promedio ponderado, fase lútea personal y máximo retraso histórico.
     */
    suspend fun getHistoricalCycleStats(): HistoricalCycleStats?

    /**
     * Correlaciones de síntomas por fase del ciclo.
     * Ordena los resultados de mayor a menor [SymptomCorrelation.totalOccurrences].
     *
     * @param minOccurrences Filtra síntomas que ocurrieron menos de N veces (evita ruido).
     */
    suspend fun getSymptomCorrelations(minOccurrences: Int = 2): List<SymptomCorrelation>

    /**
     * Distribución de humor en un rango de fechas (epoch millis).
     * Pasar [from] = 0 y [to] = Long.MAX_VALUE para el historial completo.
     */
    suspend fun getMoodDistribution(from: Long, to: Long): MoodDistribution
}
