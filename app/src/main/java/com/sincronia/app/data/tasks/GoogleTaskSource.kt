package com.sincronia.app.data.tasks

import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.core.database.TaskListEntity
import com.sincronia.app.core.network.TasksApi
import com.sincronia.app.core.network.withGoogleRetry
import com.sincronia.app.data.GoogleMappers
import com.sincronia.app.core.alarm.TaskTimeMark
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

data class TaskDownload(
    val tasks: List<TaskEntity>,
    /** Tasks Google reported as deleted (showDeleted=true). */
    val deletedGoogleIds: Set<String>,
    /** Largest `updated` received, on Google's clock; null when nothing came. */
    val maxUpdated: Instant?,
    /** `⏰ HH:mm` found in the notes, by googleId (spec 7.3). */
    val times: Map<String, LocalTime> = emptyMap(),
)

class GoogleTaskSource @Inject constructor(private val api: TasksApi) {
    /** Complete listing (all pages), with the default list marked. */
    suspend fun lists(accountId: String): List<TaskListEntity> {
        val dtos = mutableListOf<com.sincronia.app.core.network.TaskListDto>()
        var pageToken: String? = null
        do {
            val token = pageToken
            val page = withGoogleRetry { api.lists(token) }
            dtos += page.items
            pageToken = page.nextPageToken
        } while (pageToken != null)
        // If @default fails, the first list returned is the default (spec 7.2).
        val defaultId = runCatching { withGoogleRetry { api.defaultList() }.id }.getOrNull() ?: dtos.firstOrNull()?.id
        return dtos.map { GoogleMappers.taskList(it, accountId, isDefault = it.id == defaultId) }
    }

    /** All pages of one list, changed since [updatedMin] (omitted on the first sync). */
    suspend fun tasks(accountId: String, listId: String, updatedMin: Instant?): TaskDownload {
        val tasks = mutableListOf<TaskEntity>()
        val deleted = mutableSetOf<String>()
        val times = mutableMapOf<String, LocalTime>()
        var maxUpdated: Instant? = null
        var pageToken: String? = null
        do {
            val token = pageToken
            val page = withGoogleRetry { api.tasks(listId, updatedMin?.toString(), token) }
            for (dto in page.items) {
                dto.updated?.let(GoogleMappers::instant)?.let { if (maxUpdated == null || it > maxUpdated) maxUpdated = it }
                if (dto.deleted) deleted += dto.id else tasks += GoogleMappers.task(dto, accountId, listId)
                TaskTimeMark.parse(dto.notes).time?.let { times[dto.id] = it }
            }
            pageToken = page.nextPageToken
        } while (pageToken != null)
        return TaskDownload(tasks, deleted, maxUpdated, times)
    }
}
