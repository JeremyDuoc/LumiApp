package com.jeremy.lumi.ui.screens.insights

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.model.CycleInsights
import com.jeremy.lumi.domain.model.MedicalAlert
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Hoja de acompañamiento empática para la consulta médica.
 * Diseñada para que la usuaria cuente con un resumen claro, estructurado
 * y digno con las respuestas a las preguntas que su especialista formulará.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosModeSheet(
    insights: CycleInsights,
    medicalAlerts: List<MedicalAlert> = emptyList(),
    isOnContraceptive: Boolean = false,
    lmpDate: LocalDate? = null,
    daysSinceLmp: Int? = null,
    onExportPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val stats = insights.historicalStats
    val recentCycles = insights.recentCycleStats

    var checkedQuestions by rememberSaveable { mutableStateOf(setOf<Int>()) }

    val variation = if (stats != null) stats.longestCycle - stats.shortestCycle else 0
    val avgCycle = stats?.avgCycleLength?.toInt() ?: 28
    val avgPeriod = stats?.avgPeriodLength?.toInt() ?: 5
    val maxPain = recentCycles.maxOfOrNull { it.avgPainLevel } ?: 0f
    val avgPain = if (recentCycles.isNotEmpty()) recentCycles.map { it.avgPainLevel }.average().toFloat() else 0f

    val lmpFormatted = lmpDate?.format(DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale("es")))
        ?: stringResource(R.string.sos_lmp_not_registered)
    val daysElapsedText = daysSinceLmp?.let {
        stringResource(R.string.sos_days_elapsed, it, it)
    } ?: stringResource(R.string.sos_days_elapsed_pending)

    // Guión de consulta claro y estructurado en primera persona
    val scriptText = buildString {
        append(context.getString(R.string.sos_script_intro, lmpFormatted, daysElapsedText))
        append(" ")
        append(context.getString(R.string.sos_script_cycle_length, avgCycle))
        append(" ")
        if (variation <= 9) {
            append(context.getString(R.string.sos_script_regular, variation))
        } else {
            append(context.getString(R.string.sos_script_variable, variation))
        }
        append(" ")
        append(context.getString(R.string.sos_script_period_duration, avgPeriod))
        append(" ")
        if (maxPain > 0f) {
            append(context.getString(R.string.sos_script_pain, maxPain.toInt()))
            append(" ")
        }
        if (isOnContraceptive) {
            append(context.getString(R.string.sos_script_contraceptive_yes))
        } else {
            append(context.getString(R.string.sos_script_contraceptive_no))
        }
        append(" ")
        append(context.getString(R.string.sos_script_outro))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Cabecera Cálida y Tranquilizadora ─────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VolunteerActivism,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.sos_sheet_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.sos_sheet_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.sos_cd_close), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.sos_header_intro),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── GUIÓN DE CONSULTA ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sos_script_card_title),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.sos_sheet_title), scriptText))
                                Toast.makeText(context, context.getString(R.string.sos_script_copy_toast), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.sos_cd_copy),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "«$scriptText»",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.sos_script_copy_tip),
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (stats == null && recentCycles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.sos_empty_history),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // ── SECCIÓN 1: Lo que suele preguntar el especialista ──────────
                EmpatheticSectionHeader(stringResource(R.string.sos_sec_doctor_questions))

                // Pregunta 1: FUM
                FriendlyQuestionCard(
                    icon = Icons.Rounded.CalendarToday,
                    friendlyTag = stringResource(R.string.sos_tag_lmp),
                    question = stringResource(R.string.sos_q_lmp),
                    mainAnswer = lmpFormatted,
                    explanation = if (daysSinceLmp != null) {
                        stringResource(R.string.sos_expl_lmp_days, daysSinceLmp, daysSinceLmp)
                    } else {
                        stringResource(R.string.sos_expl_lmp_pending)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pregunta 2: Duración y Regularidad
                val regularityFriendly = if (variation <= 9) {
                    stringResource(R.string.sos_regularity_regular, variation)
                } else {
                    stringResource(R.string.sos_regularity_variable, variation)
                }
                FriendlyQuestionCard(
                    icon = Icons.Rounded.Repeat,
                    friendlyTag = stringResource(R.string.sos_tag_cycle),
                    question = stringResource(R.string.sos_q_cycle),
                    mainAnswer = stringResource(R.string.sos_ans_cycle, avgCycle, regularityFriendly),
                    explanation = stringResource(
                        R.string.sos_expl_cycle,
                        stats?.shortestCycle ?: avgCycle,
                        stats?.longestCycle ?: avgCycle
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pregunta 3: Días de Sangrado
                val durationFriendly = if (avgPeriod <= 8) {
                    stringResource(R.string.sos_duration_normal)
                } else {
                    stringResource(R.string.sos_duration_long)
                }
                FriendlyQuestionCard(
                    icon = Icons.Rounded.WaterDrop,
                    friendlyTag = stringResource(R.string.sos_tag_period),
                    question = stringResource(R.string.sos_q_period),
                    mainAnswer = stringResource(R.string.sos_ans_period, avgPeriod, durationFriendly),
                    explanation = stringResource(R.string.sos_expl_period)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pregunta 4: Dolor y Severidad
                val painFriendly = when {
                    maxPain >= 7f -> stringResource(R.string.sos_pain_high, maxPain.toInt())
                    maxPain >= 4f -> stringResource(R.string.sos_pain_moderate, maxPain.toInt())
                    else          -> stringResource(R.string.sos_pain_mild, maxPain.toInt())
                }
                FriendlyQuestionCard(
                    icon = Icons.Rounded.LocalHospital,
                    friendlyTag = stringResource(R.string.sos_tag_pain),
                    question = stringResource(R.string.sos_q_pain),
                    mainAnswer = painFriendly,
                    explanation = stringResource(R.string.sos_expl_pain)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // ── SECCIÓN 2: Aspectos sugeridos para la conversación ────────
                val hasRedFlag = maxPain >= 7f || variation > 9 || avgPeriod > 8 || medicalAlerts.isNotEmpty()
                if (hasRedFlag) {
                    EmpatheticSectionHeader(stringResource(R.string.sos_sec_key_notes))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFF7ED)) // Tono cálido durazno suave
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Favorite, null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.sos_notes_box_title),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C),
                                fontSize = 13.5.sp
                            )
                        }

                        Text(
                            stringResource(R.string.sos_notes_box_subtitle),
                            fontSize = 12.sp,
                            color = Color(0xFF7C2D12),
                            lineHeight = 16.sp
                        )

                        if (maxPain >= 7f) {
                            Text(
                                stringResource(R.string.sos_note_pain, maxPain.toInt()),
                                fontSize = 12.sp,
                                color = Color(0xFF7C2D12),
                                lineHeight = 16.sp
                            )
                        }
                        if (variation > 9) {
                            Text(
                                stringResource(R.string.sos_note_variation, variation),
                                fontSize = 12.sp,
                                color = Color(0xFF7C2D12),
                                lineHeight = 16.sp
                            )
                        }
                        if (avgPeriod > 8) {
                            Text(
                                stringResource(R.string.sos_note_period),
                                fontSize = 12.sp,
                                color = Color(0xFF7C2D12),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                }

                // ── SECCIÓN 3: Dudas sugeridas para consultar ──────────────────
                EmpatheticSectionHeader(stringResource(R.string.sos_sec_checklist_title))

                val checklistQuestions = listOf(
                    0 to R.string.sos_checklist_q1,
                    1 to R.string.sos_checklist_q2,
                    2 to R.string.sos_checklist_q3,
                    3 to R.string.sos_checklist_q4,
                    4 to R.string.sos_checklist_q5
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    checklistQuestions.forEach { (id, textRes) ->
                        val isChecked = checkedQuestions.contains(id)
                        val textColor by animateColorAsState(
                            if (isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    checkedQuestions = if (isChecked) checkedQuestions - id else checkedQuestions + id
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isChecked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(textRes),
                                fontSize = 13.sp,
                                color = textColor,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                lineHeight = 17.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // ── SECCIÓN 4: Botón de Exportación Principal ─────────────────
                Button(
                    onClick = onExportPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.sos_btn_export_pdf),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.sos_pdf_footer_note),
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.5.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun EmpatheticSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 15.5.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    )
}

@Composable
private fun FriendlyQuestionCard(
    icon: ImageVector,
    friendlyTag: String,
    question: String,
    mainAnswer: String,
    explanation: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = question,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = friendlyTag,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = mainAnswer,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = explanation,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}
