package com.jeremy.lumi.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

// ─────────────────────────────────────────────
//  ENUM DE TEMAS
// ─────────────────────────────────────────────
enum class AppThemePalette {
    // Oficial
    LUMI_SPARK, LUMI_SPARK_DARK,
    // 8 Temas Cohesivos Oficiales HTML v11
    NOIR, CIRUELA, BOSQUE, INDIGO, PETALO, MARFIL, SALVIA, LAVANDA,
    // Nuevas Gradientes
    LUMI_OCEAN, LUMI_OCEAN_DARK, LUMI_FOREST, LUMI_FOREST_DARK, LUMI_SUNSET, LUMI_SUNSET_DARK, LUMI_DUNE, LUMI_DUNE_DARK,
    LUMI_LAVENDER, LUMI_LAVENDER_DARK, LUMI_SAGE, LUMI_SAGE_DARK, LUMI_EARTH, LUMI_EARTH_DARK, LUMI_SLATE, LUMI_SLATE_DARK,
    CYCLE_MENSTRUAL, CYCLE_MENSTRUAL_DARK, CYCLE_OVULATION, CYCLE_OVULATION_DARK,
    // Claras
    CACTUS, HORTENSIA, TIERRA, LUXE, PIEDRA,
    // Oscuras
    MEDIANOCHE, COSMOS, CARBONO, ECLIPSE, NOCHE_ROSA, OBSIDIANA, VINO, FORJA, AMBAR,
    // Minimalistas
    PAPEL, NIEBLA, SAL, ARENA, PIZARRA, HUMO, LIENZO, CENIZA;

    fun isDark(): Boolean = this in setOf(
        NOIR, CIRUELA, BOSQUE, INDIGO,
        LUMI_SPARK_DARK, LUMI_OCEAN_DARK, LUMI_FOREST_DARK, LUMI_SUNSET_DARK, LUMI_DUNE_DARK,
        LUMI_LAVENDER_DARK, LUMI_SAGE_DARK, LUMI_EARTH_DARK, LUMI_SLATE_DARK,
        CYCLE_MENSTRUAL_DARK, CYCLE_OVULATION_DARK,
        MEDIANOCHE, COSMOS, CARBONO, ECLIPSE, NOCHE_ROSA, OBSIDIANA, VINO, FORJA, AMBAR
    )
}

// ── Noir · Atmósfera oscura minimalista aprobada en prototipo HTML ───────────
val NoirPrimary    = Color(0xFFC9A7E8)
val NoirSecondary  = Color(0xFFB7AFBC)
val NoirTertiary   = Color(0xFF817985)
val NoirBackground = Color(0xFF0B0A0F)
val NoirSurface    = Color(0xFF15131A)
val NoirOnPrimary  = Color(0xFF0B0A0F)
val NoirOnBg       = Color(0xFFF6F2F7)

private val NoirColorScheme = darkColorScheme(
    primary          = Color(0xFFC9A7E8),
    onPrimary        = Color(0xFF0B0A0F),
    primaryContainer = Color(0xFF211D27),
    secondary        = Color(0xFFB7AFBC),
    onSecondary      = Color(0xFF0B0A0F),
    tertiary         = Color(0xFFDFA1BC),
    background       = Color(0xFF0B0A0F),
    onBackground     = Color(0xFFF6F2F7),
    surface          = Color(0xFF15131A),
    onSurface        = Color(0xFFF6F2F7),
    surfaceVariant   = Color(0xFF211D27),
    onSurfaceVariant = Color(0xFFB7AFBC),
    outline          = Color(0xFF312C35)
)

// ── 8 TEMAS COHESIVOS OFICIALES (HTML v11) ──────────────────────────────────
private val CiruelaColorScheme = darkColorScheme(
    primary          = Color(0xFFE29DBF),
    onPrimary        = Color(0xFF120A10),
    primaryContainer = Color(0xFF2C1926),
    secondary        = Color(0xFFC6ABB8),
    onSecondary      = Color(0xFF120A10),
    tertiary         = Color(0xFFE8AE98),
    background       = Color(0xFF120A10),
    onBackground     = Color(0xFFFFF4F7),
    surface          = Color(0xFF1D1119),
    onSurface        = Color(0xFFFFF4F7),
    surfaceVariant   = Color(0xFF2C1926),
    onSurfaceVariant = Color(0xFFC6ABB8),
    outline          = Color(0xFF493040)
)

private val BosqueColorScheme = darkColorScheme(
    primary          = Color(0xFF83C5A5),
    onPrimary        = Color(0xFF09110F),
    primaryContainer = Color(0xFF1C2B26),
    secondary        = Color(0xFFA9BBB2),
    onSecondary      = Color(0xFF09110F),
    tertiary         = Color(0xFFCDB374),
    background       = Color(0xFF09110F),
    onBackground     = Color(0xFFF1F7F3),
    surface          = Color(0xFF111B18),
    onSurface        = Color(0xFFF1F7F3),
    surfaceVariant   = Color(0xFF1C2B26),
    onSurfaceVariant = Color(0xFFA9BBB2),
    outline          = Color(0xFF2B3D36)
)

private val IndigoColorScheme = darkColorScheme(
    primary          = Color(0xFFAABBF6),
    onPrimary        = Color(0xFF090E1C),
    primaryContainer = Color(0xFF1B2440),
    secondary        = Color(0xFFAAB4D0),
    onSecondary      = Color(0xFF090E1C),
    tertiary         = Color(0xFFC793C0),
    background       = Color(0xFF090E1C),
    onBackground     = Color(0xFFF3F5FF),
    surface          = Color(0xFF11172A),
    onSurface        = Color(0xFFF3F5FF),
    surfaceVariant   = Color(0xFF1B2440),
    onSurfaceVariant = Color(0xFFAAB4D0),
    outline          = Color(0xFF2C385A)
)

private val PetaloColorScheme = lightColorScheme(
    primary          = Color(0xFF985A89),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF0E3E9),
    secondary        = Color(0xFF756874),
    onSecondary      = Color(0xFFFFFFFF),
    tertiary         = Color(0xFFCF789D),
    background       = Color(0xFFFBF5F7),
    onBackground     = Color(0xFF30262F),
    surface          = Color(0xFFFFF9FB),
    onSurface        = Color(0xFF30262F),
    surfaceVariant   = Color(0xFFF0E3E9),
    onSurfaceVariant = Color(0xFF756874),
    outline          = Color(0xFFDCCAD4)
)

private val MarfilColorScheme = lightColorScheme(
    primary          = Color(0xFF8F6063),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEEE4D6),
    secondary        = Color(0xFF70655F),
    onSecondary      = Color(0xFFFFFFFF),
    tertiary         = Color(0xFFA67B42),
    background       = Color(0xFFF8F2E8),
    onBackground     = Color(0xFF332D2B),
    surface          = Color(0xFFFFFBF4),
    onSurface        = Color(0xFF332D2B),
    surfaceVariant   = Color(0xFFEEE4D6),
    onSurfaceVariant = Color(0xFF70655F),
    outline          = Color(0xFFD8CABC)
)

