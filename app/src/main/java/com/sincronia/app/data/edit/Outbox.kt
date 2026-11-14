package com.sincronia.app.data.edit

import androidx.room.withTransaction
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.database.OutboxEntity
import java.time.Instant
import javax.inject.Inject

object ItemType {
    const val EVENT = "EVENT"
    const val TASK = "TASK"
}

object Operation {
    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"
}

/** What enqueue decided; [NONE] means CREATE + DELETE cancelled each other (no network). */
enum class Enqueued { CREATE, UPDATE, DELETE, NONE }

/**
 * One outbox row per item (spec 7.2.1). Merge rules: CREATE + UPDATE stays CREATE, UPDATE + UPDATE
 * joins the fields, CREATE + DELETE drops the row and the local item, UPDATE + DELETE becomes DELETE.
 * A FAILED row that gets a new edit goes back to PENDING with a fresh attempt count.
 */
class Outbox @Inject constructor(private val database: AppDatabase) {
    suspend fun enqueue(
        accountId: String,
        itemType: String,
        localId: String,
        operation: String,
        changedFields: Collection<String> = emptyList(),
        now: Instant = Instant.now(),
    ): Enqueued = database.withTransaction {
        val dao = database.outboxDao()
        val current = dao.forItem(accountId, itemType, localId)
        val merged: OutboxEntity? = when {
            current == null -> OutboxEntity(accountId = accountId, itemType = itemType, itemLocalId = localId,
                operation = operation, changedFields = changedFields.distinct(), createdAt = now)
            operation == Operation.DELETE && current.operation == Operation.CREATE -> null
            operation == Operation.DELETE -> current.copy(operation = Operation.DELETE, changedFields = emptyList())
            current.operation == Operation.CREATE -> current
            current.operation == Operation.DELETE -> current
            else -> current.copy(operation = Operation.UPDATE,
                changedFields = (current.changedFields + changedFields).distinct())
        }
        if (merged == null) {
            dao.delete(current!!.id)
            deleteLocal(accountId, itemType, localId)
            return@withTransaction Enqueued.NONE
        }
        dao.upsert(merged.copy(state = "PENDING", attempts = 0, nextAttemptAt = null, lastError = null))
        Enqueued.valueOf(merged.operation)
    }

    private suspend fun deleteLocal(accountId: String, itemType: String, localId: String) {
        if (itemType == ItemType.EVENT) database.eventDao().deleteLocal(accountId, listOf(localId))
        else database.taskDao().deleteLocal(accountId, listOf(localId))
    }
}
