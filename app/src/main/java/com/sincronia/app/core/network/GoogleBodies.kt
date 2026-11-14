package com.sincronia.app.core.network

import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.core.time.TaskDueDate
import com.sincronia.app.data.edit.EventField
import com.sincronia.app.data.edit.TaskField
import com.sincronia.app.core.alarm.TaskTimeMark
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Request bodies built field by field: a PATCH carries only the changed fields, and a cleared
 * value is sent as an explicit null (e.g. `completed: null` when undoing, spec 7.3).
 */
object GoogleBodies {
    /** [fields] null = everything (insert). */
    fun event(event: EventEntity, fields: Collection<String>? = null): JsonObject = buildJsonObject {
        fun has(field: String) = fields == null || field in fields
        if (fields == null) put("id", event.googleId)
        if (has(EventField.TITLE)) put("summary", event.title)
        if (has(EventField.DESCRIPTION)) put("description", event.description?.let(::JsonPrimitive) ?: JsonNull)
        if (has(EventField.LOCATION)) put("location", event.location?.let(::JsonPrimitive) ?: JsonNull)
        if (has(EventField.TIME)) {
            val zone = event.timeZone ?: ZoneId.systemDefault().id
            if (event.allDay) {
                // end.date is exclusive, as in Google: an event only on 29/09 ends on 30/09.
                put("start", buildJsonObject { put("date", event.startDate.toString()); put("dateTime", JsonNull) })
                put("end", buildJsonObject {
                    put("date", (event.endDate ?: event.startDate!!.plusDays(1)).toString()); put("dateTime", JsonNull)
                })
            } else {
                put("start", buildJsonObject { put("dateTime", event.startAt.toString()); put("timeZone", zone); put("date", JsonNull) })
                put("end", buildJsonObject { put("dateTime", event.endAt.toString()); put("timeZone", zone); put("date", JsonNull) })
            }
        }
        if (has(EventField.REMINDERS)) {
            put("reminders", buildJsonObject {
                put("useDefault", event.reminderUseDefault)
                if (!event.reminderUseDefault) {
                    put("overrides", buildJsonArray {
                        event.reminderMinutes.forEach { add(buildJsonObject { put("method", "popup"); put("minutes", it) }) }
                    })
                }
            })
        }
    }

    /** The list is not a body field: changing it is a move (spec 7.2.1). */
    /** [time] = alarm time kept in the notes as `⏰ HH:mm` (spec 7.3). */
    fun task(task: TaskEntity, fields: Collection<String>? = null, time: LocalTime? = null): JsonObject = buildJsonObject {
        fun has(field: String) = fields == null || field in fields
        if (has(TaskField.TITLE)) put("title", task.title)
        if (has(TaskField.NOTES) || has(TaskField.TIME)) {
            put("notes", TaskTimeMark.write(task.notes, time)?.let(::JsonPrimitive) ?: JsonNull)
        }
        if (has(TaskField.DUE)) put("due", task.dueDate?.let { JsonPrimitive(TaskDueDate.format(it)) } ?: JsonNull)
        if (has(TaskField.STATUS)) {
            put("status", task.status)
            if (task.status != "completed") put("completed", JsonNull)
        }
    }
}
