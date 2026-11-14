package com.sincronia.app.core.alarm

import com.sincronia.app.core.database.AlarmEntity
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

object AlarmMode {
    /** Follows the Google reminders of the event. */
    const val AUTO = "AUTO"
    /** X minutes before, chosen in the app. */
    const val OFFSET = "OFFSET"
    /** Fixed local time (tasks and all-day events). */
    const val TIME = "TIME"
    const val OFF = "OFF"
}

/** Pure rules of spec 7.4: when an item rings. Kept apart from Android so it can be unit tested. */
object AlarmPlanner {
    /** SyncMeta key (after the `<accountId>:` prefix) of a recurring series with alarms off. */
    const val SERIES_OFF_PREFIX = "alarm:series:"

    val WINDOW: Duration = Duration.ofDays(7)
    const val MAX_SCHEDULED = 50

    /**
     * Raw alarm instant of an event, before snooze, dismissal and the 7-day window.
     * [alarm] null means the default AUTO mode. [defaultReminders] = popup minutes of the calendar.
     */
    fun eventTrigger(
        event: EventEntity,
        alarm: AlarmEntity?,
        defaultReminders: List<Int>,
        zone: ZoneId,
        seriesOff: Boolean = false,
    ): Instant? {
        if (event.deleted) return null
        val mode = alarm?.mode ?: AlarmMode.AUTO
        // "Desligar alarme da série inteira": silences every occurrence still on AUTO; an
        // occurrence with its own choice (OFFSET or TIME) keeps ringing.
        if (seriesOff && mode == AlarmMode.AUTO) return null
        return when (mode) {
            AlarmMode.OFF -> null
            AlarmMode.TIME -> {
                val time = alarm?.localTime ?: return null
                val day = event.startDate ?: event.startAt?.atZone(zone)?.toLocalDate() ?: return null
                day.atTime(time).atZone(zone).toInstant()
            }
            AlarmMode.OFFSET -> {
                val start = event.startAt?.takeUnless { event.allDay } ?: return null
                start.minus(Duration.ofMinutes((alarm?.offsetMinutes ?: 0).toLong()))
            }
            else -> {
                // No automatic alarm: all-day, special types, declined invitations and cancelled events.
                if (event.allDay || event.eventType != "default" || event.status == "cancelled" ||
                    event.selfResponseStatus == "declined") return null
                val start = event.startAt ?: return null
                val minutes = if (event.reminderUseDefault) defaultReminders.maxOrNull() else event.reminderMinutes.maxOrNull()
                minutes?.let { start.minus(Duration.ofMinutes(it.toLong())) }
            }
        }
    }

    /** Tasks ring only with a date and a TIME alarm; completed or deleted tasks never ring. */
    fun taskTrigger(task: TaskEntity, alarm: AlarmEntity?, zone: ZoneId): Instant? {
        if (task.deleted || task.status == "completed" || alarm?.mode != AlarmMode.TIME) return null
        val time = alarm.localTime ?: return null
        val day = task.dueDate ?: return null
        return day.atTime(time).atZone(zone).toInstant()
    }

    /** Snooze wins while it is in the future. */
    fun effectiveTrigger(raw: Instant?, alarm: AlarmEntity?, now: Instant): Instant? {
        val snoozed = alarm?.snoozedUntil
        return if (snoozed != null && snoozed > now) snoozed else raw
    }

    /** Spec 7.4: only (now, now + 7 days], never a dismissed trigger. */
    fun shouldSchedule(trigger: Instant?, alarm: AlarmEntity?, now: Instant): Boolean =
        trigger != null && trigger > now && trigger <= now.plus(WINDOW) && alarm?.dismissedTriggerAt != trigger

    /**
     * Alarm time already passed but the event has not started (e.g. created 5 min before with a
     * 10 min alarm): one plain notification instead of an alarm that would fire at once.
     */
    fun isLateNotice(trigger: Instant?, alarm: AlarmEntity?, eventStart: Instant?, now: Instant): Boolean =
        trigger != null && trigger <= now && eventStart != null && eventStart > now && alarm?.dismissedTriggerAt != trigger
}
