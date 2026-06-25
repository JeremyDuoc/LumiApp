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
import java.time.LocalTime
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

    // Estados de Picker de M3 eliminados a favor de WheelPicker


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
    // ── DIÁLOGOS CON WHEEL PICKER (ESTILO IOS) ───────────────────────────────

    if (showDatePickerForMethod || showDatePickerForCustom) {
        LumiWheelDatePickerDialog(
            initialDate = if (showDatePickerForMethod && methodStartDate != null) 
                java.time.Instant.ofEpochMilli(methodStartDate!!).atZone(ZoneId.systemDefault()).toLocalDate()
            else if (showDatePickerForCustom && customDate != null) 
                customDate!!
            else 
                LocalDate.now(),
            onDismissRequest = {
                showDatePickerForMethod = false
                showDatePickerForCustom = false
            },
            onConfirm = { selectedDate ->
                if (showDatePickerForMethod) {
                    val millis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    methodStartDate = millis
                    onMethodStartDateChange(millis)
                } else {
                    customDate = selectedDate
                    onCustomDateChange(selectedDate)
                }
                showDatePickerForMethod = false
                showDatePickerForCustom = false
            }
        )
    }

    if (showTimePicker) {
        LumiWheelTimePickerDialog(
            initialTime = LocalTime.of(hour, minute),
            onDismissRequest = { showTimePicker = false },
            onConfirm = { selectedTime ->
                hour = selectedTime.hour
                minute = selectedTime.minute
                onHourMinuteChange(hour, minute)
                showTimePicker = false
            }
        )
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
//  WHEEL PICKER DIALOGS (Glassmorphism & Premium UI)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LumiWheelDatePickerDialog(
    initialDate: LocalDate = LocalDate.now(),
    onDismissRequest: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var snappedDate by remember { mutableStateOf(initialDate) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Seleccionar Fecha",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                com.commandiron.wheel_picker_compose.WheelDatePicker(
                    startDate = initialDate,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    textColor = MaterialTheme.colorScheme.onSurface,
                    selectorProperties = com.commandiron.wheel_picker_compose.core.WheelPickerDefaults.selectorProperties(
                        enabled = true,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                ) { snapped ->
                    snappedDate = snapped
                }
                
                Spacer(Modifier.height(28.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(snappedDate) }) { Text("Aceptar") }
                }
            }
        }
    }
}

@Composable
fun LumiWheelTimePickerDialog(
    initialTime: LocalTime = LocalTime.now(),
    onDismissRequest: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var snappedTime by remember { mutableStateOf(initialTime) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Seleccionar Hora",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                com.commandiron.wheel_picker_compose.WheelTimePicker(
                    startTime = initialTime,
                    timeFormat = com.commandiron.wheel_picker_compose.core.TimeFormat.AM_PM,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    textColor = MaterialTheme.colorScheme.onSurface,
                    selectorProperties = com.commandiron.wheel_picker_compose.core.WheelPickerDefaults.selectorProperties(
                        enabled = true,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                ) { snapped ->
                    snappedTime = snapped
                }
                
                Spacer(Modifier.height(28.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(snappedTime) }) { Text("Aceptar") }
                }
            }
        }
    }
}