package com.jeremy.lumi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jeremy.lumi.data.preferences.ThemePreferenceManager
import com.jeremy.lumi.ui.navigation.AppNavGraph
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.LumiTheme
import com.jeremy.lumi.ui.theme.PhaseColorPalette
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Inyectamos el lector de preferencias de DataStore directamente aquí
    @Inject
    lateinit var themePreferenceManager: ThemePreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Escuchamos el Flow de DataStore en tiempo real en la raíz de la app
            val selectedTheme by themePreferenceManager.selectedThemeFlow.collectAsState(
                initial = AppThemePalette.CACTUS // Valor mientras carga
            )

            // Paleta de colores de fase elegida (preset o CUSTOM)
            val selectedPhasePalette by themePreferenceManager.selectedPhasePaletteFlow.collectAsState(
                initial = PhaseColorPalette.DEFAULT
            )

            // Colores individuales personalizados — solo se usan cuando
            // selectedPhasePalette es CUSTOM; null mientras no haya nada guardado
            val customPhaseColors by themePreferenceManager.customPhaseColorsFlow.collectAsState(
                initial = null
            )

            // Le pasamos la elección de la usuaria a nuestro Tema
            LumiTheme(
                selectedTheme        = selectedTheme,
                selectedPhasePalette = selectedPhasePalette,
                customPhaseColors    = customPhaseColors
            ) {
                AppNavGraph()
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}