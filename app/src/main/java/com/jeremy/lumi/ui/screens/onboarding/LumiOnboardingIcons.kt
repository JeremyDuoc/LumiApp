package com.jeremy.lumi.ui.screens.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Proveedor de iconografía desacoplado e intercambiable para el onboarding de Lumi.
 *
 * Cumplimiento con context/Lumi_Contexto_Continuidad_Onboarding_v7.md:
 * - Los iconos de candidates-icons-NOT-FINAL son candidatos provisionales, no aprobados.
 * - Esta interfaz permite utilizar vectores nativos legibles y calibrados por defecto,
 *   con un punto de reemplazo limpio cuando el usuario determine los recursos finales.
 */
sealed class OnboardingIconKey {
    data object Calendar : OnboardingIconKey()
    data object People : OnboardingIconKey()
    data object Heart : OnboardingIconKey()
    data object Moon : OnboardingIconKey()
    data object Leaf : OnboardingIconKey()
    data object Waves : OnboardingIconKey()
    data object Sparkle : OnboardingIconKey()
    data object Shield : OnboardingIconKey()
}

interface OnboardingIconProvider {
    fun getVector(key: OnboardingIconKey): ImageVector
}

object DefaultOnboardingIconProvider : OnboardingIconProvider {
    override fun getVector(key: OnboardingIconKey): ImageVector {
        return when (key) {
            OnboardingIconKey.Calendar -> Icons.Rounded.CalendarMonth
            OnboardingIconKey.People -> Icons.Rounded.Groups
            OnboardingIconKey.Heart -> Icons.Rounded.Favorite
            OnboardingIconKey.Moon -> Icons.Rounded.NightsStay
            OnboardingIconKey.Leaf -> Icons.Rounded.Spa
            OnboardingIconKey.Waves -> Icons.Rounded.Waves
            OnboardingIconKey.Sparkle -> Icons.Rounded.AutoAwesome
            OnboardingIconKey.Shield -> Icons.Rounded.Security
        }
    }
}
