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
     * Estrellas dispersas + luna en fase astronómica real del día.
     * Mejor con temas oscuros. En temas claros, degrada a elementos sutiles.
     */
    CELESTIAL;

    val labelRes: Int get() = when (this) {
        FLAT      -> com.jeremy.lumi.R.string.ambient_flat_label
        CELESTIAL -> com.jeremy.lumi.R.string.ambient_celestial_label
    }

    val descriptionRes: Int get() = when (this) {
        FLAT      -> com.jeremy.lumi.R.string.ambient_flat_desc
        CELESTIAL -> com.jeremy.lumi.R.string.ambient_celestial_desc
    }

    val labelEs: String get() = when (this) {
        FLAT      -> "Plano"
        CELESTIAL -> "Celeste"
    }

    val descriptionEs: String get() = when (this) {
        FLAT      -> "Sin decoración de fondo"
        CELESTIAL -> "Estrellas y luna de hoy"
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
val LocalBackgroundAmbient = compositionLocalOf { BackgroundAmbient.FLAT }

