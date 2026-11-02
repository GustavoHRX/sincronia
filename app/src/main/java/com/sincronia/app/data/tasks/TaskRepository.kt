package com.sincronia.app.data.tasks

import com.sincronia.app.core.database.TaskDao
import com.sincronia.app.core.database.TaskListDao
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class TaskRepository @Inject constructor(private val tasks: TaskDao, private val lists: TaskListDao) {
    fun lists(accountId: String) = lists.observeAll(accountId)
    fun day(accountId: String, date: LocalDate) = tasks.observeDay(accountId, date)
    fun week(accountId: String, first: LocalDate) = tasks.observeRange(accountId, first, first.plusDays(7))
    fun list(accountId: String, listId: String) =
        tasks.observeList(accountId, listId, Instant.now().minus(7, ChronoUnit.DAYS))
}
