package com.jeremy.lumi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.jeremy.lumi.data.preferences.ThemePreferenceManager
import com.jeremy.lumi.ui.navigation.AppNavGraph
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.BackgroundAmbient
import com.jeremy.lumi.ui.theme.DialStyle
import com.jeremy.lumi.ui.theme.LumiTheme
import com.jeremy.lumi.ui.theme.PhaseColorPalette
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Inyectamos el lector de preferencias de DataStore directamente aquí
    @Inject
    lateinit var themePreferenceManager: ThemePreferenceManager

    @Inject
    lateinit var languagePreferenceManager: com.jeremy.lumi.data.preferences.LanguagePreferenceManager

    override fun attachBaseContext(newBase: android.content.Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            super.attachBaseContext(newBase)
            return
        }
        val lang = kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.withTimeoutOrNull(200) {
                com.jeremy.lumi.data.preferences.LanguagePreferenceManager(newBase).getSelectedLanguage()
            } ?: com.jeremy.lumi.data.preferences.LanguagePreferenceManager.SYSTEM_DEFAULT
        }
        val localizedContext = com.jeremy.lumi.utils.LocaleHelper.wrapContext(newBase, lang)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Escuchamos el Flow de DataStore en tiempo real en la raíz de la app
            val selectedTheme by themePreferenceManager.selectedThemeFlow.collectAsStateWithLifecycle(
                initialValue = AppThemePalette.NOIR // Valor mientras carga
            )


            // Paleta de colores de fase elegida (preset o CUSTOM)
            val selectedPhasePalette by themePreferenceManager.selectedPhasePaletteFlow.collectAsStateWithLifecycle(
                initialValue = PhaseColorPalette.AUTO_HARMONY
            )

            // Colores individuales personalizados — solo se usan cuando
            // selectedPhasePalette es CUSTOM; null mientras no haya nada guardado
            val customPhaseColors by themePreferenceManager.customPhaseColorsFlow.collectAsStateWithLifecycle(
                initialValue = null
            )

            // Estilo visual del aro (Gradiente continuo, Mapa por sectores o Minimalista)
            val dialStyle by themePreferenceManager.dialStyleFlow.collectAsStateWithLifecycle(
                initialValue = DialStyle.GRADIENT
            )

            // Ambiente de fondo (Plano, Celeste)
            val selectedAmbient by themePreferenceManager.backgroundAmbientFlow.collectAsStateWithLifecycle(
                initialValue = BackgroundAmbient.FLAT
            )

            // Le pasamos la elección de la usuaria a nuestro Tema
            LumiTheme(
                selectedTheme        = selectedTheme,
                selectedPhasePalette = selectedPhasePalette,
                customPhaseColors    = customPhaseColors,
                dialStyle            = dialStyle,
                selectedAmbient      = selectedAmbient
            ) {
                AppNavGraph()
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}
