package com.jeremy.lumi.data.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MedicalReportClinicalTest {

    @Test
    fun testFigoFrequencyClassification() {
        fun classifyFrequency(cycleLength: Int): FigoFrequency = when {
            cycleLength < 24 -> FigoFrequency.FREQUENT
            cycleLength <= 38 -> FigoFrequency.NORMAL
            else -> FigoFrequency.INFREQUENT
        }

        assertEquals(FigoFrequency.FREQUENT, classifyFrequency(21))
        assertEquals(FigoFrequency.NORMAL, classifyFrequency(24))
        assertEquals(FigoFrequency.NORMAL, classifyFrequency(28))
        assertEquals(FigoFrequency.NORMAL, classifyFrequency(38))
        assertEquals(FigoFrequency.INFREQUENT, classifyFrequency(42))
    }

    @Test
    fun testFigoRegularityClassification() {
        fun classifyRegularity(shortest: Int, longest: Int): FigoRegularity {
            val variation = longest - shortest
            return if (variation <= 9) FigoRegularity.REGULAR else FigoRegularity.IRREGULAR
        }

        assertEquals(FigoRegularity.REGULAR, classifyRegularity(27, 30))
        assertEquals(FigoRegularity.REGULAR, classifyRegularity(26, 35)) // diff = 9 -> Regular
        assertEquals(FigoRegularity.IRREGULAR, classifyRegularity(25, 36)) // diff = 11 -> Irregular
    }

    @Test
    fun testFigoDurationClassification() {
        fun classifyDuration(periodLength: Int): FigoDuration {
            return if (periodLength <= 8) FigoDuration.NORMAL else FigoDuration.PROLONGED
        }

        assertEquals(FigoDuration.NORMAL, classifyDuration(4))
        assertEquals(FigoDuration.NORMAL, classifyDuration(8))
        assertEquals(FigoDuration.PROLONGED, classifyDuration(9))
    }

    @Test
    fun testWho3Over6CoverlineDetection() {
        // periodo OMS 3-sobre-6: 6 temperaturas bajas seguidas de 3 temperaturas más altas que el máximo
        // de las 6 anteriores, y la 3ª al menos 0.20°C por encima de la línea base (coverline).
        val temps = listOf(
            1 to 36.30f,
            2 to 36.25f,
            3 to 36.35f,
            4 to 36.30f,
            5 to 36.40f, // Pre-shift max = 36.40°C (Coverline)
            6 to 36.35f,
            7 to 36.50f, // Post 1 > 36.40
            8 to 36.55f, // Post 2 > 36.40
            9 to 36.65f  // Post 3 = 36.65 - 36.40 = +0.25 >= +0.20 -> Confirmado
        )

        fun computeCoverline(validTemps: List<Pair<Int, Float>>): Float? {
            if (validTemps.size < 9) return null
            for (i in 6..(validTemps.size - 3)) {
                val preDays = validTemps.subList(i - 6, i)
                val postDays = validTemps.subList(i, i + 3)
                val cover = preDays.maxOf { it.second }
                val allHigher = postDays.all { it.second > cover }
                val thirdHigherEnough = (postDays[2].second - cover) >= 0.20f
                if (allHigher && thirdHigherEnough) {
                    return cover
                }
            }
            return null
        }

        val coverline = computeCoverline(temps)
        assertNotNull(coverline)
        assertEquals(36.40f, coverline!!, 0.001f)

        // Caso sin salto térmico suficiente (<0.20°C en el 3º día)
        val flatTemps = listOf(
            1 to 36.30f, 2 to 36.30f, 3 to 36.30f,
            4 to 36.30f, 5 to 36.30f, 6 to 36.30f,
            7 to 36.35f, 8 to 36.35f, 9 to 36.35f
        )
        assertNull(computeCoverline(flatTemps))
    }

    @Test
    fun testMedicalReportDataConstruction() {
        val figo = FigoCycleClassification(
            frequency   = FigoFrequency.NORMAL,
            regularity  = FigoRegularity.REGULAR,
            duration    = FigoDuration.NORMAL,
            summaryText = "Ciclo normal FIGO"
        )
        val redFlag = ClinicalRedFlag(
            title = "Cólicos menstruales intensos (EVA 8/10)",
            description = "Dolor significativo",
            isSevere = true
        )

        val report = MedicalReportData(
            userName                = "Camila",
            userAge                 = 26,
            userHeightCm            = 165f,
            userWeightKg            = 58f,
            isOnContraceptive       = false,
            avgCycleLength          = 28,
            avgPeriodLength         = 5,
            generatedAt             = LocalDate.now(),
            historicalStats         = null,
            recentCycles            = emptyList(),
            cycleStartDates         = emptyList(),
            symptomCorrelations     = emptyList(),
            moodDistribution        = null,
            latestCycleLogs         = emptyList(),
            latestCycleStartDate    = null,
            lastMenstrualPeriodDate = LocalDate.of(2026, 2, 10),
            daysSinceLmp            = 22,
            figoClassification      = figo,
            clinicalRedFlags        = listOf(redFlag),
            bbtCoverline            = 36.45f
        )

        assertEquals("Camila", report.userName)
        assertEquals(22, report.daysSinceLmp)
        assertEquals(FigoFrequency.NORMAL, report.figoClassification?.frequency)
        assertEquals(1, report.clinicalRedFlags.size)
        assertTrue(report.clinicalRedFlags[0].isSevere)
        assertEquals(36.45f, report.bbtCoverline!!, 0.001f)
    }
}
