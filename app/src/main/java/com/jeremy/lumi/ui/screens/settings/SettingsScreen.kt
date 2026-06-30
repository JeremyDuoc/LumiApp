package com.jeremy.lumi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeremy.lumi.R
import com.jeremy.lumi.data.preferences.PhaseSlot
import com.jeremy.lumi.ui.theme.*
import com.jeremy.lumi.ui.components.CupertinoSwitch
import com.jeremy.lumi.ui.components.CupertinoActionSheet
import com.jeremy.lumi.ui.components.CupertinoActionSheetButton
import com.jeremy.lumi.ui.components.CupertinoActionSheetCancelButton

// â”€â”€ Modelo de dato para cada opción de tema â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private data class ThemeOption(
    val palette : AppThemePalette,
    val name    : String,
    val color   : Color,
    val isDark  : Boolean = false,
    val darkBg  : Color   = Color(0xFF1A1A2E)
)

// â”€â”€ Listas por grupo â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private val clarasOptions = listOf(
    ThemeOption(AppThemePalette.LUMI_SPARK, "Lumi Spark", LumiSparkPrimary),
    ThemeOption(AppThemePalette.LUMI_OCEAN, "Lumi Océano", LumiOceanPrimary),
    ThemeOption(AppThemePalette.LUMI_FOREST, "Lumi Bosque", LumiForestPrimary),
    ThemeOption(AppThemePalette.LUMI_SUNSET, "Lumi Ocaso", LumiSunsetPrimary),
    ThemeOption(AppThemePalette.LUMI_DUNE, "Lumi Duna", LumiDunePrimary),
    ThemeOption(AppThemePalette.LAVANDA,   "Lavanda",   LavandaPrimary),
    ThemeOption(AppThemePalette.CACTUS,    "Cactus",    CactusPrimary),
    ThemeOption(AppThemePalette.HORTENSIA, "Hortensia", HortensiaPrimary),
    ThemeOption(AppThemePalette.TIERRA,    "Tierra",    TierraPrimary),
    ThemeOption(AppThemePalette.LUXE,      "Luxe",      LuxePrimary),
    ThemeOption(AppThemePalette.PETALO,    "Pétalo",    PetaloPrimary),
    ThemeOption(AppThemePalette.PIEDRA,    "Piedra",    PiedraPrimary),
)

private val oscurasOptions = listOf(
    ThemeOption(AppThemePalette.LUMI_SPARK_DARK, "Lumi Spark Oscuro", LumiSparkDarkPrimary, isDark = true, darkBg = Color(0xFF1A161C)),
    ThemeOption(AppThemePalette.LUMI_OCEAN_DARK, "Lumi Océano Oscuro", LumiOceanDarkPrimary, isDark = true, darkBg = Color(0xFF070F1A)),
    ThemeOption(AppThemePalette.LUMI_FOREST_DARK, "Lumi Bosque Oscuro", LumiForestDarkPrimary, isDark = true, darkBg = Color(0xFF08120D)),
    ThemeOption(AppThemePalette.LUMI_SUNSET_DARK, "Lumi Ocaso Oscuro", LumiSunsetDarkPrimary, isDark = true, darkBg = Color(0xFF140805)),
    ThemeOption(AppThemePalette.LUMI_DUNE_DARK, "Lumi Duna Oscura", LumiDuneDarkPrimary, isDark = true, darkBg = Color(0xFF120E08)),
    ThemeOption(AppThemePalette.MEDIANOCHE, "Medianoche", MedianochePrimary, isDark = true, darkBg = Color(0xFF13101F)),
    ThemeOption(AppThemePalette.COSMOS,     "Cosmos",     CosmosPrimary,     isDark = true, darkBg = Color(0xFF080E1C)),
    ThemeOption(AppThemePalette.CARBONO,    "Carbono",    CarbonoPrimary,    isDark = true, darkBg = Color(0xFF111111)),
    ThemeOption(AppThemePalette.ECLIPSE,    "Eclipse",    EclipsePrimary,    isDark = true, darkBg = Color(0xFF160E0F)),
    ThemeOption(AppThemePalette.NOCHE_ROSA, "Noche Rosa", NochePrimary,      isDark = true, darkBg = Color(0xFF1A1318)),
    ThemeOption(AppThemePalette.OBSIDIANA,  "Obsidiana",  ObsidianaPrimary,  isDark = true, darkBg = Color(0xFF0A0F0F)),
    ThemeOption(AppThemePalette.VINO,       "Vino",       VinoPrimary,       isDark = true, darkBg = Color(0xFF120A10)),
    ThemeOption(AppThemePalette.FORJA,      "Forja",      ForjaPrimary,      isDark = true, darkBg = Color(0xFF0F1318)),
    ThemeOption(AppThemePalette.AMBAR,      "Ámbar",      AmbarPrimary,      isDark = true, darkBg = Color(0xFF120E08)),
    ThemeOption(AppThemePalette.NOIR,       "Noir",       NoirPrimary,       isDark = true, darkBg = Color(0xFF000000)),
)

