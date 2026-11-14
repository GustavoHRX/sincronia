package com.sincronia.app.ui.editor

import com.sincronia.app.core.alarm.AlarmMode
import com.sincronia.app.data.edit.ItemType
import java.time.Instant
import java.time.ZoneId

/**
 * M7: the alarm the editor is about to save, when its time has already passed. Such an alarm never
 * rings (spec 7.4), so the editor warns once; "Salvar" again saves it anyway. AUTO follows Google's
 * reminders and is left out.
 */
object PastAlarm {
    fun trigger(s: EditorState, start: Instant?, zone: ZoneId): Instant? = when {
        s.type == ItemType.TASK -> if (s.taskHasTime && s.hasDueDate) s.date.atTime(s.alarmTime).atZone(zone).toInstant() else null
        s.allDay -> if (s.alarmMode == AlarmMode.TIME) s.date.atTime(s.alarmTime).atZone(zone).toInstant() else null
        // A timed event saves TIME as OFFSET (EditorViewModel.saveEventAlarm).
        s.alarmMode == AlarmMode.OFFSET || s.alarmMode == AlarmMode.TIME -> start?.minusSeconds(s.alarmOffset * 60L)
        else -> null
    }

    fun passed(s: EditorState, start: Instant?, zone: ZoneId, now: Instant): Boolean =
        trigger(s, start, zone)?.let { it <= now } ?: false
}
