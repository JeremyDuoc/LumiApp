package com.jeremy.lumi.ui.screens.calendar

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class PlannerItemKind { TASK, EVENT, REMINDER }

data class PlannerItem(
    val id: String,
    val date: LocalDate,
    val startMinutes: Int,
    val durationMinutes: Int,
    val title: String,
    val kind: PlannerItemKind,
    val note: String = "",
    val completed: Boolean = false
) {
    val endMinutes: Int get() = (startMinutes + durationMinutes).coerceAtMost(24 * 60)
}

data class PlannerInboxItem(
    val id: String,
    val title: String,
    val durationMinutes: Int = 30,
    val kind: PlannerItemKind = PlannerItemKind.TASK,
    val note: String = ""
)

data class PlannerUiState(
    val items: List<PlannerItem> = emptyList(),
    val inbox: List<PlannerInboxItem> = emptyList()
) {
    fun itemsFor(date: LocalDate): List<PlannerItem> =
        items.filter { it.date == date }.sortedBy { it.startMinutes }

    fun activityCount(date: LocalDate): Int = items.count { it.date == date }
}

@Singleton
class PlannerStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("lumi_planner_store_v1", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<PlannerUiState> = _state.asStateFlow()

    @Synchronized
    fun createScheduled(
        date: LocalDate,
        title: String,
        startMinutes: Int,
        durationMinutes: Int,
        kind: PlannerItemKind,
        note: String = ""
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val item = PlannerItem(
            id = UUID.randomUUID().toString(),
            date = date,
            startMinutes = startMinutes.coerceIn(0, 23 * 60 + 59),
            durationMinutes = durationMinutes.coerceIn(5, 12 * 60),
            title = cleanTitle.take(80),
            kind = kind,
            note = note.trim().take(180)
        )
        commit(_state.value.copy(items = _state.value.items + item))
    }

    @Synchronized
    fun toggleCompleted(id: String) {
        commit(_state.value.copy(items = _state.value.items.map {
            if (it.id == id) it.copy(completed = !it.completed) else it
        }))
    }

    @Synchronized
    fun deleteScheduled(id: String) {
        commit(_state.value.copy(items = _state.value.items.filterNot { it.id == id }))
    }

    @Synchronized
    fun addInbox(
        title: String,
        durationMinutes: Int = 30,
        kind: PlannerItemKind = PlannerItemKind.TASK,
        note: String = ""
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val item = PlannerInboxItem(
            id = UUID.randomUUID().toString(),
            title = cleanTitle.take(80),
            durationMinutes = durationMinutes.coerceIn(5, 12 * 60),
            kind = kind,
            note = note.trim().take(180)
        )
        commit(_state.value.copy(inbox = _state.value.inbox + item))
    }

    @Synchronized
    fun scheduleInbox(id: String, date: LocalDate, startMinutes: Int) {
        val pending = _state.value.inbox.firstOrNull { it.id == id } ?: return
        val scheduled = PlannerItem(
            id = UUID.randomUUID().toString(),
            date = date,
            startMinutes = startMinutes.coerceIn(0, 23 * 60 + 59),
            durationMinutes = pending.durationMinutes,
            title = pending.title,
            kind = pending.kind,
            note = pending.note
        )
        commit(
            _state.value.copy(
                items = _state.value.items + scheduled,
                inbox = _state.value.inbox.filterNot { it.id == id }
            )
        )
    }

    @Synchronized
    fun moveToInbox(id: String) {
        val scheduled = _state.value.items.firstOrNull { it.id == id } ?: return
        val pending = PlannerInboxItem(
            id = UUID.randomUUID().toString(),
            title = scheduled.title,
            durationMinutes = scheduled.durationMinutes,
            kind = scheduled.kind,
            note = scheduled.note
        )
        commit(
            _state.value.copy(
                items = _state.value.items.filterNot { it.id == id },
                inbox = _state.value.inbox + pending
            )
        )
    }

    private fun commit(next: PlannerUiState) {
        _state.value = next
        prefs.edit().putString(KEY_STATE, encode(next).toString()).apply()
    }

    private fun readState(): PlannerUiState {
        val raw = prefs.getString(KEY_STATE, null) ?: return PlannerUiState()
        return runCatching { decode(JSONObject(raw)) }.getOrElse { PlannerUiState() }
    }

    private fun encode(state: PlannerUiState): JSONObject = JSONObject().apply {
        put("items", JSONArray().apply {
            state.items.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("date", item.date.toString())
                    put("start", item.startMinutes)
                    put("duration", item.durationMinutes)
                    put("title", item.title)
                    put("kind", item.kind.name)
                    put("note", item.note)
                    put("completed", item.completed)
                })
            }
        })
        put("inbox", JSONArray().apply {
            state.inbox.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("duration", item.durationMinutes)
                    put("kind", item.kind.name)
                    put("note", item.note)
                })
            }
        })
    }

    private fun decode(root: JSONObject): PlannerUiState {
        val items = buildList {
            val array = root.optJSONArray("items") ?: JSONArray()
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val date = runCatching { LocalDate.parse(json.optString("date")) }.getOrNull() ?: continue
                val kind = runCatching { PlannerItemKind.valueOf(json.optString("kind")) }
                    .getOrDefault(PlannerItemKind.TASK)
                add(
                    PlannerItem(
                        id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                        date = date,
                        startMinutes = json.optInt("start", 18 * 60),
                        durationMinutes = json.optInt("duration", 30),
                        title = json.optString("title"),
                        kind = kind,
                        note = json.optString("note"),
                        completed = json.optBoolean("completed", false)
                    )
                )
            }
        }
        val inbox = buildList {
            val array = root.optJSONArray("inbox") ?: JSONArray()
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val kind = runCatching { PlannerItemKind.valueOf(json.optString("kind")) }
                    .getOrDefault(PlannerItemKind.TASK)
                add(
                    PlannerInboxItem(
                        id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = json.optString("title"),
                        durationMinutes = json.optInt("duration", 30),
                        kind = kind,
                        note = json.optString("note")
                    )
                )
            }
        }
        return PlannerUiState(items = items, inbox = inbox)
    }

    private companion object {
        const val KEY_STATE = "planner_state"
    }
}
