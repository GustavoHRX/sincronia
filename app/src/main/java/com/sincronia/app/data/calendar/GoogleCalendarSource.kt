package com.sincronia.app.data.calendar

import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.network.CalendarApi
import com.sincronia.app.core.network.withGoogleRetry
import com.sincronia.app.data.GoogleMappers
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class CalendarWindow(
    val events: List<EventEntity>,
    val timeZone: String?,
    /** Default popup reminders of the primary calendar, in minutes. */
    val defaultReminders: List<Int>,
)

/** Spec 7.2: the whole window (-7 to +61 days) on every sync, all pages before anything is applied. */
class GoogleCalendarSource @Inject constructor(private val api: CalendarApi) {
    suspend fun window(accountId: String, today: LocalDate, zone: ZoneId): CalendarWindow {
        val timeMin = today.minusDays(WINDOW_PAST_DAYS).atStartOfDay(zone).toInstant().toString()
        val timeMax = today.plusDays(WINDOW_FUTURE_DAYS).atStartOfDay(zone).toInstant().toString()
        val dtos = mutableListOf<com.sincronia.app.core.network.EventDto>()
        var timeZone: String? = null
        var defaults = emptyList<Int>()
        var pageToken: String? = null
        do {
            val token = pageToken
            val page = withGoogleRetry { api.events(timeMin, timeMax, token) }
            dtos += page.items
            if (timeZone == null) {
                timeZone = page.timeZone
                defaults = page.defaultReminders.filter { it.method == "popup" }.map { it.minutes }
            }
            pageToken = page.nextPageToken
        } while (pageToken != null)
        val events = dtos.mapNotNull { GoogleMappers.event(it, accountId, timeZone) }
        return CalendarWindow(events, timeZone, defaults)
    }

    companion object {
        const val WINDOW_PAST_DAYS = 7L
        const val WINDOW_FUTURE_DAYS = 61L
    }
}
