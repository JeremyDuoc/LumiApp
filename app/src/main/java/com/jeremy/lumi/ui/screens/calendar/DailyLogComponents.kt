package com.jeremy.lumi.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.util.ClinicalLabels
import com.jeremy.lumi.ui.theme.LumiEditorialSerif
import com.jeremy.lumi.ui.theme.LumiMotion
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.ui.theme.lumiPressScale

internal data class LogSection(val key: String, val title: Int, val subtitle: Int, val icon: ImageVector)
internal val logDetailSections = listOf(
    LogSection("period", R.string.daily_period, R.string.daily_period_hint, Icons.Rounded.WaterDrop),
    LogSection("pain", R.string.daily_pain, R.string.daily_pain_hint, Icons.Rounded.Waves),
    LogSection("wellbeing", R.string.daily_wellbeing, R.string.daily_wellbeing_hint, Icons.Rounded.Bedtime),
    LogSection("mucus", R.string.daily_fertility, R.string.daily_fertility_hint, Icons.Rounded.Spa),
    LogSection("intercourse", R.string.daily_intimacy, R.string.daily_intimacy_hint, Icons.Rounded.FavoriteBorder),
    LogSection("notes", R.string.daily_notes, R.string.daily_notes_hint, Icons.Rounded.EditNote)
)

@Composable
internal fun readableLogAccent(): Color {
    val tokens = LumiThemeTokens.current
    return if (tokens.isDark) tokens.accent else lerp(tokens.accent, tokens.text, .42f)
}

@Composable
internal fun LogHint(text: String) {
    Text(text, color = LumiThemeTokens.current.muted, fontSize = 14.sp, lineHeight = 21.sp)
}

@Composable
internal fun LogEditorialTitle(title: Int, subtitle: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(title), fontFamily = LumiEditorialSerif, fontSize = 29.sp, lineHeight = 35.sp,
            color = LumiThemeTokens.current.text, modifier = Modifier.semantics { heading() })
        LogHint(stringResource(subtitle))
    }
}

@Composable
internal fun LogSectionTitle(title: Int, subtitle: Int? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(title), fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold,
            color = LumiThemeTokens.current.text, modifier = Modifier.semantics { heading() })
        subtitle?.let { LogHint(stringResource(it)) }
    }
}

@Composable
internal fun LogCard(content: @Composable ColumnScope.() -> Unit) {
    val tokens = LumiThemeTokens.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(tokens.panel, lerp(tokens.panel, tokens.accent, .035f))))
            .border(1.dp, tokens.line, RoundedCornerShape(24.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp), content = content
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LogOptionFlow(
    options: List<ClinicalLabels.OptionMapping>, selected: String?,
    enabled: Boolean = true, icons: List<ImageVector> = emptyList(), onSelect: (String?) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().selectableGroup()) {
        options.forEachIndexed { index, option ->
            LogChoice(stringResource(option.resId), selected == option.canonicalName, enabled,
                icons.getOrNull(index), radio = true) {
                onSelect(if (selected == option.canonicalName) null else option.canonicalName)
            }
        }
        if (selected != null && options.none { it.canonicalName == selected }) {
            LogChoice(selected, true, enabled, radio = true) { onSelect(null) }
        }
    }
}

