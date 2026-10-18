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
        previewBg = Color(0xFF0E0C13),
        previewAccent = Color(0xFFCBB4F1)
    ),
    CIRUELA(
        id = "ciruela",
        displayName = "Ciruela",
        isDark = true,
        appThemePalette = AppThemePalette.VINO,
        previewBg = Color(0xFF241D29),
        previewAccent = Color(0xFFE8ADC8)
    ),
    BOSQUE(
        id = "bosque",
        displayName = "Bosque",
        isDark = true,
        appThemePalette = AppThemePalette.LUMI_FOREST_DARK,
        previewBg = Color(0xFF17231F),
        previewAccent = Color(0xFFA3D4BD)
    ),
    INDIGO(
        id = "indigo",
        displayName = "Índigo",
        isDark = true,
        appThemePalette = AppThemePalette.LUMI_OCEAN_DARK,
        previewBg = Color(0xFF1A2037),
        previewAccent = Color(0xFFC0C9FF)
    ),
    PETALO(
        id = "petalo",
        displayName = "Pétalo",
        isDark = false,
        appThemePalette = AppThemePalette.PETALO,
        previewBg = Color(0xFFFFF5F8),
        previewAccent = Color(0xFFCD9AC9)
    ),
    MARFIL(
        id = "marfil",
        displayName = "Marfil",
        isDark = false,
        appThemePalette = AppThemePalette.ARENA,
        previewBg = Color(0xFFFFF9EF),
        previewAccent = Color(0xFFC99193)
    ),
    SALVIA(
        id = "salvia",
        displayName = "Salvia",
        isDark = false,
        appThemePalette = AppThemePalette.LUMI_SAGE,
        previewBg = Color(0xFFE4EEE7),
        previewAccent = Color(0xFF648F76)
    ),
    LAVANDA(
        id = "lavanda",
        displayName = "Lavanda",
        isDark = false,
        appThemePalette = AppThemePalette.LAVANDA,
        previewBg = Color(0xFFF8F1FF),
        previewAccent = Color(0xFF9B7CC5)
    );

    companion object {
        fun fromId(id: String?): LumiOnboardingTheme {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: NOIR
        }

        fun fromAppTheme(palette: AppThemePalette): LumiOnboardingTheme {
            return entries.find { it.appThemePalette == palette } ?: NOIR
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
    bg = Color(0xFF0E0C13),
    panel = Color(0xFF1D1924),
    panel2 = Color(0xFF27222E),
    line = Color(0xFF37313E),
    text = Color(0xFFFAF8FB),
    muted = Color(0xFFAAA3B5),
    muted2 = Color(0xFF858095),
    rose = Color(0xFFE6AFCA),
    accent = Color(0xFFCBB4F1),
    selection = Color(0x18BD99E8),
    veil1 = Color(0x30CCABED),
    veil2 = Color(0x38E4B1CB),
    glow = Color(0x35C58BE0),
    navbg = Color(0xF0131119),
    btnPink = Color(0xFFEAB0CE),
    btnViolet = Color(0xFFC6B1F4),
    btnHaloPink = Color(0xD6F6A7CD),
    btnHaloViolet = Color(0xE6BE98FF),
    btnHaloPeach = Color(0xBFFECCB4),
    skyA = Color(0xB2461D4C),
    skyB = Color(0x3B2C1733),
    isDark = true
)

val CiruelaTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFF141017),
    panel = Color(0xFF241D29),
    panel2 = Color(0xFF33283A),
    line = Color(0xFF453646),
    text = Color(0xFFFFF9FC),
    muted = Color(0xFFC2B1C2),
    muted2 = Color(0xFF9F8A9E),
    rose = Color(0xFFE8ADC8),
    accent = Color(0xFFDBB5EC),
    selection = Color(0x1AE3AAD7),
    veil1 = Color(0x3BE894B9),
    veil2 = Color(0x30C99EE0),
    glow = Color(0x30E699C8),
    navbg = Color(0xF0141017),
    btnPink = Color(0xFFEFB6D1),
    btnViolet = Color(0xFFD3B6ED),
    btnHaloPink = Color(0xD6F6A7CD),
    btnHaloViolet = Color(0xE6BE98FF),
    btnHaloPeach = Color(0xBFFECCB4),
    skyA = Color(0x85682743),
    skyB = Color(0x42522D64),
    isDark = true
)

val BosqueTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFF0C1413),
    panel = Color(0xFF17231F),
    panel2 = Color(0xFF24362E),
    line = Color(0xFF35493F),
    text = Color(0xFFF1FAF5),
    muted = Color(0xFFB1C4B8),
    muted2 = Color(0xFF83998D),
    rose = Color(0xFFB7D4BD),
    accent = Color(0xFFA3D4BD),
    selection = Color(0x1A8ACDAF),
    veil1 = Color(0x2E76B893),
    veil2 = Color(0x2497CEA5),
    glow = Color(0x2986CEAC),
    navbg = Color(0xF00C1413),
    btnPink = Color(0xFFB2DFC6),
    btnViolet = Color(0xFFD2DCAE),
    btnHaloPink = Color(0xC278DBB9),
    btnHaloViolet = Color(0xA3DEE798),
    btnHaloPeach = Color(0xBFFECCB4),
    skyA = Color(0x91154E40),
    skyB = Color(0x3D1F4030),
    isDark = true
)

val IndigoTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFF0D1122),
    panel = Color(0xFF1A2037),
    panel2 = Color(0xFF29304D),
    line = Color(0xFF3B4263),
    text = Color(0xFFF6F7FF),
    muted = Color(0xFFB5BBD8),
    muted2 = Color(0xFF8F99B9),
    rose = Color(0xFFC7AFDF),
    accent = Color(0xFFC0C9FF),
    selection = Color(0x1C94A9F0),
    veil1 = Color(0x359296EE),
    veil2 = Color(0x2BA077E5),
    glow = Color(0x2B97A6FF),
    navbg = Color(0xF00D1122),
    btnPink = Color(0xFFDBBFE9),
    btnViolet = Color(0xFFB2C4FA),
    btnHaloPink = Color(0xBAA096F0),
    btnHaloViolet = Color(0xDC8BADFF),
    btnHaloPeach = Color(0xBFFECCB4),
    skyA = Color(0xA32D337A),
    skyB = Color(0x42312B6B),
    isDark = true
)

val PetaloTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFFFCF7F7),
    panel = Color(0xFFFFF7F8),
    panel2 = Color(0xFFF3E9ED),
    line = Color(0xFFEBDFE7),
    text = Color(0xFF302838),
    muted = Color(0xFF786D7F),
    muted2 = Color(0xFF93899A),
    rose = Color(0xFFCE849F),
    accent = Color(0xFF9369B2),
    selection = Color(0x1CC184B4),
    veil1 = Color(0x4DD6A8DE),
    veil2 = Color(0x59F5B1CA),
    glow = Color(0x29E18FB5),
    navbg = Color(0xF0FFF7F8),
    btnPink = Color(0xFFEAB0CE),
    btnViolet = Color(0xFFC6B1F4),
    btnHaloPink = Color(0xD1E491B9),
    btnHaloViolet = Color(0xD1A77DDB),
    btnHaloPeach = Color(0xCCFFD7C3),
    skyA = Color.Transparent,
    skyB = Color.Transparent,
    isDark = false
)

val MarfilTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFFFAF6EE),
    panel = Color(0xFFFCF9F2),
    panel2 = Color(0xFFF3ECE3),
    line = Color(0xFFE7DCD4),
    text = Color(0xFF352F32),
    muted = Color(0xFF756A69),
    muted2 = Color(0xFF948986),
    rose = Color(0xFFC99193),
    accent = Color(0xFF90718F),
    selection = Color(0x1CA57997),
    veil1 = Color(0x29E6BB97),
    veil2 = Color(0x2BD3AED7),
    glow = Color(0x21CE9B9E),
    navbg = Color(0xF0FCF9F2),
    btnPink = Color(0xFFF0C2B4),
    btnViolet = Color(0xFFD9C4E0),
    btnHaloPink = Color(0xA8F2B78F),
    btnHaloViolet = Color(0xA6BB97DC),
    btnHaloPeach = Color(0xCCFFD7C3),
    skyA = Color.Transparent,
    skyB = Color.Transparent,
    isDark = false
)

val SalviaTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFFF2F7F2),
    panel = Color(0xFFF5FAF5),
    panel2 = Color(0xFFE6EEE7),
    line = Color(0xFFD3E3D9),
    text = Color(0xFF253B30),
    muted = Color(0xFF65786B),
    muted2 = Color(0xFF839B8B),
    rose = Color(0xFF82AA94),
    accent = Color(0xFF5C8E76),
    selection = Color(0x1F74A988),
    veil1 = Color(0x3380BE9A),
    veil2 = Color(0x21B0CEAA),
    glow = Color(0x2684B194),
    navbg = Color(0xF0F5FAF5),
    btnPink = Color(0xFFBCE4C8),
    btnViolet = Color(0xFFC4D9B9),
    btnHaloPink = Color(0xAD8BD7A4),
    btnHaloViolet = Color(0xA6B6CC8C),
    btnHaloPeach = Color(0xCCFFD7C3),
    skyA = Color.Transparent,
    skyB = Color.Transparent,
    isDark = false
)

val LavandaTokensV7 = LumiThemeTokensV7(
    bg = Color(0xFFF8F5FE),
    panel = Color(0xFFFBF9FF),
    panel2 = Color(0xFFEEE9F8),
    line = Color(0xFFE3DCEF),
    text = Color(0xFF342B49),
    muted = Color(0xFF756B88),
    muted2 = Color(0xFF998EAD),
    rose = Color(0xFFBD8FC3),
    accent = Color(0xFF8F70BD),
    selection = Color(0x1FAD8FDD),
    veil1 = Color(0x29BFA1E4),
    veil2 = Color(0x2BDBA6DB),
    glow = Color(0x26AD8EE2),
    navbg = Color(0xF0FBF9FF),
    btnPink = Color(0xFFE5BADD),
    btnViolet = Color(0xFFC3B8EE),
    btnHaloPink = Color(0xBFDF9BD0),
    btnHaloViolet = Color(0xB2A498E8),
    btnHaloPeach = Color(0xCCFFD7C3),
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