private val minimalistasOptions = listOf(
    ThemeOption(AppThemePalette.PAPEL,   "Papel",   PapelPrimary),
    ThemeOption(AppThemePalette.NIEBLA,  "Niebla",  NieblaPrimary),
    ThemeOption(AppThemePalette.SAL,     "Sal",     SalPrimary),
    ThemeOption(AppThemePalette.ARENA,   "Arena",   ArenaPrimary),
    ThemeOption(AppThemePalette.PIZARRA, "Pizarra", PizarraPrimary),
    ThemeOption(AppThemePalette.HUMO,    "Humo",    HumoPrimary),
    ThemeOption(AppThemePalette.LIENZO,  "Lienzo",  LienzoPrimary),
    ThemeOption(AppThemePalette.CENIZA,  "Ceniza",  CenizaPrimary),
)

// â”€â”€ Modelo de dato para cada preset de colores de fase â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private data class PhasePaletteOption(
    val palette: PhaseColorPalette,
    val name   : String
)

private val phasePaletteOptions = listOf(
    PhasePaletteOption(PhaseColorPalette.DEFAULT,     "Original"),
    PhasePaletteOption(PhaseColorPalette.OCEANO_GRADIENT, "Lumi Océano"),
    PhasePaletteOption(PhaseColorPalette.BOSQUE_GRADIENT, "Lumi Bosque"),
    PhasePaletteOption(PhaseColorPalette.OCASO_GRADIENT,  "Lumi Ocaso"),
    PhasePaletteOption(PhaseColorPalette.DUNA_GRADIENT,   "Lumi Duna"),
    PhasePaletteOption(PhaseColorPalette.PASTEL,      "Pastel"),
    PhasePaletteOption(PhaseColorPalette.MINIMALISTA, "Minimalista"),
    PhasePaletteOption(PhaseColorPalette.VIVID,       "Vívido"),
    PhasePaletteOption(PhaseColorPalette.TIERRA,      "Tierra"),
    PhasePaletteOption(PhaseColorPalette.OCEANO,      "Océano Frío"),
    PhasePaletteOption(PhaseColorPalette.MONOCROMO,   "Monocromo"),
)

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  SETTINGS SCREEN
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val currentTheme        by viewModel.currentTheme.collectAsStateWithLifecycle()
    val currentPhasePalette by viewModel.currentPhasePalette.collectAsStateWithLifecycle()
    val customPhaseColors   by viewModel.customPhaseColors.collectAsStateWithLifecycle()
    val isDark = currentTheme.isDark()
    val saveReminders by viewModel.saveRemindersInChat.collectAsStateWithLifecycle()
    val userGoal by viewModel.userGoal.collectAsStateWithLifecycle()
    val isPregnant by viewModel.isPregnant.collectAsStateWithLifecycle()

    val customPhaseColorsVal = customPhaseColors
    val activePhaseColors = if (currentPhasePalette == PhaseColorPalette.CUSTOM && customPhaseColorsVal != null) {
        customPhaseColorsVal
    } else {
        currentPhasePalette.toPhaseColors(isDark)
    }

    var showThemeSheet by remember { mutableStateOf(false) }
    var showPhaseColorSheet by remember { mutableStateOf(false) }
    var showGoalSheet by remember { mutableStateOf(false) }
    var showPregnancyEndDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupManager = remember { com.jeremy.lumi.data.backup.BackupManager(context) }
    
    val isHealthConnectEnabled by viewModel.isHealthConnectEnabled.collectAsStateWithLifecycle()
    val healthConnectLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        // FIX P3-2: Usar la propiedad expuesta del ViewModel en lugar del manager público.
        if (granted.containsAll(viewModel.healthConnectPermissions)) {
            viewModel.setIsHealthConnectEnabled(true)
        } else {
            viewModel.setIsHealthConnectEnabled(false)
            android.widget.Toast.makeText(context, "Permisos denegados", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                context.contentResolver.openOutputStream(it)?.let { os ->
                    val result = backupManager.exportDatabaseToZip(os)
                    val msg = if (result.isSuccess) "Copia de seguridad guardada con éxito." else "Error al guardar."
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                context.contentResolver.openInputStream(it)?.let { input ->
                    val result = backupManager.importDatabaseFromZip(input)
                    val msg = if (result.isSuccess) "Datos restaurados. Por favor, reinicia la app." else "Error al restaurar."
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.nav_settings), fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // â”€â”€ SALUD Y OBJETIVOS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            SettingsSectionTitle(stringResource(R.string.settings_health_goals))
            SettingsCard {
                SettingsItem(
                    icon = Icons.Rounded.Favorite,
                    title = stringResource(R.string.settings_user_goal),
                    subtitle = when (userGoal) {
                        com.jeremy.lumi.domain.model.UserGoal.TRACK_CYCLE -> stringResource(R.string.settings_goal_track)
                        com.jeremy.lumi.domain.model.UserGoal.AVOID_PREGNANCY -> stringResource(R.string.settings_goal_avoid)
                        com.jeremy.lumi.domain.model.UserGoal.SEEK_PREGNANCY -> stringResource(R.string.settings_goal_seek)
                        com.jeremy.lumi.domain.model.UserGoal.HEALTH_MONITORING -> stringResource(R.string.settings_goal_health)
                        else -> ""
                    },
                    onClick = { showGoalSheet = true }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                SettingsSwitchItem(
                    icon = Icons.Rounded.Favorite, // Un ícono apropiado temporal
                    title = stringResource(R.string.settings_pregnant),
                    subtitle = stringResource(R.string.settings_pregnant_desc),
                    checked = isPregnant,
                    onCheckedChange = { checked ->
                        if (!checked && isPregnant) {
                            showPregnancyEndDialog = true
                        } else {
                            viewModel.setIsPregnant(checked)
                        }
                    }
                )
                
                val isOnContraceptive by viewModel.isOnContraceptive.collectAsStateWithLifecycle()
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                SettingsSwitchItem(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.settings_contraceptive_title),
                    subtitle = stringResource(R.string.settings_contraceptive_desc),
                    checked = isOnContraceptive,
                    onCheckedChange = { viewModel.setIsOnContraceptive(it) }
                )
                
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                SettingsSwitchItem(
                    icon = Icons.Rounded.Favorite,
                    title = "Sincronizar con Health Connect",
                    subtitle = "Importa temperatura, sueño, pasos y ritmo cardíaco",
                    checked = isHealthConnectEnabled,
                    onCheckedChange = { checked -> 
                        if (checked) {
                            // FIX P3-2: Usar propiedades del ViewModel en lugar del manager público.
                            if (viewModel.isHealthConnectAvailable) {
                                healthConnectLauncher.launch(viewModel.healthConnectPermissions)
                            } else {
                                android.widget.Toast.makeText(context, "Health Connect no disponible en este dispositivo", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            viewModel.setIsHealthConnectEnabled(false)
                        }
                    }
                )
            }

            // â”€â”€ APARIENCIA Y TEMA â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            SettingsSectionTitle("Apariencia")
            SettingsCard {
                SettingsItem(
                    icon = Icons.Rounded.Palette,
                    title = "Tema General",
                    subtitle = currentTheme.name,
                    onClick = { showThemeSheet = true }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                SettingsItem(
                    icon = Icons.Rounded.Tune,
                    title = "Colores de Ciclo",
                    subtitle = if(currentPhasePalette == PhaseColorPalette.CUSTOM) "Personalizado" else phasePaletteOptions.find { it.palette == currentPhasePalette }?.name ?: "Original",
                    onClick = { showPhaseColorSheet = true }
                )
            }

            // â”€â”€ CHAT Y RECORDATORIOS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            SettingsSectionTitle("Chat y Recordatorios")
            SettingsCard {
                SettingsSwitchItem(
                    icon = Icons.Rounded.ChatBubbleOutline,
                    title = "Recordatorios en Chat",
                    subtitle = "Guardar un historial en la pestaña de chat",
                    checked = saveReminders,
                    onCheckedChange = { viewModel.setSaveRemindersInChat(it) }
                )
            }

            // â”€â”€ RESPALDO DE DATOS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            SettingsSectionTitle("Datos y Respaldo")
            SettingsCard {
                SettingsItem(
                    icon = Icons.Rounded.Check,
                    title = "Crear Copia de Seguridad",
                    subtitle = "Exportar tus datos a un archivo ZIP",
                    onClick = { exportLauncher.launch("Lumi_Backup.zip") }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                SettingsItem(
                    icon = Icons.Rounded.Check,
                    title = "Restaurar Copia",
                    subtitle = "Importar datos desde un ZIP",
                    onClick = { importLauncher.launch(arrayOf("application/zip")) }
                )
            }

            // â”€â”€ PRIVACIDAD & PAREJA â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            SettingsSectionTitle("Avanzado")
            SettingsCard {
                SettingsItem(
                    icon = Icons.Rounded.Favorite,
                    title = stringResource(R.string.settings_partner),
                    subtitle = stringResource(R.string.settings_partner_desc),
                    onClick = { /* TODO */ }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                SettingsItem(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.settings_privacy),
                    subtitle = stringResource(R.string.settings_privacy_desc),
                    onClick = { /* TODO */ }
                )
            }

            // ——— ZONA DE PELIGRO ——————————————————————————————————————————————
            SettingsSectionTitle("☠️ Zona de peligro")
            // Botón de borrar datos — sin envolver en SettingsCard para mayor énfasis visual
            OutlinedButton(
                onClick  = { showResetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                shape  = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector        = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = stringResource(R.string.settings_reset),
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp
                )
            }
            Text(
                text     = stringResource(R.string.settings_reset_desc),
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 28.dp)
            )

            Spacer(Modifier.height(16.dp))
            
            // ——— HERRAMIENTAS DE DESARROLLO ———————————————————————————————————
            SettingsSectionTitle("🛠 Herramientas de Desarrollo")
            OutlinedButton(
                onClick  = { viewModel.injectMockData() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                shape  = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "Generar Datos de Prueba (Dev)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp
                )
            }
            Text(
                text     = "Inyecta 4 meses de historial (ciclos y síntomas) para probar los gráficos y descubrimientos.",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 28.dp)
            )

            Spacer(Modifier.height(32.dp))
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                icon  = {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text       = stringResource(R.string.dialog_reset_title),
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.error
                    )
                },
                text  = {
                    Text(
                        text      = stringResource(R.string.dialog_reset_desc),
                        fontSize  = 14.sp,
                        lineHeight = 20.sp,
                        color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetDialog = false
                            viewModel.resetAllData()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.dialog_reset_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.dialog_reset_cancel))
                    }
                }
            )
        }

        if (showThemeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showThemeSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                    Text(stringResource(R.string.settings_general_theme), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    ThemeCarousel("Claras", clarasOptions, currentTheme) { viewModel.changeTheme(it) }
                    Spacer(Modifier.height(16.dp))
                    ThemeCarousel("Oscuras", oscurasOptions, currentTheme) { viewModel.changeTheme(it) }
                    Spacer(Modifier.height(16.dp))
                    ThemeCarousel("Minimalistas", minimalistasOptions, currentTheme) { viewModel.changeTheme(it) }
                }
            }
        }

        if (showPhaseColorSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPhaseColorSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                    Text(stringResource(R.string.settings_cycle_colors), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    PhasePaletteCarousel(
                        options       = phasePaletteOptions,
                        currentPalette= currentPhasePalette,
                        isDark        = isDark,
                        onSelect      = { viewModel.changePhasePalette(it) }
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.settings_customize_phase), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f))
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        PhaseColorPicker(
                            label         = stringResource(R.string.phase_menstrual),
                            currentColor  = activePhaseColors.menstrual,
                            onColorPicked = { viewModel.changeSinglePhaseColor(PhaseSlot.MENSTRUAL, it, isDark) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        PhaseColorPicker(
                            label         = stringResource(R.string.phase_follicular),
                            currentColor  = activePhaseColors.follicular,
                            onColorPicked = { viewModel.changeSinglePhaseColor(PhaseSlot.FOLLICULAR, it, isDark) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        PhaseColorPicker(
                            label         = stringResource(R.string.phase_ovulation),
                            currentColor  = activePhaseColors.ovulation,
                            onColorPicked = { viewModel.changeSinglePhaseColor(PhaseSlot.OVULATION, it, isDark) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        PhaseColorPicker(
                            label         = stringResource(R.string.phase_luteal),
                            currentColor  = activePhaseColors.luteal,
                            onColorPicked = { viewModel.changeSinglePhaseColor(PhaseSlot.LUTEAL, it, isDark) }
                        )
                    }
                }
            }
        }

        if (showGoalSheet) {
            ModalBottomSheet(
                onDismissRequest = { showGoalSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(bottom = 24.dp)) {
                    Text(stringResource(R.string.settings_user_goal), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(16.dp))
                    
                    val goals = listOf(
                        com.jeremy.lumi.domain.model.UserGoal.TRACK_CYCLE to stringResource(R.string.settings_goal_track),
                        com.jeremy.lumi.domain.model.UserGoal.AVOID_PREGNANCY to stringResource(R.string.settings_goal_avoid),
                        com.jeremy.lumi.domain.model.UserGoal.SEEK_PREGNANCY to stringResource(R.string.settings_goal_seek),
                        com.jeremy.lumi.domain.model.UserGoal.HEALTH_MONITORING to stringResource(R.string.settings_goal_health)
                    )
                    
                    goals.forEach { (goal, text) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                viewModel.setUserGoal(goal)
                                showGoalSheet = false 
                            }.padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = userGoal == goal,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(text, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        if (showPregnancyEndDialog) {
            AlertDialog(
                onDismissRequest = { showPregnancyEndDialog = false },
                title = { Text(stringResource(R.string.dialog_pregnancy_end_title), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.dialog_pregnancy_end_desc)) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.setIsPregnant(false)
                        showPregnancyEndDialog = false
                    }) { Text(stringResource(R.string.dialog_pregnancy_birth)) }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        viewModel.setIsPregnant(false)
                        showPregnancyEndDialog = false
                    }) { Text(stringResource(R.string.dialog_pregnancy_loss)) }
                }
            )
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  Helpers UI
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f), maxLines = 1)
        }
    }
}

