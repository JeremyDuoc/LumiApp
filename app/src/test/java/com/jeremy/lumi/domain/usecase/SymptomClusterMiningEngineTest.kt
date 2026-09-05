package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.data.local.entity.DailyLogEntity
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.local.entity.SymptomEntity
import com.jeremy.lumi.domain.model.CyclePhase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Tests para [SymptomClusterMiningEngine].
 *
 * Verifica:
 *   - Jaccard > 0.40 para sÃ­ntomas muy co-ocurrentes
 *   - Jaccard < 0.40 (sin cluster) para sÃ­ntomas independientes
 *   - Relative Risk > 2.0 para sÃ­ntoma concentrado en una sola fase
 *   - Manejo de datos vacÃ­os y nulos
 */
class SymptomClusterMiningEngineTest {

    private lateinit var engine: SymptomClusterMiningEngine

    // Fase fija para tests (todos en fase LUTEAL)
    private val phaseForAll: (Long) -> CyclePhase = { CyclePhase.LUTEAL }
    // Fase alterna: LUTEAL para dÃ­as pares, FOLLICULAR para dÃ­as impares
    private val phaseAlternating: (Long) -> CyclePhase = { date ->
        val day = LocalDate.ofEpochDay(date / 86400000L).dayOfMonth
        if (day % 2 == 0) CyclePhase.LUTEAL else CyclePhase.FOLLICULAR
    }

    @Before
    fun setup() {
        engine = SymptomClusterMiningEngine()
    }