private val SalviaColorScheme = lightColorScheme(
    primary          = Color(0xFF49745F),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDEE8E1),
    secondary        = Color(0xFF5F6F66),
    onSecondary      = Color(0xFFFFFFFF),
    tertiary         = Color(0xFF8F7C4A),
    background       = Color(0xFFEEF4F0),
    onBackground     = Color(0xFF24322B),
    surface          = Color(0xFFFAFCFA),
    onSurface        = Color(0xFF24322B),
    surfaceVariant   = Color(0xFFDEE8E1),
    onSurfaceVariant = Color(0xFF5F6F66),
    outline          = Color(0xFFC8D6CC)
)

private val LavandaColorScheme = lightColorScheme(
    primary          = Color(0xFF7557A2),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8E1F1),
    secondary        = Color(0xFF6C647B),
    onSecondary      = Color(0xFFFFFFFF),
    tertiary         = Color(0xFFAE789F),
    background       = Color(0xFFF4F1FA),
    onBackground     = Color(0xFF302A3E),
    surface          = Color(0xFFFCFAFF),
    onSurface        = Color(0xFF302A3E),
    surfaceVariant   = Color(0xFFE8E1F1),
    onSurfaceVariant = Color(0xFF6C647B),
    outline          = Color(0xFFD6CDE3)
)

// ── Claras ───────────────────────────────────────────────────────────────────

private val LumiSparkColorScheme = lightColorScheme(
    primary = LumiSparkPrimary, onPrimary = LumiSparkOnPrimary, primaryContainer = Color(0xFFF3E5F5),
    secondary = LumiSparkSecondary, onSecondary = LumiSparkOnBg, tertiary = LumiSparkTertiary,
    background = LumiSparkBackground, onBackground = LumiSparkOnBg,
    surface = LumiSparkSurface, onSurface = LumiSparkOnBg,
    surfaceVariant = Color(0xFFF8EEF3), onSurfaceVariant = LumiSparkOnBg, outline = Color(0xFFDCC8DE)
)

private val LumiSparkDarkColorScheme = darkColorScheme(
    primary = LumiSparkDarkPrimary, onPrimary = LumiSparkDarkOnPrimary, primaryContainer = Color(0xFF382A3D),
    secondary = LumiSparkDarkSecondary, onSecondary = LumiSparkDarkBackground, tertiary = LumiSparkDarkTertiary,
    background = LumiSparkDarkBackground, onBackground = LumiSparkDarkOnBg,
    surface = LumiSparkDarkSurface, onSurface = LumiSparkDarkOnBg,
    surfaceVariant = Color(0xFF302636), onSurfaceVariant = LumiSparkDarkOnBg, outline = Color(0xFF5A4862)
)

private val LumiOceanColorScheme = lightColorScheme(
    primary = LumiOceanPrimary, onPrimary = LumiOceanOnPrimary, primaryContainer = Color(0xFFE3F2FD),
    secondary = LumiOceanSecondary, onSecondary = LumiOceanOnBg, tertiary = LumiOceanTertiary,
    background = LumiOceanBackground, onBackground = LumiOceanOnBg,
    surface = LumiOceanSurface, onSurface = LumiOceanOnBg,
    surfaceVariant = Color(0xFFE8F0FE), onSurfaceVariant = LumiOceanOnBg, outline = Color(0xFFB3CDE0)
)
private val LumiOceanDarkColorScheme = darkColorScheme(
    primary = LumiOceanDarkPrimary, onPrimary = LumiOceanDarkOnPrimary, primaryContainer = Color(0xFF0D2447),
    secondary = LumiOceanDarkSecondary, onSecondary = LumiOceanDarkBackground, tertiary = LumiOceanDarkTertiary,
    background = LumiOceanDarkBackground, onBackground = LumiOceanDarkOnBg,
    surface = LumiOceanDarkSurface, onSurface = LumiOceanDarkOnBg,
    surfaceVariant = Color(0xFF14294D), onSurfaceVariant = LumiOceanDarkOnBg, outline = Color(0xFF2A4870)
)

private val LumiForestColorScheme = lightColorScheme(
    primary = LumiForestPrimary, onPrimary = LumiForestOnPrimary, primaryContainer = Color(0xFFE8F5E9),
    secondary = LumiForestSecondary, onSecondary = LumiForestOnBg, tertiary = LumiForestTertiary,
    background = LumiForestBackground, onBackground = LumiForestOnBg,
    surface = LumiForestSurface, onSurface = LumiForestOnBg,
    surfaceVariant = Color(0xFFEAF5EF), onSurfaceVariant = LumiForestOnBg, outline = Color(0xFFB8DECC)
)
private val LumiForestDarkColorScheme = darkColorScheme(
    primary = LumiForestDarkPrimary, onPrimary = LumiForestDarkOnPrimary, primaryContainer = Color(0xFF0F2E20),
    secondary = LumiForestDarkSecondary, onSecondary = LumiForestDarkBackground, tertiary = LumiForestDarkTertiary,
    background = LumiForestDarkBackground, onBackground = LumiForestDarkOnBg,
    surface = LumiForestDarkSurface, onSurface = LumiForestDarkOnBg,
    surfaceVariant = Color(0xFF153325), onSurfaceVariant = LumiForestDarkOnBg, outline = Color(0xFF2B5740)
)

private val LumiSunsetColorScheme = lightColorScheme(
    primary = LumiSunsetPrimary, onPrimary = LumiSunsetOnPrimary, primaryContainer = Color(0xFFFBE9E7),
    secondary = LumiSunsetSecondary, onSecondary = LumiSunsetOnBg, tertiary = LumiSunsetTertiary,
    background = LumiSunsetBackground, onBackground = LumiSunsetOnBg,
    surface = LumiSunsetSurface, onSurface = LumiSunsetOnBg,
    surfaceVariant = Color(0xFFFFF0E8), onSurfaceVariant = LumiSunsetOnBg, outline = Color(0xFFEABEA8)
)
private val LumiSunsetDarkColorScheme = darkColorScheme(
    primary = LumiSunsetDarkPrimary, onPrimary = LumiSunsetDarkOnPrimary, primaryContainer = Color(0xFF3E1E1A),
    secondary = LumiSunsetDarkSecondary, onSecondary = LumiSunsetDarkBackground, tertiary = LumiSunsetDarkTertiary,
    background = LumiSunsetDarkBackground, onBackground = LumiSunsetDarkOnBg,
    surface = LumiSunsetDarkSurface, onSurface = LumiSunsetDarkOnBg,
    surfaceVariant = Color(0xFF45201A), onSurfaceVariant = LumiSunsetDarkOnBg, outline = Color(0xFF6B3A30)
)

