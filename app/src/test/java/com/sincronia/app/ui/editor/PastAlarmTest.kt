package com.sincronia.app.ui.editor

import com.sincronia.app.core.alarm.AlarmMode
import com.sincronia.app.data.edit.ItemType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PastAlarmTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val day = LocalDate.of(2026, 11, 13)
    private val now = day.atTime(18, 57, 3).atZone(zone).toInstant()
    private fun at(h: Int, m: Int): Instant = day.atTime(h, m).atZone(zone).toInstant()

    @Test fun `event created at 18h57m03 for 18h57 with alarm at start is flagged`() {
        val s = EditorState(type = ItemType.EVENT, date = day, start = LocalTime.of(18, 57), alarmMode = AlarmMode.OFFSET, alarmOffset = 0)
        assertTrue(PastAlarm.passed(s, at(18, 57), zone, now))
    }

    @Test fun `event three minutes ahead is not flagged, ten minutes before is`() {
        val s = EditorState(type = ItemType.EVENT, date = day, alarmMode = AlarmMode.OFFSET, alarmOffset = 0)
        assertFalse(PastAlarm.passed(s, at(19, 0), zone, now))
        assertTrue(PastAlarm.passed(s.copy(alarmOffset = 10), at(19, 0), zone, now))
    }

    @Test fun `alarm off, Google reminders and all-day without time give no warning`() {
        val s = EditorState(type = ItemType.EVENT, date = day, alarmMode = AlarmMode.OFF)
        assertNull(PastAlarm.trigger(s, at(10, 0), zone))
        assertNull(PastAlarm.trigger(s.copy(alarmMode = AlarmMode.AUTO), at(10, 0), zone))
        assertNull(PastAlarm.trigger(s.copy(allDay = true, alarmMode = AlarmMode.OFFSET), null, zone))
    }

    @Test fun `task with time uses its date and time, task without time never warns`() {
        val s = EditorState(type = ItemType.TASK, date = day, taskHasTime = true, alarmTime = LocalTime.of(18, 30))
        assertEquals(at(18, 30), PastAlarm.trigger(s, null, zone))
        assertTrue(PastAlarm.passed(s, null, zone, now))
        assertFalse(PastAlarm.passed(s.copy(taskHasTime = false), null, zone, now))
    }
}
