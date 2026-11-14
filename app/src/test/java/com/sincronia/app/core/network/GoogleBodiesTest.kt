package com.sincronia.app.core.network

import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.data.edit.EventField
import com.sincronia.app.data.edit.TaskField
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleBodiesTest {
    @Test fun `all-day event only on 29-09 ends on 30-09`() {
        val body = GoogleBodies.event(EventEntity("l", "a", "abc", title = "Feriado", allDay = true,
            startDate = LocalDate.of(2026, 9, 29), endDate = LocalDate.of(2026, 9, 30)))
        assertEquals("""{"id":"abc","summary":"Feriado","description":null,"location":null,""" +
            """"start":{"date":"2026-09-29","dateTime":null},"end":{"date":"2026-09-30","dateTime":null},""" +
            """"reminders":{"useDefault":true}}""", body.toString())
    }

    @Test fun `PATCH with time and X minutes reminder carries only those fields`() {
        val event = EventEntity("l", "a", "abc", title = "Aula", startAt = Instant.parse("2026-09-29T13:00:00Z"),
            endAt = Instant.parse("2026-09-29T14:00:00Z"), timeZone = "America/Sao_Paulo",
            reminderUseDefault = false, reminderMinutes = listOf(15))
        val body = GoogleBodies.event(event, listOf(EventField.TIME, EventField.REMINDERS))
        assertEquals("""{"start":{"dateTime":"2026-09-29T13:00:00Z","timeZone":"America/Sao_Paulo","date":null},""" +
            """"end":{"dateTime":"2026-09-29T14:00:00Z","timeZone":"America/Sao_Paulo","date":null},""" +
            """"reminders":{"useDefault":false,"overrides":[{"method":"popup","minutes":15}]}}""", body.toString())
    }

    @Test fun `task due is a date at UTC midnight and undo sends completed null`() {
        val task = TaskEntity("l", "a", "g", "list", title = "Ler", dueDate = LocalDate.of(2026, 9, 29), status = "needsAction")
        assertEquals("""{"title":"Ler","notes":null,"due":"2026-09-29T00:00:00.000Z","status":"needsAction","completed":null}""",
            GoogleBodies.task(task).toString())
        assertEquals("""{"status":"needsAction","completed":null}""", GoogleBodies.task(task, listOf(TaskField.STATUS)).toString())
        assertEquals("""{"status":"completed"}""", GoogleBodies.task(task.copy(status = "completed"), listOf(TaskField.STATUS)).toString())
    }
}
