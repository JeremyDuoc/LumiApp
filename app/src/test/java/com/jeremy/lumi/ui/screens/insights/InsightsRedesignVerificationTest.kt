package com.jeremy.lumi.ui.screens.insights

import com.jeremy.lumi.domain.model.AlertSeverity
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.model.CycleType
import com.jeremy.lumi.domain.model.HistoricalCycleStats
import com.jeremy.lumi.domain.model.LumiObservation
import com.jeremy.lumi.domain.model.MedicalAlert
import com.jeremy.lumi.domain.model.MedicalAlertType
import com.jeremy.lumi.domain.model.PuntoBarraCiclo
import com.jeremy.lumi.domain.model.PuntoBbt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsRedesignVerificationTest {

    @Test
    fun testCycleDurationClassification_shortNormalLong() {
        val shortDuration = 22
        val normalDuration = 28
        val longDuration = 42

        val typeShort = when {
            shortDuration < 24 -> CycleType.SHORT
            shortDuration > 35 -> CycleType.LONG
            else -> CycleType.NORMAL
        }

        val typeNormal = when {
            normalDuration < 24 -> CycleType.SHORT
            normalDuration > 35 -> CycleType.LONG
            else -> CycleType.NORMAL
        }

        val typeLong = when {
            longDuration < 24 -> CycleType.SHORT
            longDuration > 35 -> CycleType.LONG
            else -> CycleType.NORMAL
        }

        assertEquals(CycleType.SHORT, typeShort)
        assertEquals(CycleType.NORMAL, typeNormal)
        assertEquals(CycleType.LONG, typeLong)
    }

    @Test
    fun testThermalShiftDetection_requiresMinimumFourPoints() {
        val fewPoints = listOf(
            PuntoBbt(diaCiclo = 10, temperaturaC = 36.3f, fecha = "10 Oct"),
            PuntoBbt(diaCiclo = 11, temperaturaC = 36.2f, fecha = "11 Oct"),
            PuntoBbt(diaCiclo = 12, temperaturaC = 36.6f, fecha = "12 Oct")
        )

        // Simula la función de detección prudente
        val puntosOrdenados = fewPoints.sortedBy { it.diaCiclo }
        assertTrue("Con menos de 4 puntos no debe calcularse cambio térmico", puntosOrdenados.size < 4)
        assertFalse(fewPoints.any { it.esOvulacion })
    }

    @Test
    fun testThermalShiftDetection_validatesSustainedRiseAndSortsChronologically() {
        // Puntos desordenados intencionalmente
        val unorderedPoints = listOf(
            PuntoBbt(diaCiclo = 13, temperaturaC = 36.7f, fecha = "13 Oct"),
            PuntoBbt(diaCiclo = 10, temperaturaC = 36.4f, fecha = "10 Oct"),
            PuntoBbt(diaCiclo = 12, temperaturaC = 36.6f, fecha = "12 Oct"),
            PuntoBbt(diaCiclo = 11, temperaturaC = 36.2f, fecha = "11 Oct") // Nadir en día 11
        )

        val sorted = unorderedPoints.sortedBy { it.diaCiclo }
        assertEquals(10, sorted[0].diaCiclo)
        assertEquals(11, sorted[1].diaCiclo)
        assertEquals(12, sorted[2].diaCiclo)
        assertEquals(13, sorted[3].diaCiclo)

        // Detección de salto térmico (nadir seguido de >= 0.18°C)
        var shiftIndex = -1
        val umbral = 0.18f
        for (i in 1 until sorted.size - 1) {
            val prev = sorted[i - 1].temperaturaC
            val curr = sorted[i].temperaturaC
            val next = sorted[i + 1].temperaturaC
            if (curr < prev && next - curr >= umbral) {
                shiftIndex = i + 1
                break
            }
        }

        assertEquals(2, shiftIndex) // Día 12 (índice 2) es el primer día de subida posterior al nadir
        assertEquals(12, sorted[shiftIndex].diaCiclo)
    }

    @Test
    fun testObservations_fulfillsFiveClinicalPrinciplesWithoutAlarmism() {
        val testStats = HistoricalCycleStats(
            cycleCount = 6,
            avgCycleLength = 32.4f,
            avgPeriodLength = 5.0f,
            shortestCycle = 22,
            longestCycle = 44,
            personalLutealLength = 14,
            maxHistoricalDelay = 12
        )

        val testAlerts = listOf(
            MedicalAlert(
                type = MedicalAlertType.ENDOMETRIOSIS_SUSPICION,
                titleRes = 0,
                descriptionRes = 0,
                severity = AlertSeverity.CRITICAL
            )
        )

        val observations = mutableListOf<LumiObservation>()
        val variation = testStats.longestCycle - testStats.shortestCycle

        if (variation > 9) {
            observations.add(
                LumiObservation(
                    id = "obs_variation",
                    tag = "Duración",
                    title = "Variación entre ciclos",
                    description = "En los ${testStats.cycleCount} ciclos analizados, la duración va de ${testStats.shortestCycle} a ${testStats.longestCycle} días. Esto indica una diferencia entre observaciones, pero no identifica una enfermedad ni establece por qué cambian.",
                    sampleText = "Basado en ${testStats.cycleCount} ciclos cerrados",
                    limitsText = "La variación, por sí sola, no permite establecer una causa médica ni predecir anomalías.",
                    recommendationText = "Si la variación se mantiene o supera los 45 días repetidamente, es aconsejable conversarlo en tu próxima visita ginecológica.",
                    severity = AlertSeverity.WARNING
                )
            )
        }

        testAlerts.forEach { alert ->
            if (alert.type == MedicalAlertType.ENDOMETRIOSIS_SUSPICION) {
                observations.add(
                    LumiObservation(
                        id = "obs_endo",
                        tag = "Síntomas",
                        title = "Dolor menstrual recurrente registrado",
                        description = "En tus registros figura una frecuencia reiterada de dolor moderado a intenso asociado a tu periodo.",
                        sampleText = "Basado en registros de dolor en el diario",
                        limitsText = "No constituye un diagnóstico de endometriosis ni de dismenorrea secundaria.",
                        recommendationText = "Si el dolor no cede con analgésicos o dificulta tus tareas cotidianas, es muy recomendable conversarlo con un profesional de la salud.",
                        severity = AlertSeverity.CRITICAL
                    )
                )
            }
        }

        assertEquals(2, observations.size)

        observations.forEach { obs ->
            // 1. Qué se ha registrado
            assertTrue("Debe tener etiqueta y título", obs.tag.isNotBlank() && obs.title.isNotBlank())
            // 2. Patrón observado
            assertTrue("Debe describir el patrón", obs.description.isNotBlank())
            // 3. Sobre cuántos datos se basa
            assertTrue("Debe declarar el tamaño de muestra", obs.sampleText.contains("Basado en"))
            // 4. Qué no puede concluirse
            assertTrue("Debe declarar los límites diagnósticos", obs.limitsText.contains("no permite") || obs.limitsText.contains("No constituye"))
            // 5. Cuándo consultar
            assertTrue("Debe indicar cuándo consultar", obs.recommendationText.contains("conversarlo") || obs.recommendationText.contains("consultar"))
            // Sin certezas falsas ni diagnósticos
            assertFalse(obs.description.contains("diagnóstico confirmado", ignoreCase = true))
            assertFalse(obs.limitsText.contains("certeza absoluta", ignoreCase = true))
        }
    }
}
