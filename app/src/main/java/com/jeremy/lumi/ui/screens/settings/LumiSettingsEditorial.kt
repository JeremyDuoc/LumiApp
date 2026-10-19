package com.jeremy.lumi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.UserGoal
import com.jeremy.lumi.ui.theme.AppThemePalette
import com.jeremy.lumi.ui.theme.BackgroundAmbient
import com.jeremy.lumi.ui.theme.DialStyle
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.ui.theme.PhaseColorPalette

/** The settings UI is a pure view. Existing ViewModel flows and launchers remain in SettingsScreen. */
internal data class LumiSettingsSnapshot(
    val theme: AppThemePalette,
    val phasePalette: PhaseColorPalette,
    val dialStyle: DialStyle,
    val ambient: BackgroundAmbient,
    val goal: UserGoal,
    val pregnant: Boolean,
    val contraceptive: Boolean,
    val healthConnected: Boolean,
    val healthAvailable: Boolean,
    val chatHistory: Boolean,
    val language: String,
    val backupBusy: Boolean,
    val bondsEnabled: Boolean,
    val developerBuild: Boolean
)

internal class LumiSettingsActions(
    val goal: () -> Unit,
    val pregnancy: (Boolean) -> Unit,
    val contraceptive: (Boolean) -> Unit,
    val health: (Boolean) -> Unit,
    val language: () -> Unit,
    val theme: () -> Unit,
    val phaseColors: () -> Unit,
    val dialStyle: () -> Unit,
    val ambient: (BackgroundAmbient) -> Unit,
    val widget: () -> Unit,
    val chatHistory: (Boolean) -> Unit,
    val driveBackup: () -> Unit,
    val encryptedExport: () -> Unit,
    val encryptedRestore: () -> Unit,
    val legacyImport: () -> Unit,
    val privacy: () -> Unit,
    val bonds: () -> Unit,
    val reset: () -> Unit,
    val mockData: () -> Unit
)

