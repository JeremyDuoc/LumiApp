package com.jeremy.lumi.ui.screens.home

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import java.time.LocalTime
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.ReminderEntity
import com.jeremy.lumi.data.local.entity.ReminderType
import com.jeremy.lumi.data.local.entity.SupplyType
import java.time.LocalDate
import java.util.Locale

@Composable
fun RemindersSection(viewModel: RemindersViewModel = hiltViewModel()) {
    val reminders    by viewModel.activeReminders.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    val context      = LocalContext.current

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // â”€â”€ Cabecera â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.reminder_title),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                IconButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.reminder_set),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }

            // â”€â”€ Lista de recordatorios activos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (reminders.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.reminder_empty_hint),
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f))
            } else {
                Spacer(Modifier.height(12.dp))
                reminders.forEach { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onCancel = { viewModel.cancelReminder(reminder) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // â”€â”€ Sheet para añadir recordatorios (Multi-select) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (showAddSheet) {
        AddReminderSheet(
            onDismiss = { showAddSheet = false },
            onSaveMultiple = { drafts ->
                ensureExactAlarmPermission(context)
                drafts.forEach { draft ->
                    if (draft.type.allowsCustomDate() && draft.useCustomDate) {
                        viewModel.createCustomReminder(
                            customDate = draft.customDate ?: LocalDate.now(),
                            hourOfDay = draft.hour,
                            minute = draft.minute,
                            repeatEveryDays = draft.customRepeatDays,
                            label = draft.customLabel.takeIf { it.isNotBlank() },
                            type = draft.type
                        )
                    } else {
                        viewModel.createReminder(
                            type = draft.type,
                            hourOfDay = draft.hour,
                            minute = draft.minute,
                            methodStartDate = draft.methodStartDate,
                            label = draft.customLabel.takeIf { it.isNotBlank() },
                            supplyType = draft.selectedSupplyType
                        )
                    }
                }
                showAddSheet = false
            }
        )
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  REMINDER ROW
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ReminderRow(reminder: ReminderEntity, onCancel: () -> Unit) {
    val timeLabel = remember(reminder.hourOfDay, reminder.minute) {
        String.format(Locale.getDefault(), "%02d:%02d", reminder.hourOfDay, reminder.minute)
    }
    
    // Mostrar la fecha para recordatorios que tienen fecha de inicio o fecha personalizada
    val dateLabel = remember(reminder.methodStartDate, reminder.isCustomDate, reminder.nextTriggerAt) {
        if (reminder.methodStartDate != null || reminder.isCustomDate) {
            val date = java.time.Instant.ofEpochMilli(reminder.nextTriggerAt)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val format = java.time.format.DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
            date.format(format)
        } else null
    }

    val displayLabel = reminder.label ?: stringResource(reminderTypeLabelRes(reminder.type))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector        = reminderTypeIcon(reminder.type),
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(displayLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground)
                
                val supplyText = reminder.supplyType?.let { stringResource(supplyTypeLabelRes(it)) }
                val timeWithDate = if (dateLabel != null) "$dateLabel, $timeLabel" else timeLabel
                val subText = if (supplyText != null) "$timeWithDate â€¢ $supplyText" else timeWithDate
                
                Text(subText, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Close, null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp))
        }
    }
}

