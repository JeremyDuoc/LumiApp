package com.jeremy.lumi.domain.util

import com.jeremy.lumi.R
import java.util.Locale

/**
 * Normalizador clínico bidireccional y canónico para datos biológicos/médicos (LUM-14).
 *
 * Permite desvincular el almacenamiento de registros locales de la lengua de visualización
 * activa en el dispositivo, reconociendo sinónimos históricos, etiquetas en español, inglés
 * y constantes canónicas, y preservando valores desconocidos o personalizados.
 */
object ClinicalLabels {

    data class OptionMapping(
        val resId: Int,
        val canonicalName: String,
        val aliases: Set<String>
    )

    // ── FLUJO MENSTRUAL ──────────────────────────────────────────────────────────
    val FLOW_MAPPINGS = listOf(
        OptionMapping(
            resId = R.string.flow_light,
            canonicalName = "Ligero",
            aliases = setOf("ligero", "light", "flow_light", "flujo ligero", "leve")
        ),
        OptionMapping(
            resId = R.string.flow_medium,
            canonicalName = "Medio",
            aliases = setOf("medio", "medium", "flow_medium", "flujo medio", "moderado")
        ),
        OptionMapping(
            resId = R.string.flow_heavy,
            canonicalName = "Abundante",
            aliases = setOf("abundante", "heavy", "flow_heavy", "flujo abundante", "intenso")
        )
    )

    // ── ESTADO DE ÁNIMO ──────────────────────────────────────────────────────────
    val MOOD_MAPPINGS = listOf(
        OptionMapping(
            resId = R.string.mood_happy,
            canonicalName = "Feliz",
            aliases = setOf("feliz", "happy", "mood_happy", "alegre")
        ),
        OptionMapping(
            resId = R.string.mood_calm,
            canonicalName = "Calmada",
            aliases = setOf("calmada", "calmado", "calm", "mood_calm", "tranquila", "tranquilo")
        ),
        OptionMapping(
            resId = R.string.mood_energetic,
            canonicalName = "Enérgica",
            aliases = setOf("enérgica", "energética", "enérgico", "energetic", "mood_energetic", "con energía")
        ),
        OptionMapping(
            resId = R.string.mood_sensitive,
            canonicalName = "Sensible",
            aliases = setOf("sensible", "sensitive", "mood_sensitive")
        ),
        OptionMapping(
            resId = R.string.mood_sad,
            canonicalName = "Triste",
            aliases = setOf("triste", "sad", "mood_sad", "bajoneada", "deprimida")
        ),
        OptionMapping(
            resId = R.string.mood_anxious,
            canonicalName = "Ansiosa",
            aliases = setOf("ansiosa", "ansioso", "anxious", "mood_anxious", "ansiedad")
        ),
        OptionMapping(
            resId = R.string.mood_irritated,
            canonicalName = "Irritable",
            aliases = setOf("irritable", "irritated", "irritada", "irritado", "mood_irritated")
        ),
        OptionMapping(
            resId = R.string.mood_overwhelmed,
            canonicalName = "Abrumada",
            aliases = setOf("abrumada", "abrumado", "overwhelmed", "mood_overwhelmed")
        )
    )

    // ── MOCO CERVICAL ────────────────────────────────────────────────────────────
    val MUCUS_MAPPINGS = listOf(
        OptionMapping(
            resId = R.string.mucus_dry,
            canonicalName = "Seco",
            aliases = setOf("seco", "dry", "mucus_dry")
        ),
        OptionMapping(
            resId = R.string.mucus_sticky,
            canonicalName = "Pegajoso",
            aliases = setOf("pegajoso", "sticky", "mucus_sticky")
        ),
        OptionMapping(
            resId = R.string.mucus_creamy,
            canonicalName = "Cremoso",
            aliases = setOf("cremoso", "creamy", "mucus_creamy")
        ),
        OptionMapping(
            resId = R.string.mucus_watery,
            canonicalName = "Acuoso",
            aliases = setOf("acuoso", "watery", "mucus_watery")
        ),
        OptionMapping(
            resId = R.string.mucus_egg_white,
            canonicalName = "Clara de huevo",
            aliases = setOf("clara de huevo", "egg white", "clara de huevo (fértil)", "mucus_egg_white")
        )
    )