private val LumiDuneColorScheme = lightColorScheme(
    primary = LumiDunePrimary, onPrimary = LumiDuneOnPrimary, primaryContainer = Color(0xFFF5EFE6),
    secondary = LumiDuneSecondary, onSecondary = LumiDuneOnBg, tertiary = LumiDuneTertiary,
    background = LumiDuneBackground, onBackground = LumiDuneOnBg,
    surface = LumiDuneSurface, onSurface = LumiDuneOnBg,
    surfaceVariant = Color(0xFFF5EFE6), onSurfaceVariant = LumiDuneOnBg, outline = Color(0xFFD6C8B4)
)
private val LumiDuneDarkColorScheme = darkColorScheme(
    primary = LumiDuneDarkPrimary, onPrimary = LumiDuneDarkOnPrimary, primaryContainer = Color(0xFF332A1C),
    secondary = LumiDuneDarkSecondary, onSecondary = LumiDuneDarkBackground, tertiary = LumiDuneDarkTertiary,
    background = LumiDuneDarkBackground, onBackground = LumiDuneDarkOnBg,
    surface = LumiDuneDarkSurface, onSurface = LumiDuneDarkOnBg,
    surfaceVariant = Color(0xFF382F1F), onSurfaceVariant = LumiDuneDarkOnBg, outline = Color(0xFF5A4C3A)
)

private val LumiLavenderColorScheme = lightColorScheme(
    primary = LumiLavenderPrimary, onPrimary = LumiLavenderOnPrimary, primaryContainer = Color(0xFFE8DCF5),
    secondary = LumiLavenderSecondary, onSecondary = LumiLavenderOnBg, tertiary = LumiLavenderTertiary,
    background = LumiLavenderBackground, onBackground = LumiLavenderOnBg,
    surface = LumiLavenderSurface, onSurface = LumiLavenderOnBg,
    surfaceVariant = Color(0xFFEDE4F8), onSurfaceVariant = LumiLavenderOnBg, outline = Color(0xFFCDB4E2)
)
private val LumiLavenderDarkColorScheme = darkColorScheme(
    primary = LumiLavenderDarkPrimary, onPrimary = LumiLavenderDarkOnPrimary, primaryContainer = Color(0xFF2C223D),
    secondary = LumiLavenderDarkSecondary, onSecondary = LumiLavenderDarkBackground, tertiary = LumiLavenderDarkTertiary,
    background = LumiLavenderDarkBackground, onBackground = LumiLavenderDarkOnBg,
    surface = LumiLavenderDarkSurface, onSurface = LumiLavenderDarkOnBg,
    surfaceVariant = Color(0xFF261D36), onSurfaceVariant = LumiLavenderDarkOnBg, outline = Color(0xFF4A3862)
)

private val LumiSageColorScheme = lightColorScheme(
    primary = LumiSagePrimary, onPrimary = LumiSageOnPrimary, primaryContainer = Color(0xFFD8EADF),
    secondary = LumiSageSecondary, onSecondary = LumiSageOnBg, tertiary = LumiSageTertiary,
    background = LumiSageBackground, onBackground = LumiSageOnBg,
    surface = LumiSageSurface, onSurface = LumiSageOnBg,
    surfaceVariant = Color(0xFFE5EFEA), onSurfaceVariant = LumiSageOnBg, outline = Color(0xFFB8CEBE)
)
private val LumiSageDarkColorScheme = darkColorScheme(
    primary = LumiSageDarkPrimary, onPrimary = LumiSageDarkOnPrimary, primaryContainer = Color(0xFF1E3328),
    secondary = LumiSageDarkSecondary, onSecondary = LumiSageDarkBackground, tertiary = LumiSageDarkTertiary,
    background = LumiSageDarkBackground, onBackground = LumiSageDarkOnBg,
    surface = LumiSageDarkSurface, onSurface = LumiSageDarkOnBg,
    surfaceVariant = Color(0xFF1A2E24), onSurfaceVariant = LumiSageDarkOnBg, outline = Color(0xFF385748)
)

private val LumiEarthColorScheme = lightColorScheme(
    primary = LumiEarthPrimary, onPrimary = LumiEarthOnPrimary, primaryContainer = Color(0xFFF5EBE6),
    secondary = LumiEarthSecondary, onSecondary = LumiEarthOnBg, tertiary = LumiEarthTertiary,
    background = LumiEarthBackground, onBackground = LumiEarthOnBg,
    surface = LumiEarthSurface, onSurface = LumiEarthOnBg,
    surfaceVariant = Color(0xFFFAF2EE), onSurfaceVariant = LumiEarthOnBg, outline = Color(0xFFD4C8BC)
)
private val LumiEarthDarkColorScheme = darkColorScheme(
    primary = LumiEarthDarkPrimary, onPrimary = LumiEarthDarkOnPrimary, primaryContainer = Color(0xFF3D2A1C),
    secondary = LumiEarthDarkSecondary, onSecondary = LumiEarthDarkBackground, tertiary = LumiEarthDarkTertiary,
    background = LumiEarthDarkBackground, onBackground = LumiEarthDarkOnBg,
    surface = LumiEarthDarkSurface, onSurface = LumiEarthDarkOnBg,
    surfaceVariant = Color(0xFF382618), onSurfaceVariant = LumiEarthDarkOnBg, outline = Color(0xFF5A4430)
)

private val LumiSlateColorScheme = lightColorScheme(
    primary = LumiSlatePrimary, onPrimary = LumiSlateOnPrimary, primaryContainer = Color(0xFFE6EBF0),
    secondary = LumiSlateSecondary, onSecondary = LumiSlateOnBg, tertiary = LumiSlateTertiary,
    background = LumiSlateBackground, onBackground = LumiSlateOnBg,
    surface = LumiSlateSurface, onSurface = LumiSlateOnBg,
    surfaceVariant = Color(0xFFEDF2F8), onSurfaceVariant = LumiSlateOnBg, outline = Color(0xFFBCC8D4)
)
private val LumiSlateDarkColorScheme = darkColorScheme(
    primary = LumiSlateDarkPrimary, onPrimary = LumiSlateDarkOnPrimary, primaryContainer = Color(0xFF1C2433),
    secondary = LumiSlateDarkSecondary, onSecondary = LumiSlateDarkBackground, tertiary = LumiSlateDarkTertiary,
    background = LumiSlateDarkBackground, onBackground = LumiSlateDarkOnBg,
    surface = LumiSlateDarkSurface, onSurface = LumiSlateDarkOnBg,
    surfaceVariant = Color(0xFF18202E), onSurfaceVariant = LumiSlateDarkOnBg, outline = Color(0xFF3A485A)
)

private val CycleMenstrualColorScheme = lightColorScheme(
    primary = CycleMenstrualPrimary, onPrimary = CycleMenstrualOnPrimary, primaryContainer = Color(0xFFF5D6DF),
    secondary = CycleMenstrualSecondary, onSecondary = CycleMenstrualOnBg, tertiary = CycleMenstrualTertiary,
    background = CycleMenstrualBackground, onBackground = CycleMenstrualOnBg,
    surface = CycleMenstrualSurface, onSurface = CycleMenstrualOnBg,
    surfaceVariant = Color(0xFFFBE4EB), onSurfaceVariant = CycleMenstrualOnBg, outline = Color(0xFFD4A8B4)
)
private val CycleMenstrualDarkColorScheme = darkColorScheme(
    primary = CycleMenstrualDarkPrimary, onPrimary = CycleMenstrualDarkOnPrimary, primaryContainer = Color(0xFF3E1A25),
    secondary = CycleMenstrualDarkSecondary, onSecondary = CycleMenstrualDarkBackground, tertiary = CycleMenstrualDarkTertiary,
    background = CycleMenstrualDarkBackground, onBackground = CycleMenstrualDarkOnBg,
    surface = CycleMenstrualDarkSurface, onSurface = CycleMenstrualDarkOnBg,
    surfaceVariant = Color(0xFF38141E), onSurfaceVariant = CycleMenstrualDarkOnBg, outline = Color(0xFF5A2C3A)
)

