package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.local.entity.SymptomEntity
import com.jeremy.lumi.domain.model.AlertSeverity
import com.jeremy.lumi.domain.model.MedicalAlertType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Tests para los nuevos detectores en [MedicalPatternDetector]:
 *   - PMDD (DSM-5 rule detector)
 *   - Perimenopause / STRAW+10 Transition Index
 *   - Luteal Phase Defect (LPD)
 *
 * Los detectores existentes (PCOS, Endometriosis) se prueban implícitamente
 * para asegurar que siguen funcionando (no regresión).
 */
class MedicalPatternDetectorExtendedTest {

    private lateinit var detector: MedicalPatternDetector

    @Before
    fun setup() {
        detector = MedicalPatternDetector()
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun toEpochMs(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun makeCycle(start: LocalDate, end: LocalDate): CycleEntity {
        val durationDays = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()
        return CycleEntity(
            startDate = toEpochMs(start),
            endDate = toEpochMs(end),
            predictedOvulationDate = null,
            cycleLength = durationDays,
            periodLength = 5
        )
    }

    /** Crea un log con un estado de ánimo específico */
    private fun makeLogWithMood(cycleId: Int, date: LocalDate, mood: String): DailyLogWithSymptoms {
        return DailyLogWithSymptoms(
            dailyLog = DailyLogEntity(
                cycleId = cycleId,
                date = toEpochMs(date),
                mood = mood
            ),
            symptoms = emptyList()
        )
    }

    /** Crea un log básico sin síntomas ni mood */
    private fun makeBasicLog(cycleId: Int, date: LocalDate): DailyLogWithSymptoms {
        return DailyLogWithSymptoms(
            dailyLog = DailyLogEntity(cycleId = cycleId, date = toEpochMs(date)),
            symptoms = emptyList()
        )
    }

    /** Crea un log con un síntoma específico */
    private fun makeLogWithSymptom(cycleId: Int, date: LocalDate, symptomName: String): DailyLogWithSymptoms {
        return DailyLogWithSymptoms(
            dailyLog = DailyLogEntity(cycleId = cycleId, date = toEpochMs(date)),
            symptoms = listOf(SymptomEntity(id = 0, dailyLogId = 0, name = symptomName, intensity = 7))
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PMDD TESTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Dataset PMDD positivo:
     * 2 ciclos de 28 días. En cada ciclo, los 5 días PRE-menstruales
     * tienen moods negativos (Ansiosa/Irritable), y los 2 primeros días
     * post-inicio tienen mood positivo (Feliz/Calmada).
     */
    @Test
    fun `test PMDD detected with negative premenstrual moods and remission at menstruation`() {
        // Cycle 1: Jan 1 → Jan 29 (28 days)
        val cycle1Start = LocalDate.of(2026, 1, 1)
        val cycle1End = LocalDate.of(2026, 1, 29)
        val cycle1 = makeCycle(cycle1Start, cycle1End)

        // Cycle 2: Jan 29 → Feb 26 (28 days)
        val cycle2Start = LocalDate.of(2026, 1, 29)
        val cycle2End = LocalDate.of(2026, 2, 26)
        val cycle2 = makeCycle(cycle2Start, cycle2End)

        val logsWithSymptoms = mutableListOf<DailyLogWithSymptoms>()

        // Cycle 1: premenstrual 5 days (Jan 24-28) → negative moods
        val c1NegMoods = listOf("Ansiosa", "Irritable", "Irritable", "Triste", "Abrumada")
        c1NegMoods.forEachIndexed { i, mood ->
            logsWithSymptoms.add(makeLogWithMood(cycle1.id, cycle1End.minusDays(5L - i), mood))
        }
        // Cycle 1: post-start (Jan 29, 30) → positive moods (remission)
        logsWithSymptoms.add(makeLogWithMood(cycle2.id, cycle2Start, "Feliz"))
        logsWithSymptoms.add(makeLogWithMood(cycle2.id, cycle2Start.plusDays(1), "Calmada"))

        // Cycle 2: premenstrual 5 days (Feb 21-25) → negative moods
        val c2NegMoods = listOf("Triste", "Ansiosa", "Irritable", "Abrumada", "Irritable")
        c2NegMoods.forEachIndexed { i, mood ->
            logsWithSymptoms.add(makeLogWithMood(cycle2.id, cycle2End.minusDays(5L - i), mood))
        }
        // Cycle 2 post-start (Feb 26, 27) → positive moods (remission)
        val cycle3Start = LocalDate.of(2026, 2, 26)
        logsWithSymptoms.add(makeLogWithMood(3, cycle3Start, "Calmada"))
        logsWithSymptoms.add(makeLogWithMood(3, cycle3Start.plusDays(1), "Feliz"))

        val closedCycles = listOf(cycle1, cycle2)
        val basicLogs = closedCycles.flatMap { cycle ->
            val start = LocalDate.ofEpochDay(cycle.startDate / 86400000L)
            val end = LocalDate.ofEpochDay(cycle.endDate!! / 86400000L)
            (0 until java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt())
                .map { i -> makeBasicLog(cycle.id, start.plusDays(i.toLong())).dailyLog }
        }

        val alerts = detector.analyzePatterns(
            closedCycles = closedCycles,
            logs = basicLogs,
            logsWithSymptoms = logsWithSymptoms
        )

        val pmddAlert = alerts.firstOrNull { it.type == MedicalAlertType.PMDD_SUSPICION }
        assertNotNull(
            "PMDD alert should be detected with consistent premenstrual negative moods + remission. " +
            "Detected alerts: ${alerts.map { it.type }}",
            pmddAlert
        )
    }

    /**
     * PMDD negativo: los moods negativos están distribuidos uniformemente
     * (no solo pre-menstruales). No debe detectar PMDD.
     */
    @Test
    fun `test PMDD not detected when negative moods are uniformly distributed`() {
        val cycle1Start = LocalDate.of(2026, 1, 1)
        val cycle1End = LocalDate.of(2026, 1, 29)
        val cycle1 = makeCycle(cycle1Start, cycle1End)

        val cycle2Start = LocalDate.of(2026, 1, 29)
        val cycle2End = LocalDate.of(2026, 2, 26)
        val cycle2 = makeCycle(cycle2Start, cycle2End)

        val logsWithSymptoms = mutableListOf<DailyLogWithSymptoms>()

        // Scatter negative moods uniformly throughout cycle 1 (NOT only premenstrual)
        val allMoods = listOf(
            0L to "Irritable", 5L to "Ansiosa", 10L to "Feliz",
            15L to "Triste", 20L to "Calmada", 25L to "Ansiosa",
            // end premenstrual days: use positive moods
            23L to "Feliz", 24L to "Calmada", 25L to "Energética",
            26L to "Feliz", 27L to "Calmada"
        )
        allMoods.forEach { (offset, mood) ->
            logsWithSymptoms.add(makeLogWithMood(cycle1.id, cycle1Start.plusDays(offset), mood))
        }

        val basicLogs = listOf(cycle1, cycle2).flatMap { cycle ->
            val start = LocalDate.ofEpochDay(cycle.startDate / 86400000L)
            val end = LocalDate.ofEpochDay(cycle.endDate!! / 86400000L)
            (0 until java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt())
                .map { i -> makeBasicLog(cycle.id, start.plusDays(i.toLong())).dailyLog }
        }

        val alerts = detector.analyzePatterns(
            closedCycles = listOf(cycle1, cycle2),
            logs = basicLogs,
            logsWithSymptoms = logsWithSymptoms
        )

        val pmddAlert = alerts.firstOrNull { it.type == MedicalAlertType.PMDD_SUSPICION }
        assertNull("PMDD should NOT be detected with uniformly distributed moods", pmddAlert)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STRAW+10 PERIMENOPAUSE TESTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Test STRAW+10 positivo: 6 ciclos con variaciones >= 7 días entre ciclos consecutivos,
     * usuaria con 45 años.
     */
    @Test
    fun `test perimenopause STRAW+10 detected with highly variable cycle lengths for user 45 years`() {
        // Cycles with lengths: 28, 35, 22, 38, 26, 42 days
        // Differences: |35-28|=7, |22-35|=13, |38-22|=16, |26-38|=12, |42-26|=16 → all >=7
        val cycleLengths = listOf(28, 35, 22, 38, 26, 42)
        val cycles = mutableListOf<CycleEntity>()
        var currentDate = LocalDate.of(2026, 1, 1)
        cycleLengths.forEach { len ->
            val endDate = currentDate.plusDays(len.toLong())
            cycles.add(makeCycle(currentDate, endDate))
            currentDate = endDate
        }

        val alerts = detector.analyzePatterns(
            closedCycles = cycles,
            logs = emptyList(),
            userAge = 45
        )

        val periAlert = alerts.firstOrNull { it.type == MedicalAlertType.PERIMENOPAUSE_TRANSITION }
        assertNotNull(
            "Perimenopause alert should fire for 45-year-old user with cycle length variations >= 7 days. " +
            "Alerts found: ${alerts.map { it.type }}",
            periAlert
        )
    }

    /**
     * Test STRAW+10 negativo: mismos ciclos irregulares pero usuaria con 35 años.
     * No debe detectar perimenopausia.
     */
    @Test
    fun `test perimenopause STRAW+10 NOT detected for user under 40`() {
        val cycleLengths = listOf(28, 35, 22, 38, 26, 42)
        val cycles = mutableListOf<CycleEntity>()
        var currentDate = LocalDate.of(2026, 1, 1)
        cycleLengths.forEach { len ->
            val endDate = currentDate.plusDays(len.toLong())
            cycles.add(makeCycle(currentDate, endDate))
            currentDate = endDate
        }

        val alerts = detector.analyzePatterns(
            closedCycles = cycles,
            logs = emptyList(),
            userAge = 35
        )

        val periAlert = alerts.firstOrNull { it.type == MedicalAlertType.PERIMENOPAUSE_TRANSITION }
        assertNull(
            "Perimenopause alert should NOT fire for user under 40, even with variable cycles",
            periAlert
        )
    }

    /**
     * Test STRAW+10 negativo: usuaria de 45 años con ciclos MUY regulares (< 7 días de variación).
     */
    @Test
    fun `test perimenopause NOT detected when cycle lengths are stable`() {
        // Stable cycles: 28, 29, 27, 28, 30, 27 → differences all < 7
        val cycleLengths = listOf(28, 29, 27, 28, 30, 27)
        val cycles = mutableListOf<CycleEntity>()
        var currentDate = LocalDate.of(2026, 1, 1)
        cycleLengths.forEach { len ->
            val endDate = currentDate.plusDays(len.toLong())
            cycles.add(makeCycle(currentDate, endDate))
            currentDate = endDate
        }

        val alerts = detector.analyzePatterns(
            closedCycles = cycles,
            logs = emptyList(),
            userAge = 48
        )

        val periAlert = alerts.firstOrNull { it.type == MedicalAlertType.PERIMENOPAUSE_TRANSITION }
        assertNull(
            "Perimenopause should NOT be detected with stable cycle lengths (< 7 days variation)",
            periAlert
        )
    }

    /**
     * Test STRAW+10 con síntomas potenciadores (sofocos) → severidad CRITICAL.
     */
    @Test
    fun `test perimenopause CRITICAL severity when hot flashes present`() {
        val cycleLengths = listOf(28, 38, 22, 35, 25, 40)
        val cycles = mutableListOf<CycleEntity>()
        var currentDate = LocalDate.of(2026, 1, 1)
        cycleLengths.forEach { len ->
            val endDate = currentDate.plusDays(len.toLong())
            cycles.add(makeCycle(currentDate, endDate))
            currentDate = endDate
        }

        // Add hot flash symptoms in recent logs
        val recentDate = currentDate.minusDays(10)
        val logsWithHotFlash = listOf(
            makeLogWithSymptom(cycles.last().id, recentDate, "sofocos"),
            makeLogWithSymptom(cycles.last().id, recentDate.plusDays(1), "Sofocos / Bochornos")
        )

        val basicLogs = logsWithHotFlash.map { it.dailyLog }

        val alerts = detector.analyzePatterns(
            closedCycles = cycles,
            logs = basicLogs,
            logsWithSymptoms = logsWithHotFlash,
            userAge = 47
        )

        val periAlert = alerts.firstOrNull { it.type == MedicalAlertType.PERIMENOPAUSE_TRANSITION }
        assertNotNull("Perimenopause alert should fire", periAlert)
        assertEquals(
            "Perimenopause with hot flashes should be CRITICAL severity",
            AlertSeverity.CRITICAL,
            periAlert?.severity
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LPD (LUTEAL PHASE DEFECT) TESTS
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `test LPD detected when average luteal phase is below 10 days`() {
        val cycles = listOf(
            makeCycle(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29)),
            makeCycle(LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 26))
        )
        val basicLogs = emptyList<DailyLogEntity>()

        // Observed luteal lengths from BBT detection: 8 and 7 days (both < 10)
        val observedLutealLengths = listOf(8, 7)

        val alerts = detector.analyzePatterns(
            closedCycles = cycles,
            logs = basicLogs,
            observedLutealLengths = observedLutealLengths
        )

        val lpdAlert = alerts.firstOrNull { it.type == MedicalAlertType.LUTEAL_PHASE_DEFECT }
        assertNotNull(
            "LPD alert should be detected with average luteal length = 7.5 days (< 10). " +
            "Alerts: ${alerts.map { it.type }}",
            lpdAlert
        )
        assertEquals("LPD severity should be WARNING", AlertSeverity.WARNING, lpdAlert?.severity)
    }

    @Test
    fun `test LPD NOT detected when luteal phase is above 10 days`() {
        val cycles = listOf(
            makeCycle(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29)),
            makeCycle(LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 26))
        )

        // Normal luteal lengths: 12 and 13 days
        val observedLutealLengths = listOf(12, 13)

        val alerts = detector.analyzePatterns(
            closedCycles = cycles,
            logs = emptyList(),
            observedLutealLengths = observedLutealLengths
        )

        val lpdAlert = alerts.firstOrNull { it.type == MedicalAlertType.LUTEAL_PHASE_DEFECT }
        assertNull("LPD should NOT be detected with normal luteal lengths (12-13 days)", lpdAlert)
    }

    @Test
    fun `test LPD NOT detected with insufficient BBT observations`() {
        val cycles = listOf(
            makeCycle(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29))
        )

        // Only 1 observation — insufficient
        val observedLutealLengths = listOf(7)

        val alerts = detector.analyzePatterns(
            closedCycles = cycles,
            logs = emptyList(),
            observedLutealLengths = observedLutealLengths
        )

        val lpdAlert = alerts.firstOrNull { it.type == MedicalAlertType.LUTEAL_PHASE_DEFECT }
        assertNull("LPD should NOT be detected with only 1 BBT observation", lpdAlert)
    }

    @Test
    fun `test LPD at boundary 10 days average is not triggered`() {
        val cycles = listOf(
            makeCycle(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29)),
            makeCycle(LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 26))
        )

        // Exactly 10 days average (10 + 10 = 20 / 2 = 10.0)
        val observedLutealLengths = listOf(10, 10)

        val alerts = detector.analyzePatterns(
            closedCycles = cycles,
            logs = emptyList(),
            observedLutealLengths = observedLutealLengths
        )

        val lpdAlert = alerts.firstOrNull { it.type == MedicalAlertType.LUTEAL_PHASE_DEFECT }
        assertNull("LPD should NOT trigger at exactly 10.0 days (< 10 is the threshold)", lpdAlert)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NON-REGRESSION: existing detectors still work
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `test PCOS detection still works after expansion`() {
        // High variance cycles: 22, 40, 20, 45 days
        val cycleLengths = listOf(22, 40, 20, 45, 25)
        val cycles = mutableListOf<CycleEntity>()
        var currentDate = LocalDate.of(2026, 1, 1)
        cycleLengths.forEach { len ->
            val endDate = currentDate.plusDays(len.toLong())
            cycles.add(makeCycle(currentDate, endDate))
            currentDate = endDate
        }

        val basicLogs = emptyList<DailyLogEntity>()
        val alerts = detector.analyzePatterns(closedCycles = cycles, logs = basicLogs)

        val pcosAlert = alerts.firstOrNull { it.type == MedicalAlertType.PCOS_SUSPICION }
        assertNotNull(
            "PCOS detection should still work after adding new detectors. Alerts: ${alerts.map { it.type }}",
            pcosAlert
        )
    }

    @Test
    fun `test endometriosis detection still works after expansion`() {
        // 2 cycles with severe pain (>= 7) for >= 2 days each
        val cycles = listOf(
            makeCycle(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29)),
            makeCycle(LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 26))
        )

