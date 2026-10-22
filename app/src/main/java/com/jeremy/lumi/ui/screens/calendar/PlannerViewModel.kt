package com.jeremy.lumi.ui.screens.calendar

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val store: PlannerStore
) : ViewModel() {
    val uiState = store.state

    fun createScheduled(
        date: LocalDate,
        title: String,
        startMinutes: Int,
        durationMinutes: Int,
        kind: PlannerItemKind,
        note: String = ""
    ) = store.createScheduled(date, title, startMinutes, durationMinutes, kind, note)

    fun toggleCompleted(id: String) = store.toggleCompleted(id)
    fun deleteScheduled(id: String) = store.deleteScheduled(id)
    fun moveToInbox(id: String) = store.moveToInbox(id)
    fun addInbox(title: String, durationMinutes: Int = 30, kind: PlannerItemKind = PlannerItemKind.TASK) =
        store.addInbox(title, durationMinutes, kind)
    fun scheduleInbox(id: String, date: LocalDate, startMinutes: Int) =
        store.scheduleInbox(id, date, startMinutes)
}
