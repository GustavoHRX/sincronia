package com.sincronia.app.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "events",
    primaryKeys = ["accountId", "localId"],
    indices = [
        Index(value = ["accountId", "googleId"], unique = true),
        Index(value = ["accountId", "startAt"]),
        Index(value = ["accountId", "startDate"]),
    ],
)
data class EventEntity(
    val localId: String,
    val accountId: String,
    val googleId: String?,
    val calendarId: String = "primary",
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val allDay: Boolean = false,
    val startAt: Instant? = null,
    val endAt: Instant? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val timeZone: String? = null,
    val status: String = "confirmed",
    val eventType: String = "default",
    val recurringEventId: String? = null,
    val originalStartTime: String? = null,
    val organizerSelf: Boolean = true,
    val guestsCanModify: Boolean = false,
    val selfResponseStatus: String? = null,
    val reminderUseDefault: Boolean = true,
    val reminderMinutes: List<Int> = emptyList(),
    val colorId: String? = null,
    val htmlLink: String? = null,
    val etag: String? = null,
    val remoteUpdated: Instant? = null,
    val updatedAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val syncState: String = "SYNCED",
)

@Entity(
    tableName = "tasks",
    primaryKeys = ["accountId", "localId"],
    indices = [
        Index(value = ["accountId", "googleId"], unique = true),
        Index(value = ["accountId", "taskListId", "dueDate"]),
        Index(value = ["accountId", "parentId"]),
    ],
)
data class TaskEntity(
    val localId: String,
    val accountId: String,
    val googleId: String?,
    val taskListId: String,
    val remoteTaskListId: String? = null,
    val title: String,
    val notes: String? = null,
    val dueDate: LocalDate? = null,
    val status: String = "needsAction",
    val completedAt: Instant? = null,
    val parentId: String? = null,
    val position: String? = null,
    val hidden: Boolean = false,
    val webViewLink: String? = null,
    val etag: String? = null,
    val remoteUpdated: Instant? = null,
    val updatedAt: Instant = Instant.now(),
    val deleted: Boolean = false,
    val syncState: String = "SYNCED",
)

@Entity(tableName = "task_lists", primaryKeys = ["accountId", "googleId"])
data class TaskListEntity(
    val accountId: String,
    val googleId: String,
    val title: String,
    val isDefault: Boolean = false,
    val etag: String? = null,
    val remoteUpdated: Instant? = null,
)

@Entity(
    tableName = "alarms",
    indices = [Index(value = ["accountId", "itemType", "itemLocalId"], unique = true), Index("triggerAt")],
)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: String,
    val itemType: String,
    val itemLocalId: String,
    val mode: String = "OFF",
    val offsetMinutes: Int? = null,
    val localTime: LocalTime? = null,
    val triggerAt: Instant? = null,
    val scheduled: Boolean = false,
    val snoozedUntil: Instant? = null,
    val dismissedTriggerAt: Instant? = null,
    val soundUri: String? = null,
    val vibrate: Boolean = true,
)

@Entity(
    tableName = "outbox",
    indices = [Index(value = ["accountId", "itemType", "itemLocalId"], unique = true)],
)
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val itemType: String,
    val itemLocalId: String,
    val operation: String,
    val changedFields: List<String> = emptyList(),
    val state: String = "PENDING",
    val attempts: Int = 0,
    val nextAttemptAt: Instant? = null,
    val lastError: String? = null,
    val createdAt: Instant = Instant.now(),
)

// The key is also prefixed with "<accountId>:" for future multi-account support.
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val key: String,
    val accountId: String,
    val value: String,
)
