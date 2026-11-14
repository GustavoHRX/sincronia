package com.sincronia.app.data.edit

import androidx.room.withTransaction
import com.sincronia.app.core.alarm.AlarmMode
import com.sincronia.app.core.alarm.AlarmPlanner
import com.sincronia.app.core.database.SyncMetaEntity
import com.sincronia.app.core.database.AlarmEntity
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/** Asks for a sync after a local write (WorkManager in the app, a no-op in tests). */
fun interface SyncRequester {
    fun request()
}

/** Event field groups sent in a PATCH; each maps to one or more Google fields. */
object EventField {
    const val TITLE = "title"
    const val DESCRIPTION = "description"
    const val LOCATION = "location"
    const val TIME = "time"
    const val REMINDERS = "reminders"
}

object TaskField {
    const val TITLE = "title"
    const val NOTES = "notes"
    const val DUE = "dueDate"
    const val STATUS = "status"
    const val LIST = "taskListId"
    /** Alarm time: rewrites the `⏰ HH:mm` mark in the notes. */
    const val TIME = "time"
}

/**
 * Every write is local first: the item is saved as PENDING and goes to the outbox in the same
 * transaction; the sync sends it later (spec 7.2.1).
 */
class EditRepository @Inject constructor(
    private val database: AppDatabase,
    private val outbox: Outbox,
    private val sync: SyncRequester,
) {
    /** New event: the Google id is generated here (localId without hyphens, spec 7.2.1). */
    suspend fun createEvent(draft: EventEntity): EventEntity {
        val localId = UUID.randomUUID().toString()
        val event = draft.copy(localId = localId, googleId = localId.replace("-", "").lowercase(),
            calendarId = "primary", timeZone = draft.timeZone ?: ZoneId.systemDefault().id,
            status = "confirmed", eventType = "default", organizerSelf = true,
            updatedAt = Instant.now(), deleted = false, syncState = PENDING)
        write { database.eventDao().upsert(listOf(event)); outbox.enqueue(event.accountId, ItemType.EVENT, localId, Operation.CREATE) }
        return event
    }

    suspend fun updateEvent(original: EventEntity, edited: EventEntity) {
        val fields = eventChanges(original, edited)
        if (fields.isEmpty()) return
        write {
            database.eventDao().upsert(listOf(edited.copy(updatedAt = Instant.now(), syncState = PENDING)))
            outbox.enqueue(edited.accountId, ItemType.EVENT, edited.localId, Operation.UPDATE, fields)
        }
    }

    suspend fun deleteEvent(event: EventEntity) = write {
        database.eventDao().upsert(listOf(event.copy(deleted = true, updatedAt = Instant.now(), syncState = PENDING)))
        outbox.enqueue(event.accountId, ItemType.EVENT, event.localId, Operation.DELETE)
    }

    /** New task: Google gives the id on creation, so googleId stays null until then. */
    suspend fun createTask(draft: TaskEntity): TaskEntity {
        val task = draft.copy(localId = UUID.randomUUID().toString(), googleId = null, remoteTaskListId = null,
            updatedAt = Instant.now(), deleted = false, syncState = PENDING)
        write { database.taskDao().upsert(listOf(task)); outbox.enqueue(task.accountId, ItemType.TASK, task.localId, Operation.CREATE) }
        return task
    }

    suspend fun updateTask(original: TaskEntity, edited: TaskEntity) {
        val fields = taskChanges(original, edited)
        if (fields.isEmpty()) return
        write {
            database.taskDao().upsert(listOf(edited.copy(updatedAt = Instant.now(), syncState = PENDING)))
            outbox.enqueue(edited.accountId, ItemType.TASK, edited.localId, Operation.UPDATE, fields)
        }
    }

    /** Completing or undoing is an UPDATE of status (spec 7.2.1). */
    suspend fun setCompleted(task: TaskEntity, completed: Boolean) = updateTask(task, task.copy(
        status = if (completed) "completed" else "needsAction",
        completedAt = if (completed) Instant.now() else null,
    ))

    suspend fun deleteTask(task: TaskEntity) = write {
        database.taskDao().upsert(listOf(task.copy(deleted = true, updatedAt = Instant.now(), syncState = PENDING)))
        outbox.enqueue(task.accountId, ItemType.TASK, task.localId, Operation.DELETE)
    }

    /** Event alarm is always local only (spec 7.4); a new choice clears snooze and dismissal. */
    suspend fun setEventAlarm(event: EventEntity, mode: String, offsetMinutes: Int? = null, time: LocalTime? = null) {
        saveAlarm(event.accountId, ItemType.EVENT, event.localId, mode, offsetMinutes, time)
        sync.request()
    }

    /** Alarm of a whole recurring series, on or off; local only, like every alarm (spec 7.4). */
    suspend fun setSeriesAlarmOff(accountId: String, seriesId: String, off: Boolean) {
        val key = "$accountId:${AlarmPlanner.SERIES_OFF_PREFIX}$seriesId"
        if (off) database.syncMetaDao().upsert(SyncMetaEntity(key, accountId, "OFF"))
        else database.syncMetaDao().delete(accountId, key)
        sync.request()
    }

    suspend fun isSeriesAlarmOff(accountId: String, seriesId: String): Boolean =
        database.syncMetaDao().value(accountId, "$accountId:${AlarmPlanner.SERIES_OFF_PREFIX}$seriesId") != null

    /** Task time lives in the alarm and in the `⏰ HH:mm` mark on Google (spec 7.3). */
    suspend fun setTaskTime(task: TaskEntity, time: LocalTime?) {
        val current = database.alarmDao().forItem(task.accountId, ItemType.TASK, task.localId)
        val currentTime = current?.takeIf { it.mode == AlarmMode.TIME }?.localTime
        if (currentTime == time) return
        write {
            saveAlarm(task.accountId, ItemType.TASK, task.localId, if (time == null) AlarmMode.OFF else AlarmMode.TIME, null, time)
            outbox.enqueue(task.accountId, ItemType.TASK, task.localId, Operation.UPDATE, listOf(TaskField.TIME))
        }
    }

    private suspend fun saveAlarm(accountId: String, type: String, localId: String, mode: String, offset: Int?, time: LocalTime?) {
        val current = database.alarmDao().forItem(accountId, type, localId)
        val base = current ?: AlarmEntity(accountId = accountId, itemType = type, itemLocalId = localId)
        database.alarmDao().upsert(base.copy(mode = mode, offsetMinutes = offset, localTime = time,
            snoozedUntil = null, dismissedTriggerAt = null))
    }

    private suspend fun write(block: suspend () -> Unit) {
        database.withTransaction { block() }
        sync.request()
    }

    companion object {
        const val PENDING = "PENDING"

        fun eventChanges(old: EventEntity, new: EventEntity): List<String> = buildList {
            if (old.title != new.title) add(EventField.TITLE)
            if (old.description != new.description) add(EventField.DESCRIPTION)
            if (old.location != new.location) add(EventField.LOCATION)
            if (old.allDay != new.allDay || old.startAt != new.startAt || old.endAt != new.endAt ||
                old.startDate != new.startDate || old.endDate != new.endDate) add(EventField.TIME)
            if (old.reminderUseDefault != new.reminderUseDefault || old.reminderMinutes != new.reminderMinutes) {
                add(EventField.REMINDERS)
            }
        }

        fun taskChanges(old: TaskEntity, new: TaskEntity): List<String> = buildList {
            if (old.title != new.title) add(TaskField.TITLE)
            if (old.notes != new.notes) add(TaskField.NOTES)
            if (old.dueDate != new.dueDate) add(TaskField.DUE)
            if (old.status != new.status) add(TaskField.STATUS)
            if (old.taskListId != new.taskListId) add(TaskField.LIST)
        }
    }
}
