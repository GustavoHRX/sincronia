package com.sincronia.app.data.calendar

import com.sincronia.app.core.database.EventEntity
import java.time.LocalDate
import java.time.ZoneId

interface CalendarSource {
    suspend fun events(today: LocalDate, zone: ZoneId): List<EventEntity>
}
