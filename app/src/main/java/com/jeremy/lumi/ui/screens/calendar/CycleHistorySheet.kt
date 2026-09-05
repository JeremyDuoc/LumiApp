package com.jeremy.lumi.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.CycleEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleHistorySheet(
    onDismiss: () -> Unit,
    viewModel: CycleHistoryViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cycles by viewModel.cycles.collectAsStateWithLifecycle()

    var cycleToEdit by remember { mutableStateOf<CycleEntity?>(null) }
    var cycleToDelete by remember { mutableStateOf<CycleEntity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historial de Ciclos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "${cycles.size} ${if (cycles.size == 1) "ciclo" else "ciclos"}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (cycles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No hay ciclos registrados aún",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(cycles, key = { it.id }) { cycle ->
                        CycleHistoryCard(
                            cycle = cycle,
                            onEdit = { cycleToEdit = cycle },
                            onDelete = { cycleToDelete = cycle }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    cycleToDelete?.let { cycle ->
        AlertDialog(
            onDismissRequest = { cycleToDelete = null },
            title = { Text("¿Eliminar este ciclo?", fontWeight = FontWeight.Bold) },
            text = { Text("Esta acción eliminará el registro del ciclo del historial. Sus notas y síntomas diarios se conservarán.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCycle(cycle.id)
                        cycleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { cycleToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de edición de ciclo (duración y período)
    cycleToEdit?.let { cycle ->
        var cycleLenText by remember { mutableStateOf(cycle.cycleLength.toString()) }
        var periodLenText by remember { mutableStateOf(cycle.periodLength.toString()) }

        AlertDialog(
            onDismissRequest = { cycleToEdit = null },
            title = { Text("Editar Ciclo", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = cycleLenText,
                        onValueChange = { cycleLenText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Duración del ciclo (días)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = periodLenText,
                        onValueChange = { periodLenText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Duración del sangrado (días)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newCycleLen = cycleLenText.toIntOrNull()?.coerceIn(15, 90) ?: cycle.cycleLength
                        val newPeriodLen = periodLenText.toIntOrNull()?.coerceIn(1, 15) ?: cycle.periodLength
                        viewModel.updateCycle(
                            cycle.copy(
                                cycleLength = newCycleLen,
                                periodLength = newPeriodLen
                            )
                        )
                        cycleToEdit = null
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { cycleToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CycleHistoryCard(
    cycle: CycleEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val zone = ZoneId.of("UTC")
    val startDate = Instant.ofEpochMilli(cycle.startDate).atZone(zone).toLocalDate()
    val endDate = cycle.endDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    val isActive = cycle.endDate == null

    val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    val startStr = startDate.format(dateFormatter)
    val endStr = endDate?.format(dateFormatter) ?: "Activo hoy"

    val durationDays = if (endDate != null) {
        ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
    } else {
        ChronoUnit.DAYS.between(startDate, LocalDate.now()).toInt() + 1
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$startStr - $endStr",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (isActive) "En curso ($durationDays días)" else "$durationDays días",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Sangrado: ${cycle.periodLength}d",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Editar ciclo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Eliminar ciclo",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
