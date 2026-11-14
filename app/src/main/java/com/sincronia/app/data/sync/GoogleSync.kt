package com.sincronia.app.data.sync

import androidx.room.withTransaction
import com.sincronia.app.core.alarm.AlarmMode
import com.sincronia.app.core.database.AlarmEntity
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.database.SyncMetaEntity
import com.sincronia.app.data.calendar.GoogleCalendarSource
import com.sincronia.app.data.tasks.GoogleTaskSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Step 3 of spec 7.2 for a Google account. Everything is downloaded before it is applied, and a
 * local change that is still pending (outbox, PENDING or FAILED) is never overwritten or deleted.
 * Alarm cancellation for deleted items arrives in M5.
 */
class GoogleSync @Inject constructor(
    private val database: AppDatabase,
    private val calendar: GoogleCalendarSource,
    private val tasks: GoogleTaskSource,
) {
    suspend fun download(accountId: String, today: LocalDate, zone: ZoneId) {
        downloadCalendar(accountId, today, zone)
        downloadTasks(accountId)
    }

    suspend fun downloadCalendar(accountId: String, today: LocalDate, zone: ZoneId) {
        val window = calendar.window(accountId, today, zone)
        database.withTransaction {
            val blocked = blockedLocalIds(accountId, "EVENT")
            val existing = database.eventDao().all(accountId).filter { it.googleId != null }.associateBy { it.googleId }
            val save = window.events.mapNotNull { event ->
                val known = existing[event.googleId]
                if (known != null && (known.localId in blocked || known.syncState != SYNCED)) null
                else event.copy(localId = known?.localId ?: UUID.randomUUID().toString())
            }
            database.eventDao().upsert(save)
            val received = window.events.mapNotNull { it.googleId }.toSet()
            val stale = existing.values.filter {
                it.syncState == SYNCED && it.googleId !in received && it.localId !in blocked
            }
            if (stale.isNotEmpty()) database.eventDao().deleteLocal(accountId, stale.map { it.localId })
            window.timeZone?.let { meta(accountId, "calendar:primary:timeZone", it) }
            meta(accountId, "calendar:primary:defaultReminders",
                Json.encodeToString(ListSerializer(Int.serializer()), window.defaultReminders))
        }
    }

    suspend fun downloadTasks(accountId: String) {
        val lists = tasks.lists(accountId)
        database.withTransaction {
            database.taskListDao().upsert(lists)
            val received = lists.map { it.googleId }.toSet()
            // A list that vanished from the complete listing takes its tasks with it.
            database.taskListDao().all(accountId).filter { it.googleId !in received }.forEach { gone ->
                database.taskDao().deleteList(accountId, gone.googleId)
                database.taskListDao().delete(accountId, gone.googleId)
            }
        }
        for (list in lists) {
            val key = "tasks:${list.googleId}:updatedMin"
            val last = database.syncMetaDao().value(accountId, "$accountId:$key")?.let(Instant::parse)
            // Google's clock minus 2 minutes, never the phone's clock.
            val download = tasks.tasks(accountId, list.googleId, last?.minusSeconds(UPDATED_MIN_MARGIN_SECONDS))
            database.withTransaction {
                val blocked = blockedLocalIds(accountId, "TASK")
                val existing = database.taskDao().all(accountId).filter { it.googleId != null }.associateBy { it.googleId }
                val accepted = download.tasks.mapNotNull { task ->
                    val known = existing[task.googleId]
                    if (known != null && (known.localId in blocked || known.syncState != SYNCED)) null
                    else task.copy(localId = known?.localId ?: UUID.randomUUID().toString())
                }
                // Google points to the parent's googleId; the screens group by local id.
                val localByGoogle = existing.mapValues { it.value.localId } + accepted.associate { it.googleId to it.localId }
                database.taskDao().upsert(accepted.map { task ->
                    task.copy(parentId = task.parentId?.let { localByGoogle[it] ?: it })
                })
                // A mark from Google creates the local alarm only once; later the app owns it
                // (an alarm turned OFF in the app keeps the mark from bringing it back).
                for (task in accepted) {
                    val time = download.times[task.googleId] ?: continue
                    if (database.alarmDao().forItem(accountId, "TASK", task.localId) == null) {
                        database.alarmDao().upsert(AlarmEntity(accountId = accountId, itemType = "TASK",
                            itemLocalId = task.localId, mode = AlarmMode.TIME, localTime = time))
                    }
                }
                val deleted = download.deletedGoogleIds.mapNotNull { existing[it] }.filter { it.localId !in blocked }
                if (deleted.isNotEmpty()) database.taskDao().deleteLocal(accountId, deleted.map { it.localId })
                val newest = listOfNotNull(last, download.maxUpdated).maxOrNull()
                if (newest != null) meta(accountId, key, newest.toString())
            }
        }
    }

    private suspend fun blockedLocalIds(accountId: String, type: String): Set<String> =
        database.outboxDao().blockingDownload(accountId).filter { it.itemType == type }.map { it.itemLocalId }.toSet()

    private suspend fun meta(accountId: String, key: String, value: String) =
        database.syncMetaDao().upsert(SyncMetaEntity("$accountId:$key", accountId, value))

    private companion object {
        const val SYNCED = "SYNCED"
        const val UPDATED_MIN_MARGIN_SECONDS = 120L
    }
}
