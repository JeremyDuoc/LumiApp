package com.jeremy.lumi.ui.screens.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.domain.util.ClinicalLabels
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  DAILY LOG SHEET — iOS-style redesign
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLogSheet(
    day: Int,
    month: Int,
    year: Int,
    savedLog: com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms?,
    activeCategories: Set<String>,
    onActiveCategoriesChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean = false,
    onSave: (
        flow: String?,
        pain: Int,
        mood: String?,
        symptoms: List<String>,
        mucus: String?,
        notes: String,
        hadIntercourse: Boolean,
        protectionUsed: Boolean?,
        contraceptionMethod: String?,
        intercourseNotes: String?,
        showOnCalendar: Boolean,
        sleepHours: Float?,
        energyLevel: Int?,
        stressLevel: Int?,
        waterIntake: Float?,
        basalBodyTemp: Float?,
        spotting: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isEditing        = savedLog != null
    var selectedFlow     by remember { mutableStateOf<Int?>(null) }
    var painLevel        by remember { mutableFloatStateOf(0f) }
    var selectedMood     by remember { mutableStateOf<Int?>(null) }
    var selectedMucus    by remember { mutableStateOf<Int?>(null) }
    var selectedSymptoms by remember { mutableStateOf(setOf<Int>()) }
    var notes            by remember { mutableStateOf("") }
    
    var showCustomizeSheet by remember { mutableStateOf(false) }

    var hadIntercourse   by remember { mutableStateOf(false) }
    var protectionUsed   by remember { mutableStateOf<Boolean?>(null) }
    var selectedMethod   by remember { mutableStateOf<Int?>(null) }
    var intercourseNotes by remember { mutableStateOf("") }
    var showOnCalendar   by remember { mutableStateOf(true) }

    var sleepHours       by remember { mutableStateOf<Float?>(null) }
    var waterIntake      by remember { mutableStateOf<Float?>(null) }
    var energyLevel      by remember { mutableStateOf<Int?>(null) }
    var stressLevel      by remember { mutableStateOf<Int?>(null) }
    var basalBodyTemp    by remember { mutableStateOf("") }
    var spotting         by remember { mutableStateOf(false) }

    val flowOptions    = listOf(R.string.flow_light, R.string.flow_medium, R.string.flow_heavy)
    val moodOptions    = listOf(
        R.string.mood_happy, R.string.mood_calm, R.string.mood_energetic, R.string.mood_sensitive,
        R.string.mood_sad, R.string.mood_anxious, R.string.mood_irritated, R.string.mood_overwhelmed
    )
    val mucusOptions   = listOf(R.string.mucus_dry, R.string.mucus_sticky, R.string.mucus_creamy, R.string.mucus_watery, R.string.mucus_egg_white)
    
    val physicalSymptoms = listOf(
        R.string.symp_cramps, R.string.symp_headache, R.string.symp_migraine, R.string.symp_back_pain,
        R.string.symp_acne, R.string.symp_bloating, R.string.symp_fatigue, R.string.symp_pelvic_pain,
        R.string.symp_breast_tenderness, R.string.symp_dizziness, R.string.symp_hot_flashes, R.string.symp_insomnia, R.string.symp_fever
    )
    val digestiveSymptoms = listOf(R.string.symp_nausea, R.string.symp_diarrhea, R.string.symp_constipation)
    val clinicalSymptoms  = listOf(
        R.string.symp_libido_high, R.string.symp_libido_low,
        R.string.symp_lh_positive, R.string.symp_lh_negative
    )
    val symptomOptions = physicalSymptoms + digestiveSymptoms + clinicalSymptoms
    
    val methodOptions  = listOf(
        R.string.method_condom, R.string.method_pill, R.string.method_iud,
        R.string.method_patch, R.string.method_ring, R.string.method_none
    )

    val flowStrings    = flowOptions.map    { stringResource(id = it) }
    val moodStrings    = moodOptions.map    { stringResource(id = it) }
    val mucusStrings   = mucusOptions.map   { stringResource(id = it) }
    val symptomStrings = symptomOptions.map { stringResource(id = it) }
    val methodStrings  = methodOptions.map  { stringResource(id = it) }

    var unrecognizedSymptoms by remember { mutableStateOf<List<com.jeremy.lumi.data.local.entity.SymptomEntity>>(emptyList()) }

    LaunchedEffect(savedLog) {
        savedLog?.let { log ->
            selectedFlow = ClinicalLabels.findFlowResId(log.dailyLog.flowIntensity)
                ?: log.dailyLog.flowIntensity?.let { saved ->
                    val i = flowStrings.indexOf(saved); if (i >= 0) flowOptions[i] else null
                }
            painLevel = log.dailyLog.painLevel.toFloat().coerceIn(0f, 10f)
            selectedMood = ClinicalLabels.findMoodResId(log.dailyLog.mood)
                ?: log.dailyLog.mood?.let { saved ->
                    val i = moodStrings.indexOf(saved); if (i >= 0) moodOptions[i] else null
                }
            selectedMucus = ClinicalLabels.findMucusResId(log.dailyLog.cervicalMucus)
                ?: log.dailyLog.cervicalMucus?.let { saved ->
                    val i = mucusStrings.indexOf(saved); if (i >= 0) mucusOptions[i] else null
                }
            val sel = mutableSetOf<Int>()
            val unrec = mutableListOf<com.jeremy.lumi.data.local.entity.SymptomEntity>()
            log.symptoms.forEach { s ->
                val resId = ClinicalLabels.findSymptomResId(s.name)
                    ?: symptomStrings.indexOf(s.name).takeIf { it >= 0 }?.let { symptomOptions[it] }
                if (resId != null) {
                    sel.add(resId)
                } else {
                    unrec.add(s)
                }
            }
            selectedSymptoms = sel
            unrecognizedSymptoms = unrec
            notes            = log.dailyLog.notes ?: ""
            hadIntercourse   = log.dailyLog.hadIntercourse
            protectionUsed   = log.dailyLog.protectionUsed
            selectedMethod   = ClinicalLabels.findMethodResId(log.dailyLog.contraceptionMethod)
                ?: log.dailyLog.contraceptionMethod?.let { saved ->
                    val i = methodStrings.indexOf(saved); if (i >= 0) methodOptions[i] else null
                }
            intercourseNotes = log.dailyLog.intercourseNotes ?: ""
            showOnCalendar   = log.dailyLog.showIntercourseOnCalendar
            
            sleepHours       = log.dailyLog.sleepHours
            waterIntake      = log.dailyLog.waterIntake
            energyLevel      = log.dailyLog.energyLevel
            stressLevel      = log.dailyLog.stressLevel
            basalBodyTemp    = log.dailyLog.basalBodyTemp?.toString() ?: ""
            spotting         = log.dailyLog.spotting
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.background,
        modifier         = Modifier.fillMaxHeight(),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // ── Header ────────────────────────────────────────────────
            SheetHeader(day = day, month = month, year = year)
            Spacer(Modifier.height(4.dp))

            // ── Scrollable content ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                var flowExpanded     by remember { mutableStateOf(true) }
                var painExpanded     by remember { mutableStateOf(true) }
                var moodExpanded     by remember { mutableStateOf(false) }
                var symptomsExpanded by remember { mutableStateOf(false) }
                var mucusExpanded    by remember { mutableStateOf(false) }
                var intercourseExp   by remember { mutableStateOf(false) }
                var notesExpanded    by remember { mutableStateOf(false) }
                
                var wellbeingExp     by remember { mutableStateOf(false) }
                var vitalsExp        by remember { mutableStateOf(false) }
                
                var showMucusInfo    by remember { mutableStateOf(false) }

                if (showMucusInfo) {
                    AlertDialog(
                        onDismissRequest = { showMucusInfo = false },
                        title = { Text(stringResource(R.string.log_cervical_mucus)) },
                        text = { Text(stringResource(R.string.log_mucus_info_desc)) },
                        confirmButton = {
                            TextButton(onClick = { showMucusInfo = false }) {
                                Text(stringResource(R.string.btn_understood))
                            }
                        }
                    )
                }

                LaunchedEffect(savedLog) {
                    if (savedLog != null) {
                        if (savedLog.dailyLog.mood != null)           moodExpanded     = true
                        if (savedLog.symptoms.isNotEmpty())           symptomsExpanded = true
                        if (savedLog.dailyLog.cervicalMucus != null)  mucusExpanded    = true
                        if (savedLog.dailyLog.hadIntercourse)         intercourseExp   = true
                        if (!savedLog.dailyLog.notes.isNullOrBlank()) notesExpanded    = true
                        if (savedLog.dailyLog.sleepHours != null || savedLog.dailyLog.energyLevel != null || savedLog.dailyLog.stressLevel != null) wellbeingExp = true
                        if (savedLog.dailyLog.basalBodyTemp != null) vitalsExp = true
                    }
                }

                // ── GRUPO 1: Sangrado y Dolor ────────────────────────────────────────
                Text(stringResource(R.string.log_section_bleeding_pain), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))

                CollapsibleLogCard(
                    titleRes  = R.string.log_flow,
                    icon      = Icons.Rounded.WaterDrop,
                    expanded  = flowExpanded,
                    hasData   = selectedFlow != null || spotting,
                    onToggle  = { flowExpanded = !flowExpanded }
                ) {
                    FlowSelector(
                        options  = flowOptions,
                        selected = selectedFlow,
                        onSelect = { tapped ->
                            selectedFlow = if (selectedFlow == tapped) null else tapped
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { spotting = !spotting }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.log_spotting), fontSize = 14.sp)
                        Switch(checked = spotting, onCheckedChange = { spotting = it })
                    }
                }

                CollapsibleLogCard(
                    titleRes = R.string.log_pain,
                    icon     = Icons.Rounded.LocalHospital,
                    expanded = painExpanded,
                    hasData  = painLevel > 0f,
                    onToggle = { painExpanded = !painExpanded }
                ) {
                    PainSelector(
                        painLevel     = painLevel,
                        onValueChange = { painLevel = it }
                    )
                }

                // ── GRUPO 2: Estado y Síntomas ───────────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.log_section_mood_symptoms), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))

                CollapsibleLogCard(
                    titleRes = R.string.log_mood,
                    icon     = Icons.Rounded.Cloud,
                    expanded = moodExpanded,
                    hasData  = selectedMood != null,
                    onToggle = { moodExpanded = !moodExpanded }
                ) {
                    MoodSelector(
                        options  = moodOptions,
                        selected = selectedMood,
                        onSelect = { selectedMood = it }
                    )
                }

                if (activeCategories.contains("physical") || activeCategories.contains("digestive")) {
                    CollapsibleLogCard(
                        titleRes = R.string.log_symptoms,
                        icon     = Icons.Rounded.Info,
                        expanded = symptomsExpanded,
                        hasData  = selectedSymptoms.isNotEmpty(),
                        onToggle = { symptomsExpanded = !symptomsExpanded }
                    ) {
                        val visibleSymptoms = mutableListOf<Int>()
                        if (activeCategories.contains("physical"))  visibleSymptoms.addAll(physicalSymptoms)
                        if (activeCategories.contains("digestive")) visibleSymptoms.addAll(digestiveSymptoms)
                        visibleSymptoms.addAll(clinicalSymptoms)

                        Box(modifier = Modifier.fillMaxWidth()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding        = PaddingValues(end = 32.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                items(visibleSymptoms) { optionRes ->
                                    val isSelected = selectedSymptoms.contains(optionRes)
                                    PillChip(
                                        text       = stringResource(id = optionRes),
                                        isSelected = isSelected,
                                        onClick    = {
                                            selectedSymptoms =
                                                if (isSelected) selectedSymptoms - optionRes
                                                else selectedSymptoms + optionRes
                                        }
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(32.dp)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                                        )
                                    )
                            )
                        }
                    }
                }

                if (activeCategories.contains("mucus")) {
                    CollapsibleLogCard(
                        titleRes = R.string.log_cervical_mucus,
                        icon     = Icons.Rounded.Opacity,
                        expanded = mucusExpanded,
                        hasData  = selectedMucus != null,
                        onToggle = { mucusExpanded = !mucusExpanded },
                        onInfoClick = { showMucusInfo = true }
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding        = PaddingValues(start = 0.dp, top = 4.dp, end = 0.dp, bottom = 4.dp)
                        ) {
                            items(mucusOptions) { optionRes ->
                                PillChip(
                                    text       = stringResource(id = optionRes),
                                    isSelected = selectedMucus == optionRes,
                                    onClick    = {
                                        selectedMucus = if (selectedMucus == optionRes) null else optionRes
                                    }
                                )
                            }
                        }
                    }
                }

                // ── GRUPO 3: Bienestar e Intimidad ───────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.log_section_wellbeing_intimacy), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))

                if (activeCategories.contains("intercourse")) {
                    CollapsibleLogCard(
                        titleRes = R.string.log_intercourse,
                        icon     = Icons.Rounded.Favorite,
                        expanded = intercourseExp,
                        hasData  = hadIntercourse,
                        onToggle = { intercourseExp = !intercourseExp }
                    ) {
                        IntercourseCard(
                            hadIntercourse           = hadIntercourse,
                            onHadIntercourseChange   = { hadIntercourse = it },
                            protectionUsed           = protectionUsed,
                            onProtectionChange       = { newProtection ->
                                protectionUsed = newProtection
                                if (newProtection == false) selectedMethod = null
                            },
                            methodOptions            = methodOptions,
                            selectedMethod           = selectedMethod,
                            onMethodSelect           = { selectedMethod = it },
                            intercourseNotes         = intercourseNotes,
                            onIntercourseNotesChange = { intercourseNotes = it },
                            showOnCalendar           = showOnCalendar,
                            onShowOnCalendarChange   = { showOnCalendar = it }
                        )
                    }
                }
                
                if (activeCategories.contains("wellbeing")) {
                    CollapsibleLogCard(
                        titleRes = R.string.category_wellbeing,
                        icon     = Icons.Rounded.LocalFlorist,
                        expanded = wellbeingExp,
                        hasData  = sleepHours != null || waterIntake != null || energyLevel != null || stressLevel != null,
                        onToggle = { wellbeingExp = !wellbeingExp }
                    ) {
                        val haptic = LocalHapticFeedback.current
                        
                        WellbeingSliderRow(
                            label = stringResource(R.string.log_sleep_hours),
                            value = sleepHours,
                            valueDisplay = sleepHours?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                            range = 0f..14f,
                            steps = 27,
                            onValueChange = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                sleepHours = if (it < 0.5f) null else it 
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        WellbeingSliderRow(
                            label = stringResource(R.string.log_water_liters),
                            value = waterIntake,
                            valueDisplay = waterIntake?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                            range = 0f..5f,
                            steps = 9,
                            onValueChange = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                waterIntake = if (it < 0.2f) null else it 
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        WellbeingSliderRow(
                            label = stringResource(R.string.log_energy_level),
                            value = energyLevel?.toFloat(),
                            valueDisplay = energyLevel?.toString() ?: "—",
                            range = 0f..10f,
                            steps = 9,
                            onValueChange = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                energyLevel = if (it < 1f) null else it.toInt() 
                            }
                        )
                        Spacer(Modifier.height(16.dp))

                        WellbeingSliderRow(
                            label = stringResource(R.string.log_stress_level),
                            value = stressLevel?.toFloat(),
                            valueDisplay = stressLevel?.toString() ?: "—",
                            range = 0f..10f,
                            steps = 9,
                            onValueChange = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                stressLevel = if (it < 1f) null else it.toInt() 
                            }
                        )
                    }
                }

                if (activeCategories.contains("vitals")) {
                    CollapsibleLogCard(
                        titleRes = R.string.category_vitals,
                        icon     = Icons.Rounded.Info,
                        expanded = vitalsExp,
                        hasData  = basalBodyTemp.isNotBlank(),
                        onToggle = { vitalsExp = !vitalsExp }
                    ) {
                        OutlinedTextField(
                            value = basalBodyTemp,
                            onValueChange = { basalBodyTemp = it },
                            label = { Text(stringResource(R.string.log_bbt_label)) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                // ── Notas ─────────────────────────────────────────────────────────
                Spacer(Modifier.height(8.dp))
                CollapsibleLogCard(
                    titleRes = R.string.log_notes,
                    icon     = Icons.Rounded.Edit,
                    expanded = notesExpanded,
                    hasData  = notes.isNotBlank(),
                    onToggle = { notesExpanded = !notesExpanded }
                ) {
                    OutlinedTextField(
                        value         = notes,
                        onValueChange = { notes = it },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder   = {
                            Text(
                                stringResource(id = R.string.log_notes_hint),
                                color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                                fontSize = 14.sp
                            )
                        },
                        shape  = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            unfocusedBorderColor    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                            focusedContainerColor   = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                TextButton(
                    onClick = { showCustomizeSheet = true },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp)
                        .height(52.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_customize_log),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Botón guardar / actualizar ───────────────────────────────────
            Spacer(Modifier.height(12.dp))
            SaveButton(
                enabled = !isSaving,
                isEditing = isEditing,
                onClick = {
                    val finalFlow     = selectedFlow?.let    { ClinicalLabels.getCanonicalFlow(it) ?: flowStrings[flowOptions.indexOf(it)] }
                    val finalMood     = selectedMood?.let    { ClinicalLabels.getCanonicalMood(it) ?: moodStrings[moodOptions.indexOf(it)] }
                    val finalSymptoms = selectedSymptoms.mapNotNull { ClinicalLabels.getCanonicalSymptom(it) ?: symptomStrings.getOrNull(symptomOptions.indexOf(it)) } + unrecognizedSymptoms.map { it.name }
                    val finalMucus    = selectedMucus?.let   { ClinicalLabels.getCanonicalMucus(it) ?: mucusStrings[mucusOptions.indexOf(it)] }
                    val finalMethod   = selectedMethod?.let  { ClinicalLabels.getCanonicalMethod(it) ?: methodStrings[methodOptions.indexOf(it)] }
                    val bbtParsed = basalBodyTemp.replace(",", ".").toFloatOrNull()
                    onSave(
                        finalFlow, painLevel.toInt(), finalMood, finalSymptoms, finalMucus, notes,
                        hadIntercourse,
                        if (hadIntercourse) protectionUsed else null,
                        if (hadIntercourse) finalMethod    else null,
                        if (hadIntercourse) intercourseNotes.takeIf { it.isNotBlank() } else null,
                        showOnCalendar,
                        sleepHours,
                        energyLevel,
                        stressLevel,
                        waterIntake,
                        bbtParsed,
                        spotting
                    )
                }
            )
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    if (showCustomizeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCustomizeSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    stringResource(R.string.btn_customize_log),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                
                val categories = listOf(
                    "physical" to stringResource(R.string.category_physical),
                    "digestive" to stringResource(R.string.category_digestive),
                    "mucus" to stringResource(R.string.category_mucus),
                    "intercourse" to stringResource(R.string.category_intercourse),
                    "wellbeing" to stringResource(R.string.category_wellbeing),
                    "vitals" to stringResource(R.string.category_vitals)
                )
                
                categories.forEach { (key, title) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable {
                                val newCats = activeCategories.toMutableSet()
                                if (newCats.contains(key)) newCats.remove(key) else newCats.add(key)
                                onActiveCategoriesChange(newCats)
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = activeCategories.contains(key),
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun WellbeingSliderRow(
    label: String,
    value: Float?,
    valueDisplay: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = valueDisplay,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Slider(
            value = value ?: range.start,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SheetHeader(day: Int, month: Int, year: Int) {
    val monthName = remember(month, year) {
        LocalDate.of(year, month, day)
            .format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es")))
    }
    Row(
        modifier          = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = day.toString(),
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text       = stringResource(id = R.string.log_title),
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text     = monthName,
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  LOG CARD — contenedor visual de cada sección
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LogCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        content = content
    )
}

@Composable
private fun CardSectionTitle(titleRes: Int) {
    Text(
        text       = stringResource(id = titleRes),
        fontSize   = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        letterSpacing = 0.4.sp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  COLLAPSIBLE LOG CARD — sección expandible con header clickeable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CollapsibleLogCard(
    titleRes : Int,
    icon     : ImageVector,
    expanded : Boolean,
    hasData  : Boolean,
    onToggle : () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    content  : @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val chevronRotation by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label         = "chevron_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            
            Text(
                text          = stringResource(id = titleRes),
                fontSize      = 14.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                letterSpacing = 0.4.sp,
                modifier      = Modifier.weight(1f)
            )
            
            if (onInfoClick != null) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onInfoClick()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = stringResource(R.string.cd_info),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            
            if (hasData) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                imageVector        = Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier           = Modifier
                    .size(24.dp)
                    .rotate(chevronRotation),
                tint               = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically() + fadeIn(tween(200)),
            exit    = shrinkVertically() + fadeOut(tween(150))
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                content  = content
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FLOW SELECTOR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FlowSelector(options: List<Int>, selected: Int?, onSelect: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, optionRes ->
            val isSelected = selected == optionRes
            val bgAnim by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else            MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(220), label = "flow_bg_$index"
            )
            val contentAnim by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary
                else            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                animationSpec = tween(220), label = "flow_fg_$index"
            )
            val borderModifier = if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier
            val scaleAnim by animateFloatAsState(
                targetValue   = if (isSelected) 1.02f else 1f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label         = "flow_scale_$index"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .scale(scaleAnim)
                    .clip(RoundedCornerShape(16.dp))
                    .then(borderModifier)
                    .background(bgAnim)
                    .clickable(
                        indication             = null,
                        interactionSource      = remember { MutableInteractionSource() }
                    ) { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(optionRes) 
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(index + 1) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(contentAnim)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text       = stringResource(id = optionRes),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = contentAnim
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MOOD SELECTOR 
// ─────────────────────────────────────────────────────────────────────────────

private val moodIcons = listOf(
    Icons.Rounded.LocalFlorist,
    Icons.Rounded.Spa,
    Icons.Rounded.Bolt,
    Icons.Rounded.FavoriteBorder,
    Icons.Rounded.Cloud,
    Icons.Rounded.Psychology,
    Icons.Rounded.LocalFireDepartment,
    Icons.Rounded.Waves
)

@Composable
private fun MoodSelector(options: List<Int>, selected: Int?, onSelect: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val chunked = options.chunked(4)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chunked.forEachIndexed { rowIndex, rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowOptions.forEachIndexed { colIndex, optionRes ->
                    val globalIndex = rowIndex * 4 + colIndex
                    val isSelected = selected == optionRes
                    val bgAnim by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        animationSpec = tween(200), label = "mood_bg_$globalIndex"
                    )
                    val textAnim by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        animationSpec = tween(200), label = "mood_text_$globalIndex"
                    )
                    val borderModifier = if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier
                    val scaleAnim by animateFloatAsState(
                        targetValue = if (isSelected) 1.02f else 1f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                        label = "mood_scale_$globalIndex"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .scale(scaleAnim)
                            .clip(RoundedCornerShape(16.dp))
                            .then(borderModifier)
                            .background(bgAnim)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(optionRes)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = moodIcons.getOrElse(globalIndex) { Icons.Rounded.Info },
                                contentDescription = null,
                                tint = textAnim,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = optionRes),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textAnim,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PAIN SELECTOR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PainSelector(painLevel: Float, onValueChange: (Float) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val neutral = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    val safe    = MaterialTheme.colorScheme.primary
    val warning = MaterialTheme.colorScheme.tertiary
    val danger  = MaterialTheme.colorScheme.error

    val rawColor = if (painLevel == 0f) neutral else {
        val t = (painLevel - 1f) / 9f
        if (t < 0.5f) lerp(safe, warning, t * 2f)
        else          lerp(warning, danger, (t - 0.5f) * 2f)
    }
    val animColor by animateColorAsState(rawColor, tween(280), label = "pain_color")

    Column {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedContent(
                    targetState    = painLevel.toInt(),
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith
                                (slideOutVertically { -it / 2 } + fadeOut())
                    },
                    label = "pain_num"
                ) { value ->
                    Text(
                        text       = if (value == 0) "—" else value.toString(),
                        fontSize   = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = animColor
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text       = "/ 10",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    modifier   = Modifier.padding(bottom = 6.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(animColor.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text       = stringResource(id = painLabelRes(painLevel.toInt())),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = animColor
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Slider(
            value         = painLevel,
            onValueChange = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onValueChange(it) 
            },
            valueRange    = 0f..10f,
            steps         = 9,
            colors        = SliderDefaults.colors(
                thumbColor         = animColor,
                activeTrackColor   = animColor,
                inactiveTrackColor = animColor.copy(alpha = 0.18f)
            )
        )
    }
}

private fun painLabelRes(level: Int): Int = when (level) {
    0       -> R.string.pain_none
    in 1..3 -> R.string.pain_mild
    in 4..6 -> R.string.pain_moderate
    else    -> R.string.pain_intense
}

// ─────────────────────────────────────────────────────────────────────────────
//  INTERCOURSE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IntercourseCard(
    hadIntercourse: Boolean,
    onHadIntercourseChange: (Boolean) -> Unit,
    protectionUsed: Boolean?,
    onProtectionChange: (Boolean?) -> Unit,
    methodOptions: List<Int>,
    selectedMethod: Int?,
    onMethodSelect: (Int?) -> Unit,
    intercourseNotes: String,
    onIntercourseNotesChange: (String) -> Unit,
    showOnCalendar: Boolean,
    onShowOnCalendarChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = stringResource(R.string.log_had_intercourse_question),
                fontSize   = 14.sp,
                color      = MaterialTheme.colorScheme.onBackground
            )
            com.popovanton0.heartswitch.HeartSwitch(
                checked         = hadIntercourse,
                onCheckedChange = { checked ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onHadIntercourseChange(checked)
                    if (!checked) {
                        onProtectionChange(null)
                        onMethodSelect(null)
                        onIntercourseNotesChange("")
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = hadIntercourse,
            enter   = expandVertically(animationSpec = tween(280, easing = EaseOutCubic)) + fadeIn(tween(240)),
            exit    = shrinkVertically(animationSpec = tween(220, easing = EaseInCubic)) + fadeOut(tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f),
                    thickness = 1.dp
                )

                CardSectionTitle(titleRes = R.string.log_protection)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillChip(
                        text       = stringResource(id = R.string.protection_yes),
                        isSelected = protectionUsed == true,
                        onClick    = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onProtectionChange(if (protectionUsed == true) null else true) 
                        }
                    )
                    PillChip(
                        text       = stringResource(id = R.string.protection_no),
                        isSelected = protectionUsed == false,
                        onClick    = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onProtectionChange(if (protectionUsed == false) null else false) 
                        }
                    )
                }

                AnimatedVisibility(
                    visible = protectionUsed == true,
                    enter   = expandVertically(tween(250)) + fadeIn(tween(200)),
                    exit    = shrinkVertically(tween(200)) + fadeOut(tween(150))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CardSectionTitle(titleRes = R.string.log_method)
                        val filteredMethods = methodOptions.filter { it != R.string.method_none }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp)
                        ) {
                            items(filteredMethods) { optionRes ->
                                PillChip(
                                    text       = stringResource(id = optionRes),
                                    isSelected = selectedMethod == optionRes,
                                    onClick    = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onMethodSelect(optionRes) 
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value         = intercourseNotes,
                    onValueChange = onIntercourseNotesChange,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .height(76.dp),
                    placeholder   = {
                        Text(
                            stringResource(id = R.string.log_intercourse_notes),
                            color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            fontSize = 14.sp
                        )
                    },
                    shape  = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                        focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text     = stringResource(id = R.string.log_show_on_calendar),
                        fontSize = 14.sp,
                        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked         = showOnCalendar,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShowOnCalendarChange(it)
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PILL CHIP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PillChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val bg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else            MaterialTheme.colorScheme.surfaceVariant,
        tween(220), label = "chip_bg"
    )
    val fg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        tween(220), label = "chip_fg"
    )
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.02f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label         = "chip_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(26.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(26.dp))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick() 
            }
            .padding(horizontal = 20.dp)
            .heightIn(min = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SAVE BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SaveButton(isEditing: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush)
            .clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = if (isEditing) stringResource(id = R.string.btn_update_log)
                         else           stringResource(id = R.string.btn_save_log),
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onPrimary,
            letterSpacing = 0.5.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  LEGACY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionTitle(titleRes: Int) {
    Text(
        text       = stringResource(id = titleRes),
        fontSize   = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun ChipSelectionRow(options: List<Int>, singleSelection: Int?, onSelect: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { optionRes ->
            PillChip(
                text       = stringResource(id = optionRes),
                isSelected = singleSelection == optionRes,
                onClick    = { onSelect(optionRes) }
            )
        }
    }
}

@Composable
fun CleanChip(text: String, isSelected: Boolean, onClick: () -> Unit) =
    PillChip(text = text, isSelected = isSelected, onClick = onClick)