    // ── MÉTODOS ANTICONCEPTIVOS ──────────────────────────────────────────────────
    val METHOD_MAPPINGS = listOf(
        OptionMapping(
            resId = R.string.method_condom,
            canonicalName = "Condón",
            aliases = setOf("condón", "condon", "preservativo", "condom", "method_condom")
        ),
        OptionMapping(
            resId = R.string.method_pill,
            canonicalName = "Píldora",
            aliases = setOf("píldora", "pildora", "pastilla", "pill", "method_pill")
        ),
        OptionMapping(
            resId = R.string.method_iud,
            canonicalName = "DIU",
            aliases = setOf("diu", "iud", "diu de cobre", "diu hormonal", "method_iud")
        ),
        OptionMapping(
            resId = R.string.method_patch,
            canonicalName = "Parche",
            aliases = setOf("parche", "patch", "method_patch")
        ),
        OptionMapping(
            resId = R.string.method_ring,
            canonicalName = "Anillo",
            aliases = setOf("anillo", "anillo vaginal", "vaginal ring", "ring", "method_ring")
        ),
        OptionMapping(
            resId = R.string.method_none,
            canonicalName = "Ninguno",
            aliases = setOf("ninguno", "none", "sin método", "method_none")
        )
    )

    // ── SÍNTOMAS ─────────────────────────────────────────────────────────────────
    val SYMPTOM_MAPPINGS = listOf(
        OptionMapping(
            resId = R.string.symp_cramps,
            canonicalName = "Cólicos",
            aliases = setOf("cólicos", "colicos", "cólico", "colico", "cramps", "menstrual cramps", "calambres")
        ),
        OptionMapping(
            resId = R.string.symp_headache,
            canonicalName = "Dolor de cabeza",
            aliases = setOf("dolor de cabeza", "headache", "cefalea")
        ),
        OptionMapping(
            resId = R.string.symp_migraine,
            canonicalName = "Migraña",
            aliases = setOf("migraña", "migrana", "migraine")
        ),
        OptionMapping(
            resId = R.string.symp_back_pain,
            canonicalName = "Dolor lumbar",
            aliases = setOf("dolor lumbar", "lower back pain", "back pain", "dolor de espalda")
        ),
        OptionMapping(
            resId = R.string.symp_acne,
            canonicalName = "Acné",
            aliases = setOf("acné", "acne", "granitos", "pimples")
        ),
        OptionMapping(
            resId = R.string.symp_bloating,
            canonicalName = "Hinchazón",
            aliases = setOf("hinchazón", "hinchazon", "bloating", "distensión", "distension")
        ),
        OptionMapping(
            resId = R.string.symp_fatigue,
            canonicalName = "Fatiga",
            aliases = setOf("fatiga", "fatigue", "cansancio", "agotamiento", "tiredness")
        ),
        OptionMapping(
            resId = R.string.symp_pelvic_pain,
            canonicalName = "Dolor Pélvico",
            aliases = setOf("dolor pélvico", "dolor pelvico", "pelvic pain")
        ),
        OptionMapping(
            resId = R.string.symp_breast_tenderness,
            canonicalName = "Sensibilidad Senos",
            aliases = setOf("sensibilidad senos", "sensibilidad en los senos", "sensibilidad en senos", "breast tenderness", "dolor de senos")
        ),
        OptionMapping(
            resId = R.string.symp_dizziness,
            canonicalName = "Mareos",
            aliases = setOf("mareos", "mareo", "dizziness")
        ),
        OptionMapping(
            resId = R.string.symp_hot_flashes,
            canonicalName = "Sofocos / Bochornos",
            aliases = setOf("sofocos / bochornos", "sofocos", "bochornos", "hot flashes", "bochorno")
        ),
        OptionMapping(
            resId = R.string.symp_insomnia,
            canonicalName = "Insomnio",
            aliases = setOf("insomnio", "insomnia", "dificultad para dormir")
        ),
        OptionMapping(
            resId = R.string.symp_fever,
            canonicalName = "Fiebre",
            aliases = setOf("fiebre", "fever")
        ),
        OptionMapping(
            resId = R.string.symp_nausea,
            canonicalName = "Náuseas",
            aliases = setOf("náuseas", "nauseas", "náusea", "nausea")
        ),
        OptionMapping(
            resId = R.string.symp_diarrhea,
            canonicalName = "Prob. Digestivos",
            aliases = setOf("prob. digestivos", "problemas digestivos", "digestive upset", "diarrea", "diarrhea")
        ),
        OptionMapping(
            resId = R.string.symp_constipation,
            canonicalName = "Estreñimiento",
            aliases = setOf("estreñimiento", "estrenimiento", "constipation")
        ),
        OptionMapping(
            resId = R.string.symp_libido_high,
            canonicalName = "Deseo sexual alto",
            aliases = setOf("deseo sexual alto", "high sex drive", "high libido", "deseo alto", "libido alta")
        ),
        OptionMapping(
            resId = R.string.symp_libido_low,
            canonicalName = "Deseo sexual bajo",
            aliases = setOf("deseo sexual bajo", "low sex drive", "low libido", "deseo bajo", "libido baja")
        ),
        OptionMapping(
            resId = R.string.symp_lh_positive,
            canonicalName = "Test ovulación LH (+)",
            aliases = setOf("test ovulación lh (+)", "test ovulacion lh (+)", "lh ovulation test (+)", "test lh (+)", "lh (+)", "lh positive")
        ),
        OptionMapping(
            resId = R.string.symp_lh_negative,
            canonicalName = "Test ovulación LH (-)",
            aliases = setOf("test ovulación lh (-)", "test ovulacion lh (-)", "lh ovulation test (-)", "test lh (-)", "lh (-)", "lh negative")
        )
    )

