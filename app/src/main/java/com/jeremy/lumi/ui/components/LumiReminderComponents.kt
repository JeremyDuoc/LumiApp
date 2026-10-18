package com.jeremy.lumi.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.ReminderType
import com.jeremy.lumi.data.local.entity.SupplyType
import com.jeremy.lumi.ui.screens.home.LumiWheelDatePickerDialog
import com.jeremy.lumi.ui.screens.home.LumiWheelTimePickerDialog
import com.jeremy.lumi.ui.screens.home.ReminderDraftState
import com.jeremy.lumi.ui.theme.LumiDesignTokens
import com.jeremy.lumi.ui.theme.LumiMotion
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ReminderCategory {
    CYCLE, METHOD, CUSTOM
}

/**
 * Pestañas superiores (Ciclo, Método, Libre) con línea activa fina e indicador kicker.
 */
@Composable
fun LumiReminderTabs(
    selectedCategory: ReminderCategory,
    onSelectCategory: (ReminderCategory) -> Unit,
    modifier: Modifier = Modifier,
    tokens: LumiDesignTokens = LumiThemeTokens.current
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = tokens.line.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(bottom = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val categories = listOf(
                ReminderCategory.CYCLE to stringResource(R.string.reminder_tab_cycle),
                ReminderCategory.METHOD to stringResource(R.string.reminder_tab_method),
                ReminderCategory.CUSTOM to stringResource(R.string.reminder_tab_custom)
            )

            categories.forEach { (cat, label) ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 44.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectCategory(cat) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 14.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) tokens.text else tokens.muted
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(0.52f)
                                .height(2.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(tokens.accent)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        val kickerText = when (selectedCategory) {
            ReminderCategory.CYCLE -> stringResource(R.string.reminder_kicker_cycle)
            ReminderCategory.METHOD -> stringResource(R.string.reminder_kicker_method)
            ReminderCategory.CUSTOM -> stringResource(R.string.reminder_kicker_custom)
        }

        Text(
            text = kickerText.uppercase(Locale.getDefault()),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
            color = tokens.muted.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
        )
    }
}

/**
 * Fila de plantilla de recordatorio con glyph, títulos y nodo de acción / confirmado.
 */
@Composable
fun LumiReminderTemplateRow(
    type: ReminderType,
    name: String,
    description: String,
    metaText: String?,
    isActive: Boolean,
    isQueued: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tokens: LumiDesignTokens = LumiThemeTokens.current
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.985f else 1f

    val rowShape = RoundedCornerShape(19.dp)
    val bgModifier = if (isActive) {
        Modifier.background(
            Brush.horizontalGradient(
                colors = listOf(
                    tokens.accent.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.015f)
                )
            ),
            shape = rowShape
        ).border(1.dp, tokens.accent.copy(alpha = 0.28f), rowShape)
    } else {
        Modifier.background(Color.Transparent, shape = rowShape)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(rowShape)
            .then(bgModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Icon Pit circular con iluminación radial
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) tokens.accent.copy(alpha = 0.12f)
                    else tokens.muted.copy(alpha = 0.08f)
                ),
            contentAlignment = Alignment.Center
        ) {
            LumiReminderGlyph(
                type = type,
                tint = if (isActive) tokens.accent else tokens.text.copy(alpha = 0.85f),
                size = 22.dp
            )
        }

        Spacer(Modifier.width(12.dp))

        // 2. Textos (Título + Descripción o Meta cuando está encolado)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (isQueued && metaText != null) metaText else description,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = if (isQueued) tokens.accent.copy(alpha = 0.9f) else tokens.muted
            )
        }

        Spacer(Modifier.width(10.dp))

        // 3. Node indicador (+ o ✓ confirmado)
        Box(
            modifier = Modifier
                .size(31.dp)
                .clip(CircleShape)
                .background(
                    if (isQueued) tokens.accent
                    else Color.White.copy(alpha = 0.03f)
                )
                .border(
                    width = if (isQueued) 0.dp else 1.dp,
                    color = if (isQueued) Color.Transparent else tokens.line.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isQueued) "✓" else "+",
                fontSize = if (isQueued) 15.sp else 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isQueued) Color(0xFF160E1E) else tokens.muted
            )
        }
    }
}

