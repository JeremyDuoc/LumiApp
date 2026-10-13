package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.dao.LumiDao
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.SymptomEntity
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import com.jeremy.lumi.utils.LumiClock
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Inyecta 6 meses de historial clínico con alta irregularidad biológica y registro denso de síntomas.
 *
 * Diseñado para validar exhaustivamente:
 *   1. Motor de Correlaciones de Spearman (Sueño vs Dolor, Sueño vs Energía, Agua vs Dolor con N > 190).
 *   2. Efectos Cruzados con Rezago Temporal (Lag-1: Estrés en T-1 dispara Dolor en T).
 *   3. Minería de Clústeres de Síntomas con Índice de Jaccard (J >= 0.60) y Riesgo Relativo por Fase.
 *   4. Curva de Temperatura Basal (BBT) Sintotérmica (caída ovulatoria + salto térmico de progesterona).
 *   5. Detección de Patrones Clínicos y Alertas Médicas (SOP / Irregularidad por alta varianza sigma >= 8.0,
 *      PMDD según DSM-5, Endometriosis por dolor recurrente EVA >= 7, STRAW+10 por saltos > 7d).
 *   6. Intervalos de Confianza y Factores de Certeza en Home ("Certeza moderada / En calibración",
 *      barra de Regularidad baja ~15-25%, Historial 98%, Síntomas densos 85%+).
 */
