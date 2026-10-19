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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.ui.semantics.Role
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
import com.jeremy.lumi.ui.components.AmbientBackgroundLayer
import com.jeremy.lumi.ui.theme.AmbientScreen
import androidx.compose.ui.graphics.drawscope.clipPath

// ── Modelo de dato para cada preset de colores de fase ────────────────────────

private data class PhasePaletteOption(
    val palette: PhaseColorPalette,
    val name   : String
)

private val phasePaletteOptions = listOf(
    PhasePaletteOption(PhaseColorPalette.AUTO_HARMONY, "Armonía"),
    PhasePaletteOption(PhaseColorPalette.MINIMALISTA, "Noir"),
    PhasePaletteOption(PhaseColorPalette.PASTEL,      "Blush")
)

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  SETTINGS SCREEN
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToBackup: () -> Unit = {},
    onNavigateToPartner: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentTheme        by viewModel.currentTheme.collectAsStateWithLifecycle()
    val currentPhasePalette by viewModel.currentPhasePalette.collectAsStateWithLifecycle()
    val customPhaseColors   by viewModel.customPhaseColors.collectAsStateWithLifecycle()
    val dialStyle           by viewModel.dialStyle.collectAsStateWithLifecycle()
    val backgroundAmbient   by viewModel.backgroundAmbient.collectAsStateWithLifecycle()
    val isDark = currentTheme.isDark()
    val saveReminders by viewModel.saveRemindersInChat.collectAsStateWithLifecycle()
    val widgetDetailsVisible by viewModel.revealWidgetDetails.collectAsStateWithLifecycle()
    val userGoal by viewModel.userGoal.collectAsStateWithLifecycle()
    val isPregnant by viewModel.isPregnant.collectAsStateWithLifecycle()
    val isOnContraceptive by viewModel.isOnContraceptive.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()

    val customPhaseColorsVal = customPhaseColors
    val activePhaseColors = if (currentPhasePalette == PhaseColorPalette.CUSTOM && customPhaseColorsVal != null) {
        customPhaseColorsVal
    } else {
        currentPhasePalette.toPhaseColors(isDark, currentTheme)
    }

    var showThemeSheet by remember { mutableStateOf(false) }
    var showPhaseColorSheet by remember { mutableStateOf(false) }
    var showDialStyleSheet by remember { mutableStateOf(false) }
    var showGoalSheet by remember { mutableStateOf(false) }
    var showPregnancyEndDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showMockDataDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    // Manual backups: passwords exist in memory only until SAF returns and encryption finishes.
    var showEncryptedExportPassword by remember { mutableStateOf(false) }
    var showEncryptedRestorePassword by remember { mutableStateOf(false) }
    var showLegacyImportWarning by remember { mutableStateOf(false) }
    var pendingEncryptedExportPassword by remember { mutableStateOf<CharArray?>(null) }
    var selectedEncryptedImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var manualBackupBusy by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupManager = viewModel.backupManager
    
    val isHealthConnectEnabled by viewModel.isHealthConnectEnabled.collectAsStateWithLifecycle()
    val healthConnectLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        // FIX P3-2: Usar la propiedad expuesta del ViewModel en lugar del manager público.
        if (granted.containsAll(viewModel.healthConnectPermissions)) {
            viewModel.setIsHealthConnectEnabled(true)
        } else {
            viewModel.setIsHealthConnectEnabled(false)
            android.widget.Toast.makeText(context, context.getString(R.string.settings_health_connect_denied), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // A permission can be revoked in Android while Lumi is in the background.
    // Reconcile the stored opt-in with the actual Health Connect permission state on resume.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshHealthConnectPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val encryptedExportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val password = pendingEncryptedExportPassword
        pendingEncryptedExportPassword = null
        if (uri == null || password == null) {
            password?.fill('\u0000')
        } else {
            coroutineScope.launch {
                manualBackupBusy = true
                try {
                    val result = context.contentResolver.openOutputStream(uri)?.let {
                        backupManager.exportEncryptedDatabaseArchive(it, password)
                    }
                    val success = result?.isSuccess == true
                    if (!success) {
                        // A failed export may have left an empty destination document.
                        runCatching { android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri) }
                    }
                    android.widget.Toast.makeText(
                        context,
                        context.getString(if (success) R.string.settings_backup_export_success else R.string.settings_backup_export_error),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    runCatching { android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri) }
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_backup_export_error), android.widget.Toast.LENGTH_LONG).show()
                } finally {
                    password.fill('\u0000')
                    manualBackupBusy = false
                }
            }
        }
    }

    val encryptedImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedEncryptedImportUri = uri
            showEncryptedRestorePassword = true
        }
    }

    val legacyImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                manualBackupBusy = true
                try {
                    val result = context.contentResolver.openInputStream(uri)?.let { input ->
                        backupManager.importDatabaseFromZip(input)
                    }
                    android.widget.Toast.makeText(
                        context,
                        context.getString(if (result?.isSuccess == true) R.string.settings_backup_import_success else R.string.settings_backup_import_error),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_backup_import_error), android.widget.Toast.LENGTH_LONG).show()
                } finally {
                    manualBackupBusy = false
                }
            }
        }
    }

    if (showEncryptedExportPassword) {
        ManualBackupPasswordDialog(
            exporting = true,
            onDismiss = { showEncryptedExportPassword = false },
            onConfirm = { password ->
                showEncryptedExportPassword = false
                pendingEncryptedExportPassword?.fill('\u0000')
                pendingEncryptedExportPassword = password
                encryptedExportLauncher.launch("Lumi_Backup_Encrypted.lbx")
            }
        )
    }

    if (showEncryptedRestorePassword) {
        ManualBackupPasswordDialog(
            exporting = false,
            onDismiss = {
                showEncryptedRestorePassword = false
                selectedEncryptedImportUri = null
            },
            onConfirm = { password ->
                showEncryptedRestorePassword = false
                val uri = selectedEncryptedImportUri
                selectedEncryptedImportUri = null
                if (uri == null) {
                    password.fill('\u0000')
                } else {
                    coroutineScope.launch {
                        manualBackupBusy = true
                        try {
                            val result = context.contentResolver.openInputStream(uri)?.let {
                                backupManager.importEncryptedDatabaseArchive(it, password)
                            }
                            val message = when {
                                result?.isSuccess == true -> R.string.settings_backup_import_success
                                result?.exceptionOrNull() is com.jeremy.lumi.data.backup.WrongPassphraseException -> R.string.backup_error_wrong_pass
                                result?.exceptionOrNull() is com.jeremy.lumi.data.backup.CorruptBackupException -> R.string.backup_error_corrupt
                                else -> R.string.settings_backup_import_error
                            }
                            android.widget.Toast.makeText(context, context.getString(message), android.widget.Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, context.getString(R.string.settings_backup_import_error), android.widget.Toast.LENGTH_LONG).show()
                        } finally {
                            password.fill('\u0000')
                            manualBackupBusy = false
                        }
                    }
                }
            }
        )
    }

    if (showLegacyImportWarning) {
        AlertDialog(
            onDismissRequest = { showLegacyImportWarning = false },
            title = { Text(stringResource(R.string.manual_backup_legacy_title)) },
            text = { Text(stringResource(R.string.manual_backup_legacy_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showLegacyImportWarning = false
                    legacyImportLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                }) { Text(stringResource(R.string.manual_backup_legacy_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showLegacyImportWarning = false }) {
                    Text(stringResource(R.string.backup_action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back),
                                tint = LumiThemeTokens.current.text
                            )
                        }
                    }
                },
                actions = {
                    com.jeremy.lumi.ui.components.LumiBrandLockup(
                        modifier = Modifier.padding(end = 22.dp),
                        flowerSize = 18,
                        fontSize = 19.sp,
                        animated = false
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LumiThemeTokens.current.bg
                )
            )
        },
        containerColor = LumiThemeTokens.current.bg
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            LumiSettingsBody(
                state = LumiSettingsSnapshot(
                    theme = currentTheme,
                    phasePalette = currentPhasePalette,
                    dialStyle = dialStyle,
                    ambient = backgroundAmbient,
                    goal = userGoal,
                    pregnant = isPregnant,
                    contraceptive = isOnContraceptive,
                    healthConnected = isHealthConnectEnabled,
                    healthAvailable = viewModel.isHealthConnectAvailable,
                    chatHistory = saveReminders,
                    widgetDetailsVisible = widgetDetailsVisible,
                    language = currentLanguage,
                    backupBusy = manualBackupBusy,
                    bondsEnabled = com.jeremy.lumi.BuildConfig.ENABLE_BONDS,
                    developerBuild = com.jeremy.lumi.BuildConfig.DEBUG
                ),
                actions = LumiSettingsActions(
                    goal = { showGoalSheet = true },
                    pregnancy = { checked ->
                        if (!checked && isPregnant) {
                            showPregnancyEndDialog = true
                        } else {
                            viewModel.setIsPregnant(checked)
                        }
                    },
                    contraceptive = { viewModel.setIsOnContraceptive(it) },
                    health = { checked ->
                        if (checked) {
                            if (viewModel.isHealthConnectAvailable) {
                                healthConnectLauncher.launch(viewModel.healthConnectPermissions)
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_health_connect_unavailable),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            viewModel.setIsHealthConnectEnabled(false)
                        }
                    },
                    language = { showLanguageDialog = true },
                    theme = { showThemeSheet = true },
                    phaseColors = { showPhaseColorSheet = true },
                    dialStyle = { showDialStyleSheet = true },
                    ambient = { viewModel.changeBackgroundAmbient(it) },
                    widget = { showWidgetPicker = true },
                    widgetReveal = { viewModel.setRevealWidgetDetails(it) },
                    chatHistory = { viewModel.setSaveRemindersInChat(it) },
                    driveBackup = onNavigateToBackup,
                    encryptedExport = { if (!manualBackupBusy) showEncryptedExportPassword = true },
                    encryptedRestore = { if (!manualBackupBusy) encryptedImportLauncher.launch(arrayOf("*/*")) },
                    legacyImport = { if (!manualBackupBusy) showLegacyImportWarning = true },
                    privacy = { showPrivacySheet = true },
                    bonds = onNavigateToPartner,
                    reset = { showResetDialog = true },
                    mockData = { showMockDataDialog = true }
                )
            )
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
                        text       = stringResource(R.string.settings_reset_confirm_data_title),
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.error
                    )
                },
                text  = {
                    Text(
                        text      = stringResource(R.string.settings_reset_confirm_data_desc),
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
                        Text(stringResource(R.string.settings_reset_confirm_data_action))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.dialog_reset_cancel))
                    }
                }
            )
        }

        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.settings_language_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            "system" to stringResource(R.string.settings_language_system),
                            "es" to stringResource(R.string.settings_language_es),
                            "en" to stringResource(R.string.settings_language_en)
                        )
                        options.forEach { (code, label) ->
                            val selected = (currentLanguage == code)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = {
                                            showLanguageDialog = false
                                            val act = com.jeremy.lumi.utils.LocaleHelper.findActivity(context)
                                            viewModel.setLanguage(code, act)
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    fontSize = 15.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            )
        }

        val themeRevealTrigger = com.jeremy.lumi.ui.theme.LocalThemeRevealTrigger.current

        if (showThemeSheet) {
            com.jeremy.lumi.ui.components.LumiThemeSheet(
                selectedTheme = currentTheme,
                onSelectThemeAt = { palette, center ->
                    if (themeRevealTrigger != null) {
                        themeRevealTrigger.changeThemeWithReveal(palette, center)
                    } else {
                        viewModel.changeTheme(palette)
                    }
                    showThemeSheet = false
                },
                onSelectTheme = { palette ->
                    if (themeRevealTrigger != null) {
                        themeRevealTrigger.changeThemeWithReveal(palette, null)
                    } else {
                        viewModel.changeTheme(palette)
                    }
                    showThemeSheet = false
                },
                onDismiss = { showThemeSheet = false }
            )
        }

        if (showPhaseColorSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPhaseColorSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                    Text(
                        stringResource(R.string.settings_cycle_colors),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    PhasePaletteCarousel(
                        options        = phasePaletteOptions,
                        currentPalette = currentPhasePalette,
                        currentTheme   = currentTheme,
                        isDark         = isDark,
                        onSelect       = {
                            viewModel.changePhasePalette(it)
                            showPhaseColorSheet = false
                        }
                    )
                }
            }
        }

        if (showDialStyleSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDialStyleSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_dial_style_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))

                    val dialOptions = listOf(
                        Triple(
                            DialStyle.GRADIENT,
                            stringResource(R.string.settings_dial_style_gradient),
                            stringResource(R.string.settings_dial_style_gradient_desc)
                        ),
                        Triple(
                            DialStyle.CYCLE_MAP,
                            stringResource(R.string.settings_dial_style_cycle_map),
                            stringResource(R.string.settings_dial_style_cycle_map_desc)
                        ),
                        Triple(
                            DialStyle.MINIMALIST,
                            stringResource(R.string.settings_dial_style_minimalist),
                            stringResource(R.string.settings_dial_style_minimalist_desc)
                        )
                    )

                    dialOptions.forEach { (style, title, desc) ->
                        val isSelected = dialStyle == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.changeDialStyle(style)
                                    showDialStyleSheet = false
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            // Miniatura visual del aro ilustrativa
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(32.dp)) {
                                    val strokeW = 4.dp.toPx()
                                    drawCircle(
                                        color = if (isDark) Color(0xFF383842) else Color(0xFFE4E4EC),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW)
                                    )
                                    when (style) {
                                        DialStyle.GRADIENT -> {
                                            val miniSweep = androidx.compose.ui.graphics.Brush.sweepGradient(
                                                listOf(
                                                    activePhaseColors.menstrual,
                                                    activePhaseColors.follicular,
                                                    activePhaseColors.ovulation,
                                                    activePhaseColors.luteal,
                                                    activePhaseColors.menstrual
                                                )
                                            )
                                            drawCircle(
                                                brush = miniSweep,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW)
                                            )
                                        }
                                        DialStyle.CYCLE_MAP -> {
                                            val softAlpha = 0.35f
                                            drawArc(activePhaseColors.menstrual.copy(alpha = softAlpha), -90f, 65f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
                                            drawArc(activePhaseColors.follicular.copy(alpha = softAlpha), -25f, 95f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
                                            drawArc(activePhaseColors.ovulation.copy(alpha = softAlpha), 70f, 90f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
                                            drawArc(activePhaseColors.luteal.copy(alpha = softAlpha), 160f, 110f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
                                            // Arco activo al frente
                                            drawArc(
                                                color = activePhaseColors.menstrual,
                                                startAngle = -90f,
                                                sweepAngle = 65f,
                                                useCenter = false,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                            )
                                        }
                                        DialStyle.MINIMALIST -> {
                                            drawArc(
                                                color = activePhaseColors.menstrual,
                                                startAngle = -90f,
                                                sweepAngle = 65f,
                                                useCenter = false,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.settings_prediction_not_contraception),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }
        }

        if (showPregnancyEndDialog) {
            AlertDialog(
                onDismissRequest = { showPregnancyEndDialog = false },
                title = { Text(stringResource(R.string.dialog_pregnancy_end_title), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.settings_pregnancy_end_confirmation)) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.setIsPregnant(false)
                        showPregnancyEndDialog = false
                    }) { Text(stringResource(R.string.settings_pregnancy_end_action)) }
                },
                dismissButton = {
                    TextButton(onClick = { showPregnancyEndDialog = false }) {
                        Text(stringResource(R.string.dialog_reset_cancel))
                    }
                }
            )
        }


        if (showWidgetPicker) {
            LumiWidgetPickerSheet(
                onDismiss = { showWidgetPicker = false },
                onSelect = { kind ->
                    showWidgetPicker = false
                    val receiverClass = when (kind) {
                        com.jeremy.lumi.ui.widget.LumiWidgetKind.CYCLE ->
                            com.jeremy.lumi.ui.widget.LumiWidgetReceiver::class.java
                        com.jeremy.lumi.ui.widget.LumiWidgetKind.WEEK ->
                            com.jeremy.lumi.ui.widget.LumiWeekWidgetReceiver::class.java
                        com.jeremy.lumi.ui.widget.LumiWidgetKind.QUIET ->
                            com.jeremy.lumi.ui.widget.LumiQuietWidgetReceiver::class.java
                    }
                    val manager = android.appwidget.AppWidgetManager.getInstance(context)
                    val provider = android.content.ComponentName(context, receiverClass)
                    if (manager.isRequestPinAppWidgetSupported) {
                        manager.requestPinAppWidget(provider, null, null)
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.widget_pin_unsupported),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }

        if (showPrivacySheet) {
            LumiSettingsPrivacySheet(
                onDismiss = { showPrivacySheet = false },
                onOpenBackup = onNavigateToBackup
            )
        }

        if (showMockDataDialog && com.jeremy.lumi.BuildConfig.DEBUG) {
            AlertDialog(
                onDismissRequest = { showMockDataDialog = false },
                title = { Text(stringResource(R.string.settings_dev_generate_mock)) },
                text = { Text(stringResource(R.string.settings_dev_generate_mock_desc)) },
                confirmButton = {
                    TextButton(onClick = {
                        showMockDataDialog = false
                        viewModel.injectMockData()
                    }) { Text(stringResource(android.R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showMockDataDialog = false }) {
                        Text(stringResource(R.string.dialog_reset_cancel))
                    }
                }
            )
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  Helpers UI
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun PhasePaletteCarousel(
    options       : List<PhasePaletteOption>,
    currentPalette: PhaseColorPalette,
    currentTheme  : AppThemePalette,
    isDark        : Boolean,
    onSelect      : (PhaseColorPalette) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(options, key = { it.palette.name }) { option ->
            val colors = option.palette.toPhaseColors(isDark, currentTheme)
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
