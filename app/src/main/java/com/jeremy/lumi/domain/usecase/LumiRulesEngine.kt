package com.jeremy.lumi.domain.usecase

import android.content.Context
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.domain.util.ClinicalLabels
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

import androidx.annotation.StringRes

enum class QuestionCategory(@StringRes val labelRes: Int) {
    CYCLE(R.string.chat_cat_cycle),
    SYMPTOMS(R.string.chat_cat_symptoms),
    FERTILITY(R.string.chat_cat_fertility),
    PATTERNS(R.string.chat_cat_patterns),
    WELLBEING(R.string.chat_cat_wellbeing)
}

enum class ChatQuestion(
    @StringRes val displayTextRes: Int,
    val category: QuestionCategory
) {
    // Mi Ciclo
    WHEN_NEXT_PERIOD(R.string.chat_q_when_next_period, QuestionCategory.CYCLE),
    WHAT_PHASE_AM_I(R.string.chat_q_what_phase_am_i, QuestionCategory.CYCLE),
    WHY_LATE_PERIOD(R.string.chat_q_why_late_period, QuestionCategory.CYCLE),
    SPOTTING_EXPLANATION(R.string.chat_q_spotting_explanation, QuestionCategory.CYCLE),

    // Dolor y Síntomas
    WHY_CRAMPS(R.string.chat_q_why_cramps, QuestionCategory.SYMPTOMS),
    HEADACHE_RELIEF(R.string.chat_q_headache_relief, QuestionCategory.SYMPTOMS),
    DIGESTIVE_RELIEF(R.string.chat_q_digestive_relief, QuestionCategory.SYMPTOMS),
    WHY_FEEL_TIRED(R.string.chat_q_why_feel_tired, QuestionCategory.SYMPTOMS),
    WHY_MOOD_CHANGES(R.string.chat_q_why_mood_changes, QuestionCategory.SYMPTOMS),

    // Fertilidad y Métodos
    WHEN_FERTILE_WINDOW(R.string.chat_q_when_fertile_window, QuestionCategory.FERTILITY),
    UNPROTECTED_SEX(R.string.chat_q_unprotected_sex, QuestionCategory.FERTILITY),
    CONTRACEPTION_INFO(R.string.chat_q_contraception_info, QuestionCategory.FERTILITY),

    // Mis Patrones
    BIO_PATTERNS(R.string.chat_q_bio_patterns, QuestionCategory.PATTERNS),
    SLEEP_AND_CYCLE(R.string.chat_q_sleep_and_cycle, QuestionCategory.PATTERNS),
    STRESS_AND_CYCLE(R.string.chat_q_stress_and_cycle, QuestionCategory.PATTERNS),

    // Nutrición y Hábitos
    NUTRITION_RECOMMENDATION(R.string.chat_q_nutrition_recommendation, QuestionCategory.WELLBEING),
    EXERCISE_RECOMMENDATION(R.string.chat_q_exercise_recommendation, QuestionCategory.WELLBEING),
    HOW_IMPROVE_ENERGY(R.string.chat_q_how_improve_energy, QuestionCategory.WELLBEING);
}

data class CycleContext(
    val phase           : CyclePhase,
    val dayOfCycle      : Int,
    val dayOfPhase      : Int,
    val daysUntilPeriod : Int,
    val daysUntilOvulation: Int,
    val cycleLength     : Int,
    val isLate          : Boolean,
    val delayDays       : Int,
    val todayPainLevel  : Int       = 0,
    val todayEnergyLevel: Int?      = null,
    val todaySleepHours : Float?    = null,
    val todayStressLevel: Int?      = null,
    val todaySymptoms   : List<String> = emptyList(),
    val todayMood       : String?   = null,
    val todayBbt        : Float?    = null,
    val userGoal        : String    = "TRACK_CYCLE",
    val isOnContraceptive: Boolean  = false
)

