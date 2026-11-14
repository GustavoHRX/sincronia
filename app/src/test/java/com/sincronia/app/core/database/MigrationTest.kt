package com.sincronia.app.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "migration-test.db"
    private var room: AppDatabase? = null

    @Before fun createVersion1() {
        context.deleteDatabase(dbName)
        val schema = Json.parseToJsonElement(
            File("schemas/com.sincronia.app.core.database.AppDatabase/1.json").readText(),
        ).jsonObject.getValue("database").jsonObject
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(dbName).apply { parentFile?.mkdirs() }, null)
        for (entity in schema.getValue("entities").jsonArray.map { it.jsonObject }) {
            val table = entity.getValue("tableName").jsonPrimitive.content
            db.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            entity["indices"]?.jsonArray?.forEach {
                db.execSQL(it.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            }
        }
        schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
        db.execSQL(
            "INSERT INTO events (localId, accountId, googleId, calendarId, title, allDay, startAt, endAt, status, " +
                "eventType, organizerSelf, guestsCanModify, reminderUseDefault, reminderMinutes, updatedAt, deleted, " +
                "syncState) VALUES " +
                "('whole', 'demo', 'whole', 'primary', 'Whole second', 0, '2026-09-29T12:00:00Z', " +
                "'2026-09-29T13:00:00Z', 'confirmed', 'default', 1, 0, 1, '[5,10]', '2026-09-29T11:00:00.123456Z', 0, " +
                "'SYNCED'), " +
                "('fraction', 'demo', 'fraction', 'primary', 'With fraction', 0, '2026-09-29T12:00:00.500Z', " +
                "'2026-09-29T13:00:00Z', 'confirmed', 'default', 1, 0, 1, '[]', '2026-09-29T11:00:00Z', 0, 'SYNCED'), " +
                "('all-day', 'demo', 'all-day', 'primary', 'All day', 1, NULL, NULL, 'confirmed', 'default', 1, 0, 1, " +
                "'[]', '2026-09-29T11:00:00Z', 0, 'SYNCED')",
        )
        db.execSQL("UPDATE events SET startDate = '2026-09-29', endDate = '2026-09-30' WHERE localId = 'all-day'")
        db.execSQL(
            "INSERT INTO tasks (localId, accountId, googleId, taskListId, title, dueDate, status, completedAt, hidden, " +
                "updatedAt, deleted, syncState) VALUES ('done', 'demo', 'done', 'personal', 'Done', '2026-09-29', " +
                "'completed', '2026-09-29T15:30:00.250Z', 0, '2026-09-29T15:30:01Z', 0, 'SYNCED')",
        )
        db.execSQL(
            "INSERT INTO task_lists (accountId, googleId, title, isDefault, remoteUpdated) VALUES " +
                "('demo', 'personal', 'Pessoal', 1, '2026-09-01T08:00:00Z')",
        )
        db.execSQL(
            "INSERT INTO alarms (accountId, itemType, itemLocalId, mode, triggerAt, scheduled, vibrate) VALUES " +
                "('demo', 'EVENT', 'whole', 'AUTO', '2026-09-29T11:50:00Z', 1, 1)",
        )
        db.execSQL(
            "INSERT INTO outbox (accountId, itemType, itemLocalId, operation, changedFields, state, attempts, " +
                "createdAt) VALUES ('demo', 'EVENT', 'whole', 'UPDATE', '[\"title\"]', 'PENDING', 0, " +
                "'2026-09-29T11:00:00Z')",
        )
        db.execSQL("INSERT INTO sync_meta (`key`, accountId, value) VALUES ('demo:k', 'demo', 'v')")
        db.version = 1
        db.close()
    }

    @After fun tearDown() {
        room?.close()
        context.deleteDatabase(dbName)
    }

    // Room validates the migrated schema against version 2 when it opens the file.
    @Test fun `migration 1 to 2 turns ISO text into epoch milliseconds and keeps the data`() = runBlocking {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2).allowMainThreadQueries().build().also { room = it }

        val whole = db.eventDao().byGoogleId("demo", "whole")!!
        assertEquals(Instant.parse("2026-09-29T12:00:00Z"), whole.startAt)
        assertEquals(Instant.parse("2026-09-29T13:00:00Z"), whole.endAt)
        assertEquals(Instant.parse("2026-09-29T11:00:00.123Z"), whole.updatedAt)
        assertEquals(listOf(5, 10), whole.reminderMinutes)
        assertEquals(Instant.parse("2026-09-29T12:00:00.500Z"), db.eventDao().byGoogleId("demo", "fraction")!!.startAt)
        val allDay = db.eventDao().byGoogleId("demo", "all-day")!!
        assertNull(allDay.startAt)
        assertEquals(LocalDate.of(2026, 9, 29), allDay.startDate)

        val task = db.taskDao().byGoogleId("demo", "done")!!
        assertEquals(Instant.parse("2026-09-29T15:30:00.250Z"), task.completedAt)
        assertEquals(LocalDate.of(2026, 9, 29), task.dueDate)
        assertEquals(Instant.parse("2026-09-01T08:00:00Z"), db.taskListDao().all("demo").single().remoteUpdated)
        assertEquals(Instant.parse("2026-09-29T11:50:00Z"), db.alarmDao().forItem("demo", "EVENT", "whole")!!.triggerAt)
        val entry = db.outboxDao().forItem("demo", "EVENT", "whole")!!
        assertEquals(Instant.parse("2026-09-29T11:00:00Z"), entry.createdAt)
        assertEquals(listOf("title"), entry.changedFields)
        assertEquals("v", db.syncMetaDao().value("demo", "demo:k"))

        db.openHelper.readableDatabase.query(
            "SELECT count(*) FROM events WHERE typeof(startAt) = 'text' OR typeof(updatedAt) = 'text'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
    }

    // The v1 limitation: as text, "12:00:00.500Z" sorted before "12:00:00Z".
    @Test fun `instants in the same second are ordered by time after the migration`() = runBlocking {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2).allowMainThreadQueries().build().also { room = it }
        val day = LocalDate.of(2026, 9, 29)
        val ordered = db.eventDao().observeDay(
            "demo", day, Instant.parse("2026-09-29T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z"),
        ).first().filter { !it.allDay }.map { it.localId }
        assertEquals(listOf("whole", "fraction"), ordered)
    }
}
