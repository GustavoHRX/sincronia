package com.sincronia.app.core.time

import java.time.LocalDate

/** Tasks API due is a calendar date, even though its wire format ends in UTC midnight. */
object TaskDueDate {
    fun parse(due: String): LocalDate = LocalDate.parse(due.take(10))
    fun format(date: LocalDate): String = "${date}T00:00:00.000Z"
}