@Composable
private fun LogChoice(
    text: String, selected: Boolean, enabled: Boolean = true,
    icon: ImageVector? = null, radio: Boolean = false, onClick: () -> Unit
) {
    val tokens = LumiThemeTokens.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val haptic = LocalHapticFeedback.current
    val background by animateColorAsState(if (selected) lerp(tokens.panel, tokens.accent, .18f) else tokens.panel2,
        LumiMotion.SpatialColorSpring, label = "choice_fill")
    val border by animateColorAsState(if (focused) tokens.text else if (selected) tokens.accent else tokens.line,
        LumiMotion.SpatialColorSpring, label = "choice_outline")
    val action = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
    val behavior = if (radio) Modifier.selectable(selected, enabled = enabled, role = Role.RadioButton,
        interactionSource = interaction, indication = null, onClick = action)
    else Modifier.toggleable(selected, enabled = enabled, role = Role.Checkbox,
        interactionSource = interaction, indication = null, onValueChange = { action() })
    Row(
        Modifier.lumiPressScale(interaction, .97f).clip(RoundedCornerShape(16.dp))
            .background(background).border(if (selected || focused) 1.5.dp else 1.dp, border, RoundedCornerShape(16.dp))
            .then(behavior).heightIn(min = 48.dp).padding(horizontal = 13.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected || icon != null) Icon(if (selected) Icons.Rounded.Check else icon!!, null,
            Modifier.size(18.dp), tint = if (selected) readableLogAccent() else tokens.muted)
        Text(text, color = tokens.text, fontSize = 14.sp, lineHeight = 20.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f, fill = false))
    }
}

@Composable
internal fun logSymptomLabel(name: String): String =
    logSymptomCatalog.firstOrNull { it.name == name }?.let { stringResource(it.resId) } ?: name

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LogSymptomChoices(names: List<String>, selected: Set<String>, enabled: Boolean, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        names.forEach { name -> key(name) {
            LogChoice(logSymptomLabel(name), name in selected, enabled) { onToggle(name) }
        } }
    }
}

@Composable
internal fun LogDetailSection(section: LogSection, draft: DailyLogDraft, enabled: Boolean, update: (DailyLogDraft) -> Unit) {
    val tokens = LumiThemeTokens.current
    var expanded by rememberSaveable(section.key) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, LumiMotion.GentleSpring, label = "detail_chevron")
    val stateLabel = stringResource(if (expanded) R.string.daily_expand else R.string.daily_collapse)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
        .background(tokens.panel).border(1.dp, tokens.line, RoundedCornerShape(22.dp))) {
        Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { expanded = !expanded }
            .semantics { stateDescription = stateLabel }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).background(tokens.accentSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(section.icon, null, Modifier.size(20.dp), tint = readableLogAccent())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(section.title), color = tokens.text, fontSize = 15.sp,
                    lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
                Text(sectionSummary(section.key, draft) ?: stringResource(section.subtitle),
                    color = tokens.muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
            Icon(Icons.Rounded.ExpandMore, null, Modifier.size(20.dp).rotate(rotation), tint = tokens.muted)
        }
        AnimatedVisibility(expanded,
            enter = expandVertically(spring(stiffness = 260f, dampingRatio = .95f)) + fadeIn(),
            exit = shrinkVertically(spring(stiffness = 340f)) + fadeOut()) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(color = tokens.line)
                when (section.key) {
                    "period" -> {
                        LogHint(stringResource(R.string.daily_flow_question))
                        val options = listOf(ClinicalLabels.OptionMapping(R.string.daily_no_flow, "Ninguno", emptySet())) +
                            ClinicalLabels.FLOW_MAPPINGS
                        LogOptionFlow(options, if (draft.flow == "Sin flujo") "Ninguno" else draft.flow, enabled) { update(draft.copy(flow = it)) }
                        LogToggleRow(stringResource(R.string.daily_spotting), draft.spotting, enabled,
                            stringResource(R.string.daily_spotting_hint)) { update(draft.copy(spotting = it)) }
                    }
                    "pain" -> NumberScale(stringResource(R.string.daily_pain_question), draft.pain, 0..10, enabled,
                        stringResource(R.string.daily_no_pain), stringResource(R.string.daily_strong_pain)) { update(draft.copy(pain = it)) }
                    "wellbeing" -> {
                        NumberField(draft.sleep, R.string.daily_sleep, 0f..24f, R.string.daily_sleep_error, enabled) { update(draft.copy(sleep = it)) }
                        OptionalScale(R.string.daily_energy, draft.energy, enabled) { update(draft.copy(energy = it)) }
                        OptionalScale(R.string.daily_stress, draft.stress, enabled) { update(draft.copy(stress = it)) }
                        NumberField(draft.water, R.string.daily_water, 0f..20f, R.string.daily_water_error, enabled) { update(draft.copy(water = it)) }
                    }
                    "mucus" -> {
                        LogHint(stringResource(R.string.daily_mucus_question))
                        LogOptionFlow(ClinicalLabels.MUCUS_MAPPINGS, draft.mucus, enabled) { update(draft.copy(mucus = it)) }
                        NumberField(draft.temperature, R.string.daily_temperature, 30f..45f,
                            R.string.daily_temperature_error, enabled) { update(draft.copy(temperature = it)) }
                    }
                    "intercourse" -> {
                        LogToggleRow(stringResource(R.string.daily_had_sex), draft.hadIntercourse, enabled) { update(draft.copy(hadIntercourse = it)) }
                        AnimatedVisibility(draft.hadIntercourse, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                LogHint(stringResource(R.string.daily_protection))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                                    LogChoice(stringResource(R.string.daily_yes), draft.protection == true, enabled, radio = true) {
                                        update(draft.copy(protection = if (draft.protection == true) null else true))
                                    }
                                    LogChoice(stringResource(R.string.daily_no), draft.protection == false, enabled, radio = true) {
                                        update(draft.copy(protection = if (draft.protection == false) null else false))
                                    }
                                }
                                LogHint(stringResource(R.string.daily_method))
                                LogOptionFlow(ClinicalLabels.METHOD_MAPPINGS, draft.method, enabled) { update(draft.copy(method = it)) }
                                LogToggleRow(stringResource(R.string.daily_calendar_visibility), draft.showIntimacy, enabled) { update(draft.copy(showIntimacy = it)) }
                                NoteField(draft.intimacyNotes, R.string.daily_intimacy_notes, enabled) { update(draft.copy(intimacyNotes = it)) }
                            }
                        }
                    }
                    "notes" -> NoteField(draft.notes, R.string.daily_notes_placeholder, enabled) { update(draft.copy(notes = it)) }
                }
            }
        }
    }
}