private val CycleOvulationColorScheme = lightColorScheme(
    primary = CycleOvulationPrimary, onPrimary = CycleOvulationOnPrimary, primaryContainer = Color(0xFFD4EAF0),
    secondary = CycleOvulationSecondary, onSecondary = CycleOvulationOnBg, tertiary = CycleOvulationTertiary,
    background = CycleOvulationBackground, onBackground = CycleOvulationOnBg,
    surface = CycleOvulationSurface, onSurface = CycleOvulationOnBg,
    surfaceVariant = Color(0xFFE2F4FA), onSurfaceVariant = CycleOvulationOnBg, outline = Color(0xFFB4C8D4)
)
private val CycleOvulationDarkColorScheme = darkColorScheme(
    primary = CycleOvulationDarkPrimary, onPrimary = CycleOvulationDarkOnPrimary, primaryContainer = Color(0xFF1A333E),
    secondary = CycleOvulationDarkSecondary, onSecondary = CycleOvulationDarkBackground, tertiary = CycleOvulationDarkTertiary,
    background = CycleOvulationDarkBackground, onBackground = CycleOvulationDarkOnBg,
    surface = CycleOvulationDarkSurface, onSurface = CycleOvulationDarkOnBg,
    surfaceVariant = Color(0xFF142E38), onSurfaceVariant = CycleOvulationDarkOnBg, outline = Color(0xFF2C4A5A)
)

private val CactusColorScheme = lightColorScheme(
    primary = CactusPrimary, onPrimary = CactusOnPrimary, primaryContainer = Color(0xFFD8EDDF),
    secondary = CactusSecondary, onSecondary = CactusOnBg, tertiary = CactusTertiary,
    background = CactusBackground, onBackground = CactusOnBg,
    surface = CactusSurface, onSurface = CactusOnBg,
    surfaceVariant = Color(0xFFE5EEEA), onSurfaceVariant = CactusOnBg, outline = Color(0xFFB8CEC5)
)
private val HortensiaColorScheme = lightColorScheme(
    primary = HortensiaPrimary, onPrimary = HortensiaOnPrimary, primaryContainer = Color(0xFFFFD6E7),
    secondary = HortensiaSecondary, onSecondary = HortensiaOnBg, tertiary = HortensiaTertiary,
    background = HortensiaBackground, onBackground = HortensiaOnBg,
    surface = HortensiaSurface, onSurface = HortensiaOnBg,
    surfaceVariant = Color(0xFFFFE0EC), onSurfaceVariant = HortensiaOnBg, outline = Color(0xFFFFB3D0)
)
private val TierraColorScheme = lightColorScheme(
    primary = TierraPrimary, onPrimary = TierraOnPrimary, primaryContainer = Color(0xFFE8D8CC),
    secondary = TierraSecondary, onSecondary = TierraOnBg, tertiary = TierraTertiary,
    background = TierraBackground, onBackground = TierraOnBg,
    surface = TierraSurface, onSurface = TierraOnBg,
    surfaceVariant = Color(0xFFEDE8E3), onSurfaceVariant = TierraOnBg, outline = Color(0xFFCCBFB4)
)
private val LuxeColorScheme = lightColorScheme(
    primary = LuxePrimary, onPrimary = LuxeOnPrimary, primaryContainer = Color(0xFFE8D0CC),
    secondary = LuxeSecondary, onSecondary = LuxeOnBg, tertiary = LuxeTertiary,
    background = LuxeBackground, onBackground = LuxeOnBg,
    surface = LuxeSurface, onSurface = LuxeOnBg,
    surfaceVariant = Color(0xFFEDE5DE), onSurfaceVariant = LuxeOnBg, outline = Color(0xFFCCBBAA)
)
private val PiedraColorScheme = lightColorScheme(
    primary = PiedraPrimary, onPrimary = PiedraOnPrimary, primaryContainer = Color(0xFFE0DEDC),
    secondary = PiedraSecondary, onSecondary = PiedraOnBg, tertiary = PiedraTertiary,
    background = PiedraBackground, onBackground = PiedraOnBg,
    surface = PiedraSurface, onSurface = PiedraOnBg,
    surfaceVariant = Color(0xFFECEAE8), onSurfaceVariant = PiedraOnBg, outline = Color(0xFFCCCAC8)
)

// ── Oscuras ──────────────────────────────────────────────────────────────────

private val MedianocheColorScheme = darkColorScheme(
    primary = MedianochePrimary, onPrimary = MedianocheOnPrimary, primaryContainer = Color(0xFF2E2550),
    secondary = MedianocheSecondary, onSecondary = MedianocheBackground, tertiary = MedianocheTertiary,
    background = MedianocheBackground, onBackground = MedianocheOnBg,
    surface = MedianocheSurface, onSurface = MedianocheOnBg,
    surfaceVariant = Color(0xFF2A2440), onSurfaceVariant = MedianocheOnBg, outline = Color(0xFF4A4068)
)
private val CosmosColorScheme = darkColorScheme(
    primary = CosmosPrimary, onPrimary = CosmosOnPrimary, primaryContainer = Color(0xFF1E1535),
    secondary = CosmosSecondary, onSecondary = CosmosBackground, tertiary = CosmosTertiary,
    background = CosmosBackground, onBackground = CosmosOnBg,
    surface = CosmosSurface, onSurface = CosmosOnBg,
    surfaceVariant = Color(0xFF131B30), onSurfaceVariant = CosmosOnBg, outline = Color(0xFF2A3858)
)
private val CarbonoColorScheme = darkColorScheme(
    primary = CarbonoPrimary, onPrimary = CarbonoOnPrimary, primaryContainer = Color(0xFF2E1E1E),
    secondary = CarbonoSecondary, onSecondary = CarbonoBackground, tertiary = CarbonoTertiary,
    background = CarbonoBackground, onBackground = CarbonoOnBg,
    surface = CarbonoSurface, onSurface = CarbonoOnBg,
    surfaceVariant = Color(0xFF252525), onSurfaceVariant = CarbonoOnBg, outline = Color(0xFF3A3A3A)
)
private val EclipseColorScheme = darkColorScheme(
    primary = EclipsePrimary, onPrimary = EclipseOnPrimary, primaryContainer = Color(0xFF2E1A1C),
    secondary = EclipseSecondary, onSecondary = EclipseBackground, tertiary = EclipseTertiary,
    background = EclipseBackground, onBackground = EclipseOnBg,
    surface = EclipseSurface, onSurface = EclipseOnBg,
    surfaceVariant = Color(0xFF2A1A1C), onSurfaceVariant = EclipseOnBg, outline = Color(0xFF4A3033)
)
private val NocheRosaColorScheme = darkColorScheme(
    primary = NochePrimary, onPrimary = NocheOnPrimary, primaryContainer = Color(0xFF2E1820),
    secondary = NocheSecondary, onSecondary = NocheBackground, tertiary = NocheTertiary,
    background = NocheBackground, onBackground = NocheOnBg,
    surface = NocheSurface, onSurface = NocheOnBg,
    surfaceVariant = Color(0xFF2E2028), onSurfaceVariant = NocheOnBg, outline = Color(0xFF503848)
)
private val ObsidianaColorScheme = darkColorScheme(
    primary = ObsidianaPrimary, onPrimary = ObsidianaOnPrimary, primaryContainer = Color(0xFF0E2828),
    secondary = ObsidianaSecondary, onSecondary = ObsidianaBackground, tertiary = ObsidianaTertiary,
    background = ObsidianaBackground, onBackground = ObsidianaOnBg,
    surface = ObsidianaSurface, onSurface = ObsidianaOnBg,
    surfaceVariant = Color(0xFF182020), onSurfaceVariant = ObsidianaOnBg, outline = Color(0xFF2A3C3C)
)
private val VinoColorScheme = darkColorScheme(
    primary = VinoPrimary, onPrimary = VinoOnPrimary, primaryContainer = Color(0xFF2A1020),
    secondary = VinoSecondary, onSecondary = VinoBackground, tertiary = VinoTertiary,
    background = VinoBackground, onBackground = VinoOnBg,
    surface = VinoSurface, onSurface = VinoOnBg,
    surfaceVariant = Color(0xFF261018), onSurfaceVariant = VinoOnBg, outline = Color(0xFF4A2838)
)
private val ForjaColorScheme = darkColorScheme(
    primary = ForjaPrimary, onPrimary = ForjaOnPrimary, primaryContainer = Color(0xFF182030),
    secondary = ForjaSecondary, onSecondary = ForjaBackground, tertiary = ForjaTertiary,
    background = ForjaBackground, onBackground = ForjaOnBg,
    surface = ForjaSurface, onSurface = ForjaOnBg,
    surfaceVariant = Color(0xFF1E2830), onSurfaceVariant = ForjaOnBg, outline = Color(0xFF304050)
)
private val AmbarColorScheme = darkColorScheme(
    primary = AmbarPrimary, onPrimary = AmbarOnPrimary, primaryContainer = Color(0xFF2A1E08),
    secondary = AmbarSecondary, onSecondary = AmbarBackground, tertiary = AmbarTertiary,
    background = AmbarBackground, onBackground = AmbarOnBg,
    surface = AmbarSurface, onSurface = AmbarOnBg,
    surfaceVariant = Color(0xFF281E10), onSurfaceVariant = AmbarOnBg, outline = Color(0xFF483820)
)

