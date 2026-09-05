package com.jeremy.lumi.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Ambiente decorativo de fondo — eje independiente del tema de color.
 * El usuario elige paleta de color Y ambiente por separado:
 *   - Paleta: qué colores usa la app (Oscuro morado, Rosa claro, NOIR, Azul…)
 *   - Ambiente: qué decoración de fondo se aplica (Plano, Aura, Celeste, Orgánico)
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
    ORGANIC;

    val labelEs: String get() = when (this) {
        FLAT      -> "Plano"
        AURA      -> "Aura"
        CELESTIAL -> "Celeste"
        ORGANIC   -> "Orgánico"
    }

    val descriptionEs: String get() = when (this) {
        FLAT      -> "Sin decoración de fondo"
        AURA      -> "Halo de luz con tu color de fase"
        CELESTIAL -> "Estrellas y luna de hoy"
        ORGANIC   -> "Formas orgánicas suaves"
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
