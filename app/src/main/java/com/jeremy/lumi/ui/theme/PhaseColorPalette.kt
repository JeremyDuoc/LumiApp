package com.jeremy.lumi.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
//  PHASE COLORS — el set de 4 colores activo (menstrual/folicular/ovulación/lútea)
// ─────────────────────────────────────────────────────────────────────────────

data class PhaseColors(
    val menstrual  : Color,
    val follicular : Color,
    val ovulation  : Color,
    val luteal     : Color
)

// Fallback usado por LocalPhaseColors antes de que se resuelva la preferencia real
val PhaseColorsDefaultLight = PhaseColors(
    menstrual  = Color(0xFFE11D48), // Rosa/Coral con contraste accesible
    follicular = Color(0xFF0D9488), // Teal con contraste accesible
    ovulation  = Color(0xFF9333EA), // Morado con contraste accesible
    luteal     = Color(0xFFD97706)  // Ámbar/Dorado cálido accesible (>4.5:1 ratio en fondos claros)
)

// ─────────────────────────────────────────────────────────────────────────────
//  PALETAS PREDEFINIDAS — cada una trae su propia versión light y dark.
//  El criterio de selección no es el tema general de la app sino isDark del
//  tema activo, igual que ya funcionaba PhaseMenstrualLight/Dark antes.
// ─────────────────────────────────────────────────────────────────────────────

enum class PhaseColorPalette {
    AUTO_HARMONY,   // Armonía automática: se adapta armónicamente al tema visual de la app
    DEFAULT,        // La paleta original: rojo coral / ámbar / esmeralda / violeta
    PASTEL,         // Suave, dulce, bajo contraste — para quien prefiere algo delicado
    MINIMALISTA,    // Casi monocromo, diferenciado solo por tono de gris-color
    VIVID,          // Saturado y contrastado, fácil de distinguir de un vistazo
    TIERRA,         // Cálido, terroso, ligado a la paleta Tierra/Lienzo de temas
    OCEANO,         // Fríos, azulados — alternativa a los cálidos por defecto
    MONOCROMO,      // Un solo hue (violeta) en 4 intensidades — máxima calma visual
    OCEANO_GRADIENT,
    BOSQUE_GRADIENT,
    OCASO_GRADIENT,
    DUNA_GRADIENT,
    CUSTOM;         // La usuaria definió los 4 colores manualmente

    val labelEs: String
        get() = when (this) {
            AUTO_HARMONY -> "Armonía con el tema"
            DEFAULT     -> "Original"
            PASTEL      -> "Pastel"
            MINIMALISTA -> "Minimalista"
            VIVID       -> "Vívido"
            TIERRA      -> "Tierra"
            OCEANO      -> "Océano"
            MONOCROMO   -> "Monocromo"
            OCEANO_GRADIENT -> "Lumi Océano"
            BOSQUE_GRADIENT -> "Lumi Bosque"
            OCASO_GRADIENT  -> "Lumi Ocaso"
            DUNA_GRADIENT   -> "Lumi Duna"
            CUSTOM      -> "Personalizado"
        }