    // ── MÉTODOS DE BÚSQUEDA Y RESOLUCIÓN ──────────────────────────────────────────

    private fun normalizeKey(raw: String): String =
        raw.trim().lowercase(Locale.ROOT)

    fun findFlowResId(value: String?): Int? {
        if (value.isNullOrBlank()) return null
        val key = normalizeKey(value)
        return FLOW_MAPPINGS.firstOrNull { it.canonicalName.lowercase(Locale.ROOT) == key || it.aliases.contains(key) }?.resId
    }

    fun getCanonicalFlow(resId: Int): String? =
        FLOW_MAPPINGS.firstOrNull { it.resId == resId }?.canonicalName

    fun findMoodResId(value: String?): Int? {
        if (value.isNullOrBlank()) return null
        val key = normalizeKey(value)
        return MOOD_MAPPINGS.firstOrNull { it.canonicalName.lowercase(Locale.ROOT) == key || it.aliases.contains(key) }?.resId
    }

    fun getCanonicalMood(resId: Int): String? =
        MOOD_MAPPINGS.firstOrNull { it.resId == resId }?.canonicalName

    fun findMucusResId(value: String?): Int? {
        if (value.isNullOrBlank()) return null
        val key = normalizeKey(value)
        return MUCUS_MAPPINGS.firstOrNull { it.canonicalName.lowercase(Locale.ROOT) == key || it.aliases.contains(key) }?.resId
    }

    fun getCanonicalMucus(resId: Int): String? =
        MUCUS_MAPPINGS.firstOrNull { it.resId == resId }?.canonicalName

    fun findMethodResId(value: String?): Int? {
        if (value.isNullOrBlank()) return null
        val key = normalizeKey(value)
        return METHOD_MAPPINGS.firstOrNull { it.canonicalName.lowercase(Locale.ROOT) == key || it.aliases.contains(key) }?.resId
    }

    fun getCanonicalMethod(resId: Int): String? =
        METHOD_MAPPINGS.firstOrNull { it.resId == resId }?.canonicalName

    fun findSymptomResId(value: String?): Int? {
        if (value.isNullOrBlank()) return null
        val key = normalizeKey(value)
        return SYMPTOM_MAPPINGS.firstOrNull { it.canonicalName.lowercase(Locale.ROOT) == key || it.aliases.contains(key) }?.resId
    }

    fun getCanonicalSymptom(resId: Int): String? =
        SYMPTOM_MAPPINGS.firstOrNull { it.resId == resId }?.canonicalName

    /**
     * Comprueba si el estado de ánimo coincide con alguno de los recursos objetivo especificados,
     * independiente del idioma en que se haya guardado originalmente.
     */
    fun matchesMood(mood: String?, vararg targetResIds: Int): Boolean {
        val foundResId = findMoodResId(mood) ?: return false
        return targetResIds.contains(foundResId)
    }

    /**
     * Comprueba si la lista de síntomas contiene alguno de los recursos objetivo especificados,
     * independiente del idioma de origen.
     */
    fun hasAnySymptom(symptoms: Collection<String>, vararg targetResIds: Int): Boolean {
        val targetSet = targetResIds.toSet()
        return symptoms.any { s ->
            val resId = findSymptomResId(s)
            resId != null && targetSet.contains(resId)
        }
    }
}