@Singleton
class LumiRulesEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _currentCycleContext = MutableStateFlow<CycleContext?>(null)
    val currentCycleContext: StateFlow<CycleContext?> = _currentCycleContext.asStateFlow()

    fun updateContext(context: CycleContext) {
        _currentCycleContext.value = context
    }

    private fun getRandomStringArray(resId: Int): String {
        val array = context.resources.getStringArray(resId)
        return array.random()
    }

    fun generateWelcomeMessage(ctx: CycleContext): String {
        val baseGreeting = getRandomStringArray(R.array.chat_welcome_base)
        
        val isPmsWindow = ctx.phase == CyclePhase.LUTEAL && ctx.daysUntilPeriod in 0..6
        val hasPmsSymptoms = ctx.todayPainLevel > 0 || 
                             ClinicalLabels.matchesMood(ctx.todayMood, R.string.mood_sad, R.string.mood_irritated, R.string.mood_sensitive) || 
                             ClinicalLabels.hasAnySymptom(ctx.todaySymptoms, R.string.symp_headache, R.string.symp_bloating, R.string.symp_breast_tenderness, R.string.symp_fatigue, R.string.symp_acne) ||
                             (ctx.todayStressLevel != null && ctx.todayStressLevel >= 6)
        
        val isTiredOrSad = ClinicalLabels.matchesMood(ctx.todayMood, R.string.mood_sad) || ClinicalLabels.hasAnySymptom(ctx.todaySymptoms, R.string.symp_fatigue)
        val isHappyOrEnergetic = ClinicalLabels.matchesMood(ctx.todayMood, R.string.mood_happy, R.string.mood_energetic, R.string.mood_calm)

        if (isPmsWindow && hasPmsSymptoms) {
            val moodMsg = ctx.todayMood?.let { "te sientas ${it.lowercase()}" } ?: "tengas estos síntomas"
            return baseGreeting + String.format(getRandomStringArray(R.array.chat_welcome_pms), moodMsg)
        }
        
        // periodo: Fase Folicular + Cansancio (Inusual)
        if (ctx.phase == CyclePhase.FOLLICULAR && isTiredOrSad) {
            return baseGreeting + context.getString(R.string.chat_welcome_follicular_tired)
        }
        
        // periodo: Fase Folicular + Energía (Esperado)
        if (ctx.phase == CyclePhase.FOLLICULAR && isHappyOrEnergetic) {
            return baseGreeting + context.getString(R.string.chat_welcome_follicular_energy)
        }
        
        // periodo: Fase Lútea Temprana/Media + Antojos o Cansancio
        val hasLutealSymptoms = ctx.todaySymptoms.intersect(listOf("Antojos", "Hinchazón", "Digestión pesada")).isNotEmpty()
        if (ctx.phase == CyclePhase.LUTEAL && !isPmsWindow && (hasLutealSymptoms || isTiredOrSad)) {
            return baseGreeting + context.getString(R.string.chat_welcome_luteal_symptoms)
        }

        val painPrefix = if (ctx.todayPainLevel >= 7) {
            String.format(getRandomStringArray(R.array.chat_welcome_pain), ctx.todayPainLevel)
        } else ""

        val phaseMessage = when (ctx.phase) {
            CyclePhase.MENSTRUAL -> getRandomStringArray(R.array.chat_welcome_phase_menstrual)
            CyclePhase.FOLLICULAR -> getRandomStringArray(R.array.chat_welcome_phase_follicular)
            CyclePhase.OVULATION -> getRandomStringArray(R.array.chat_welcome_phase_ovulation)
            CyclePhase.LUTEAL -> getRandomStringArray(R.array.chat_welcome_phase_luteal)
            else -> getRandomStringArray(R.array.chat_welcome_phase_unknown)
        }
        return baseGreeting + painPrefix + phaseMessage
    }

    fun answerQuestion(question: ChatQuestion, cycleContext: CycleContext): String {
        return when (question) {
            ChatQuestion.WHY_FEEL_TIRED          -> answerTired(cycleContext)
            ChatQuestion.WHEN_NEXT_PERIOD        -> answerNextPeriod(cycleContext)
            ChatQuestion.WHEN_FERTILE_WINDOW     -> answerFertileWindow(cycleContext)
            ChatQuestion.WHY_MOOD_CHANGES        -> answerMoodChanges(cycleContext)
            ChatQuestion.WHY_CRAMPS              -> answerCramps(cycleContext)
            ChatQuestion.WHAT_PHASE_AM_I         -> answerCurrentPhase(cycleContext)
            ChatQuestion.HOW_IMPROVE_ENERGY      -> answerImproveEnergy(cycleContext)
            ChatQuestion.SLEEP_AND_CYCLE         -> answerSleepAndCycle(cycleContext)
            ChatQuestion.STRESS_AND_CYCLE        -> answerStressAndCycle(cycleContext)
            ChatQuestion.EXERCISE_RECOMMENDATION -> answerExercise(cycleContext)
            ChatQuestion.WHY_LATE_PERIOD         -> answerLatePeriod(cycleContext)
            ChatQuestion.SPOTTING_EXPLANATION    -> answerSpotting(cycleContext)
            ChatQuestion.HEADACHE_RELIEF         -> context.getString(R.string.chat_smart_headache_body, "")
            ChatQuestion.DIGESTIVE_RELIEF        -> context.getString(R.string.chat_smart_digestive_body)
            ChatQuestion.UNPROTECTED_SEX         -> context.getString(R.string.chat_smart_unprotected_sex)
            ChatQuestion.CONTRACEPTION_INFO      -> if (cycleContext.isOnContraceptive) context.getString(R.string.chat_smart_contraception_active) else context.getString(R.string.chat_smart_contraception_natural)
            ChatQuestion.BIO_PATTERNS            -> context.getString(R.string.chat_smart_patterns_empty)
            ChatQuestion.NUTRITION_RECOMMENDATION -> context.getString(R.string.chat_smart_nutrition_general)
        }
    }

    fun getAvailableQuestions(context: CycleContext): List<ChatQuestion> {
        val filtered = ChatQuestion.entries.filter { question ->
            when (question) {
                ChatQuestion.WHY_LATE_PERIOD     -> context.isLate
                ChatQuestion.SPOTTING_EXPLANATION -> context.todaySymptoms.any {
                    it.contains("spotting", ignoreCase = true) || it.contains("manchado", ignoreCase = true)
                }
                ChatQuestion.WHY_CRAMPS          -> context.todayPainLevel > 0 || context.daysUntilPeriod in 0..2
                ChatQuestion.CONTRACEPTION_INFO  -> context.isOnContraceptive
                ChatQuestion.WHEN_FERTILE_WINDOW -> !context.isOnContraceptive
                else -> true
            }
        }
        
        return filtered.sortedByDescending { question ->
            when {
                context.isLate && question == ChatQuestion.WHY_LATE_PERIOD -> 4
                context.todayPainLevel >= 5 && question == ChatQuestion.WHY_CRAMPS -> 3
                context.daysUntilPeriod in 0..2 && (question == ChatQuestion.WHEN_NEXT_PERIOD || question == ChatQuestion.WHY_CRAMPS) -> 3
                context.daysUntilOvulation in 0..2 && question == ChatQuestion.WHEN_FERTILE_WINDOW -> 3
                else -> 0
            }
        }
    }

    private fun answerTired(ctx: CycleContext): String {
        val sleepNote = if (ctx.todaySleepHours != null && ctx.todaySleepHours < 7f) {
            String.format(getRandomStringArray(R.array.chat_energy_sleep_tip), ctx.todaySleepHours.toString())
        } else ""

        val resId = when (ctx.phase) {
            CyclePhase.MENSTRUAL -> R.array.chat_tired_menstrual
            CyclePhase.FOLLICULAR -> R.array.chat_tired_follicular
            CyclePhase.OVULATION -> R.array.chat_tired_ovulation
            CyclePhase.LUTEAL -> R.array.chat_tired_luteal
            else -> R.array.chat_tired_unknown
        }
        return String.format(getRandomStringArray(resId), sleepNote)
    }

    private fun answerNextPeriod(ctx: CycleContext): String {
        return if (ctx.isLate) {
            String.format(getRandomStringArray(R.array.chat_next_period_late), ctx.delayDays, ctx.cycleLength)
        } else {
            val base = when (ctx.daysUntilPeriod) {
                0 -> getRandomStringArray(R.array.chat_next_period_base_0)
                1 -> getRandomStringArray(R.array.chat_next_period_base_1)
                else -> String.format(getRandomStringArray(R.array.chat_next_period_base_n), ctx.daysUntilPeriod)
            }
            String.format(getRandomStringArray(R.array.chat_next_period_on_time), base)
        }
    }

    private fun answerFertileWindow(ctx: CycleContext): String {
        if (ctx.isOnContraceptive) {
            return getRandomStringArray(R.array.chat_fertile_contraceptive)
        }
        return if (ctx.daysUntilOvulation <= 0) {
            if (ctx.phase == CyclePhase.OVULATION) {
                getRandomStringArray(R.array.chat_fertile_now)
            } else {
                String.format(getRandomStringArray(R.array.chat_fertile_past), ctx.cycleLength - 14)
            }
        } else {
            String.format(getRandomStringArray(R.array.chat_fertile_future), ctx.daysUntilOvulation)
        }
    }

    private fun answerMoodChanges(ctx: CycleContext): String {
        val resId = when (ctx.phase) {
            CyclePhase.MENSTRUAL -> R.array.chat_mood_menstrual
            CyclePhase.FOLLICULAR -> R.array.chat_mood_follicular
            CyclePhase.OVULATION -> R.array.chat_mood_ovulation
            CyclePhase.LUTEAL -> R.array.chat_mood_luteal
            else -> R.array.chat_mood_unknown
        }
        return getRandomStringArray(resId)
    }

    private fun answerCramps(ctx: CycleContext): String {
        val adviceResId = when {
            ctx.todayPainLevel >= 8 -> R.array.chat_cramps_advice_high
            ctx.todayPainLevel >= 5 -> R.array.chat_cramps_advice_med
            else -> R.array.chat_cramps_advice_low
        }
        val advice = getRandomStringArray(adviceResId)
        
        val resId = when (ctx.phase) {
            CyclePhase.MENSTRUAL -> R.array.chat_cramps_menstrual
            CyclePhase.OVULATION -> R.array.chat_cramps_ovulation
            else -> R.array.chat_cramps_unknown
        }
        return String.format(getRandomStringArray(resId), advice)
    }

    private fun answerCurrentPhase(ctx: CycleContext): String {
        val phaseName = when (ctx.phase) {
            CyclePhase.MENSTRUAL  -> context.getString(R.string.phase_menstrual_name)
            CyclePhase.FOLLICULAR -> context.getString(R.string.phase_follicular_name)
            CyclePhase.OVULATION  -> context.getString(R.string.phase_ovulation_name)
            CyclePhase.LUTEAL     -> context.getString(R.string.phase_luteal_name)
            CyclePhase.PREGNANCY  -> context.getString(R.string.phase_pregnancy_name)
            CyclePhase.UNKNOWN    -> context.getString(R.string.phase_unknown_name)
        }
        val phaseDescResId = when (ctx.phase) {
            CyclePhase.MENSTRUAL  -> R.array.chat_phase_desc_menstrual
            CyclePhase.FOLLICULAR -> R.array.chat_phase_desc_follicular
            CyclePhase.OVULATION  -> R.array.chat_phase_desc_ovulation
            CyclePhase.LUTEAL     -> R.array.chat_phase_desc_luteal
            else -> null
        }
        val phaseDesc = phaseDescResId?.let { getRandomStringArray(it) } ?: ""
        val untilPeriod = if (ctx.daysUntilPeriod > 0) context.getString(R.string.chat_phase_info_until_period, ctx.daysUntilPeriod) else ""
        
        return String.format(getRandomStringArray(R.array.chat_phase_info), 
            ctx.dayOfCycle, ctx.dayOfPhase, phaseName, phaseDesc, untilPeriod)
    }

    private fun answerImproveEnergy(ctx: CycleContext): String {
        val sleepTip = if (ctx.todaySleepHours != null && ctx.todaySleepHours < 7f)
            String.format(getRandomStringArray(R.array.chat_energy_sleep_tip), ctx.todaySleepHours.toString())
        else ""

        val resId = when (ctx.phase) {
            CyclePhase.MENSTRUAL  -> R.array.chat_energy_menstrual
            CyclePhase.FOLLICULAR -> R.array.chat_energy_follicular
            CyclePhase.OVULATION  -> R.array.chat_energy_ovulation
            CyclePhase.LUTEAL     -> R.array.chat_energy_luteal
            else -> R.array.chat_energy_unknown
        }
        return String.format(getRandomStringArray(resId), sleepTip)
    }

    private fun answerSleepAndCycle(ctx: CycleContext): String {
        val personalNote = if (ctx.todaySleepHours != null && ctx.todaySleepHours < 6f) {
            String.format(getRandomStringArray(R.array.chat_sleep_note_low), ctx.todaySleepHours.toString())
        } else ""
        return String.format(getRandomStringArray(R.array.chat_sleep_cycle), personalNote)
    }

    private fun answerStressAndCycle(ctx: CycleContext): String {
        val stressNote = if (ctx.todayStressLevel != null && ctx.todayStressLevel >= 7) {
            String.format(getRandomStringArray(R.array.chat_stress_note_high), ctx.todayStressLevel)
        } else ""
        return String.format(getRandomStringArray(R.array.chat_stress_cycle), stressNote)
    }

    private fun answerExercise(ctx: CycleContext): String {
        val resId = when (ctx.phase) {
            CyclePhase.MENSTRUAL  -> R.array.chat_exercise_menstrual
            CyclePhase.FOLLICULAR -> R.array.chat_exercise_follicular
            CyclePhase.OVULATION  -> R.array.chat_exercise_ovulation
            CyclePhase.LUTEAL     -> R.array.chat_exercise_luteal
            else -> R.array.chat_exercise_unknown
        }
        return getRandomStringArray(resId)
    }

    private fun answerLatePeriod(ctx: CycleContext): String {
        return if (!ctx.isLate) {
            getRandomStringArray(R.array.chat_late_normal)
        } else {
            val adviceResId = if (ctx.delayDays >= 14) R.array.chat_late_delayed_advice_long else R.array.chat_late_delayed_advice_short
            val advice = String.format(getRandomStringArray(adviceResId), ctx.delayDays)
            String.format(getRandomStringArray(R.array.chat_late_delayed), ctx.delayDays, advice)
        }
    }

    private fun answerSpotting(ctx: CycleContext): String {
        return String.format(getRandomStringArray(R.array.chat_spotting), ctx.cycleLength - 14)
    }
}
