package com.jeremy.lumi.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Tokens de diseño semánticos unificados para Lumi.
 *
 * Fuente de verdad visual: `lumi_redesign_interactivo.html`
 * Permite que todas las pantallas (Home, Calendario, Ritmos, Bonds, Ajustes)
 * compartan la misma gramática visual, bordes finos, superficies y contrastes.
 */
data class LumiDesignTokens(
    val bg: Color,
    val panel: Color,
    val panel2: Color,
    val text: Color,
    val muted: Color,
    val dim: Color,
    val line: Color,
    val accent: Color,
    val accentSoft: Color,
    val peach: Color,
    val rose: Color,
    val mint: Color,
    val gold: Color,
    val nav: Color,
    val glow: Color,
    val field: Color,
    val pill: Color,
    val ringTrack: Color,
    val ringGradient: Brush,
    val isDark: Boolean
)

// ── Tokens aprobados: NOIR (Atmósfera oscura por defecto) ───────────────────
val NoirTokens = LumiDesignTokens(
    bg = Color(0xFF0E0C13),
    panel = Color(0xFF1D1924),
    panel2 = Color(0xFF27222E),
    text = Color(0xFFFAF8FB),
    muted = Color(0xFFAAA3B5),
    dim = Color(0xFF858095),
    line = Color(0xFF37313E),
    accent = Color(0xFFCBB4F1),
    accentSoft = Color(0x24CBB4F1),
    peach = Color(0xFFF6BAAB),
    rose = Color(0xFFE6AFCA),
    mint = Color(0xFF9CCDB5),
    gold = Color(0xFFF0D3A4),
    nav = Color(0xFF131119),
    glow = Color(0x35C58BE0),
    field = Color(0xFF27222E),
    pill = Color(0xFF27222E),
    ringTrack = Color(0xFF2B2832),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFFCBB4F1), Color(0xFFE6AFCA), Color(0xFFF6BAAB))
    ),
    isDark = true
)

// ── CIRUELA (Atmósfera oscura profunda y cálida) ─────────────────────────────
val CiruelaTokens = LumiDesignTokens(
    bg = Color(0xFF141017),
    panel = Color(0xFF241D29),
    panel2 = Color(0xFF33283A),
    text = Color(0xFFFFF9FC),
    muted = Color(0xFFC2B1C2),
    dim = Color(0xFF9F8A9E),
    line = Color(0xFF453646),
    accent = Color(0xFFDBB5EC),
    accentSoft = Color(0x24DBB5EC),
    peach = Color(0xFFF4C4B7),
    rose = Color(0xFFE8ADC8),
    mint = Color(0xFFA8D1C9),
    gold = Color(0xFFE6CCA8),
    nav = Color(0xFF141017),
    glow = Color(0x30E699C8),
    field = Color(0xFF33283A),
    pill = Color(0xFF33283A),
    ringTrack = Color(0xFF352B3B),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFFDBB5EC), Color(0xFFE8ADC8), Color(0xFFEFB6D1))
    ),
    isDark = true
)

// ── BOSQUE (Atmósfera oscura naturaleza esmeralda) ───────────────────────────
val BosqueTokens = LumiDesignTokens(
    bg = Color(0xFF0C1413),
    panel = Color(0xFF17231F),
    panel2 = Color(0xFF24362E),
    text = Color(0xFFF1FAF5),
    muted = Color(0xFFB1C4B8),
    dim = Color(0xFF83998D),
    line = Color(0xFF35493F),
    accent = Color(0xFFA3D4BD),
    accentSoft = Color(0x24A3D4BD),
    peach = Color(0xFFDFCCA8),
    rose = Color(0xFFB7D4BD),
    mint = Color(0xFFB2DFC6),
    gold = Color(0xFFD2DCAE),
    nav = Color(0xFF0C1413),
    glow = Color(0x2986CEAC),
    field = Color(0xFF24362E),
    pill = Color(0xFF24362E),
    ringTrack = Color(0xFF22352D),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFFA3D4BD), Color(0xFFB2DFC6), Color(0xFFD2DCAE))
    ),
    isDark = true
)

