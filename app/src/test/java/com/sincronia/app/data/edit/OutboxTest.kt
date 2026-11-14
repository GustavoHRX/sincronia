package com.sincronia.app.data.edit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.database.TaskEntity
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
class OutboxTest {
    private lateinit var db: AppDatabase
    private lateinit var outbox: Outbox
    private val a = "acc"

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        outbox = Outbox(db)
    }

    @After fun tearDown() = db.close()

    private suspend fun row(id: String) = db.outboxDao().forItem(a, ItemType.TASK, id)

    @Test fun `CREATE + UPDATE stays one CREATE`() = runBlocking {
        outbox.enqueue(a, ItemType.TASK, "t", Operation.CREATE)
        assertEquals(Enqueued.CREATE, outbox.enqueue(a, ItemType.TASK, "t", Operation.UPDATE, listOf("title")))
        assertEquals(Operation.CREATE, row("t")!!.operation)
        assertEquals(1, db.outboxDao().blockingDownload(a).size)
    }

    @Test fun `CREATE + DELETE removes the row and the local item without network`() = runBlocking {
        db.taskDao().upsert(listOf(TaskEntity("t", a, null, "l", title = "Nova", syncState = "PENDING")))
        outbox.enqueue(a, ItemType.TASK, "t", Operation.CREATE)
        assertEquals(Enqueued.NONE, outbox.enqueue(a, ItemType.TASK, "t", Operation.DELETE))
        assertNull(row("t"))
        assertNull(db.taskDao().byLocalId(a, "t"))
    }

    @Test fun `UPDATE + UPDATE joins the changed fields`() = runBlocking {
        outbox.enqueue(a, ItemType.TASK, "t", Operation.UPDATE, listOf("title"))
        outbox.enqueue(a, ItemType.TASK, "t", Operation.UPDATE, listOf("status", "title"))
        assertEquals(listOf("title", "status"), row("t")!!.changedFields)
    }

    @Test fun `UPDATE + DELETE becomes DELETE and later edits keep it`() = runBlocking {
        outbox.enqueue(a, ItemType.TASK, "t", Operation.UPDATE, listOf("title"))
        assertEquals(Enqueued.DELETE, outbox.enqueue(a, ItemType.TASK, "t", Operation.DELETE))
        assertEquals(Enqueued.DELETE, outbox.enqueue(a, ItemType.TASK, "t", Operation.UPDATE, listOf("notes")))
        assertEquals(Operation.DELETE, row("t")!!.operation)
        assertEquals(emptyList<String>(), row("t")!!.changedFields)
    }

    @Test fun `a new edit on a FAILED row resets it to PENDING`() = runBlocking {
        outbox.enqueue(a, ItemType.TASK, "t", Operation.UPDATE, listOf("title"))
        db.outboxDao().upsert(row("t")!!.copy(state = "FAILED", attempts = 5, lastError = "400"))
        outbox.enqueue(a, ItemType.TASK, "t", Operation.UPDATE, listOf("notes"))
        val entry = row("t")!!
        assertEquals("PENDING", entry.state)
        assertEquals(0, entry.attempts)
    }
}
