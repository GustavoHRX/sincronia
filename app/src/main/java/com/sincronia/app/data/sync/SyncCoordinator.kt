package com.sincronia.app.data.sync

import androidx.room.withTransaction
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.data.calendar.CalendarSource
import com.sincronia.app.data.demo.DemoPreferences
import com.sincronia.app.data.tasks.TaskSource
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** M1 pipeline: authorize -> outbox -> download -> alarms. Google transport and alarms arrive in M3/M5. */
@Singleton
class SyncCoordinator @Inject constructor(
    private val database: AppDatabase,
    private val calendar: CalendarSource,
    private val tasks: TaskSource,
    private val preferences: DemoPreferences,
) {
    private val mutex = Mutex()

    suspend fun run() = mutex.withLock {
        if (!preferences.enabled.first()) return@withLock
        val accountId = "demo"
        // 1. Demo authorization is skipped. 2. Outbox has no network processor until M4.
        // 3. Fetch complete snapshots first, then apply together; keep pending local edits.
        val today = LocalDate.now()
        val events = calendar.events(today, ZoneId.systemDefault())
        val lists = tasks.lists()
        val downloadedTasks = tasks.tasks(today)
        database.withTransaction {
            val outbox = database.outboxDao().blockingDownload(accountId)
            val pending = outbox.map { it.itemType to it.itemLocalId }.toSet()
            val eventIds = events.map { it.googleId }.toSet()
            val taskIds = downloadedTasks.map { it.googleId }.toSet()
            database.eventDao().upsert(events.filter { ("EVENT" to it.localId) !in pending })
            database.taskListDao().upsert(lists)
            database.taskDao().upsert(downloadedTasks.filter { ("TASK" to it.localId) !in pending })
            database.eventDao().synced(accountId).filter { it.googleId !in eventIds && ("EVENT" to it.localId) !in pending }
                .takeIf { it.isNotEmpty() }?.let { stale ->
                    database.eventDao().upsert(stale.map { it.copy(deleted = true) })
                }
            // The demo task source is a complete snapshot, unlike future incremental Google downloads.
            database.taskDao().all(accountId).filter { it.googleId !in taskIds && it.syncState == "SYNCED" && ("TASK" to it.localId) !in pending }
                .takeIf { it.isNotEmpty() }?.let { stale ->
                    database.taskDao().upsert(stale.map { it.copy(deleted = true) })
                }
        }
        // 4. Alarm rescheduling becomes effective in M5.
    }
}