/**
 * Chip selector LumiReminderChoice para opciones de días y suministros.
 */
@Composable
fun LumiReminderChoice(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tokens: LumiDesignTokens = LumiThemeTokens.current
) {
    val chipShape = RoundedCornerShape(14.dp)
    val bg = if (isSelected) tokens.accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f)
    val border = if (isSelected) tokens.accent.copy(alpha = 0.55f) else tokens.line.copy(alpha = 0.35f)
    val textColor = if (isSelected) tokens.accent else tokens.text

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 42.dp)
            .clip(chipShape)
            .background(bg)
            .border(1.dp, border, chipShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

/**
 * Campo selector de hora compacto integrado con LumiWheelTimePickerDialog.
 */
@Composable
fun LumiReminderTimeField(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    tokens: LumiDesignTokens = LumiThemeTokens.current
) {
    var showPicker by remember { mutableStateOf(false) }
    val formattedTime = remember(hour, minute) {
        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    val fieldShape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(fieldShape)
            .background(Color.White.copy(alpha = 0.025f))
            .border(1.dp, tokens.line.copy(alpha = 0.35f), fieldShape)
            .clickable { showPicker = true }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = formattedTime,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = tokens.text
        )
    }

    if (showPicker) {
        LumiWheelTimePickerDialog(
            initialTime = LocalTime.of(hour, minute),
            onDismissRequest = { showPicker = false },
            onConfirm = { selected ->
                onTimeChange(selected.hour, selected.minute)
                showPicker = false
            }
        )
    }
}

/**
 * Campo selector de fecha compacto integrado con LumiWheelDatePickerDialog.
 */
@Composable
fun LumiReminderDateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    tokens: LumiDesignTokens = LumiThemeTokens.current
) {
    var showPicker by remember { mutableStateOf(false) }
    val formattedDate = remember(date) {
        date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
    }

    val fieldShape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(fieldShape)
            .background(Color.White.copy(alpha = 0.025f))
            .border(1.dp, tokens.line.copy(alpha = 0.35f), fieldShape)
            .clickable { showPicker = true }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = formattedDate,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            color = tokens.text
        )
    }

    if (showPicker) {
        LumiWheelDatePickerDialog(
            initialDate = date,
            onDismissRequest = { showPicker = false },
            onConfirm = { selected ->
                onDateChange(selected)
                showPicker = false
            }
        )
    }
}

/**
 * Callout informativo de progressive disclosure (borde izquierdo de acento).
 */
@Composable
fun LumiReminderInfoCallout(
    text: String,
    modifier: Modifier = Modifier,
    tokens: LumiDesignTokens = LumiThemeTokens.current
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        tokens.accent.copy(alpha = 0.065f),
                        Color.Transparent
                    )
                )
            )
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tokens.accent.copy(alpha = 0.7f))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = tokens.muted
        )
    }
}

/**
 * Panel de configuración específico del recordatorio activo con progressive disclosure.
 */
