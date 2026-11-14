package com.sincronia.app.data.sync

import androidx.room.withTransaction
import com.sincronia.app.core.alarm.AlarmMode
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.OutboxEntity
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.core.log.AppLogger
import com.sincronia.app.core.network.CalendarApi
import com.sincronia.app.core.network.EventDto
import com.sincronia.app.core.network.GoogleBodies
import com.sincronia.app.core.network.TaskDto
import com.sincronia.app.core.network.TasksApi
import com.sincronia.app.core.network.googleReason
import com.sincronia.app.core.network.isTransient
import com.sincronia.app.data.GoogleMappers
import com.sincronia.app.data.edit.ItemType
import com.sincronia.app.data.edit.Operation
import com.sincronia.app.data.edit.TaskField
import com.sincronia.app.core.time.TaskDueDate
import java.io.InterruptedIOException
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/**
 * Step 2 of spec 7.2: sends the outbox (7.2.1). The row keeps no copy of the item; each send reads
 * the current entity. Success updates the item and removes the row in one transaction.
 */
class OutboxProcessor @Inject constructor(
    private val database: AppDatabase,
    private val calendar: CalendarApi,
    private val tasks: TasksApi,
    private val logger: AppLogger,
) {
    /** Thrown when a PATCH got 404/410 but the item still exists (it may be just created). */
    private class NotYetVisible : Exception()

    suspend fun process(accountId: String, now: Instant = Instant.now()) {
        for (entry in database.outboxDao().ready(accountId, now)) {
            try {
                if (entry.itemType == ItemType.EVENT) sendEvent(entry) else sendTask(entry)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                fail(entry, error, now)
            }
        }
    }

    suspend fun hasReady(accountId: String, now: Instant = Instant.now()) =
        database.outboxDao().ready(accountId, now).isNotEmpty()

    // ---- events ----

    private suspend fun sendEvent(entry: OutboxEntity) {
        val event = database.eventDao().byLocalId(entry.accountId, entry.itemLocalId)
            ?: return database.outboxDao().delete(entry.id)
        val id = event.googleId ?: return database.outboxDao().delete(entry.id)
        when (entry.operation) {
            Operation.CREATE -> {
                val dto = try {
                    calendar.insert(GoogleBodies.event(event))
                } catch (error: HttpException) {
                    // 409 duplicate on a retry: the first attempt created it.
                    if (error.code() == 409) calendar.event(id) else throw error
                }
                succeedEvent(entry, dto)
            }
            Operation.UPDATE -> {
                val dto = try {
                    calendar.patch(id, GoogleBodies.event(event, entry.changedFields))
                } catch (error: HttpException) {
                    if (error.code() !in GONE) throw error
                    if (eventGone(id)) return removeLocal(entry) else throw NotYetVisible()
                }
                succeedEvent(entry, dto)
            }
            Operation.DELETE -> {
                val response = calendar.delete(id)
                if (!response.isSuccessful && response.code() !in GONE) throw HttpException(response)
                removeLocal(entry)
            }
        }
    }

    private suspend fun eventGone(id: String): Boolean = try {
        calendar.event(id).status == "cancelled"
    } catch (error: HttpException) {
        if (error.code() in GONE) true else throw error
    }

    private suspend fun succeedEvent(entry: OutboxEntity, dto: EventDto) = finish(entry) {
        val current = database.eventDao().byLocalId(entry.accountId, entry.itemLocalId) ?: return@finish
        database.eventDao().upsert(listOf(current.copy(googleId = dto.id, etag = dto.etag ?: current.etag,
            remoteUpdated = dto.updated?.let(GoogleMappers::instant) ?: current.remoteUpdated, syncState = SYNCED)))
    }

    // ---- tasks ----

    private suspend fun sendTask(entry: OutboxEntity) {
        var task = database.taskDao().byLocalId(entry.accountId, entry.itemLocalId)
            ?: return database.outboxDao().delete(entry.id)
        when (entry.operation) {
            Operation.CREATE -> {
                val listId = task.taskListId
                val dto = (if (entry.lastError == TIMEOUT) findCreated(task, entry.createdAt) else null)
                    ?: tasks.insert(listId, GoogleBodies.task(task, time = taskTime(task)))
                succeedTask(entry, dto, listId)
            }
            Operation.UPDATE -> {
                val id = task.googleId ?: return
                var listId = task.remoteTaskListId ?: task.taskListId
                var dto: TaskDto? = null
                if (task.taskListId != listId) {
                    dto = tasks.move(listId, id, task.taskListId)
                    listId = task.taskListId
                    // Remember the move at once so a failed PATCH does not move again.
                    task = task.copy(remoteTaskListId = listId)
                    database.taskDao().upsert(listOf(task))
                }
                val fields = entry.changedFields - TaskField.LIST
                if (fields.isNotEmpty()) {
                    dto = try {
                        tasks.patch(listId, id, GoogleBodies.task(task, fields, taskTime(task)))
                    } catch (error: HttpException) {
                        if (error.code() !in GONE) throw error
                        if (taskGone(listId, id)) return removeLocal(entry) else throw NotYetVisible()
                    }
                }
                succeedTask(entry, dto, listId)
            }
            Operation.DELETE -> {
                val id = task.googleId ?: return removeLocal(entry)
                val response = tasks.delete(task.remoteTaskListId ?: task.taskListId, id)
                if (!response.isSuccessful && response.code() !in GONE) throw HttpException(response)
                removeLocal(entry)
            }
        }
    }

    private suspend fun taskTime(task: TaskEntity) = database.alarmDao().forItem(task.accountId, ItemType.TASK, task.localId)
        ?.takeIf { it.mode == AlarmMode.TIME }?.localTime

    /** After a timeout, the first POST may have worked: adopt a task with the same title and date. */
    private suspend fun findCreated(task: TaskEntity, createdAt: Instant): TaskDto? {
        var pageToken: String? = null
        do {
            val page = tasks.tasks(task.taskListId, createdAt.minus(Duration.ofMinutes(2)).toString(), pageToken)
            page.items.firstOrNull {
                !it.deleted && it.title == task.title && it.due?.let(TaskDueDate::parse) == task.dueDate
            }?.let { return it }
            pageToken = page.nextPageToken
        } while (pageToken != null)
        return null
    }

    private suspend fun taskGone(listId: String, id: String): Boolean = try {
        tasks.task(listId, id).deleted
    } catch (error: HttpException) {
        if (error.code() in GONE) true else throw error
    }

    private suspend fun succeedTask(entry: OutboxEntity, dto: TaskDto?, listId: String) = finish(entry) {
        val current = database.taskDao().byLocalId(entry.accountId, entry.itemLocalId) ?: return@finish
        database.taskDao().upsert(listOf(current.copy(
            googleId = dto?.id ?: current.googleId,
            remoteTaskListId = listId,
            etag = dto?.etag ?: current.etag,
            remoteUpdated = dto?.updated?.let(GoogleMappers::instant) ?: current.remoteUpdated,
            webViewLink = dto?.webViewLink ?: current.webViewLink,
            syncState = SYNCED,
        )))
    }

    // ---- shared ----

    /**
     * Applies [update] and removes the row in one transaction. If the user edited the item while it
     * was being sent, the row changed: keep it (and the item PENDING) so the new edit goes up next.
     */
    private suspend fun finish(entry: OutboxEntity, update: suspend () -> Unit) = database.withTransaction {
        val row = database.outboxDao().forItem(entry.accountId, entry.itemType, entry.itemLocalId)
        update()
        if (row == null || row == entry) {
            row?.let { database.outboxDao().delete(it.id) }
        } else {
            markItem(entry, PENDING)
        }
    }

    private suspend fun removeLocal(entry: OutboxEntity) = database.withTransaction {
        if (entry.itemType == ItemType.EVENT) database.eventDao().deleteLocal(entry.accountId, listOf(entry.itemLocalId))
        else database.taskDao().deleteLocal(entry.accountId, listOf(entry.itemLocalId))
        database.outboxDao().delete(entry.id)
        // Alarm cancellation arrives in M5.
    }

    private suspend fun fail(entry: OutboxEntity, error: Exception, now: Instant) {
        val timeout = error is InterruptedIOException
        val transient = timeout || error is NotYetVisible || isTransient(error) ||
            (error is HttpException && error.code() == 401)
        val reason = when {
            timeout -> TIMEOUT
            error is HttpException -> listOfNotNull(error.code().toString(), googleReason(error)).joinToString(" ")
            else -> error.javaClass.simpleName
        }
        val attempts = entry.attempts + 1
        val failed = !transient || attempts >= MAX_ATTEMPTS
        logger.w(TAG, "Outbox ${entry.itemType} ${entry.operation} failed ($reason), attempt $attempts")
        database.withTransaction {
            val row = database.outboxDao().forItem(entry.accountId, entry.itemType, entry.itemLocalId) ?: return@withTransaction
            database.outboxDao().upsert(row.copy(
                attempts = attempts,
                lastError = reason,
                state = if (failed) "FAILED" else "PENDING",
                nextAttemptAt = if (failed) null else now.plus(BACKOFF[attempts - 1]),
            ))
            if (failed) markItem(entry, "FAILED")
        }
    }

    private suspend fun markItem(entry: OutboxEntity, state: String) {
        if (entry.itemType == ItemType.EVENT) {
            database.eventDao().byLocalId(entry.accountId, entry.itemLocalId)?.let {
                database.eventDao().upsert(listOf(it.copy(syncState = state)))
            }
        } else {
            database.taskDao().byLocalId(entry.accountId, entry.itemLocalId)?.let {
                database.taskDao().upsert(listOf(it.copy(syncState = state)))
            }
        }
    }

    // ---- "Tentar de novo" and "Descartar alteração" ----

    suspend fun retry(accountId: String, itemType: String, localId: String) = database.withTransaction {
        val row = database.outboxDao().forItem(accountId, itemType, localId) ?: return@withTransaction
        database.outboxDao().upsert(row.copy(state = "PENDING", attempts = 0, nextAttemptAt = null, lastError = null))
        markItem(row, PENDING)
    }

    /** Drops the local change and takes Google's version again (or removes a never-sent item). */
    suspend fun discard(accountId: String, itemType: String, localId: String) {
        val row = database.outboxDao().forItem(accountId, itemType, localId) ?: return
        if (row.operation == Operation.CREATE) return removeLocal(row)
        if (itemType == ItemType.EVENT) {
            val local = database.eventDao().byLocalId(accountId, localId) ?: return removeLocal(row)
            val remote = goneAsNull { calendar.event(local.googleId!!) }
            val zone = database.syncMetaDao().value(accountId, "$accountId:calendar:primary:timeZone")
            val mapped = remote?.let { GoogleMappers.event(it, accountId, zone) } ?: return removeLocal(row)
            database.withTransaction {
                database.eventDao().upsert(listOf(mapped.copy(localId = local.localId)))
                database.outboxDao().delete(row.id)
            }
        } else {
            val local = database.taskDao().byLocalId(accountId, localId) ?: return removeLocal(row)
            val listId = local.remoteTaskListId ?: local.taskListId
            val remote = goneAsNull { tasks.task(listId, local.googleId!!) }
            if (remote == null || remote.deleted) return removeLocal(row)
            val mapped = GoogleMappers.task(remote, accountId, listId)
            database.withTransaction {
                database.taskDao().upsert(listOf(mapped.copy(localId = local.localId, parentId = local.parentId)))
                database.outboxDao().delete(row.id)
            }
        }
    }

    /** 404/410 = deleted in Google; any other error (offline) propagates, so nothing is dropped. */
    private suspend fun <T> goneAsNull(fetch: suspend () -> T): T? = try {
        fetch()
    } catch (error: HttpException) {
        if (error.code() in GONE) null else throw error
    }

    companion object {
        private const val TAG = "Outbox"
        private const val SYNCED = "SYNCED"
        private const val PENDING = "PENDING"
        const val TIMEOUT = "timeout"
        const val MAX_ATTEMPTS = 5
        private val GONE = setOf(404, 410)
        /** Spec 7.2.1: 1, 5, 15 and 60 minutes; the 5th failure becomes FAILED. */
        val BACKOFF: List<Duration> = listOf(1L, 5L, 15L, 60L).map(Duration::ofMinutes)
    }
}