        val logsWithPain = listOf(
            DailyLogEntity(cycleId = cycles[0].id, date = toEpochMs(LocalDate.of(2026, 1, 2)), painLevel = 8),
            DailyLogEntity(cycleId = cycles[0].id, date = toEpochMs(LocalDate.of(2026, 1, 3)), painLevel = 9),
            DailyLogEntity(cycleId = cycles[1].id, date = toEpochMs(LocalDate.of(2026, 1, 30)), painLevel = 8),
            DailyLogEntity(cycleId = cycles[1].id, date = toEpochMs(LocalDate.of(2026, 1, 31)), painLevel = 7)
        )

        val alerts = detector.analyzePatterns(closedCycles = cycles, logs = logsWithPain)

        val endoAlert = alerts.firstOrNull { it.type == MedicalAlertType.ENDOMETRIOSIS_SUSPICION }
        assertNotNull(
            "Endometriosis detection should still work after expansion. Alerts: ${alerts.map { it.type }}",
            endoAlert
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EDGE CASES
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `test empty cycles returns empty alerts`() {
        val alerts = detector.analyzePatterns(
            closedCycles = emptyList(),
            logs = emptyList()
        )
        assertTrue("Empty input should produce no alerts", alerts.isEmpty())
    }

    @Test
    fun `test null userAge skips STRAW+10 detection`() {
        val cycleLengths = listOf(28, 38, 22, 35, 25, 40)
        val cycles = mutableListOf<CycleEntity>()
        var currentDate = LocalDate.of(2026, 1, 1)
        cycleLengths.forEach { len ->
            val endDate = currentDate.plusDays(len.toLong())
            cycles.add(makeCycle(currentDate, endDate))
            currentDate = endDate
        }

        // userAge = null → STRAW+10 should be skipped entirely
        val alerts = detector.analyzePatterns(
            closedCycles = cycles,
            logs = emptyList(),
            userAge = null
        )

        val periAlert = alerts.firstOrNull { it.type == MedicalAlertType.PERIMENOPAUSE_TRANSITION }
        assertNull("STRAW+10 should be silently skipped when userAge is null", periAlert)
    }
}
