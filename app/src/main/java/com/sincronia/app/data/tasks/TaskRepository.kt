package com.sincronia.app.data.tasks

import com.sincronia.app.core.database.TaskDao
import com.sincronia.app.core.database.TaskListDao
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class TaskRepository @Inject constructor(private val tasks: TaskDao, private val lists: TaskListDao) {
    fun lists(accountId: String) = lists.observeAll(accountId)
    fun day(accountId: String, date: LocalDate) = tasks.observeDay(accountId, date)
    fun week(accountId: String, first: LocalDate) = tasks.observeRange(accountId, first, first.plusDays(7))
    fun list(accountId: String, listId: String) =
        tasks.observeList(accountId, listId, Instant.now().minus(7, ChronoUnit.DAYS))

    /** Not completed and due before [today]; tasks without a date never become overdue. */
    fun overdue(accountId: String, today: LocalDate) = tasks.observeOverdue(accountId, today)

    /** Completed during the local day [date] in [zone]. */
    fun completedOn(accountId: String, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()) =
        tasks.observeCompletedBetween(
            accountId,
            date.atStartOfDay(zone).toInstant(),
            date.plusDays(1).atStartOfDay(zone).toInstant(),
        )
}
