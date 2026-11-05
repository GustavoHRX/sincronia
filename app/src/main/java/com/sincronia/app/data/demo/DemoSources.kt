package com.sincronia.app.data.demo

import android.content.Context
import com.sincronia.app.R
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.core.database.TaskListEntity
import com.sincronia.app.data.calendar.CalendarSource
import com.sincronia.app.data.tasks.TaskSource
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import dagger.hilt.android.qualifiers.ApplicationContext

class FakeCalendarSource(@param:ApplicationContext private val context: Context) : CalendarSource {
    override suspend fun events(today: LocalDate, zone: ZoneId): List<EventEntity> {
        fun at(date: LocalDate, time: LocalTime) = date.atTime(time).atZone(zone).toInstant()
        return listOf(
            EventEntity("demo-meeting", "demo", "demo-meeting", title = context.getString(R.string.demo_meeting),
                startAt = at(today, LocalTime.of(10, 0)), endAt = at(today, LocalTime.of(11, 0))),
            EventEntity("demo-dinner", "demo", "demo-dinner", title = context.getString(R.string.demo_dinner),
                startAt = at(today.plusDays(2), LocalTime.of(19, 30)),
                endAt = at(today.plusDays(2), LocalTime.of(21, 0))),
            EventEntity("demo-all-day", "demo", "demo-all-day", title = context.getString(R.string.demo_all_day),
                allDay = true, startDate = today, endDate = today.plusDays(1)),
            EventEntity("demo-exams", "demo", "demo-exams", title = context.getString(R.string.demo_exams),
                allDay = true, startDate = today.minusDays(1), endDate = today.plusDays(2)),
            EventEntity("demo-overnight", "demo", "demo-overnight", title = context.getString(R.string.demo_overnight),
                startAt = at(today.minusDays(1), LocalTime.of(22, 0)),
                endAt = at(today, LocalTime.of(1, 0))),
            EventEntity("demo-recurring", "demo", "demo-recurring", title = context.getString(R.string.demo_recurring),
                recurringEventId = "demo-series", originalStartTime = at(today.plusDays(1), LocalTime.of(9, 0)).toString(),
                startAt = at(today.plusDays(1), LocalTime.of(9, 0)),
                endAt = at(today.plusDays(1), LocalTime.of(9, 30))),
        )
    }
}

class FakeTaskSource(@param:ApplicationContext private val context: Context) : TaskSource {
    override suspend fun lists() = listOf(
        TaskListEntity("demo", "personal", context.getString(R.string.demo_personal), isDefault = true),
        TaskListEntity("demo", "work", context.getString(R.string.demo_work)),
    )

    override suspend fun tasks(today: LocalDate) = listOf(
        TaskEntity("demo-shopping", "demo", "demo-shopping", "personal", "personal", context.getString(R.string.demo_shopping), dueDate = today),
        TaskEntity("demo-milk", "demo", "demo-milk", "personal", "personal", context.getString(R.string.demo_milk), dueDate = today,
            parentId = "demo-shopping", position = "0001"),
        TaskEntity("demo-late", "demo", "demo-late", "personal", "personal", context.getString(R.string.demo_late), dueDate = today.minusDays(1)),
        TaskEntity("demo-complete", "demo", "demo-complete", "personal", "personal", context.getString(R.string.demo_complete),
            dueDate = today, status = "completed", completedAt = today.atStartOfDay(ZoneId.systemDefault()).toInstant()),
        TaskEntity("demo-report", "demo", "demo-report", "work", "work", context.getString(R.string.demo_report), dueDate = today.plusDays(2)),
    )
}
