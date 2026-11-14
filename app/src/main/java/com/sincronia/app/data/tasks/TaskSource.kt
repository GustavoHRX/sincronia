package com.sincronia.app.data.tasks

import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.core.database.TaskListEntity
import java.time.LocalDate

interface TaskSource {
    suspend fun lists(): List<TaskListEntity>
    suspend fun tasks(today: LocalDate): List<TaskEntity>
}
