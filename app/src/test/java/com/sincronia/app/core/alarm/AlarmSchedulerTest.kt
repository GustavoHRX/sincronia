package com.sincronia.app.core.alarm

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sincronia.app.core.database.AlarmEntity
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.SyncMetaEntity
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.data.settings.SettingsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmSchedulerTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = Instant.parse("2026-09-29T12:00:00Z") // 09:00 local
    private val a = "acc"
    private lateinit var db: AppDatabase
    private lateinit var scheduler: AlarmScheduler
    private val scheduled = mutableMapOf<Int, Instant>()
    private val calls = mutableListOf<String>()
    private val lateNotices = mutableListOf<String>()

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val clock = object : AlarmClock {
            override fun schedule(alarmId: Int, trigger: Instant, itemType: String, itemLocalId: String): Boolean {
                calls += "schedule $alarmId"; scheduled[alarmId] = trigger; return true
            }
            override fun cancel(alarmId: Int) { calls += "cancel $alarmId"; scheduled.remove(alarmId) }
            override fun scheduleDailySummary(trigger: Instant?) {}
        }
        val notices = object : AlarmNotices {
            override fun late(alarmId: Int, itemType: String, itemLocalId: String) { lateNotices += itemLocalId }
        }
        scheduler = AlarmScheduler(db, clock, notices, SettingsRepository(context))
        runBlocking { db.syncMetaDao().upsert(SyncMetaEntity("$a:calendar:primary:defaultReminders", a, "[10]")) }
    }

    @After fun tearDown() = db.close()

    private fun event(id: String, startLocal: String) = EventEntity(id, a, "g$id", title = id,
        startAt = LocalDate.of(2026, 9, 29).atTime(LocalTime.parse(startLocal)).atZone(zone).toInstant(),
        endAt = LocalDate.of(2026, 9, 29).atTime(LocalTime.parse(startLocal)).plusHours(1).atZone(zone).toInstant())

    @Test fun `3 alarms get 3 distinct request codes and running twice does not duplicate`() = runBlocking {
        db.eventDao().upsert(listOf(event("a", "10:00"), event("b", "11:00")))
        db.taskDao().upsert(listOf(TaskEntity("t", a, "gt", "l", title = "Pagar", dueDate = LocalDate.of(2026, 9, 29))))
        db.alarmDao().upsert(AlarmEntity(accountId = a, itemType = "TASK", itemLocalId = "t", mode = AlarmMode.TIME,
            localTime = LocalTime.of(15, 0)))
        scheduler.rescheduleAll(a, now, zone)
        assertEquals(3, scheduled.size)
        assertEquals(Instant.parse("2026-09-29T12:50:00Z"), scheduled.values.min())
        val first = calls.toList()
        scheduler.rescheduleAll(a, now, zone)
        assertEquals(first, calls)
        assertEquals(3, db.alarmDao().scheduled().size)
    }

    @Test fun `an alarm that left the window is cancelled and a changed one is rescheduled`() = runBlocking {
        db.eventDao().upsert(listOf(event("a", "10:00"), event("b", "11:00")))
        scheduler.rescheduleAll(a, now, zone)
        val idA = db.alarmDao().forItem(a, "EVENT", "a")!!.id
        db.eventDao().upsert(listOf(event("a", "10:00").copy(deleted = true), event("b", "12:00")))
        scheduler.rescheduleAll(a, now, zone)
        assertTrue("cancel $idA" in calls)
        assertEquals(listOf(Instant.parse("2026-09-29T14:50:00Z")), scheduled.values.toList())
    }

    @Test fun `first sync at 15h with past events and tasks of today fires nothing`() = runBlocking {
        db.eventDao().upsert(listOf(event("a", "10:00"), event("b", "14:00")))
        db.taskDao().upsert(listOf(TaskEntity("t", a, "gt", "l", title = "Pagar", dueDate = LocalDate.of(2026, 9, 29))))
        scheduler.rescheduleAll(a, Instant.parse("2026-09-29T18:00:00Z"), zone)
        assertTrue(scheduled.isEmpty())
        assertTrue(lateNotices.isEmpty())
    }

    @Test fun `alarm already passed for an event not yet started gives one notice`() = runBlocking {
        db.eventDao().upsert(listOf(event("soon", "09:05")))
        scheduler.rescheduleAll(a, now, zone)
        scheduler.rescheduleAll(a, now, zone)
        assertEquals(listOf("soon"), lateNotices)
        assertTrue(scheduled.isEmpty())
    }

    @Test fun `no more than 50 alarms in AlarmManager`() = runBlocking {
        db.eventDao().upsert((0 until 60).map { i ->
            event("e$i", "10:00").copy(startAt = now.plusSeconds(3600L * (i + 1)), endAt = now.plusSeconds(3600L * (i + 2)))
        })
        scheduler.rescheduleAll(a, now, zone)
        assertEquals(50, scheduled.size)
        scheduler.cancelAll()
        assertTrue(scheduled.isEmpty())
    }

    @Test fun `turning a series off cancels its occurrences, also future ones, and on brings them back`() = runBlocking {
        db.eventDao().upsert(listOf(event("s1", "10:00").copy(recurringEventId = "serie"),
            event("s2", "11:00").copy(recurringEventId = "serie"), event("other", "13:00")))
        scheduler.rescheduleAll(a, now, zone)
        assertEquals(3, scheduled.size)
        val key = "$a:${AlarmPlanner.SERIES_OFF_PREFIX}serie"
        db.syncMetaDao().upsert(SyncMetaEntity(key, a, "OFF"))
        // A new occurrence arriving in a later sync is silent too.
        db.eventDao().upsert(listOf(event("s3", "12:00").copy(recurringEventId = "serie")))
        scheduler.rescheduleAll(a, now, zone)
        assertEquals(listOf(Instant.parse("2026-09-29T15:50:00Z")), scheduled.values.toList())
        db.syncMetaDao().delete(a, key)
        scheduler.rescheduleAll(a, now, zone)
        assertEquals(4, scheduled.size)
    }

    @Test fun `after a reboot every alarm is registered again`() = runBlocking {
        db.eventDao().upsert(listOf(event("a", "10:00"), event("b", "11:00")))
        scheduler.rescheduleAll(a, now, zone)
        assertEquals(2, scheduled.size)
        scheduled.clear() // the reboot empties AlarmManager; the table still says "scheduled"
        scheduler.rescheduleAll(a, now, zone)
        assertTrue("without the boot step nothing is registered again", scheduled.isEmpty())
        scheduler.systemAlarmsCleared()
        scheduler.rescheduleAll(a, now, zone)
        assertEquals(2, scheduled.size)
    }
}
