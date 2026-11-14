package com.sincronia.app.data.edit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
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
class EditRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: EditRepository
    private var syncRequests = 0
    private val a = "acc"

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = EditRepository(db, Outbox(db)) { syncRequests++ }
    }

    @After fun tearDown() = db.close()

    @Test fun `new event gets a base32hex Google id and a CREATE`() = runBlocking {
        val event = repo.createEvent(EventEntity("", a, null, title = "Reunião",
            startAt = Instant.parse("2026-09-29T13:00:00Z"), endAt = Instant.parse("2026-09-29T14:00:00Z")))
        assertTrue(event.googleId!!.matches(Regex("[0-9a-v]{32}")))
        assertEquals(Operation.CREATE, db.outboxDao().forItem(a, ItemType.EVENT, event.localId)!!.operation)
        assertEquals("PENDING", db.eventDao().byLocalId(a, event.localId)!!.syncState)
        assertEquals(1, syncRequests)
    }

    @Test fun `editing only sends what changed, and completing is a status UPDATE`() = runBlocking {
        val task = TaskEntity("t", a, "g", "l", "l", "Ler", dueDate = LocalDate.of(2026, 9, 29))
        db.taskDao().upsert(listOf(task))
        repo.updateTask(task, task.copy(title = "Ler livro", taskListId = "l2"))
        repo.setCompleted(task.copy(title = "Ler livro", taskListId = "l2"), true)
        val entry = db.outboxDao().forItem(a, ItemType.TASK, "t")!!
        assertEquals(listOf(TaskField.TITLE, TaskField.LIST, TaskField.STATUS), entry.changedFields)
        assertEquals("completed", db.taskDao().byLocalId(a, "t")!!.status)
    }

    @Test fun `unchanged save does not enqueue`() = runBlocking {
        val task = TaskEntity("t", a, "g", "l", "l", "Ler")
        db.taskDao().upsert(listOf(task))
        repo.updateTask(task, task)
        assertNull(db.outboxDao().forItem(a, ItemType.TASK, "t"))
        assertEquals(0, syncRequests)
    }

    @Test fun `deleting a synced item hides it until the DELETE goes up`() = runBlocking {
        val task = TaskEntity("t", a, "g", "l", "l", "Ler")
        db.taskDao().upsert(listOf(task))
        repo.deleteTask(task)
        assertTrue(db.taskDao().byLocalId(a, "t")!!.deleted)
        assertEquals(Operation.DELETE, db.outboxDao().forItem(a, ItemType.TASK, "t")!!.operation)
    }
}
