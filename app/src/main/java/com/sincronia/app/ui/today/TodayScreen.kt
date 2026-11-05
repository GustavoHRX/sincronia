package com.sincronia.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sincronia.app.R
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.ui.components.EventCard
import com.sincronia.app.ui.components.TaskCard
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBr = Locale.forLanguageTag("pt-BR")
private val dateFormat = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ptBr)
private val shortDate = DateTimeFormatter.ofPattern("d/M")
private val hourFormat = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item(key = "header") { Header(state) }
            when {
                state.loading -> item(key = "loading") { Message(stringResource(R.string.loading)) }
                state.isEmpty -> item(key = "empty") { Message(stringResource(R.string.today_empty)) }
                else -> sections(state)
            }
        }
    }
}

private fun LazyListScope.sections(state: TodayUiState) {
    if (state.overdue.isNotEmpty()) {
        item(key = "h-overdue") { SectionTitle(stringResource(R.string.today_overdue)) }
        items(state.overdue, key = { "overdue-${it.localId}" }) { task ->
            TaskCard(task.title, overdueLabel(task, state.date))
        }
    }
    if (state.allDayEvents.isNotEmpty() || state.allDayTasks.isNotEmpty()) {
        item(key = "h-all-day") { SectionTitle(stringResource(R.string.all_day)) }
        items(state.allDayEvents, key = { "all-day-${it.localId}" }) { event -> EventCard(event, state.date) }
        items(state.allDayTasks, key = { "task-${it.localId}" }) { task ->
            TaskCard(task.title, stringResource(R.string.task_no_time))
        }
    }
    if (state.timeline.isNotEmpty()) {
        item(key = "h-timeline") { SectionTitle(stringResource(R.string.today_timeline)) }
        items(state.timeline, key = { entry ->
            when (entry) {
                is TimelineEntry.Event -> "event-${entry.event.localId}"
                is TimelineEntry.Now -> "now"
            }
        }) { entry ->
            when (entry) {
                is TimelineEntry.Event -> EventCard(entry.event, state.date)
                is TimelineEntry.Now -> NowLine(entry.at.atZone(ZoneId.systemDefault()).format(hourFormat))
            }
        }
    }
    if (state.completed.isNotEmpty()) {
        item(key = "h-completed") { SectionTitle(stringResource(R.string.today_completed)) }
        items(state.completed, key = { "done-${it.localId}" }) { task ->
            TaskCard(task.title, stringResource(R.string.today_completed_label), completed = true)
        }
    }
}

@Composable
private fun Header(state: TodayUiState) {
    val greeting = stringResource(
        when (state.greeting) {
            Greeting.MORNING -> R.string.greeting_morning
            Greeting.AFTERNOON -> R.string.greeting_afternoon
            Greeting.EVENING -> R.string.greeting_evening
        },
    )
    val title = state.firstName?.let { stringResource(R.string.greeting_with_name, greeting, it) } ?: greeting
    Text(title, Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp).semantics { heading() },
        style = MaterialTheme.typography.headlineMedium)
    Text(state.date.format(dateFormat).replaceFirstChar { it.titlecase(ptBr) },
        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp).semantics { heading() },
        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Message(text: String) {
    Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
}

/** Current time marker; `secondary` is the contrast-safe light blue from section 9. */
@Composable
private fun NowLine(time: String) {
    val color = MaterialTheme.colorScheme.secondary
    val description = stringResource(R.string.today_now_description, time)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.today_now, time), Modifier.padding(end = 8.dp),
            style = MaterialTheme.typography.labelMedium, color = color)
        Box(Modifier.size(8.dp).background(color, CircleShape))
        HorizontalDivider(Modifier.weight(1f), thickness = 2.dp, color = color)
    }
}

@Composable
private fun overdueLabel(task: TaskEntity, today: LocalDate): String {
    val due = task.dueDate ?: return stringResource(R.string.task_no_time)
    return if (due == today.minusDays(1)) stringResource(R.string.today_overdue_yesterday)
    else stringResource(R.string.today_overdue_since, due.format(shortDate))
}
