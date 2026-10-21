package com.jeremy.lumi.ui.screens.calendar

import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.domain.util.ClinicalLabels
import java.io.Serializable

/** A date-scoped, saveable draft. Unknown historical values remain editable and are never dropped. */
internal data class DailyLogDraft(
    val flow: String? = null,
    val spotting: Boolean = false,
    val pain: Int = 0,
    val mood: String? = null,
    val symptoms: Map<String, Int> = emptyMap(),
    val mucus: String? = null,
    val notes: String = "",
    val hadIntercourse: Boolean = false,
    val protection: Boolean? = null,
    val method: String? = null,
    val intimacyNotes: String = "",
    val showIntimacy: Boolean = true,
    val sleep: String = "",
    val energy: Int? = null,
    val stress: Int? = null,
    val water: String = "",
    val temperature: String = ""
) : Serializable {
    fun toggleSymptom(name: String): DailyLogDraft = copy(
        symptoms = if (name in symptoms) symptoms - name else symptoms + (name to 0)
    )

    val hasContent: Boolean
        get() = flow != null || spotting || pain > 0 || mood != null || symptoms.isNotEmpty() ||
            mucus != null || notes.isNotBlank() || hadIntercourse || sleep.isNotBlank() ||
            energy != null || stress != null || water.isNotBlank() || temperature.isNotBlank()

    val validMeasurements: Boolean
        get() = validNumber(sleep, 0f..24f) && validNumber(water, 0f..20f) &&
            validNumber(temperature, 30f..45f)

    companion object {
        fun from(log: DailyLogWithSymptoms?): DailyLogDraft {
            if (log == null) return DailyLogDraft()
            val day = log.dailyLog
            return DailyLogDraft(
                flow = canonical(day.flowIntensity, ClinicalLabels.FLOW_MAPPINGS),
                spotting = day.spotting,
                pain = day.painLevel,
                mood = canonical(day.mood, ClinicalLabels.MOOD_MAPPINGS),
                symptoms = log.symptoms.associate {
                    // Exact catalog names win over broad historical aliases (e.g. Calambres/Cólicos).
                    val name = logSymptomCatalog.firstOrNull { item ->
                        item.name.equals(it.name, true) || item.fallback.equals(it.name, true)
                    }?.name ?: canonical(it.name, ClinicalLabels.SYMPTOM_MAPPINGS)!!
                    name to it.intensity
                },
                mucus = canonical(day.cervicalMucus, ClinicalLabels.MUCUS_MAPPINGS),
                notes = day.notes.orEmpty(),
                hadIntercourse = day.hadIntercourse,
                protection = day.protectionUsed,
                method = canonical(day.contraceptionMethod, ClinicalLabels.METHOD_MAPPINGS),
                intimacyNotes = day.intercourseNotes.orEmpty(),
                showIntimacy = day.showIntercourseOnCalendar,
                sleep = day.sleepHours?.toString().orEmpty(),
                energy = day.energyLevel,
                stress = day.stressLevel,
                water = day.waterIntake?.toString().orEmpty(),
                temperature = day.basalBodyTemp?.toString().orEmpty()
            )
        }

        private fun canonical(value: String?, mappings: List<ClinicalLabels.OptionMapping>): String? =
            value?.let { raw ->
                mappings.firstOrNull {
                    it.canonicalName.equals(raw, true) || it.aliases.any { alias -> alias.equals(raw, true) }
                }?.canonicalName ?: raw
            }
    }
}

internal fun parseLogNumber(value: String): Float? =
    value.trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }

internal fun validNumber(value: String, range: ClosedFloatingPointRange<Float>): Boolean =
    value.isBlank() || parseLogNumber(value)?.let { it in range } == true

internal fun normalizeSymptomSearch(value: String): String =
    java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "").lowercase(java.util.Locale.ROOT).trim()