// ── Minimalistas ─────────────────────────────────────────────────────────────

private val PapelColorScheme = lightColorScheme(
    primary = PapelPrimary, onPrimary = PapelOnPrimary, primaryContainer = Color(0xFFEDEBE8),
    secondary = PapelSecondary, onSecondary = PapelOnBg, tertiary = PapelTertiary,
    background = PapelBackground, onBackground = PapelOnBg,
    surface = PapelSurface, onSurface = PapelOnBg,
    surfaceVariant = Color(0xFFF0EDE9), onSurfaceVariant = PapelOnBg, outline = Color(0xFFCECBC6)
)
private val NieblaColorScheme = lightColorScheme(
    primary = NieblaPrimary, onPrimary = NieblaOnPrimary, primaryContainer = Color(0xFFD6E5F5),
    secondary = NieblaSecondary, onSecondary = NieblaOnBg, tertiary = NieblaTertiary,
    background = NieblaBackground, onBackground = NieblaOnBg,
    surface = NieblaSurface, onSurface = NieblaOnBg,
    surfaceVariant = Color(0xFFE8F0FA), onSurfaceVariant = NieblaOnBg, outline = Color(0xFFB8CEEA)
)
private val SalColorScheme = lightColorScheme(
    primary = SalPrimary, onPrimary = SalOnPrimary, primaryContainer = Color(0xFFF5E5E6),
    secondary = SalSecondary, onSecondary = SalOnBg, tertiary = SalTertiary,
    background = SalBackground, onBackground = SalOnBg,
    surface = SalSurface, onSurface = SalOnBg,
    surfaceVariant = Color(0xFFF5ECEC), onSurfaceVariant = SalOnBg, outline = Color(0xFFE0C8C8)
)
private val ArenaColorScheme = lightColorScheme(
    primary = ArenaPrimary, onPrimary = ArenaOnPrimary, primaryContainer = Color(0xFFE8E4DE),
    secondary = ArenaSecondary, onSecondary = ArenaOnBg, tertiary = ArenaTertiary,
    background = ArenaBackground, onBackground = ArenaOnBg,
    surface = ArenaSurface, onSurface = ArenaOnBg,
    surfaceVariant = Color(0xFFEEEAE4), onSurfaceVariant = ArenaOnBg, outline = Color(0xFFCCC8C0)
)
private val PizarraColorScheme = lightColorScheme(
    primary = PizarraPrimary, onPrimary = PizarraOnPrimary, primaryContainer = Color(0xFFDEE0E4),
    secondary = PizarraSecondary, onSecondary = PizarraOnBg, tertiary = PizarraTertiary,
    background = PizarraBackground, onBackground = PizarraOnBg,
    surface = PizarraSurface, onSurface = PizarraOnBg,
    surfaceVariant = Color(0xFFEAECEE), onSurfaceVariant = PizarraOnBg, outline = Color(0xFFC4C8CC)
)
private val HumoColorScheme = lightColorScheme(
    primary = HumoPrimary, onPrimary = HumoOnPrimary, primaryContainer = Color(0xFFE8E4DC),
    secondary = HumoSecondary, onSecondary = HumoOnBg, tertiary = HumoTertiary,
    background = HumoBackground, onBackground = HumoOnBg,
    surface = HumoSurface, onSurface = HumoOnBg,
    surfaceVariant = Color(0xFFEEECE8), onSurfaceVariant = HumoOnBg, outline = Color(0xFFCCCAC4)
)
private val LienzoColorScheme = lightColorScheme(
    primary = LienzoPrimary, onPrimary = LienzoOnPrimary, primaryContainer = Color(0xFFEEE4D4),
    secondary = LienzoSecondary, onSecondary = LienzoOnBg, tertiary = LienzoTertiary,
    background = LienzoBackground, onBackground = LienzoOnBg,
    surface = LienzoSurface, onSurface = LienzoOnBg,
    surfaceVariant = Color(0xFFF0EAE0), onSurfaceVariant = LienzoOnBg, outline = Color(0xFFD0C8B8)
)
private val CenizaColorScheme = lightColorScheme(
    primary = CenizaPrimary, onPrimary = CenizaOnPrimary, primaryContainer = Color(0xFFE4E4E4),
    secondary = CenizaSecondary, onSecondary = CenizaOnBg, tertiary = CenizaTertiary,
    background = CenizaBackground, onBackground = CenizaOnBg,
    surface = CenizaSurface, onSurface = CenizaOnBg,
    surfaceVariant = Color(0xFFEEEEEE), onSurfaceVariant = CenizaOnBg, outline = Color(0xFFCCCCCC)
)

// ─────────────────────────────────────────────────────────────────────────────
//  COMPOSITION LOCALS
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────
//  ESTILO DEL ARO (HOME DIAL)
// ─────────────────────────────────────────────
enum class DialStyle {
    GRADIENT,
    CYCLE_MAP,
    MINIMALIST
}

