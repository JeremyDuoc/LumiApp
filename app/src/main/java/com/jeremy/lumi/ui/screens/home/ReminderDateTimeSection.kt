package com.jeremy.lumi.ui.screens.home

// ─────────────────────────────────────────────────────────────────────────────
//  PIEZA A INSERTAR dentro de tu AddReminderSheet existente (RemindersSection.kt)
//
//  No es un archivo nuevo independiente — es el bloque de UI que reemplaza
//  la sección "Fecha de inicio" que ya tenías, añadiendo el toggle entre
//  "usar el intervalo sugerido" y "elegir mi propia fecha y hora".
//
//  INTEGRACIÓN: dentro de AddReminderSheet, reemplaza el bloque que empieza
//  con `if (selectedType?.requiresStartDate() == true) { ... }` por esto.
// ─────────────────────────────────────────────────────────────────────────────

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Sección de fecha/hora con dos modos:
 *  - Sugerida: usa el intervalo de fábrica del tipo (lo que ya tenías)
 *  - Personalizada: la usuaria elige fecha Y hora exactas, con repetición opcional
 *
 * @param allowSuggestedMode si es false, el toggle no se muestra y el
 *        componente arranca directo en modo personalizado. Úsalo para
 *        ReminderType.CUSTOM, que no tiene intervalo sugerido alguno.
 * @param onModeChange notifica al padre si el modo actual es personalizado
 *        (true) o sugerido (false). El padre lo necesita para decidir si
 *        llama a createReminder() o a createCustomReminder().
 *
 * Devuelve hacia arriba (vía los callbacks) todo lo que el sheet necesita
 * para llamar a createReminder() o createCustomReminder() según el modo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDateTimeSection(
    suggestedIntervalLabel : String,
    allowSuggestedMode      : Boolean = true,
    onMethodStartDateChange: (Long?) -> Unit,
    onCustomDateChange     : (LocalDate?) -> Unit,
    onCustomRepeatChange   : (Long?) -> Unit,
    onHourMinuteChange     : (Int, Int) -> Unit,
    onModeChange            : (Boolean) -> Unit
) {
    var useCustomDate by remember { mutableStateOf(!allowSuggestedMode) }
    LaunchedEffect(Unit) { onModeChange(useCustomDate) } // notifica el estado inicial

    var methodStartDate  by remember { mutableStateOf<Long?>(null) }
    var customDate       by remember { mutableStateOf<LocalDate?>(null) }
    var hour             by remember { mutableIntStateOf(20) }
    var minute           by remember { mutableIntStateOf(0) }
    var repeatEnabled    by remember { mutableStateOf(false) }
    var repeatDays       by remember { mutableStateOf("90") }  // ej. 90 días ≈ 3 meses

    // Estados para mostrar los diálogos de M3
    var showDatePickerForMethod by remember { mutableStateOf(false) }
    var showDatePickerForCustom by remember { mutableStateOf(false) }
    var showTimePicker          by remember { mutableStateOf(false) }

    // Estados de Picker de M3
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = true
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // ── Toggle: sugerido vs personalizado — solo si el tipo lo permite ───
        if (allowSuggestedMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                ToggleChip(
                    text       = stringResource(R.string.reminder_mode_suggested),
                    isSelected = !useCustomDate,
                    modifier   = Modifier.weight(1f),
                    onClick    = { useCustomDate = false; onModeChange(false) }
                )
                ToggleChip(
                    text       = stringResource(R.string.reminder_mode_custom),
                    isSelected = useCustomDate,
                    modifier   = Modifier.weight(1f),
                    onClick    = { useCustomDate = true; onModeChange(true) }
                )
            }
        }

        if (!useCustomDate) {
            // ── MODO SUGERIDO ────────────────────────────────────────────────
            Text(suggestedIntervalLabel, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))

            Text(stringResource(R.string.reminder_start_date_hint),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground)

            OutlinedButton(
                onClick = { showDatePickerForMethod = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (methodStartDate != null) stringResource(R.string.reminder_start_date_set)
                    else stringResource(R.string.reminder_start_date_pick),
                    fontSize = 14.sp
                )
            }
        } else {
            // ── MODO PERSONALIZADO ───────────────────────────────────────────
            Text(stringResource(R.string.reminder_custom_date_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))

            // Fecha
            Text(stringResource(R.string.reminder_custom_pick_date),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground)
            
            OutlinedButton(
                onClick = { showDatePickerForCustom = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    customDate?.toString() ?: stringResource(R.string.reminder_custom_pick_date),
                    fontSize = 14.sp
                )
            }

            // Hora — siempre visible
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

            // Repetición opcional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Repeat, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reminder_custom_repeat),
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                }
                Switch(
                    checked = repeatEnabled,
                    onCheckedChange = {
                        repeatEnabled = it
                        onCustomRepeatChange(if (it) repeatDays.toLongOrNull() else null)
                    }
                )
            }

            AnimatedVisibility(visible = repeatEnabled, enter = expandVertically(), exit = shrinkVertically()) {
                OutlinedTextField(
                    value = repeatDays,
                    onValueChange = {
                        repeatDays = it.filter { c -> c.isDigit() }
                        onCustomRepeatChange(repeatDays.toLongOrNull())
                    },
                    label    = { Text(stringResource(R.string.reminder_custom_repeat_days_label)) },
                    suffix   = { Text(stringResource(R.string.reminder_days_suffix)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }
    }
    // ── DIÁLOGOS DE MATERIAL 3 ───────────────────────────────────────────────

    if (showDatePickerForMethod || showDatePickerForCustom) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePickerForMethod = false
                showDatePickerForCustom = false
            },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        if (showDatePickerForMethod) {
                            methodStartDate = millis
                            onMethodStartDateChange(millis)
                        } else {
                            val date = java.time.Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                            customDate = date
                            onCustomDateChange(date)
                        }
                    }
                    showDatePickerForMethod = false
                    showDatePickerForCustom = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePickerForMethod = false
                    showDatePickerForCustom = false
                }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        LumiTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
                    onHourMinuteChange(hour, minute)
                    showTimePicker = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
private fun ToggleChip(
    text       : String,
    isSelected : Boolean,
    modifier   : Modifier = Modifier,
    onClick    : () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = text,
            fontSize   = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BEAUTIFUL MATERIAL 3 TIME PICKER DIALOG WRAPPER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LumiTimePickerDialog(
    title: String = "Seleccionar hora",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        ),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}