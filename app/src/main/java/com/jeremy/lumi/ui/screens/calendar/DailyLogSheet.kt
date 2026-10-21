package com.jeremy.lumi.ui.screens.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.local.entity.SymptomEntity
import com.jeremy.lumi.domain.util.ClinicalLabels
import com.jeremy.lumi.ui.theme.LumiSans
import com.jeremy.lumi.ui.theme.LumiEditorialSerif
import androidx.compose.ui.text.font.FontStyle
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.popovanton0.heartswitch.HeartSwitch
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  MODELOS DE DATOS DEL CATÁLOGO DE SÍNTOMAS
// ─────────────────────────────────────────────────────────────────────────────

private data class SymptomCatalogItem(
    val name: String,
    val resId: Int? = null,
    val category: String
)

// ─────────────────────────────────────────────────────────────────────────────
//  DAILY LOG SHEET — Rediseño Intuitivo e Interactivo (Inspirado en el prototipo v2)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLogSheet(
    day: Int,
    month: Int,
    year: Int,
    savedLog: DailyLogWithSymptoms?,
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
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isEditing = savedLog != null

    // ── Estados de selección del registro ────────────────────────────────────
    var selectedFlow     by remember { mutableStateOf<Int?>(null) } // null = "Ninguno", o R.string.flow_*
    var flowIsNone       by remember { mutableStateOf(false) }     // Selección explícita de "Ninguno"
    var spotting         by remember { mutableStateOf(false) }
    var painLevel        by remember { mutableFloatStateOf(0f) }
    var selectedMood     by remember { mutableStateOf<Int?>(null) }
    var selectedMucus    by remember { mutableStateOf<Int?>(null) }

    // Síntomas
    var selectedSymptoms by remember { mutableStateOf(setOf<Int>()) } // IDs de recursos conocidos
    var unrecognizedSymptoms by remember { mutableStateOf<List<SymptomEntity>>(emptyList()) }
    var customSymptoms   by remember { mutableStateOf(listOf<String>()) }
    var selectedCustomSymptoms by remember { mutableStateOf(setOf<String>()) }
    val symptomIntensities = remember { mutableStateMapOf<String, Int>() } // 1: Leve, 2: Moderada, 3: Intensa

    // Intimidad / Actividad sexual
    var hadIntercourse   by remember { mutableStateOf(false) }
    var intercourseAnsweredNo by remember { mutableStateOf(false) }
    var protectionUsed   by remember { mutableStateOf<Boolean?>(null) }
    var selectedMethod   by remember { mutableStateOf<Int?>(null) }
    var intercourseNotes by remember { mutableStateOf("") }
    var showOnCalendar   by remember { mutableStateOf(true) }

    // Bienestar
    var sleepHours       by remember { mutableStateOf<Float?>(null) }
    var waterIntake      by remember { mutableStateOf<Float?>(null) }
    var energyLevel      by remember { mutableStateOf<Int?>(null) } // 1..5
    var stressLevel      by remember { mutableStateOf<Int?>(null) } // 1..5
    var basalBodyTemp    by remember { mutableStateOf("") }

    // Notas
    var notes            by remember { mutableStateOf("") }

    // ── Opciones maestras ───────────────────────────────────────────────────
    val flowOptions = listOf(R.string.flow_light, R.string.flow_medium, R.string.flow_heavy)
    val moodOptions = listOf(
        R.string.mood_happy, R.string.mood_calm, R.string.mood_energetic, R.string.mood_sensitive,
        R.string.mood_sad, R.string.mood_anxious, R.string.mood_irritated, R.string.mood_overwhelmed
    )
    val mucusOptions = listOf(
        R.string.mucus_dry, R.string.mucus_sticky, R.string.mucus_creamy,
        R.string.mucus_watery, R.string.mucus_egg_white
    )
    val methodOptions = listOf(
        R.string.method_condom, R.string.method_pill, R.string.method_iud,
        R.string.method_patch, R.string.method_ring, R.string.method_none
    )

    // Catálogo completo de síntomas organizado por categorías
    val catalogSymptoms = remember {
        listOf(
            // Dolor
            SymptomCatalogItem("Cólicos", R.string.symp_cramps, "Dolor"),
            SymptomCatalogItem("Dolor lumbar", R.string.symp_back_pain, "Dolor"),
            SymptomCatalogItem("Dolor de cabeza", R.string.symp_headache, "Dolor"),
            SymptomCatalogItem("Migraña", R.string.symp_migraine, "Dolor"),
            SymptomCatalogItem("Pecho sensible", R.string.symp_breast_tenderness, "Dolor"),
            SymptomCatalogItem("Dolor pélvico", R.string.symp_pelvic_pain, "Dolor"),
            SymptomCatalogItem("Dolor muscular", R.string.symp_muscle_pain, "Dolor"),
            SymptomCatalogItem("Calambres", R.string.symp_spasms, "Dolor"),

            // Digestivo
            SymptomCatalogItem("Hinchazón", R.string.symp_bloating, "Digestivo"),
            SymptomCatalogItem("Náuseas", R.string.symp_nausea, "Digestivo"),
            SymptomCatalogItem("Estreñimiento", R.string.symp_constipation, "Digestivo"),
            SymptomCatalogItem("Diarrea", R.string.symp_diarrhea, "Digestivo"),
            SymptomCatalogItem("Gases", R.string.symp_gas, "Digestivo"),
            SymptomCatalogItem("Acidez", R.string.symp_heartburn, "Digestivo"),
            SymptomCatalogItem("Antojos", R.string.symp_cravings, "Digestivo"),

            // Cuerpo
            SymptomCatalogItem("Fatiga / Cansancio", R.string.symp_fatigue, "Cuerpo"),
            SymptomCatalogItem("Mareos", R.string.symp_dizziness, "Cuerpo"),
            SymptomCatalogItem("Sofocos", R.string.symp_hot_flashes, "Cuerpo"),
            SymptomCatalogItem("Fiebre", R.string.symp_fever, "Cuerpo"),
            SymptomCatalogItem("Escalofríos", R.string.symp_chills, "Cuerpo"),
            SymptomCatalogItem("Retención de líquidos", R.string.symp_fluid_retention, "Cuerpo"),

            // Piel
            SymptomCatalogItem("Acné", R.string.symp_acne, "Piel"),
            SymptomCatalogItem("Piel seca", R.string.symp_dry_skin, "Piel"),
            SymptomCatalogItem("Piel grasa", R.string.symp_oily_skin, "Piel"),

            // Urinario e Íntimo
            SymptomCatalogItem("Orinar frecuente", R.string.symp_frequent_urination, "Urinario"),
            SymptomCatalogItem("Ardor al orinar", R.string.symp_burning_urination, "Urinario"),
            SymptomCatalogItem("Picazón vaginal", R.string.symp_vaginal_itching, "Íntimo"),
            SymptomCatalogItem("Sequedad vaginal", R.string.symp_vaginal_dryness, "Íntimo"),
            SymptomCatalogItem("Dolor en relaciones", R.string.symp_pain_intercourse, "Íntimo"),

            // Sueño y mente
            SymptomCatalogItem("Insomnio", R.string.symp_insomnia, "Sueño y mente"),
            SymptomCatalogItem("Deseo sexual alto", R.string.symp_libido_high, "Sueño y mente"),
            SymptomCatalogItem("Deseo sexual bajo", R.string.symp_libido_low, "Sueño y mente"),
            SymptomCatalogItem("Cambios de humor", R.string.symp_mood_swings, "Sueño y mente"),
            SymptomCatalogItem("Ansiedad", R.string.symp_anxiety, "Sueño y mente"),

            // Clínico
            SymptomCatalogItem("Test ovulación LH (+)", R.string.symp_lh_positive, "Clínico"),
            SymptomCatalogItem("Test ovulación LH (-)", R.string.symp_lh_negative, "Clínico")
        )
    }

    // Top 6 síntomas frecuentes para la pestaña "Hoy"
    val quickSymptoms = remember {
        listOf(
            R.string.symp_cramps,
            R.string.symp_bloating,
            R.string.symp_headache,
            R.string.symp_fatigue,
            R.string.symp_breast_tenderness,
            R.string.symp_acne
        )
    }

    val flowStrings   = flowOptions.map   { stringResource(id = it) }
    val moodStrings   = moodOptions.map   { stringResource(id = it) }
    val mucusStrings  = mucusOptions.map  { stringResource(id = it) }
    val methodStrings = methodOptions.map { stringResource(id = it) }

    // ── Carga de datos guardados ─────────────────────────────────────────────
    LaunchedEffect(savedLog) {
        savedLog?.let { log ->
            selectedFlow = ClinicalLabels.findFlowResId(log.dailyLog.flowIntensity)
                ?: log.dailyLog.flowIntensity?.let { saved ->
                    val i = flowStrings.indexOf(saved); if (i >= 0) flowOptions[i] else null
                }
            if (log.dailyLog.flowIntensity == "Sin flujo" || log.dailyLog.flowIntensity == "Ninguno") {
                flowIsNone = true
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

            val selResIds = mutableSetOf<Int>()
            val unrec = mutableListOf<SymptomEntity>()
            val customNames = mutableSetOf<String>()

            log.symptoms.forEach { s ->
                val resId = ClinicalLabels.findSymptomResId(s.name)
                if (resId != null) {
                    selResIds.add(resId)
                } else {
                    val inCatalog = catalogSymptoms.firstOrNull { it.name.equals(s.name, ignoreCase = true) }
                    if (inCatalog?.resId != null) {
                        selResIds.add(inCatalog.resId)
                    } else if (inCatalog != null) {
                        customNames.add(inCatalog.name)
                    } else {
                        unrec.add(s)
                        customNames.add(s.name)
                    }
                }
                symptomIntensities[s.name] = if (s.intensity in 1..3) s.intensity else 2
            }
            selectedSymptoms = selResIds
            unrecognizedSymptoms = unrec
            selectedCustomSymptoms = customNames
            customSymptoms = (customSymptoms + customNames).distinct()

            notes = log.dailyLog.notes ?: ""
            hadIntercourse = log.dailyLog.hadIntercourse
            protectionUsed = log.dailyLog.protectionUsed
            selectedMethod = ClinicalLabels.findMethodResId(log.dailyLog.contraceptionMethod)
                ?: log.dailyLog.contraceptionMethod?.let { saved ->
                    val i = methodStrings.indexOf(saved); if (i >= 0) methodOptions[i] else null
                }
            intercourseNotes = log.dailyLog.intercourseNotes ?: ""
            showOnCalendar = log.dailyLog.showIntercourseOnCalendar

            sleepHours  = log.dailyLog.sleepHours
            waterIntake = log.dailyLog.waterIntake
            energyLevel = log.dailyLog.energyLevel?.coerceIn(1, 5)
            stressLevel = log.dailyLog.stressLevel?.coerceIn(1, 5)
            basalBodyTemp = log.dailyLog.basalBodyTemp?.toString() ?: ""
            spotting    = log.dailyLog.spotting
        }
    }

    // ── Contador de elementos marcados ───────────────────────────────────────
    val loggedCount = remember(
        selectedFlow, flowIsNone, spotting, painLevel, selectedMood,
        selectedSymptoms, selectedCustomSymptoms, selectedMucus,
        hadIntercourse, intercourseAnsweredNo, sleepHours, waterIntake,
        energyLevel, stressLevel, basalBodyTemp, notes
    ) {
        var c = 0
        if (selectedFlow != null || flowIsNone) c++
        if (spotting) c++
        if (painLevel > 0f) c++
        if (selectedMood != null) c++
        c += selectedSymptoms.size + selectedCustomSymptoms.size
        if (selectedMucus != null) c++
        if (hadIntercourse || intercourseAnsweredNo) c++
        if (sleepHours != null) c++
        if (waterIntake != null) c++
        if (energyLevel != null) c++
        if (stressLevel != null) c++
        if (basalBodyTemp.isNotBlank()) c++
        if (notes.isNotBlank()) c++
        c
    }

    // ── Navegación por pestañas ──────────────────────────────────────────────
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val tabs = listOf(
        TabInfo(stringResource(R.string.log_tab_today), Icons.Rounded.Favorite),
        TabInfo(stringResource(R.string.log_tab_symptoms), Icons.Rounded.AutoAwesome),
        TabInfo(stringResource(R.string.log_tab_wellbeing), Icons.Rounded.Nightlight),
        TabInfo(stringResource(R.string.log_tab_notes), Icons.Rounded.Edit)
    )
    val stepLabels = listOf(
        stringResource(R.string.log_step_essential),
        stringResource(R.string.log_step_symptoms),
        stringResource(R.string.log_step_wellbeing),
        stringResource(R.string.log_step_notes)
    )

    // Estados de modales
    var showReviewModal by remember { mutableStateOf(false) }
    var showCustomSymptomDialog by remember { mutableStateOf(false) }
    var showMucusInfoDialog by remember { mutableStateOf(false) }
    var adjustingIntensityFor by remember { mutableStateOf<String?>(null) }

    // Búsqueda y filtros de síntomas
    var symptomQuery by remember { mutableStateOf("") }
    var activeCategoryFilter by remember { mutableStateOf("Todos") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.background,
        modifier         = Modifier.fillMaxHeight(),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // ── HEADER SUPERIOR ELEGANTE ─────────────────────────────────────
            DailyLogHeader(
                day = day,
                month = month,
                year = year,
                currentStep = pagerState.currentPage,
                stepLabel = stepLabels[pagerState.currentPage],
                onClose = onDismiss
            )

            // ── BARRA DE PESTAÑAS NAVEGABLES ─────────────────────────────────
            DailyLogTabBar(
                tabs = tabs,
                selectedTab = pagerState.currentPage,
                onTabSelect = { index ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp
            )

            // ── CONTENIDO PRINCIPAL: PAGER HORIZONTAL SUAVE ──────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> TabHoyContent(
                        activeCategories = activeCategories,
                        selectedFlow = selectedFlow,
                        flowIsNone = flowIsNone,
                        onFlowChange = { flowRes, isNone ->
                            selectedFlow = flowRes
                            flowIsNone = isNone
                        },
                        spotting = spotting,
                        onSpottingChange = { spotting = it },
                        moodOptions = moodOptions,
                        selectedMood = selectedMood,
                        onMoodSelect = { selectedMood = if (selectedMood == it) null else it },
                        painLevel = painLevel,
                        onPainChange = { painLevel = it },
                        quickSymptoms = quickSymptoms,
                        selectedSymptoms = selectedSymptoms,
                        onToggleQuickSymptom = { resId ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedSymptoms = if (selectedSymptoms.contains(resId)) {
                                selectedSymptoms - resId
                            } else {
                                selectedSymptoms + resId
                            }
                        },
                        onNavigateToAllSymptoms = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        hadIntercourse = hadIntercourse,
                        intercourseAnsweredNo = intercourseAnsweredNo,
                        onHadIntercourseChange = { checked ->
                            hadIntercourse = checked
                            if (checked) {
                                intercourseAnsweredNo = false
                            } else {
                                protectionUsed = null
                                selectedMethod = null
                                intercourseNotes = ""
                            }
                        },
                        onIntercourseAnswerNo = {
                            intercourseAnsweredNo = true
                            hadIntercourse = false
                            protectionUsed = null
                            selectedMethod = null
                            intercourseNotes = ""
                        },
                        onClearIntercourse = {
                            intercourseAnsweredNo = false
                            hadIntercourse = false
                            protectionUsed = null
                            selectedMethod = null
                            intercourseNotes = ""
                        },
                        protectionUsed = protectionUsed,
                        onProtectionChange = { protectionUsed = it },
                        methodOptions = methodOptions,
                        selectedMethod = selectedMethod,
                        onMethodSelect = { selectedMethod = it },
                        intercourseNotes = intercourseNotes,
                        onIntercourseNotesChange = { intercourseNotes = it },
                        showOnCalendar = showOnCalendar,
                        onShowOnCalendarChange = { showOnCalendar = it }
                    )

                    1 -> TabSintomasContent(
                        query = symptomQuery,
                        onQueryChange = { symptomQuery = it },
                        activeFilter = activeCategoryFilter,
                        onFilterChange = { activeCategoryFilter = it },
                        catalog = catalogSymptoms,
                        customSymptoms = customSymptoms,
                        selectedSymptoms = selectedSymptoms,
                        selectedCustomSymptoms = selectedCustomSymptoms,
                        symptomIntensities = symptomIntensities,
                        onToggleCatalogSymptom = { item ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (item.resId != null) {
                                selectedSymptoms = if (selectedSymptoms.contains(item.resId)) {
                                    selectedSymptoms - item.resId
                                } else {
                                    selectedSymptoms + item.resId
                                }
                            } else {
                                selectedCustomSymptoms = if (selectedCustomSymptoms.contains(item.name)) {
                                    selectedCustomSymptoms - item.name
                                } else {
                                    selectedCustomSymptoms + item.name
                                }
                            }
                        },
                        onAddCustomClick = { showCustomSymptomDialog = true },
                        onAdjustIntensity = { symptomName -> adjustingIntensityFor = symptomName }
                    )

                    2 -> TabBienestarContent(
                        activeCategories = activeCategories,
                        sleepHours = sleepHours,
                        onSleepChange = { sleepHours = it },
                        energyLevel = energyLevel,
                        onEnergyChange = { energyLevel = it },
                        stressLevel = stressLevel,
                        onStressChange = { stressLevel = it },
                        waterIntake = waterIntake,
                        onWaterChange = { waterIntake = it },
                        basalBodyTemp = basalBodyTemp,
                        onBasalBodyTempChange = { basalBodyTemp = it },
                        mucusOptions = mucusOptions,
                        selectedMucus = selectedMucus,
                        onMucusSelect = { selectedMucus = if (selectedMucus == it) null else it },
                        onMucusInfoClick = { showMucusInfoDialog = true }
                    )

                    3 -> TabNotasContent(
                        notes = notes,
                        onNotesChange = { notes = it },
                        activeCategories = activeCategories,
                        onActiveCategoriesChange = onActiveCategoriesChange
                    )
                }
            }

            // ── STICKY BOTTOM BAR CON CONTADOR Y BOTÓN GUARDAR ───────────────
            DailyLogStickyFooter(
                loggedCount = loggedCount,
                isEditing = isEditing,
                isSaving = isSaving,
                onReviewClick = { showReviewModal = true },
                onSaveClick = {
                    val finalFlow = when {
                        flowIsNone -> "Ninguno"
                        selectedFlow != null -> ClinicalLabels.getCanonicalFlow(selectedFlow!!)
                            ?: flowStrings.getOrNull(flowOptions.indexOf(selectedFlow!!))
                        else -> null
                    }
                    val finalMood = selectedMood?.let {
                        ClinicalLabels.getCanonicalMood(it) ?: moodStrings.getOrNull(moodOptions.indexOf(it))
                    }
                    val catalogResMap = catalogSymptoms.filter { it.resId != null }.associate { it.resId!! to it.name }
                    val finalSelectedSymptoms = (
                        selectedSymptoms.mapNotNull { resId ->
                            ClinicalLabels.getCanonicalSymptom(resId) ?: catalogResMap[resId]
                        } + selectedCustomSymptoms + unrecognizedSymptoms.map { it.name }
                    ).distinct()

                    val finalMucus = selectedMucus?.let {
                        ClinicalLabels.getCanonicalMucus(it) ?: mucusStrings.getOrNull(mucusOptions.indexOf(it))
                    }
                    val finalMethod = selectedMethod?.let {
                        ClinicalLabels.getCanonicalMethod(it) ?: methodStrings.getOrNull(methodOptions.indexOf(it))
                    }
                    val bbtParsed = basalBodyTemp.replace(",", ".").toFloatOrNull()

                    onSave(
                        finalFlow,
                        painLevel.toInt(),
                        finalMood,
                        finalSelectedSymptoms,
                        finalMucus,
                        notes,
                        hadIntercourse,
                        if (hadIntercourse) protectionUsed else null,
                        if (hadIntercourse) finalMethod else null,
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
        }
    }

    // ── MODAL: RESUMEN ANTES DE GUARDAR ──────────────────────────────────────
    if (showReviewModal) {
        ReviewSummarySheet(
            flow = when {
                flowIsNone -> "Ninguno"
                selectedFlow != null -> flowStrings.getOrNull(flowOptions.indexOf(selectedFlow!!))
                else -> null
            },
            spotting = spotting,
            pain = painLevel.toInt(),
            mood = selectedMood?.let { moodStrings.getOrNull(moodOptions.indexOf(it)) },
            symptoms = (
                selectedSymptoms.mapNotNull { resId ->
                    ClinicalLabels.getCanonicalSymptom(resId)
                        ?: catalogSymptoms.firstOrNull { it.resId == resId }?.name
                } + selectedCustomSymptoms
            ),
            hadIntercourse = hadIntercourse,
            protectionUsed = protectionUsed,
            method = selectedMethod?.let { methodStrings.getOrNull(methodOptions.indexOf(it)) },
            sleepHours = sleepHours,
            energyLevel = energyLevel,
            stressLevel = stressLevel,
            waterIntake = waterIntake,
            bbt = basalBodyTemp,
            mucus = selectedMucus?.let { mucusStrings.getOrNull(mucusOptions.indexOf(it)) },
            notes = notes,
            onDismiss = { showReviewModal = false }
        )
    }

    // ── DIÁLOGO: AGREGAR SÍNTOMA PROPIO ──────────────────────────────────────
    if (showCustomSymptomDialog) {
        AddCustomSymptomDialog(
            onDismiss = { showCustomSymptomDialog = false },
            onAdd = { customName ->
                if (customName.isNotBlank()) {
                    customSymptoms = (customSymptoms + customName.trim()).distinct()
                    selectedCustomSymptoms = selectedCustomSymptoms + customName.trim()
                    symptomIntensities[customName.trim()] = 2
                }
                showCustomSymptomDialog = false
            }
        )
    }

    // ── DIÁLOGO: AJUSTAR INTENSIDAD DE SÍNTOMA ──────────────────────────────
    adjustingIntensityFor?.let { symptomName ->
        SymptomIntensityDialog(
            symptomName = symptomName,
            currentIntensity = symptomIntensities[symptomName] ?: 2,
            onSelectIntensity = { intensity ->
                symptomIntensities[symptomName] = intensity
                adjustingIntensityFor = null
            },
            onDismiss = { adjustingIntensityFor = null }
        )
    }

    // ── DIÁLOGO: EXPLICACIÓN CLÍNICA DE MOCO CERVICAL ────────────────────────
    if (showMucusInfoDialog) {
        AlertDialog(
            onDismissRequest = { showMucusInfoDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.log_cervical_mucus),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.log_mucus_info_desc),
                    lineHeight = 20.sp,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showMucusInfoDialog = false }) {
                    Text(stringResource(R.string.btn_understood))
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HEADER PRINCIPAL CON PROGRESS BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DailyLogHeader(
    day: Int,
    month: Int,
    year: Int,
    currentStep: Int,
    stepLabel: String,
    onClose: () -> Unit
) {
    val tokens = LumiThemeTokens.current
    val monthName = remember(month, year, day) {
        val pattern = if (Locale.getDefault().language == "en") "MMMM d" else "d 'de' MMMM"
        LocalDate.of(year, month, day)
            .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(tokens.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.toString(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.accent
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.log_title),
                        fontSize = 19.sp,
                        fontFamily = LumiSans,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp,
                        color = tokens.text
                    )
                    Text(
                        text = monthName,
                        fontSize = 14.sp,
                        fontFamily = LumiEditorialSerif,
                        fontStyle = FontStyle.Italic,
                        color = tokens.accent
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tokens.panel)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.btn_close),
                    modifier = Modifier.size(20.dp),
                    tint = tokens.muted
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BARRA DE PESTAÑAS NAVEGABLES
// ─────────────────────────────────────────────────────────────────────────────

private data class TabInfo(val title: String, val icon: ImageVector)

@Composable
private fun DailyLogTabBar(
    tabs: List<TabInfo>,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedTab == index
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent,
                label = "tab_bg_$index"
            )
            val fg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                label = "tab_fg_$index"
            )
            val borderModifier = if (isSelected) {
                Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            } else Modifier

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(borderModifier)
                    .background(bg)
                    .clickable { onTabSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = fg
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = tab.title,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = fg,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PESTAÑA 0: HOY (LO ESENCIAL — FLUJO EN 5 SEGUNDOS)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TabHoyContent(
    activeCategories: Set<String>,
    selectedFlow: Int?,
    flowIsNone: Boolean,
    onFlowChange: (resId: Int?, isNone: Boolean) -> Unit,
    spotting: Boolean,
    onSpottingChange: (Boolean) -> Unit,
    moodOptions: List<Int>,
    selectedMood: Int?,
    onMoodSelect: (Int) -> Unit,
    painLevel: Float,
    onPainChange: (Float) -> Unit,
    quickSymptoms: List<Int>,
    selectedSymptoms: Set<Int>,
    onToggleQuickSymptom: (Int) -> Unit,
    onNavigateToAllSymptoms: () -> Unit,
    hadIntercourse: Boolean,
    intercourseAnsweredNo: Boolean,
    onHadIntercourseChange: (Boolean) -> Unit,
    onIntercourseAnswerNo: () -> Unit,
    onClearIntercourse: () -> Unit,
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. SANGRADO Y PERIODO
        CardSection(
            title = stringResource(R.string.log_period_status_question)
        ) {
            FlowSelectorWithNone(
                options = listOf(R.string.flow_light, R.string.flow_medium, R.string.flow_heavy),
                selected = selectedFlow,
                isNone = flowIsNone,
                onSelect = { optionRes ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (optionRes == null) {
                        onFlowChange(null, true)
                    } else {
                        onFlowChange(optionRes, false)
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            // Manchado fuera del periodo (Spotting)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpottingChange(!spotting)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.log_spotting),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = spotting,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpottingChange(it)
                    }
                )
            }
        }

        // 2. ¿CÓMO TE ENCUENTRAS? (ESTADO DE ÁNIMO)
        CardSection(
            title = stringResource(R.string.log_how_feeling_question)
        ) {
            MoodGridSelector(
                options = moodOptions,
                selected = selectedMood,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMoodSelect(it)
                }
            )
        }

        // 3. NIVEL DE DOLOR
        if ("physical" in activeCategories) {
            CardSection(
                title = stringResource(R.string.log_pain_question),
                subtitle = stringResource(R.string.log_pain_intensity_range),
                actionText = if (painLevel > 0f) stringResource(R.string.log_no_pain) else null,
                onActionClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPainChange(0f)
                }
            ) {
                PainSliderCard(
                    painLevel = painLevel,
                    onPainChange = onPainChange
                )
            }
        }

        // 4. SÍNTOMAS HABITUALES A UN TOQUE
        CardSection(
            title = stringResource(R.string.log_frequent_symptoms),
            actionText = stringResource(R.string.log_view_all_arrow),
            onActionClick = onNavigateToAllSymptoms
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val half1 = quickSymptoms.take(3)
                val half2 = quickSymptoms.drop(3)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    half1.forEach { resId ->
                        QuickSymptomPill(
                            resId = resId,
                            isSelected = selectedSymptoms.contains(resId),
                            onClick = { onToggleQuickSymptom(resId) }
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    half2.forEach { resId ->
                        QuickSymptomPill(
                            resId = resId,
                            isSelected = selectedSymptoms.contains(resId),
                            onClick = { onToggleQuickSymptom(resId) }
                        )
                    }
                }
            }
        }

        // 5. REGISTRO ÍNTIMO CON EL BOTÓN CORAZÓN ORIGINAL DE LUMI
        if ("intercourse" in activeCategories) {
            IntimacyInteractiveCard(
                hadIntercourse = hadIntercourse,
                answeredNo = intercourseAnsweredNo,
                onHadIntercourseChange = onHadIntercourseChange,
                onAnswerNo = onIntercourseAnswerNo,
                onClear = onClearIntercourse,
                protectionUsed = protectionUsed,
                onProtectionChange = onProtectionChange,
                methodOptions = methodOptions,
                selectedMethod = selectedMethod,
                onMethodSelect = onMethodSelect,
                intercourseNotes = intercourseNotes,
                onIntercourseNotesChange = onIntercourseNotesChange,
                showOnCalendar = showOnCalendar,
                onShowOnCalendarChange = onShowOnCalendarChange
            )
        }

        Spacer(Modifier.height(10.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SELECTOR DE FLUJO CON GOTAS Y OPCIÓN "NINGUNO"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FlowSelectorWithNone(
    options: List<Int>,
    selected: Int?,
    isNone: Boolean,
    onSelect: (Int?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Opción: "Ninguno"
        val isNoneActive = isNone && selected == null
        val noneBg by animateColorAsState(
            if (isNoneActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant,
            label = "flow_none_bg"
        )
        val noneFg by animateColorAsState(
            if (isNoneActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            label = "flow_none_fg"
        )
        val noneBorder = if (isNoneActive) {
            Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
        } else Modifier

        Box(
            modifier = Modifier
                .weight(1f)
                .height(76.dp)
                .clip(RoundedCornerShape(16.dp))
                .then(noneBorder)
                .background(noneBg)
                .clickable { onSelect(null) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = noneFg
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Ninguno",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = noneFg
                )
            }
        }

        // Opciones: Ligero (1 gota), Medio (2 gotas), Abundante (3 gotas)
        options.forEachIndexed { index, optionRes ->
            val isSelected = selected == optionRes && !isNone
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "flow_bg_$index"
            )
            val fg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                label = "flow_fg_$index"
            )
            val border = if (isSelected) {
                Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            } else Modifier

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .then(border)
                    .background(bg)
                    .clickable { onSelect(optionRes) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(index + 1) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = fg
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = optionRes),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = fg
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SELECTOR DE ÁNIMO EN GRID MODERNO
// ─────────────────────────────────────────────────────────────────────────────

private val moodIconList = listOf(
    Icons.Rounded.SentimentSatisfied,
    Icons.Rounded.Spa,
    Icons.Rounded.Bolt,
    Icons.Rounded.Favorite,
    Icons.Rounded.Cloud,
    Icons.Rounded.Waves,
    Icons.Rounded.LocalFireDepartment,
    Icons.Rounded.Psychology
)

@Composable
private fun MoodGridSelector(
    options: List<Int>,
    selected: Int?,
    onSelect: (Int) -> Unit
) {
    val chunked = options.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunked.forEachIndexed { rowIndex, rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowOptions.forEachIndexed { colIndex, optionRes ->
                    val globalIndex = rowIndex * 3 + colIndex
                    val isSelected = selected == optionRes
                    val bg by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        label = "mood_bg_$globalIndex"
                    )
                    val fg by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        label = "mood_fg_$globalIndex"
                    )
                    val border = if (isSelected) {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    } else Modifier

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .then(border)
                            .background(bg)
                            .clickable { onSelect(optionRes) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = moodIconList.getOrElse(globalIndex) { Icons.Rounded.SentimentSatisfied },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = fg
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = optionRes),
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = fg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // Si la última fila tiene menos de 3 elementos, agregamos spacers para que no se estiren
                if (rowOptions.size < 3) {
                    repeat(3 - rowOptions.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CARD DE DOLOR CON SLIDER Y AVISO CLÍNICO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PainSliderCard(
    painLevel: Float,
    onPainChange: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val neutral = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    val safe    = MaterialTheme.colorScheme.primary
    val warning = MaterialTheme.colorScheme.tertiary
    val danger  = MaterialTheme.colorScheme.error

    val rawColor = if (painLevel == 0f) neutral else {
        val t = (painLevel - 1f) / 9f
        if (t < 0.5f) lerp(safe, warning, t * 2f)
        else lerp(warning, danger, (t - 0.5f) * 2f)
    }
    val animColor by animateColorAsState(rawColor, tween(250), label = "pain_color")

    val painText = when (painLevel.toInt()) {
        0 -> stringResource(R.string.log_no_pain)
        in 1..3 -> stringResource(R.string.log_pain_mild)
        in 4..6 -> stringResource(R.string.log_pain_moderate)
        in 7..8 -> stringResource(R.string.log_pain_severe)
        else -> stringResource(R.string.log_pain_very_severe)
    }

    val painOptions = listOf(
        Triple(0f, "0", "Nada"),
        Triple(2f, "2", "Leve"),
        Triple(5f, "5", "Medio"),
        Triple(7f, "7", "Fuerte"),
        Triple(9f, "9", "Severo")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = painText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = animColor
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = painLevel.toInt().toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = animColor
                )
                Text(
                    text = " / 10",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            painOptions.forEach { (valFloat, score, label) ->
                val isSelected = (valFloat == 0f && painLevel == 0f) ||
                    (valFloat == 2f && painLevel in 1f..3f) ||
                    (valFloat == 5f && painLevel in 4f..6f) ||
                    (valFloat == 7f && painLevel in 7f..8f) ||
                    (valFloat == 9f && painLevel >= 9f) ||
                    (painLevel == valFloat)

                val btnBg by animateColorAsState(
                    if (isSelected) animColor.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    label = "pain_btn_bg_$score"
                )
                val borderModifier = if (isSelected) {
                    Modifier.border(1.5.dp, animColor, RoundedCornerShape(12.dp))
                } else Modifier

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(borderModifier)
                        .background(btnBg)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPainChange(valFloat)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = score,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) animColor else MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = label,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) animColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Espacio fijo reservado para el aviso clínico cuando dolor >= 7 para no empujar el layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (painLevel >= 7f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.log_pain_warning),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.error,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PILL DE SÍNTOMA RÁPIDO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickSymptomPill(
    resId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "quick_bg"
    )
    val fg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        label = "quick_fg"
    )
    val border = if (isSelected) {
        Modifier.border(1.2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(border)
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = resId),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = fg
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TARJETA DE INTIMIDAD CON EL BOTÓN CORAZÓN ORIGINAL DE LUMI
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IntimacyInteractiveCard(
    hadIntercourse: Boolean,
    answeredNo: Boolean,
    onHadIntercourseChange: (Boolean) -> Unit,
    onAnswerNo: () -> Unit,
    onClear: () -> Unit,
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(
                1.dp,
                if (hadIntercourse) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        // Cabecera con emblema corazón
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8A0C0).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFED6190)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.log_intercourse),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.log_intimacy_title),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            // Estado pequeño a la derecha
            val statusText = when {
                hadIntercourse -> stringResource(R.string.log_intimacy_yes)
                answeredNo -> stringResource(R.string.log_intimacy_no)
                else -> stringResource(R.string.log_intimacy_none)
            }
            Text(
                text = statusText,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (hadIntercourse) Color(0xFFED6190) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.log_had_intimacy_question),
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isNone = !hadIntercourse && !answeredNo
            // 1. Sin registrar
            val noneBg by animateColorAsState(
                if (isNone) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "none_btn_bg"
            )
            val noneBorder = if (isNone) {
                Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            } else Modifier

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(noneBorder)
                    .background(noneBg)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClear()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.onboarding_summary_not_logged),
                    fontSize = 11.5.sp,
                    fontWeight = if (isNone) FontWeight.Bold else FontWeight.Medium,
                    color = if (isNone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // 2. No
            val noBg by animateColorAsState(
                if (answeredNo) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "no_btn_bg"
            )
            val noBorder = if (answeredNo) {
                Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            } else Modifier

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(noBorder)
                    .background(noBg)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAnswerNo()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.common_no),
                    fontSize = 12.sp,
                    fontWeight = if (answeredNo) FontWeight.Bold else FontWeight.Medium,
                    color = if (answeredNo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // 3. ❤️ Sí
            val yesBg by animateColorAsState(
                if (hadIntercourse) Color(0xFFED6190).copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "yes_btn_bg"
            )
            val yesBorder = if (hadIntercourse) {
                Modifier.border(1.dp, Color(0xFFED6190), RoundedCornerShape(12.dp))
            } else Modifier

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(yesBorder)
                    .background(yesBg)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onHadIntercourseChange(true)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.log_had_intimacy_yes),
                    fontSize = 12.sp,
                    fontWeight = if (hadIntercourse) FontWeight.Bold else FontWeight.Medium,
                    color = if (hadIntercourse) Color(0xFFED6190) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Sección expandida si hadIntercourse == true
        AnimatedVisibility(
            visible = hadIntercourse,
            enter = expandVertically(tween(250)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                // ¿Se usó protección?
                Column {
                    Text(
                        text = stringResource(R.string.log_protection),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillChip(
                            text = stringResource(R.string.protection_yes),
                            isSelected = protectionUsed == true,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onProtectionChange(if (protectionUsed == true) null else true)
                            }
                        )
                        PillChip(
                            text = stringResource(R.string.protection_no),
                            isSelected = protectionUsed == false,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onProtectionChange(if (protectionUsed == false) null else false)
                            }
                        )
                    }
                }

                // Método anticonceptivo
                AnimatedVisibility(visible = protectionUsed == true) {
                    Column {
                        Text(
                            text = stringResource(R.string.log_method),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                        )
                        Spacer(Modifier.height(8.dp))
                        val filtered = methodOptions.filter { it != R.string.method_none }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filtered) { methodRes ->
                                PillChip(
                                    text = stringResource(id = methodRes),
                                    isSelected = selectedMethod == methodRes,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onMethodSelect(if (selectedMethod == methodRes) null else methodRes)
                                    }
                                )
                            }
                        }
                    }
                }

                // Notas íntimas opcionales
                OutlinedTextField(
                    value = intercourseNotes,
                    onValueChange = onIntercourseNotesChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    placeholder = {
                        Text(
                            stringResource(R.string.log_intercourse_notes),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp)
                )

                // Mostrar en el calendario
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.log_show_on_calendar),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Switch(
                        checked = showOnCalendar,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShowOnCalendarChange(it)
                        }
                    )
                }
            }
        }

        // Botón para quitar respuesta si ya se seleccionó Sí o No
        if (hadIntercourse || answeredNo) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.log_clear_answer),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onClear() }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PESTAÑA 1: CATÁLOGO COMPLETO DE SÍNTOMAS CON BÚSQUEDA Y CATEGORÍAS