@Composable
internal fun LumiSettingsBody(state: LumiSettingsSnapshot, actions: LumiSettingsActions) {
    val tokens = LumiThemeTokens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.nav_settings),
                    fontSize = 35.sp,
                    lineHeight = 40.sp,
                    letterSpacing = (-1.4).sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = stringResource(R.string.settings_editorial_intro),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = tokens.muted
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        SettingsThemeFeature(state.theme, actions.theme)

        SettingsEditorialSection(stringResource(R.string.settings_health_goals)) {
            SettingsEditorialEntry(
                icon = Icons.Rounded.TrackChanges,
                title = stringResource(R.string.settings_user_goal),
                subtitle = when (state.goal) {
                    UserGoal.TRACK_CYCLE -> stringResource(R.string.settings_goal_track)
                    UserGoal.AVOID_PREGNANCY -> stringResource(R.string.settings_goal_avoid)
                    UserGoal.SEEK_PREGNANCY -> stringResource(R.string.settings_goal_seek)
                    UserGoal.HEALTH_MONITORING -> stringResource(R.string.settings_goal_health)
                },
                onClick = actions.goal
            )
            SettingsEntryDivider()
            SettingsEditorialToggle(
                icon = Icons.Rounded.FavoriteBorder,
                title = stringResource(R.string.settings_pregnant),
                subtitle = stringResource(R.string.settings_pregnancy_mode_note),
                checked = state.pregnant,
                onCheckedChange = actions.pregnancy
            )
            SettingsEntryDivider()
            SettingsEditorialToggle(
                icon = Icons.Rounded.Medication,
                title = stringResource(R.string.settings_contraceptive_title),
                subtitle = stringResource(R.string.settings_contraceptive_mode_note),
                checked = state.contraceptive,
                onCheckedChange = actions.contraceptive
            )
        }

        SettingsEditorialSection(stringResource(R.string.settings_section_appearance)) {
            SettingsEditorialEntry(
                icon = Icons.Rounded.Language,
                title = stringResource(R.string.settings_language_title),
                subtitle = when (state.language) {
                    "en" -> stringResource(R.string.settings_language_en)
                    "es" -> stringResource(R.string.settings_language_es)
                    else -> stringResource(R.string.settings_language_system)
                },
                onClick = actions.language
            )
            SettingsEntryDivider()
            SettingsEditorialEntry(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.settings_cycle_colors),
                subtitle = when (state.phasePalette) {
                    PhaseColorPalette.AUTO_HARMONY -> stringResource(R.string.settings_palette_auto)
                    PhaseColorPalette.PASTEL -> "Blush"
                    PhaseColorPalette.MINIMALISTA -> "Noir"
                    else -> state.phasePalette.labelEs
                },
                onClick = actions.phaseColors
            )
            SettingsEntryDivider()
            SettingsEditorialEntry(
                icon = Icons.Rounded.DonutLarge,
                title = stringResource(R.string.settings_dial_style_title),
                subtitle = when (state.dialStyle) {
                    DialStyle.GRADIENT -> stringResource(R.string.settings_dial_style_gradient)
                    DialStyle.CYCLE_MAP -> stringResource(R.string.settings_dial_style_cycle_map)
                    DialStyle.MINIMALIST -> stringResource(R.string.settings_dial_style_minimalist)
                },
                onClick = actions.dialStyle
            )
            SettingsEntryDivider()
            SettingsAmbientChooser(state.ambient, actions.ambient)
        }

        SettingsEditorialSection(stringResource(R.string.settings_section_utilities_new)) {
            SettingsEditorialEntry(
                icon = Icons.Rounded.Widgets,
                title = stringResource(R.string.settings_add_widget),
                subtitle = stringResource(R.string.settings_add_widget_desc),
                onClick = actions.widget
            )
            SettingsEntryDivider()
            SettingsEditorialToggle(
                icon = Icons.Rounded.ChatBubbleOutline,
                title = stringResource(R.string.settings_chat_reminders_title),
                subtitle = stringResource(R.string.settings_chat_reminders_desc),
                checked = state.chatHistory,
                onCheckedChange = actions.chatHistory
            )
        }

        SettingsEditorialSection(stringResource(R.string.settings_section_protection_new)) {
            SettingsEditorialEntry(
                icon = Icons.Rounded.Shield,
                title = stringResource(R.string.settings_privacy_row_title),
                subtitle = stringResource(R.string.settings_privacy_explained),
                onClick = actions.privacy
            )
            SettingsEntryDivider()
            SettingsEditorialToggle(
                icon = Icons.Rounded.MonitorHeart,
                title = stringResource(R.string.settings_health_connect_title),
                subtitle = when {
                    !state.healthAvailable -> stringResource(R.string.settings_health_status_unavailable_new)
                    state.healthConnected -> stringResource(R.string.settings_health_status_connected_new)
                    else -> stringResource(R.string.settings_health_status_off_new)
                },
                checked = state.healthConnected,
                onCheckedChange = actions.health
            )
        }

        SettingsEditorialSection(stringResource(R.string.settings_data_backup_section)) {
            SettingsEditorialEntry(
                icon = Icons.Rounded.CloudUpload,
                title = stringResource(R.string.backup_settings_entry_title),
                subtitle = stringResource(R.string.backup_settings_entry_subtitle),
                onClick = actions.driveBackup
            )
            SettingsEntryDivider()
            SettingsEditorialEntry(
                icon = Icons.Rounded.FileUpload,
                title = stringResource(R.string.manual_backup_encrypted_export_title),
                subtitle = stringResource(R.string.manual_backup_encrypted_export_desc),
                enabled = !state.backupBusy,
                onClick = actions.encryptedExport
            )
            SettingsEntryDivider()
            SettingsEditorialEntry(
                icon = Icons.Rounded.FileDownload,
                title = stringResource(R.string.manual_backup_encrypted_restore_title),
                subtitle = stringResource(R.string.manual_backup_encrypted_restore_desc),
                enabled = !state.backupBusy,
                onClick = actions.encryptedRestore
            )
            SettingsEntryDivider()
            SettingsEditorialEntry(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.manual_backup_import_legacy_title),
                subtitle = stringResource(R.string.manual_backup_import_legacy_desc),
                enabled = !state.backupBusy,
                onClick = actions.legacyImport
            )
            if (state.backupBusy) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = tokens.accent
                    )
                    Text(stringResource(R.string.manual_backup_operation_busy), color = tokens.muted, fontSize = 12.sp)
                }
            }
        }

        if (state.bondsEnabled) {
            SettingsEditorialSection(stringResource(R.string.settings_section_advanced)) {
                SettingsEditorialEntry(
                    icon = Icons.Rounded.FavoriteBorder,
                    title = stringResource(R.string.settings_partner),
                    subtitle = stringResource(R.string.settings_partner_desc),
                    onClick = actions.bonds
                )
            }
        }

        SettingsEditorialSection(stringResource(R.string.settings_danger_zone)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .66f), RoundedCornerShape(12.dp))
                    .clickable(onClick = actions.reset)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_reset), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(3.dp))
                    Text(stringResource(R.string.settings_reset_desc), fontSize = 11.sp, lineHeight = 16.sp, color = tokens.muted)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.error.copy(alpha = .7f), modifier = Modifier.size(19.dp))
            }
            Text(
                stringResource(R.string.settings_drive_remains_note),
                color = tokens.dim,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 9.dp, start = 2.dp)
            )
        }

        if (state.developerBuild) {
            SettingsEditorialSection(stringResource(R.string.settings_dev_tools)) {
                SettingsEditorialEntry(
                    icon = Icons.Rounded.BugReport,
                    title = stringResource(R.string.settings_dev_generate_mock),
                    subtitle = stringResource(R.string.settings_dev_generate_mock_desc),
                    onClick = actions.mockData
                )
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun SettingsThemeFeature(theme: AppThemePalette, onClick: () -> Unit) {
    val tokens = LumiThemeTokens.current
    val themeName = when (theme) {
        AppThemePalette.NOIR -> "Noir"
        AppThemePalette.CIRUELA, AppThemePalette.VINO -> "Ciruela"
        AppThemePalette.BOSQUE, AppThemePalette.LUMI_FOREST, AppThemePalette.LUMI_FOREST_DARK -> "Bosque"
        AppThemePalette.INDIGO, AppThemePalette.LUMI_OCEAN, AppThemePalette.LUMI_OCEAN_DARK -> "Índigo"
        AppThemePalette.PETALO -> "Pétalo"
        AppThemePalette.MARFIL, AppThemePalette.ARENA -> "Marfil"
        AppThemePalette.SALVIA, AppThemePalette.LUMI_SAGE, AppThemePalette.LUMI_SAGE_DARK, AppThemePalette.CACTUS -> "Salvia"
        AppThemePalette.LAVANDA, AppThemePalette.LUMI_LAVENDER, AppThemePalette.LUMI_LAVENDER_DARK -> "Lavanda"
        else -> if (theme.isDark()) "Noir" else "Pétalo"
    }
    val samples = listOf(tokens.accent, tokens.rose, tokens.peach, tokens.mint)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(tokens.panel)
            .border(1.dp, tokens.line, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Palette, null, tint = tokens.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(R.string.settings_theme_general),
                color = tokens.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.ChevronRight, stringResource(R.string.settings_theme_general), tint = tokens.muted, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = themeName,
            color = tokens.text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            letterSpacing = (-.8).sp
        )
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.settings_theme_change_hint), color = tokens.muted, fontSize = 12.sp)
        Spacer(Modifier.height(19.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            samples.forEach { swatch ->
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(7.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(swatch)
                )
            }
        }
    }
}

