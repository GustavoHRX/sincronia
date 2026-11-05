package com.sincronia.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sincronia.app.R
import com.sincronia.app.core.database.EventEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val hourFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dayHourFormat = DateTimeFormatter.ofPattern("d/M HH:mm")
private val dayFormat = DateTimeFormatter.ofPattern("d/M")

/** Event card shared by Today and Agenda; times are shown relative to [day]. */
@Composable
fun EventCard(event: EventEntity, day: LocalDate, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Text(event.title, Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp),
            style = MaterialTheme.typography.titleMedium)
        Text(eventTimeLabel(event, day), Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (event.recurringEventId != null) {
            Text(stringResource(R.string.recurring_read_only), Modifier.padding(start = 12.dp, bottom = 12.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Task card shared by Today and Agenda. Completing from the card arrives in M4. */
@Composable
fun TaskCard(title: String, subtitle: String, modifier: Modifier = Modifier, completed: Boolean = false) {
    Card(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = completed, onCheckedChange = null, enabled = false,
                modifier = Modifier.padding(start = 12.dp, end = 4.dp))
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (completed) TextDecoration.LineThrough else null)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun eventTimeLabel(event: EventEntity, day: LocalDate): String {
    if (event.allDay) {
        val lastDay = event.endDate?.minusDays(1)
        return if (lastDay != null && lastDay > day) {
            stringResource(R.string.all_day_until, lastDay.format(dayFormat))
        } else {
            stringResource(R.string.all_day)
        }
    }
    val zone = ZoneId.systemDefault()
    val start = event.startAt?.atZone(zone) ?: return ""
    val end = event.endAt?.atZone(zone) ?: return relativeTime(start, day)
    return stringResource(R.string.time_range, relativeTime(start, day), relativeTime(end, day))
}

@Composable
private fun relativeTime(time: ZonedDateTime, day: LocalDate): String = when (time.toLocalDate()) {
    day -> time.format(hourFormat)
    day.minusDays(1) -> stringResource(R.string.time_yesterday, time.format(hourFormat))
    day.plusDays(1) -> stringResource(R.string.time_tomorrow, time.format(hourFormat))
    else -> time.format(dayHourFormat)
}
