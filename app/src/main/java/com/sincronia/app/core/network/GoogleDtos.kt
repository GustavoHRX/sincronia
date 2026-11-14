package com.sincronia.app.core.network

import kotlinx.serialization.Serializable

// Only the fields the app reads; unknown fields are ignored by the Json configuration.

@Serializable
data class EventListDto(
    val items: List<EventDto> = emptyList(),
    val nextPageToken: String? = null,
    val timeZone: String? = null,
    val defaultReminders: List<ReminderDto> = emptyList(),
)

@Serializable
data class EventDto(
    val id: String,
    val status: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null,
    val start: EventTimeDto? = null,
    val end: EventTimeDto? = null,
    val recurringEventId: String? = null,
    val originalStartTime: EventTimeDto? = null,
    val organizer: OrganizerDto? = null,
    val guestsCanModify: Boolean = false,
    val attendees: List<AttendeeDto> = emptyList(),
    val reminders: RemindersDto? = null,
    val colorId: String? = null,
    val htmlLink: String? = null,
    val etag: String? = null,
    val updated: String? = null,
    val eventType: String? = null,
)

@Serializable
data class EventTimeDto(val dateTime: String? = null, val date: String? = null, val timeZone: String? = null)

@Serializable
data class OrganizerDto(val self: Boolean = false)

@Serializable
data class AttendeeDto(val self: Boolean = false, val responseStatus: String? = null)

@Serializable
data class RemindersDto(val useDefault: Boolean = true, val overrides: List<ReminderDto> = emptyList())

@Serializable
data class ReminderDto(val method: String? = null, val minutes: Int = 0)

@Serializable
data class TaskListsDto(val items: List<TaskListDto> = emptyList(), val nextPageToken: String? = null)

@Serializable
data class TaskListDto(val id: String, val title: String? = null, val etag: String? = null, val updated: String? = null)

@Serializable
data class TasksDto(val items: List<TaskDto> = emptyList(), val nextPageToken: String? = null)

@Serializable
data class TaskDto(
    val id: String,
    val title: String? = null,
    val notes: String? = null,
    val due: String? = null,
    val status: String? = null,
    val completed: String? = null,
    val parent: String? = null,
    val position: String? = null,
    val hidden: Boolean = false,
    val deleted: Boolean = false,
    val webViewLink: String? = null,
    val etag: String? = null,
    val updated: String? = null,
)

/** Google error body: the reason lives in error.errors[0].reason (spec 7.2). */
@Serializable
data class GoogleErrorDto(val error: GoogleErrorBody? = null) {
    val reason: String? get() = error?.errors?.firstOrNull()?.reason
}

@Serializable
data class GoogleErrorBody(val code: Int = 0, val message: String? = null, val errors: List<GoogleErrorItem> = emptyList())

@Serializable
data class GoogleErrorItem(val reason: String? = null)
