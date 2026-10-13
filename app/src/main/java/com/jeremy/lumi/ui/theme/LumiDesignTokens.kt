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
    bg = Color(0xFF0B0B11),
    panel = Color(0xFF1B1923),
    panel2 = Color(0xFF23202B),
    text = Color(0xFFF6F2F9),
    muted = Color(0xFF9D96AB),
    dim = Color(0xFF716C7D),
    line = Color(0x1AE1D1ED),       // rgba(225, 209, 237, 0.10)
    accent = Color(0xFFCFB8F4),
    accentSoft = Color(0x24CFB8F4),   // rgba(207, 184, 244, 0.14)
    peach = Color(0xFFF6BAAB),
    rose = Color(0xFFE6A6BE),
    mint = Color(0xFF9CCDB5),
    gold = Color(0xFFF0D3A4),
    nav = Color(0xFF0D0C12),
    glow = Color(0x29AB6FD3),       // rgba(171, 111, 211, 0.16)
    field = Color(0xFF292531),
    pill = Color(0xFF27232E),
    ringTrack = Color(0xFF2B2832),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFFBDA8F4), Color(0xFFDCC3F4), Color(0xFFEDB2CF))
    ),
    isDark = true
)

// ── Tokens aprobados: BLUSH (Atmósfera clara cálida) ────────────────────────
val BlushTokens = LumiDesignTokens(
    bg = Color(0xFFFFFBF9),
    panel = Color(0xFFFFF4F0),
    panel2 = Color(0xFFF7E9E8),
    text = Color(0xFF3C303C),
    muted = Color(0xFF867985),
    dim = Color(0xFFA499A2),
    line = Color(0x1F6E4963),       // rgba(110, 73, 99, 0.12)
    accent = Color(0xFFA381C4),
    accentSoft = Color(0x21B18ECB),   // rgba(177, 142, 203, 0.13)
    peach = Color(0xFFEFAC98),
    rose = Color(0xFFDD91AE),
    mint = Color(0xFF79AD9A),
    gold = Color(0xFFCEA36F),
    nav = Color(0xFFFFFBF9),
    glow = Color(0x3BE5ACC3),       // rgba(229, 172, 195, 0.23)
    field = Color(0xFFF6E9E6),
    pill = Color(0xFFF1E7ED),
    ringTrack = Color(0xFFF1E8EC),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFFA381C4), Color(0xFFD6A8D0), Color(0xFFEFAC98))
    ),
    isDark = false
)

// ── Tokens aprobados: SAGE (Atmósfera verde/teal profunda) ──────────────────
val SageTokens = LumiDesignTokens(
    bg = Color(0xFF0D181A),
    panel = Color(0xFF19272A),
    panel2 = Color(0xFF233337),
    text = Color(0xFFF1F6F4),
    muted = Color(0xFFA1B1B0),
    dim = Color(0xFF829596),
    line = Color(0x1FC9E7E2),       // rgba(201, 231, 226, 0.12)
    accent = Color(0xFFB9C8BA),
    accentSoft = Color(0x1FA5CFC0),   // rgba(165, 207, 192, 0.12)
    peach = Color(0xFFE5B6A2),
    rose = Color(0xFFD8B4C9),
    mint = Color(0xFFA5CEB9),
    gold = Color(0xFFE1CA9D),
    nav = Color(0xFF0D181A),
    glow = Color(0x2E69A091),       // rgba(105, 160, 145, 0.18)
    field = Color(0xFF26393B),
    pill = Color(0xFF29393B),
    ringTrack = Color(0xFF2C4141),
    ringGradient = Brush.linearGradient(
        listOf(Color(0xFF8CAFA0), Color(0xFFB9C8BA), Color(0xFFE5B6A2))
    ),
    isDark = true
)

/**
 * Función adaptadora para garantizar que CUALQUIERA de las 37 paletas existentes
 * en Lumi se mapee limpiamente a los tokens semánticos de diseño.
 */
fun resolveLumiTokens(palette: AppThemePalette): LumiDesignTokens {
    return when (palette) {
        AppThemePalette.NOIR -> NoirTokens
        AppThemePalette.LUMI_SPARK_DARK -> NoirTokens.copy(
            ringGradient = Brush.linearGradient(listOf(Color(0xFF9B72C0), Color(0xFFE8A0C0)))
        )
        AppThemePalette.LUMI_SAGE_DARK -> SageTokens
        AppThemePalette.LUMI_SAGE -> SageTokens.copy(isDark = false, bg = Color(0xFFF0F5F2), panel = Color(0xFFFFFFFF))
        AppThemePalette.PETALO, AppThemePalette.LUMI_SPARK -> BlushTokens
        AppThemePalette.LAVANDA -> BlushTokens.copy(accent = Color(0xFF8B5EA8))
        AppThemePalette.CACTUS -> BlushTokens.copy(accent = Color(0xFF6E9E87))
        else -> {
            val dark = palette.isDark()
            if (dark) {
                NoirTokens.copy(
                    glow = Color(0x20CFB8F4)
                )
            } else {
                BlushTokens.copy(
                    glow = Color(0x25E5ACC3)
                )
            }
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
