package com.jeremy.lumi.ui.screens.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.data.preferences.DefaultLogFavorites
import com.jeremy.lumi.domain.util.ClinicalLabels
import com.jeremy.lumi.ui.theme.LumiEditorialSerif
import com.jeremy.lumi.ui.theme.LumiMotion
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class LogPage { DAY, CATALOG, CUSTOMIZE, FAVORITES }

// Existing category preferences are retained. Newly configurable essentials are visible by default.
internal fun sectionEnabled(key: String, categories: Set<String>) =
    if (key == "mucus" || key == "intercourse") key in categories else "hide_$key" !in categories

private fun changeSection(key: String, enabled: Boolean, categories: Set<String>): Set<String> =
    if (key == "mucus" || key == "intercourse") {
        if (enabled) categories + key else categories - key
    } else {
        if (enabled) categories - "hide_$key" else categories + "hide_$key"
    }

internal fun DailyLogDraft.hasSectionData(key: String) = when (key) {
    "period" -> flow != null || spotting
    "pain" -> pain > 0
    "wellbeing" -> sleep.isNotBlank() || water.isNotBlank() || energy != null || stress != null
    "mucus" -> mucus != null || temperature.isNotBlank()
    "intercourse" -> hadIntercourse || intimacyNotes.isNotBlank()
    "notes" -> notes.isNotBlank()
    else -> false
}

