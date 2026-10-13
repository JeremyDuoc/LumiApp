package com.jeremy.lumi.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Ambiente decorativo de fondo — eje independiente del tema de color.
 * El usuario elige paleta de color Y ambiente por separado:
 *   - Paleta: qué colores usa la app (Oscuro morado, Rosa claro, NOIR, Azul…)
 *   - Ambiente: qué decoración de fondo se aplica (Plano, Aura, Celeste, Orgánico, Aurora, Pétalos, Nebulosa, Aliento)
 */
enum class BackgroundAmbient {

    /** Sin decoración — fondo sólido del tema. Máxima legibilidad y accesibilidad. */
    FLAT,

    /**
     * Halo radial suave usando el color de la fase menstrual actual.
     * Se adapta automáticamente al ciclo — default recomendado para usuarios nuevos.
     */
    AURA,

    /**
     * Estrellas dispersas + luna en fase astronómica real del día.
     * Mejor con temas oscuros. En temas claros, degrada a elementos sutiles.
     */
    CELESTIAL,

    /**
     * Blobs orgánicos suaves usando los 4 colores de fase.
     * Estilo wellness (Calm / Headspace). Opacidad se auto-reduce en pantallas
     * con contenido denso para garantizar legibilidad.
     */
    ORGANIC,

    /**
     * Ondas horizontales suaves estilo aurora boreal (verde/cian/violeta).
     * Fluyen lentamente de lado a lado con parallax. Mejor con temas oscuros;
     * en temas claros usa tonos pastel tenues.
     */
    AURORA,

    /**
     * Círculos translúcidos tipo pétalos que flotan suavemente hacia arriba.
     * Rosa/melocotón en claro · Lavanda/rosa en oscuro.
     * Muy bajo consumo de GPU — solo 6 partículas estáticas con deriva animada.
     */
    PETALS,

    /**
     * Manchas de niebla cósmica superpuestas — azul/cian/magenta profundo.
     * Diseñado para temas oscuros; en temas claros usa los colores de fase
     * con opacidad extra reducida para mantener la legibilidad.
     */
    NEBULA,

    /**
     * Un único gradiente radial central que "respira" en ciclos de ~4 s.
     * Ultra-minimal: un pulso, un color, máxima calma.
     */
    BREATH;

    val labelRes: Int get() = when (this) {
        FLAT      -> com.jeremy.lumi.R.string.ambient_flat_label
        AURA      -> com.jeremy.lumi.R.string.ambient_aura_label
        CELESTIAL -> com.jeremy.lumi.R.string.ambient_celestial_label
        ORGANIC   -> com.jeremy.lumi.R.string.ambient_organic_label
        AURORA    -> com.jeremy.lumi.R.string.ambient_aurora_label
        PETALS    -> com.jeremy.lumi.R.string.ambient_petals_label
        NEBULA    -> com.jeremy.lumi.R.string.ambient_nebula_label
        BREATH    -> com.jeremy.lumi.R.string.ambient_breath_label
    }

    val descriptionRes: Int get() = when (this) {
        FLAT      -> com.jeremy.lumi.R.string.ambient_flat_desc
        AURA      -> com.jeremy.lumi.R.string.ambient_aura_desc
        CELESTIAL -> com.jeremy.lumi.R.string.ambient_celestial_desc
        ORGANIC   -> com.jeremy.lumi.R.string.ambient_organic_desc
        AURORA    -> com.jeremy.lumi.R.string.ambient_aurora_desc
        PETALS    -> com.jeremy.lumi.R.string.ambient_petals_desc
        NEBULA    -> com.jeremy.lumi.R.string.ambient_nebula_desc
        BREATH    -> com.jeremy.lumi.R.string.ambient_breath_desc
    }

    val labelEs: String get() = when (this) {
        FLAT      -> "Plano"
        AURA      -> "Aura"
        CELESTIAL -> "Celeste"
        ORGANIC   -> "Orgánico"
        AURORA    -> "Aurora"
        PETALS    -> "Pétalos"
        NEBULA    -> "Nebulosa"
        BREATH    -> "Aliento"
    }

    val descriptionEs: String get() = when (this) {
        FLAT      -> "Sin decoración de fondo"
        AURA      -> "Halo de luz con tu color de fase"
        CELESTIAL -> "Estrellas y luna de hoy"
        ORGANIC   -> "Formas orgánicas suaves"
        AURORA    -> "Ondas de aurora boreal"
        PETALS    -> "Pétalos flotando suavemente"
        NEBULA    -> "Niebla cósmica profunda"
        BREATH    -> "Un pulso suave, ultra-minimal"
    }
}

/**
 * Tipo de pantalla — controla cuánta decoración se muestra.
 * Cada pantalla declara su densidad de contenido al llamar [AmbientBackgroundLayer].
 */
enum class AmbientScreen {
    /** Pantalla principal: efectos completos, opacidad máxima. */
    HOME,
    /** Contenido denso (estadísticas, calendario): efectos reducidos al 50%. */
    CONTENT,
    /** Chat / lectura continua: solo fondo sólido, sin decoración. */
    CHAT
}

/**
 * CompositionLocal que expone el ambiente activo a cualquier composable.
 * Se alimenta desde [LumiTheme] con el valor guardado en DataStore.
 */
val LocalBackgroundAmbient = compositionLocalOf { BackgroundAmbient.AURA }
