package com.jeremy.lumi.ui.screens.home

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.ReminderEntity
import com.jeremy.lumi.data.local.entity.ReminderType
import com.jeremy.lumi.data.local.entity.SupplyType
import com.jeremy.lumi.ui.components.*
import com.jeremy.lumi.ui.theme.LumiDesignTokens
import com.jeremy.lumi.ui.theme.LumiMotion
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RemindersSection(
    viewModel: RemindersViewModel = hiltViewModel(),
    tokens: LumiDesignTokens = LumiThemeTokens.current
) {
    val reminders by viewModel.activeReminders.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    val surfaceColor = tokens.panel
    val borderColor = tokens.line
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, borderColor, shape)
            .padding(20.dp)
    ) {
        Column {
            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reminder_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                if (reminders.isNotEmpty()) {
                    TextButton(
                        onClick = { showAddSheet = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reminder_set),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.accent
                        )
                    }
                }
            }

            // Lista de recordatorios activos
            if (reminders.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showAddSheet = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.reminder_empty_hint),
                        fontSize = 13.sp,
                        color = tokens.muted,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.reminder_set),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.accent
                    )
                }
            } else {
                Spacer(Modifier.height(12.dp))
                reminders.take(10).forEach { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onCancel = { viewModel.cancelReminder(reminder) },
                        tokens = tokens
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // Sheet para añadir recordatorios
    if (showAddSheet) {
        AddReminderSheetContainer(
            viewModel = viewModel,
            onDismiss = { showAddSheet = false }
        )
    }
}

@Composable
fun AddReminderSheetContainer(
    viewModel: RemindersViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AddReminderSheet(
        onDismiss = onDismiss,
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
            onDismiss()
        }
    )
}

@Composable
private fun ReminderRow(
    reminder: ReminderEntity,
    onCancel: () -> Unit,
    tokens: LumiDesignTokens = LumiThemeTokens.current
) {
    val timeLabel = remember(reminder.hourOfDay, reminder.minute) {
        String.format(Locale.getDefault(), "%02d:%02d", reminder.hourOfDay, reminder.minute)
    }

    val dateLabel = remember(reminder.methodStartDate, reminder.isCustomDate, reminder.nextTriggerAt) {
        if (reminder.methodStartDate != null || reminder.isCustomDate) {
            val date = Instant.ofEpochMilli(reminder.nextTriggerAt)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val format = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
            date.format(format)
        } else null
    }

    val displayLabel = reminder.label ?: getReminderTypeName(reminder.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.025f))
            .border(1.dp, tokens.line.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tokens.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                LumiReminderGlyph(
                    type = reminder.type,
                    tint = tokens.accent,
                    size = 20.dp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = displayLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.text
                )

                val supplyText = reminder.supplyType?.let {
                    when (it) {
                        SupplyType.TAMPON -> stringResource(R.string.supply_tampon)
                        SupplyType.PAD -> stringResource(R.string.supply_pad)
                        SupplyType.MENSTRUAL_CUP -> stringResource(R.string.supply_cup)
                        SupplyType.PERIOD_UNDERWEAR -> stringResource(R.string.supply_underwear)
                        SupplyType.OTHER -> stringResource(R.string.supply_other)
                    }
                }
                val timeWithDate = if (dateLabel != null) "$dateLabel, $timeLabel" else timeLabel
                val subText = if (supplyText != null) "$timeWithDate • $supplyText" else timeWithDate

                Text(
                    text = subText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = tokens.accent.copy(alpha = 0.9f)
                )
            }
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.btn_cancel),
                tint = tokens.muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@androidx.compose.runtime.Stable
class ReminderDraftState(val type: ReminderType) {
    var enabled by mutableStateOf(false)
    var hour by mutableIntStateOf(20)
    var minute by mutableIntStateOf(0)
    var daysBefore by mutableIntStateOf(
        when (type) {
            ReminderType.PERIOD_SOON -> 2
            ReminderType.OVULATION_SOON -> 1
            ReminderType.SUPPLY_REMINDER -> 3
            else -> 1
        }
    )
    var methodStartDate by mutableStateOf<Long?>(null)
    var customDate by mutableStateOf<LocalDate?>(null)
    var customRepeatDays by mutableStateOf<Long?>(null)
    var customLabel by mutableStateOf("")
    var selectedSupplyType by mutableStateOf<SupplyType?>(
        if (type == ReminderType.SUPPLY_REMINDER) SupplyType.TAMPON else null
    )
    var useCustomDate by mutableStateOf(type == ReminderType.CUSTOM)
}