@Composable
private fun sectionSummary(key: String, draft: DailyLogDraft): String? = when (key) {
    "period" -> listOfNotNull(
        draft.flow?.let { value ->
            if (value == "Ninguno" || value == "Sin flujo") stringResource(R.string.daily_no_flow)
            else ClinicalLabels.FLOW_MAPPINGS.firstOrNull { it.canonicalName == value }?.let { stringResource(it.resId) } ?: value
        }, if (draft.spotting) stringResource(R.string.daily_spotting) else null
    ).takeIf { it.isNotEmpty() }?.joinToString(" · ")
    "pain" -> if (draft.pain > 0) stringResource(R.string.daily_intensity_value, draft.pain) else null
    "notes" -> draft.notes.takeIf { it.isNotBlank() }?.take(80)
    "mucus" -> listOfNotNull(
        draft.mucus?.let { value -> ClinicalLabels.MUCUS_MAPPINGS.firstOrNull { it.canonicalName == value }?.let { stringResource(it.resId) } ?: value },
        draft.temperature.takeIf { it.isNotBlank() }?.let { "$it °C" }
    ).takeIf { it.isNotEmpty() }?.joinToString(" · ")
    "intercourse" -> if (draft.hadIntercourse) stringResource(R.string.daily_had_sex) else null
    "wellbeing" -> listOfNotNull(draft.sleep.takeIf { it.isNotBlank() }?.let { "$it h" },
        draft.energy?.let { "${stringResource(R.string.daily_energy)} $it/${if (it in 1..5) 5 else 10}" },
        draft.stress?.let { "${stringResource(R.string.daily_stress)} $it/${if (it in 1..5) 5 else 10}" },
        draft.water.takeIf { it.isNotBlank() }?.let { "$it L" }).takeIf { it.isNotEmpty() }?.joinToString(" · ")
    else -> null
}

