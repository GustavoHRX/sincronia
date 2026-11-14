package com.sincronia.app.ui.today

import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class Greeting { MORNING, AFTERNOON, EVENING }

/** One row of the Today timeline: a timed event or the "now" marker. */
sealed interface TimelineEntry {
    data class Event(val event: EventEntity) : TimelineEntry
    data class Now(val at: Instant) : TimelineEntry
    /** Task with an alarm time (M5): shown in the timeline at that time. */
    data class Task(val task: TaskEntity, val at: Instant) : TimelineEntry
}

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    val greeting: Greeting = TodayPlanner.greetingFor(LocalTime.now()),
    /** First name of the signed-in account; null in demo mode (login arrives in M3). */
    val firstName: String? = null,
    val overdue: List<TaskEntity> = emptyList(),
    val allDayEvents: List<EventEntity> = emptyList(),
    val allDayTasks: List<TaskEntity> = emptyList(),
    val timeline: List<TimelineEntry> = emptyList(),
    val completed: List<TaskEntity> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
) {
    val isEmpty: Boolean
        get() = overdue.isEmpty() && allDayEvents.isEmpty() && allDayTasks.isEmpty() &&
            timeline.isEmpty() && completed.isEmpty()
}

/** Pure F4 rules, kept apart from Room and Compose so they can be unit tested. */
object TodayPlanner {
    fun greetingFor(time: LocalTime): Greeting = when {
        time < LocalTime.of(5, 0) -> Greeting.EVENING
        time < LocalTime.NOON -> Greeting.MORNING
        time < LocalTime.of(18, 0) -> Greeting.AFTERNOON
        else -> Greeting.EVENING
    }

    /**
     * [events] are the items the DAO returns for [date] (all-day ones covering it and timed ones
     * crossing it). Until M5 every task has no alarm time, so all of today's tasks go to "Dia inteiro".
     */
    fun build(
        date: LocalDate,
        now: Instant,
        zone: ZoneId,
        events: List<EventEntity>,
        overdue: List<TaskEntity>,
        todayTasks: List<TaskEntity>,
        completed: List<TaskEntity>,
        firstName: String? = null,
        refreshing: Boolean = false,
        taskTimes: Map<String, LocalTime> = emptyMap(),
    ): TodayUiState {
        val (allDay, timed) = events.partition { it.allDay }
        val (withTime, withoutTime) = todayTasks.partition { it.localId in taskTimes }
        val timedTasks = withTime.map { TimelineEntry.Task(it, date.atTime(taskTimes.getValue(it.localId)).atZone(zone).toInstant()) }
        return TodayUiState(
            date = date,
            greeting = greetingFor(now.atZone(zone).toLocalTime()),
            firstName = firstName,
            overdue = overdue,
            allDayEvents = allDay.sortedWith(compareBy<EventEntity> { it.startDate }.thenBy { it.title }),
            allDayTasks = withoutTime,
            timeline = timeline(timed, now, timedTasks),
            completed = completed,
            loading = false,
            refreshing = refreshing,
        )
    }

    /** Timed events and timed tasks by start; the "now" line goes after everything that started. */
    fun timeline(timed: List<EventEntity>, now: Instant, tasks: List<TimelineEntry.Task> = emptyList()): List<TimelineEntry> {
        if (timed.isEmpty() && tasks.isEmpty()) return emptyList()
        val entries: List<Pair<Instant, TimelineEntry>> = timed.map { (it.startAt ?: Instant.MIN) to TimelineEntry.Event(it) } +
            tasks.map { it.at to it }
        val sorted = entries.sortedWith(compareBy<Pair<Instant, TimelineEntry>> { it.first }.thenBy {
            when (val e = it.second) { is TimelineEntry.Event -> e.event.title; is TimelineEntry.Task -> e.task.title; else -> "" }
        })
        val started = sorted.count { it.first <= now }
        return sorted.take(started).map { it.second } + TimelineEntry.Now(now) + sorted.drop(started).map { it.second }
    }
}
