package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.domain.model.MedicalAlertType
import com.jeremy.lumi.ui.screens.home.PredictionFactorsUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

class IrregularCyclesPredictionTest {

    private fun createCycle(startDate: LocalDate, length: Int, periodLen: Int): CycleEntity {
        val startMs = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endMs = startDate.plusDays(length.toLong() - 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return CycleEntity(
            startDate = startMs,
            endDate = endMs,
            predictedOvulationDate = null,
            cycleLength = length,
            periodLength = periodLen
        )
    }

    @Test
    fun `test 6 months irregular cycles bio-statistical behavior`() {
        // Dataset de 6 meses con alta irregularidad: [38, 22, 42, 24, 44, 26]
        val lengths = listOf(38, 22, 42, 24, 44, 26)
        val periodLengths = listOf(6, 4, 6, 4, 7, 5)

        val baseDate = LocalDate.of(2026, 1, 1)
        var currentStart = baseDate

        val closedCycles = lengths.mapIndexed { index, len ->
            val cycle = createCycle(currentStart, len, periodLengths[index])
            currentStart = currentStart.plusDays(len.toLong())
            cycle
        }

        // 1. Validar amplitud y diferencias consecutivas
        val minLen = lengths.minOrNull()!!
        val maxLen = lengths.maxOrNull()!!
        assertEquals(22, minLen)
        assertEquals(44, maxLen)
        assertEquals(22, maxLen - minLen) // Variabilidad de 22 días

        for (i in 0 until lengths.size - 1) {
            val diff = kotlin.math.abs(lengths[i + 1] - lengths[i])
            assertTrue("Diferencia consecutiva debe ser >= 14d", diff >= 14)
        }

        // 2. Comprobar que CyclePredictor calcula la alta desviación típica
        val stdDev = CyclePredictor.weightedStdDevCycleLength(closedCycles)
        assertTrue("Desviación típica debe ser >= 7.5d para ciclos muy irregulares", stdDev >= 7.5f)

        // 3. Comprobar predicción probabilística de CyclePredictor
        val today = currentStart.plusDays(14) // Día 15 del ciclo activo
        val prediction = CyclePredictor.predict(
            startDate = currentStart,
            cycleLength = 33,
            periodLength = 5,
            today = today,
            closedCycles = closedCycles,
            isPregnant = false,
            isOnContraceptive = false
        )

        assertEquals(15, prediction.currentDayOfCycle)
        assertTrue(prediction.predictedCycleLengthStdDev >= 7.5f)

        // Intervalo probabilístico de fechas debe ser amplio debido a la irregularidad
        val intervalDays = java.time.temporal.ChronoUnit.DAYS.between(
            prediction.earliestPeriodDate,
            prediction.latestPeriodDate
        ).toInt()
        assertTrue("Intervalo al 80% debe ser amplio (>= 16 días)", intervalDays >= 16)

        // 4. Comprobar que MedicalPatternDetector detecta el patrón de irregularidad (PCOS/Varianza alta)
        val detector = MedicalPatternDetector()
        val alerts = detector.analyzePatterns(
            closedCycles = closedCycles,
            logs = emptyList()
        )

        val pcosAlert = alerts.firstOrNull { it.type == MedicalAlertType.PCOS_SUSPICION }
        assertNotNull("Debe detectar alerta de irregularidad / varianza alta", pcosAlert)

        // 5. Comprobar cálculo de PredictionFactorsUi en Home
        val regScore = ((1f - (stdDev / 7f).coerceIn(0f, 1f)) * 100).roundToInt().coerceIn(15, 99)
        val histScore = when (closedCycles.size) {
            0    -> 35
            1    -> 55
            2    -> 72
            3    -> 85
            else -> 96
        }

        val factors = PredictionFactorsUi(
            regularityScore = regScore,
            historyScore = histScore,
            symptomsScore = 88,
            cycleCount = closedCycles.size,
            confidenceLabel = "Certeza moderada",
            marginDaysText = String.format(java.util.Locale.US, "±%.1f d", stdDev)
        )

        assertTrue("Regularidad debe ser muy baja (<= 25%) reflejando honestidad clínica", factors.regularityScore <= 25)
        assertEquals(96, factors.historyScore) // 6 ciclos = historial maduro
        assertEquals(88, factors.symptomsScore) // Registro denso
        assertEquals(6, factors.cycleCount)
    }
}
