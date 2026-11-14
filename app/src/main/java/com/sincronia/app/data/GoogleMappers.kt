package com.sincronia.app.data

import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.core.database.TaskListEntity
import com.sincronia.app.core.network.EventDto
import com.sincronia.app.core.network.TaskDto
import com.sincronia.app.core.network.TaskListDto
import com.sincronia.app.core.alarm.TaskTimeMark
import com.sincronia.app.core.time.TaskDueDate
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Google JSON to Room. `localId` stays blank: the sync keeps the local id of an item it already
 * knows (same googleId) and creates a UUID for new ones.
 */
object GoogleMappers {
    fun event(dto: EventDto, accountId: String, calendarTimeZone: String?): EventEntity? {
        if (dto.status == "cancelled") return null
        val allDay = dto.start?.date != null
        val startDate = dto.start?.date?.let(LocalDate::parse)
        val startAt = dto.start?.dateTime?.let(::instant)
        if (startDate == null && startAt == null) return null
        return EventEntity(
            localId = "",
            accountId = accountId,
            googleId = dto.id,
            title = dto.summary.orEmpty(),
            description = dto.description,
            location = dto.location,
            allDay = allDay,
            startAt = if (allDay) null else startAt,
            endAt = if (allDay) null else dto.end?.dateTime?.let(::instant) ?: startAt,
            startDate = startDate,
            // Exclusive, as in the Google API.
            endDate = if (allDay) dto.end?.date?.let(LocalDate::parse) ?: startDate?.plusDays(1) else null,
            timeZone = dto.start?.timeZone ?: calendarTimeZone,
            status = dto.status ?: "confirmed",
            eventType = dto.eventType ?: "default",
            recurringEventId = dto.recurringEventId,
            originalStartTime = dto.originalStartTime?.let { it.dateTime ?: it.date },
            organizerSelf = dto.organizer?.self ?: true,
            guestsCanModify = dto.guestsCanModify,
            selfResponseStatus = dto.attendees.firstOrNull { it.self }?.responseStatus,
            reminderUseDefault = dto.reminders?.useDefault ?: true,
            reminderMinutes = dto.reminders?.overrides.orEmpty().filter { it.method == "popup" }.map { it.minutes },
            colorId = dto.colorId,
            htmlLink = dto.htmlLink,
            etag = dto.etag,
            remoteUpdated = dto.updated?.let(::instant),
            updatedAt = Instant.now(),
            syncState = "SYNCED",
        )
    }

    /** `parentId` keeps the parent's googleId; the sync translates it to the local id. */
    fun task(dto: TaskDto, accountId: String, listId: String): TaskEntity = TaskEntity(
        localId = "",
        accountId = accountId,
        googleId = dto.id,
        taskListId = listId,
        remoteTaskListId = listId,
        title = dto.title.orEmpty(),
        notes = TaskTimeMark.parse(dto.notes).notes,
        dueDate = dto.due?.let(TaskDueDate::parse),
        status = dto.status ?: "needsAction",
        completedAt = dto.completed?.let(::instant),
        parentId = dto.parent,
        position = dto.position,
        hidden = dto.hidden,
        webViewLink = dto.webViewLink,
        etag = dto.etag,
        remoteUpdated = dto.updated?.let(::instant),
        updatedAt = Instant.now(),
        syncState = "SYNCED",
    )

    fun taskList(dto: TaskListDto, accountId: String, isDefault: Boolean) = TaskListEntity(
        accountId = accountId,
        googleId = dto.id,
        title = dto.title.orEmpty(),
        isDefault = isDefault,
        etag = dto.etag,
        remoteUpdated = dto.updated?.let(::instant),
    )

    /** RFC 3339 with offset or Z. */
    fun instant(value: String): Instant = OffsetDateTime.parse(value).toInstant()
}