    fun toPhaseColors(isDark: Boolean, currentTheme: AppThemePalette = AppThemePalette.LUMI_SPARK): PhaseColors = when (this) {
        AUTO_HARMONY -> when (currentTheme) {

            // ── Familia Violeta / Rosa — MONOCROMO ─────────────────────────────
            // Lumi Spark (el morado/rosa de marca armoniza con paleta violeta)
            AppThemePalette.LUMI_SPARK      -> PhaseMonocromoLight
            AppThemePalette.LUMI_SPARK_DARK -> PhaseMonocromoDark
            // Lavanda y sus variantes
            AppThemePalette.LUMI_LAVENDER, AppThemePalette.LUMI_LAVENDER_DARK ->
                if (isDark) PhaseMonocromoDark else PhaseMonocromoLight
            AppThemePalette.LAVANDA -> PhaseMonocromoLight
            // Oscuros violeta/rosa
            AppThemePalette.MEDIANOCHE -> PhaseMonocromoDark
            AppThemePalette.NOCHE_ROSA -> PhaseMonocromoDark
            AppThemePalette.CIRUELA, AppThemePalette.VINO -> PhaseMonocromoDark
            // Pastel rosa
            AppThemePalette.PETALO -> PhasePastelLight
            AppThemePalette.SAL    -> PhasePastelLight
            // Ciclo menstrual (rosa fuerte → colores de fase más cálidos)
            AppThemePalette.CYCLE_MENSTRUAL      -> PhaseDefaultLight
            AppThemePalette.CYCLE_MENSTRUAL_DARK -> PhaseDefaultDark

            // ── Familia Océano / Azul / Frío — OCEANO ─────────────────────────
            AppThemePalette.INDIGO, AppThemePalette.LUMI_OCEAN, AppThemePalette.LUMI_OCEAN_DARK ->
                if (isDark) PhaseOceanoGradientDark else PhaseOceanoGradientLight
            AppThemePalette.LUMI_SLATE, AppThemePalette.LUMI_SLATE_DARK ->
                if (isDark) PhaseOceanoDark else PhaseOceanoLight
            AppThemePalette.COSMOS -> PhaseOceanoGradientDark
            AppThemePalette.FORJA  -> PhaseOceanoDark
            AppThemePalette.NIEBLA -> PhaseOceanoLight
            // Ciclo ovulación (azul/teal → familia océano)
            AppThemePalette.CYCLE_OVULATION      -> PhaseOceanoGradientLight
            AppThemePalette.CYCLE_OVULATION_DARK -> PhaseOceanoGradientDark

            // ── Familia Verde / Naturaleza — BOSQUE ───────────────────────────
            AppThemePalette.BOSQUE, AppThemePalette.LUMI_FOREST, AppThemePalette.LUMI_FOREST_DARK ->
                if (isDark) PhaseBosqueGradientDark else PhaseBosqueGradientLight
            AppThemePalette.SALVIA, AppThemePalette.LUMI_SAGE, AppThemePalette.LUMI_SAGE_DARK ->
                if (isDark) PhaseBosqueGradientDark else PhaseBosqueGradientLight
            AppThemePalette.CACTUS    -> PhaseBosqueGradientLight
            AppThemePalette.OBSIDIANA -> PhaseBosqueGradientDark  // Teal profundo

            // ── Familia Cálida / Atardecer — OCASO ────────────────────────────
            AppThemePalette.LUMI_SUNSET, AppThemePalette.LUMI_SUNSET_DARK ->
                if (isDark) PhaseOcasoGradientDark else PhaseOcasoGradientLight
            AppThemePalette.HORTENSIA -> PhaseOcasoGradientLight  // Rosa vibrante / rojo
            AppThemePalette.ECLIPSE   -> PhaseOcasoGradientDark   // Burdeos oscuro
            AppThemePalette.CARBONO   -> PhaseOcasoGradientDark   // Acento rojo sobre negro

            // ── Familia Tierra / Cálida — TIERRA ──────────────────────────────
            AppThemePalette.LUMI_DUNE, AppThemePalette.LUMI_DUNE_DARK ->
                if (isDark) PhaseDunaGradientDark else PhaseDunaGradientLight
            AppThemePalette.LUMI_EARTH, AppThemePalette.LUMI_EARTH_DARK ->
                if (isDark) PhaseTierraDark else PhaseTierraLight
            AppThemePalette.TIERRA -> PhaseTierraLight
            AppThemePalette.LUXE   -> PhaseTierraLight  // Burdeos + beige cálido
            AppThemePalette.LIENZO -> PhaseTierraLight  // Beige artístico
            AppThemePalette.MARFIL, AppThemePalette.ARENA  -> PhaseTierraLight  // Arena neutra cálida
            AppThemePalette.AMBAR  -> PhaseTierraDark   // Ámbar oscuro
            AppThemePalette.HUMO   -> PhaseTierraLight  // Gris cálido

            // ── Familia Neutra / Minimalista — MINIMALISTA ────────────────────
            AppThemePalette.PAPEL   -> PhaseMinimalistaLight
            AppThemePalette.PIZARRA -> PhaseMinimalistaLight
            AppThemePalette.PIEDRA  -> PhaseMinimalistaLight
            AppThemePalette.CENIZA  -> PhaseMinimalistaLight
            AppThemePalette.NOIR    -> PhaseMinimalistaDark

            // Fallback: cualquier tema sin asignación explícita
            else -> if (isDark) PhaseDefaultDark else PhaseDefaultLight
        }
        DEFAULT     -> if (isDark) PhaseDefaultDark     else PhaseDefaultLight
        PASTEL      -> if (isDark) PhasePastelDark      else PhasePastelLight
        MINIMALISTA -> if (isDark) PhaseMinimalistaDark else PhaseMinimalistaLight
        VIVID       -> if (isDark) PhaseVividDark       else PhaseVividLight
        TIERRA      -> if (isDark) PhaseTierraDark      else PhaseTierraLight
        OCEANO      -> if (isDark) PhaseOceanoDark      else PhaseOceanoLight
        MONOCROMO   -> if (isDark) PhaseMonocromoDark   else PhaseMonocromoLight
        OCEANO_GRADIENT -> if (isDark) PhaseOceanoGradientDark else PhaseOceanoGradientLight
        BOSQUE_GRADIENT -> if (isDark) PhaseBosqueGradientDark else PhaseBosqueGradientLight
        OCASO_GRADIENT  -> if (isDark) PhaseOcasoGradientDark else PhaseOcasoGradientLight
        DUNA_GRADIENT   -> if (isDark) PhaseDunaGradientDark else PhaseDunaGradientLight
        // CUSTOM nunca debería resolverse aquí — LumiTheme intercepta antes y usa
        // los colores guardados individualmente. Este caso es solo un fallback seguro.
        CUSTOM      -> if (isDark) PhaseDefaultDark     else PhaseDefaultLight
    }

