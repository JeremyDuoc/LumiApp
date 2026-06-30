package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.domain.model.CyclePhase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    val dayOfPhase          : Int
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
        val c = cycleLength.coerceAtLeast(15)
        val ovulationDay = c - lutealLength
        return when {
            dayInCycle <= periodLength                            -> CyclePhase.MENSTRUAL
            !isOnContraceptive && dayInCycle in (ovulationDay - 1)..(ovulationDay + 1) -> CyclePhase.OVULATION
            dayInCycle <= ovulationDay                            -> CyclePhase.FOLLICULAR
            else                                                 -> CyclePhase.LUTEAL
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
        if (closedCycles.isEmpty()) return 28f
        
        // 1. Extraer duraciones válidas
        val durations = closedCycles.mapNotNull { cycle ->
            if (cycle.endDate != null) {
                val startLocal = cycle.startDate.toLocalDate()
                val endLocal   = cycle.endDate.toLocalDate()
                val duration   = ChronoUnit.DAYS.between(startLocal, endLocal).toInt()
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
        
        closedCycles.forEachIndexed { index, cycle ->
            if (cycle.endDate != null) {
                val startLocal = cycle.startDate.toLocalDate()
                val endLocal   = cycle.endDate.toLocalDate()
                val duration   = ChronoUnit.DAYS.between(startLocal, endLocal).toInt()
                
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

    /**
     * Estima la fase lútea personal a partir del historial.
     *
     * Lógica Corregida: Sin datos biológicos reales de ovulación (tests LH o temperatura basal),
     * usar la diferencia entre `endDate` y `predictedOvulationDate` crea un bucle de retroalimentación
     * donde la app "aprende" de sus propios errores de predicción. Clínicamente, la fase lútea es
     * constante (14 días promedio) y lo que varía en un retraso es la fase folicular.
     * Hasta que se implemente el registro de tests de ovulación, se debe devolver el valor clínico estándar.
     */
    fun personalLutealLength(closedCycles: List<CycleEntity>): Int {
        // Fallback a valor clínico estándar para evitar corromper las predicciones con falsos positivos matemáticos.
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
            val actualLength = ChronoUnit.DAYS.between(
                cycle.startDate.toLocalDate(),
                cycle.endDate.toLocalDate()
            ).toInt()
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
        today        : LocalDate       = LocalDate.now(),
        closedCycles : List<CycleEntity> = emptyList(),
        isPregnant   : Boolean         = false,
        isOnContraceptive: Boolean     = false
    ): CyclePrediction {

        if (isPregnant) {
            return CyclePrediction(
                currentDayOfCycle   = dayInCycle(startDate, cycleLength.coerceAtLeast(15), today),
                currentPhase        = CyclePhase.PREGNANCY,
                nextPeriodDate      = LocalDate.MAX, 
                daysUntilNextPeriod = -1,
                nextOvulationDate   = LocalDate.MAX,
                daysUntilOvulation  = -1,
                cycleLength         = cycleLength,
                periodLength        = periodLength,
                delayState          = DelayState.ON_TIME,
                delayDays           = 0,
                isLate              = false,
                dayOfPhase          = ChronoUnit.DAYS.between(startDate, today).toInt() + 1
            )
        }

        // ── Elegir la mejor estimación de cycleLength ────────────────────────
        val effectiveCycleLen = if (isOnContraceptive) {
            28 // El ciclo de la píldora suele ser de 28 días
        } else if (closedCycles.size >= MIN_CYCLES_FOR_HISTORY) {
            weightedAverageCycleLength(closedCycles).roundToInt().coerceAtLeast(15)
        } else {
            cycleLength.coerceAtLeast(15)
        }

        // ── Fase lútea personal ──────────────────────────────────────────────
        val lutealLen = if (closedCycles.size >= MIN_CYCLES_FOR_HISTORY)
            personalLutealLength(closedCycles)
        else LUTEAL_LENGTH_DEFAULT

        val c = effectiveCycleLen

        // ── Día real (puede superar c si hay retraso) ────────────────────────
        val realDay = dayInCycle(startDate, c, today)

        // ── Retraso real ─────────────────────────────────────────────────────
        val maxDelay       = maxHistoricalDelay(closedCycles)
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
            CyclePhase.OVULATION -> realDay - (ovulationDay - 2) // Ventana: ovDay-1, ovDay, ovDay+1 -> 1, 2, 3
            CyclePhase.LUTEAL -> realDay - (if (isOnContraceptive) ovulationDay else (ovulationDay + 1))
            else -> 1
        }.coerceAtLeast(1)

        // ── Próximo inicio de periodo ────────────────────────────────────────
        // No avanzar automáticamente al siguiente ciclo. La regla esperada sigue siendo la fecha original del ciclo actual.
        val nextPeriod = startDate.plusDays(c.toLong())

        // ── Próxima ovulación (usa lutealLen personal) ───────────────────────
        val nextOvulation = if (isOnContraceptive) LocalDate.MAX else nextPeriod.minusDays(lutealLen.toLong())

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
            dayOfPhase          = dayOfPhase
        )
    }

    // ── Extension helper ─────────────────────────────────────────────────────
    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}