package com.jeremy.lumi.ui.screens.bonds

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Sistema de diseño y tokens visuales para BONDS.
 *
 * Reproduce fielmente la estética de la referencia:
 * - Deep Navy elegante (#193458)
 * - Warm White / Crema (#FFFCFA)
 * - Peach suave (#FFC9B5)
 * - Blush rosa empolvado (#F3B6CA)
 * - Lavender (#C8B5F1)
 * - Secondary Text neutral (#68758A)
 * - Tarjetas amplias con esquinas suaves de 28-32dp
 * - Gradientes de aura etéreos y superficies translúcidas
 */
object BondsTheme {

    // Paleta de colores base
    val DeepNavy = Color(0xFF193458)
    val DeepNavyDark = Color(0xFF11223A)
    val WarmWhite = Color(0xFFFFFDFB)
    val WarmBackground = Color(0xFFFAF7F5)
    val PureWhite = Color(0xFFFFFFFF)
    
    // Tonos pasteles característicos
    val Peach = Color(0xFFFFC9B5)
    val PeachLight = Color(0xFFFFF1EB)
    val Blush = Color(0xFFF3B6CA)
    val BlushLight = Color(0xFFFDEBF1)
    val Lavender = Color(0xFFC8B5F1)
    val LavenderLight = Color(0xFFF4EFFD)
    val SoftViolet = Color(0xFF9070D8)

    // Textos y neutros
    val TextPrimary = Color(0xFF192A3E)
    val TextSecondary = Color(0xFF68758A)
    val TextTertiary = Color(0xFF9EAAB9)
    val BorderSubtle = Color(0xFFEDE8E3)
    val DividerSubtle = Color(0xFFF0EBE6)

    // Colores de acento para avatares y fases en el círculo
    val DotMenstrual = Color(0xFFE87A90)
    val DotFollicular = Color(0xFFE5B54F)
    val DotOvulation = Color(0xFF5ABEA0)
    val DotLuteal = Color(0xFF8F80DE)

    // Gradiente del fondo pastel de la tarjeta contextual (Teléfono 1)
    val FeelingCardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFF2EB),
            Color(0xFFFDEAF0),
            Color(0xFFF3EDFC)
        )
    )

    // Gradiente de borde de avatar
    val AvatarRingGradient = Brush.sweepGradient(
        colors = listOf(
            Peach,
            Blush,
            Lavender,
            SoftViolet,
            Peach
        )
    )

    // Gradiente de la tarjeta oscura "Link your circles" (Teléfono 3)
    val DarkCardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E3557),
            Color(0xFF14243C)
        )
    )

    // Aura etérea ascendente para el envío de detalles (Teléfono 2)
    val AuraGradientColors = listOf(
        Color(0xFFFFC9B5).copy(alpha = 0.65f),
        Color(0xFFF3B6CA).copy(alpha = 0.45f),
        Color(0xFFC8B5F1).copy(alpha = 0.30f),
        Color.Transparent
    )

    // Formas
    val CardShape = RoundedCornerShape(28.dp)
    val CardShapeLarge = RoundedCornerShape(32.dp)
    val CardShapeMedium = RoundedCornerShape(22.dp)
    val PillShape = RoundedCornerShape(100.dp)
    val GestureCardShape = RoundedCornerShape(24.dp)
}
