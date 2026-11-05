package com.sincronia.app.ui.today

import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayPlannerTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val day = LocalDate.of(2026, 9, 29)
    private fun at(date: LocalDate, hour: Int, minute: Int = 0) = date.atTime(hour, minute).atZone(zone).toInstant()
    private fun timed(id: String, start: java.time.Instant, end: java.time.Instant) =
        EventEntity(id, "demo", id, title = id, startAt = start, endAt = end)
    private fun allDay(id: String, start: LocalDate, end: LocalDate) =
        EventEntity(id, "demo", id, title = id, allDay = true, startDate = start, endDate = end)
    private fun task(id: String, due: LocalDate?) = TaskEntity(id, "demo", id, "personal", title = id, dueDate = due)

    @Test fun `greeting changes at 5h, 12h and 18h`() {
        assertEquals(Greeting.EVENING, TodayPlanner.greetingFor(LocalTime.of(4, 59)))
        assertEquals(Greeting.MORNING, TodayPlanner.greetingFor(LocalTime.of(5, 0)))
        assertEquals(Greeting.MORNING, TodayPlanner.greetingFor(LocalTime.of(11, 59)))
        assertEquals(Greeting.AFTERNOON, TodayPlanner.greetingFor(LocalTime.NOON))
        assertEquals(Greeting.AFTERNOON, TodayPlanner.greetingFor(LocalTime.of(17, 59)))
        assertEquals(Greeting.EVENING, TodayPlanner.greetingFor(LocalTime.of(18, 0)))
    }

    @Test fun `sections follow F4 with the event that began yesterday first`() {
        val overnight = timed("overnight", at(day.minusDays(1), 22), at(day, 1))
        val meeting = timed("meeting", at(day, 10), at(day, 11))
        val dinner = timed("dinner", at(day, 19), at(day, 21))
        val single = allDay("study", day, day.plusDays(1))
        val trip = allDay("trip", day.minusDays(1), day.plusDays(2))
        val late = task("late", day.minusDays(1))
        val shopping = task("shopping", day)
        val done = task("done", day).copy(status = "completed")

        val state = TodayPlanner.build(
            date = day, now = at(day, 15), zone = zone,
            events = listOf(dinner, single, meeting, trip, overnight),
            overdue = listOf(late), todayTasks = listOf(shopping), completed = listOf(done),
        )

        assertFalse(state.loading)
        assertEquals(Greeting.AFTERNOON, state.greeting)
        assertEquals(listOf("late"), state.overdue.map { it.localId })
        assertEquals(listOf("trip", "study"), state.allDayEvents.map { it.localId })
        assertEquals(listOf("shopping"), state.allDayTasks.map { it.localId })
        assertEquals(listOf("overnight", "meeting", "now", "dinner"), state.timeline.map(::id))
        assertEquals(listOf("done"), state.completed.map { it.localId })
        assertFalse(state.isEmpty)
    }

    @Test fun `now line goes first before any event and last after all of them`() {
        val meeting = timed("meeting", at(day, 10), at(day, 11))
        assertEquals(listOf("now", "meeting"), TodayPlanner.timeline(listOf(meeting), at(day, 8)).map(::id))
        assertEquals(listOf("meeting", "now"), TodayPlanner.timeline(listOf(meeting), at(day, 10)).map(::id))
        assertEquals(listOf("meeting", "now"), TodayPlanner.timeline(listOf(meeting), at(day, 23)).map(::id))
    }

    @Test fun `empty day has no timeline and no now line`() {
        val state = TodayPlanner.build(day, at(day, 9), zone, emptyList(), emptyList(), emptyList(), emptyList())
        assertTrue(state.timeline.isEmpty())
        assertTrue(state.isEmpty)
    }

    private fun id(entry: TimelineEntry) = when (entry) {
        is TimelineEntry.Event -> entry.event.localId
        is TimelineEntry.Now -> "now"
    }
}
