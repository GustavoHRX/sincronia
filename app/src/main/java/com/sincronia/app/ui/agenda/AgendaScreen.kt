package com.sincronia.app.ui.agenda

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sincronia.app.R
import com.sincronia.app.core.database.EventEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun AgendaScreen(viewModel: AgendaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val first = state.selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val format = remember { DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("pt-BR")) }
    var drag = 0f
    Column(Modifier.fillMaxSize().pointerInput(Unit) {
        detectHorizontalDragGestures(onDragEnd = {
            if (drag > 80) viewModel.moveWeek(-1)
            if (drag < -80) viewModel.moveWeek(1)
            drag = 0f
        }) { change, amount -> change.consume(); drag += amount }
    }) {
        Text(stringResource(R.string.nav_agenda), modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val previousLabel = stringResource(R.string.agenda_previous_week)
            val nextLabel = stringResource(R.string.agenda_next_week)
            OutlinedButton(onClick = { viewModel.moveWeek(-1) },
                modifier = Modifier.semantics { contentDescription = previousLabel }) {
                Text(stringResource(R.string.previous_arrow))
            }
            Text("${first.dayOfMonth}/${first.monthValue} – ${first.plusDays(6).dayOfMonth}/${first.plusDays(6).monthValue}",
                modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { viewModel.moveWeek(1) },
                modifier = Modifier.semantics { contentDescription = nextLabel }) {
                Text(stringResource(R.string.next_arrow))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            (0L..6L).forEach { offset ->
                val day = first.plusDays(offset)
                val hasEvent = state.weekEvents.any { it.covers(day) } || state.weekTasks.any { it.dueDate == day }
                val selected = day == state.selected
                Card(
                    modifier = Modifier.weight(1f).padding(2.dp).clickable { viewModel.select(day) },
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Text(format.format(day), Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text(day.dayOfMonth.toString(), Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text(if (hasEvent) "•" else " ", Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
        Text(state.selected.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))),
            modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
        if (state.loading) Text(stringResource(R.string.loading), Modifier.padding(16.dp))
        else if (state.events.isEmpty() && state.tasks.isEmpty()) Text(stringResource(R.string.agenda_empty), Modifier.padding(16.dp))
        else LazyColumn(Modifier.fillMaxSize()) {
            items(state.events, key = { it.localId }) { event -> EventCard(event, state.selected) }
            items(state.tasks, key = { it.localId }) { task ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row {
                        Checkbox(checked = false, onCheckedChange = null, enabled = false)
                        Column {
                            Text(task.title, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.task_no_time), Modifier.padding(bottom = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun EventEntity.covers(date: LocalDate): Boolean = if (allDay) {
    startDate != null && endDate != null && startDate <= date && endDate > date
} else {
    val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
    val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
    startAt != null && endAt != null && startAt < end && endAt > start
}

@Composable
private fun EventCard(event: EventEntity, selected: LocalDate) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Text(event.title, Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp),
            style = MaterialTheme.typography.titleMedium)
        val localStart = event.startAt?.atZone(ZoneId.systemDefault())
        val time = if (event.allDay) stringResource(R.string.all_day)
            else if (localStart?.toLocalDate() != selected) localStart?.format(DateTimeFormatter.ofPattern("d/M HH:mm")) ?: ""
            else localStart?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()
        Text(time, Modifier.padding(start = 12.dp, bottom = 12.dp))
        if (event.recurringEventId != null) Text(stringResource(R.string.recurring_read_only), Modifier.padding(start = 12.dp, bottom = 12.dp))
    }
}
