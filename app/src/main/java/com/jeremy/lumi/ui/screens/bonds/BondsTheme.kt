package com.jeremy.lumi.ui.screens.bonds

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jeremy.lumi.ui.theme.LumiThemeTokens

/**
 * Sistema de diseño y tokens visuales para BONDS.
 *
 * Totalmente integrado con el motor de temas de Lumi (`LumiThemeTokens`):
 * - Responde reactivamente a Noir (oscuro), Blush (claro cálido), Sage (verde/teal), Charcoal, etc.
 * - Mantiene la identidad y composición de Bonds (tarjetas amplias, auras suaves, aros degradados).
 * - En temas oscuros, las tarjetas y fondos adoptan superficies oscuras coherentes con alto contraste.
 * - En temas claros, mantiene la calidez pastel y la tipografía Deep Navy refinada.
 */
object BondsTheme {

    // Paleta de colores base sincronizada con el tema activo
    val DeepNavy: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.text

    val DeepNavyDark: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LumiThemeTokens.current.isDark) LumiThemeTokens.current.panel2 else Color(0xFF11223A)

    val WarmWhite: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.panel

    val WarmBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.bg

    val PureWhite: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.panel

    // Tonos pasteles y semánticos característicos
    val Peach: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.peach

    val PeachLight: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LumiThemeTokens.current.isDark) LumiThemeTokens.current.peach.copy(alpha = 0.20f) else Color(0xFFFFF1EB)

    val Blush: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.rose

    val BlushLight: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LumiThemeTokens.current.isDark) LumiThemeTokens.current.rose.copy(alpha = 0.20f) else Color(0xFFFDEBF1)

    val Lavender: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.accent

    val LavenderLight: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LumiThemeTokens.current.isDark) LumiThemeTokens.current.accentSoft else Color(0xFFF4EFFD)

    val SoftViolet: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.accent

    // Textos y neutros
    val TextPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.text

    val TextSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.muted

    val TextTertiary: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.dim

    val BorderSubtle: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.line

    val DividerSubtle: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.line

    // Colores de acento para avatares y fases en el círculo
    val DotMenstrual: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.rose

    val DotFollicular: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.gold

    val DotOvulation: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.mint

    val DotLuteal: Color
        @Composable
        @ReadOnlyComposable
        get() = LumiThemeTokens.current.accent

    // Gradiente del fondo pastel de la tarjeta contextual (Teléfono 1)
    val FeelingCardGradient: Brush
        @Composable
        @ReadOnlyComposable
        get() {
            val tokens = LumiThemeTokens.current
            return if (tokens.isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        tokens.panel,
                        tokens.panel2,
                        tokens.panel
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFF2EB),
                        Color(0xFFFDEAF0),
                        Color(0xFFF3EDFC)
                    )
                )
            }
        }

    // Gradiente de borde de avatar
    val AvatarRingGradient: Brush
        @Composable
        @ReadOnlyComposable
        get() {
            val tokens = LumiThemeTokens.current
            return Brush.sweepGradient(
                colors = listOf(
                    tokens.peach,
                    tokens.rose,
                    tokens.accent,
                    tokens.mint,
                    tokens.peach
                )
            )
        }

    // Gradiente de la tarjeta oscura "Link your circles" (Teléfono 3)
    val DarkCardGradient: Brush
        @Composable
        @ReadOnlyComposable
        get() {
            val tokens = LumiThemeTokens.current
            return if (tokens.isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        tokens.panel2,
                        tokens.panel
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E3557),
                        Color(0xFF14243C)
                    )
                )
            }
        }

    // Aura etérea ascendente para el envío de detalles (Teléfono 2)
    val AuraGradientColors: List<Color>
        @Composable
        @ReadOnlyComposable
        get() {
            val tokens = LumiThemeTokens.current
            return listOf(
                tokens.peach.copy(alpha = if (tokens.isDark) 0.35f else 0.65f),
                tokens.rose.copy(alpha = if (tokens.isDark) 0.25f else 0.45f),
                tokens.accent.copy(alpha = if (tokens.isDark) 0.20f else 0.30f),
                Color.Transparent
            )
        }

    // Formas
    val CardShape = RoundedCornerShape(28.dp)
    val CardShapeLarge = RoundedCornerShape(32.dp)
    val CardShapeMedium = RoundedCornerShape(22.dp)
    val PillShape = RoundedCornerShape(100.dp)
    val GestureCardShape = RoundedCornerShape(24.dp)
}
