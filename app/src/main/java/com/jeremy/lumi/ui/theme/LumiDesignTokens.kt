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
    bg = Color(0xFF0B0A0F),
    panel = Color(0xFF15131A),
    panel2 = Color(0xFF211D27),
    text = Color(0xFFF6F2F7),
    muted = Color(0xFFB7AFBC),
    dim = Color(0xFF817985),
    line = Color(0xFF312C35),
    accent = Color(0xFFC9A7E8),
    accentSoft = Color(0x26C9A7E8),
    peach = Color(0xFFE6B0A1),
    rose = Color(0xFFDFA1BC),
    mint = Color(0xFF8FC5AF),
    gold = Color(0xFFD8B879),
    nav = Color(0xFF0E0C12),
    glow = Color(0x32C9A7E8),
    field = Color(0xFF211D27),
    pill = Color(0xFF211D27),
    ringTrack = Color(0xFF29242F),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFFC9A7E8), Color(0xFFDFA1BC), Color(0xFFE6B0A1))
    ),
    isDark = true
)

// ── CIRUELA (Atmósfera oscura profunda y cálida) ─────────────────────────────
val CiruelaTokens = LumiDesignTokens(
    bg = Color(0xFF120A10),
    panel = Color(0xFF1D1119),
    panel2 = Color(0xFF2C1926),
    text = Color(0xFFFFF4F7),
    muted = Color(0xFFC6ABB8),
    dim = Color(0xFF987C8A),
    line = Color(0xFF493040),
    accent = Color(0xFFE29DBF),
    accentSoft = Color(0x26E29DBF),
    peach = Color(0xFFE8AE98),
    rose = Color(0xFFD77EAB),
    mint = Color(0xFF91BFA8),
    gold = Color(0xFFD9B77F),
    nav = Color(0xFF150D13),
    glow = Color(0x34D77EAB),
    field = Color(0xFF2C1926),
    pill = Color(0xFF2C1926),
    ringTrack = Color(0xFF34202D),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFFE29DBF), Color(0xFFD77EAB), Color(0xFFE8AE98))
    ),
    isDark = true
)

// ── BOSQUE (Atmósfera oscura naturaleza esmeralda) ───────────────────────────
val BosqueTokens = LumiDesignTokens(
    bg = Color(0xFF09110F),
    panel = Color(0xFF111B18),
    panel2 = Color(0xFF1C2B26),
    text = Color(0xFFF1F7F3),
    muted = Color(0xFFA9BBB2),
    dim = Color(0xFF768A81),
    line = Color(0xFF2B3D36),
    accent = Color(0xFF83C5A5),
    accentSoft = Color(0x2683C5A5),
    peach = Color(0xFFD8AA91),
    rose = Color(0xFFCF8FA5),
    mint = Color(0xFF79B99A),
    gold = Color(0xFFCDB374),
    nav = Color(0xFF0C1411),
    glow = Color(0x3079B99A),
    field = Color(0xFF1C2B26),
    pill = Color(0xFF1C2B26),
    ringTrack = Color(0xFF22322C),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFF83C5A5), Color(0xFF79B99A), Color(0xFFCDB374))
    ),
    isDark = true
)

// ── ÍNDIGO (Atmósfera oscura cósmica noche) ──────────────────────────────────
val IndigoTokens = LumiDesignTokens(
    bg = Color(0xFF090E1C),
    panel = Color(0xFF11172A),
    panel2 = Color(0xFF1B2440),
    text = Color(0xFFF3F5FF),
    muted = Color(0xFFAAB4D0),
    dim = Color(0xFF7C88AA),
    line = Color(0xFF2C385A),
    accent = Color(0xFFAABBF6),
    accentSoft = Color(0x26AABBF6),
    peach = Color(0xFFD5A9B4),
    rose = Color(0xFFC793C0),
    mint = Color(0xFF84BDB7),
    gold = Color(0xFFCFB67B),
    nav = Color(0xFF0C1222),
    glow = Color(0x32AABBF6),
    field = Color(0xFF1B2440),
    pill = Color(0xFF1B2440),
    ringTrack = Color(0xFF202A49),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFFAABBF6), Color(0xFFC793C0), Color(0xFF84BDB7))
    ),
    isDark = true
)