@Composable
internal fun LogToggleRow(label: String, checked: Boolean, enabled: Boolean = true,
    subtitle: String? = null, onToggle: (Boolean) -> Unit) {
    val tokens = LumiThemeTokens.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onToggle)
        .heightIn(min = 56.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, color = tokens.text, fontSize = 14.sp, lineHeight = 20.sp)
            subtitle?.let { Text(it, color = tokens.muted, fontSize = 12.sp, lineHeight = 18.sp) }
        }
        Switch(checked, onCheckedChange = null, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = tokens.accent, checkedThumbColor = tokens.bg))
    }
}

@Composable
private fun NumberScale(label: String, value: Int, range: IntRange, enabled: Boolean,
    low: String, high: String, onChange: (Int) -> Unit) {
    val tokens = LumiThemeTokens.current
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.sp, color = tokens.text, modifier = Modifier.weight(1f))
            AnimatedContent(value, transitionSpec = { fadeIn().togetherWith(fadeOut()) }, label = "scale_value") {
                Text("$it/${range.last}", color = readableLogAccent(), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            }
        }
        Slider(value.toFloat().coerceIn(range.first.toFloat(), range.last.toFloat()),
            onValueChange = { onChange(kotlin.math.round(it).toInt()) }, enabled = enabled,
            valueRange = range.first.toFloat()..range.last.toFloat(), steps = range.last - range.first - 1,
            modifier = Modifier.semantics { contentDescription = label },
            colors = SliderDefaults.colors(thumbColor = readableLogAccent(), activeTrackColor = readableLogAccent(), inactiveTrackColor = tokens.line))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(low, color = tokens.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(high, color = tokens.muted, fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionalScale(labelId: Int, value: Int?, enabled: Boolean, onChange: (Int?) -> Unit) {
    val label = stringResource(labelId)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LogHint(label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.selectableGroup()) {
            (1..5).forEach { level ->
                Box(Modifier.semantics { contentDescription = "$label, $level/5" }) {
                    LogChoice(level.toString(), value == level, enabled, radio = true) { onChange(if (level == value) null else level) }
                }
            }
            // Preserve older ten-point values until the user explicitly changes them.
            if (value != null && value !in 1..5) LogChoice("$value/10", true, enabled, radio = true) { onChange(null) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.daily_low), fontSize = 12.sp, color = LumiThemeTokens.current.muted)
            Text(stringResource(R.string.daily_high), fontSize = 12.sp, color = LumiThemeTokens.current.muted)
        }
    }
}

@Composable
private fun NumberField(value: String, labelId: Int, range: ClosedFloatingPointRange<Float>, errorId: Int,
    enabled: Boolean, onChange: (String) -> Unit) {
    val label = stringResource(labelId)
    val invalid = !validNumber(value, range)
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        enabled = enabled, singleLine = true, isError = invalid,
        supportingText = if (invalid) ({ Text(stringResource(errorId)) }) else null,
        trailingIcon = if (value.isNotEmpty()) ({
            IconButton(onClick = { onChange("") }, enabled = enabled) { Icon(Icons.Rounded.Close, stringResource(R.string.daily_clear, label)) }
        }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp), colors = logFieldColors())
}

@Composable
private fun NoteField(value: String, label: Int, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, modifier = Modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(stringResource(label)) }, minLines = 3, maxLines = 8,
        shape = RoundedCornerShape(16.dp), colors = logFieldColors())
}

@Composable
private fun logFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LumiThemeTokens.current.text, unfocusedTextColor = LumiThemeTokens.current.text,
    focusedBorderColor = readableLogAccent(), unfocusedBorderColor = LumiThemeTokens.current.line,
    focusedLabelColor = readableLogAccent(), unfocusedLabelColor = LumiThemeTokens.current.muted, cursorColor = readableLogAccent())