/** Preferences and the day's draft are separate: customizing never records symptoms for today. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLogSheet(
    day: Int, month: Int, year: Int,
    savedLog: DailyLogWithSymptoms?,
    activeCategories: Set<String>,
    onActiveCategoriesChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean = false,
    isLoading: Boolean = false,
    loadFailed: Boolean = false,
    saveFailed: Boolean = false,
    onRetryLoad: () -> Unit = {},
    customSymptoms: Set<String> = emptySet(),
    onCustomSymptomsChange: (Set<String>) -> Unit = {},
    favoriteSymptoms: Set<String> = DefaultLogFavorites,
    onFavoriteSymptomsChange: (Set<String>) -> Unit = {},
    onSave: (
        flow: String?, pain: Int, mood: String?, symptoms: List<String>, mucus: String?,
        notes: String, hadIntercourse: Boolean, protectionUsed: Boolean?,
        contraceptionMethod: String?, intercourseNotes: String?, showOnCalendar: Boolean,
        sleepHours: Float?, energyLevel: Int?, stressLevel: Int?, waterIntake: Float?,
        basalBodyTemp: Float?, spotting: Boolean, symptomIntensities: Map<String, Int>
    ) -> Unit
) {
    val tokens = LumiThemeTokens.current
    val contentDensity = LocalDensity.current
    // Keep the draft above the dialog's separate Android ComposeView/save-state owner.
    // This also lets it survive temporary loading UI and Activity/process recreation.
    var initialDraft by rememberSaveable(year, month, day) { mutableStateOf<DailyLogDraft?>(null) }
    var currentDraft by rememberSaveable(year, month, day) { mutableStateOf<DailyLogDraft?>(null) }
    LaunchedEffect(isLoading, loadFailed, savedLog, year, month, day) {
        if (!isLoading && !loadFailed && initialDraft == null) {
            initialDraft = DailyLogDraft.from(savedLog)
            currentDraft = initialDraft
        }
    }
    // A non-observable holder avoids a recomposition loop when the latest close lambda changes.
    val closeRequest = remember { arrayOfNulls<() -> Unit>(1) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden) {
                closeRequest[0]?.invoke()
                false
            } else true
        }
    )
    ModalBottomSheet(
        onDismissRequest = { closeRequest[0]?.invoke() ?: onDismiss() },
        sheetState = sheetState, containerColor = tokens.bg, contentColor = tokens.text,
        dragHandle = null, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        CompositionLocalProvider(LocalDensity provides contentDensity) {
        if (isLoading || loadFailed || currentDraft == null) {
            Column(
                Modifier.fillMaxWidth().fillMaxHeight(.94f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!loadFailed) CircularProgressIndicator(color = tokens.accent)
                Spacer(Modifier.height(20.dp))
                Text(stringResource(if (loadFailed) R.string.daily_load_error else R.string.daily_loading))
                if (loadFailed) TextButton(onClick = onRetryLoad) { Text(stringResource(R.string.daily_retry)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_close)) }
            }
            SideEffect { closeRequest[0] = onDismiss }
        } else {
            key(year, month, day) {
                DailyLogEditor(
                    day, month, year, savedLog, activeCategories, onActiveCategoriesChange,
                    customSymptoms, onCustomSymptomsChange, favoriteSymptoms, onFavoriteSymptomsChange,
                    isSaving, saveFailed, onDismiss, initialDraft!!, currentDraft!!,
                    onDraftChange = { currentDraft = it },
                    registerClose = { closeRequest[0] = it },
                    onSave = { draft ->
                        onSave(
                            draft.flow, draft.pain, draft.mood, draft.symptoms.keys.toList(), draft.mucus,
                            draft.notes, draft.hadIntercourse,
                            if (draft.hadIntercourse) draft.protection else null,
                            if (draft.hadIntercourse) draft.method else null,
                            if (draft.hadIntercourse) draft.intimacyNotes.takeIf(String::isNotBlank) else null,
                            draft.showIntimacy, parseLogNumber(draft.sleep), draft.energy, draft.stress,
                            parseLogNumber(draft.water), parseLogNumber(draft.temperature),
                            draft.spotting, draft.symptoms
                        )
                    }
                )
            }
        }
        }
    }
}

@Composable
private fun DailyLogEditor(
    day: Int, month: Int, year: Int, savedLog: DailyLogWithSymptoms?,
    activeCategories: Set<String>, onCategories: (Set<String>) -> Unit,
    customSymptoms: Set<String>, onCustomSymptoms: (Set<String>) -> Unit,
    favoriteSymptoms: Set<String>, onFavorites: (Set<String>) -> Unit,
    isSaving: Boolean, saveFailed: Boolean, onDismiss: () -> Unit,
    initial: DailyLogDraft, draft: DailyLogDraft, onDraftChange: (DailyLogDraft) -> Unit,
    registerClose: (() -> Unit) -> Unit, onSave: (DailyLogDraft) -> Unit
) {
    val tokens = LumiThemeTokens.current
    val haptic = LocalHapticFeedback.current
    val focus = LocalFocusManager.current
    var page by rememberSaveable { mutableStateOf(LogPage.DAY) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var addingCustom by rememberSaveable { mutableStateOf(false) }
    var intensityFor by rememberSaveable { mutableStateOf<String?>(null) }
    var categories by remember(activeCategories) { mutableStateOf(activeCategories) }
    var favorites by remember(favoriteSymptoms) { mutableStateOf(favoriteSymptoms) }
    var custom by remember(customSymptoms) { mutableStateOf(customSymptoms) }
    val dayScroll = rememberLazyListState()
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = remember(day, month, year, locale) {
        LocalDate.of(year, month, day).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    }
    val close: () -> Unit = {
        focus.clearFocus()
        when {
            isSaving -> Unit
            page == LogPage.FAVORITES -> page = LogPage.CUSTOMIZE
            page != LogPage.DAY -> page = LogPage.DAY
            draft != initial -> discard = true
            else -> onDismiss()
        }
    }
    SideEffect { registerClose(close) }
    BackHandler { close() }
    val update: (DailyLogDraft) -> Unit = { if (!isSaving) onDraftChange(it) }
    val toggle: (String) -> Unit = { update(draft.toggleSymptom(it)) }
    val setFavorites: (Set<String>) -> Unit = { favorites = it; onFavorites(it) }

    Column(Modifier.fillMaxWidth().fillMaxHeight(.96f).imePadding().testTag("daily_editor")) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (page != LogPage.DAY) IconButton(onClick = close) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.daily_back), tint = tokens.text)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (savedLog == null) R.string.daily_eyebrow else R.string.calendar_edit_record).uppercase(locale), color = readableLogAccent(), fontSize = 10.sp,
                    letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text(dateLabel.replaceFirstChar { it.titlecase(locale) }, fontSize = 13.sp, color = tokens.text)
            }
            if (page == LogPage.DAY) IconButton(onClick = close, enabled = !isSaving, modifier = Modifier.testTag("daily_close")) {
                Icon(Icons.Rounded.Close, stringResource(R.string.btn_close), tint = tokens.text)
            }
        }
        AnimatedContent(
            targetState = page, modifier = Modifier.weight(1f),
            transitionSpec = {
                (fadeIn(LumiMotion.ShortFade) + slideInHorizontally(LumiMotion.StepSlide) { it / 14 })
                    .togetherWith(fadeOut(LumiMotion.ShortFade))
            }, label = "daily_page"
        ) { currentPage ->
            when (currentPage) {
                LogPage.DAY -> LazyColumn(
                    state = dayScroll, modifier = Modifier.fillMaxSize().testTag("daily_scroll"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item("welcome") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(if (LocalDate.of(year, month, day).isBefore(com.jeremy.lumi.utils.LumiClock.today()))
                                R.string.calendar_past_greeting else R.string.daily_greeting), fontFamily = LumiEditorialSerif,
                                fontSize = 32.sp, lineHeight = 38.sp, color = tokens.text)
                            LogHint(stringResource(R.string.daily_intro))
                            if (savedLog != null) LogHint(stringResource(R.string.daily_editing))
                            TextButton(onClick = { page = LogPage.CUSTOMIZE }, enabled = !isSaving,
                                contentPadding = PaddingValues(horizontal = 0.dp), modifier = Modifier.testTag("daily_customize")) {
                                Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp), tint = readableLogAccent())
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.daily_customize), color = readableLogAccent(), fontSize = 14.sp)
                            }
                        }
                    }
                    item("mood") {
                        LogCard {
                            LogSectionTitle(R.string.daily_mood, R.string.daily_mood_hint)
                            LogOptionFlow(
                                ClinicalLabels.MOOD_MAPPINGS, draft.mood, enabled = !isSaving,
                                icons = listOf(Icons.Rounded.SentimentSatisfiedAlt, Icons.Rounded.Spa,
                                    Icons.Rounded.WbSunny, Icons.Rounded.FavoriteBorder,
                                    Icons.Rounded.SentimentDissatisfied, Icons.Rounded.Air,
                                    Icons.Rounded.Thunderstorm, Icons.Rounded.Cloud)
                            ) { update(draft.copy(mood = it)) }
                        }
                    }
                    item("symptoms") {
                        LogCard {
                            LogSectionTitle(R.string.daily_symptoms, R.string.daily_symptoms_hint)
                            if (favorites.isEmpty()) LogHint(stringResource(R.string.daily_favorites_empty))
                            LogSymptomChoices(favorites.toList(), draft.symptoms.keys, !isSaving, toggle)
                            TextButton(onClick = { page = LogPage.CATALOG }, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp), tint = readableLogAccent())
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.daily_all_symptoms), color = readableLogAccent(), fontSize = 14.sp)
                            }
                            AnimatedVisibility(draft.symptoms.isNotEmpty(),
                                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    HorizontalDivider(color = tokens.line)
                                    Text(stringResource(R.string.daily_selected), color = tokens.text,
                                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                                    draft.symptoms.forEach { (name, intensity) -> key(name) {
                                        val label = logSymptomLabel(name)
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                                .clickable(enabled = !isSaving) { intensityFor = name }
                                                .padding(vertical = 10.dp, horizontal = 4.dp)) {
                                                Text(label, fontSize = 14.sp, color = tokens.text)
                                                Text(if (intensity == 0) stringResource(R.string.daily_add_intensity)
                                                    else stringResource(R.string.daily_intensity_value, intensity),
                                                    color = readableLogAccent(), fontSize = 12.sp)
                                            }
                                            IconButton(onClick = { toggle(name) }, enabled = !isSaving) {
                                                Icon(Icons.Rounded.Close, stringResource(R.string.daily_remove, label),
                                                    Modifier.size(18.dp), tint = tokens.muted)
                                            }
                                        }
                                    } }
                                }
                            }
                        }
                    }
                    item("details_heading") { LogSectionTitle(R.string.daily_details, R.string.daily_optional) }
                    items(logDetailSections.filter { sectionEnabled(it.key, categories) || draft.hasSectionData(it.key) },
                        key = { it.key }) { section -> LogDetailSection(section, draft, !isSaving, update) }
                }
                LogPage.CATALOG, LogPage.FAVORITES -> LogSymptomCatalog(
                    favoritesMode = currentPage == LogPage.FAVORITES,
                    custom = custom + draft.symptoms.keys.filter { name -> logSymptomCatalog.none { it.name == name } },
                    selected = if (currentPage == LogPage.FAVORITES) favorites else draft.symptoms.keys,
                    onToggle = {
                        if (currentPage == LogPage.FAVORITES) setFavorites(if (it in favorites) favorites - it else favorites + it)
                        else toggle(it)
                    }, onAddCustom = { addingCustom = true }
                )
                LogPage.CUSTOMIZE -> LazyColumn(
                    modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item { LogEditorialTitle(R.string.daily_customize_title, R.string.daily_customize_hint) }
                    item {
                        LogCard {
                            LogSectionTitle(R.string.daily_favorites, R.string.daily_favorites_hint)
                            // Read-only preview. Selection changes happen in the explicitly labeled picker.
                            Text(favorites.map { logSymptomLabel(it) }.joinToString(" · "),
                                color = tokens.text, fontSize = 14.sp, lineHeight = 22.sp)
                            TextButton(onClick = { page = LogPage.FAVORITES }) {
                                Icon(Icons.Rounded.StarOutline, null, tint = readableLogAccent())
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.daily_favorites_edit), color = readableLogAccent())
                            }
                        }
                    }
                    item {
                        LogCard {
                            LogSectionTitle(R.string.daily_customize_sections)
                            logDetailSections.forEach { section ->
                                LogToggleRow(stringResource(section.title), sectionEnabled(section.key, categories)) { enabled ->
                                    categories = changeSection(section.key, enabled, categories)
                                    onCategories(categories)
                                }
                            }
                            LogHint(stringResource(R.string.daily_customize_keep))
                        }
                    }
                }
            }
        }
        Surface(color = tokens.bg, shadowElevation = 12.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (page == LogPage.DAY) {
                    val status = when {
                        saveFailed -> R.string.daily_save_error
                        !draft.validMeasurements -> R.string.daily_invalid
                        draft.hasContent -> R.string.daily_save_ready
                        else -> R.string.daily_save_hint
                    }
                    Text(stringResource(status), fontSize = 12.sp, lineHeight = 17.sp,
                        color = if (saveFailed || !draft.validMeasurements) MaterialTheme.colorScheme.error else tokens.muted,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    LogPrimaryAction(
                        stringResource(when {
                            isSaving -> R.string.daily_saving
                            savedLog != null -> R.string.daily_update
                            else -> R.string.daily_save
                        }),
                        enabled = !isSaving && draft.validMeasurements &&
                            (draft.hasContent || savedLog != null) && (savedLog == null || draft != initial),
                        loading = isSaving, modifier = Modifier.testTag("daily_save")
                    ) {
                        focus.clearFocus()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(draft)
                    }
                } else {
                    if (page == LogPage.CATALOG || page == LogPage.FAVORITES) Text(
                        stringResource(R.string.daily_count, if (page == LogPage.FAVORITES) favorites.size else draft.symptoms.size),
                        fontSize = 13.sp, color = tokens.muted, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    LogPrimaryAction(stringResource(R.string.daily_done)) {
                        focus.clearFocus()
                        page = LogPage.DAY
                    }
                }
            }
        }
    }
    if (discard) AlertDialog(
        onDismissRequest = { discard = false }, containerColor = tokens.panel,
        title = { Text(stringResource(R.string.daily_discard_title)) },
        text = { Text(stringResource(R.string.daily_discard_body)) },
        confirmButton = { TextButton(onClick = { discard = false; onDismiss() }) {
            Text(stringResource(R.string.daily_discard), color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(stringResource(R.string.daily_keep)) } }
    )
    if (addingCustom) LogCustomSymptomDialog(
        existing = logSymptomCatalog.map { it.name }.toSet() + custom + draft.symptoms.keys,
        onDismiss = { addingCustom = false }
    ) { name ->
        custom = custom + name
        onCustomSymptoms(custom)
        if (page == LogPage.FAVORITES) setFavorites(favorites + name) else update(draft.toggleSymptom(name))
        addingCustom = false
    }
    intensityFor?.let { name ->
        LogIntensityDialog(logSymptomLabel(name), draft.symptoms[name] ?: 0,
            onDismiss = { intensityFor = null },
            onDone = { value -> update(draft.copy(symptoms = draft.symptoms + (name to value))); intensityFor = null })
    }
}
