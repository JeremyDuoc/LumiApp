package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.dao.LumiDao
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.SymptomEntity
import com.jeremy.lumi.utils.LumiClock
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Inyecta 4 meses de historial clínico y bioestadístico de alta precisión para probar:
 *   1. Motor de Correlaciones Multivariable de Spearman (Sueño vs Dolor, Sueño vs Energía, Agua vs Dolor).
 *   2. Efectos Cruzados con Rezago Temporal (Lag-1: Estrés en T-1 dispara Dolor en T).
 *   3. Minería de Clústeres de Síntomas con Índice de Jaccard (J > 0.60) y Riesgo Relativo por Fase.
 *   4. Curva de Temperatura Basal (BBT) Sintotérmica con periodo 3-sobre-6 de la OMS / Criterios de Roetzer.
 *   5. Detección de Patrones Clínicos (PMDD según DSM-5, Tests de Ovulación LH (+)).
 *   6. Intervalos de Confianza Probabilísticos de Ciclo (80% y 95%).
 */
class MockDataGeneratorUseCase @Inject constructor(
    private val dao: LumiDao
) {
    suspend operator fun invoke() {
        // 1. Limpiamos la BD para evitar datos residuales o inconsistentes
        dao.clearAllDataForDev()

        val today = LumiClock.today()

        // 4 ciclos cerrados previos de alta fidelidad biológica (28, 29, 28, 28)
        val closedCycleLengths = listOf(28, 29, 28, 28)
        val currentCycleLength = 28
        val periodLength = 5

        // El ciclo actual lleva 14 días (hoy es el día 15, fase ovulatoria / lútea temprana)
        val currentCycleDaysPassed = 14
        val totalHistoricalDays = closedCycleLengths.sum() + currentCycleDaysPassed
        var cycleStartDate = today.minusDays(totalHistoricalDays.toLong())

        var previousDayStress = 3

        // 2. Generar los 4 ciclos pasados cerrados
        for (cycleLen in closedCycleLengths) {
            val cycleEndDate = cycleStartDate.plusDays(cycleLen.toLong() - 1)

            val cycle = CycleEntity(
                startDate = cycleStartDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                endDate = cycleEndDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                predictedOvulationDate = cycleStartDate.plusDays((cycleLen - 14 - 1).toLong())
                    .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                cycleLength = cycleLen,
                periodLength = periodLength
            )
            val cycleId = dao.insertCycle(cycle).toInt()

            previousDayStress = generateLogsForCycle(
                startDate = cycleStartDate,
                daysCount = cycleLen,
                periodLength = periodLength,
                cycleId = cycleId,
                initialStress = previousDayStress
            )

            cycleStartDate = cycleEndDate.plusDays(1)
        }

        // 3. Generar el ciclo actual activo (abierto, hasta el día de hoy)
        val activeCycle = CycleEntity(
            startDate = cycleStartDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            endDate = null,
            predictedOvulationDate = cycleStartDate.plusDays((currentCycleLength - 14 - 1).toLong())
                .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            cycleLength = currentCycleLength,
            periodLength = periodLength
        )
        val activeCycleId = dao.insertCycle(activeCycle).toInt()

        val daysUntilToday = java.time.temporal.ChronoUnit.DAYS.between(cycleStartDate, today).toInt()
        val daysToInject = (daysUntilToday + 1).coerceAtLeast(1)

        generateLogsForCycle(
            startDate = cycleStartDate,
            daysCount = daysToInject,
            periodLength = periodLength,
            cycleId = activeCycleId,
            initialStress = previousDayStress
        )
    }

    private suspend fun generateLogsForCycle(
        startDate: LocalDate,
        daysCount: Int,
        periodLength: Int,
        cycleId: Int,
        initialStress: Int
    ): Int {
        var prevStress = initialStress
        val ovulationDayIndex = 13 // Día 14 del ciclo (0-indexed 13)

        for (dayIndex in 0 until daysCount) {
            val date = startDate.plusDays(dayIndex.toLong())
            val dateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

            val isMenstrual = dayIndex < periodLength
            val isFollicular = dayIndex in periodLength until (ovulationDayIndex - 2)
            val isOvulation = dayIndex in (ovulationDayIndex - 2)..(ovulationDayIndex + 1)
            val isLutealMid = dayIndex in (ovulationDayIndex + 2)..(daysCount - 5)
            val isPms = dayIndex >= (daysCount - 4) // Últimos 4 días premenstruales

            // ── 1. Sueño vs Dolor y Energía (Fuerte correlación de Spearman) ──
            val isShortSleepDay = (dayIndex % 7 == 1) || (isPms && dayIndex % 2 == 0)
            val sleepHours = if (isShortSleepDay) {
                4.5f + Random.nextFloat() * 1.0f // 4.5 - 5.5h (sueño insuficiente)
            } else {
                7.5f + Random.nextFloat() * 1.2f // 7.5 - 8.7h (buen descanso)
            }

            // Energía directamente proporcional al sueño
            val energyLevel = if (sleepHours < 6.0f) {
                Random.nextInt(2, 5)
            } else {
                Random.nextInt(7, 10)
            }

            // ── 2. Estrés y Efecto Cruzado con Rezago Temporal (Lag-1) ───────
            val isStressSpike = (dayIndex % 6 == 3) || isPms
            val stressLevel = if (isStressSpike) {
                Random.nextInt(7, 10)
            } else {
                Random.nextInt(2, 5)
            }

            // Dolor basal + impacto del sueño + impacto de menstruación + rezago temporal (Lag-1 de estrés ayer)
            var pain = 0
            if (isMenstrual) {
                pain = if (dayIndex < 2) Random.nextInt(6, 9) else Random.nextInt(3, 6)
            } else if (isPms) {
                pain = Random.nextInt(2, 5)
            }

            // Si ayer hubo estrés alto (Lag-1), hoy detona +2 a +3 de dolor pélvico o cefalea
            if (prevStress >= 7) {
                pain = (pain + Random.nextInt(2, 4)).coerceAtMost(10)
            }

            // Si durmió poco, el dolor se amplifica
            if (sleepHours < 6.0f && pain > 0) {
                pain = (pain + 2).coerceAtMost(10)
            }

            // ── 3. Hidratación (Agua) ─────────────────────────────────────────
            val waterIntake = if (sleepHours > 7.0f && stressLevel < 6) {
                2.2f + Random.nextFloat() * 0.6f // 2.2 - 2.8 L
            } else {
                1.0f + Random.nextFloat() * 0.5f // 1.0 - 1.5 L
            }

            // ── 4. Temperatura Basal Sintotérmica (OMS periodo 3-sobre-6) ───────
            val bbt = when {
                dayIndex < ovulationDayIndex -> {
                    36.18f + (Random.nextFloat() * 0.14f) // 36.18 - 36.32
                }
                dayIndex == ovulationDayIndex -> {
                    36.15f + (Random.nextFloat() * 0.08f) // Bajada ovulatoria
                }
                else -> {
                    36.65f + (Random.nextFloat() * 0.18f) // 36.65 - 36.83 (salto térmico)
                }
            }

            // ── 5. Moco Cervical y Relaciones Sexuales ────────────────────────
            val cervicalMucus = when {
                isMenstrual -> null
                isFollicular -> "Cremoso"
                dayIndex in (ovulationDayIndex - 2)..ovulationDayIndex -> "Clara de Huevo"
                else -> "Seco"
            }

            val hadSex = dayIndex in listOf(11, 13, 20)
            val protection = if (hadSex) true else null
            val method = if (hadSex) "Preservativo" else null

            // ── 6. Flujo Menstrual ───────────────────────────────────────────
            val flowIntensity = when (dayIndex) {
                0, 1 -> "Abundante"
                2 -> "Medio"
                3, 4 -> "Ligero"
                else -> null
            }

            // ── 7. Estados de Ánimo (Patrón Clínico PMDD de DSM-5) ────────────
            val mood = when {
                isPms -> when (dayIndex % 3) {
                    0 -> "Irritable"
                    1 -> "Ansiosa"
                    else -> "Abrumada"
                }
                isMenstrual -> if (dayIndex < 2) "En calma" else "Cansada"
                isOvulation -> "Energética"
                else -> "Feliz"
            }

            // Guardar DailyLogEntity
            val logEntity = DailyLogEntity(
                cycleId = cycleId,
                date = dateMillis,
                flowIntensity = flowIntensity,
                painLevel = pain,
                spotting = false,
                mood = mood,
                energyLevel = energyLevel,
                sleepHours = (sleepHours * 10).roundToInt() / 10f,
                stressLevel = stressLevel,
                waterIntake = (waterIntake * 10).roundToInt() / 10f,
                basalBodyTemp = (bbt * 100).roundToInt() / 100f,
                cervicalMucus = cervicalMucus,
                hadIntercourse = hadSex,
                protectionUsed = protection,
                contraceptionMethod = method,
                notes = if (isOvulation) "Pico de energía y moco clara de huevo fértil" else null
            )
            val logId = dao.insertDailyLog(logEntity).toInt()

            // ── 8. Inyección de Clústeres de Síntomas con Alto Jaccard (J >= 0.60) ──
            val symptomsToInsert = mutableListOf<String>()

            // Clúster 1: Menstrual ("Cólicos" + "Dolor lumbar" + "Fatiga") -> J > 0.80 en Menstruación
            if (isMenstrual && dayIndex in 0..2) {
                symptomsToInsert.add("Cólicos")
                symptomsToInsert.add("Dolor lumbar")
                symptomsToInsert.add("Fatiga")
            }

            // Clúster 2: Premenstrual / Lúteo ("Migraña" + "Náuseas" + "Hinchazón" + "Sensibilidad Senos") -> J > 0.70 en Fase Lútea
            if (isPms) {
                symptomsToInsert.add("Migraña")
                symptomsToInsert.add("Náuseas")
                symptomsToInsert.add("Hinchazón")
                symptomsToInsert.add("Sensibilidad Senos")
            }

            // Clúster 3: Ovulatorio ("Test ovulación LH (+)" + "Deseo sexual alto")
            if (dayIndex == ovulationDayIndex || dayIndex == (ovulationDayIndex - 1)) {
                symptomsToInsert.add("Test ovulación LH (+)")
                symptomsToInsert.add("Deseo sexual alto")
            }

            if (symptomsToInsert.isNotEmpty()) {
                val entities = symptomsToInsert.map { name ->
                    SymptomEntity(
                        dailyLogId = logId,
                        name = name,
                        intensity = 5
                    )
                }
                dao.insertSymptoms(entities)
            }

            prevStress = stressLevel
        }

        return prevStress
    }
}
