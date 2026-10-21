package com.jeremy.lumi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.R
import com.jeremy.lumi.ui.screens.calendar.CalendarDateMark
import com.jeremy.lumi.ui.screens.calendar.CalendarDay
import com.jeremy.lumi.ui.screens.calendar.calendarAccent
import com.jeremy.lumi.ui.screens.calendar.phaseLabel
import com.jeremy.lumi.ui.screens.home.CycleDayUi
import com.jeremy.lumi.ui.theme.LumiEditorialSerif
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.utils.LumiClock
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun LumiWeekTimeline(
    weekDays: List<CycleDayUi>, selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit, onViewCalendarClick: () -> Unit,
    onResetToToday: () -> Unit, modifier: Modifier = Modifier,
    onOpenDay: (LocalDate) -> Unit = { onViewCalendarClick() }
) {
    val tokens = LumiThemeTokens.current
    val locale = LocalConfiguration.current.locales[0]
    val today = LumiClock.today()
    val weeks = remember(weekDays) { weekDays.chunked(7).filter { it.size == 7 } }
    if (weeks.isEmpty()) return
    val todayWeek = weeks.indexOfFirst { week -> week.any { it.date == today } }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = todayWeek, pageCount = { weeks.size })
    val scope = rememberCoroutineScope()
    val effectiveDate = selectedDate ?: today
    LaunchedEffect(effectiveDate, weeks.first().first().date) {
        val target = weeks.indexOfFirst { week -> week.any { it.date == effectiveDate } }
        if (target >= 0 && target != pager.currentPage) pager.animateScrollToPage(target)
    }
    val currentWeek = weeks[pager.currentPage.coerceIn(weeks.indices)]
    val first = currentWeek.first().date
    val last = currentWeek.last().date
    val short = DateTimeFormatter.ofPattern("d MMM", locale)
    val range = if (first.month == last.month) "${first.dayOfMonth} – ${last.format(short)}"
        else "${first.format(short)} – ${last.format(short)}"
    val selected = currentWeek.firstOrNull { it.date == effectiveDate }
    Surface(modifier.padding(horizontal = 20.dp).fillMaxWidth().testTag("home_week"),
        shape = RoundedCornerShape(26.dp), color = tokens.panel.copy(alpha = .92f),
        border = BorderStroke(1.dp, tokens.line)) {
        Column(Modifier.padding(horizontal = 12.dp).padding(top = 6.dp, bottom = 12.dp)) {
            Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.calendar_week_title),
                        fontFamily = LumiEditorialSerif, fontSize = 20.sp, color = tokens.text)
                    Text(range.replaceFirstChar { it.titlecase(locale) }, fontSize = 11.sp, color = tokens.muted)
                }
                TextButton(onClick = onViewCalendarClick, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text(stringResource(R.string.calendar_view), color = calendarAccent(), fontSize = 12.sp)
                }
                IconButton(enabled = pager.currentPage > 0, onClick = {
                    scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) }
                }) { Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.calendar_previous_week),
                    tint = if (pager.currentPage > 0) tokens.muted else tokens.line) }
                IconButton(enabled = pager.currentPage < weeks.lastIndex, onClick = {
                    scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(weeks.lastIndex)) }
                }) { Icon(Icons.Rounded.ChevronRight, stringResource(R.string.calendar_next_week),
                    tint = if (pager.currentPage < weeks.lastIndex) tokens.muted else tokens.line) }
            }
            Spacer(Modifier.height(6.dp))
            HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth()
                .height((82 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp).testTag("week_pager")) { page ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    weeks[page].forEach { day ->
                        CalendarDateMark(
                            CalendarDay(day.dayOfMonth, day.phase, day.date == today, day.hasLog,
                                isPrediction = day.isPrediction, isFuture = day.date > today, date = day.date),
                            selected = day.date == effectiveDate, onClick = { onSelectDate(day.date) },
                            weekday = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                                .replaceFirstChar { it.titlecase(locale) }.take(3), modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = tokens.line)
            Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (selected != null) {
                        Text((if (selected.date == today) stringResource(R.string.calendar_today) else selected.date.format(short)) +
                            " · " + stringResource(if (selected.hasLog) R.string.calendar_recorded else R.string.calendar_no_record),
                            fontSize = 12.sp, fontWeight = FontWeight.Medium, color = tokens.text)
                        Text(if (selected.phase == com.jeremy.lumi.domain.model.CyclePhase.UNKNOWN)
                            stringResource(R.string.calendar_no_phase)
                            else stringResource(R.string.calendar_estimated_phase, phaseLabel(selected.phase)),
                            fontSize = 11.sp, lineHeight = 16.sp, color = tokens.muted)
                    } else Text(stringResource(R.string.calendar_choose_day), fontSize = 12.sp, color = tokens.muted)
                }
                if (effectiveDate != today || pager.currentPage != todayWeek) {
                    TextButton(onClick = {
                        onResetToToday()
                        scope.launch { pager.animateScrollToPage(todayWeek) }
                    }, modifier = Modifier.testTag("week_today"), contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(stringResource(R.string.calendar_today), color = calendarAccent(), fontSize = 12.sp)
                    }
                }
                if (selected != null) {
                    TextButton(onClick = { onOpenDay(selected.date) }, modifier = Modifier.testTag("week_open_day"),
                        contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(stringResource(when {
                            selected.date > today -> R.string.calendar_view_estimate
                            selected.date == today && selected.hasLog -> R.string.calendar_edit_today
                            selected.date == today -> R.string.calendar_log_today
                            else -> R.string.calendar_view_day
                        }), fontSize = 12.sp, color = calendarAccent())
                    }
                }
            }
        }
    }
}
