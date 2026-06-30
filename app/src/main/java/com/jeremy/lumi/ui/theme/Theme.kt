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

// ─────────────────────────────────────────────
//  ENUM DE TEMAS
// ─────────────────────────────────────────────
enum class AppThemePalette {
    // Oficial
    LUMI_SPARK, LUMI_SPARK_DARK,
    // Nuevas Gradientes
    LUMI_OCEAN, LUMI_OCEAN_DARK, LUMI_FOREST, LUMI_FOREST_DARK, LUMI_SUNSET, LUMI_SUNSET_DARK, LUMI_DUNE, LUMI_DUNE_DARK,
    LUMI_LAVENDER, LUMI_LAVENDER_DARK, LUMI_SAGE, LUMI_SAGE_DARK, LUMI_EARTH, LUMI_EARTH_DARK, LUMI_SLATE, LUMI_SLATE_DARK,
    CYCLE_MENSTRUAL, CYCLE_MENSTRUAL_DARK, CYCLE_OVULATION, CYCLE_OVULATION_DARK,
    // Claras
    LAVANDA, CACTUS, HORTENSIA, TIERRA, LUXE, PETALO, PIEDRA,
    // Oscuras
    MEDIANOCHE, COSMOS, CARBONO, ECLIPSE, NOCHE_ROSA, OBSIDIANA, VINO, FORJA, AMBAR, NOIR,
    // Minimalistas
    PAPEL, NIEBLA, SAL, ARENA, PIZARRA, HUMO, LIENZO, CENIZA;

    fun isDark(): Boolean = this in setOf(
        LUMI_SPARK_DARK, LUMI_OCEAN_DARK, LUMI_FOREST_DARK, LUMI_SUNSET_DARK, LUMI_DUNE_DARK,
        LUMI_LAVENDER_DARK, LUMI_SAGE_DARK, LUMI_EARTH_DARK, LUMI_SLATE_DARK,
        CYCLE_MENSTRUAL_DARK, CYCLE_OVULATION_DARK,
        MEDIANOCHE, COSMOS, CARBONO, ECLIPSE, NOCHE_ROSA, OBSIDIANA, VINO, FORJA, AMBAR, NOIR
    )
}

// ── Noir · Negro minimalista puro (Instagram/X dark style) ───────────────────
// A diferencia de Carbono (#111111, con acentos rojos) o Medianoche (índigo),
// Noir es deliberadamente neutro: negro absoluto, blanco/gris sin tinte de color.
val NoirPrimary    = Color(0xFFE8E8E8)   // Casi blanco, sin calidez ni frialdad
val NoirSecondary  = Color(0xFF8A8A8E)   // Gris medio iOS-like
val NoirTertiary   = Color(0xFF636366)   // Gris oscuro neutro
val NoirBackground = Color(0xFF000000)   // Negro absoluto
val NoirSurface    = Color(0xFF1C1C1E)   // Superficie estándar iOS dark
val NoirOnPrimary  = Color(0xFF000000)
val NoirOnBg       = Color(0xFFFFFFFF)

