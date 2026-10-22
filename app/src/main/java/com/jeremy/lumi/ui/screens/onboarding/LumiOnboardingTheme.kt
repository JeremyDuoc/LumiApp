package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jeremy.lumi.ui.theme.AppThemePalette

/**
 * Los 8 temas cohesivos aprobados en el prototipo HTML v7 de Lumi:
 * - 4 Oscuros: Noir, Ciruela, Bosque, Índigo
 * - 4 Claros: Pétalo, Marfil, Salvia, Lavanda
 */
enum class LumiOnboardingTheme(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    val appThemePalette: AppThemePalette,
    val previewBg: Color,
    val previewAccent: Color
) {
    NOIR(
        id = "noir",
        displayName = "Noir",
        isDark = true,
        appThemePalette = AppThemePalette.NOIR,
        previewBg = Color(0xFF0B0A0F),
        previewAccent = Color(0xFFC9A7E8)
    ),
    CIRUELA(
        id = "ciruela",
        displayName = "Ciruela",
        isDark = true,
        appThemePalette = AppThemePalette.CIRUELA,
        previewBg = Color(0xFF120A10),
        previewAccent = Color(0xFFE29DBF)
    ),
    BOSQUE(
        id = "bosque",
        displayName = "Bosque",
        isDark = true,
        appThemePalette = AppThemePalette.BOSQUE,
        previewBg = Color(0xFF09110F),
        previewAccent = Color(0xFF83C5A5)
    ),
    INDIGO(
        id = "indigo",
        displayName = "Índigo",
        isDark = true,
        appThemePalette = AppThemePalette.INDIGO,
        previewBg = Color(0xFF090E1C),
        previewAccent = Color(0xFFAABBF6)
    ),
    PETALO(
        id = "petalo",
        displayName = "Pétalo",
        isDark = false,
        appThemePalette = AppThemePalette.PETALO,
        previewBg = Color(0xFFFBF5F7),
        previewAccent = Color(0xFF985A89)
    ),
    MARFIL(
        id = "marfil",
        displayName = "Marfil",
        isDark = false,
        appThemePalette = AppThemePalette.MARFIL,
        previewBg = Color(0xFFF8F2E8),
        previewAccent = Color(0xFF8F6063)
    ),
    SALVIA(
        id = "salvia",
        displayName = "Salvia",
        isDark = false,
        appThemePalette = AppThemePalette.SALVIA,
        previewBg = Color(0xFFEEF4F0),
        previewAccent = Color(0xFF49745F)
    ),
    LAVANDA(
        id = "lavanda",
        displayName = "Lavanda",
        isDark = false,
        appThemePalette = AppThemePalette.LAVANDA,
        previewBg = Color(0xFFF4F1FA),
        previewAccent = Color(0xFF7557A2)
    );

    companion object {
        fun fromId(id: String?): LumiOnboardingTheme {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: NOIR
        }

        fun fromAppTheme(palette: AppThemePalette): LumiOnboardingTheme {
            return when (palette) {
                AppThemePalette.NOIR -> NOIR
                AppThemePalette.CIRUELA, AppThemePalette.VINO -> CIRUELA
                AppThemePalette.BOSQUE, AppThemePalette.LUMI_FOREST, AppThemePalette.LUMI_FOREST_DARK -> BOSQUE
                AppThemePalette.INDIGO, AppThemePalette.LUMI_OCEAN, AppThemePalette.LUMI_OCEAN_DARK -> INDIGO
                AppThemePalette.PETALO -> PETALO
                AppThemePalette.MARFIL, AppThemePalette.ARENA -> MARFIL
                AppThemePalette.SALVIA, AppThemePalette.LUMI_SAGE, AppThemePalette.LUMI_SAGE_DARK, AppThemePalette.CACTUS -> SALVIA
                AppThemePalette.LAVANDA, AppThemePalette.LUMI_LAVENDER, AppThemePalette.LUMI_LAVENDER_DARK -> LAVANDA
                else -> entries.find { it.appThemePalette == palette } ?: NOIR
            }
        }
    }
}

/**
 * Tokens de color de alta fidelidad para el onboarding v7.
 * Mapeo exacto de las variables CSS de reference/lumi_onboarding_v7_luz_cromatica.html.
 */
data class LumiThemeTokensV7(
    val bg: Color,
    val panel: Color,
    val panel2: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val muted2: Color,
    val rose: Color,
    val accent: Color,
    val selection: Color,
    val veil1: Color,
    val veil2: Color,
    val glow: Color,
    val navbg: Color,
    val btnPink: Color,
    val btnViolet: Color,
    val btnHaloPink: Color,
    val btnHaloViolet: Color,
    val btnHaloPeach: Color,
    val skyA: Color,
    val skyB: Color,
    val isDark: Boolean
)