/**
 * Pantalla / Sheet rediseñado de creación de recordatorios según el HTML de referencia.
 * Flujo: ELEGIR → CONFIGURAR SOLO LO NECESARIO → AÑADIR A MI LISTA → ACTIVAR TODOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderSheet(
    onDismiss: () -> Unit,
    onSaveMultiple: (List<ReminderDraftState>) -> Unit,
    tokens: LumiDesignTokens = LumiThemeTokens.current
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val drafts = remember {
        ReminderType.entries.associateWith { ReminderDraftState(it) }
    }

    var selectedCategory by remember { mutableStateOf(ReminderCategory.CYCLE) }
    var activeTypeToEdit by remember { mutableStateOf<ReminderType?>(null) }
    val scrollState = rememberScrollState()

    val cycleTypes = listOf(
        ReminderType.PERIOD_SOON,
        ReminderType.OVULATION_SOON,
        ReminderType.LOG_DAILY,
        ReminderType.SUPPLY_REMINDER
    )

    val methodTypes = listOf(
        ReminderType.PILL_DAILY,
        ReminderType.PATCH_WEEKLY,
        ReminderType.RING_MONTHLY,
        ReminderType.INJECTION_MONTHLY,
        ReminderType.INJECTION_QUARTERLY
    )

    val customTypes = listOf(
        ReminderType.CUSTOM
    )

    val visibleTypes = when (selectedCategory) {
        ReminderCategory.CYCLE -> cycleTypes
        ReminderCategory.METHOD -> methodTypes
        ReminderCategory.CUSTOM -> customTypes
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.bg,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxHeight(0.94f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
        ) {
            // 1. Header con botón volver / cerrar y título editorial
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "‹",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = tokens.text
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.reminder_add_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            }

            // 2. Intro descriptiva limpia
            Text(
                text = stringResource(R.string.reminder_intro),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = tokens.muted,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            // 3. Contenido desplazable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // Tabs de categorías (Ciclo, Método, Libre)
                LumiReminderTabs(
                    selectedCategory = selectedCategory,
                    onSelectCategory = { cat ->
                        selectedCategory = cat
                        activeTypeToEdit = null
                    },
                    tokens = tokens
                )

                Spacer(Modifier.height(8.dp))

                // Catálogo de plantillas
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    visibleTypes.forEach { type ->
                        val draft = drafts[type]!!
                        val isQueued = draft.enabled
                        val metaText = if (isQueued) formatDraftMeta(draft) else null

                        LumiReminderTemplateRow(
                            type = type,
                            name = getReminderTypeName(type),
                            description = getReminderTypeSub(type),
                            metaText = metaText,
                            isActive = activeTypeToEdit == type,
                            isQueued = isQueued,
                            onClick = {
                                activeTypeToEdit = if (activeTypeToEdit == type) null else type
                            },
                            tokens = tokens
                        )
                    }
                }

                // Panel de configuración animado con progressive disclosure
                AnimatedVisibility(
                    visible = activeTypeToEdit != null,
                    enter = fadeIn(tween(200, easing = LumiMotion.EaseOutExpo)) +
                            slideInVertically(tween(200, easing = LumiMotion.EaseOutExpo)) { 14 },
                    exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { 14 }
                ) {
                    activeTypeToEdit?.let { type ->
                        val currentDraft = drafts[type]!!
                        LumiReminderConfigurator(
                            draft = currentDraft,
                            isQueued = currentDraft.enabled,
                            onAddOrUpdate = {
                                currentDraft.enabled = true
                                activeTypeToEdit = null
                            },
                            modifier = Modifier.padding(top = 16.dp),
                            tokens = tokens
                        )
                    }
                }

                // Cola de recordatorios preparados ("Tu lista")
                val queuedDrafts = drafts.values.filter { it.enabled }
                LumiReminderQueue(
                    queuedDrafts = queuedDrafts,
                    onSelectForEdit = { type ->
                        selectedCategory = when {
                            type in cycleTypes -> ReminderCategory.CYCLE
                            type in methodTypes -> ReminderCategory.METHOD
                            else -> ReminderCategory.CUSTOM
                        }
                        activeTypeToEdit = type
                    },
                    onRemove = { draft ->
                        draft.enabled = false
                        if (activeTypeToEdit == draft.type) {
                            activeTypeToEdit = null
                        }
                    },
                    onActivateAll = {
                        val validDrafts = queuedDrafts.filter { draft ->
                            when {
                                draft.type == ReminderType.SUPPLY_REMINDER -> draft.selectedSupplyType != null
                                draft.type == ReminderType.CUSTOM -> draft.customLabel.isNotBlank()
                                else -> true
                            }
                        }
                        onSaveMultiple(validDrafts)
                    },
                    modifier = Modifier.padding(top = 22.dp, bottom = 24.dp),
                    tokens = tokens
                )
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

private fun ensureExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!am.canScheduleExactAlarms()) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }
}