class MockDataGeneratorUseCase @Inject constructor(
    private val dao: LumiDao,
    private val onboardingPreferenceManager: OnboardingPreferenceManager
) {
    suspend operator fun invoke() {
        // 1. Limpiamos la BD para asegurar un entorno de pruebas limpio
        dao.clearAllDataForDev()

        // 2. Ajustamos preferencias: Onboarding listo, ciclo reportado irregular, todas las categorías activas
        onboardingPreferenceManager.markOnboardingCompleted(isObserver = false)
        onboardingPreferenceManager.setIsRegular(false)
        onboardingPreferenceManager.setActiveLogCategories(
            setOf("physical", "digestive", "mucus", "intercourse", "wellbeing", "vitals")
        )

        val today = LumiClock.today()

        // 6 ciclos cerrados históricos de duraciones marcadamente irregulares:
        // [38d, 22d, 42d, 24d, 44d, 26d] -> Amplitud: 22 a 44 días (22 días de variación).
        // Diferencias consecutivas: 16d, 20d, 18d, 20d, 18d (todas >= 7d, activando STRAW+10 / Perimenopausia / SOP).
        // Desviación típica sigma ≈ 8.9 días (activando alerta médica de varianza alta PCOS_HIGH_VARIANCE_THRESHOLD = 8.0).
        // Suma de días cerrados: 38 + 22 + 42 + 24 + 44 + 26 = 196 días (~6.5 meses).
        val closedCycleLengths = listOf(38, 22, 42, 24, 44, 26)
        val periodLengths      = listOf(6,  4,  6,  4,  7,  5)

        // El ciclo activo actual empezó hace 14 días (hoy es el día 15, fase folicular tardía / pre-ovulatoria)
        val currentCycleDaysPassed = 14
        val currentEstimatedLength = 33
        val currentPeriodLength    = 5

        val totalHistoricalDays = closedCycleLengths.sum() + currentCycleDaysPassed
        var cycleStartDate = today.minusDays(totalHistoricalDays.toLong())

        var previousDayStress = 3

        // 3. Generar los 6 ciclos pasados cerrados
        for (i in closedCycleLengths.indices) {
            val cycleLen = closedCycleLengths[i]
            val periodLen = periodLengths[i]
            val cycleEndDate = cycleStartDate.plusDays(cycleLen.toLong() - 1)

            // Luteal phase fisiológica promedio de 14 días
            val ovDayIndex = (cycleLen - 14 - 1).coerceAtLeast(periodLen + 1)
            val predictedOvulationDate = cycleStartDate.plusDays(ovDayIndex.toLong())
                .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

            val cycle = CycleEntity(
                startDate = cycleStartDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                endDate = cycleEndDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                predictedOvulationDate = predictedOvulationDate,
                cycleLength = cycleLen,
                periodLength = periodLen
            )
            val cycleId = dao.insertCycle(cycle).toInt()

            previousDayStress = generateLogsForCycle(
                startDate = cycleStartDate,
                daysCount = cycleLen,
                periodLength = periodLen,
                cycleId = cycleId,
                initialStress = previousDayStress,
                isClosedCycle = true
            )

            cycleStartDate = cycleEndDate.plusDays(1)
        }

        // 4. Generar el ciclo actual activo (abierto, hasta el día de hoy)
        val activeOvDayIndex = (currentEstimatedLength - 14 - 1).coerceAtLeast(currentPeriodLength + 1)
        val activeCycle = CycleEntity(
            startDate = cycleStartDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            endDate = null,
            predictedOvulationDate = cycleStartDate.plusDays(activeOvDayIndex.toLong())
                .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            cycleLength = currentEstimatedLength,
            periodLength = currentPeriodLength
        )
        val activeCycleId = dao.insertCycle(activeCycle).toInt()

        val daysUntilToday = java.time.temporal.ChronoUnit.DAYS.between(cycleStartDate, today).toInt()
        val daysToInject = (daysUntilToday + 1).coerceAtLeast(1)

        generateLogsForCycle(
            startDate = cycleStartDate,
            daysCount = daysToInject,
            periodLength = currentPeriodLength,
            cycleId = activeCycleId,
            initialStress = previousDayStress,
            isClosedCycle = false
        )
    }

    private suspend fun generateLogsForCycle(
        startDate: LocalDate,
        daysCount: Int,
        periodLength: Int,
        cycleId: Int,
        initialStress: Int,
        isClosedCycle: Boolean
    ): Int {
        var prevStress = initialStress
        // En ciclos irregulares la ovulación se desplaza según la duración total del ciclo
        val ovulationDayIndex = (daysCount - 14 - 1).coerceAtLeast(periodLength + 2)

        for (dayIndex in 0 until daysCount) {
            val date = startDate.plusDays(dayIndex.toLong())
            val dateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

            val isMenstrual  = dayIndex < periodLength
            val isFollicular = dayIndex in periodLength until (ovulationDayIndex - 2)
            val isOvulation  = dayIndex in (ovulationDayIndex - 2)..(ovulationDayIndex + 1)
            val isLutealMid  = dayIndex in (ovulationDayIndex + 2)..(daysCount - 5)
            val isPms        = dayIndex >= (daysCount - 4) && isClosedCycle

            // ── 1. Sueño vs Dolor y Energía (Fuerte correlación de Spearman rs < -0.55) ──
            val isShortSleepDay = (dayIndex % 7 == 2) || (isPms && dayIndex % 2 == 0)
            val sleepHours = if (isShortSleepDay) {
                4.2f + Random.nextFloat() * 1.2f // 4.2h - 5.4h (insuficiente)
            } else {
                7.4f + Random.nextFloat() * 1.3f // 7.4h - 8.7h (descanso óptimo)
            }

            // Energía directamente proporcional al sueño y a la fase biológica
            val energyLevel = when {
                sleepHours < 5.8f -> Random.nextInt(2, 5)
                isOvulation       -> Random.nextInt(8, 11)
                isFollicular      -> Random.nextInt(7, 10)
                isMenstrual       -> Random.nextInt(3, 6)
                else              -> Random.nextInt(4, 7)
            }

            // ── 2. Estrés y Efecto Cruzado con Rezago Temporal (Lag-1) ──────────
            val isStressSpike = (dayIndex % 6 == 3) || (isPms && dayIndex % 3 == 1)
            val stressLevel = if (isStressSpike) {
                Random.nextInt(7, 10)
            } else {
                Random.nextInt(2, 5)
            }

            // ── 3. Dolor (Escala EVA 0-10) ──────────────────────────────────────
            var pain = 0
            if (isMenstrual) {
                pain = when (dayIndex) {
                    0 -> Random.nextInt(7, 10) // Dolor menstrual severo día 1 (Endo check)
                    1 -> Random.nextInt(6, 9)  // Día 2
                    2 -> Random.nextInt(4, 6)
                    else -> Random.nextInt(1, 4)
                }
            } else if (isOvulation) {
                // Dolor intermenstrual pélvico (Mittelschmerz)
                if (dayIndex == ovulationDayIndex || dayIndex == ovulationDayIndex - 1) {
                    pain = Random.nextInt(2, 5)
                }
            } else if (isPms) {
                pain = Random.nextInt(3, 6) // Cefalea o dolor lumbar premenstrual
            }

            // Efecto Rezago Temporal (Lag-1): estrés alto ayer detona dolor pélvico/cefalea hoy
            if (prevStress >= 7 && dayIndex > 0) {
                pain = (pain + Random.nextInt(2, 4)).coerceAtMost(10)
            }

            // Falta de sueño amplifica el dolor percibido
            if (sleepHours < 5.8f && pain > 0) {
                pain = (pain + 2).coerceAtMost(10)
            }

            // ── 4. Hidratación (Agua) ───────────────────────────────────────────
            val waterIntake = if (sleepHours > 7.0f && stressLevel < 6) {
                2.2f + Random.nextFloat() * 0.6f // 2.2L - 2.8L
            } else {
                1.0f + Random.nextFloat() * 0.5f // 1.0L - 1.5L
            }

            // ── 5. Curva de Temperatura Basal (BBT) Sintotérmica ────────────────
            val bbt = when {
                dayIndex < ovulationDayIndex -> {
                    36.18f + (Random.nextFloat() * 0.14f) // Fase folicular baja: 36.18 - 36.32
                }
                dayIndex == ovulationDayIndex -> {
                    36.10f + (Random.nextFloat() * 0.08f) // Bajada ovulatoria
                }
                else -> {
                    36.66f + (Random.nextFloat() * 0.19f) // Salto térmico lúteo: 36.66 - 36.85
                }
            }

            // ── 6. Flujo Menstrual y Manchado (Spotting) ─────────────────────────
            val flowIntensity = when {
                isMenstrual && dayIndex in 0..1 -> "Abundante"
                isMenstrual && dayIndex == 2    -> "Medio"
                isMenstrual                     -> "Ligero"
                else                            -> null
            }

            // En ciclos anovulatorios / largos (>= 38d), spotting intermenstrual frecuente
            val isSpottingDay = (daysCount >= 38 && dayIndex in listOf(16, 17)) ||
                                (isMenstrual && dayIndex == periodLength - 1)

            // ── 7. Moco Cervical ─────────────────────────────────────────────────
            val cervicalMucus = when {
                isMenstrual -> null
                dayIndex in (ovulationDayIndex - 2)..ovulationDayIndex -> "Clara de Huevo"
                dayIndex in (ovulationDayIndex - 4)..(ovulationDayIndex - 3) -> "Acuoso"
                isFollicular -> "Cremoso"
                isLutealMid  -> "Pegajoso"
                else         -> "Seco"
            }

            // ── 8. Actividad Sexual ──────────────────────────────────────────────
            val hadSex = dayIndex in listOf(ovulationDayIndex - 2, ovulationDayIndex, ovulationDayIndex + 4)
            val protection = if (hadSex) (dayIndex % 2 == 0) else null
            val method = if (hadSex && protection == true) "Condón" else null

            // ── 9. Estados de Ánimo (Patrón Clínico PMDD de DSM-5) ───────────────
            val mood = when {
                isPms -> when (dayIndex % 3) {
                    0 -> "Irritable"
                    1 -> "Ansiosa"
                    else -> "Abrumada"
                }
                isMenstrual -> if (dayIndex < 2) "Cansada" else "En calma"
                isOvulation -> "Enérgica"
                isFollicular -> if (dayIndex % 2 == 0) "Feliz" else "Enérgica"
                else -> "Calmada"
            }

            // ── 10. Notas Contextuales Clínicas ──────────────────────────────────
            val note = when {
                isMenstrual && dayIndex == 0 -> "Inicio de periodo con cólicos fuertes y dolor lumbar. Guatero caliente."
                isMenstrual && dayIndex == 1 -> "Flujo abundante, reposo en cama."
                dayIndex == ovulationDayIndex -> "Test LH positivo marcado y moco elástico. Puntada en ovario."
                isSpottingDay && !isMenstrual -> "Leve manchado intermenstrual rosado."
                isPms && dayIndex == daysCount - 2 -> "Cefalea intensa, hinchazón y bastante irritabilidad hoy."
                !isClosedCycle && dayIndex == daysCount - 1 -> "Día 15 del ciclo: energía alta y piel radiante."
                else -> null
            }

            // Guardar DailyLogEntity
            val logEntity = DailyLogEntity(
                cycleId = cycleId,
                date = dateMillis,
                flowIntensity = flowIntensity,
                painLevel = pain,
                spotting = isSpottingDay,
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
                notes = note
            )
            val logId = dao.insertDailyLog(logEntity).toInt()

            // ── 11. Inyección Densa de Síntomas con Alto Jaccard (J >= 0.60) ─────
            val symptomsToInsert = mutableListOf<String>()

            // Clúster 1: Menstrual ("Cólicos" + "Dolor lumbar" + "Fatiga" + "Dolor de cabeza")
            if (isMenstrual) {
                symptomsToInsert.add("Cólicos")
                symptomsToInsert.add("Dolor lumbar")
                symptomsToInsert.add("Fatiga")
                if (dayIndex <= 1) {
                    symptomsToInsert.add("Dolor de cabeza")
                    symptomsToInsert.add("Prob. Digestivos")
                }
            }

            // Clúster 2: Folicular ("Acné" + "Deseo sexual alto")
            if (isFollicular) {
                if (dayIndex in periodLength..(periodLength + 2)) {
                    symptomsToInsert.add("Acné")
                }
                if (dayIndex >= ovulationDayIndex - 4) {
                    symptomsToInsert.add("Deseo sexual alto")
                }
            }

            // Clúster 3: Ovulatorio ("Test ovulación LH (+)" + "Deseo sexual alto" + "Dolor Pélvico")
            if (isOvulation) {
                if (dayIndex == ovulationDayIndex || dayIndex == ovulationDayIndex - 1) {
                    symptomsToInsert.add("Test ovulación LH (+)")
                    symptomsToInsert.add("Dolor Pélvico")
                }
                symptomsToInsert.add("Deseo sexual alto")
                symptomsToInsert.add("Sensibilidad Senos")
            }

            // Clúster 4: Lúteo Medio ("Sensibilidad Senos" + "Hinchazón" + "Estreñimiento")
            if (isLutealMid) {
                symptomsToInsert.add("Sensibilidad Senos")
                symptomsToInsert.add("Hinchazón")
                if (dayIndex % 2 == 0) {
                    symptomsToInsert.add("Estreñimiento")
                    symptomsToInsert.add("Fatiga")
                }
            }

            // Clúster 5: Premenstrual / SPM ("Migraña" + "Náuseas" + "Hinchazón" + "Sensibilidad Senos" + "Insomnio")
            if (isPms) {
                symptomsToInsert.add("Migraña")
                symptomsToInsert.add("Náuseas")
                symptomsToInsert.add("Hinchazón")
                symptomsToInsert.add("Sensibilidad Senos")
                symptomsToInsert.add("Insomnio")
                symptomsToInsert.add("Dolor lumbar")
                if (daysCount >= 38) {
                    // Síntoma de transición perimenopáusica en ciclos largos
                    symptomsToInsert.add("Sofocos / Bochornos")
                }
            }

            // Insertar todos los síntomas generados para este día
            if (symptomsToInsert.isNotEmpty()) {
                val entities = symptomsToInsert.map { name ->
                    SymptomEntity(
                        dailyLogId = logId,
                        name = name,
                        intensity = if (isMenstrual || isPms) 7 else 5
                    )
                }
                dao.insertSymptoms(entities)
            }

            prevStress = stressLevel
        }

        return prevStress
    }
}