@Composable
internal fun LogPrimaryAction(text: String, enabled: Boolean = true, loading: Boolean = false,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tokens = LumiThemeTokens.current
    val interaction = remember { MutableInteractionSource() }
    val buttonColor = if (tokens.isDark) tokens.accent else readableLogAccent()
    val foreground = if (tokens.isDark) Color(0xFF201927) else Color.White
    Button(onClick, enabled = enabled, interactionSource = interaction,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).lumiPressScale(interaction, .985f),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = foreground,
            disabledContainerColor = tokens.panel2, disabledContentColor = tokens.muted),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = tokens.muted, strokeWidth = 2.dp)
        else Icon(Icons.Rounded.Check, null, Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LogSymptomCatalog(favoritesMode: Boolean, custom: Set<String>, selected: Set<String>,
    onToggle: (String) -> Unit, onAddCustom: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val search = normalizeSymptomSearch(query)
    val localized = logSymptomCatalog.map { Triple(it.name, stringResource(it.resId), it.category) }
    val filtered = (localized + custom.sorted().map { Triple(it, it, R.string.log_cat_custom) })
        .distinctBy { it.first }.filter { search.isEmpty() || normalizeSymptomSearch(it.second).contains(search) ||
            normalizeSymptomSearch(it.first).contains(search) }.groupBy { it.third }
    LazyColumn(Modifier.fillMaxSize().testTag("daily_catalog_scroll"), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { LogEditorialTitle(if (favoritesMode) R.string.daily_favorites else R.string.daily_catalog,
            if (favoritesMode) R.string.daily_favorites_catalog_hint else R.string.daily_catalog_hint) }
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.daily_search)) }, leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = if (query.isNotEmpty()) ({
                    IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, stringResource(R.string.daily_clear, stringResource(R.string.daily_search))) }
                }) else null, shape = RoundedCornerShape(18.dp), colors = logFieldColors())
        }
        item {
            OutlinedButton(onClick = onAddCustom, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                border = BorderStroke(1.dp, LumiThemeTokens.current.line), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Rounded.Add, null, tint = readableLogAccent())
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.daily_custom_add), color = readableLogAccent())
            }
        }
        if (filtered.isEmpty()) item { LogHint(stringResource(R.string.daily_empty_search)) }
        filtered.forEach { (category, symptoms) ->
            item("category_$category") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LogSectionTitle(category)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        symptoms.forEach { (name, label, _) ->
                            LogChoice(label, name in selected, icon = if (favoritesMode) Icons.Rounded.StarOutline else null) { onToggle(name) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LogCustomSymptomDialog(existing: Set<String>, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val localizedNames = logSymptomCatalog.map { stringResource(it.resId) }
    val duplicate = text.isNotBlank() && (existing + localizedNames).any { normalizeSymptomSearch(it) == normalizeSymptomSearch(text) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = LumiThemeTokens.current.panel,
        title = { Text(stringResource(R.string.daily_custom_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LogHint(stringResource(R.string.daily_custom_hint))
                OutlinedTextField(text, { if (it.length <= 60) text = it }, singleLine = true,
                    label = { Text(stringResource(R.string.daily_custom_name)) }, isError = duplicate,
                    supportingText = if (duplicate) ({ Text(stringResource(R.string.daily_custom_duplicate)) }) else null,
                    colors = logFieldColors())
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(text.trim()) }, enabled = text.isNotBlank() && !duplicate) { Text(stringResource(R.string.daily_custom_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.daily_cancel)) } })
}

@Composable
internal fun LogIntensityDialog(name: String, current: Int, onDismiss: () -> Unit, onDone: (Int) -> Unit) {
    var value by rememberSaveable(name) { mutableIntStateOf(current) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = LumiThemeTokens.current.panel,
        title = { Text(stringResource(R.string.daily_intensity, name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LogHint(stringResource(R.string.daily_intensity_hint))
                NumberScale(name, value, 0..10, true, stringResource(R.string.daily_unspecified), stringResource(R.string.daily_strong_pain)) { value = it }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(value) }) { Text(stringResource(R.string.daily_done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.daily_cancel)) } })
}