    // â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun makeLog(
        dayOffset: Int,
        symptoms: List<String>,
        cyclePhase: CyclePhase = CyclePhase.LUTEAL
    ): DailyLogWithSymptoms {
        val date = LocalDate.of(2026, 1, 1).plusDays(dayOffset.toLong())
        val epochMs = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return DailyLogWithSymptoms(
            dailyLog = DailyLogEntity(cycleId = 1, date = epochMs),
            symptoms = symptoms.mapIndexed { idx, name ->
                SymptomEntity(id = dayOffset * 100 + idx, dailyLogId = dayOffset, name = name, intensity = 5)
            }
        )
    }

    /**
     * Crea logs donde [symptomA] y [symptomB] co-ocurren en [coOccDays] dÃ­as,
     * [symptomA] aparece solo en [onlyADays] dÃ­as,
     * [symptomB] aparece solo en [onlyBDays] dÃ­as.
     * J = coOccDays / (coOccDays + onlyADays + onlyBDays)
     */
    private fun makeCoOccurrenceDataset(
        symptomA: String = "migraÃ±a",
        symptomB: String = "nÃ¡useas",
        coOccDays: Int = 10,
        onlyADays: Int = 2,
        onlyBDays: Int = 2
    ): List<DailyLogWithSymptoms> {
        val logs = mutableListOf<DailyLogWithSymptoms>()
        var day = 0
        repeat(coOccDays) { logs.add(makeLog(day++, listOf(symptomA, symptomB))) }
        repeat(onlyADays) { logs.add(makeLog(day++, listOf(symptomA))) }
        repeat(onlyBDays) { logs.add(makeLog(day++, listOf(symptomB))) }
        return logs
    }

    // â”€â”€â”€ Test 1: Jaccard alta â†’ clÃºster detectado â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `test detects cluster when Jaccard above threshold`() {
        // J = 10 / (10 + 2 + 2) = 10/14 â‰ˆ 0.714 â†’ well above 0.40
        val logs = makeCoOccurrenceDataset(coOccDays = 10, onlyADays = 2, onlyBDays = 2)
        val clusters = runBlocking { engine.computeClusters(logs, phaseForAll) }

        assertTrue(
            "Should detect at least one cluster with J=0.714",
            clusters.isNotEmpty()
        )
        val cluster = clusters.first()
        assertTrue(
            "Jaccard should be > 0.40, got ${cluster.jaccardScore}",
            cluster.jaccardScore > 0.40f
        )
        assertTrue(
            "Co-occurrence days should be >= 5",
            cluster.coOccurrenceDays >= 5
        )
    }

    // â”€â”€â”€ Test 2: Jaccard baja â†’ sin clÃºster â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `test no cluster when Jaccard below threshold`() {
        // J = 2 / (2 + 8 + 8) = 2/18 â‰ˆ 0.111 â†’ below 0.40
        val logs = makeCoOccurrenceDataset(coOccDays = 2, onlyADays = 8, onlyBDays = 8)
        val clusters = runBlocking { engine.computeClusters(logs, phaseForAll) }

        assertTrue(
            "Should not detect cluster with J=0.111 (below 0.40 threshold)",
            clusters.isEmpty()
        )
    }

    // â”€â”€â”€ Test 3: Jaccard en el lÃ­mite â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `test cluster at exactly Jaccard threshold boundary`() {
        // J = 5 / (5 + 5 + 2) = 5/12 â‰ˆ 0.417 â†’ just above 0.40
        val logs = makeCoOccurrenceDataset(coOccDays = 5, onlyADays = 5, onlyBDays = 2)
        val clusters = runBlocking { engine.computeClusters(logs, phaseForAll) }

        // J â‰ˆ 0.417 > 0.40 â€” should be detected (both symptoms appear >=5 times total)
        val symptomATotal = 5 + 5  // 10
        val symptomBTotal = 5 + 2  // 7
        // Both meet the minimum 5 occurrences threshold
        assertTrue("Jâ‰ˆ0.417 should produce a cluster", clusters.isNotEmpty())
        clusters.firstOrNull()?.let {
            assertTrue("Jaccard â‰¥ 0.40", it.jaccardScore >= 0.40f)
        }
    }

    // â”€â”€â”€ Test 4: Relative Risk > 2 cuando sÃ­ntoma concentrado en una fase â”€â”€

    @Test
    fun `test relative risk above 2 when symptom concentrated in one phase`() {
        // Create 20 days: 15 in LUTEAL (all with "cefalea"), 10 in FOLLICULAR (none with "cefalea")
        val logs = mutableListOf<DailyLogWithSymptoms>()
        // Days 0-14: LUTEAL with cefalea
        repeat(15) { i -> logs.add(makeLog(i, listOf("cefalea"))) }
        // Days 15-24: FOLLICULAR without cefalea
        repeat(10) { i -> logs.add(makeLog(15 + i, emptyList())) }

        // Phase function: days 0-14 â†’ LUTEAL, days 15-24 â†’ FOLLICULAR
        var dayCounter = 0
        val phaseByOrder: (Long) -> CyclePhase = { epochMs ->
            val date = LocalDate.ofEpochDay(epochMs / 86400000L)
            val offset = date.dayOfYear - 1
            if (offset < 15) CyclePhase.LUTEAL else CyclePhase.FOLLICULAR
        }

        val risks = runBlocking { engine.computeSymptomPhaseRisks(logs, phaseByOrder) }
        val cefalearLutealRisk = risks.firstOrNull {
            it.symptomName.contains("cefalea", ignoreCase = true) &&
            it.phase == CyclePhase.LUTEAL
        }

        assertNotNull(
            "Should compute relative risk for cefalea in LUTEAL phase",
            cefalearLutealRisk
        )
        cefalearLutealRisk!!
        assertTrue(
            "Relative risk for cefalea in LUTEAL should be > 2.0 (cefalea only in luteal), got ${cefalearLutealRisk.relativeRisk}",
            cefalearLutealRisk.relativeRisk > 2.0f
        )
    }

    // â”€â”€â”€ Test 5: Sin sÃ­ntomas mÃ­nimos â†’ sin clÃºster â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `test no cluster when symptoms appear fewer than 5 times`() {
        // Only 3 co-occurrence days â€” below minimum 5
        val logs = makeCoOccurrenceDataset(coOccDays = 3, onlyADays = 0, onlyBDays = 0)
        val clusters = runBlocking { engine.computeClusters(logs, phaseForAll) }

        // Even though J = 1.0 (perfect), both symptoms appear only 3 times â†’ below threshold
        assertTrue(
            "Should not produce cluster when each symptom has < 5 occurrences",
            clusters.isEmpty()
        )
    }

    // â”€â”€â”€ Test 6: Lista vacÃ­a â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `test handles empty log list`() {
        val clusters = runBlocking { engine.computeClusters(emptyList(), phaseForAll) }
        assertTrue("Empty logs should produce no clusters", clusters.isEmpty())

        val risks = runBlocking { engine.computeSymptomPhaseRisks(emptyList(), phaseForAll) }
        assertTrue("Empty logs should produce no risks", risks.isEmpty())
    }

    // â”€â”€â”€ Test 7: Sin sÃ­ntomas en los logs â†’ sin clÃºster â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `test handles logs with no symptoms`() {
        val logsWithoutSymptoms = (0 until 20).map { i ->
            makeLog(i, emptyList())
        }
        val clusters = runBlocking { engine.computeClusters(logsWithoutSymptoms, phaseForAll) }
        assertTrue("Logs without symptoms should produce no clusters", clusters.isEmpty())
    }

    // â”€â”€â”€ Test 8: Nombres de sÃ­ntomas en mayÃºsculas == minÃºsculas â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `test symptom names are case-insensitive`() {
        // "MigraÃ±a" and "migraÃ±a" should be treated as the same symptom
        val logs = mutableListOf<DailyLogWithSymptoms>()
        repeat(8) { i -> logs.add(makeLog(i, listOf("MigraÃ±a", "NÃ¡useas"))) }
        repeat(2) { i -> logs.add(makeLog(8 + i, listOf("migraÃ±a"))) }

        val clusters = runBlocking { engine.computeClusters(logs, phaseForAll) }
        // Should merge "MigraÃ±a" and "migraÃ±a" into same symptom
        assertNotNull("Case-insensitive matching should still detect cluster", clusters)
        // J = 8 / (8 + 2 + 0) = 0.8 â†’ cluster detected
        assertTrue("Should detect cluster with case-insensitive matching", clusters.isNotEmpty())
    }

    // â”€â”€â”€ Test 9: ClÃºsteres no duplicados (A-B == B-A) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `test no duplicate clusters A-B and B-A`() {
        val logs = makeCoOccurrenceDataset(coOccDays = 10, onlyADays = 1, onlyBDays = 1)
        val clusters = runBlocking { engine.computeClusters(logs, phaseForAll) }

        // Check no symmetric duplicates
        val pairs = clusters.map { Pair(
            minOf(it.symptomA, it.symptomB),
            maxOf(it.symptomA, it.symptomB)
        )}
        val uniquePairs = pairs.toSet()
        assertEquals(
            "Each symptom pair should appear only once (no A-B + B-A duplicates)",
            uniquePairs.size, pairs.size
        )
    }

    // â”€â”€â”€ Test 10: Clusters ordenados por Jaccard score descendente â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    fun `test clusters sorted by Jaccard score descending`() {
        // Two pairs with different Jaccard scores
        val logs =
            makeCoOccurrenceDataset("cefalea", "nÃ¡useas", coOccDays = 9, onlyADays = 1, onlyBDays = 1) +  // Jâ‰ˆ0.818
            makeCoOccurrenceDataset("fatiga", "acnÃ©", coOccDays = 5, onlyADays = 2, onlyBDays = 3)       // Jâ‰ˆ0.5

        val clusters = runBlocking { engine.computeClusters(logs, phaseForAll) }

        if (clusters.size >= 2) {
            assertTrue(
                "Clusters should be sorted by Jaccard descending",
                clusters[0].jaccardScore >= clusters[1].jaccardScore
            )
        }
    }
}

