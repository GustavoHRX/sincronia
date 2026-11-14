package com.sincronia.app.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.core.database.TaskListEntity
import com.sincronia.app.data.ActiveAccount
import com.sincronia.app.core.alarm.AlarmMode
import com.sincronia.app.data.edit.EditPolicy
import com.sincronia.app.data.settings.SettingsRepository
import com.sincronia.app.data.edit.EditRepository
import com.sincronia.app.data.edit.ItemType
import com.sincronia.app.data.edit.ReadOnlyReason
import com.sincronia.app.data.edit.SyncRequester
import com.sincronia.app.data.sync.OutboxProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Reminder choice: null minutes with [useDefault] = calendar default; empty = no reminder. */
data class Reminder(val useDefault: Boolean, val minutes: Int? = null)

enum class EditorMessage { NONE, TITLE_REQUIRED, END_BEFORE_START, OFFLINE, PAST_ALARM }

data class EditorState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val type: String = ItemType.EVENT,
    val title: String = "",
    val notes: String = "",
    val location: String = "",
    val allDay: Boolean = false,
    val date: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now(),
    val start: LocalTime = LocalTime.of(9, 0),
    val end: LocalTime = LocalTime.of(10, 0),
    val hasDueDate: Boolean = true,
    val listId: String? = null,
    val lists: List<TaskListEntity> = emptyList(),
    val reminder: Reminder = Reminder(useDefault = true),
    val readOnly: ReadOnlyReason? = null,
    /** Local alarm (spec 7.4): AUTO, OFFSET, TIME or OFF. */
    val alarmMode: String = AlarmMode.AUTO,
    val alarmOffset: Int = 10,
    val alarmTime: LocalTime = LocalTime.of(9, 0),
    val taskHasTime: Boolean = false,
    /** Recurring series of this occurrence, and whether its alarms are off as a whole. */
    val seriesId: String? = null,
    val seriesOff: Boolean = false,
    val failed: Boolean = false,
    val busy: Boolean = false,
    val message: EditorMessage = EditorMessage.NONE,
    val closed: Boolean = false,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val database: AppDatabase,
    private val edits: EditRepository,
    private val outbox: OutboxProcessor,
    private val sync: SyncRequester,
    private val activeAccount: ActiveAccount,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val itemId: String? = handle.get<String>("id")?.takeIf { it.isNotBlank() }
    private var accountId = ""
    private var originalEvent: EventEntity? = null
    private var originalTask: TaskEntity? = null
    /** Times untouched keep the original instants (multi-day timed events stay intact). */
    private var timeTouched = false

    private val _state = MutableStateFlow(EditorState(
        type = handle.get<String>("type")?.takeIf { it.isNotBlank() } ?: ItemType.EVENT,
        date = handle.get<String>("date")?.takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: LocalDate.now(),
        listId = handle.get<String>("list")?.takeIf { it.isNotBlank() },
    ).let { it.copy(endDate = it.date) })
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            accountId = activeAccount.id.first()
            val lists = database.taskListDao().all(accountId).sortedWith(compareByDescending<TaskListEntity> { it.isDefault }.thenBy { it.title })
            _state.update { it.copy(lists = lists, listId = it.listId ?: lists.firstOrNull()?.googleId) }
            if (itemId == null) {
                // New event: "10 min antes" by default, also sent to Google as its reminder (spec 7.4).
                val minutes = settings.current().newEventAlarmMinutes
                _state.update { it.copy(loading = false, reminder = Reminder(false, minutes),
                    alarmMode = AlarmMode.OFFSET, alarmOffset = minutes) }
            } else if (_state.value.type == ItemType.EVENT) {
                database.eventDao().byLocalId(accountId, itemId)?.let(::loadEvent) ?: close()
            } else {
                database.taskDao().byLocalId(accountId, itemId)?.let(::loadTask) ?: close()
            }
            originalEvent?.recurringEventId?.let { series ->
                val off = edits.isSeriesAlarmOff(accountId, series)
                _state.update { it.copy(seriesId = series, seriesOff = off) }
            }
            itemId?.let { id ->
                database.alarmDao().forItem(accountId, _state.value.type, id)?.let { alarm ->
                    _state.update {
                        it.copy(alarmMode = alarm.mode, alarmOffset = alarm.offsetMinutes ?: it.alarmOffset,
                            alarmTime = alarm.localTime ?: it.alarmTime,
                            taskHasTime = alarm.itemType == ItemType.TASK && alarm.mode == AlarmMode.TIME)
                    }
                }
            }
            loadedAlarm = alarmFields(_state.value)
        }
    }

    private fun loadEvent(event: EventEntity) {
        originalEvent = event
        val start = event.startAt?.atZone(zone)
        val end = event.endAt?.atZone(zone)
        _state.update {
            it.copy(loading = false, isNew = false, title = event.title, notes = event.description.orEmpty(),
                location = event.location.orEmpty(), allDay = event.allDay,
                date = event.startDate ?: start?.toLocalDate() ?: it.date,
                endDate = event.endDate?.minusDays(1) ?: end?.toLocalDate() ?: it.date,
                start = start?.toLocalTime()?.withSecond(0)?.withNano(0) ?: it.start,
                end = end?.toLocalTime()?.withSecond(0)?.withNano(0) ?: it.end,
                reminder = if (event.reminderUseDefault) Reminder(true) else Reminder(false, event.reminderMinutes.firstOrNull()),
                readOnly = EditPolicy.eventReadOnly(event), failed = event.syncState == "FAILED")
        }
    }

    private fun loadTask(task: TaskEntity) {
        originalTask = task
        _state.update {
            it.copy(loading = false, isNew = false, title = task.title, notes = task.notes.orEmpty(),
                hasDueDate = task.dueDate != null, date = task.dueDate ?: it.date, listId = task.taskListId,
                failed = task.syncState == "FAILED")
        }
    }

    /** Alarm fields as loaded: editing only the title of a past event must not warn about its alarm. */
    private var loadedAlarm: List<Any?>? = null
    private fun alarmFields(s: EditorState) = listOf(s.alarmMode, s.alarmOffset, s.alarmTime, s.taskHasTime, s.hasDueDate, s.date, s.allDay)

    fun edit(change: (EditorState) -> EditorState) = _state.update { change(it).copy(message = EditorMessage.NONE) }
    fun editTime(change: (EditorState) -> EditorState) { timeTouched = true; edit(change) }

    fun save() {
        val s = _state.value
        if (s.title.isBlank()) return _state.update { it.copy(message = EditorMessage.TITLE_REQUIRED) }
        if (s.type == ItemType.EVENT && s.allDay && s.endDate < s.date) {
            return _state.update { it.copy(message = EditorMessage.END_BEFORE_START) }
        }
        val changed = s.isNew || timeTouched || alarmFields(s) != loadedAlarm
        // Second "Salvar" right after the warning saves anyway (the message is still on screen).
        if (changed && s.message != EditorMessage.PAST_ALARM && PastAlarm.passed(s, eventStart(s), zone, Instant.now())) {
            return _state.update { it.copy(message = EditorMessage.PAST_ALARM) }
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            if (s.type == ItemType.EVENT) saveEvent(s) else saveTask(s)
            close()
        }
    }

    private fun eventStart(s: EditorState): Instant? {
        val original = originalEvent
        return if (original != null && !timeTouched && s.allDay == original.allDay) original.startAt
        else if (s.allDay) null else s.date.atTime(s.start).atZone(zone).toInstant()
    }

    private suspend fun saveEvent(s: EditorState) {
        val base = originalEvent ?: EventEntity("", accountId, null, title = "")
        val reminders = if (s.reminder.useDefault) emptyList() else listOfNotNull(s.reminder.minutes)
        var event = base.copy(title = s.title.trim(), description = s.notes.ifBlank { null },
            location = s.location.ifBlank { null }, reminderUseDefault = s.reminder.useDefault, reminderMinutes = reminders)
        if (originalEvent == null || timeTouched || s.allDay != base.allDay) {
            event = if (s.allDay) {
                event.copy(allDay = true, startDate = s.date, endDate = s.endDate.plusDays(1), startAt = null, endAt = null)
            } else {
                val startAt = s.date.atTime(s.start).atZone(zone).toInstant()
                // An end at or before the start crosses midnight.
                val endDay = if (s.end <= s.start) s.date.plusDays(1) else s.date
                event.copy(allDay = false, startAt = startAt, endAt = endDay.atTime(s.end).atZone(zone).toInstant(),
                    startDate = null, endDate = null, timeZone = zone.id)
            }
        }
        val original = originalEvent
        val saved = if (original == null) edits.createEvent(event) else event.also { edits.updateEvent(original, it) }
        saveEventAlarm(saved, s)
    }

    private suspend fun saveEventAlarm(event: EventEntity, s: EditorState) {
        val mode = when {
            s.alarmMode == AlarmMode.TIME && !s.allDay -> AlarmMode.OFFSET
            s.alarmMode in setOf(AlarmMode.OFFSET, AlarmMode.AUTO) && s.allDay -> AlarmMode.OFF
            else -> s.alarmMode
        }
        s.seriesId?.let { edits.setSeriesAlarmOff(accountId, it, s.seriesOff) }
        edits.setEventAlarm(event, mode, s.alarmOffset.takeIf { mode == AlarmMode.OFFSET },
            s.alarmTime.takeIf { mode == AlarmMode.TIME })
    }

    /** Read-only events keep the local alarm editable (spec 7.3). */
    fun saveAlarmOnly() {
        val event = originalEvent ?: return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            saveEventAlarm(event, _state.value)
            close()
        }
    }

    private suspend fun saveTask(s: EditorState) {
        val listId = s.listId ?: return
        val base = originalTask ?: TaskEntity("", accountId, null, listId, title = "")
        val task = base.copy(title = s.title.trim(), notes = s.notes.ifBlank { null },
            dueDate = if (s.hasDueDate) s.date else null, taskListId = listId)
        val original = originalTask
        val saved = if (original == null) edits.createTask(task) else task.also { edits.updateTask(original, it) }
        // The editor requires a date to set a time (spec 7.4).
        edits.setTaskTime(saved, s.alarmTime.takeIf { s.taskHasTime && s.hasDueDate })
    }

    fun delete() {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            originalEvent?.let { edits.deleteEvent(it) }
            originalTask?.let { edits.deleteTask(it) }
            close()
        }
    }

    fun retrySync() {
        val id = itemId ?: return
        viewModelScope.launch {
            outbox.retry(accountId, _state.value.type, id)
            sync.request()
            close()
        }
    }

    fun discardChange() {
        val id = itemId ?: return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                outbox.discard(accountId, _state.value.type, id)
                close()
            } catch (_: Exception) {
                _state.update { it.copy(busy = false, message = EditorMessage.OFFLINE) }
            }
        }
    }

    private fun close() = _state.update { it.copy(closed = true, busy = false) }
}
