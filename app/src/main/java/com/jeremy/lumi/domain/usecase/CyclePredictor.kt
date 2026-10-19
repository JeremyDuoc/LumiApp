package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.closedLengthDays
import com.jeremy.lumi.domain.model.CyclePhase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

// ─── Estado de retraso ────────────────────────────────────────────────────────
enum class DelayState {
    ON_TIME,          // dentro del ciclo esperado
    LATE,             // pasó la fecha esperada (1–7 días)
    VERY_LATE,        // retraso significativo (8–14 días)
    EXTREMELY_LATE    // >14 días — sugerir consulta
}

data class CyclePrediction(
    val currentDayOfCycle   : Int,
    val currentPhase        : CyclePhase,
    val nextPeriodDate      : LocalDate,
    val daysUntilNextPeriod : Int,
    val nextOvulationDate   : LocalDate,
    val daysUntilOvulation  : Int,
    val cycleLength         : Int,
    val periodLength        : Int,
    val delayState          : DelayState,
    val delayDays           : Int,
    val isLate              : Boolean,
    val dayOfPhase          : Int,
    // --- Campos de fertilidad clínica (OMS / ACOG) ---
    val fertileWindowStartDate : LocalDate = if (nextOvulationDate == LocalDate.MAX) LocalDate.MAX else nextOvulationDate.minusDays(5),
    val fertileWindowEndDate   : LocalDate = if (nextOvulationDate == LocalDate.MAX) LocalDate.MAX else nextOvulationDate.plusDays(1),
    val isInFertileWindow      : Boolean = false,
    val isOvulationPeak        : Boolean = false,
    // New probabilistic fields (defaults preserve existing behavior)
    val earliestPeriodDate: LocalDate = nextPeriodDate, // approximate lower bound
    val latestPeriodDate: LocalDate = nextPeriodDate,   // approximate upper bound
    val predictionConfidenceIndex: Float = 0.5f,         // historical info index, NOT accuracy probability
    val predictedCycleLengthStdDev: Float = 0f,          // historical observed standard deviation
    val fertilityIsEstimated: Boolean = true             // calendar estimate, NOT confirmed ovulation
)

object CyclePredictor {

    // Fase lútea estándar — se usa cuando no hay suficiente historial personal
    private const val LUTEAL_LENGTH_DEFAULT = 14

    // ── Umbrales de retraso ───────────────────────────────────────────────────
    private const val VERY_LATE_THRESHOLD = 8
    private const val EXTREME_THRESHOLD   = 15

    // Mínimo de ciclos cerrados para activar el predictor histórico
    private const val MIN_CYCLES_FOR_HISTORY = 3

    // Factor de decaimiento exponencial: el ciclo más reciente pesa el doble
    // que el de hace 5 ciclos. λ = ln(2)/5 ≈ 0.1386
    private const val DECAY_LAMBDA = 0.1386

    // ─────────────────────────────────────────────────────────────────────────
    //  API PÚBLICA — helpers de calendario
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Día real dentro del ciclo (1-based, sin módulo).
     * Puede ser > cycleLength si hay retraso.
     */
    fun dayInCycle(startDate: LocalDate, cycleLength: Int, date: LocalDate): Int {
        val diff = ChronoUnit.DAYS.between(startDate, date).toInt()
        return if (diff < 0) 1 else diff + 1
    }

    /**
     * Versión con módulo — para colorear días del calendario.
     * No usar para detectar retrasos.
     */
    fun dayInCycleWrapped(startDate: LocalDate, cycleLength: Int, date: LocalDate): Int {
        val c    = cycleLength.coerceAtLeast(15)
        val diff = ChronoUnit.DAYS.between(startDate, date).toInt()
        return ((diff % c) + c) % c + 1
    }

