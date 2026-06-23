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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────
//  ENUM DE TEMAS
// ─────────────────────────────────────────────
enum class AppThemePalette {
    // Claras
    LAVANDA, CACTUS, HORTENSIA, TIERRA, LUXE, PETALO, PIEDRA,
    // Oscuras
    MEDIANOCHE, COSMOS, CARBONO, ECLIPSE, NOCHE_ROSA, OBSIDIANA, VINO, FORJA, AMBAR, NOIR,
    // Minimalistas
    PAPEL, NIEBLA, SAL, ARENA, PIZARRA, HUMO, LIENZO, CENIZA;

    fun isDark(): Boolean = this in setOf(
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

// ─────────────────────────────────────────────────────────────────────────────
//  LUMI THEME
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LumiTheme(
    selectedTheme        : AppThemePalette   = AppThemePalette.CACTUS,
    selectedPhasePalette : PhaseColorPalette = PhaseColorPalette.DEFAULT,
    customPhaseColors    : PhaseColors?      = null,   // non-null cuando CUSTOM está guardado
    content              : @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
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
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides isDark,
        LocalPhaseColors provides phaseColors
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}