// ── PÉTALO (Atmósfera clara suave floral) ────────────────────────────────────
val PetaloTokens = LumiDesignTokens(
    bg = Color(0xFFFBF5F7),
    panel = Color(0xFFFFF9FB),
    panel2 = Color(0xFFF0E3E9),
    text = Color(0xFF30262F),
    muted = Color(0xFF756874),
    dim = Color(0xFF968692),
    line = Color(0xFFDCCAD4),
    accent = Color(0xFF985A89),
    accentSoft = Color(0x24985A89),
    peach = Color(0xFFE0A08F),
    rose = Color(0xFFCF789D),
    mint = Color(0xFF6FA593),
    gold = Color(0xFFAE834D),
    nav = Color(0xFFFFF9FB),
    glow = Color(0x24CF789D),
    field = Color(0xFFF0E3E9),
    pill = Color(0xFFF0E3E9),
    ringTrack = Color(0xFFE5D5DD),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFF985A89), Color(0xFFCF789D), Color(0xFFE7A39B))
    ),
    isDark = false
)

// ── MARFIL (Atmósfera clara neutra cálida lino) ──────────────────────────────
val MarfilTokens = LumiDesignTokens(
    bg = Color(0xFFF8F2E8),
    panel = Color(0xFFFFFBF4),
    panel2 = Color(0xFFEEE4D6),
    text = Color(0xFF332D2B),
    muted = Color(0xFF70655F),
    dim = Color(0xFF92847B),
    line = Color(0xFFD8CABC),
    accent = Color(0xFF8F6063),
    accentSoft = Color(0x248F6063),
    peach = Color(0xFFC98F78),
    rose = Color(0xFFB87983),
    mint = Color(0xFF779887),
    gold = Color(0xFFA67B42),
    nav = Color(0xFFFFFBF4),
    glow = Color(0x22B87983),
    field = Color(0xFFEEE4D6),
    pill = Color(0xFFEEE4D6),
    ringTrack = Color(0xFFE4D8CA),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFF8F6063), Color(0xFFB87983), Color(0xFFD4A18A))
    ),
    isDark = false
)

// ── SALVIA (Atmósfera clara botánica menta calma) ────────────────────────────
val SalviaTokens = LumiDesignTokens(
    bg = Color(0xFFEEF4F0),
    panel = Color(0xFFFAFCFA),
    panel2 = Color(0xFFDEE8E1),
    text = Color(0xFF24322B),
    muted = Color(0xFF5F6F66),
    dim = Color(0xFF819187),
    line = Color(0xFFC8D6CC),
    accent = Color(0xFF49745F),
    accentSoft = Color(0x2449745F),
    peach = Color(0xFFB89378),
    rose = Color(0xFFB8798A),
    mint = Color(0xFF5F9678),
    gold = Color(0xFF8F7C4A),
    nav = Color(0xFFFAFCFA),
    glow = Color(0x245F9678),
    field = Color(0xFFDEE8E1),
    pill = Color(0xFFDEE8E1),
    ringTrack = Color(0xFFD5E1D8),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFF49745F), Color(0xFF6F9D86), Color(0xFFB3A078))
    ),
    isDark = false
)

// ── LAVANDA (Atmósfera clara bruma lila editorial) ───────────────────────────
val LavandaTokens = LumiDesignTokens(
    bg = Color(0xFFF4F1FA),
    panel = Color(0xFFFCFAFF),
    panel2 = Color(0xFFE8E1F1),
    text = Color(0xFF302A3E),
    muted = Color(0xFF6C647B),
    dim = Color(0xFF9186A3),
    line = Color(0xFFD6CDE3),
    accent = Color(0xFF7557A2),
    accentSoft = Color(0x247557A2),
    peach = Color(0xFFC99BA6),
    rose = Color(0xFFAE789F),
    mint = Color(0xFF729B93),
    gold = Color(0xFF917D9B),
    nav = Color(0xFFFCFAFF),
    glow = Color(0x247557A2),
    field = Color(0xFFE8E1F1),
    pill = Color(0xFFE8E1F1),
    ringTrack = Color(0xFFDED6EB),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFF7557A2), Color(0xFFA978B1), Color(0xFFC4A6D9))
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