val LocalIsDarkTheme = compositionLocalOf { false }

// Tema real de la raíz; evita el falso NOIR inicial de SettingsViewModel en MainScreen.
val LocalActiveLumiTheme = compositionLocalOf { AppThemePalette.NOIR }

data class ThemeRevealTrigger(
    val changeThemeWithReveal: (palette: AppThemePalette, center: Offset?) -> Unit
)

val LocalThemeRevealTrigger = compositionLocalOf<ThemeRevealTrigger?> { null }

// Expone los colores de fase activos a cualquier composable sin pasarlos como
// parámetro. Se alimenta en LumiTheme con: preset elegido, o el set custom de
// la usuaria cuando el preset activo es CUSTOM.
val LocalPhaseColors = compositionLocalOf { PhaseColorsDefaultLight }

// Colores semánticos de fase — SIEMPRE DEFAULT, independientes del tema.
// Usar para etiquetas de fase, chip de fase, color del anillo de progreso.
// Nunca usan AUTO_HARMONY ni paletas armónicas — solo rojo/teal/morado/ámbar fijos.
val LocalSemanticPhaseColors = compositionLocalOf { PhaseColorsDefaultLight }

// Expone el estilo visual del aro del Home (Gradiente continuo vs. Mapa por sectores vs. Minimalista)
val LocalDialStyle = compositionLocalOf { DialStyle.GRADIENT }

// Expone un gradiente de marca activo para componentes que deban destacar.
// Será null si el tema activo no es Lumi Spark oficial.
val LocalBrandGradient = compositionLocalOf<Brush?> { null }

// Expone un gradiente sutil para el fondo general de la app.
val LocalBrandBackgroundGradient = compositionLocalOf<Brush?> { null }

// ─────────────────────────────────────────────
//  ANIMACIÓN FLUIDA DE TEMAS Y PULSO CONECTADO
// ─────────────────────────────────────────────

@Composable
fun rememberAnimatedColorScheme(
    target: ColorScheme,
    animationSpec: AnimationSpec<Color> = tween(450, easing = CubicBezierEasing(0.2f, 0.83f, 0.2f, 1f))
): ColorScheme {
    val primary by animateColorAsState(target.primary, animationSpec, label = "cs_primary")
    val onPrimary by animateColorAsState(target.onPrimary, animationSpec, label = "cs_onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, animationSpec, label = "cs_primaryContainer")
    val secondary by animateColorAsState(target.secondary, animationSpec, label = "cs_secondary")
    val onSecondary by animateColorAsState(target.onSecondary, animationSpec, label = "cs_onSecondary")
    val tertiary by animateColorAsState(target.tertiary, animationSpec, label = "cs_tertiary")
    val background by animateColorAsState(target.background, animationSpec, label = "cs_bg")
    val onBackground by animateColorAsState(target.onBackground, animationSpec, label = "cs_onBg")
    val surface by animateColorAsState(target.surface, animationSpec, label = "cs_surface")
    val onSurface by animateColorAsState(target.onSurface, animationSpec, label = "cs_onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, animationSpec, label = "cs_surfaceVariant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, animationSpec, label = "cs_onSurfaceVariant")
    val outline by animateColorAsState(target.outline, animationSpec, label = "cs_outline")

    return remember(primary, onPrimary, primaryContainer, secondary, onSecondary, tertiary, background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant, outline) {
        target.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            tertiary = tertiary,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline
        )
    }
}

@Composable
fun rememberAnimatedLumiTokens(
    target: LumiDesignTokens,
    animationSpec: AnimationSpec<Color> = tween(450, easing = CubicBezierEasing(0.2f, 0.83f, 0.2f, 1f))
): LumiDesignTokens {
    val bg by animateColorAsState(target.bg, animationSpec, label = "tok_bg")
    val panel by animateColorAsState(target.panel, animationSpec, label = "tok_panel")
    val panel2 by animateColorAsState(target.panel2, animationSpec, label = "tok_panel2")
    val text by animateColorAsState(target.text, animationSpec, label = "tok_text")
    val muted by animateColorAsState(target.muted, animationSpec, label = "tok_muted")
    val dim by animateColorAsState(target.dim, animationSpec, label = "tok_dim")
    val line by animateColorAsState(target.line, animationSpec, label = "tok_line")
    val accent by animateColorAsState(target.accent, animationSpec, label = "tok_accent")
    val accentSoft by animateColorAsState(target.accentSoft, animationSpec, label = "tok_accentSoft")
    val peach by animateColorAsState(target.peach, animationSpec, label = "tok_peach")
    val rose by animateColorAsState(target.rose, animationSpec, label = "tok_rose")
    val mint by animateColorAsState(target.mint, animationSpec, label = "tok_mint")
    val gold by animateColorAsState(target.gold, animationSpec, label = "tok_gold")
    val nav by animateColorAsState(target.nav, animationSpec, label = "tok_nav")
    val glow by animateColorAsState(target.glow, animationSpec, label = "tok_glow")
    val field by animateColorAsState(target.field, animationSpec, label = "tok_field")
    val pill by animateColorAsState(target.pill, animationSpec, label = "tok_pill")
    val ringTrack by animateColorAsState(target.ringTrack, animationSpec, label = "tok_ringTrack")

    return remember(bg, panel, panel2, text, muted, dim, line, accent, accentSoft, peach, rose, mint, gold, nav, glow, field, pill, ringTrack, target.ringGradient, target.isDark) {
        LumiDesignTokens(
            bg = bg,
            panel = panel,
            panel2 = panel2,
            text = text,
            muted = muted,
            dim = dim,
            line = line,
            accent = accent,
            accentSoft = accentSoft,
            peach = peach,
            rose = rose,
            mint = mint,
            gold = gold,
            nav = nav,
            glow = glow,
            field = field,
            pill = pill,
            ringTrack = ringTrack,
            ringGradient = target.ringGradient,
            isDark = target.isDark
        )
    }
}

/**
 * Pulso de cambio de tema conectado según especificación HTML v11:
 * radial-gradient at 87% 14%, escala 0.55 -> 1.55, opacidad 0 -> 0.72 -> 0 en 720ms.
 */
