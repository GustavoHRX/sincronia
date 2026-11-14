package com.sincronia.app.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.OutboxEntity
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.core.log.AppLogger
import com.sincronia.app.core.network.AccessTokenProvider
import com.sincronia.app.core.network.CalendarApi
import com.sincronia.app.core.network.GoogleHttp
import com.sincronia.app.core.network.TasksApi
import com.sincronia.app.data.edit.EditRepository
import com.sincronia.app.data.edit.ItemType
import com.sincronia.app.data.edit.Outbox
import com.sincronia.app.data.edit.TaskField
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OutboxProcessorTest {
    private val a = "acc"
    private val server = MockWebServer()
    private val log = ConcurrentLinkedQueue<String>()
    /** "METHOD path" -> queued responses (code, body); the last one repeats. */
    private val script = mutableMapOf<String, MutableList<Pair<Int, String>>>()
    private lateinit var db: AppDatabase
    private lateinit var processor: OutboxProcessor
    private lateinit var edits: EditRepository
    private val now = Instant.parse("2026-09-29T12:00:00Z")

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val key = "${request.method} ${request.url.encodedPath}"
                log += key + (request.url.query?.let { "?$it" } ?: "") + " " + request.body?.utf8().orEmpty()
                val queue = script[key] ?: return MockResponse.Builder().code(500).build()
                val (code, body) = if (queue.size > 1) queue.removeAt(0) else queue.first()
                return MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()
            }
        }
        server.start()
        val tokens = object : AccessTokenProvider {
            override fun currentToken() = "t"
            override suspend fun refresh(rejected: String?) = "t"
        }
        val retrofit = GoogleHttp.retrofit(GoogleHttp.client(tokens), server.url("/").toString())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        processor = OutboxProcessor(db, retrofit.create(CalendarApi::class.java), retrofit.create(TasksApi::class.java),
            AppLogger(context))
        edits = EditRepository(db, Outbox(db)) {}
    }

    @After fun tearDown() {
        db.close()
        server.close()
    }

    private fun on(key: String, vararg responses: Pair<Int, String>) { script[key] = responses.toMutableList() }
    private val events = "/calendar/v3/calendars/primary/events"

    private suspend fun newEvent() = edits.createEvent(EventEntity("", a, null, title = "Reunião",
        startAt = Instant.parse("2026-09-29T13:00:00Z"), endAt = Instant.parse("2026-09-29T14:00:00Z"), timeZone = "UTC"))

    @Test fun `new event is inserted with the app id and becomes SYNCED`() = runBlocking {
        val event = newEvent()
        on("POST $events", 200 to """{"id":"${event.googleId}","etag":"e1","updated":"2026-09-29T12:00:01.000Z"}""")
        processor.process(a, now)
        assertTrue(log.single().contains("\"id\":\"${event.googleId}\""))
        val saved = db.eventDao().byLocalId(a, event.localId)!!
        assertEquals("SYNCED", saved.syncState)
        assertEquals("e1", saved.etag)
        assertNull(db.outboxDao().forItem(a, ItemType.EVENT, event.localId))
    }

    @Test fun `409 on a retried insert fetches the event and counts as created`() = runBlocking {
        val event = newEvent()
        on("POST $events", 409 to """{"error":{"code":409,"errors":[{"reason":"duplicate"}]}}""")
        on("GET $events/${event.googleId}", 200 to """{"id":"${event.googleId}","etag":"e2"}""")
        processor.process(a, now)
        assertEquals("SYNCED", db.eventDao().byLocalId(a, event.localId)!!.syncState)
        assertNull(db.outboxDao().forItem(a, ItemType.EVENT, event.localId))
    }

    @Test fun `PATCH 404 then GET 404 means it was deleted in Google`() = runBlocking {
        val event = EventEntity("e", a, "g1", title = "Velho", startAt = now, endAt = now.plusSeconds(3600))
        db.eventDao().upsert(listOf(event))
        edits.updateEvent(event, event.copy(title = "Novo"))
        on("PATCH $events/g1", 404 to "{}")
        on("GET $events/g1", 404 to "{}")
        processor.process(a, now)
        assertNull(db.eventDao().byLocalId(a, "e"))
        assertNull(db.outboxDao().forItem(a, ItemType.EVENT, "e"))
        assertEquals("""{"summary":"Novo"}""", log.first().substringAfter(" ").substringAfter(" "))
    }

    @Test fun `PATCH 404 while GET still finds it is a transient error`() = runBlocking {
        val event = EventEntity("e", a, "g1", title = "Velho", startAt = now, endAt = now.plusSeconds(3600))
        db.eventDao().upsert(listOf(event))
        edits.updateEvent(event, event.copy(title = "Novo"))
        on("PATCH $events/g1", 404 to "{}")
        on("GET $events/g1", 200 to """{"id":"g1","status":"confirmed"}""")
        processor.process(a, now)
        val row = db.outboxDao().forItem(a, ItemType.EVENT, "e")!!
        assertEquals("PENDING", row.state)
        assertEquals(now.plus(Duration.ofMinutes(1)), row.nextAttemptAt)
    }

    @Test fun `DELETE answered 410 is a success`() = runBlocking {
        val event = EventEntity("e", a, "g1", title = "X", startAt = now, endAt = now.plusSeconds(3600))
        db.eventDao().upsert(listOf(event))
        edits.deleteEvent(event)
        on("DELETE $events/g1", 410 to "{}")
        processor.process(a, now)
        assertNull(db.eventDao().byLocalId(a, "e"))
        assertNull(db.outboxDao().forItem(a, ItemType.EVENT, "e"))
    }

    @Test fun `transient errors back off 1, 5, 15, 60 min and the 5th attempt fails`() = runBlocking {
        val event = newEvent()
        on("POST $events", 503 to "{}")
        var time = now
        val waits = mutableListOf<Duration>()
        repeat(5) {
            processor.process(a, time)
            val row = db.outboxDao().forItem(a, ItemType.EVENT, event.localId)!!
            row.nextAttemptAt?.let { waits += Duration.between(time, it); time = it }
        }
        assertEquals(listOf(1L, 5L, 15L, 60L).map(Duration::ofMinutes), waits)
        assertEquals("FAILED", db.outboxDao().forItem(a, ItemType.EVENT, event.localId)!!.state)
        assertEquals("FAILED", db.eventDao().byLocalId(a, event.localId)!!.syncState)
    }

    @Test fun `final error fails at once without blocking the other items`() = runBlocking {
        val first = newEvent()
        val second = newEvent()
        on("POST $events", 400 to """{"error":{"code":400,"errors":[{"reason":"invalid"}]}}""",
            200 to """{"id":"${second.googleId}"}""")
        processor.process(a, now)
        val failed = listOf(first, second).map { db.outboxDao().forItem(a, ItemType.EVENT, it.localId) }
        assertEquals(1, failed.count { it?.state == "FAILED" })
        assertEquals("400 invalid", failed.firstNotNullOf { it }.lastError)
        assertEquals(1, failed.count { it == null })
    }

    private fun tasks(list: String) = "/tasks/v1/lists/$list/tasks"

    @Test fun `new task gets the Google id and the remote list`() = runBlocking {
        val task = edits.createTask(TaskEntity("", a, null, "l1", title = "Ler", dueDate = LocalDate.of(2026, 9, 29)))
        on("POST ${tasks("l1")}", 200 to """{"id":"gt","etag":"t1"}""")
        processor.process(a, now)
        val saved = db.taskDao().byLocalId(a, task.localId)!!
        assertEquals("gt", saved.googleId)
        assertEquals("l1", saved.remoteTaskListId)
        assertTrue(log.single().contains("\"due\":\"2026-09-29T00:00:00.000Z\""))
    }

    @Test fun `after a timeout the task already created is adopted instead of posted again`() = runBlocking {
        val task = edits.createTask(TaskEntity("", a, null, "l1", title = "Ler", dueDate = LocalDate.of(2026, 9, 29)))
        val row = db.outboxDao().forItem(a, ItemType.TASK, task.localId)!!
        db.outboxDao().upsert(row.copy(attempts = 1, lastError = OutboxProcessor.TIMEOUT, createdAt = now))
        on("GET ${tasks("l1")}", 200 to """{"items":[{"id":"other","title":"Ler","due":"2026-09-30T00:00:00.000Z"},
            {"id":"gt","title":"Ler","due":"2026-09-29T00:00:00.000Z"}]}""")
        processor.process(a, now)
        assertEquals("gt", db.taskDao().byLocalId(a, task.localId)!!.googleId)
        assertFalse(log.any { it.startsWith("POST") })
        assertTrue(log.single().contains("updatedMin=2026-09-29T11:58:00Z"))
    }

    @Test fun `changing list moves first and then patches on the new list`() = runBlocking {
        val task = TaskEntity("t", a, "gt", "l1", "l1", "Ler")
        db.taskDao().upsert(listOf(task))
        edits.updateTask(task, task.copy(taskListId = "l2", title = "Ler livro"))
        on("POST ${tasks("l1")}/gt/move", 200 to """{"id":"gt"}""")
        on("PATCH ${tasks("l2")}/gt", 200 to """{"id":"gt","etag":"t2"}""")
        processor.process(a, now)
        assertEquals(listOf("POST", "PATCH"), log.map { it.substringBefore(" ") })
        assertTrue(log.first().contains("destinationTasklist=l2"))
        assertTrue(log.last().endsWith("""{"title":"Ler livro"}"""))
        val saved = db.taskDao().byLocalId(a, "t")!!
        assertEquals("l2", saved.remoteTaskListId)
        assertEquals("SYNCED", saved.syncState)
    }

    @Test fun `an edit made while sending stays queued`() = runBlocking {
        val task = TaskEntity("t", a, "gt", "l1", "l1", "Ler")
        db.taskDao().upsert(listOf(task))
        edits.setCompleted(task, true)
        val entry = db.outboxDao().forItem(a, ItemType.TASK, "t")!!
        // Simulates the user editing again between the read and the answer.
        db.outboxDao().upsert(entry.copy(changedFields = listOf(TaskField.STATUS, TaskField.TITLE)))
        on("PATCH ${tasks("l1")}/gt", 200 to """{"id":"gt"}""")
        processor.process(a, now)
        // The row read by process() was the edited one, so this send already covers both fields.
        assertNull(db.outboxDao().forItem(a, ItemType.TASK, "t"))
        assertTrue(log.single().contains("\"title\":\"Ler\""))
    }

    @Test fun `retry and discard after a failure`() = runBlocking {
        val task = TaskEntity("t", a, "gt", "l1", "l1", "Ler")
        db.taskDao().upsert(listOf(task))
        edits.updateTask(task, task.copy(title = "Errado"))
        db.outboxDao().upsert(db.outboxDao().forItem(a, ItemType.TASK, "t")!!.copy(state = "FAILED", attempts = 5))
        processor.retry(a, ItemType.TASK, "t")
        assertEquals("PENDING", db.outboxDao().forItem(a, ItemType.TASK, "t")!!.state)
        on("GET ${tasks("l1")}/gt", 200 to """{"id":"gt","title":"Ler","status":"needsAction"}""")
        processor.discard(a, ItemType.TASK, "t")
        assertNull(db.outboxDao().forItem(a, ItemType.TASK, "t"))
        val restored = db.taskDao().byLocalId(a, "t")!!
        assertEquals("Ler", restored.title)
        assertEquals("SYNCED", restored.syncState)
    }

    @Test fun `task time goes up as the last line of the notes`() = runBlocking {
        val task = TaskEntity("t", a, "gt", "l1", "l1", "Pagar", notes = "Boleto", dueDate = LocalDate.of(2026, 9, 29))
        db.taskDao().upsert(listOf(task))
        edits.setTaskTime(task, java.time.LocalTime.of(14, 30))
        on("PATCH ${tasks("l1")}/gt", 200 to """{"id":"gt"}""")
        processor.process(a, now)
        assertTrue(log.single().endsWith("""{"notes":"Boleto\n⏰ 14:30"}"""))
        edits.setTaskTime(task, null)
        processor.process(a, now)
        assertTrue(log.last().endsWith("""{"notes":"Boleto"}"""))
        assertEquals("OFF", db.alarmDao().forItem(a, "TASK", "t")!!.mode)
    }
}