    /** Fase para un día del ciclo dado. Usa siempre dayInCycleWrapped. */
    fun phaseForDay(
        dayInCycle: Int,
        cycleLength: Int,
        periodLength: Int,
        lutealLength: Int = LUTEAL_LENGTH_DEFAULT,
        isPregnant: Boolean = false,
        isOnContraceptive: Boolean = false
    ): CyclePhase {
        if (isPregnant) return CyclePhase.PREGNANCY
        if (isOnContraceptive) {
            return if (dayInCycle <= periodLength) CyclePhase.MENSTRUAL else CyclePhase.FOLLICULAR
        }
        val c = cycleLength.coerceAtLeast(15)
        val ovulationDay = c - lutealLength
        return when {
            dayInCycle <= periodLength                            -> CyclePhase.MENSTRUAL
            // Ventana fértil biológica: 5 días previos a ovulación + día de ovulación + 1 día posterior (OMS/ACOG)
            dayInCycle in (ovulationDay - 5)..(ovulationDay + 1)   -> CyclePhase.OVULATION
            dayInCycle < (ovulationDay - 5)                       -> CyclePhase.FOLLICULAR
            else                                                 -> CyclePhase.LUTEAL
        }
    }

    /**
     * Determina la fase y si es predicción para una fecha específica en relación con el ciclo activo y el día de hoy.
     * Garantiza rigor biológico:
     * - Días pasados o hoy (date <= today) en retraso (> cycleLength) son LUTEAL (retraso), NUNCA MENSTRUAL.
     * - Un día pasado solo es MENSTRUAL si está dentro de los días de sangrado inicial (1..periodLength).
     * - Si hoy está en retraso, las predicciones futuras (date > today) proyectan el periodo a partir del día siguiente.
     */
    fun phaseForDateInActiveCycle(
        startDate: LocalDate,
        cycleLength: Int,
        periodLength: Int,
        date: LocalDate,
        today: LocalDate = com.jeremy.lumi.utils.LumiClock.today(),
        isPregnant: Boolean = false,
        isOnContraceptive: Boolean = false
    ): Pair<CyclePhase, Boolean> {
        if (isPregnant) return Pair(CyclePhase.PREGNANCY, false)

        val diffFromStart = ChronoUnit.DAYS.between(startDate, date).toInt()
        val c = cycleLength.coerceAtLeast(15)
        val pLen = periodLength.coerceIn(1, 10)
        val isFuture = date.isAfter(today)

        if (!isFuture) {
            // Fecha en el pasado o hoy (date <= today): la usuaria ya vivió o está viviendo este día
            return when {
                diffFromStart < 0 -> Pair(CyclePhase.UNKNOWN, false)
                diffFromStart < pLen -> Pair(CyclePhase.MENSTRUAL, false) // Periodo inicial registrado del ciclo activo
                diffFromStart < c -> {
                    val dayInCycle = diffFromStart + 1
                    Pair(phaseForDay(dayInCycle, c, pLen, isPregnant = false, isOnContraceptive = isOnContraceptive), false)
                }
                else -> {
                    // DÍA DE RETRASO (diffFromStart >= cycleLength):
                    // Biológicamente es una fase lútea prolongada mientras no llegue el sangrado. NUNCA menstrual.
                    Pair(CyclePhase.LUTEAL, false)
                }
            }
        } else {
            // Fecha en el futuro (date > today): predicción proyectada
            val todayDiffFromStart = ChronoUnit.DAYS.between(startDate, today).toInt()
            val isCurrentlyLate = todayDiffFromStart >= c

            val phase = if (isCurrentlyLate) {
                // Si el ciclo actual ya tiene retraso hoy, el periodo se proyecta a partir de los próximos días
                val daysAheadFromToday = ChronoUnit.DAYS.between(today, date).toInt()
                if (daysAheadFromToday in 1..pLen) {
                    CyclePhase.MENSTRUAL
                } else {
                    val dayWrapped = (daysAheadFromToday - 1) % c + 1
                    phaseForDay(dayWrapped, c, pLen, isPregnant = false, isOnContraceptive = isOnContraceptive)
                }
            } else {
                // Ciclo normal a tiempo: predicción estándar repetida por módulo
                val dayWrapped = dayInCycleWrapped(startDate, c, date)
                phaseForDay(dayWrapped, c, pLen, isPregnant = false, isOnContraceptive = isOnContraceptive)
            }
            return Pair(phase, true)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PREDICTOR MEJORADO — usa historial de ciclos cerrados
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calcula el promedio ponderado exponencial de duración de ciclos con detección de outliers.
     *
     * Utiliza la Mediana y la Desviación Media Absoluta (MAD) para identificar
     * ciclos anómalos. Si un ciclo es un outlier, su peso se reduce drásticamente.
     * Los ciclos normales reciben más peso si son recientes (decaimiento exponencial).
     *
     * @param closedCycles Lista de ciclos cerrados, ordenados más reciente primero.
     */
    fun weightedAverageCycleLength(closedCycles: List<CycleEntity>): Float {
        val validHistory = CycleHistoryV2.validCycles(closedCycles)
        if (validHistory.isEmpty()) return 28f
        
        // 1. Extraer duraciones válidas
        val durations = validHistory.mapNotNull { cycle ->
            if (cycle.endDate != null) {
                val duration = cycle.closedLengthDays() ?: return@mapNotNull null
                if (duration in 15..60) duration else null
            } else null
        }
        
        if (durations.isEmpty()) return 28f
        if (durations.size < 3) {
            // Sin historial suficiente, un promedio simple es lo más seguro
            return durations.average().toFloat()
        }

        // 2. Calcular Mediana
        val sorted = durations.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2].toDouble()
        }

        // 3. Calcular MAD (Median Absolute Deviation)
        val absoluteDeviations = durations.map { kotlin.math.abs(it - median) }.sorted()
        val mad = if (absoluteDeviations.size % 2 == 0) {
            (absoluteDeviations[absoluteDeviations.size / 2 - 1] + absoluteDeviations[absoluteDeviations.size / 2]) / 2.0
        } else {
            absoluteDeviations[absoluteDeviations.size / 2]
        }
        
        // Evitar MAD = 0 si todos los ciclos son idénticos o casi idénticos
        val effectiveMad = if (mad == 0.0) 1.5 else mad
        
        // 4. Calcular promedio ponderado con penalización de outliers
        var weightedSum = 0.0
        var totalWeight = 0.0
        
        validHistory.forEachIndexed { index, cycle ->
            if (cycle.endDate != null) {
                val duration = cycle.closedLengthDays() ?: return@forEachIndexed
                
                if (duration in 15..60) {
                    val baseWeight = Math.exp(-DECAY_LAMBDA * index)
                    
                    // Si se desvía más de 2.5 * MAD, es un outlier (peso reducido al 5%)
                    val isOutlier = kotlin.math.abs(duration - median) > (2.5 * effectiveMad)
                    val finalWeight = if (isOutlier) baseWeight * 0.05 else baseWeight
                    
                    weightedSum += duration * finalWeight
                    totalWeight += finalWeight
                }
            }
        }
        
        return if (totalWeight == 0.0) median.toFloat() else (weightedSum / totalWeight).toFloat()
    }