val LumiThemeTokensV7.accentSoft: Color get() = selection
val LumiThemeTokensV7.dim: Color get() = muted2

val NoirTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFF0B0A0F),
    panel = Color(0xFF15131A),
    panel2 = Color(0xFF211D27),
    line = Color(0xFF312C35),
    text = Color(0xFFF6F2F7),
    muted = Color(0xFFB7AFBC),
    muted2 = Color(0xFF817985),
    rose = Color(0xFFDFA1BC),
    accent = Color(0xFFC9A7E8),
    selection = Color(0x20C9A7E8),
    veil1 = Color(0x30C9A7E8),
    veil2 = Color(0x2ADFA1BC),
    glow = Color(0x32C9A7E8),
    navbg = Color(0xF00E0C12),
    btnPink = Color(0xFFDFA1BC),
    btnViolet = Color(0xFFC9A7E8),
    btnHaloPink = Color(0xBFDFA1BC),
    btnHaloViolet = Color(0xC9C9A7E8),
    btnHaloPeach = Color(0xA8E6B0A1),
    skyA = Color(0x8A321C3B),
    skyB = Color(0x38201628),
    isDark = true
)

val CiruelaTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFF120A10),
    panel = Color(0xFF1D1119),
    panel2 = Color(0xFF2C1926),
    line = Color(0xFF493040),
    text = Color(0xFFFFF4F7),
    muted = Color(0xFFC6ABB8),
    muted2 = Color(0xFF987C8A),
    rose = Color(0xFFD77EAB),
    accent = Color(0xFFE29DBF),
    selection = Color(0x22E29DBF),
    veil1 = Color(0x32D77EAB),
    veil2 = Color(0x28E8AE98),
    glow = Color(0x34D77EAB),
    navbg = Color(0xF0150D13),
    btnPink = Color(0xFFD77EAB),
    btnViolet = Color(0xFFBE9BC8),
    btnHaloPink = Color(0xC2D77EAB),
    btnHaloViolet = Color(0xB8BE9BC8),
    btnHaloPeach = Color(0x9FE8AE98),
    skyA = Color(0x8853253E),
    skyB = Color(0x3F3A2038),
    isDark = true
)

val BosqueTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFF09110F),
    panel = Color(0xFF111B18),
    panel2 = Color(0xFF1C2B26),
    line = Color(0xFF2B3D36),
    text = Color(0xFFF1F7F3),
    muted = Color(0xFFA9BBB2),
    muted2 = Color(0xFF768A81),
    rose = Color(0xFFCF8FA5),
    accent = Color(0xFF83C5A5),
    selection = Color(0x2083C5A5),
    veil1 = Color(0x2C79B99A),
    veil2 = Color(0x24CDB374),
    glow = Color(0x3079B99A),
    navbg = Color(0xF00C1411),
    btnPink = Color(0xFF79B99A),
    btnViolet = Color(0xFFCDB374),
    btnHaloPink = Color(0xAA79B99A),
    btnHaloViolet = Color(0x9FCDB374),
    btnHaloPeach = Color(0x90D8AA91),
    skyA = Color(0x82123F35),
    skyB = Color(0x38203A30),
    isDark = true
)

val IndigoTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFF090E1C),
    panel = Color(0xFF11172A),
    panel2 = Color(0xFF1B2440),
    line = Color(0xFF2C385A),
    text = Color(0xFFF3F5FF),
    muted = Color(0xFFAAB4D0),
    muted2 = Color(0xFF7C88AA),
    rose = Color(0xFFC793C0),
    accent = Color(0xFFAABBF6),
    selection = Color(0x20AABBF6),
    veil1 = Color(0x2FAABBF6),
    veil2 = Color(0x26C793C0),
    glow = Color(0x32AABBF6),
    navbg = Color(0xF00C1222),
    btnPink = Color(0xFFC793C0),
    btnViolet = Color(0xFFAABBF6),
    btnHaloPink = Color(0xABC793C0),
    btnHaloViolet = Color(0xBFAABBF6),
    btnHaloPeach = Color(0x8FD5A9B4),
    skyA = Color(0x8C27366E),
    skyB = Color(0x3D22294F),
    isDark = true
)

val PetaloTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFFFBF5F7),
    panel = Color(0xFFFFF9FB),
    panel2 = Color(0xFFF0E3E9),
    line = Color(0xFFDCCAD4),
    text = Color(0xFF30262F),
    muted = Color(0xFF756874),
    muted2 = Color(0xFF968692),
    rose = Color(0xFFCF789D),
    accent = Color(0xFF985A89),
    selection = Color(0x1F985A89),
    veil1 = Color(0x38CF789D),
    veil2 = Color(0x30E0A08F),
    glow = Color(0x24CF789D),
    navbg = Color(0xF0FFF9FB),
    btnPink = Color(0xFFCF789D),
    btnViolet = Color(0xFF985A89),
    btnHaloPink = Color(0xA8CF789D),
    btnHaloViolet = Color(0x92985A89),
    btnHaloPeach = Color(0x9FE0A08F),
    skyA = Color.Transparent,
    skyB = Color.Transparent,
    isDark = false
)

val MarfilTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFFF8F2E8),
    panel = Color(0xFFFFFBF4),
    panel2 = Color(0xFFEEE4D6),
    line = Color(0xFFD8CABC),
    text = Color(0xFF332D2B),
    muted = Color(0xFF70655F),
    muted2 = Color(0xFF92847B),
    rose = Color(0xFFB87983),
    accent = Color(0xFF8F6063),
    selection = Color(0x1F8F6063),
    veil1 = Color(0x2AC98F78),
    veil2 = Color(0x25B87983),
    glow = Color(0x22B87983),
    navbg = Color(0xF0FFFBF4),
    btnPink = Color(0xFFB87983),
    btnViolet = Color(0xFFA67B42),
    btnHaloPink = Color(0x92B87983),
    btnHaloViolet = Color(0x82A67B42),
    btnHaloPeach = Color(0x9FC98F78),
    skyA = Color.Transparent,
    skyB = Color.Transparent,
    isDark = false
)

val SalviaTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFFEEF4F0),
    panel = Color(0xFFFAFCFA),
    panel2 = Color(0xFFDEE8E1),
    line = Color(0xFFC8D6CC),
    text = Color(0xFF24322B),
    muted = Color(0xFF5F6F66),
    muted2 = Color(0xFF819187),
    rose = Color(0xFFB8798A),
    accent = Color(0xFF49745F),
    selection = Color(0x1F49745F),
    veil1 = Color(0x2B5F9678),
    veil2 = Color(0x228F7C4A),
    glow = Color(0x245F9678),
    navbg = Color(0xF0FAFCFA),
    btnPink = Color(0xFF5F9678),
    btnViolet = Color(0xFF8F7C4A),
    btnHaloPink = Color(0x925F9678),
    btnHaloViolet = Color(0x7E8F7C4A),
    btnHaloPeach = Color(0x8FB89378),
    skyA = Color.Transparent,
    skyB = Color.Transparent,
    isDark = false
)

val LavandaTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFFF4F1FA),
    panel = Color(0xFFFCFAFF),
    panel2 = Color(0xFFE8E1F1),
    line = Color(0xFFD6CDE3),
    text = Color(0xFF302A3E),
    muted = Color(0xFF6C647B),
    muted2 = Color(0xFF9186A3),
    rose = Color(0xFFAE789F),
    accent = Color(0xFF7557A2),
    selection = Color(0x1F7557A2),
    veil1 = Color(0x2E7557A2),
    veil2 = Color(0x28AE789F),
    glow = Color(0x247557A2),
    navbg = Color(0xF0FCFAFF),
    btnPink = Color(0xFFAE789F),
    btnViolet = Color(0xFF7557A2),
    btnHaloPink = Color(0x94AE789F),
    btnHaloViolet = Color(0x967557A2),
    btnHaloPeach = Color(0x83C99BA6),
    skyA = Color.Transparent,
    skyB = Color.Transparent,
    isDark = false
)

fun getThemeTokensV7(theme: LumiOnboardingTheme): LumiThemeTokensV7 {
    return when (theme) {
        LumiOnboardingTheme.NOIR -> NoirTokensV7
        LumiOnboardingTheme.CIRUELA -> CiruelaTokensV7
        LumiOnboardingTheme.BOSQUE -> BosqueTokensV7
        LumiOnboardingTheme.INDIGO -> IndigoTokensV7
        LumiOnboardingTheme.PETALO -> PetaloTokensV7
        LumiOnboardingTheme.MARFIL -> MarfilTokensV7
        LumiOnboardingTheme.SALVIA -> SalviaTokensV7
        LumiOnboardingTheme.LAVANDA -> LavandaTokensV7
    }
}

val LocalLumiOnboardingTokens = compositionLocalOf { NoirTokensV7 }

object LumiOnboardingThemeTokens {
    val current: LumiThemeTokensV7
        @Composable
        @ReadOnlyComposable
        get() = LocalLumiOnboardingTokens.current
}
