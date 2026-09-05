package com.jeremy.lumi.domain.usecase

import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.domain.model.BioVariable
import com.jeremy.lumi.domain.model.CyclePhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class LumiSmartChatEngine @Inject constructor(
    private val correlationMatrixEngine: AdvancedBioCorrelationMatrixEngine,
    private val clusterMiningEngine: SymptomClusterMiningEngine,
    private val medicalPatternDetector: MedicalPatternDetector,
    private val stringResolver: ChatStringResolver
) {

    /**
     * Responde a una pregunta predefinida estructurada seleccionada por el usuario.
     */
    suspend fun generateAnswerForQuestion(
        question: ChatQuestion,
        cycleContext: CycleContext?,
        recentLogs: List<DailyLogWithSymptoms> = emptyList()
    ): String = withContext(Dispatchers.Default) {
        when (question) {
            ChatQuestion.WHEN_NEXT_PERIOD        -> buildNextPeriodResponse(cycleContext)
            ChatQuestion.WHAT_PHASE_AM_I         -> buildCurrentPhaseResponse(cycleContext)
            ChatQuestion.WHY_LATE_PERIOD         -> buildLatePeriodResponse(cycleContext)
            ChatQuestion.SPOTTING_EXPLANATION    -> buildDischargeResponse(cycleContext)
            ChatQuestion.WHY_CRAMPS              -> buildCrampsResponse(cycleContext)
            ChatQuestion.HEADACHE_RELIEF         -> buildHeadacheResponse(cycleContext)
            ChatQuestion.DIGESTIVE_RELIEF        -> buildDigestiveResponse(cycleContext)
            ChatQuestion.WHY_FEEL_TIRED          -> buildSleepResponse(cycleContext)
            ChatQuestion.WHY_MOOD_CHANGES        -> buildMoodResponse(cycleContext)
            ChatQuestion.WHEN_FERTILE_WINDOW     -> buildFertilityResponse(cycleContext)
            ChatQuestion.UNPROTECTED_SEX         -> buildUnprotectedSexResponse(cycleContext)
            ChatQuestion.CONTRACEPTION_INFO      -> buildContraceptionResponse(cycleContext)
            ChatQuestion.BIO_PATTERNS            -> buildPersonalInsightsSummary(cycleContext, recentLogs)
            ChatQuestion.SLEEP_AND_CYCLE         -> buildSleepResponse(cycleContext)
            ChatQuestion.STRESS_AND_CYCLE        -> buildStressResponse(cycleContext)
            ChatQuestion.NUTRITION_RECOMMENDATION -> buildNutritionResponse(cycleContext)
            ChatQuestion.EXERCISE_RECOMMENDATION -> buildExerciseResponse(cycleContext)
            ChatQuestion.HOW_IMPROVE_ENERGY      -> buildEnergyResponse(cycleContext)
        }
    }

    /**
     * Responde a consultas en texto (para pruebas o fallback contextual).
     */
    suspend fun generateResponse(
        query: String,
        cycleContext: CycleContext?,
        recentLogs: List<DailyLogWithSymptoms> = emptyList()
    ): String = withContext(Dispatchers.Default) {
        val cleanQuery = normalizeText(query)

        // Relaciones sin protección / Emergencia
        if (containsAny(cleanQuery, listOf("sin proteccion", "se rompio", "postday", "pastilla del dia despues", "anticonceptivo de emergencia", "riesgo de embarazo", "coito"))) {
            return@withContext buildUnprotectedSexResponse(cycleContext)
        }

        // Dolor y cólicos
        if (containsAny(cleanQuery, listOf("colico", "colicos", "vientre", "ovarios", "dolor pelvico", "calambre", "dismenorrea"))) {
            return@withContext buildCrampsResponse(cycleContext)
        }

        // Dolor de cabeza / migraña
        if (containsAny(cleanQuery, listOf("cabeza", "migrana", "jaqueca", "cefalea"))) {
            return@withContext buildHeadacheResponse(cycleContext)
        }

        // Retraso menstrual
        if (containsAny(cleanQuery, listOf("retraso", "atraso", "no me baja", "no me viene", "periodo tarde", "tarde mi periodo"))) {
            return@withContext buildLatePeriodResponse(cycleContext)
        }

        // Fertilidad / ovulación
        if (containsAny(cleanQuery, listOf("fertil", "ovulacion", "ovulando", "dias fertiles", "embarazada", "quedar embarazada"))) {
            return@withContext buildFertilityResponse(cycleContext)
        }

        // Estado de ánimo / SPM
        if (containsAny(cleanQuery, listOf("animo", "humor", "irritable", "enojada", "llorar", "ansiedad", "triste", "spm", "pms"))) {
            return@withContext buildMoodResponse(cycleContext)
        }

        // Sueño / cansancio
        if (containsAny(cleanQuery, listOf("sueno", "insomnio", "dormir", "cansada", "agotada", "energia"))) {
            return@withContext buildSleepResponse(cycleContext)
        }

        // Digestión / náuseas
        if (containsAny(cleanQuery, listOf("nausea", "nauseas", "asco", "vomito", "hinchazon", "inflamada", "gases"))) {
            return@withContext buildDigestiveResponse(cycleContext)
        }

        // Patrones y descubrimientos
        if (containsAny(cleanQuery, listOf("patrones", "descubrimientos", "correlaciones", "datos", "estadisticas"))) {
            return@withContext buildPersonalInsightsSummary(cycleContext, recentLogs)
        }

        // Nutrición
        if (containsAny(cleanQuery, listOf("comer", "comida", "dieta", "alimentacion", "nutricion", "alimentos"))) {
            return@withContext buildNutritionResponse(cycleContext)
        }

        // Ejercicio
        if (containsAny(cleanQuery, listOf("ejercicio", "entrenar", "gimnasio", "gym", "pesas", "cardio", "deporte"))) {
            return@withContext buildExerciseResponse(cycleContext)
        }

        // Flujo vaginal / manchado
        if (containsAny(cleanQuery, listOf("flujo", "manchado", "spotting", "moco"))) {
            return@withContext buildDischargeResponse(cycleContext)
        }

        // Saludos
        if (containsAny(cleanQuery, listOf("hola", "buenos dias", "buenas tardes", "buenas noches", "que tal", "lumi"))) {
            return@withContext buildGreetingResponse(cycleContext)
        }

        // Fallback contextualizado
        return@withContext buildContextualFallback(query, cycleContext)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GENERADORES DE RESPUESTA ESTRUCTURADOS (100% STRINGS XML, CERO HARDCODED)
    // ─────────────────────────────────────────────────────────────────────────

    fun buildCrampsResponse(ctx: CycleContext?): String {
        val currentPain = if (ctx != null && ctx.todayPainLevel > 0) {
            stringResolver.getString(R.string.chat_smart_cramps_today_pain, ctx.todayPainLevel)
        } else ""

        return stringResolver.getString(R.string.chat_smart_cramps_body, currentPain)
    }

    fun buildHeadacheResponse(ctx: CycleContext?): String {
        val phaseNote = if (ctx != null) {
            when (ctx.phase) {
                CyclePhase.LUTEAL -> stringResolver.getString(R.string.chat_smart_headache_luteal, ctx.daysUntilPeriod)
                CyclePhase.MENSTRUAL -> stringResolver.getString(R.string.chat_smart_headache_menstrual)
                else -> stringResolver.getString(R.string.chat_smart_headache_general, formatPhaseName(ctx.phase))
            }
        } else ""

        return stringResolver.getString(R.string.chat_smart_headache_body, phaseNote)
    }

    fun buildLatePeriodResponse(ctx: CycleContext?): String {
        if (ctx == null) {
            return stringResolver.getString(R.string.chat_smart_late_no_ctx)
        }

        return if (!ctx.isLate) {
            stringResolver.getString(
                R.string.chat_smart_late_on_time,
                ctx.dayOfCycle,
                ctx.cycleLength,
                ctx.daysUntilPeriod
            )
        } else {
            stringResolver.getString(
                R.string.chat_smart_late_delayed,
                ctx.delayDays,
                ctx.dayOfCycle,
                ctx.cycleLength
            )
        }
    }

    fun buildUnprotectedSexResponse(ctx: CycleContext?): String {
        return stringResolver.getString(R.string.chat_smart_unprotected_sex)
    }

    fun buildFertilityResponse(ctx: CycleContext?): String {
        if (ctx == null) {
            return stringResolver.getString(R.string.chat_smart_fertility_no_ctx)
        }

        if (ctx.isOnContraceptive) {
            return stringResolver.getString(R.string.chat_smart_fertility_contraceptive)
        }

        val ovulationDay = (ctx.cycleLength - 14).coerceAtLeast(10)
        val fertileStart = ovulationDay - 5
        val fertileEnd = ovulationDay + 1

        val currentStatus = when {
            ctx.dayOfCycle in fertileStart..fertileEnd -> {
                if (ctx.dayOfCycle == ovulationDay) {
                    stringResolver.getString(R.string.chat_smart_fertility_ovulation_today)
                } else {
                    stringResolver.getString(R.string.chat_smart_fertility_in_window)
                }
            }
            ctx.dayOfCycle < fertileStart -> {
                stringResolver.getString(R.string.chat_smart_fertility_before, fertileStart)
            }
            else -> {
                stringResolver.getString(R.string.chat_smart_fertility_after)
            }
        }

        val ovulationTiming = when {
            ctx.daysUntilOvulation > 0 -> stringResolver.getString(R.string.chat_smart_fertility_timing_future, ctx.daysUntilOvulation)
            ctx.daysUntilOvulation == 0 -> stringResolver.getString(R.string.chat_smart_fertility_timing_today)
            else -> stringResolver.getString(R.string.chat_smart_fertility_timing_past, -ctx.daysUntilOvulation)
        }

        return stringResolver.getString(
            R.string.chat_smart_fertility_body,
            ctx.dayOfCycle,
            ctx.cycleLength,
            currentStatus,
            ovulationDay,
            ovulationTiming,
            fertileStart,
            fertileEnd
        )
    }

    fun buildMoodResponse(ctx: CycleContext?): String {
        val phaseNote = if (ctx != null) {
            if (ctx.phase == CyclePhase.LUTEAL) {
                stringResolver.getString(R.string.chat_smart_mood_luteal, ctx.dayOfCycle)
            } else {
                stringResolver.getString(R.string.chat_smart_mood_general, formatPhaseName(ctx.phase))
            }
        } else ""

        return stringResolver.getString(R.string.chat_smart_mood_body, phaseNote)
    }

    fun buildSleepResponse(ctx: CycleContext?): String {
        val sleepData = if (ctx?.todaySleepHours != null) {
            stringResolver.getString(R.string.chat_smart_sleep_today, ctx.todaySleepHours.toString())
        } else ""

        return stringResolver.getString(R.string.chat_smart_sleep_body, sleepData)
    }

    fun buildDigestiveResponse(ctx: CycleContext?): String {
        return stringResolver.getString(R.string.chat_smart_digestive_body)
    }

    suspend fun buildPersonalInsightsSummary(
        ctx: CycleContext?,
        logs: List<DailyLogWithSymptoms>
    ): String {
        if (logs.isEmpty()) {
            return stringResolver.getString(R.string.chat_smart_patterns_empty)
        }

        val insights = correlationMatrixEngine.computeInsights(logs)
        val clusters = clusterMiningEngine.computeClusters(logs) { _ -> ctx?.phase }

        val insightsText = if (insights.isNotEmpty()) {
            insights.take(2).joinToString("\n") { ins ->
                val dir = if (ins.correlationScore > 0) {
                    stringResolver.getString(R.string.chat_smart_patterns_dir_direct)
                } else {
                    stringResolver.getString(R.string.chat_smart_patterns_dir_inverse)
                }
                val lag = if (ins.isLagged) stringResolver.getString(R.string.chat_smart_patterns_lag) else ""
                stringResolver.getString(
                    R.string.chat_smart_patterns_insight_item,
                    formatVarName(ins.variableA),
                    dir,
                    formatVarName(ins.variableB),
                    (abs(ins.correlationScore) * 100).toInt(),
                    lag
                )
            }
        } else {
            stringResolver.getString(R.string.chat_smart_patterns_insight_stable)
        }

        val clusterText = if (clusters.isNotEmpty()) {
            val topCluster = clusters.first()
            stringResolver.getString(
                R.string.chat_smart_patterns_cluster_item,
                topCluster.symptomA.replaceFirstChar { it.uppercase() },
                topCluster.symptomB.replaceFirstChar { it.uppercase() },
                (topCluster.jaccardScore * 100).toInt()
            )
        } else ""

        return stringResolver.getString(R.string.chat_smart_patterns_body, insightsText, clusterText)
    }

    fun buildNutritionResponse(ctx: CycleContext?): String {
        val phase = ctx?.phase ?: CyclePhase.FOLLICULAR
        return when (phase) {
            CyclePhase.MENSTRUAL  -> stringResolver.getString(R.string.chat_smart_nutrition_menstrual)
            CyclePhase.FOLLICULAR -> stringResolver.getString(R.string.chat_smart_nutrition_follicular)
            CyclePhase.OVULATION  -> stringResolver.getString(R.string.chat_smart_nutrition_ovulation)
            CyclePhase.LUTEAL     -> stringResolver.getString(R.string.chat_smart_nutrition_luteal)
            else                  -> stringResolver.getString(R.string.chat_smart_nutrition_general)
        }
    }

    fun buildExerciseResponse(ctx: CycleContext?): String {
        val phase = ctx?.phase ?: CyclePhase.FOLLICULAR
        return when (phase) {
            CyclePhase.MENSTRUAL  -> stringResolver.getString(R.string.chat_smart_exercise_menstrual)
            CyclePhase.FOLLICULAR -> stringResolver.getString(R.string.chat_smart_exercise_follicular)
            CyclePhase.OVULATION  -> stringResolver.getString(R.string.chat_smart_exercise_ovulation)
            CyclePhase.LUTEAL     -> stringResolver.getString(R.string.chat_smart_exercise_luteal)
            else                  -> stringResolver.getString(R.string.chat_smart_exercise_general)
        }
    }

    fun buildDischargeResponse(ctx: CycleContext?): String {
        return stringResolver.getString(R.string.chat_smart_discharge_body)
    }

    fun buildNextPeriodResponse(ctx: CycleContext?): String {
        if (ctx == null) {
            return stringResolver.getString(R.string.chat_smart_next_period_no_ctx)
        }
        return if (ctx.isLate) {
            stringResolver.getString(R.string.chat_smart_next_period_late, ctx.delayDays, ctx.cycleLength)
        } else {
            stringResolver.getString(
                R.string.chat_smart_next_period_estimate,
                ctx.daysUntilPeriod,
                ctx.dayOfCycle,
                ctx.cycleLength
            )
        }
    }

    fun buildCurrentPhaseResponse(ctx: CycleContext?): String {
        if (ctx == null) return stringResolver.getString(R.string.chat_smart_current_phase_no_ctx)
        return stringResolver.getString(
            R.string.chat_smart_current_phase_body,
            ctx.dayOfCycle,
            formatPhaseName(ctx.phase),
            ctx.dayOfPhase,
            ctx.daysUntilPeriod
        )
    }

    fun buildContraceptionResponse(ctx: CycleContext?): String {
        return if (ctx?.isOnContraceptive == true) {
            stringResolver.getString(R.string.chat_smart_contraception_active)
        } else {
            stringResolver.getString(R.string.chat_smart_contraception_natural)
        }
    }

    fun buildStressResponse(ctx: CycleContext?): String {
        return stringResolver.getString(R.string.chat_smart_stress_body)
    }

    fun buildEnergyResponse(ctx: CycleContext?): String {
        return stringResolver.getString(R.string.chat_smart_energy_body)
    }

    private fun buildGreetingResponse(ctx: CycleContext?): String {
        val cycleState = if (ctx != null) {
            stringResolver.getString(R.string.chat_smart_greeting_ctx, ctx.dayOfCycle, formatPhaseName(ctx.phase))
        } else stringResolver.getString(R.string.chat_smart_greeting_no_ctx)

        return stringResolver.getString(R.string.chat_smart_greeting_body, cycleState)
    }

    private fun buildContextualFallback(query: String, ctx: CycleContext?): String {
        val cycleState = if (ctx != null) {
            stringResolver.getString(
                R.string.chat_smart_fallback_ctx,
                ctx.dayOfCycle,
                formatPhaseName(ctx.phase),
                ctx.daysUntilPeriod
            )
        } else stringResolver.getString(R.string.chat_smart_fallback_no_ctx)

        return stringResolver.getString(R.string.chat_smart_fallback_body, cycleState)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private fun normalizeText(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .trim()
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    fun formatPhaseName(phase: CyclePhase): String = stringResolver.formatPhaseName(phase)

    fun formatVarName(variable: BioVariable): String = stringResolver.formatBioVarName(variable)
}