// ── ÍNDIGO (Atmósfera oscura cósmica noche) ──────────────────────────────────
val IndigoTokens = LumiDesignTokens(
    bg = Color(0xFF0D1122),
    panel = Color(0xFF1A2037),
    panel2 = Color(0xFF29304D),
    text = Color(0xFFF6F7FF),
    muted = Color(0xFFB5BBD8),
    dim = Color(0xFF8F99B9),
    line = Color(0xFF3B4263),
    accent = Color(0xFFC0C9FF),
    accentSoft = Color(0x24C0C9FF),
    peach = Color(0xFFE2C4DC),
    rose = Color(0xFFC7AFDF),
    mint = Color(0xFFB2C4FA),
    gold = Color(0xFFE5D5BA),
    nav = Color(0xFF0D1122),
    glow = Color(0x2B97A6FF),
    field = Color(0xFF29304D),
    pill = Color(0xFF29304D),
    ringTrack = Color(0xFF262E49),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFFC0C9FF), Color(0xFFC7AFDF), Color(0xFFB2C4FA))
    ),
    isDark = true
)

// ── PÉTALO (Atmósfera clara suave floral) ────────────────────────────────────
val PetaloTokens = LumiDesignTokens(
    bg = Color(0xFFFCF7F7),
    panel = Color(0xFFFFFAFB),
    panel2 = Color(0xFFF3E9ED),
    text = Color(0xFF302838),
    muted = Color(0xFF786D7F),
    dim = Color(0xFF93899A),
    line = Color(0xFFEBDFE7),
    accent = Color(0xFF9369B2),
    accentSoft = Color(0x209369B2),
    peach = Color(0xFFE59CA8),
    rose = Color(0xFFCE849F),
    mint = Color(0xFF7CAEA0),
    gold = Color(0xFFB5935A),
    nav = Color(0xFFFFFAFB),
    glow = Color(0x29E18FB5),
    field = Color(0xFFF3E9ED),
    pill = Color(0xFFF3E9ED),
    ringTrack = Color(0xFFEFE4EA),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFF9369B2), Color(0xFFCE849F), Color(0xFFEAB0CE))
    ),
    isDark = false
)

// ── MARFIL (Atmósfera clara neutra cálida lino) ──────────────────────────────
val MarfilTokens = LumiDesignTokens(
    bg = Color(0xFFFAF6EE),
    panel = Color(0xFFFFFCF7),
    panel2 = Color(0xFFF3ECE3),
    text = Color(0xFF352F32),
    muted = Color(0xFF756A69),
    dim = Color(0xFF948986),
    line = Color(0xFFE7DCD4),
    accent = Color(0xFF90718F),
    accentSoft = Color(0x2090718F),
    peach = Color(0xFFCFA094),
    rose = Color(0xFFC99193),
    mint = Color(0xFF86A59A),
    gold = Color(0xFFA6855B),
    nav = Color(0xFFFFFCF7),
    glow = Color(0x21CE9B9E),
    field = Color(0xFFF3ECE3),
    pill = Color(0xFFF3ECE3),
    ringTrack = Color(0xFFECE4D9),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFF90718F), Color(0xFFC99193), Color(0xFFF0C2B4))
    ),
    isDark = false
)

