package com.jeremy.lumi.domain.usecase

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.BioVariable
import com.jeremy.lumi.domain.model.CyclePhase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface ChatStringResolver {
    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
    fun getStringArray(@ArrayRes resId: Int): Array<String>
    fun getRandomStringFromArray(@ArrayRes resId: Int): String
    fun formatPhaseName(phase: CyclePhase): String
    fun formatBioVarName(variable: BioVariable): String
}

@Singleton
class AndroidChatStringResolver @Inject constructor(
    @ApplicationContext private val context: Context
) : ChatStringResolver {

    override fun getString(resId: Int): String = context.getString(resId)

    override fun getString(resId: Int, vararg formatArgs: Any): String =
        context.getString(resId, *formatArgs)

    override fun getStringArray(resId: Int): Array<String> =
        context.resources.getStringArray(resId)

    override fun getRandomStringFromArray(resId: Int): String {
        val array = getStringArray(resId)
        return if (array.isNotEmpty()) array.random() else ""
    }

    override fun formatPhaseName(phase: CyclePhase): String = when (phase) {
        CyclePhase.MENSTRUAL  -> context.getString(R.string.phase_menstrual_name)
        CyclePhase.FOLLICULAR -> context.getString(R.string.phase_follicular_name)
        CyclePhase.OVULATION  -> context.getString(R.string.phase_ovulation_name)
        CyclePhase.LUTEAL     -> context.getString(R.string.phase_luteal_name)
        CyclePhase.PREGNANCY  -> context.getString(R.string.phase_pregnancy_name)
        CyclePhase.UNKNOWN    -> context.getString(R.string.phase_unknown_name)
    }

    override fun formatBioVarName(variable: BioVariable): String = when (variable) {
        BioVariable.SLEEP_HOURS     -> context.getString(R.string.bio_var_sleep)
        BioVariable.PAIN_LEVEL      -> context.getString(R.string.bio_var_pain)
        BioVariable.STRESS_LEVEL    -> context.getString(R.string.bio_var_stress)
        BioVariable.ENERGY_LEVEL    -> context.getString(R.string.bio_var_energy)
        BioVariable.WATER_INTAKE    -> context.getString(R.string.bio_var_water)
        BioVariable.BASAL_BODY_TEMP -> context.getString(R.string.bio_var_bbt)
    }
}