private val NoirColorScheme = darkColorScheme(
    primary          = NoirPrimary,
    onPrimary        = NoirOnPrimary,
    primaryContainer = Color(0xFF2C2C2E),
    secondary        = NoirSecondary,
    onSecondary      = NoirBackground,
    tertiary         = NoirTertiary,
    background       = NoirBackground,
    onBackground     = NoirOnBg,
    surface          = NoirSurface,
    onSurface        = NoirOnBg,
    surfaceVariant   = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    outline          = Color(0xFF38383A)
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

private val LavandaColorScheme = lightColorScheme(
    primary = LavandaPrimary, onPrimary = LavandaOnPrimary, primaryContainer = Color(0xFFE8D8F5),
    secondary = LavandaSecondary, onSecondary = LavandaOnBg, tertiary = LavandaTertiary,
    background = LavandaBackground, onBackground = LavandaOnBg,
    surface = LavandaSurface, onSurface = LavandaOnBg,
    surfaceVariant = Color(0xFFEEE6F5), onSurfaceVariant = LavandaOnBg, outline = Color(0xFFCDB8D8)
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
private val PetaloColorScheme = lightColorScheme(
    primary = PetaloPrimary, onPrimary = PetaloOnPrimary, primaryContainer = Color(0xFFFFE0EA),
    secondary = PetaloSecondary, onSecondary = PetaloOnBg, tertiary = PetaloTertiary,
    background = PetaloBackground, onBackground = PetaloOnBg,
    surface = PetaloSurface, onSurface = PetaloOnBg,
    surfaceVariant = Color(0xFFF5ECF0), onSurfaceVariant = PetaloOnBg, outline = Color(0xFFDDCCD4)
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

val LocalIsDarkTheme = compositionLocalOf { false }

// Expone los colores de fase activos a cualquier composable sin pasarlos como
// parámetro. Se alimenta en LumiTheme con: preset elegido, o el set custom de
// la usuaria cuando el preset activo es CUSTOM.
val LocalPhaseColors = compositionLocalOf { PhaseColorsDefaultLight }

// Expone un gradiente de marca activo para componentes que deban destacar.
// Será null si el tema activo no es Lumi Spark oficial.
val LocalBrandGradient = compositionLocalOf<Brush?> { null }

// Expone un gradiente sutil para el fondo general de la app.
val LocalBrandBackgroundGradient = compositionLocalOf<Brush?> { null }

// ─────────────────────────────────────────────────────────────────────────────
//  LUMI THEME
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LumiTheme(
    selectedTheme        : AppThemePalette   = AppThemePalette.LUMI_SPARK_DARK,
    selectedPhasePalette : PhaseColorPalette = PhaseColorPalette.DEFAULT,
    customPhaseColors    : PhaseColors?      = null,   // non-null cuando CUSTOM está guardado
    content              : @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        AppThemePalette.LUMI_SPARK      -> LumiSparkColorScheme
        AppThemePalette.LUMI_SPARK_DARK -> LumiSparkDarkColorScheme
        AppThemePalette.LUMI_OCEAN      -> LumiOceanColorScheme
        AppThemePalette.LUMI_OCEAN_DARK -> LumiOceanDarkColorScheme
        AppThemePalette.LUMI_FOREST     -> LumiForestColorScheme
        AppThemePalette.LUMI_FOREST_DARK -> LumiForestDarkColorScheme
        AppThemePalette.LUMI_SUNSET     -> LumiSunsetColorScheme
        AppThemePalette.LUMI_SUNSET_DARK -> LumiSunsetDarkColorScheme
        AppThemePalette.LUMI_DUNE       -> LumiDuneColorScheme
        AppThemePalette.LUMI_DUNE_DARK  -> LumiDuneDarkColorScheme
        AppThemePalette.LUMI_LAVENDER   -> LumiLavenderColorScheme
        AppThemePalette.LUMI_LAVENDER_DARK -> LumiLavenderDarkColorScheme
        AppThemePalette.LUMI_SAGE       -> LumiSageColorScheme
        AppThemePalette.LUMI_SAGE_DARK  -> LumiSageDarkColorScheme
        AppThemePalette.LUMI_EARTH      -> LumiEarthColorScheme
        AppThemePalette.LUMI_EARTH_DARK -> LumiEarthDarkColorScheme
        AppThemePalette.LUMI_SLATE      -> LumiSlateColorScheme
        AppThemePalette.LUMI_SLATE_DARK -> LumiSlateDarkColorScheme
        AppThemePalette.CYCLE_MENSTRUAL -> CycleMenstrualColorScheme
        AppThemePalette.CYCLE_MENSTRUAL_DARK -> CycleMenstrualDarkColorScheme
        AppThemePalette.CYCLE_OVULATION -> CycleOvulationColorScheme
        AppThemePalette.CYCLE_OVULATION_DARK -> CycleOvulationDarkColorScheme
        AppThemePalette.LAVANDA    -> LavandaColorScheme
        AppThemePalette.CACTUS     -> CactusColorScheme
        AppThemePalette.HORTENSIA  -> HortensiaColorScheme
        AppThemePalette.TIERRA     -> TierraColorScheme
        AppThemePalette.LUXE       -> LuxeColorScheme
        AppThemePalette.PETALO     -> PetaloColorScheme
        AppThemePalette.PIEDRA     -> PiedraColorScheme
        AppThemePalette.MEDIANOCHE -> MedianocheColorScheme
        AppThemePalette.COSMOS     -> CosmosColorScheme
        AppThemePalette.CARBONO    -> CarbonoColorScheme
        AppThemePalette.ECLIPSE    -> EclipseColorScheme
        AppThemePalette.NOCHE_ROSA -> NocheRosaColorScheme
        AppThemePalette.OBSIDIANA  -> ObsidianaColorScheme
        AppThemePalette.VINO       -> VinoColorScheme
        AppThemePalette.FORJA      -> ForjaColorScheme
        AppThemePalette.AMBAR      -> AmbarColorScheme
        AppThemePalette.NOIR       -> NoirColorScheme
        AppThemePalette.PAPEL      -> PapelColorScheme
        AppThemePalette.NIEBLA     -> NieblaColorScheme
        AppThemePalette.SAL        -> SalColorScheme
        AppThemePalette.ARENA      -> ArenaColorScheme
        AppThemePalette.PIZARRA    -> PizarraColorScheme
        AppThemePalette.HUMO       -> HumoColorScheme
        AppThemePalette.LIENZO     -> LienzoColorScheme
        AppThemePalette.CENIZA     -> CenizaColorScheme
    }

    val isDark = selectedTheme.isDark()

    // Resolver qué colores de fase usar:
    //   1. Si el preset activo es CUSTOM y hay un set guardado → usarlo
    //   2. Si no → resolver desde el preset elegido (DEFAULT, PASTEL, etc.)
    val phaseColors =
        if (selectedPhasePalette == PhaseColorPalette.CUSTOM && customPhaseColors != null) {
            customPhaseColors
        } else {
            selectedPhasePalette.toPhaseColors(isDark)
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
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
        AppThemePalette.LUMI_SPARK -> Brush.verticalGradient(listOf(Color(0xFF9B72C0).copy(alpha = 0.35f), Color(0xFFE8A0C0).copy(alpha = 0.25f), Color.Transparent))
        AppThemePalette.LUMI_SPARK_DARK -> Brush.verticalGradient(listOf(Color(0xFF9B72C0).copy(alpha = 0.45f), Color(0xFFE8A0C0).copy(alpha = 0.30f), Color.Transparent))
        AppThemePalette.LUMI_OCEAN -> Brush.verticalGradient(listOf(Color(0xFF3A7BD5).copy(alpha = 0.25f), Color(0xFF00D2FF).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_OCEAN_DARK -> Brush.verticalGradient(listOf(Color(0xFF3A7BD5).copy(alpha = 0.35f), Color(0xFF00D2FF).copy(alpha = 0.20f), Color.Transparent))
        AppThemePalette.LUMI_FOREST -> Brush.verticalGradient(listOf(Color(0xFF1D976C).copy(alpha = 0.25f), Color(0xFF93F9B9).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_FOREST_DARK -> Brush.verticalGradient(listOf(Color(0xFF1D976C).copy(alpha = 0.35f), Color(0xFF93F9B9).copy(alpha = 0.20f), Color.Transparent))
        AppThemePalette.LUMI_SUNSET -> Brush.verticalGradient(listOf(Color(0xFFF12711).copy(alpha = 0.20f), Color(0xFFF5AF19).copy(alpha = 0.10f), Color.Transparent))
        AppThemePalette.LUMI_SUNSET_DARK -> Brush.verticalGradient(listOf(Color(0xFFF12711).copy(alpha = 0.30f), Color(0xFFF5AF19).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_DUNE -> Brush.verticalGradient(listOf(Color(0xFFB78628).copy(alpha = 0.25f), Color(0xFFFCC201).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_DUNE_DARK -> Brush.verticalGradient(listOf(Color(0xFFB78628).copy(alpha = 0.35f), Color(0xFFFCC201).copy(alpha = 0.20f), Color.Transparent))
        AppThemePalette.LUMI_LAVENDER -> Brush.verticalGradient(listOf(Color(0xFF8C72CB).copy(alpha = 0.25f), Color(0xFFC0A0E8).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_LAVENDER_DARK -> Brush.verticalGradient(listOf(Color(0xFF8C72CB).copy(alpha = 0.35f), Color(0xFFC0A0E8).copy(alpha = 0.20f), Color.Transparent))
        AppThemePalette.LUMI_SAGE -> Brush.verticalGradient(listOf(Color(0xFF6A9B82).copy(alpha = 0.25f), Color(0xFF90C2A8).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_SAGE_DARK -> Brush.verticalGradient(listOf(Color(0xFF6A9B82).copy(alpha = 0.35f), Color(0xFF90C2A8).copy(alpha = 0.20f), Color.Transparent))
        AppThemePalette.LUMI_EARTH -> Brush.verticalGradient(listOf(Color(0xFFA6856A).copy(alpha = 0.25f), Color(0xFFC4A68C).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_EARTH_DARK -> Brush.verticalGradient(listOf(Color(0xFFA6856A).copy(alpha = 0.35f), Color(0xFFC4A68C).copy(alpha = 0.20f), Color.Transparent))
        AppThemePalette.LUMI_SLATE -> Brush.verticalGradient(listOf(Color(0xFF6B7B8C).copy(alpha = 0.25f), Color(0xFF94A4B4).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.LUMI_SLATE_DARK -> Brush.verticalGradient(listOf(Color(0xFF6B7B8C).copy(alpha = 0.35f), Color(0xFF94A4B4).copy(alpha = 0.20f), Color.Transparent))
        AppThemePalette.CYCLE_MENSTRUAL -> Brush.verticalGradient(listOf(Color(0xFFC05C7A).copy(alpha = 0.25f), Color(0xFFE08BA0).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.CYCLE_MENSTRUAL_DARK -> Brush.verticalGradient(listOf(Color(0xFFC05C7A).copy(alpha = 0.35f), Color(0xFFE08BA0).copy(alpha = 0.20f), Color.Transparent))
        AppThemePalette.CYCLE_OVULATION -> Brush.verticalGradient(listOf(Color(0xFF4A90A4).copy(alpha = 0.25f), Color(0xFF75BCCF).copy(alpha = 0.15f), Color.Transparent))
        AppThemePalette.CYCLE_OVULATION_DARK -> Brush.verticalGradient(listOf(Color(0xFF4A90A4).copy(alpha = 0.35f), Color(0xFF75BCCF).copy(alpha = 0.20f), Color.Transparent))
        else -> null
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides isDark,
        LocalPhaseColors provides phaseColors,
        LocalBrandGradient provides brandGradient,
        LocalBrandBackgroundGradient provides brandBackgroundGradient
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}