    /**
     * Devuelve los colores semánticos de fase — SIEMPRE la paleta DEFAULT,
     * independientemente del tema de la app.
     * Usar para: etiquetas de fase, chip de fase en HomeScreen, color del
     * anillo de progreso. NO para indicadores de calendario (esos sí pueden
     * armonizar con el tema).
     */
    fun toSemanticPhaseColors(isDark: Boolean): PhaseColors =
        if (isDark) PhaseDefaultDark else PhaseDefaultLight
}

// ── DEFAULT · la paleta original de Lumi ──────────────────────────────────────
val PhaseDefaultLight = PhaseColors(
    menstrual  = Color(0xFFE11D48), // Rosa/Coral con contraste accesible
    follicular = Color(0xFF0D9488), // Teal con contraste accesible
    ovulation  = Color(0xFF9333EA), // Morado con contraste accesible
    luteal     = Color(0xFFD97706)  // Ámbar/Dorado cálido accesible (>4.5:1 ratio en fondos claros)
)
val PhaseDefaultDark = PhaseColors(
    menstrual  = Color(0xFFFB7185), // Rosa coral brillante para dark
    follicular = Color(0xFF2DD4BF), // Teal vibrante para dark
    ovulation  = Color(0xFFC084FC), // Morado brillante para dark
    luteal     = Color(0xFFFBBF24)  // Dorado cálido brillante para dark
)

// ── PASTEL · dulce, bajo contraste, ideal para quien quiere algo suave ────────
val PhasePastelLight = PhaseColors(
    menstrual  = Color(0xFFF0A8B4),
    follicular = Color(0xFFF0CE94),
    ovulation  = Color(0xFFA0D4B8),
    luteal     = Color(0xFFBCB0E8)
)
val PhasePastelDark = PhaseColors(
    menstrual  = Color(0xFFF5C2CA),
    follicular = Color(0xFFF5DEB0),
    ovulation  = Color(0xFFBCE4CE),
    luteal     = Color(0xFFD2C8F0)
)

// ── MINIMALISTA · casi monocromo, distinguible por matiz tenue de gris ────────
val PhaseMinimalistaLight = PhaseColors(
    menstrual  = Color(0xFF8A6868),
    follicular = Color(0xFF8A7E68),
    ovulation  = Color(0xFF688A78),
    luteal     = Color(0xFF70688A)
)
val PhaseMinimalistaDark = PhaseColors(
    menstrual  = Color(0xFFC4A0A0),
    follicular = Color(0xFFC4B49C),
    ovulation  = Color(0xFFA0C4B0),
    luteal     = Color(0xFFAEA0C4)
)

// ── VIVID · saturado, distinción inmediata a un vistazo rápido ────────────────
val PhaseVividLight = PhaseColors(
    menstrual  = Color(0xFFE8334F),
    follicular = Color(0xFFF59B00),
    ovulation  = Color(0xFF00A86B),
    luteal     = Color(0xFF6C3CE0)
)
val PhaseVividDark = PhaseColors(
    menstrual  = Color(0xFFFF6B82),
    follicular = Color(0xFFFFB13D),
    ovulation  = Color(0xFF38E0A0),
    luteal     = Color(0xFFA888FF)
)

// ── TIERRA · cálido, terroso — coherente con temas Tierra/Lienzo/Ámbar ────────
val PhaseTierraLight = PhaseColors(
    menstrual  = Color(0xFFB05A4A),
    follicular = Color(0xFFC49048),
    ovulation  = Color(0xFF7E9468),
    luteal     = Color(0xFF8E7090)
)
val PhaseTierraDark = PhaseColors(
    menstrual  = Color(0xFFD68C7C),
    follicular = Color(0xFFE0BA7C),
    ovulation  = Color(0xFFAEC096),
    luteal     = Color(0xFFBC9CBE)
)

