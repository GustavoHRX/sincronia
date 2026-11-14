package com.sincronia.app.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.OutboxEntity
import com.sincronia.app.core.network.AccessTokenProvider
import com.sincronia.app.core.network.CalendarApi
import com.sincronia.app.core.network.GoogleHttp
import com.sincronia.app.core.network.TasksApi
import com.sincronia.app.data.calendar.GoogleCalendarSource
import com.sincronia.app.data.tasks.GoogleTaskSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GoogleSyncTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.of(2026, 9, 29)
    private val account = "sub-1"
    private val server = MockWebServer()
    private val requests = CopyOnWriteArrayList<RecordedRequest>()
    /** Path (with query for paging) -> JSON body; missing paths answer an empty list. */
    private val routes = mutableMapOf<String, String>()
    private lateinit var db: AppDatabase
    private lateinit var sync: GoogleSync

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val url = request.url
                val key = url.encodedPath + (url.queryParameter("pageToken")?.let { "#$it" } ?: "")
                val body = routes[key] ?: if (url.encodedPath.endsWith("@default")) """{"id":"l1"}""" else "{}"
                return MockResponse.Builder().addHeader("Content-Type", "application/json").body(body).build()
            }
        }
        server.start()
        val tokens = object : AccessTokenProvider {
            override fun currentToken() = "t"
            override suspend fun refresh(rejected: String?) = "t"
        }
        val retrofit = GoogleHttp.retrofit(GoogleHttp.client(tokens), server.url("/").toString())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        sync = GoogleSync(db, GoogleCalendarSource(retrofit.create(CalendarApi::class.java)),
            GoogleTaskSource(retrofit.create(TasksApi::class.java)))
    }

    @After fun tearDown() {
        db.close()
        server.close()
    }

    private val eventsPath = "/calendar/v3/calendars/primary/events"
    private fun event(id: String, start: String, end: String) =
        """{"id":"$id","summary":"Evento $id","start":{"dateTime":"$start"},"end":{"dateTime":"$end"},"updated":"2026-09-01T00:00:00.000Z"}"""

    @Test fun `calendar window is -7 to +61 days and every page is read`() = runBlocking {
        routes[eventsPath] = """{"timeZone":"America/Sao_Paulo","defaultReminders":[{"method":"popup","minutes":10},
            {"method":"email","minutes":60}],"items":[${event("a", "2026-09-29T10:00:00-03:00", "2026-09-29T11:00:00-03:00")}],
            "nextPageToken":"2"}"""
        routes["$eventsPath#2"] = """{"items":[${event("b", "2026-09-30T10:00:00-03:00", "2026-09-30T11:00:00-03:00")}]}"""
        sync.downloadCalendar(account, today, zone)

        val first = requests.first().url
        assertEquals("2026-09-22T03:00:00Z", first.queryParameter("timeMin"))
        assertEquals("2026-11-29T03:00:00Z", first.queryParameter("timeMax"))
        assertEquals(2, db.eventDao().all(account).size)
        assertEquals("America/Sao_Paulo", db.syncMetaDao().value(account, "$account:calendar:primary:timeZone"))
        assertEquals("[10]", db.syncMetaDao().value(account, "$account:calendar:primary:defaultReminders"))
    }

    @Test fun `61 days after the first sync an event on day +62 arrives`() = runBlocking {
        routes[eventsPath] = """{"items":[]}"""
        sync.downloadCalendar(account, today, zone)
        val later = today.plusDays(61)
        routes[eventsPath] = """{"items":[${event("far", "2026-12-01T10:00:00-03:00", "2026-12-01T11:00:00-03:00")}]}"""
        sync.downloadCalendar(account, later, zone)

        assertTrue(Instant.parse(requests.last().url.queryParameter("timeMax")) > Instant.parse("2026-12-01T13:00:00Z"))
        assertEquals("Evento far", db.eventDao().byGoogleId(account, "far")!!.title)
    }

    @Test fun `event deleted in Google disappears and the local id is kept for the others`() = runBlocking {
        routes[eventsPath] = """{"items":[${event("keep", "2026-09-29T10:00:00-03:00", "2026-09-29T11:00:00-03:00")},
            ${event("gone", "2026-09-29T12:00:00-03:00", "2026-09-29T13:00:00-03:00")}]}"""
        sync.downloadCalendar(account, today, zone)
        val keepId = db.eventDao().byGoogleId(account, "keep")!!.localId
        routes[eventsPath] = """{"items":[${event("keep", "2026-09-29T10:00:00-03:00", "2026-09-29T11:30:00-03:00")}]}"""
        sync.downloadCalendar(account, today, zone)

        assertNull(db.eventDao().byGoogleId(account, "gone"))
        val keep = db.eventDao().byGoogleId(account, "keep")!!
        assertEquals(keepId, keep.localId)
        assertEquals(Instant.parse("2026-09-29T14:30:00Z"), keep.endAt)
    }

    @Test fun `a pending local edit is neither overwritten nor deleted`() = runBlocking {
        db.eventDao().upsert(listOf(EventEntity("local-1", account, "edited", title = "Meu título",
            startAt = Instant.parse("2026-09-29T13:00:00Z"), endAt = Instant.parse("2026-09-29T14:00:00Z"),
            syncState = "PENDING")))
        db.eventDao().upsert(listOf(EventEntity("local-2", account, "missing", title = "Na fila",
            startAt = Instant.parse("2026-09-29T15:00:00Z"), endAt = Instant.parse("2026-09-29T16:00:00Z"))))
        db.outboxDao().upsert(OutboxEntity(accountId = account, itemType = "EVENT", itemLocalId = "local-2",
            operation = "UPDATE", createdAt = Instant.now()))
        routes[eventsPath] = """{"items":[${event("edited", "2026-09-29T10:00:00-03:00", "2026-09-29T11:00:00-03:00")}]}"""
        sync.downloadCalendar(account, today, zone)

        assertEquals("Meu título", db.eventDao().byGoogleId(account, "edited")!!.title)
        assertEquals("Na fila", db.eventDao().byGoogleId(account, "missing")!!.title)
    }

    @Test fun `all-day event on 29-09 does not show on 30-09`() = runBlocking {
        routes[eventsPath] = """{"items":[{"id":"d","summary":"Feriado","start":{"date":"2026-09-29"},"end":{"date":"2026-09-30"}}]}"""
        sync.downloadCalendar(account, today, zone)
        fun day(date: LocalDate) = db.eventDao().observeDay(account, date, date.atStartOfDay(zone).toInstant(),
            date.plusDays(1).atStartOfDay(zone).toInstant())
        assertEquals(1, day(today).first().size)
        assertTrue(day(today.plusDays(1)).first().isEmpty())
    }

    private val listsPath = "/tasks/v1/users/@me/lists"
    private fun tasksPath(list: String) = "/tasks/v1/lists/$list/tasks"
    private fun task(id: String, extra: String = "") =
        """{"id":"$id","title":"Tarefa $id","status":"needsAction","updated":"2026-09-29T12:00:00.000Z"$extra}"""

    @Test fun `3 pages of 100 tasks are all read`() = runBlocking {
        routes[listsPath] = """{"items":[{"id":"l1","title":"Pessoal"}]}"""
        routes[tasksPath("l1")] = """{"items":[${(1..100).joinToString { task("a$it") }}],"nextPageToken":"2"}"""
        routes[tasksPath("l1") + "#2"] = """{"items":[${(1..100).joinToString { task("b$it") }}],"nextPageToken":"3"}"""
        routes[tasksPath("l1") + "#3"] = """{"items":[${(1..100).joinToString { task("c$it") }}]}"""
        sync.downloadTasks(account)

        assertEquals(300, db.taskDao().all(account).size)
        val taskRequests = requests.filter { it.url.encodedPath == tasksPath("l1") }
        assertEquals(3, taskRequests.size)
        assertTrue(taskRequests.all { it.url.queryParameter("maxResults") == "100" })
        assertTrue(db.taskListDao().all(account).single().isDefault)
    }

    @Test fun `due at UTC midnight stays on 29-09 in Sao Paulo`() = runBlocking {
        routes[listsPath] = """{"items":[{"id":"l1","title":"Pessoal"}]}"""
        routes[tasksPath("l1")] = """{"items":[${task("due", ",\"due\":\"2026-09-29T00:00:00.000Z\"")}]}"""
        sync.downloadTasks(account)
        assertEquals(LocalDate.of(2026, 9, 29), db.taskDao().byGoogleId(account, "due")!!.dueDate)
    }

    @Test fun `next sync sends updatedMin 2 minutes before the newest update and applies deletions`() = runBlocking {
        routes[listsPath] = """{"items":[{"id":"l1","title":"Pessoal"}]}"""
        routes[tasksPath("l1")] = """{"items":[${task("p")},${task("c", ",\"parent\":\"p\"")}]}"""
        sync.downloadTasks(account)
        assertNull(requests.last { it.url.encodedPath == tasksPath("l1") }.url.queryParameter("updatedMin"))
        val parent = db.taskDao().byGoogleId(account, "p")!!
        assertEquals(parent.localId, db.taskDao().byGoogleId(account, "c")!!.parentId)

        routes[tasksPath("l1")] = """{"items":[{"id":"c","deleted":true,"updated":"2026-09-29T13:00:00.000Z"}]}"""
        sync.downloadTasks(account)
        assertEquals("2026-09-29T11:58:00Z",
            requests.last { it.url.encodedPath == tasksPath("l1") }.url.queryParameter("updatedMin"))
        assertNull(db.taskDao().byGoogleId(account, "c"))
        assertEquals("2026-09-29T13:00:00Z", db.syncMetaDao().value(account, "$account:tasks:l1:updatedMin"))
    }

    @Test fun `a list that vanished takes its tasks and a moved task changes list`() = runBlocking {
        routes[listsPath] = """{"items":[{"id":"l1","title":"Pessoal"},{"id":"l2","title":"Trabalho"}]}"""
        routes[tasksPath("l1")] = """{"items":[${task("move")}]}"""
        routes[tasksPath("l2")] = """{"items":[${task("old")}]}"""
        sync.downloadTasks(account)
        routes[listsPath] = """{"items":[{"id":"l1","title":"Pessoal"},{"id":"l3","title":"Novo"}]}"""
        routes[tasksPath("l1")] = """{"items":[]}"""
        routes[tasksPath("l3")] = """{"items":[${task("move")}]}"""
        sync.downloadTasks(account)

        assertEquals(setOf("l1", "l3"), db.taskListDao().all(account).map { it.googleId }.toSet())
        assertNull(db.taskDao().byGoogleId(account, "old"))
        assertEquals("l3", db.taskDao().byGoogleId(account, "move")!!.taskListId)
        assertEquals(1, db.taskDao().all(account).size)
    }

    @Test fun `mark in the notes creates the alarm once and is stripped from Room`() = runBlocking {
        routes[listsPath] = """{"items":[{"id":"l1","title":"Pessoal"}]}"""
        routes[tasksPath("l1")] = """{"items":[${task("m", ",\"notes\":\"Boleto\\n⏰ 14:30\",\"due\":\"2026-09-29T00:00:00.000Z\"")}]}"""
        sync.downloadTasks(account)
        val saved = db.taskDao().byGoogleId(account, "m")!!
        assertEquals("Boleto", saved.notes)
        val alarm = db.alarmDao().forItem(account, "TASK", saved.localId)!!
        assertEquals(java.time.LocalTime.of(14, 30), alarm.localTime)
        // Turned off in the app: a later download with the mark does not bring it back.
        db.alarmDao().upsert(alarm.copy(mode = "OFF"))
        sync.downloadTasks(account)
        assertEquals("OFF", db.alarmDao().forItem(account, "TASK", saved.localId)!!.mode)
    }
}
