package com.jeremy.lumi.ui.screens.partner

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.model.CyclePhase

/**
 * Vocabulario visual compartido del Modo Pareja: íconos, etiquetas y colores
 * por acción de cuidado y por fase. Centralizado para que Hub, Historias,
 * Calendario Compartido y Diario hablen el mismo idioma visual.
 */

/** Detalles que la UI ofrece (COFFEE queda solo por compatibilidad de datos). */
val visibleCareActions: List<CareAction> = listOf(
    CareAction.HUG,
    CareAction.TEA,
    CareAction.CHOCOLATE,
    CareAction.HEATING_PAD,
    CareAction.PHARMACY
)

fun careActionIcon(action: CareAction): ImageVector = when (action) {
    CareAction.HUG -> Icons.Rounded.Favorite
    CareAction.TEA -> Icons.Rounded.LocalCafe
    CareAction.COFFEE -> Icons.Rounded.Coffee
    CareAction.CHOCOLATE -> Icons.Rounded.Cookie
    CareAction.HEATING_PAD -> Icons.Rounded.Whatshot
    CareAction.PHARMACY -> Icons.Rounded.LocalPharmacy
}

@StringRes
fun careActionLabelRes(action: CareAction): Int = when (action) {
    CareAction.HUG -> R.string.pm_care_hug
    CareAction.TEA -> R.string.pm_care_tea
    CareAction.COFFEE -> R.string.pm_care_coffee
    CareAction.CHOCOLATE -> R.string.pm_care_chocolate
    CareAction.HEATING_PAD -> R.string.pm_care_heating_pad
    CareAction.PHARMACY -> R.string.pm_care_pharmacy
}

fun careActionTint(action: CareAction): Color = when (action) {
    CareAction.HUG -> Color(0xFFE84393)
    CareAction.TEA -> Color(0xFFB5854B)
    CareAction.COFFEE -> Color(0xFF8B5E3C)
    CareAction.CHOCOLATE -> Color(0xFF6F4E37)
    CareAction.HEATING_PAD -> Color(0xFFEB5757)
    CareAction.PHARMACY -> Color(0xFF38B2AC)
}

@StringRes
fun phaseLabelRes(phase: CyclePhase): Int = when (phase) {
    CyclePhase.MENSTRUAL -> R.string.pm_phase_menstrual
    CyclePhase.FOLLICULAR -> R.string.pm_phase_follicular
    CyclePhase.OVULATION -> R.string.pm_phase_ovulation
    CyclePhase.LUTEAL -> R.string.pm_phase_luteal
    CyclePhase.PREGNANCY -> R.string.pm_phase_pregnancy
    CyclePhase.UNKNOWN -> R.string.pm_phase_unknown
}

/**
 * Paleta fija del Modo Pareja por fase (independiente del tema para que ambas
 * personas vean exactamente los mismos colores en el calendario compartido).
 */
fun phaseColor(phase: CyclePhase): Color = when (phase) {
    CyclePhase.MENSTRUAL -> Color(0xFFE84393)
    CyclePhase.FOLLICULAR -> Color(0xFF48C6EF)
    CyclePhase.OVULATION -> Color(0xFFFFB75E)
    CyclePhase.LUTEAL -> Color(0xFFA18CD1)
    CyclePhase.PREGNANCY -> Color(0xFF43E97B)
    CyclePhase.UNKNOWN -> Color(0xFF9E9E9E)
}

/** Gradiente inmersivo de fondo para la vista de historias, según la fase. */
fun phaseGradient(phase: CyclePhase): List<Color> = when (phase) {
    // Para lograr el estilo "Glassmorphism / Premium", usamos fondos muy oscuros
    // con un ligerísimo tinte del color de la fase, simulando un resplandor.
    CyclePhase.MENSTRUAL -> listOf(Color(0xFF1E1018), Color(0xFF120B0F))
    CyclePhase.FOLLICULAR -> listOf(Color(0xFF101B24), Color(0xFF090E14))
    CyclePhase.OVULATION -> listOf(Color(0xFF241910), Color(0xFF140D09))
    CyclePhase.LUTEAL -> listOf(Color(0xFF1A1324), Color(0xFF0F0A14))
    CyclePhase.PREGNANCY -> listOf(Color(0xFF10241A), Color(0xFF09140E))
    CyclePhase.UNKNOWN -> listOf(Color(0xFF161618), Color(0xFF0B0B0C))
}