@Composable
private fun SettingsSwitchItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f), maxLines = 1)
        }
        CupertinoSwitch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            checkedTrackColor = MaterialTheme.colorScheme.primary
        )
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  THEME CAROUSEL â€” etiqueta + LazyRow deslizable
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ThemeCarousel(
    label        : String,
    options      : List<ThemeOption>,
    currentTheme : AppThemePalette,
    onSelect     : (AppThemePalette) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text          = label,
            fontSize      = 12.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            letterSpacing = 0.5.sp,
            modifier      = Modifier.padding(horizontal = 24.dp)
        )
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(options, key = { it.palette.name }) { option ->
                ThemeSelector(
                    option       = option,
                    isSelected   = currentTheme == option.palette,
                    onClick      = { onSelect(option.palette) }
                )
            }
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  THEME SELECTOR â€” círculo + nombre
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ThemeSelector(
    option     : ThemeOption,
    isSelected : Boolean,
    onClick    : () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (option.isDark) option.darkBg else option.color)
                .border(
                    width = if (isSelected) 3.dp else if (option.isDark) 1.5.dp else 0.dp,
                    color = when {
                        isSelected  -> MaterialTheme.colorScheme.primary
                        option.isDark -> option.color.copy(alpha = 0.7f)
                        else        -> Color.Transparent
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Para temas oscuros: punto del color primario en el centro
            if (option.isDark && !isSelected) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(option.color))
            }
            if (isSelected) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text       = option.name,
            fontSize   = 11.sp,
            color      = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isSelected) 0.9f else 0.65f),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines   = 1
        )
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  PHASE PALETTE CAROUSEL â€” cada opción es un mini-anillo de 4 cuartos de color
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun PhasePaletteCarousel(
    options       : List<PhasePaletteOption>,
    currentPalette: PhaseColorPalette,
    isDark        : Boolean,
    onSelect      : (PhaseColorPalette) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(options, key = { it.palette.name }) { option ->
            val colors = option.palette.toPhaseColors(isDark)
            val isSelected = currentPalette == option.palette
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelect(option.palette) }
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .padding(if (isSelected) 4.dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Mini-anillo de 4 cuadrantes, uno por fase
                    Column(modifier = Modifier.clip(CircleShape)) {
                        Row {
                            Box(Modifier.size(22.dp).background(colors.menstrual))
                            Box(Modifier.size(22.dp).background(colors.follicular))
                        }
                        Row {
                            Box(Modifier.size(22.dp).background(colors.luteal))
                            Box(Modifier.size(22.dp).background(colors.ovulation))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text       = option.name,
                    fontSize   = 11.sp,
                    color      = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isSelected) 0.9f else 0.65f),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines   = 1
                )
            }
        }
    }
}