@Composable
fun ConnectedThemePulse(
    accentColor: Color,
    pulseKey: Any?,
    modifier: Modifier = Modifier
) {
    var hasLaunchedOnce by remember { mutableStateOf(false) }
    var animating by remember { mutableStateOf(false) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(pulseKey) {
        if (!hasLaunchedOnce) {
            hasLaunchedOnce = true
            return@LaunchedEffect
        }
        animating = true
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 720, easing = CubicBezierEasing(0f, 0f, .58f, 1f))
        )
        animating = false
    }

    if (animating) {
        val p = animProgress.value
        val scale = lerp(0.55f, 1.55f, p)
        val opacity = if (p <= 0.38f) {
            lerp(0f, 0.72f, p / 0.38f)
        } else {
            lerp(0.72f, 0f, (p - 0.38f) / 0.62f)
        }

        val density = LocalDensity.current.density
        val config = LocalConfiguration.current
        val widthPx = config.screenWidthDp.toFloat() * density
        val heightPx = config.screenHeightDp.toFloat() * density
        val pulseCenter = Offset(widthPx * 0.87f, heightPx * 0.14f)

        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = opacity
                }
                .background(
                    Brush.radialGradient(
                        0f to accentColor.copy(alpha = 0.18f),
                        0.67f to Color.Transparent,
                        center = pulseCenter,
                        radius = kotlin.math.hypot(widthPx * 0.87f, heightPx * 0.86f) * 0.67f
                    )
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  LUMI THEME
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LumiTheme(
    selectedTheme        : AppThemePalette   = AppThemePalette.NOIR,
    selectedPhasePalette : PhaseColorPalette = PhaseColorPalette.AUTO_HARMONY,
    customPhaseColors    : PhaseColors?      = null,   // non-null cuando CUSTOM está guardado
    dialStyle            : DialStyle         = DialStyle.GRADIENT,
    selectedAmbient      : BackgroundAmbient = BackgroundAmbient.FLAT,
    content              : @Composable () -> Unit
) {
    val rawColorScheme = when (selectedTheme) {
        AppThemePalette.NOIR            -> NoirColorScheme
        AppThemePalette.CIRUELA         -> CiruelaColorScheme
        AppThemePalette.BOSQUE          -> BosqueColorScheme
        AppThemePalette.INDIGO          -> IndigoColorScheme
        AppThemePalette.PETALO          -> PetaloColorScheme
        AppThemePalette.MARFIL          -> MarfilColorScheme
        AppThemePalette.SALVIA          -> SalviaColorScheme
        AppThemePalette.LAVANDA         -> LavandaColorScheme
        AppThemePalette.LUMI_SPARK      -> LumiSparkColorScheme
        AppThemePalette.LUMI_SPARK_DARK -> LumiSparkDarkColorScheme
        AppThemePalette.LUMI_OCEAN      -> LumiOceanColorScheme
        AppThemePalette.LUMI_OCEAN_DARK -> IndigoColorScheme
        AppThemePalette.LUMI_FOREST     -> LumiForestColorScheme
        AppThemePalette.LUMI_FOREST_DARK -> BosqueColorScheme
        AppThemePalette.LUMI_SUNSET     -> LumiSunsetColorScheme
        AppThemePalette.LUMI_SUNSET_DARK -> LumiSunsetDarkColorScheme
        AppThemePalette.LUMI_DUNE       -> LumiDuneColorScheme
        AppThemePalette.LUMI_DUNE_DARK  -> LumiDuneDarkColorScheme
        AppThemePalette.LUMI_LAVENDER   -> LavandaColorScheme
        AppThemePalette.LUMI_LAVENDER_DARK -> LumiLavenderDarkColorScheme
        AppThemePalette.LUMI_SAGE       -> SalviaColorScheme
        AppThemePalette.LUMI_SAGE_DARK  -> LumiSageDarkColorScheme
        AppThemePalette.LUMI_EARTH      -> LumiEarthColorScheme
        AppThemePalette.LUMI_EARTH_DARK -> LumiEarthDarkColorScheme
        AppThemePalette.LUMI_SLATE      -> LumiSlateColorScheme
        AppThemePalette.LUMI_SLATE_DARK -> LumiSlateDarkColorScheme
        AppThemePalette.CYCLE_MENSTRUAL -> CycleMenstrualColorScheme
        AppThemePalette.CYCLE_MENSTRUAL_DARK -> CycleMenstrualDarkColorScheme
        AppThemePalette.CYCLE_OVULATION -> CycleOvulationColorScheme
        AppThemePalette.CYCLE_OVULATION_DARK -> CycleOvulationDarkColorScheme
        AppThemePalette.CACTUS     -> SalviaColorScheme
        AppThemePalette.HORTENSIA  -> HortensiaColorScheme
        AppThemePalette.TIERRA     -> TierraColorScheme
        AppThemePalette.LUXE       -> LuxeColorScheme
        AppThemePalette.PIEDRA     -> PiedraColorScheme
        AppThemePalette.MEDIANOCHE -> MedianocheColorScheme
        AppThemePalette.COSMOS     -> CosmosColorScheme
        AppThemePalette.CARBONO    -> CarbonoColorScheme
        AppThemePalette.ECLIPSE    -> EclipseColorScheme
        AppThemePalette.NOCHE_ROSA -> NocheRosaColorScheme
        AppThemePalette.OBSIDIANA  -> ObsidianaColorScheme
        AppThemePalette.VINO       -> CiruelaColorScheme
        AppThemePalette.FORJA      -> ForjaColorScheme
        AppThemePalette.AMBAR      -> AmbarColorScheme
        AppThemePalette.PAPEL      -> PapelColorScheme
        AppThemePalette.NIEBLA     -> NieblaColorScheme
        AppThemePalette.SAL        -> SalColorScheme
        AppThemePalette.ARENA      -> MarfilColorScheme
        AppThemePalette.PIZARRA    -> PizarraColorScheme
        AppThemePalette.HUMO       -> HumoColorScheme
        AppThemePalette.LIENZO     -> LienzoColorScheme
        AppThemePalette.CENIZA     -> CenizaColorScheme
    }

    val animatedColorScheme = rawColorScheme // v18: sin colores intermedios ni desincronización con la revelación CSS
    val isDark = selectedTheme.isDark()

    // Resolver qué colores de fase usar:
    //   1. Si el preset activo es CUSTOM y hay un set guardado → usarlo
    //   2. Si no → resolver desde el preset elegido (DEFAULT, PASTEL, etc.)
    val phaseColors =
        if (selectedPhasePalette == PhaseColorPalette.CUSTOM && customPhaseColors != null) {
            customPhaseColors
        } else {
            selectedPhasePalette.toPhaseColors(isDark, selectedTheme)
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = animatedColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !isDark
        }
    }

    // Definir el gradiente de marca
    val brandGradient = when (selectedTheme) {
        AppThemePalette.LUMI_SPARK, AppThemePalette.LUMI_SPARK_DARK -> Brush.horizontalGradient(listOf(Color(0xFF9B72C0), Color(0xFFE8A0C0)))
        AppThemePalette.LUMI_OCEAN, AppThemePalette.LUMI_OCEAN_DARK -> Brush.horizontalGradient(listOf(Color(0xFF3A7BD5), Color(0xFF00D2FF)))
        AppThemePalette.LUMI_FOREST, AppThemePalette.LUMI_FOREST_DARK -> Brush.horizontalGradient(listOf(Color(0xFF1D976C), Color(0xFF93F9B9)))
        AppThemePalette.LUMI_SUNSET, AppThemePalette.LUMI_SUNSET_DARK -> Brush.horizontalGradient(listOf(Color(0xFFF12711), Color(0xFFF5AF19)))
        AppThemePalette.LUMI_DUNE, AppThemePalette.LUMI_DUNE_DARK -> Brush.horizontalGradient(listOf(Color(0xFFB78628), Color(0xFFFCC201)))
        AppThemePalette.LUMI_LAVENDER, AppThemePalette.LUMI_LAVENDER_DARK -> Brush.horizontalGradient(listOf(Color(0xFF8C72CB), Color(0xFFC0A0E8)))
        AppThemePalette.LUMI_SAGE, AppThemePalette.LUMI_SAGE_DARK -> Brush.horizontalGradient(listOf(Color(0xFF6A9B82), Color(0xFF90C2A8)))
        AppThemePalette.LUMI_EARTH, AppThemePalette.LUMI_EARTH_DARK -> Brush.horizontalGradient(listOf(Color(0xFFA6856A), Color(0xFFC4A68C)))
        AppThemePalette.LUMI_SLATE, AppThemePalette.LUMI_SLATE_DARK -> Brush.horizontalGradient(listOf(Color(0xFF6B7B8C), Color(0xFF94A4B4)))
        AppThemePalette.CYCLE_MENSTRUAL, AppThemePalette.CYCLE_MENSTRUAL_DARK -> Brush.horizontalGradient(listOf(Color(0xFFC05C7A), Color(0xFFE08BA0)))
        AppThemePalette.CYCLE_OVULATION, AppThemePalette.CYCLE_OVULATION_DARK -> Brush.horizontalGradient(listOf(Color(0xFF4A90A4), Color(0xFF75BCCF)))
        else -> null
    }

    val brandBackgroundGradient = when (selectedTheme) {
        AppThemePalette.LUMI_SPARK -> Brush.verticalGradient(listOf(Color(0xFF9B72C0).copy(alpha = 0.08f), Color(0xFFE8A0C0).copy(alpha = 0.04f), Color.Transparent))
        AppThemePalette.LUMI_SPARK_DARK -> Brush.verticalGradient(listOf(Color(0xFF9B72C0).copy(alpha = 0.35f), Color(0xFFE8A0C0).copy(alpha = 0.20f), Color.Transparent))
        AppThemePalette.LUMI_OCEAN -> Brush.verticalGradient(listOf(Color(0xFF3A7BD5).copy(alpha = 0.08f), Color(0xFF00D2FF).copy(alpha = 0.04f), Color.Transparent))
        AppThemePalette.LUMI_OCEAN_DARK -> Brush.verticalGradient(listOf(Color(0xFF3A7BD5).copy(alpha = 0.30f), Color(0xFF00D2FF).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_FOREST -> Brush.verticalGradient(listOf(Color(0xFF1D976C).copy(alpha = 0.08f), Color(0xFF93F9B9).copy(alpha = 0.04f), Color.Transparent))
        AppThemePalette.LUMI_FOREST_DARK -> Brush.verticalGradient(listOf(Color(0xFF1D976C).copy(alpha = 0.30f), Color(0xFF93F9B9).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_SUNSET -> Brush.verticalGradient(listOf(Color(0xFFF12711).copy(alpha = 0.08f), Color(0xFFF5AF19).copy(alpha = 0.04f), Color.Transparent))
        AppThemePalette.LUMI_SUNSET_DARK -> Brush.verticalGradient(listOf(Color(0xFFF12711).copy(alpha = 0.25f), Color(0xFFF5AF19).copy(alpha = 0.12f), Color.Transparent))
        AppThemePalette.LUMI_DUNE -> Brush.verticalGradient(listOf(Color(0xFFB78628).copy(alpha = 0.08f), Color(0xFFFCC201).copy(alpha = 0.04f), Color.Transparent))
        AppThemePalette.LUMI_DUNE_DARK -> Brush.verticalGradient(listOf(Color(0xFFB78628).copy(alpha = 0.30f), Color(0xFFFCC201).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_LAVENDER -> Brush.verticalGradient(listOf(Color(0xFF8C72CB).copy(alpha = 0.08f), Color(0xFFC0A0E8).copy(alpha = 0.04f), Color.Transparent))
        AppThemePalette.LUMI_LAVENDER_DARK -> Brush.verticalGradient(listOf(Color(0xFF8C72CB).copy(alpha = 0.30f), Color(0xFFC0A0E8).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_SAGE -> Brush.verticalGradient(listOf(Color(0xFF6A9B82).copy(alpha = 0.08f), Color(0xFF90C2A8).copy(alpha = 0.04f), Color.Transparent))
        AppThemePalette.LUMI_SAGE_DARK -> Brush.verticalGradient(listOf(Color(0xFF6A9B82).copy(alpha = 0.30f), Color(0xFF90C2A8).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_EARTH -> Brush.verticalGradient(listOf(Color(0xFFA6856A).copy(alpha = 0.08f), Color(0xFFC4A68C).copy(alpha = 0.04f), Color.Transparent))
        AppThemePalette.LUMI_EARTH_DARK -> Brush.verticalGradient(listOf(Color(0xFFA6856A).copy(alpha = 0.30f), Color(0xFFC4A68C).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_SLATE -> Brush.verticalGradient(listOf(Color(0xFF6B7B8C).copy(alpha = 0.08f), Color(0xFF94A4B4).copy(alpha = 0.04f), Color.Transparent))
        AppThemePalette.LUMI_SLATE_DARK -> Brush.verticalGradient(listOf(Color(0xFF6B7B8C).copy(alpha = 0.30f), Color(0xFF94A4B4).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.CYCLE_MENSTRUAL -> Brush.verticalGradient(listOf(Color(0xFFC05C7A).copy(alpha = 0.08f), Color(0xFFE08BA0).copy(alpha = 0.04f), Color.Transparent))
        AppThemePalette.CYCLE_MENSTRUAL_DARK -> Brush.verticalGradient(listOf(Color(0xFFC05C7A).copy(alpha = 0.30f), Color(0xFFE08BA0).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.CYCLE_OVULATION -> Brush.verticalGradient(listOf(Color(0xFF4A90A4).copy(alpha = 0.08f), Color(0xFF75BCCF).copy(alpha = 0.04f), Color.Transparent))
        AppThemePalette.CYCLE_OVULATION_DARK -> Brush.verticalGradient(listOf(Color(0xFF4A90A4).copy(alpha = 0.30f), Color(0xFF75BCCF).copy(alpha = 0.15f), Color.Transparent))
        else -> null
    }

    // Resolver tokens de diseño semánticos unificados del prototipo
    val rawLumiTokens = resolveLumiTokens(selectedTheme)
    val animatedLumiTokens = rawLumiTokens // v18: tokens inmediatos, también en cambio oscuro↔claro

    // Los colores semánticos SIEMPRE son DEFAULT (rojo/teal/morado/ámbar).
    val semanticPhaseColors = selectedPhasePalette.toSemanticPhaseColors(isDark)

    CompositionLocalProvider(
        LocalIsDarkTheme provides isDark,
        LocalActiveLumiTheme provides selectedTheme,
        LocalLumiTokens provides animatedLumiTokens,
        LocalPhaseColors provides phaseColors,
        LocalSemanticPhaseColors provides semanticPhaseColors,
        LocalDialStyle provides dialStyle,
        LocalBrandGradient provides brandGradient,
        LocalBrandBackgroundGradient provides brandBackgroundGradient,
        LocalBackgroundAmbient provides selectedAmbient
    ) {
        MaterialTheme(colorScheme = animatedColorScheme, typography = Typography) {
            Box(Modifier.fillMaxSize()) {
                content()
                // El pulso solo pertenece al Home. Onboarding usa revelación circular de 690 ms.
            }
        }
    }
}