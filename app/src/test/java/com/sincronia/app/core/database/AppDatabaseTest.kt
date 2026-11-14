package com.sincronia.app.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.sincronia.app.core.time.TaskDueDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDatabaseTest {
    private lateinit var db: AppDatabase
    private val day = LocalDate.of(2026, 9, 29)
    private val start = day.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant()
    private val end = day.plusDays(1).atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant()

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() { db.close() }

    @Test fun `day boundaries and account deletion filter`() = runBlocking {
        db.eventDao().upsert(listOf(
            EventEntity("all-day", "demo", "all-day", title = "One day", allDay = true,
                startDate = day, endDate = day.plusDays(1)),
            EventEntity("overnight", "demo", "overnight", title = "Overnight",
                startAt = start.minusSeconds(3600), endAt = start.plusSeconds(3600)),
            EventEntity("next-day", "demo", "next-day", title = "Tomorrow",
                startAt = end, endAt = end.plusSeconds(3600)),
            EventEntity("deleted", "demo", "deleted", title = "Deleted", allDay = true,
                startDate = day, endDate = day.plusDays(1), deleted = true),
            EventEntity("all-day", "another", "all-day", title = "Other", allDay = true,
                startDate = day, endDate = day.plusDays(1)),
        ))
        assertEquals(setOf("all-day", "overnight"), db.eventDao().observeDay("demo", day, start, end)
            .first().map { it.localId }.toSet())
        assertEquals(emptyList<EventEntity>(), db.eventDao().observeDay("demo", day.plusDays(1), end,
            day.plusDays(2).atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant()).first()
            .filter { it.localId == "all-day" })
        assertEquals("Other", db.eventDao().byGoogleId("another", "all-day")?.title)
        assertEquals("One day", db.eventDao().byGoogleId("demo", "all-day")?.title)
    }

    @Test fun `task due retains its date and lists are isolated`() = runBlocking {
        val due = TaskDueDate.parse("2026-09-29T00:00:00.000Z")
        assertEquals(day, due)
        assertEquals("2026-09-29T00:00:00.000Z", TaskDueDate.format(due))
        db.taskListDao().upsert(listOf(TaskListEntity("demo", "personal", "Pessoal"),
            TaskListEntity("other", "personal", "Outra")))
        db.taskDao().upsert(listOf(
            TaskEntity("one", "demo", "one", "personal", title = "Visible", dueDate = due),
            TaskEntity("two", "demo", "two", "personal", title = "Hidden", dueDate = due, deleted = true),
            TaskEntity("three", "other", "three", "personal", title = "Other", dueDate = due),
        ))
        assertEquals(listOf("one"), db.taskDao().observeDay("demo", day).first().map { it.localId })
        assertEquals(1, db.taskListDao().observeAll("demo").first().size)
    }

    @Test fun `today queries find overdue, completed today and multi-day all-day events`() = runBlocking {
        db.taskDao().upsert(listOf(
            TaskEntity("late", "demo", "late", "personal", title = "Late", dueDate = day.minusDays(2)),
            TaskEntity("today", "demo", "today", "personal", title = "Today", dueDate = day),
            TaskEntity("no-date", "demo", "no-date", "personal", title = "No date"),
            TaskEntity("late-done", "demo", "late-done", "personal", title = "Late done", dueDate = day.minusDays(1),
                status = "completed", completedAt = start.plusSeconds(60)),
            TaskEntity("done-yesterday", "demo", "done-yesterday", "personal", title = "Done yesterday",
                dueDate = day.minusDays(1), status = "completed", completedAt = start.minusSeconds(60)),
            TaskEntity("done-hidden", "demo", "done-hidden", "personal", title = "Hidden", dueDate = day,
                status = "completed", completedAt = start.plusSeconds(120), hidden = true),
            TaskEntity("late-deleted", "demo", "late-deleted", "personal", title = "Deleted",
                dueDate = day.minusDays(1), deleted = true),
            TaskEntity("late-other", "other", "late-other", "personal", title = "Other", dueDate = day.minusDays(1)),
        ))
        assertEquals(listOf("late"), db.taskDao().observeOverdue("demo", day).first().map { it.localId })
        assertEquals(listOf("late-done"), db.taskDao().observeCompletedBetween("demo", start, end).first()
            .map { it.localId })

        db.eventDao().upsert(listOf(EventEntity("trip", "demo", "trip", title = "Trip", allDay = true,
            startDate = day.minusDays(1), endDate = day.plusDays(2))))
        val nextDay = day.plusDays(2)
        assertEquals(listOf("trip"), db.eventDao().observeDay("demo", day, start, end).first().map { it.localId })
        assertEquals(emptyList<String>(), db.eventDao().observeDay("demo", nextDay,
            nextDay.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant(),
            nextDay.plusDays(1).atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant()).first().map { it.localId })
    }

    @Test fun `json lists and alarm outbox metadata persist`() = runBlocking {
        val event = EventEntity("id", "demo", "id", title = "Test", reminderMinutes = listOf(5, 10),
            startAt = Instant.parse("2026-09-29T12:00:00Z"), endAt = Instant.parse("2026-09-29T13:00:00Z"))
        db.eventDao().upsert(listOf(event))
        assertEquals(listOf(5, 10), db.eventDao().byGoogleId("demo", "id")?.reminderMinutes)
        db.outboxDao().upsert(OutboxEntity(accountId = "demo", itemType = "EVENT", itemLocalId = "id",
            operation = "UPDATE", changedFields = listOf("title", "description")))
        assertEquals(listOf("title", "description"), db.outboxDao().forItem("demo", "EVENT", "id")?.changedFields)
        val alarmId = db.alarmDao().upsert(AlarmEntity(accountId = "demo", itemType = "EVENT", itemLocalId = "id"))
        assertEquals(alarmId.toInt(), db.alarmDao().forItem("demo", "EVENT", "id")?.id)
        db.syncMetaDao().upsert(SyncMetaEntity("demo:calendar:primary:timeZone", "demo", "America/Sao_Paulo"))
        assertEquals("America/Sao_Paulo", db.syncMetaDao().value("demo", "demo:calendar:primary:timeZone"))
        assertNull(db.syncMetaDao().value("other", "demo:calendar:primary:timeZone"))
    }
}