@Composable
private fun SettingsEditorialSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val tokens = LumiThemeTokens.current
    Column(modifier = Modifier.padding(top = 30.dp)) {
        Text(title, color = tokens.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = .1.sp)
        Spacer(Modifier.height(11.dp))
        Column(content = content)
    }
}

@Composable
private fun SettingsEntryDivider() {
    val tokens = LumiThemeTokens.current
    HorizontalDivider(color = tokens.line, thickness = .7.dp, modifier = Modifier.padding(start = 39.dp))
}

@Composable
private fun SettingsEditorialEntry(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val tokens = LumiThemeTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 70.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tokens.accent.copy(alpha = if (enabled) .86f else .35f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(19.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, color = tokens.text.copy(alpha = if (enabled) 1f else .48f))
            Spacer(Modifier.height(3.dp))
            Text(subtitle, fontSize = 12.sp, lineHeight = 17.sp, color = tokens.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Rounded.ChevronRight, null, tint = tokens.dim, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsEditorialToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val tokens = LumiThemeTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .defaultMinSize(minHeight = 75.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tokens.accent.copy(alpha = .86f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(19.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = tokens.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = tokens.muted, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 3)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.height(28.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.bg,
                checkedTrackColor = tokens.accent,
                checkedBorderColor = tokens.accent,
                uncheckedTrackColor = tokens.panel2,
                uncheckedBorderColor = tokens.line,
                uncheckedThumbColor = tokens.muted
            )
        )
    }
}

@Composable
private fun SettingsAmbientChooser(value: BackgroundAmbient, onChange: (BackgroundAmbient) -> Unit) {
    val tokens = LumiThemeTokens.current
    Column(Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = tokens.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(19.dp))
            Text(stringResource(R.string.settings_ambient_title), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = tokens.text)
        }
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(BackgroundAmbient.FLAT, BackgroundAmbient.CELESTIAL).forEach { ambient ->
                val selected = value == ambient
                val icon = if (ambient == BackgroundAmbient.FLAT) Icons.Rounded.Layers else Icons.Rounded.NightsStay
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) tokens.accentSoft else tokens.panel)
                        .border(1.dp, if (selected) tokens.accent else tokens.line, RoundedCornerShape(12.dp))
                        .clickable { onChange(ambient) }
                        .padding(vertical = 13.dp, horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = if (selected) tokens.accent else tokens.muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(9.dp))
                    Text(stringResource(ambient.labelRes), color = if (selected) tokens.text else tokens.muted, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    if (selected) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Rounded.Check, null, tint = tokens.accent, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LumiSettingsPrivacySheet(onDismiss: () -> Unit, onOpenBackup: () -> Unit) {
    val tokens = LumiThemeTokens.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.panel
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Icon(Icons.Rounded.Shield, null, tint = tokens.accent, modifier = Modifier.size(27.dp))
            Spacer(Modifier.height(13.dp))
            Text(stringResource(R.string.settings_privacy_details_title), color = tokens.text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            PrivacyExplanation(stringResource(R.string.settings_privacy_local_title), stringResource(R.string.settings_privacy_local_desc))
            PrivacyExplanation(stringResource(R.string.settings_privacy_backup_title), stringResource(R.string.settings_privacy_backup_desc))
            PrivacyExplanation(stringResource(R.string.settings_privacy_health_title), stringResource(R.string.settings_privacy_health_desc))
            PrivacyExplanation(stringResource(R.string.settings_privacy_social_title), stringResource(R.string.settings_privacy_social_desc))
            Spacer(Modifier.height(18.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onDismiss(); onOpenBackup() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.accent),
                border = androidx.compose.foundation.BorderStroke(1.dp, tokens.accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.backup_settings_entry_title))
            }
        }
    }
}

@Composable
private fun PrivacyExplanation(title: String, body: String) {
    val tokens = LumiThemeTokens.current
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = tokens.text)
        Spacer(Modifier.height(4.dp))
        Text(body, fontSize = 12.sp, lineHeight = 19.sp, color = tokens.muted)
    }
}
