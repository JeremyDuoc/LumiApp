package com.jeremy.lumi.domain.usecase

import androidx.annotation.ArrayRes
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CyclePhase

object DailyInsightGenerator {

    /**
     * Devuelve el ID de recurso (@ArrayRes) de una lista de mensajes cálidos
     * dependiendo de la fase y el día relativo del ciclo.
     */
    @ArrayRes
    fun getInsightForDay(
        phase: CyclePhase,
        dayOfPhase: Int, // Día 1 de la regla, Día 2 de la fase folicular, etc.
        cycleDay: Int,   // Día global del ciclo (1 a N)
        isLate: Boolean,
        userGoal: String = "health"
    ): Int {
        if (isLate) {
            return if (userGoal == "SEEK_PREGNANCY") {
                R.array.insight_late_goal_pregnancy
            } else {
                R.array.insight_late
            }
        }

        return when (phase) {
            CyclePhase.MENSTRUAL -> {
                when {
                    dayOfPhase == 1 -> R.array.insight_mens_day1
                    dayOfPhase == 2 -> R.array.insight_mens_day2
                    dayOfPhase >= 3 -> R.array.insight_mens_day3
                    else -> R.array.insight_mens_end
                }
            }
            CyclePhase.FOLLICULAR -> {
                when {
                    dayOfPhase <= 3 -> R.array.insight_foli_start
                    dayOfPhase in 4..7 -> R.array.insight_foli_mid
                    else -> R.array.insight_foli_end
                }
            }
            CyclePhase.OVULATION -> {
                if (userGoal == "SEEK_PREGNANCY") {
                    R.array.insight_ovu_goal_pregnancy
                } else {
                    when (dayOfPhase) {
                        1 -> R.array.insight_ovu_day1
                        2 -> R.array.insight_ovu_day2
                        else -> R.array.insight_ovu_end
                    }
                }
            }
            CyclePhase.LUTEAL -> {
                when {
                    dayOfPhase <= 3 -> R.array.insight_luteal_start
                    dayOfPhase in 4..7 -> R.array.insight_luteal_mid
                    dayOfPhase >= 8 -> R.array.insight_luteal_pre
                    else -> R.array.insight_luteal_end
                }
            }
            CyclePhase.UNKNOWN -> {
                R.array.insight_unknown
            }
            CyclePhase.PREGNANCY -> {
                R.array.insight_pregnancy
            }
        }
    }
}
