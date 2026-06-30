package com.jeremy.lumi.domain.usecase

import android.content.Context
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ChatQuestion(
    val displayText: String,
    val category: QuestionCategory
) {
    WHY_FEEL_TIRED("¿Por qué me siento tan cansada hoy?", QuestionCategory.WELLBEING),
    WHEN_NEXT_PERIOD("¿Cuándo llega mi próximo periodo?", QuestionCategory.CYCLE),
    WHEN_FERTILE_WINDOW("¿Cuándo es mi próxima ventana fértil?", QuestionCategory.CYCLE),
    WHY_MOOD_CHANGES("¿Por qué tengo tantos cambios de humor?", QuestionCategory.WELLBEING),
    WHY_CRAMPS("¿Por qué tengo cólicos?", QuestionCategory.WELLBEING),
    WHAT_PHASE_AM_I("¿En qué fase del ciclo estoy?", QuestionCategory.CYCLE),
    HOW_IMPROVE_ENERGY("¿Cómo puedo tener más energía hoy?", QuestionCategory.TIPS),
    SLEEP_AND_CYCLE("¿Cómo afecta el sueño a mi ciclo?", QuestionCategory.TIPS),
    STRESS_AND_CYCLE("¿El estrés retrasa mi regla?", QuestionCategory.TIPS),
    EXERCISE_RECOMMENDATION("¿Qué tipo de ejercicio me recomiendas hoy?", QuestionCategory.TIPS),
    WHY_LATE_PERIOD("¿Por qué se retrasó mi regla?", QuestionCategory.CONCERN),
    SPOTTING_EXPLANATION("Tengo manchado fuera de mi periodo, ¿es normal?", QuestionCategory.CONCERN);
}

enum class QuestionCategory {
    CYCLE, WELLBEING, TIPS, CONCERN
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
                             ctx.todayMood in listOf("Triste", "Irritado", "Sensible") || 
                             ctx.todaySymptoms.intersect(listOf("Dolor de cabeza", "Hinchazón", "Sensibilidad en los senos", "Cansancio", "Acné")).isNotEmpty() ||
                             (ctx.todayStressLevel != null && ctx.todayStressLevel >= 6)
        
        if (isPmsWindow && hasPmsSymptoms) {
            val moodMsg = ctx.todayMood?.let { "te sientas ${it.lowercase()}" } ?: "tengas estos síntomas"
            return baseGreeting + String.format(getRandomStringArray(R.array.chat_welcome_pms), moodMsg)
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

    fun answerQuestion(question: ChatQuestion, context: CycleContext): String {
        return when (question) {
            ChatQuestion.WHY_FEEL_TIRED       -> answerTired(context)
            ChatQuestion.WHEN_NEXT_PERIOD     -> answerNextPeriod(context)
            ChatQuestion.WHEN_FERTILE_WINDOW  -> answerFertileWindow(context)
            ChatQuestion.WHY_MOOD_CHANGES     -> answerMoodChanges(context)
            ChatQuestion.WHY_CRAMPS           -> answerCramps(context)
            ChatQuestion.WHAT_PHASE_AM_I      -> answerCurrentPhase(context)
            ChatQuestion.HOW_IMPROVE_ENERGY   -> answerImproveEnergy(context)
            ChatQuestion.SLEEP_AND_CYCLE      -> answerSleepAndCycle(context)
            ChatQuestion.STRESS_AND_CYCLE     -> answerStressAndCycle(context)
            ChatQuestion.EXERCISE_RECOMMENDATION -> answerExercise(context)
            ChatQuestion.WHY_LATE_PERIOD      -> answerLatePeriod(context)
            ChatQuestion.SPOTTING_EXPLANATION -> answerSpotting(context)
        }
    }

    fun getAvailableQuestions(context: CycleContext): List<ChatQuestion> {
        val filtered = ChatQuestion.entries.filter { question ->
            when (question) {
                ChatQuestion.WHY_LATE_PERIOD     -> context.isLate
                ChatQuestion.SPOTTING_EXPLANATION -> false
                ChatQuestion.WHY_CRAMPS          -> context.todayPainLevel > 0 || context.daysUntilPeriod in 0..2
                ChatQuestion.WHEN_FERTILE_WINDOW -> !context.isOnContraceptive &&
                    (context.userGoal in listOf("AVOID_PREGNANCY", "SEEK_PREGNANCY") || context.daysUntilOvulation in 0..2)
                else -> true
            }
        }
        
        return filtered.sortedByDescending { question ->
            when {
                context.daysUntilPeriod in 0..2 && (question == ChatQuestion.WHEN_NEXT_PERIOD || question == ChatQuestion.WHY_CRAMPS) -> 2
                context.daysUntilOvulation in 0..2 && question == ChatQuestion.WHEN_FERTILE_WINDOW -> 2
                question.category == QuestionCategory.CONCERN && context.isLate -> 1
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
            CyclePhase.MENSTRUAL  -> "Menstrual"
            CyclePhase.FOLLICULAR -> "Folicular"
            CyclePhase.OVULATION  -> "Ovulación"
            CyclePhase.LUTEAL     -> "Lútea"
            CyclePhase.PREGNANCY  -> "Embarazo"
            CyclePhase.UNKNOWN    -> "Desconocida"
        }
        val phaseDescResId = when (ctx.phase) {
            CyclePhase.MENSTRUAL  -> R.array.chat_phase_desc_menstrual
            CyclePhase.FOLLICULAR -> R.array.chat_phase_desc_follicular
            CyclePhase.OVULATION  -> R.array.chat_phase_desc_ovulation
            CyclePhase.LUTEAL     -> R.array.chat_phase_desc_luteal
            else -> null
        }
        val phaseDesc = phaseDescResId?.let { getRandomStringArray(it) } ?: ""
        val untilPeriod = if (ctx.daysUntilPeriod > 0) "Tu próximo periodo se estima en ${ctx.daysUntilPeriod} días." else ""
        
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