// ── OCEANO · fríos y azulados, alternativa a la paleta cálida estándar ────────
val PhaseOceanoLight = PhaseColors(
    menstrual  = Color(0xFF3D6FA8),
    follicular = Color(0xFF45A0A0),
    ovulation  = Color(0xFF3D9E8E),
    luteal     = Color(0xFF5B5EA8)
)
val PhaseOceanoDark = PhaseColors(
    menstrual  = Color(0xFF7EA8E0),
    follicular = Color(0xFF7CD0D0),
    ovulation  = Color(0xFF6EDCC4),
    luteal     = Color(0xFF9C9EE0)
)

// ── MONOCROMO · un solo hue (violeta) en 4 intensidades — máxima calma ────────
val PhaseMonocromoLight = PhaseColors(
    menstrual  = Color(0xFF6B4D8C),
    follicular = Color(0xFF9176AC),
    ovulation  = Color(0xFFB39ECC),
    luteal     = Color(0xFF4A3568)
)
val PhaseMonocromoDark = PhaseColors(
    menstrual  = Color(0xFFC4A8E0),
    follicular = Color(0xFFA888CC),
    ovulation  = Color(0xFF8C68B0),
    luteal     = Color(0xFFE0CCF0)
)

// ── LUMI OCEANO GRADIENT ─────────────────────────────────────────────────────
val PhaseOceanoGradientLight = PhaseColors(
    menstrual  = Color(0xFF1565C0), // Azul índigo
    follicular = Color(0xFF42A5F5), // Azul medio
    ovulation  = Color(0xFF00D2FF), // Celeste brillante
    luteal     = Color(0xFF7E57C2)  // Púrpura oceánico
)
val PhaseOceanoGradientDark = PhaseColors(
    menstrual  = Color(0xFF6BA2EC), 
    follicular = Color(0xFF64B5F6),
    ovulation  = Color(0xFF4DD8FF), 
    luteal     = Color(0xFF9575CD)
)

// ── LUMI BOSQUE GRADIENT ─────────────────────────────────────────────────────
val PhaseBosqueGradientLight = PhaseColors(
    menstrual  = Color(0xFF0B6623), // Verde pino oscuro
    follicular = Color(0xFF66BB6A), // Verde hoja
    ovulation  = Color(0xFF2EFEA5), // Menta vibrante
    luteal     = Color(0xFF00897B)  // Teal / Verde azulado
)
val PhaseBosqueGradientDark = PhaseColors(
    menstrual  = Color(0xFF3DB78A),
    follicular = Color(0xFF81C784),
    ovulation  = Color(0xFF8AF3B5),
    luteal     = Color(0xFF4DB6AC)
)

// ── LUMI OCASO GRADIENT ──────────────────────────────────────────────────────
val PhaseOcasoGradientLight = PhaseColors(
    menstrual  = Color(0xFFD84315), // Terracota
    follicular = Color(0xFFFF7043), // Naranja atardecer
    ovulation  = Color(0xFFFFB300), // Dorado
    luteal     = Color(0xFFEC407A)  // Rosa atardecer
)
val PhaseOcasoGradientDark = PhaseColors(
    menstrual  = Color(0xFFFA6A5A),
    follicular = Color(0xFFFF8A65),
    ovulation  = Color(0xFFF7C34D),
    luteal     = Color(0xFFF06292)
)

// ── LUMI DUNA GRADIENT ───────────────────────────────────────────────────────
val PhaseDunaGradientLight = PhaseColors(
    menstrual  = Color(0xFF5D4037), // Marrón chocolate
    follicular = Color(0xFF8D6E63), // Arena oscuro
    ovulation  = Color(0xFFD4AF37), // Dorado
    luteal     = Color(0xFFBCAAA4)  // Arena claro
)
val PhaseDunaGradientDark = PhaseColors(
    menstrual  = Color(0xFFA1887F),
    follicular = Color(0xFFBCAAA4),
    ovulation  = Color(0xFFFFE082),
    luteal     = Color(0xFFD7CCC8)
)

// ─────────────────────────────────────────────────────────────────────────────
//  SWATCHES PARA SELECCIÓN MANUAL POR FASE
//  Conjunto curado de 16 colores que cubre toda la rueda — usado en el grid
//  rápido del selector individual antes de abrir la rueda HSV libre.
// ─────────────────────────────────────────────────────────────────────────────

val PhaseColorSwatches = listOf(
    Color(0xFFE0566B), Color(0xFFE0A33C), Color(0xFF3D9E72), Color(0xFF6E63C4),
    Color(0xFFF0A8B4), Color(0xFFF0CE94), Color(0xFFA0D4B8), Color(0xFFBCB0E8),
    Color(0xFFE8334F), Color(0xFFF59B00), Color(0xFF00A86B), Color(0xFF6C3CE0),
    Color(0xFF3D6FA8), Color(0xFF45A0A0), Color(0xFFB05A4A), Color(0xFF6B4D8C)
)