private fun supplyTypeLabelRes(type: SupplyType): Int = when (type) {
    SupplyType.TAMPON -> R.string.supply_tampon
    SupplyType.PAD -> R.string.supply_pad
    SupplyType.MENSTRUAL_CUP -> R.string.supply_cup
    SupplyType.PERIOD_UNDERWEAR -> R.string.supply_underwear
    SupplyType.OTHER -> R.string.supply_other
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  STATE CLASS FOR MULTI-SELECT
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

class ReminderDraftState(val type: ReminderType) {
    var enabled by mutableStateOf(false)
    var hour by mutableIntStateOf(20)
    var minute by mutableIntStateOf(0)
    var methodStartDate by mutableStateOf<Long?>(null)
    var customDate by mutableStateOf<LocalDate?>(null)
    var customRepeatDays by mutableStateOf<Long?>(null)
    var customLabel by mutableStateOf("")
    var selectedSupplyType by mutableStateOf<SupplyType?>(null)
    var useCustomDate by mutableStateOf(type == ReminderType.CUSTOM)
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  ADD REMINDER SHEET
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderSheet(
    onDismiss : () -> Unit,
    onSaveMultiple: (List<ReminderDraftState>) -> Unit
) {
    val context    = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val drafts = remember {
        ReminderType.entries.associateWith { ReminderDraftState(it) }
    }

    val automaticTypes = listOf(
        ReminderType.PERIOD_SOON,
        ReminderType.OVULATION_SOON,
        ReminderType.LOG_DAILY,
        ReminderType.SUPPLY_REMINDER
    )
    val simpleTimeTypes = listOf(ReminderType.PILL_DAILY)
    val methodTypes = listOf(
        ReminderType.PATCH_WEEKLY,
        ReminderType.RING_MONTHLY,
        ReminderType.INJECTION_MONTHLY,
        ReminderType.INJECTION_QUARTERLY
    )
    val freeformTypes = listOf(ReminderType.CUSTOM)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.background,
        modifier         = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.reminder_add_title), fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // â”€â”€ Sección: recordatorios de ciclo â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                SectionGroupLabel(stringResource(R.string.reminder_group_cycle))
                automaticTypes.forEach { type ->
                    ReminderTypeSwitchRow(drafts[type]!!, simpleTimeTypes, context)
                }

                Spacer(Modifier.height(8.dp))

                // â”€â”€ Sección: métodos anticonceptivos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                SectionGroupLabel(stringResource(R.string.reminder_group_contraception))
                (simpleTimeTypes + methodTypes).forEach { type ->
                    ReminderTypeSwitchRow(drafts[type]!!, simpleTimeTypes, context)
                }

                Spacer(Modifier.height(8.dp))

                // â”€â”€ Sección: recordatorio libre â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                SectionGroupLabel(stringResource(R.string.reminder_group_freeform))
                freeformTypes.forEach { type ->
                    ReminderTypeSwitchRow(drafts[type]!!, simpleTimeTypes, context)
                }
            }

            Spacer(Modifier.height(12.dp))

            val enabledDrafts = drafts.values.filter { it.enabled }
            val canConfirm = enabledDrafts.isNotEmpty() && enabledDrafts.all { draft ->
                when {
                    draft.type == ReminderType.SUPPLY_REMINDER -> draft.selectedSupplyType != null
                    draft.type == ReminderType.CUSTOM -> draft.customLabel.isNotBlank() && draft.customDate != null
                    draft.type.allowsCustomDate() -> if (draft.useCustomDate) draft.customDate != null else draft.methodStartDate != null
                    else -> true
                }
            }

            Button(
                onClick = { onSaveMultiple(enabledDrafts) },
                enabled  = canConfirm,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(stringResource(R.string.reminder_set) + if (enabledDrafts.size > 1) " (${enabledDrafts.size})" else "", fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  INDIVIDUAL SWITCH ROW AND CONFIGURATION
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ReminderTypeSwitchRow(
    draft: ReminderDraftState,
    simpleTimeTypes: List<ReminderType>,
    context: Context
) {
    val type = draft.type
    val isSelected = draft.enabled

    val bg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else Color.Transparent, tween(200), label = "row_bg"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                width  = if (isSelected) 1.dp else 0.dp,
                color  = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                shape  = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { draft.enabled = !draft.enabled }
        ) {
            Icon(reminderTypeIcon(type), null,
                tint     = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(reminderTypeLabelRes(type)), fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(reminderTypeDescRes(type)), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
            Switch(
                checked = isSelected,
                onCheckedChange = { draft.enabled = it },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
            )
        }

        // Todos los recordatorios necesitan al menos elegir la hora
        val hasConfig = true

        AnimatedVisibility(
            visible = isSelected && hasConfig,
            enter = expandVertically(tween(300)),
            exit = shrinkVertically(tween(300))
        ) {
            Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), thickness = 0.5.dp)
                
                when {
                    type == ReminderType.SUPPLY_REMINDER -> {
                        SupplySelector(
                            selected = draft.selectedSupplyType,
                            onSelect = { draft.selectedSupplyType = it }
                        )
                        TimePickerButton(draft.hour, draft.minute) { h, m -> draft.hour = h; draft.minute = m; }
                    }
                    type in simpleTimeTypes -> {
                        TimePickerButton(draft.hour, draft.minute) { h, m -> draft.hour = h; draft.minute = m; }
                    }
                    type.allowsCustomDate() -> {
                        val suggestedLabel = type.defaultRepeatIntervalDays()?.let { days ->
                            stringResource(R.string.cal_next_phase_days, days.toInt())
                        } ?: ""

                        ReminderDateTimeSection(
                            suggestedIntervalLabel  = suggestedLabel,
                            allowSuggestedMode      = type != ReminderType.CUSTOM,
                            onMethodStartDateChange = { draft.methodStartDate = it },
                            onCustomDateChange      = { draft.customDate = it },
                            onCustomRepeatChange    = { draft.customRepeatDays = it },
                            onHourMinuteChange      = { h, m -> draft.hour = h; draft.minute = m },
                            onModeChange            = { draft.useCustomDate = it }
                        )
                    }
                    else -> {
                        // Standard type like PERIOD_SOON or OVULATION_SOON
                        TimePickerButton(draft.hour, draft.minute) { h, m -> draft.hour = h; draft.minute = m; }
                    }
                }

                if (type.allowsCustomDate() || type == ReminderType.PILL_DAILY) {
                    Text(
                        if (type == ReminderType.CUSTOM) stringResource(R.string.reminder_custom_text_label)
                        else stringResource(R.string.reminder_label_hint),
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedTextField(
                        value         = draft.customLabel,
                        onValueChange = { draft.customLabel = it },
                        placeholder   = {
                            Text(
                                if (type == ReminderType.CUSTOM) stringResource(R.string.reminder_custom_text_hint)
                                else stringResource(R.string.reminder_label_hint),
                                fontSize = 14.sp
                            )
                        },
                        isError    = type == ReminderType.CUSTOM && draft.customLabel.isBlank(),
                        modifier   = Modifier.fillMaxWidth(),
                        shape      = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TimePickerButton(hour: Int, minute: Int, onTimeChange: (Int, Int) -> Unit) {
    var showTimePicker by remember { mutableStateOf(false) }

    Text(stringResource(R.string.reminder_pick_time),
        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground)
    OutlinedButton(
        onClick = { showTimePicker = true },
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
            fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }

    if (showTimePicker) {
        LumiWheelTimePickerDialog(
            initialTime = LocalTime.of(hour, minute),
            onDismissRequest = { showTimePicker = false },
            onConfirm = { selectedTime ->
                onTimeChange(selectedTime.hour, selectedTime.minute)
                showTimePicker = false
            }
        )
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  SUPPLY SELECTOR â€” elegir qué método usa habitualmente (tampón/copa/etc.)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun SupplySelector(
    selected: SupplyType?,
    onSelect: (SupplyType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text       = stringResource(R.string.reminder_supply_pick_label),
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground
        )

        val options = listOf(
            SupplyType.TAMPON           to R.string.supply_tampon,
            SupplyType.PAD              to R.string.supply_pad,
            SupplyType.MENSTRUAL_CUP    to R.string.supply_cup,
            SupplyType.PERIOD_UNDERWEAR to R.string.supply_underwear,
            SupplyType.OTHER            to R.string.supply_other
        )

        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options.size) { index ->
                val (type, labelRes) = options[index]
                SupplyChip(
                    text       = stringResource(labelRes),
                    isSelected = selected == type,
                    onClick    = { onSelect(type) }
                )
            }
        }
    }
}

@Composable
private fun SupplyChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent
    val content = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onBackground
    val border = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = content, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  COMPONENTES AUXILIARES
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun SectionGroupLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.06.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  MAPEOS DE RECURSOS POR TIPO
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private fun reminderTypeLabelRes(type: ReminderType): Int = when (type) {
    ReminderType.PERIOD_SOON          -> R.string.reminder_period_soon
    ReminderType.OVULATION_SOON       -> R.string.reminder_ovulation_soon
    ReminderType.LOG_DAILY            -> R.string.reminder_log_daily
    ReminderType.SUPPLY_REMINDER      -> R.string.reminder_supply
    ReminderType.PILL_DAILY           -> R.string.reminder_daily_pill
    ReminderType.PATCH_WEEKLY         -> R.string.reminder_patch_weekly
    ReminderType.RING_MONTHLY         -> R.string.reminder_ring_remove
    ReminderType.INJECTION_MONTHLY    -> R.string.reminder_injection_monthly
    ReminderType.INJECTION_QUARTERLY  -> R.string.reminder_injection_quarterly
    ReminderType.CUSTOM               -> R.string.reminder_custom
}

private fun reminderTypeDescRes(type: ReminderType): Int = when (type) {
    ReminderType.PERIOD_SOON          -> R.string.reminder_period_soon_desc
    ReminderType.OVULATION_SOON       -> R.string.reminder_ovulation_soon_desc
    ReminderType.LOG_DAILY            -> R.string.reminder_log_daily_desc
    ReminderType.SUPPLY_REMINDER      -> R.string.reminder_supply_desc
    ReminderType.PILL_DAILY           -> R.string.reminder_daily_pill_desc
    ReminderType.PATCH_WEEKLY         -> R.string.reminder_patch_desc
    ReminderType.RING_MONTHLY         -> R.string.reminder_ring_desc
    ReminderType.INJECTION_MONTHLY    -> R.string.reminder_injection_monthly_desc
    ReminderType.INJECTION_QUARTERLY  -> R.string.reminder_injection_quarterly_desc
    ReminderType.CUSTOM               -> R.string.reminder_custom_desc
}

private fun reminderTypeIcon(type: ReminderType): ImageVector = when (type) {
    ReminderType.PERIOD_SOON          -> Icons.Rounded.WaterDrop
    ReminderType.OVULATION_SOON       -> Icons.Rounded.Favorite
    ReminderType.LOG_DAILY            -> Icons.Rounded.EditNote
    ReminderType.SUPPLY_REMINDER      -> Icons.Rounded.ShoppingBag
    ReminderType.PILL_DAILY           -> Icons.Rounded.Medication
    ReminderType.PATCH_WEEKLY         -> Icons.Rounded.Healing
    ReminderType.RING_MONTHLY         -> Icons.Rounded.Loop
    ReminderType.INJECTION_MONTHLY,
    ReminderType.INJECTION_QUARTERLY  -> Icons.Rounded.Vaccines
    ReminderType.CUSTOM               -> Icons.Rounded.EditCalendar
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  PERMISO ALARMA EXACTA (Android 12+)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private fun ensureExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!am.canScheduleExactAlarms()) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data  = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }
}
