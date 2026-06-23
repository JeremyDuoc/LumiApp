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
    menstrual  = Color(0xFFE0566B),
    follicular = Color(0xFFE0A33C),
    ovulation  = Color(0xFF3D9E72),
    luteal     = Color(0xFF6E63C4)
)

// ─────────────────────────────────────────────────────────────────────────────
//  PALETAS PREDEFINIDAS — cada una trae su propia versión light y dark.
//  El criterio de selección no es el tema general de la app sino isDark del
//  tema activo, igual que ya funcionaba PhaseMenstrualLight/Dark antes.
// ─────────────────────────────────────────────────────────────────────────────

enum class PhaseColorPalette {
    DEFAULT,        // La paleta original: rojo coral / ámbar / esmeralda / violeta
    PASTEL,         // Suave, dulce, bajo contraste — para quien prefiere algo delicado
    MINIMALISTA,    // Casi monocromo, diferenciado solo por tono de gris-color
    VIVID,          // Saturado y contrastado, fácil de distinguir de un vistazo
    TIERRA,         // Cálido, terroso, ligado a la paleta Tierra/Lienzo de temas
    OCEANO,         // Fríos, azulados — alternativa a los cálidos por defecto
    MONOCROMO,      // Un solo hue (violeta) en 4 intensidades — máxima calma visual
    CUSTOM;         // La usuaria definió los 4 colores manualmente

    val labelEs: String
        get() = when (this) {
            DEFAULT     -> "Original"
            PASTEL      -> "Pastel"
            MINIMALISTA -> "Minimalista"
            VIVID       -> "Vívido"
            TIERRA      -> "Tierra"
            OCEANO      -> "Océano"
            MONOCROMO   -> "Monocromo"
            CUSTOM      -> "Personalizado"
        }

    fun toPhaseColors(isDark: Boolean): PhaseColors = when (this) {
        DEFAULT     -> if (isDark) PhaseDefaultDark     else PhaseDefaultLight
        PASTEL      -> if (isDark) PhasePastelDark      else PhasePastelLight
        MINIMALISTA -> if (isDark) PhaseMinimalistaDark else PhaseMinimalistaLight
        VIVID       -> if (isDark) PhaseVividDark       else PhaseVividLight
        TIERRA      -> if (isDark) PhaseTierraDark      else PhaseTierraLight
        OCEANO      -> if (isDark) PhaseOceanoDark      else PhaseOceanoLight
        MONOCROMO   -> if (isDark) PhaseMonocromoDark   else PhaseMonocromoLight
        // CUSTOM nunca debería resolverse aquí — LumiTheme intercepta antes y usa
        // los colores guardados individualmente. Este caso es solo un fallback seguro.
        CUSTOM      -> if (isDark) PhaseDefaultDark     else PhaseDefaultLight
    }
}

// ── DEFAULT · la paleta original de Lumi ──────────────────────────────────────
val PhaseDefaultLight = PhaseColors(
    menstrual  = Color(0xFFE0566B),
    follicular = Color(0xFFE0A33C),
    ovulation  = Color(0xFF3D9E72),
    luteal     = Color(0xFF6E63C4)
)
val PhaseDefaultDark = PhaseColors(
    menstrual  = Color(0xFFF07E8F),
    follicular = Color(0xFFF0BD6A),
    ovulation  = Color(0xFF5FC796),
    luteal     = Color(0xFF9C92E0)
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