// ─────────────────────────────────────────────────────────────────────────────

private fun symptomCategoryRes(cat: String): Int = when (cat) {
    "Todos" -> R.string.log_cat_all
    "Dolor" -> R.string.log_cat_pain
    "Digestivo" -> R.string.log_cat_digestive_filter
    "Cuerpo" -> R.string.log_cat_body
    "Piel" -> R.string.log_cat_skin
    "Urinario" -> R.string.log_cat_urinary
    "Íntimo" -> R.string.log_cat_intimate
    "Sueño y mente" -> R.string.log_cat_sleep_mind
    "Clínico" -> R.string.log_cat_clinical
    "Propios" -> R.string.log_cat_custom
    else -> R.string.log_cat_all
}

@Composable
private fun TabSintomasContent(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilter: String,
    onFilterChange: (String) -> Unit,
    catalog: List<SymptomCatalogItem>,
    customSymptoms: List<String>,
    selectedSymptoms: Set<Int>,
    selectedCustomSymptoms: Set<String>,
    symptomIntensities: Map<String, Int>,
    onToggleCatalogSymptom: (SymptomCatalogItem) -> Unit,
    onAddCustomClick: () -> Unit,
    onAdjustIntensity: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val categories = listOf("Todos", "Dolor", "Digestivo", "Cuerpo", "Piel", "Urinario", "Íntimo", "Sueño y mente", "Clínico", "Propios")

    val fullList = remember(catalog, customSymptoms) {
        catalog + customSymptoms.map { SymptomCatalogItem(it, null, "Propios") }
    }

    val filteredList = remember(fullList, query, activeFilter) {
        val q = query.trim().lowercase(Locale.getDefault())
        fullList.filter { item ->
            val matchesCategory = (activeFilter == "Todos") || (item.category == activeFilter)
            val matchesQuery = q.isEmpty() || item.name.lowercase(Locale.getDefault()).contains(q)
            matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Buscador
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.log_search_placeholder), fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        // Filtro horizontal por categoría
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories) { cat ->
                val isActive = activeFilter == cat
                val bg by animateColorAsState(
                    if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    label = "filter_bg"
                )
                val fg by animateColorAsState(
                    if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    label = "filter_fg"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(bg)
                        .clickable { onFilterChange(cat) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(symptomCategoryRes(cat)), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = fg)
                }
            }
        }

        // Grid de 2 columnas de síntomas
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.log_symptom_not_found),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            val chunked = filteredList.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chunked.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            val isSelected = if (item.resId != null) {
                                selectedSymptoms.contains(item.resId)
                            } else {
                                selectedCustomSymptoms.contains(item.name)
                            }

                            val bg by animateColorAsState(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                label = "symp_item_bg"
                            )
                            val fg by animateColorAsState(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                label = "symp_item_fg"
                            )
                            val border = if (isSelected) {
                                Modifier.border(1.2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                            } else Modifier

                            val displayName = if (item.resId != null) stringResource(item.resId) else item.name

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .then(border)
                                    .background(bg)
                                    .clickable { onToggleCatalogSymptom(item) }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = fg,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else fg
                                    )
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Botón: Añadir síntoma propio
        OutlinedButton(
            onClick = onAddCustomClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.log_add_custom_symptom), fontSize = 12.5.sp)
        }

        // Sección de síntomas seleccionados con botón de ajuste de intensidad
        val totalSelectedNames = remember(selectedSymptoms, selectedCustomSymptoms, catalog) {
            val fromRes = selectedSymptoms.mapNotNull { resId ->
                ClinicalLabels.getCanonicalSymptom(resId)
                    ?: catalog.firstOrNull { it.resId == resId }?.name
            }
            (fromRes + selectedCustomSymptoms).distinct()
        }

        if (totalSelectedNames.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            CardSection(
                title = stringResource(R.string.log_selected_symptoms_count, totalSelectedNames.size),
                subtitle = stringResource(R.string.log_adjust_intensity_desc)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    totalSelectedNames.forEach { sName ->
                        val intensity = symptomIntensities[sName] ?: 2
                        val intensityLabel = when (intensity) {
                            1 -> stringResource(R.string.log_intensity_mild)
                            2 -> stringResource(R.string.log_intensity_moderate)
                            else -> stringResource(R.string.log_intensity_severe)
                        }
                        val sDisplayName = catalog.firstOrNull { it.name == sName }?.resId?.let { stringResource(it) } ?: sName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✓ $sDisplayName",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = { onAdjustIntensity(sName) }) {
                                Text(
                                    text = "$intensityLabel ›",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PESTAÑA 2: BIENESTAR (SUEÑO, ENERGÍA, ESTRÉS, AGUA, TEMPERATURA Y MOCO)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TabBienestarContent(
    activeCategories: Set<String>,
    sleepHours: Float?,
    onSleepChange: (Float?) -> Unit,
    energyLevel: Int?,
    onEnergyChange: (Int?) -> Unit,
    stressLevel: Int?,
    onStressChange: (Int?) -> Unit,
    waterIntake: Float?,
    onWaterChange: (Float?) -> Unit,
    basalBodyTemp: String,
    onBasalBodyTempChange: (String) -> Unit,
    mucusOptions: List<Int>,
    selectedMucus: Int?,
    onMucusSelect: (Int) -> Unit,
    onMucusInfoClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if ("wellbeing" in activeCategories) {
            // Horas de sueño con Stepper
            HabitStepperRow(
                title = stringResource(R.string.log_sleep_hours_title),
                subtitle = stringResource(R.string.log_sleep_hours_desc),
                displayValue = sleepHours?.let { String.format(Locale.US, "%.1f h", it) } ?: "0 h",
                onDecrement = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val current = sleepHours ?: 7.0f
                    val next = (current - 0.5f).coerceAtLeast(0f)
                    onSleepChange(if (next == 0f) null else next)
                },
                onIncrement = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val current = sleepHours ?: 7.0f
                    val next = (current + 0.5f).coerceAtMost(24f)
                    onSleepChange(next)
                }
            )

            // Nivel de energía (1 a 5)
            SegmentMeterRow(
                title = stringResource(R.string.log_energy_title),
                subtitle = stringResource(R.string.log_energy_desc),
                currentValue = energyLevel,
                onValueSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEnergyChange(if (energyLevel == it) null else it)
                }
            )

            // Nivel de estrés (1 a 5)
            SegmentMeterRow(
                title = stringResource(R.string.log_stress_title),
                subtitle = stringResource(R.string.log_stress_desc),
                currentValue = stressLevel,
                onValueSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStressChange(if (stressLevel == it) null else it)
                }
            )

            // Agua que bebiste con Stepper
            HabitStepperRow(
                title = stringResource(R.string.log_water_title),
                subtitle = stringResource(R.string.log_water_desc),
                displayValue = waterIntake?.let { String.format(Locale.US, "%.2f L", it) } ?: "0 L",
                onDecrement = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val current = waterIntake ?: 1.5f
                    val next = (current - 0.25f).coerceAtLeast(0f)
                    onWaterChange(if (next == 0f) null else next)
                },
                onIncrement = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val current = waterIntake ?: 1.5f
                    val next = (current + 0.25f).coerceAtMost(10f)
                    onWaterChange(next)
                }
            )
        }

        // Señales corporales
        if ("vitals" in activeCategories || "mucus" in activeCategories) {
            if ("wellbeing" in activeCategories) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
            }

            Text(
                text = stringResource(R.string.log_body_signals),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Temperatura basal (°C)
            if ("vitals" in activeCategories) {
                Column {
                    Text(
                        text = stringResource(R.string.log_bbt_celsius),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = basalBodyTemp,
                        onValueChange = onBasalBodyTempChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.log_bbt_placeholder), fontSize = 12.5.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    Text(
                        text = stringResource(R.string.log_bbt_disclaimer),
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            // Moco cervical
            if ("mucus" in activeCategories) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.log_cervical_mucus),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = onMucusInfoClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = stringResource(R.string.cd_info),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(mucusOptions) { optionRes ->
                            PillChip(
                                text = stringResource(id = optionRes),
                                isSelected = selectedMucus == optionRes,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onMucusSelect(optionRes)
                                }
                            )
                        }
                    }
                }
            }
        }

        if ("wellbeing" !in activeCategories && "vitals" !in activeCategories && "mucus" !in activeCategories) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.log_customize_desc),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PESTAÑA 3: NOTAS Y PREFERENCIAS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TabNotasContent(
    notes: String,
    onNotesChange: (String) -> Unit,
    activeCategories: Set<String>,
    onActiveCategoriesChange: (Set<String>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CardSection(
            title = stringResource(R.string.log_private_note_title),
            subtitle = stringResource(R.string.log_private_note_desc)
        ) {
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.log_notes_hint),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
            Text(
                text = stringResource(R.string.log_char_count, notes.length, 1000),
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            thickness = 1.dp
        )

        CardSection(
            title = stringResource(R.string.log_customize_title),
            subtitle = stringResource(R.string.log_customize_desc)
        ) {
            val categories = listOf(
                "physical" to stringResource(R.string.category_physical),
                "digestive" to stringResource(R.string.category_digestive),
                "mucus" to stringResource(R.string.category_mucus),
                "intercourse" to stringResource(R.string.category_intercourse),
                "wellbeing" to stringResource(R.string.category_wellbeing),
                "vitals" to stringResource(R.string.category_vitals)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { (key, title) ->
                    val isChecked = activeCategories.contains(key)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable {
                                val newCats = activeCategories.toMutableSet()
                                if (isChecked) newCats.remove(key) else newCats.add(key)
                                onActiveCategoriesChange(newCats)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }

        // Tarjeta informativa de privacidad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.log_privacy_banner_title),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.log_privacy_banner_desc),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  STICKY FOOTER CON CONTADOR Y BOTÓN PRINCIPAL DE GUARDAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DailyLogStickyFooter(
    loggedCount: Int,
    isEditing: Boolean,
    isSaving: Boolean,
    onReviewClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val tokens = LumiThemeTokens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val markedText = when (loggedCount) {
                0 -> stringResource(R.string.log_nothing_marked)
                1 -> stringResource(R.string.log_items_marked_one)
                else -> stringResource(R.string.log_items_marked_other, loggedCount)
            }
            Text(
                text = markedText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            TextButton(
                onClick = onReviewClick,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.log_view_summary),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Botón principal de Guardar con degradado
        val gradient = Brush.horizontalGradient(
            listOf(
                tokens.rose,
                tokens.accent
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(gradient)
                .clickable(enabled = !isSaving) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSaveClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = if (tokens.isDark) tokens.bg else tokens.text
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = if (isEditing) stringResource(R.string.btn_update_log)
                           else stringResource(R.string.btn_save_log),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tokens.isDark) tokens.bg else tokens.text,
                    letterSpacing = 0.4.sp
                )
            }
        }

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  COMPONENTES REUTILIZABLES (STEPPER, METERS, SECTIONS)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardSection(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
            if (actionText != null && onActionClick != null) {
                Text(
                    text = actionText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onActionClick() }
                        .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun HabitStepperRow(
    title: String,
    subtitle: String,
    displayValue: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier.widthIn(min = 54.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayValue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SegmentMeterRow(
    title: String,
    subtitle: String,
    currentValue: Int?,
    onValueSelect: (Int) -> Unit
) {
    val tokens = LumiThemeTokens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            Text(
                text = currentValue?.let { stringResource(R.string.log_meter_of_five, it) } ?: stringResource(R.string.log_meter_unrecorded),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentValue != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..5).forEach { num ->
                val isActive = currentValue != null && num <= currentValue
                val bg by animateColorAsState(
                    if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    label = "meter_bg_$num"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, tokens.line, RoundedCornerShape(6.dp))
                        .background(bg)
                        .clickable { onValueSelect(num) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$num",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MODALES Y DIÁLOGOS (RESUMEN, INTENSIDAD, PERSONALIZADO)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSummarySheet(
    flow: String?,
    spotting: Boolean,
    pain: Int,
    mood: String?,
    symptoms: List<String>,
    hadIntercourse: Boolean,
    protectionUsed: Boolean?,
    method: String?,
    sleepHours: Float?,
    energyLevel: Int?,
    stressLevel: Int?,
    waterIntake: Float?,
    bbt: String,
    mucus: String?,
    notes: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.log_summary_title),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.log_summary_subtitle),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(16.dp))

            val items = buildList {
                if (flow != null) add(stringResource(R.string.log_flow) to flow)
                if (spotting) add(stringResource(R.string.log_spotting_intermenstrual) to stringResource(R.string.protection_yes))
                if (pain > 0) add(stringResource(R.string.log_cat_pain) to "$pain / 10")
                if (mood != null) add(stringResource(R.string.log_mood) to mood)
                if (symptoms.isNotEmpty()) add(stringResource(R.string.log_selected_symptoms_count, symptoms.size) to symptoms.joinToString(", "))
                if (hadIntercourse) {
                    val prot = when (protectionUsed) {
                        true -> if (method != null) stringResource(R.string.log_protected_with_method, method) else stringResource(R.string.log_protected_general)
                        false -> stringResource(R.string.log_unprotected)
                        null -> stringResource(R.string.log_unspecified)
                    }
                    add(stringResource(R.string.log_intimacy_title) to prot)
                }
                if (sleepHours != null) add(stringResource(R.string.log_sleep_hours) to String.format(Locale.US, "%.1f h", sleepHours))
                if (energyLevel != null) add(stringResource(R.string.log_energy_level) to "$energyLevel / 5")
                if (stressLevel != null) add(stringResource(R.string.log_stress_level) to "$stressLevel / 5")
                if (waterIntake != null) add(stringResource(R.string.log_water_liters) to String.format(Locale.US, "%.2f L", waterIntake))
                if (bbt.isNotBlank()) add(stringResource(R.string.log_bbt_label) to "$bbt °C")
                if (mucus != null) add(stringResource(R.string.log_cervical_mucus) to mucus)
                if (notes.isNotBlank()) add(stringResource(R.string.log_tab_notes) to notes)
            }

            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.log_empty_summary),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                items.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            value,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Listo", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AddCustomSymptomDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.log_custom_symptom_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.log_custom_symptom_desc), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.log_custom_symptom_placeholder)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.btn_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

@Composable
private fun SymptomIntensityDialog(
    symptomName: String,
    currentIntensity: Int,
    onSelectIntensity: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val levels = listOf(
        1 to stringResource(R.string.log_intensity_mild),
        2 to stringResource(R.string.log_intensity_moderate),
        3 to stringResource(R.string.log_intensity_severe)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.log_symptom_intensity_title, symptomName), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.log_symptom_intensity_subtitle), fontSize = 12.sp)
                levels.forEach { (level, label) ->
                    val isSelected = currentIntensity == level
                    Button(
                        onClick = { onSelectIntensity(level) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (isSelected) ButtonDefaults.buttonColors()
                                 else ButtonDefaults.filledTonalButtonColors(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_close)) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  PILL CHIP & LEGACY HELPERS (Para compatibilidad con el resto del proyecto)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PillChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val bg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant,
        tween(200), label = "chip_bg"
    )
    val fg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        tween(200), label = "chip_fg"
    )
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CleanChip(text: String, isSelected: Boolean, onClick: () -> Unit) =
    PillChip(text = text, isSelected = isSelected, onClick = onClick)

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
