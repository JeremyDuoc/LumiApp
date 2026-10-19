package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.ai.AIPredictionResult
import com.jeremy.lumi.ai.CycleAIPredictor
import com.jeremy.lumi.ai.PredictionSource
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.domain.model.CyclePhase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class GetEffectiveCyclePredictionUseCaseTest {

    private lateinit var fakeAIPredictor: FakeCycleAIPredictor
    private lateinit var useCase: GetEffectiveCyclePredictionUseCase

    private class FakeCycleAIPredictor : CycleAIPredictor {
        var shouldBeReady: Boolean = false
        var cannedResult: AIPredictionResult? = null

        override fun isModelReady(): Boolean = shouldBeReady

        override fun predict(
            closedCycles: List<CycleEntity>,
            currentCycleLen: Float,
            periodLength: Float,
            age: Float?,
            height: Float?,
            weight: Float?
        ): AIPredictionResult {
            return cannedResult ?: AIPredictionResult(
                predictedCycleLength = currentCycleLen,
                confidenceScore = 0.4f,
                isIrregular = false,
                predictionRangeDays = 2,
                source = PredictionSource.MATHEMATICAL
            )
        }
    }

    private fun mockCycleEntity(start: LocalDate, end: LocalDate?, cycleLen: Int = 28, periodLen: Int = 5): CycleEntity {
        return CycleEntity(
            startDate = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            endDate = end?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            predictedOvulationDate = null,
            cycleLength = cycleLen,
            periodLength = periodLen
        )
    }

    @Before
    fun setUp() {
        fakeAIPredictor = FakeCycleAIPredictor()
        useCase = GetEffectiveCyclePredictionUseCase(fakeAIPredictor)
    }

    @Test
    fun testWhenNoActiveCycleReturnsDefaultResult() {
        val result = useCase(
            activeCycle = null,
            closedCycles = emptyList()
        )

        assertNull(result.prediction)
        assertEquals(28, result.effectiveCycleLength)
        assertEquals(5, result.periodLength)
        assertEquals(PredictionSourceType.NONE, result.source)
    }

    @Test
    fun testWhenOnContraceptiveForces28Days() {
        val startDate = LocalDate.of(2026, 9, 1)
        val today = LocalDate.of(2026, 9, 10)
        val activeCycle = mockCycleEntity(startDate, null, cycleLen = 35)

        val result = useCase(
            activeCycle = activeCycle,
            closedCycles = emptyList(),
            isOnContraceptive = true,
            targetDate = today
        )

        assertNotNull(result.prediction)
        assertEquals(28, result.effectiveCycleLength)
        assertEquals(PredictionSourceType.CONTRACEPTIVE, result.source)
        assertEquals(0, result.prediction?.delayDays)
        assertEquals(DelayState.ON_TIME, result.prediction?.delayState)
    }

    @Test
    fun testWhenUnderThreeClosedCyclesUsesActiveCycleLength() {
        val startDate = LocalDate.of(2026, 9, 1)
        val today = LocalDate.of(2026, 9, 5)
        val activeCycle = mockCycleEntity(startDate, null, cycleLen = 32)
        val oneClosedCycle = listOf(
            mockCycleEntity(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), cycleLen = 31)
        )

        val result = useCase(
            activeCycle = activeCycle,
            closedCycles = oneClosedCycle,
            targetDate = today
        )

        assertNotNull(result.prediction)
        assertEquals(32, result.effectiveCycleLength)
        assertEquals(PredictionSourceType.PROFILE_DEFAULT, result.source)
    }

    @Test
    fun testWhenThreeOrMoreClosedCyclesWithoutAIUsesHistoricalMath() {
        val startDate = LocalDate.of(2026, 9, 1)
        val today = LocalDate.of(2026, 9, 5)
        val activeCycle = mockCycleEntity(startDate, null, cycleLen = 28)

        // 3 cycles each lasting exactly 30 days (start to end inclusive: 30 days)
        val closedCycles = listOf(
            mockCycleEntity(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 30), cycleLen = 30),
            mockCycleEntity(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 31), cycleLen = 30),
            mockCycleEntity(LocalDate.of(2026, 6, 2), LocalDate.of(2026, 7, 1), cycleLen = 30)
        )

        fakeAIPredictor.shouldBeReady = false
        fakeAIPredictor.cannedResult = null

        val result = useCase(
            activeCycle = activeCycle,
            closedCycles = closedCycles,
            targetDate = today
        )

        assertNotNull(result.prediction)
        assertEquals(30, result.effectiveCycleLength)
        assertEquals(PredictionSourceType.HISTORICAL_MATH, result.source)
    }

    @Test
    fun testLegacyBlendedTfliteResultIsRejected() {
        val startDate = LocalDate.of(2026, 9, 1)
        val today = LocalDate.of(2026, 9, 5)
        val activeCycle = mockCycleEntity(startDate, null, cycleLen = 28)

        val closedCycles = listOf(
            mockCycleEntity(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 30), cycleLen = 30),
            mockCycleEntity(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 31), cycleLen = 30),
            mockCycleEntity(LocalDate.of(2026, 6, 2), LocalDate.of(2026, 7, 1), cycleLen = 30)
        )

        fakeAIPredictor.shouldBeReady = true
        fakeAIPredictor.cannedResult = AIPredictionResult(
            predictedCycleLength = 31.4f,
            confidenceScore = 0.85f,
            isIrregular = false,
            predictionRangeDays = 3,
            source = PredictionSource.BLENDED
        )

        val result = useCase(
            activeCycle = activeCycle,
            closedCycles = closedCycles,
            age = 25f,
            height = 165f,
            weight = 60f,
            targetDate = today
        )

        assertNotNull(result.prediction)
        assertEquals(30, result.effectiveCycleLength) // Historical prior cycles, not leaky TFLite
        assertEquals(PredictionSourceType.HISTORICAL_MATH, result.source)
        assertNull(result.aiConfidence)
        assertTrue(result.prediction!!.predictionConfidenceIndex <= 0.60f)
    }

    @Test
    fun testCalendarAndHomeParityOnIdenticalInputs() {
        val startDate = LocalDate.of(2026, 9, 1)
        val today = LocalDate.of(2026, 9, 15)
        val activeCycle = mockCycleEntity(startDate, null, cycleLen = 28)

        val closedCycles = listOf(
            mockCycleEntity(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 30), cycleLen = 30),
            mockCycleEntity(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 31), cycleLen = 30),
            mockCycleEntity(LocalDate.of(2026, 6, 2), LocalDate.of(2026, 7, 1), cycleLen = 30)
        )

        fakeAIPredictor.cannedResult = AIPredictionResult(
            predictedCycleLength = 30.0f,
            confidenceScore = 0.80f,
            isIrregular = false,
            predictionRangeDays = 2,
            source = PredictionSource.BLENDED
        )

        // Simula la llamada desde HomeScreen
        val homeResult = useCase(
            activeCycle = activeCycle,
            closedCycles = closedCycles,
            targetDate = today
        )

        // Simula la llamada desde CalendarScreen
        val calendarResult = useCase(
            activeCycle = activeCycle,
            closedCycles = closedCycles,
            targetDate = today
        )

        assertEquals(homeResult.effectiveCycleLength, calendarResult.effectiveCycleLength)
        assertEquals(homeResult.source, calendarResult.source)
        assertEquals(homeResult.prediction?.currentPhase, calendarResult.prediction?.currentPhase)
        assertEquals(homeResult.prediction?.currentDayOfCycle, calendarResult.prediction?.currentDayOfCycle)
        assertEquals(homeResult.prediction?.nextPeriodDate, calendarResult.prediction?.nextPeriodDate)
        assertEquals(homeResult.prediction?.delayDays, calendarResult.prediction?.delayDays)
    }
    @Test
    fun testNonFiniteResidualOutputFallsBackWithoutCrashing() {
        val active = mockCycleEntity(LocalDate.of(2026, 9, 1), null, cycleLen = 28)
        val prior = listOf(
            mockCycleEntity(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 30)),
            mockCycleEntity(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 31)),
            mockCycleEntity(LocalDate.of(2026, 6, 2), LocalDate.of(2026, 7, 1))
        )
        fakeAIPredictor.shouldBeReady = true
        fakeAIPredictor.cannedResult = AIPredictionResult(
            Float.NaN, 0.99f, false, 0, PredictionSource.RIDGE_V2
        )
        val result = useCase(active, prior, targetDate = LocalDate.of(2026, 9, 4))
        assertEquals(30, result.effectiveCycleLength)
        assertEquals(PredictionSourceType.HISTORICAL_MATH, result.source)
        assertNull(result.aiConfidence)
    }

    @Test
    fun testResidualMustBeExplicitlyEnabledBeforeApplyingCorrection() {
        val active = mockCycleEntity(LocalDate.of(2026, 9, 1), null, cycleLen = 28)
        val prior = listOf(
            mockCycleEntity(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 30)),
            mockCycleEntity(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 31)),
            mockCycleEntity(LocalDate.of(2026, 6, 2), LocalDate.of(2026, 7, 1))
        )
        fakeAIPredictor.shouldBeReady = false
        fakeAIPredictor.cannedResult = AIPredictionResult(
            45f, 1f, false, 0, PredictionSource.RIDGE_V2
        )
        val result = useCase(active, prior, targetDate = LocalDate.of(2026, 9, 4))
        assertEquals(30, result.effectiveCycleLength)
        assertEquals(PredictionSourceType.HISTORICAL_MATH, result.source)
    }
}
