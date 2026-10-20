package com.jeremy.lumi.domain.usecase

import java.text.Normalizer
import java.util.Locale

/**
 * Offline, deterministic intent routing for free-form chat.
 * Safety-critical intents are evaluated before routine symptom questions.
 * No medical diagnosis or remote API is performed here.
 */
internal enum class ChatIntent {
    URGENT_SYMPTOMS, EMERGENCY_CONTRACEPTION, PREGNANCY_RISK,
    PERSONAL_PATTERNS, LATE_PERIOD, NEXT_PERIOD, CURRENT_PHASE,
    FERTILITY, CONTRACEPTION, CRAMPS, HEADACHE, DIGESTION,
    SPOTTING, MOOD, SLEEP, STRESS, ENERGY, NUTRITION,
    EXERCISE, GREETING, UNKNOWN
}

internal object ChatIntentRouter {
    fun classify(query: String): ChatIntent {
        val text = normalize(query)
        if (text.isBlank()) return ChatIntent.UNKNOWN
        fun has(vararg patterns: String) = patterns.any { Regex(pattern = patternsAsRegex(it)).containsMatchIn(text) }

        // Explicit red flags must take precedence over ordinary cramps or cycle content.
        if (has("me desmaye", "me estoy desmayando", "perdi el conocimiento",
                "sangrado incontrolable", "empapo una toalla por hora",
                "dolor pelvico insoportable", "dolor abdominal insoportable",
                "dolor muy fuerte y repentino", "dolor de cabeza repentino e intenso",
                "dificultad para respirar", "no puedo respirar", "dolor en el pecho")) return ChatIntent.URGENT_SYMPTOMS

        if (has("sin proteccion", "sin preservativo", "sin condon",
                "se rompio el condon", "se rompio el preservativo",
                "no usamos condon", "no usamos preservativo", "se salio el condon",
                "anticoncepcion de emergencia", "anticonceptivo de emergencia",
                "pastilla del dia despues", "postday", "ulipristal",
                "levonorgestrel")) return ChatIntent.EMERGENCY_CONTRACEPTION

        if (has("puedo estar embarazada", "riesgo de embarazo", "prueba de embarazo",
                "test de embarazo", "estoy embarazada")) return ChatIntent.PREGNANCY_RISK

        if (has("patron", "patrones", "correlacion", "correlaciones", "descubrimiento",
                "mis datos", "mis registros", "estadisticas", "se relaciona con",
                "relacion entre", "sintomas recurrentes")) return ChatIntent.PERSONAL_PATTERNS

        if (has("retraso", "atraso", "retrasada", "atrasada", "no me baja", "no me viene",
                "no me llega", "periodo tarde", "regla tarde")) return ChatIntent.LATE_PERIOD

        if (has("cuando llega mi", "cuando viene mi", "proximo periodo",
                "siguiente periodo", "proxima menstruacion",
                "cuando me baja", "cuando me viene")) return ChatIntent.NEXT_PERIOD

        if (has("en que fase", "que fase", "fase del ciclo", "dia del ciclo",
                "en que etapa")) return ChatIntent.CURRENT_PHASE

        if (has("fertil", "fertiles", "ovulacion", "ovulando",
                "quedar embarazada", "ventana fertil")) return ChatIntent.FERTILITY

        if (has("anticonceptivo", "anticonceptivos", "pastilla anticonceptiva",
                "metodo anticonceptivo", "implante", "diu")) return ChatIntent.CONTRACEPTION

        if (has("colico", "colicos", "vientre", "ovarios", "dolor pelvico",
                "calambres", "dismenorrea")) return ChatIntent.CRAMPS

        if (has("migrana", "jaqueca", "cefalea", "dolor de cabeza")) return ChatIntent.HEADACHE
        if (has("nausea", "vomito", "hinchazon", "inflamada", "gases")) return ChatIntent.DIGESTION
        if (has("flujo vaginal", "flujo", "manchado", "spotting", "moco cervical")) return ChatIntent.SPOTTING
        if (has("animo", "humor", "irritable", "enojada", "llorar",
                "ansiedad", "triste", "spm", "tdpm")) return ChatIntent.MOOD
        if (has("sueno", "insomnio", "dormir", "cansada", "agotada")) return ChatIntent.SLEEP
        if (has("estres", "estresada", "tension emocional")) return ChatIntent.STRESS
        if (has("energia", "fatiga", "agotamiento")) return ChatIntent.ENERGY
        if (has("comer", "comida", "dieta", "alimentacion", "nutricion", "alimentos")) return ChatIntent.NUTRITION
        if (has("ejercicio", "entrenar", "gimnasio", "gym", "pesas", "cardio", "deporte")) return ChatIntent.EXERCISE
        if (has("hola", "buenos dias", "buenas tardes", "buenas noches",
                "que tal", "lumi")) return ChatIntent.GREETING
        return ChatIntent.UNKNOWN
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun patternsAsRegex(phrase: String): String =
        "\\b" + Regex.escape(phrase).replace(" ", "\\E\\s+\\Q") + "\\b"
}
