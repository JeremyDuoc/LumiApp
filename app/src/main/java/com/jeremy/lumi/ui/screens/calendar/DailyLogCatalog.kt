package com.jeremy.lumi.ui.screens.calendar

import com.jeremy.lumi.R
import com.jeremy.lumi.domain.util.ClinicalLabels

internal data class LogSymptom(val fallback: String, val resId: Int, val category: Int) {
    val name: String get() = ClinicalLabels.getCanonicalSymptom(resId) ?: fallback
}

internal val logSymptomCatalog = listOf(
    LogSymptom("Cólicos", R.string.symp_cramps, R.string.log_cat_pain),
    LogSymptom("Dolor lumbar", R.string.symp_back_pain, R.string.log_cat_pain),
    LogSymptom("Dolor de cabeza", R.string.symp_headache, R.string.log_cat_pain),
    LogSymptom("Migraña", R.string.symp_migraine, R.string.log_cat_pain),
    LogSymptom("Pecho sensible", R.string.symp_breast_tenderness, R.string.log_cat_pain),
    LogSymptom("Dolor pélvico", R.string.symp_pelvic_pain, R.string.log_cat_pain),
    LogSymptom("Dolor muscular", R.string.symp_muscle_pain, R.string.log_cat_pain),
    LogSymptom("Calambres", R.string.symp_spasms, R.string.log_cat_pain),
    LogSymptom("Hinchazón", R.string.symp_bloating, R.string.log_cat_digestive_filter),
    LogSymptom("Náuseas", R.string.symp_nausea, R.string.log_cat_digestive_filter),
    LogSymptom("Estreñimiento", R.string.symp_constipation, R.string.log_cat_digestive_filter),
    LogSymptom("Diarrea", R.string.symp_diarrhea, R.string.log_cat_digestive_filter),
    LogSymptom("Gases", R.string.symp_gas, R.string.log_cat_digestive_filter),
    LogSymptom("Acidez", R.string.symp_heartburn, R.string.log_cat_digestive_filter),
    LogSymptom("Antojos", R.string.symp_cravings, R.string.log_cat_digestive_filter),
    LogSymptom("Fatiga / Cansancio", R.string.symp_fatigue, R.string.log_cat_body),
    LogSymptom("Mareos", R.string.symp_dizziness, R.string.log_cat_body),
    LogSymptom("Sofocos", R.string.symp_hot_flashes, R.string.log_cat_body),
    LogSymptom("Fiebre", R.string.symp_fever, R.string.log_cat_body),
    LogSymptom("Escalofríos", R.string.symp_chills, R.string.log_cat_body),
    LogSymptom("Retención de líquidos", R.string.symp_fluid_retention, R.string.log_cat_body),
    LogSymptom("Acné", R.string.symp_acne, R.string.log_cat_skin),
    LogSymptom("Piel seca", R.string.symp_dry_skin, R.string.log_cat_skin),
    LogSymptom("Piel grasa", R.string.symp_oily_skin, R.string.log_cat_skin),
    LogSymptom("Orinar frecuente", R.string.symp_frequent_urination, R.string.log_cat_urinary),
    LogSymptom("Ardor al orinar", R.string.symp_burning_urination, R.string.log_cat_urinary),
    LogSymptom("Picazón vaginal", R.string.symp_vaginal_itching, R.string.log_cat_intimate),
    LogSymptom("Sequedad vaginal", R.string.symp_vaginal_dryness, R.string.log_cat_intimate),
    LogSymptom("Dolor en relaciones", R.string.symp_pain_intercourse, R.string.log_cat_intimate),
    LogSymptom("Insomnio", R.string.symp_insomnia, R.string.log_cat_sleep_mind),
    LogSymptom("Deseo sexual alto", R.string.symp_libido_high, R.string.log_cat_sleep_mind),
    LogSymptom("Deseo sexual bajo", R.string.symp_libido_low, R.string.log_cat_sleep_mind),
    LogSymptom("Cambios de humor", R.string.symp_mood_swings, R.string.log_cat_sleep_mind),
    LogSymptom("Ansiedad", R.string.symp_anxiety, R.string.log_cat_sleep_mind),
    LogSymptom("Test ovulación LH (+)", R.string.symp_lh_positive, R.string.log_cat_clinical),
    LogSymptom("Test ovulación LH (-)", R.string.symp_lh_negative, R.string.log_cat_clinical),
)
