package com.sincronia.app.core.network

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoogleApisTest {
    private val server = MockWebServer()
    private val tokens = object : AccessTokenProvider {
        var token: String? = "old"
        val rejected = mutableListOf<String?>()
        var next: String? = "new"
        override fun currentToken() = token
        override suspend fun refresh(rejected: String?): String? {
            this.rejected += rejected
            token = next
            return next
        }
    }
    private lateinit var calendar: CalendarApi
    private lateinit var tasks: TasksApi

    @Before fun setUp() {
        server.start()
        val retrofit = GoogleHttp.retrofit(GoogleHttp.client(tokens), server.url("/").toString())
        calendar = retrofit.create(CalendarApi::class.java)
        tasks = retrofit.create(TasksApi::class.java)
    }

    @After fun tearDown() = server.close()

    private fun json(body: String, code: Int = 200) = MockResponse.Builder().code(code)
        .addHeader("Content-Type", "application/json").body(body).build()

    @Test fun `events request has the window, single events and the bearer token`() = runBlocking {
        server.enqueue(json("""{"timeZone":"America/Sao_Paulo","defaultReminders":[{"method":"popup","minutes":10}],
            "items":[{"id":"e1","summary":"Aula","start":{"dateTime":"2026-09-29T10:00:00-03:00"},
            "end":{"dateTime":"2026-09-29T11:00:00-03:00"},"unknownField":1}],"nextPageToken":"p2"}"""))
        val page = calendar.events("2026-09-22T03:00:00Z", "2026-11-29T03:00:00Z")
        val request = server.takeRequest()
        assertEquals("/calendar/v3/calendars/primary/events", request.url.encodedPath)
        assertEquals("true", request.url.queryParameter("singleEvents"))
        assertEquals("250", request.url.queryParameter("maxResults"))
        assertEquals("2026-09-22T03:00:00Z", request.url.queryParameter("timeMin"))
        assertNull(request.url.queryParameter("pageToken"))
        assertEquals("Bearer old", request.headers["Authorization"])
        assertEquals("p2", page.nextPageToken)
        assertEquals("America/Sao_Paulo", page.timeZone)
        assertEquals(10, page.defaultReminders.single().minutes)
        assertEquals("Aula", page.items.single().summary)
    }

    @Test fun `tasks request asks for 100 items including completed hidden and deleted`() = runBlocking {
        server.enqueue(json("""{"items":[{"id":"t1","title":"Ler","due":"2026-09-29T00:00:00.000Z","deleted":true}]}"""))
        val page = tasks.tasks("list 1", updatedMin = "2026-09-29T12:00:00.000Z", pageToken = "p3")
        val url = server.takeRequest().url
        assertEquals("/tasks/v1/lists/list%201/tasks", url.encodedPath)
        assertEquals("100", url.queryParameter("maxResults"))
        assertEquals("true", url.queryParameter("showCompleted"))
        assertEquals("true", url.queryParameter("showHidden"))
        assertEquals("true", url.queryParameter("showDeleted"))
        assertEquals("p3", url.queryParameter("pageToken"))
        assertEquals("2026-09-29T12:00:00.000Z", url.queryParameter("updatedMin"))
        assertTrue(page.items.single().deleted)
    }

    @Test fun `401 clears the token and retries once with a fresh one`() = runBlocking {
        server.enqueue(json("""{"error":{"code":401}}""", 401))
        server.enqueue(json("""{"items":[{"id":"l1","title":"Pessoal"}]}"""))
        val lists = tasks.lists()
        assertEquals("Bearer old", server.takeRequest().headers["Authorization"])
        assertEquals("Bearer new", server.takeRequest().headers["Authorization"])
        assertEquals(listOf<String?>("old"), tokens.rejected)
        assertEquals("Pessoal", lists.items.single().title)
    }

    @Test fun `second 401 is not retried again`() {
        server.enqueue(json("""{"error":{"code":401}}""", 401))
        server.enqueue(json("""{"error":{"code":401}}""", 401))
        val failure = runCatching { runBlocking { tasks.defaultList() } }.exceptionOrNull()
        assertTrue(failure is retrofit2.HttpException)
        assertEquals(401, (failure as retrofit2.HttpException).code())
        assertEquals(2, server.requestCount)
    }

    @Test fun `error reason is read from the body`() {
        val error = googleJson.decodeFromString(GoogleErrorDto.serializer(),
            """{"error":{"code":403,"errors":[{"reason":"rateLimitExceeded"}]}}""")
        assertEquals("rateLimitExceeded", error.reason)
    }
}
