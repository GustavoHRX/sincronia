package com.sincronia.app.core.alarm

import com.sincronia.app.core.database.AlarmEntity
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPlannerTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val start = Instant.parse("2026-09-29T17:00:00Z") // 14:00 in Sao Paulo
    private val event = EventEntity("e", "a", "g", title = "Aula", startAt = start, endAt = start.plusSeconds(3600))
    private fun alarm(mode: String, offset: Int? = null, time: LocalTime? = null) =
        AlarmEntity(accountId = "a", itemType = "EVENT", itemLocalId = "e", mode = mode, offsetMinutes = offset, localTime = time)

    @Test fun `AUTO uses the earliest reminder, default or override, and none means no alarm`() {
        assertEquals(start.minusSeconds(30 * 60), AlarmPlanner.eventTrigger(event, null, listOf(10, 30), zone))
        val own = event.copy(reminderUseDefault = false, reminderMinutes = listOf(5, 15))
        assertEquals(start.minusSeconds(15 * 60), AlarmPlanner.eventTrigger(own, null, listOf(30), zone))
        assertNull(AlarmPlanner.eventTrigger(event.copy(reminderUseDefault = false), null, listOf(30), zone))
    }

    @Test fun `no automatic alarm for all-day, special, declined or cancelled events`() {
        val day = event.copy(allDay = true, startAt = null, endAt = null, startDate = LocalDate.of(2026, 9, 29))
        assertNull(AlarmPlanner.eventTrigger(day, null, listOf(10), zone))
        assertNull(AlarmPlanner.eventTrigger(event.copy(eventType = "birthday"), null, listOf(10), zone))
        assertNull(AlarmPlanner.eventTrigger(event.copy(selfResponseStatus = "declined"), null, listOf(10), zone))
        assertNull(AlarmPlanner.eventTrigger(event.copy(status = "cancelled"), null, listOf(10), zone))
        // The user may still set a time on an all-day event.
        assertEquals(Instant.parse("2026-09-29T11:00:00Z"),
            AlarmPlanner.eventTrigger(day, alarm(AlarmMode.TIME, time = LocalTime.of(8, 0)), emptyList(), zone))
    }

    @Test fun `OFFSET and OFF are local choices`() {
        assertEquals(start.minusSeconds(600), AlarmPlanner.eventTrigger(event, alarm(AlarmMode.OFFSET, 10), listOf(30), zone))
        assertNull(AlarmPlanner.eventTrigger(event, alarm(AlarmMode.OFF), listOf(30), zone))
    }

    @Test fun `task rings at due date plus local time, never when completed`() {
        val task = TaskEntity("t", "a", "g", "l", title = "Pagar", dueDate = LocalDate.of(2026, 9, 29))
        val time = alarm(AlarmMode.TIME, time = LocalTime.of(14, 30))
        assertEquals(Instant.parse("2026-09-29T17:30:00Z"), AlarmPlanner.taskTrigger(task, time, zone))
        assertNull(AlarmPlanner.taskTrigger(task.copy(status = "completed"), time, zone))
        assertNull(AlarmPlanner.taskTrigger(task.copy(dueDate = null), time, zone))
        assertNull(AlarmPlanner.taskTrigger(task, null, zone))
    }

    @Test fun `first sync at 15h does not fire alarms of the past`() {
        val now = Instant.parse("2026-09-29T18:00:00Z") // 15:00 in Sao Paulo, event started at 14:00
        val trigger = AlarmPlanner.eventTrigger(event, null, listOf(10), zone)
        assertFalse(AlarmPlanner.shouldSchedule(trigger, null, now))
        assertFalse(AlarmPlanner.isLateNotice(trigger, null, event.startAt, now))
    }

    @Test fun `window, dismissal, snooze and the late notice`() {
        val now = start.minusSeconds(3600)
        val trigger = start.minusSeconds(600)
        assertTrue(AlarmPlanner.shouldSchedule(trigger, null, now))
        assertFalse(AlarmPlanner.shouldSchedule(now.plusSeconds(8 * 86400), null, now))
        assertFalse(AlarmPlanner.shouldSchedule(trigger, alarm(AlarmMode.AUTO).copy(dismissedTriggerAt = trigger), now))
        val snoozed = alarm(AlarmMode.AUTO).copy(snoozedUntil = trigger.plusSeconds(300))
        assertEquals(trigger.plusSeconds(300), AlarmPlanner.effectiveTrigger(trigger, snoozed, now))
        // Created 5 min before the start with a 10 min alarm: notice once, not an alarm.
        val late = start.minusSeconds(300)
        assertTrue(AlarmPlanner.isLateNotice(trigger, null, start, late))
        assertFalse(AlarmPlanner.isLateNotice(trigger, alarm(AlarmMode.AUTO).copy(dismissedTriggerAt = trigger), start, late))
    }

    @Test fun `series off silences AUTO occurrences but keeps an occurrence with its own alarm`() {
        val occurrence = event.copy(recurringEventId = "s")
        assertNull(AlarmPlanner.eventTrigger(occurrence, null, listOf(10), zone, seriesOff = true))
        assertNull(AlarmPlanner.eventTrigger(occurrence, alarm(AlarmMode.AUTO), listOf(10), zone, seriesOff = true))
        assertEquals(start.minusSeconds(300),
            AlarmPlanner.eventTrigger(occurrence, alarm(AlarmMode.OFFSET, 5), listOf(10), zone, seriesOff = true))
    }
}