    fun weightedStdDevCycleLength(closedCycles: List<CycleEntity>): Float {
        val validHistory = CycleHistoryV2.validCycles(closedCycles)
        if (validHistory.isEmpty()) return 0f
        
        val durations = validHistory.mapNotNull { cycle ->
            if (cycle.endDate != null) {
                val duration = cycle.closedLengthDays() ?: return@mapNotNull null
                if (duration in 15..60) duration else null
            } else null
        }
        
        if (durations.size < 3) return 0f

        val sorted = durations.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2].toDouble()
        }

        val absoluteDeviations = durations.map { kotlin.math.abs(it - median) }.sorted()
        val mad = if (absoluteDeviations.size % 2 == 0) {
            (absoluteDeviations[absoluteDeviations.size / 2 - 1] + absoluteDeviations[absoluteDeviations.size / 2]) / 2.0
        } else {
            absoluteDeviations[absoluteDeviations.size / 2]
        }
        
        val effectiveMad = if (mad == 0.0) 1.5 else mad
        
        var weightedSum = 0.0
        var totalWeight = 0.0
        val valuesAndWeights = mutableListOf<Pair<Int, Double>>()
        
        validHistory.forEachIndexed { index, cycle ->
            if (cycle.endDate != null) {
                val duration = cycle.closedLengthDays() ?: return@forEachIndexed
                
                if (duration in 15..60) {
                    val baseWeight = Math.exp(-DECAY_LAMBDA * index)
                    val isOutlier = kotlin.math.abs(duration - median) > (2.5 * effectiveMad)
                    val finalWeight = if (isOutlier) baseWeight * 0.05 else baseWeight
                    
                    valuesAndWeights.add(Pair(duration, finalWeight))
                    weightedSum += duration * finalWeight
                    totalWeight += finalWeight
                }
            }
        }
        
        if (totalWeight == 0.0 || valuesAndWeights.isEmpty()) return 1.0f
        val meanW = weightedSum / totalWeight
        var varSum = 0.0
        for (vw in valuesAndWeights) {
            varSum += vw.second * (vw.first - meanW) * (vw.first - meanW)
        }
        val varianceW = varSum / totalWeight
        return kotlin.math.sqrt(varianceW).toFloat().coerceAtLeast(1.0f)
    }

    /**
     * Motor Sintotérmico (periodo 3-sobre-6 de la OMS / Criterios de Roetzer).
     *
     * Analiza lecturas de temperatura basal (BBT) para detectar el salto térmico post-ovulatorio:
     *   - Al menos 6 días preovulatorios para definir la línea de base (coverline).
     *   - 3 días consecutivos por encima de la coverline.
     *   - El 3er día supera la coverline en al menos 0.20 °C.
     *
     * @param temperaturesWithDay Lista de pares (día del ciclo, temperatura en °C).
     * @return El día del ciclo de ovulación estimada (último día bajo), o null si no se detecta.
     */
    fun detectBiphasicShift(temperaturesWithDay: List<Pair<Int, Float>>): Int? {
        val valid = temperaturesWithDay.filter { it.second in 35.0f..38.5f }.sortedBy { it.first }
        if (valid.size < 9) return null

        for (i in 6..(valid.size - 3)) {
            val preDays = valid.subList(i - 6, i)
            val postDays = valid.subList(i, i + 3)

            // Criterio de continuidad temporal: el primer día post-shift debe ser contiguo al último pre-shift
            if (postDays.first().first != preDays.last().first + 1) continue

            val coverline = preDays.maxOf { it.second }
            val allHigher = postDays.all { it.second > coverline }
            val thirdHigherEnough = (postDays[2].second - coverline) >= 0.20f

            if (allHigher && thirdHigherEnough) {
                return valid[i - 1].first
            }
        }
        return null
    }

    /**
     * Estima la fase lútea personal a partir del historial y de observaciones sintotérmicas reales.
     * Si no hay suficientes ciclos con cambio térmico confirmado, devuelve el estándar clínico (14 días).
     */
    fun personalLutealLength(
        closedCycles: List<CycleEntity>,
        observedLutealLengths: List<Int> = emptyList()
    ): Int {
        val validObserved = observedLutealLengths.filter { it in 9..18 }
        if (validObserved.size >= 2) {
            return validObserved.average().roundToInt().coerceIn(10, 16)
        }
        return LUTEAL_LENGTH_DEFAULT
    }

    /**
     * Máximo retraso histórico observado.
     *
     * Compara la duración real de cada ciclo contra su cycleLength guardado.
     * Se usa para decidir cuándo mostrar una alerta: solo cuando el retraso
     * actual supera el máximo histórico personal + un margen de 2 días.
     */
    fun maxHistoricalDelay(closedCycles: List<CycleEntity>): Int {
        if (closedCycles.isEmpty()) return 0
        return closedCycles.mapNotNull { cycle ->
            if (cycle.endDate == null) return@mapNotNull null
            val actualLength = cycle.closedLengthDays() ?: return@mapNotNull null
            (actualLength - cycle.cycleLength).coerceAtLeast(0)
        }.maxOrNull() ?: 0
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PREDICT — punto de entrada principal
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Predice el estado actual del ciclo.
     *
     * Si se proporciona [closedCycles] con al menos [MIN_CYCLES_FOR_HISTORY]
     * elementos, activa la predicción mejorada con:
     *   - Promedio ponderado exponencial (más peso a ciclos recientes)
     *   - Fase lútea personal
     *   - Retraso real vs. máximo histórico personal
     *
     * Sin historial suficiente, funciona igual que antes.
     */
    fun predict(
        startDate    : LocalDate,
        cycleLength  : Int             = 28,
        periodLength : Int             = 5,
        today        : LocalDate       = com.jeremy.lumi.utils.LumiClock.today(),
        closedCycles : List<CycleEntity> = emptyList(),
        isPregnant   : Boolean         = false,
        isOnContraceptive: Boolean     = false,
        effectiveCycleLength: Int?     = null,
        predictionConfidence: Float?   = null
    ): CyclePrediction {
        val cycleStartMs = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val validHistory = CycleHistoryV2.validCycles(closedCycles, cycleStartMs)

        if (isPregnant) {
            return CyclePrediction(
                currentDayOfCycle   = dayInCycle(startDate, (effectiveCycleLength ?: cycleLength).coerceAtLeast(15), today),
                currentPhase        = CyclePhase.PREGNANCY,
                nextPeriodDate      = LocalDate.MAX, 
                daysUntilNextPeriod = -1,
                nextOvulationDate   = LocalDate.MAX,
                daysUntilOvulation  = -1,
                cycleLength         = effectiveCycleLength ?: cycleLength,
                periodLength        = periodLength,
                delayState          = DelayState.ON_TIME,
                delayDays           = 0,
                isLate              = false,
                dayOfPhase          = (ChronoUnit.DAYS.between(startDate, today).toInt() + 1).coerceAtLeast(1),
                fertileWindowStartDate = LocalDate.MAX,
                fertileWindowEndDate   = LocalDate.MAX,
                isInFertileWindow      = false,
                isOvulationPeak        = false,
                earliestPeriodDate = LocalDate.MAX,
                latestPeriodDate = LocalDate.MAX,
                predictionConfidenceIndex = 0f,
                predictedCycleLengthStdDev = 0f
            )
        }

        // ── Elegir la mejor estimación de cycleLength ────────────────────────
        val effectiveCycleLen = when {
            isOnContraceptive -> 28 // El ciclo de la píldora suele ser de 28 días
            effectiveCycleLength != null -> effectiveCycleLength.coerceIn(15, 60)
            validHistory.size >= MIN_CYCLES_FOR_HISTORY -> weightedAverageCycleLength(validHistory).roundToInt().coerceIn(15, 60)
            else -> cycleLength.coerceAtLeast(15)
        }

        // ── Fase lútea personal ──────────────────────────────────────────────
        val lutealLen = if (validHistory.size >= MIN_CYCLES_FOR_HISTORY)
            personalLutealLength(validHistory)
        else LUTEAL_LENGTH_DEFAULT

        val c = effectiveCycleLen

        // ── Día real (puede superar c si hay retraso) ────────────────────────
        val realDay = dayInCycle(startDate, c, today)

        // ── Retraso real ─────────────────────────────────────────────────────
        val maxDelay       = maxHistoricalDelay(validHistory)
        val rawDelay       = (realDay - c).coerceAtLeast(0)
        // FIX P3-4: Con anticonceptivos hormonales la hemorragia por privación
        // ocurre de forma regulada y no aplica el concepto de "retraso".
        // Forzar siempre ON_TIME para evitar el banner de "período tardío" incorrecto.
        val delayDays      = if (isOnContraceptive) 0 else rawDelay

        val delayState = when {
            isOnContraceptive                                          -> DelayState.ON_TIME
            delayDays == 0                                            -> DelayState.ON_TIME
            // Para usuarias sin historial, el umbral mínimo es 7 días.
            // Con historial, se adapta al máximo retraso personal + 2 días de margen.
            delayDays <= (maxDelay + 2).coerceAtLeast(7)             -> DelayState.LATE
            delayDays < EXTREME_THRESHOLD                            -> DelayState.VERY_LATE
            else                                                     -> DelayState.EXTREMELY_LATE
        }

        // ── Fase actual ──────────────────────────────────────────────────────
        val currentPhase = if (delayDays > 0) CyclePhase.LUTEAL else phaseForDay(realDay, c, periodLength, lutealLen, false, isOnContraceptive)

        // ── Día de la fase ───────────────────────────────────────────────────
        val ovulationDay = c - lutealLen
        val dayOfPhase = when (currentPhase) {
            CyclePhase.MENSTRUAL -> realDay
            CyclePhase.FOLLICULAR -> realDay - periodLength
            CyclePhase.OVULATION -> realDay - (ovulationDay - 5) + 1 // Ventana OMS/ACOG de 7 días: (ovDay - 5)..(ovDay + 1)
            CyclePhase.LUTEAL -> realDay - (ovulationDay + 1)
            else -> 1
        }.coerceAtLeast(1)

        // ── Próximo inicio de periodo ────────────────────────────────────────
        // No avanzar automáticamente al siguiente ciclo. La periodo esperada sigue siendo la fecha original del ciclo actual.
        val nextPeriod = startDate.plusDays(c.toLong())

        // ── Próxima ovulación (usa lutealLen personal) ───────────────────────
        val nextOvulation = if (isOnContraceptive) {
            LocalDate.MAX
        } else {
            startDate.plusDays((ovulationDay - 1).toLong().coerceAtLeast(0))
        }

        val fertileStart = if (isOnContraceptive) LocalDate.MAX else nextOvulation.minusDays(5)
        val fertileEnd   = if (isOnContraceptive) LocalDate.MAX else nextOvulation.plusDays(1)
        val inFertile    = !isOnContraceptive && !today.isBefore(fertileStart) && !today.isAfter(fertileEnd)
        val isPeak       = !isOnContraceptive && today == nextOvulation

        val stdDev = if (validHistory.size >= MIN_CYCLES_FOR_HISTORY)
            weightedStdDevCycleLength(validHistory) else 4f

        // Historical residual calibration; approximate display range, not a guarantee.
        val intervalDays = if (validHistory.size >= MIN_CYCLES_FOR_HISTORY) {
            kotlin.math.ceil(PredictionCalibrationV2.RATIO * (1f + stdDev))
                .toInt().coerceIn(2, 14)
        } else 7

        val earliest = nextPeriod.minusDays(intervalDays.toLong())
        val latest = nextPeriod.plusDays(intervalDays.toLong())

        // Availability-of-history index ONLY. Do not display as % certainty.
        val historyIndex = when {
            validHistory.size < 3 -> 0.25f
            validHistory.size < 6 -> 0.40f
            else -> 0.50f
        }
        val regularityBonus = if (stdDev < 2f) 0.10f else if (stdDev < 4f) 0.05f else 0f
        val confidenceIndex = (historyIndex + regularityBonus).coerceIn(0f, 0.60f)

        return CyclePrediction(
            currentDayOfCycle   = realDay,
            currentPhase        = currentPhase,
            nextPeriodDate      = nextPeriod,
            daysUntilNextPeriod = ChronoUnit.DAYS.between(today, nextPeriod).toInt(),
            nextOvulationDate   = nextOvulation,
            daysUntilOvulation  = if (isOnContraceptive) -1 else ChronoUnit.DAYS.between(today, nextOvulation).toInt(),
            cycleLength         = c,
            periodLength        = periodLength,
            delayState          = delayState,
            delayDays           = delayDays,
            isLate              = delayDays > 0,
            dayOfPhase          = dayOfPhase,
            fertileWindowStartDate = fertileStart,
            fertileWindowEndDate   = fertileEnd,
            isInFertileWindow      = inFertile,
            isOvulationPeak        = isPeak,
            earliestPeriodDate     = earliest,
            latestPeriodDate       = latest,
            predictionConfidenceIndex = confidenceIndex,
            predictedCycleLengthStdDev = stdDev,
            fertilityIsEstimated = !isOnContraceptive
        )
    }
}