@Composable
fun LumiReminderConfigurator(
    draft: ReminderDraftState,
    isQueued: Boolean,
    onAddOrUpdate: () -> Unit,
    modifier: Modifier = Modifier,
    tokens: LumiDesignTokens = LumiThemeTokens.current
) {
    val type = draft.type
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = tokens.line.copy(alpha = 0.35f),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(top = 18.dp)
    ) {
        // Cabecera del configurador: Halo grande con glifo + Título
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                tokens.accent.copy(alpha = 0.18f),
                                tokens.accent.copy(alpha = 0.025f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                LumiReminderGlyph(
                    type = type,
                    tint = tokens.accent,
                    size = 24.dp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = getReminderTypeName(type),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Text(
                    text = getReminderTypeSub(type),
                    fontSize = 12.5.sp,
                    color = tokens.muted
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Progressive disclosure según tipo
        when {
            type == ReminderType.PERIOD_SOON || type == ReminderType.OVULATION_SOON -> {
                Text(
                    text = stringResource(R.string.reminder_question_when_know),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3).forEach { days ->
                        val label = if (days == 1) stringResource(R.string.reminder_days_before_single)
                        else stringResource(R.string.reminder_days_before_plural, days)
                        LumiReminderChoice(
                            text = label,
                            isSelected = draft.daysBefore == days,
                            onClick = { draft.daysBefore = days },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.reminder_question_what_time),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text
                )
                Spacer(Modifier.height(6.dp))
                LumiReminderTimeField(
                    hour = draft.hour,
                    minute = draft.minute,
                    onTimeChange = { h, m -> draft.hour = h; draft.minute = m }
                )

                Spacer(Modifier.height(12.dp))
                LumiReminderInfoCallout(text = stringResource(R.string.reminder_info_cycle_auto))
            }

            type == ReminderType.LOG_DAILY -> {
                Text(
                    text = stringResource(R.string.reminder_question_what_time_best),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text
                )
                Spacer(Modifier.height(6.dp))
                LumiReminderTimeField(
                    hour = draft.hour,
                    minute = draft.minute,
                    onTimeChange = { h, m -> draft.hour = h; draft.minute = m }
                )

                Spacer(Modifier.height(12.dp))
                LumiReminderInfoCallout(text = stringResource(R.string.reminder_info_daily_repeat))
            }

            type == ReminderType.SUPPLY_REMINDER -> {
                Text(
                    text = stringResource(R.string.reminder_question_what_supply),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text
                )
                Spacer(Modifier.height(8.dp))

                val supplyOptions = listOf(
                    SupplyType.TAMPON to R.string.supply_tampon,
                    SupplyType.PAD to R.string.supply_pad,
                    SupplyType.MENSTRUAL_CUP to R.string.supply_cup,
                    SupplyType.PERIOD_UNDERWEAR to R.string.supply_underwear,
                    SupplyType.OTHER to R.string.supply_other
                )

                // Chips para supply en 2 filas compactas o FlowRow
                OptInSupplyChoiceRow(
                    options = supplyOptions,
                    selected = draft.selectedSupplyType ?: SupplyType.TAMPON,
                    onSelect = { draft.selectedSupplyType = it }
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.reminder_question_when_notify),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 3, 7).forEach { days ->
                        val label = if (days == 1) stringResource(R.string.reminder_days_before_single)
                        else stringResource(R.string.reminder_days_before_plural, days)
                        LumiReminderChoice(
                            text = label,
                            isSelected = draft.daysBefore == days,
                            onClick = { draft.daysBefore = days },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.reminder_pick_time),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text
                )
                Spacer(Modifier.height(6.dp))
                LumiReminderTimeField(
                    hour = draft.hour,
                    minute = draft.minute,
                    onTimeChange = { h, m -> draft.hour = h; draft.minute = m }
                )
            }

            type == ReminderType.PILL_DAILY -> {
                Text(
                    text = stringResource(R.string.reminder_question_what_time_best),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text
                )
                Spacer(Modifier.height(6.dp))
                LumiReminderTimeField(
                    hour = draft.hour,
                    minute = draft.minute,
                    onTimeChange = { h, m -> draft.hour = h; draft.minute = m }
                )

                Spacer(Modifier.height(12.dp))
                LumiReminderInfoCallout(text = stringResource(R.string.reminder_info_daily_repeat))
            }

            type.requiresStartDate() -> {
                // Métodos con intervalo (Parche, Anillo, Inyecciones)
                val intervalDays = type.defaultRepeatIntervalDays() ?: 7L
                val currentDate = remember(draft.methodStartDate) {
                    draft.methodStartDate?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    } ?: LocalDate.now()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = stringResource(R.string.reminder_question_when_started),
                            fontSize = 12.sp,
                            color = tokens.muted
                        )
                        Spacer(Modifier.height(6.dp))
                        LumiReminderDateField(
                            date = currentDate,
                            onDateChange = { selected ->
                                draft.methodStartDate = selected.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            }
                        )
                    }

                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = stringResource(R.string.reminder_pick_time),
                            fontSize = 12.sp,
                            color = tokens.muted
                        )
                        Spacer(Modifier.height(6.dp))
                        LumiReminderTimeField(
                            hour = draft.hour,
                            minute = draft.minute,
                            onTimeChange = { h, m -> draft.hour = h; draft.minute = m }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                LumiReminderInfoCallout(
                    text = stringResource(R.string.reminder_info_method_calc, intervalDays.toInt())
                )

                // Disclosure secundario: "Necesito otro intervalo ↓"
                var showCustomInterval by remember { mutableStateOf(draft.customRepeatDays != null) }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.reminder_disclosure_custom_interval),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = tokens.accent,
                    modifier = Modifier
                        .clickable { showCustomInterval = !showCustomInterval }
                        .padding(vertical = 4.dp)
                )

                AnimatedVisibility(visible = showCustomInterval) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        var repeatText by remember { mutableStateOf(draft.customRepeatDays?.toString() ?: intervalDays.toString()) }
                        OutlinedTextField(
                            value = repeatText,
                            onValueChange = {
                                val clean = it.filter { c -> c.isDigit() }
                                repeatText = clean
                                draft.customRepeatDays = clean.toLongOrNull()
                                draft.useCustomDate = clean.isNotBlank()
                            },
                            label = { Text(stringResource(R.string.reminder_custom_repeat_days_label), fontSize = 12.sp) },
                            suffix = { Text(stringResource(R.string.reminder_days_suffix), fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tokens.accent,
                                unfocusedBorderColor = tokens.line.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            type == ReminderType.CUSTOM -> {
                // Recordatorio libre
                Text(
                    text = stringResource(R.string.reminder_question_what_remember),
                    fontSize = 12.sp,
                    color = tokens.muted
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = draft.customLabel,
                    onValueChange = { if (it.length <= 60) draft.customLabel = it },
                    placeholder = { Text(stringResource(R.string.reminder_custom_placeholder), fontSize = 14.sp, color = tokens.muted.copy(alpha = 0.6f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tokens.accent,
                        unfocusedBorderColor = tokens.line.copy(alpha = 0.4f)
                    )
                )

                Spacer(Modifier.height(12.dp))

                val customLocalDate = draft.customDate ?: LocalDate.now()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = stringResource(R.string.reminder_custom_pick_date),
                            fontSize = 12.sp,
                            color = tokens.muted
                        )
                        Spacer(Modifier.height(6.dp))
                        LumiReminderDateField(
                            date = customLocalDate,
                            onDateChange = { draft.customDate = it }
                        )
                    }

                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = stringResource(R.string.reminder_pick_time),
                            fontSize = 12.sp,
                            color = tokens.muted
                        )
                        Spacer(Modifier.height(6.dp))
                        LumiReminderTimeField(
                            hour = draft.hour,
                            minute = draft.minute,
                            onTimeChange = { h, m -> draft.hour = h; draft.minute = m }
                        )
                    }
                }

                // Disclosure secundario: Repetir recordatorio libre
                var showRepeatCustom by remember { mutableStateOf(draft.customRepeatDays != null) }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.reminder_disclosure_custom_repeat),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = tokens.accent,
                    modifier = Modifier
                        .clickable { showRepeatCustom = !showRepeatCustom }
                        .padding(vertical = 4.dp)
                )

                AnimatedVisibility(visible = showRepeatCustom) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        var customRepeatDaysText by remember { mutableStateOf(draft.customRepeatDays?.toString() ?: "30") }
                        OutlinedTextField(
                            value = customRepeatDaysText,
                            onValueChange = {
                                val clean = it.filter { c -> c.isDigit() }
                                customRepeatDaysText = clean
                                draft.customRepeatDays = clean.toLongOrNull()
                            },
                            label = { Text(stringResource(R.string.reminder_custom_repeat_days_label), fontSize = 12.sp) },
                            suffix = { Text(stringResource(R.string.reminder_days_suffix), fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tokens.accent,
                                unfocusedBorderColor = tokens.line.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Botón "Añadir a mi lista" / "Actualizar"
        Button(
            onClick = {
                // Asegurar defaults requeridos antes de guardar
                if (type == ReminderType.SUPPLY_REMINDER && draft.selectedSupplyType == null) {
                    draft.selectedSupplyType = SupplyType.TAMPON
                }
                if (type.requiresStartDate() && draft.methodStartDate == null) {
                    draft.methodStartDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
                if (type == ReminderType.CUSTOM && draft.customDate == null) {
                    draft.customDate = LocalDate.now()
                }
                onAddOrUpdate()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = tokens.accent,
                contentColor = Color(0xFF1B1022)
            )
        ) {
            Text(
                text = if (isQueued) stringResource(R.string.reminder_btn_update)
                else stringResource(R.string.reminder_btn_add_to_list),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OptInSupplyChoiceRow(
    options: List<Pair<SupplyType, Int>>,
    selected: SupplyType,
    onSelect: (SupplyType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.take(3).forEach { (type, resId) ->
                LumiReminderChoice(
                    text = stringResource(resId),
                    isSelected = selected == type,
                    onClick = { onSelect(type) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.drop(3).forEach { (type, resId) ->
                LumiReminderChoice(
                    text = stringResource(resId),
                    isSelected = selected == type,
                    onClick = { onSelect(type) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Sección "Tu lista" con resumen de preparados, eliminación '×' y botón de activación general.
 */
@Composable
fun LumiReminderQueue(
    queuedDrafts: List<ReminderDraftState>,
    onSelectForEdit: (ReminderType) -> Unit,
    onRemove: (ReminderDraftState) -> Unit,
    onActivateAll: () -> Unit,
    modifier: Modifier = Modifier,
    tokens: LumiDesignTokens = LumiThemeTokens.current
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = tokens.line.copy(alpha = 0.35f),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(top = 18.dp)
    ) {
        // Cabecera: Tu lista | X preparados
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.reminder_queue_title),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )

            val count = queuedDrafts.size
            val countText = if (count == 1) stringResource(R.string.reminder_queue_count_single)
            else stringResource(R.string.reminder_queue_count_plural, count)

            Text(
                text = countText,
                fontSize = 11.5.sp,
                color = tokens.muted
            )
        }

        Spacer(Modifier.height(10.dp))

        // Lista de recordatorios preparados
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            queuedDrafts.forEach { draft ->
                val meta = formatDraftMeta(draft)
                val itemShape = RoundedCornerShape(15.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(itemShape)
                        .background(Color.White.copy(alpha = 0.025f))
                        .border(1.dp, tokens.line.copy(alpha = 0.35f), itemShape)
                        .clickable { onSelectForEdit(draft.type) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (draft.type == ReminderType.CUSTOM && draft.customLabel.isNotBlank()) draft.customLabel
                            else getReminderTypeName(draft.type),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.text
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = meta,
                            fontSize = 11.5.sp,
                            color = tokens.muted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onRemove(draft) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "×",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium,
                            color = tokens.muted
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Botón activar todos
        val count = queuedDrafts.size
        val isEnabled = count > 0
        val activateText = when (count) {
            0 -> stringResource(R.string.reminder_activate_empty)
            1 -> stringResource(R.string.reminder_activate_single)
            else -> stringResource(R.string.reminder_activate_plural, count)
        }

        val buttonShape = RoundedCornerShape(18.dp)
        Button(
            onClick = onActivateAll,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = tokens.accent.copy(alpha = 0.22f),
                contentColor = tokens.text,
                disabledContainerColor = tokens.line.copy(alpha = 0.08f),
                disabledContentColor = tokens.muted.copy(alpha = 0.45f)
            ),
            border = if (isEnabled) BorderStroke(1.dp, tokens.accent.copy(alpha = 0.35f)) else null
        ) {
            Text(
                text = activateText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Funciones auxiliares para formatos y textos limpios
@Composable
fun getReminderTypeName(type: ReminderType): String = when (type) {
    ReminderType.PERIOD_SOON -> stringResource(R.string.reminder_period_soon).trimEnd('.')
    ReminderType.OVULATION_SOON -> stringResource(R.string.reminder_ovulation_soon)
    ReminderType.LOG_DAILY -> stringResource(R.string.reminder_log_daily)
    ReminderType.SUPPLY_REMINDER -> stringResource(R.string.reminder_supply)
    ReminderType.PILL_DAILY -> stringResource(R.string.reminder_daily_pill)
    ReminderType.PATCH_WEEKLY -> stringResource(R.string.reminder_patch_weekly)
    ReminderType.RING_MONTHLY -> stringResource(R.string.reminder_ring_remove)
    ReminderType.INJECTION_MONTHLY -> stringResource(R.string.reminder_injection_monthly)
    ReminderType.INJECTION_QUARTERLY -> stringResource(R.string.reminder_injection_quarterly)
    ReminderType.CUSTOM -> stringResource(R.string.reminder_custom)
}

@Composable
fun getReminderTypeSub(type: ReminderType): String = when (type) {
    ReminderType.PERIOD_SOON -> stringResource(R.string.reminder_period_soon_desc)
    ReminderType.OVULATION_SOON -> stringResource(R.string.reminder_ovulation_soon_desc)
    ReminderType.LOG_DAILY -> stringResource(R.string.reminder_log_daily_desc)
    ReminderType.SUPPLY_REMINDER -> stringResource(R.string.reminder_supply_desc)
    ReminderType.PILL_DAILY -> stringResource(R.string.reminder_daily_pill_desc)
    ReminderType.PATCH_WEEKLY -> stringResource(R.string.reminder_patch_desc)
    ReminderType.RING_MONTHLY -> stringResource(R.string.reminder_ring_desc)
    ReminderType.INJECTION_MONTHLY -> stringResource(R.string.reminder_injection_monthly_desc)
    ReminderType.INJECTION_QUARTERLY -> stringResource(R.string.reminder_injection_quarterly_desc)
    ReminderType.CUSTOM -> stringResource(R.string.reminder_custom_desc)
}

@Composable
fun formatDraftMeta(draft: ReminderDraftState): String {
    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", draft.hour, draft.minute)
    return when (draft.type) {
        ReminderType.PERIOD_SOON, ReminderType.OVULATION_SOON -> {
            if (draft.daysBefore == 1) {
                stringResource(R.string.reminder_days_before_single_meta, timeStr)
            } else {
                stringResource(R.string.reminder_days_before_meta, draft.daysBefore, timeStr)
            }
        }
        ReminderType.LOG_DAILY, ReminderType.PILL_DAILY -> {
            stringResource(R.string.reminder_daily_repeat_meta, timeStr)
        }
        ReminderType.SUPPLY_REMINDER -> {
            val supplyName = when (draft.selectedSupplyType ?: SupplyType.TAMPON) {
                SupplyType.TAMPON -> stringResource(R.string.supply_tampon)
                SupplyType.PAD -> stringResource(R.string.supply_pad)
                SupplyType.MENSTRUAL_CUP -> stringResource(R.string.supply_cup)
                SupplyType.PERIOD_UNDERWEAR -> stringResource(R.string.supply_underwear)
                SupplyType.OTHER -> stringResource(R.string.supply_other)
            }
            if (draft.daysBefore == 1) {
                stringResource(R.string.reminder_supply_single_meta, supplyName, timeStr)
            } else {
                stringResource(R.string.reminder_supply_meta, supplyName, draft.daysBefore, timeStr)
            }
        }
        ReminderType.PATCH_WEEKLY, ReminderType.RING_MONTHLY,
        ReminderType.INJECTION_MONTHLY, ReminderType.INJECTION_QUARTERLY -> {
            val interval = draft.customRepeatDays ?: draft.type.defaultRepeatIntervalDays() ?: 7L
            stringResource(R.string.reminder_method_interval_meta, interval.toInt(), timeStr)
        }
        ReminderType.CUSTOM -> {
            val dateStr = draft.customDate?.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())) ?: ""
            stringResource(R.string.reminder_date_time_meta, dateStr, timeStr)
        }
    }
}
