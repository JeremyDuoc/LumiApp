package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.dao.LumiDao
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.SymptomEntity
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.random.Random

/**
 * Inyecta meses de datos históricos para poder testear los Insights, 
 * Calendario y Gráficos sin tener que ingresar datos manualmente uno por uno.
 * ESTO ES UNA HERRAMIENTA SOLO PARA DESARROLLO/TESTING.
 */
class MockDataGeneratorUseCase @Inject constructor(
    private val dao: LumiDao
) {
    suspend operator fun invoke() {
        // 1. Limpiamos la BD para evitar mezclar datos
        dao.clearAllDataForDev()

        val today = LocalDate.now()
        val symptomsList = listOf("Cansancio", "Dolor de cabeza", "Cólicos", "Hinchazón", "Antojos")

        // Simularemos 8 ciclos muy irregulares para probar el motor de ML
        val irregularCycleLengths = listOf(24, 38, 29, 45, 26, 31, 41, 28)
        // Hacemos que el ciclo actual lleve 20 días para tener datos suficientes para gráficas
        val totalDays = irregularCycleLengths.dropLast(1).sum() + 20
        var currentCycleStart = today.minusDays(totalDays.toLong())

        // 2. Generar 7 ciclos pasados cerrados
        for (i in 0 until 7) {
            val cycleLength = irregularCycleLengths[i]
            val periodLength = Random.nextInt(4, 7)
            val cycleEnd = currentCycleStart.plusDays(cycleLength.toLong() - 1)

            val cycle = CycleEntity(
                startDate = currentCycleStart.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                endDate = cycleEnd.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                predictedOvulationDate = null,
                cycleLength = cycleLength,
                periodLength = periodLength
            )
            val cycleId = dao.insertCycle(cycle).toInt()

            // Generar logs para este ciclo
            generateLogsForCycle(currentCycleStart, cycleLength, periodLength, symptomsList, cycleId)

            currentCycleStart = cycleEnd.plusDays(1)
        }

        // 3. Generar el ciclo actual (abierto)
        val cycleLength = irregularCycleLengths.last()
        val periodLength = 5
        val cycle = CycleEntity(
            startDate = currentCycleStart.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            endDate = null,
            predictedOvulationDate = null,
            cycleLength = cycleLength,
            periodLength = periodLength
        )
        val cycleId = dao.insertCycle(cycle).toInt()
        
        val daysUntilToday = java.time.temporal.ChronoUnit.DAYS.between(currentCycleStart, today).toInt()
        // Asegurarnos de no inyectar días en el futuro
        val daysToInject = if (daysUntilToday >= 0) daysUntilToday + 1 else 0
        if (daysToInject > 0) {
            generateLogsForCycle(currentCycleStart, daysToInject, periodLength, symptomsList, cycleId)
        }
    }

    private suspend fun generateLogsForCycle(
        start: LocalDate,
        days: Int,
        periodLength: Int,
        symptomsList: List<String>,
        cycleId: Int
    ) {
        for (day in 0 until days) {
            val date = start.plusDays(day.toLong())
            val dateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

            val isPeriod = day < periodLength
            val isPms = day > (days - 5) // Últimos 5 días (SPM)

            // Lógica para que las correlaciones bivariables brillen
            // Si duerme poco (< 6h), el dolor es más alto.
            val isShortSleep = Random.nextFloat() < 0.3f
            val sleepHours = if (isShortSleep) Random.nextInt(3, 6).toFloat() else Random.nextInt(6, 10).toFloat()
            
            // Si el estrés es alto (>=7), la energía es baja.
            val isHighStress = Random.nextFloat() < 0.2f
            val stress = if (isHighStress) Random.nextInt(7, 10) else Random.nextInt(1, 5)
            val energy = if (isHighStress) Random.nextInt(1, 4) else Random.nextInt(5, 10)

            // Dolor fuerte durante los primeros días de menstruación
            var pain = 0
            if (isPeriod) {
                pain = if (day < 2) Random.nextInt(6, 10) else Random.nextInt(2, 6)
                // Inyectar correlación bivariable (Sueño vs Dolor)
                if (isShortSleep) pain = (pain * 1.3f).toInt().coerceAtMost(10)
            } else if (isPms) {
                pain = Random.nextInt(1, 5)
            }

            // Lógica para temperatura basal (BBT) bifásica simulada
            val isFollicular = day < (days - 14).coerceAtLeast(10) // Aproximación ovulación
            val bbt = if (isFollicular) {
                36.1f + Random.nextFloat() * 0.3f // 36.1 - 36.4
            } else {
                36.5f + Random.nextFloat() * 0.4f // 36.5 - 36.9
            }

            val log = DailyLogEntity(
                cycleId = cycleId,
                date = dateMillis,
                flowIntensity = if (isPeriod) "Medio" else null,
                painLevel = pain,
                mood = if (isPeriod || isPms) "Triste" else "Feliz",
                sleepHours = sleepHours,
                energyLevel = energy,
                stressLevel = stress,
                basalBodyTemp = bbt
            )
            val logId = dao.insertDailyLog(log).toInt()

            // Insertar algunos síntomas físicos (fase lútea/menstrual)
            if (isPeriod || isPms) {
                val numSymptoms = Random.nextInt(1, 3)
                val selected = symptomsList.shuffled().take(numSymptoms)
                selected.forEach { sym ->
                    dao.insertSymptoms(listOf(SymptomEntity(dailyLogId = logId, name = sym, intensity = 5)))
                }
            }
        }
    }
}
