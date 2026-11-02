package com.sincronia.app.data.calendar

import com.sincronia.app.core.database.EventDao
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import com.sincronia.app.core.database.EventEntity

class EventRepository @Inject constructor(private val dao: EventDao) {
    fun day(accountId: String, date: LocalDate): Flow<List<EventEntity>> {
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        return dao.observeDay(accountId, date, start, end)
    }

    fun week(accountId: String, first: LocalDate): Flow<List<EventEntity>> = dao.observeRange(
        accountId, first, first.plusDays(7),
        first.atStartOfDay(ZoneId.systemDefault()).toInstant(),
        first.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant(),
    )
}