// ── SALVIA (Atmósfera clara botánica menta calma) ────────────────────────────
val SalviaTokens = LumiDesignTokens(
    bg = Color(0xFFF2F7F2),
    panel = Color(0xFFFCFEFD),
    panel2 = Color(0xFFE6EEE7),
    text = Color(0xFF253B30),
    muted = Color(0xFF65786B),
    dim = Color(0xFF839B8B),
    line = Color(0xFFD3E3D9),
    accent = Color(0xFF5C8E76),
    accentSoft = Color(0x205C8E76),
    peach = Color(0xFFBFA08E),
    rose = Color(0xFF82AA94),
    mint = Color(0xFF67A788),
    gold = Color(0xFF948F5B),
    nav = Color(0xFFFCFEFD),
    glow = Color(0x2684B194),
    field = Color(0xFFE6EEE7),
    pill = Color(0xFFE6EEE7),
    ringTrack = Color(0xFFDFEAE1),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFF5C8E76), Color(0xFF82AA94), Color(0xFFBCE4C8))
    ),
    isDark = false
)

// ── LAVANDA (Atmósfera clara bruma lila editorial) ───────────────────────────
val LavandaTokens = LumiDesignTokens(
    bg = Color(0xFFF8F5FE),
    panel = Color(0xFFFFFCFF),
    panel2 = Color(0xFFEEE9F8),
    text = Color(0xFF342B49),
    muted = Color(0xFF756B88),
    dim = Color(0xFF998EAD),
    line = Color(0xFFE3DCEF),
    accent = Color(0xFF8F70BD),
    accentSoft = Color(0x208F70BD),
    peach = Color(0xFFCFAABF),
    rose = Color(0xFFBD8FC3),
    mint = Color(0xFF89B3A7),
    gold = Color(0xFFA68DB5),
    nav = Color(0xFFFFFCFF),
    glow = Color(0x26AD8EE2),
    field = Color(0xFFEEE9F8),
    pill = Color(0xFFEEE9F8),
    ringTrack = Color(0xFFE8E1F3),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFF8F70BD), Color(0xFFBD8FC3), Color(0xFFC3B8EE))
    ),
    isDark = false
)

// Aliases para compatibilidad previa
val BlushTokens = PetaloTokens
val SageTokens = BosqueTokens

/**
 * Función adaptadora para garantizar que CUALQUIERA de las paletas existentes
 * en Lumi se mapee limpiamente a los tokens semánticos de diseño.
 */
fun resolveLumiTokens(palette: AppThemePalette): LumiDesignTokens {
    return when (palette) {
        AppThemePalette.NOIR -> NoirTokens
        AppThemePalette.CIRUELA, AppThemePalette.VINO -> CiruelaTokens
        AppThemePalette.BOSQUE, AppThemePalette.LUMI_FOREST, AppThemePalette.LUMI_FOREST_DARK -> BosqueTokens
        AppThemePalette.INDIGO, AppThemePalette.LUMI_OCEAN, AppThemePalette.LUMI_OCEAN_DARK -> IndigoTokens
        AppThemePalette.PETALO -> PetaloTokens
        AppThemePalette.MARFIL, AppThemePalette.ARENA -> MarfilTokens
        AppThemePalette.SALVIA, AppThemePalette.LUMI_SAGE, AppThemePalette.LUMI_SAGE_DARK, AppThemePalette.CACTUS -> SalviaTokens
        AppThemePalette.LAVANDA, AppThemePalette.LUMI_LAVENDER, AppThemePalette.LUMI_LAVENDER_DARK -> LavandaTokens
        AppThemePalette.LUMI_SPARK_DARK -> NoirTokens.copy(
            ringGradient = Brush.linearGradient(listOf(Color(0xFF9B72C0), Color(0xFFE8A0C0)))
        )
        AppThemePalette.LUMI_SPARK -> PetaloTokens.copy(
            ringGradient = Brush.linearGradient(listOf(Color(0xFF9B72C0), Color(0xFFE8A0C0)))
        )
        else -> {
            val dark = palette.isDark()
            if (dark) NoirTokens else PetaloTokens
        }
    }
}

val LocalLumiTokens = compositionLocalOf { NoirTokens }

object LumiThemeTokens {
    val current: LumiDesignTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalLumiTokens.current
}
