package com.sincronia.app.ui.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sincronia.app.R
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.data.edit.ItemType
import com.sincronia.app.ui.components.ItemActions
import com.sincronia.app.ui.components.NewItemButton
import com.sincronia.app.ui.components.TaskCard
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import java.time.LocalDate

@Composable
fun TasksScreen(actions: ItemActions, viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val open = state.tasks.filter { it.status != "completed" }
    val completed = state.tasks.filter { it.status == "completed" }
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.nav_tasks), modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            state.lists.forEach { list ->
                FilterChip(selected = state.selectedListId == list.googleId,
                    onClick = { viewModel.selectList(list.googleId) }, label = { Text(list.title) },
                    modifier = Modifier.padding(end = 8.dp))
            }
        }
        if (state.loading) Text(stringResource(R.string.loading), Modifier.padding(16.dp))
        else if (state.tasks.isEmpty()) Text(stringResource(R.string.tasks_empty), Modifier.padding(16.dp))
        else LazyColumn(Modifier.fillMaxSize()) {
            val ordered = orderTasks(open)
            items(ordered, key = { it.localId }) { task -> TaskRow(task, actions, viewModel::setCompleted) }
            if (completed.isNotEmpty()) {
                item {
                    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.padding(8.dp)) {
                        Text("${stringResource(R.string.tasks_completed)} (${completed.size})")
                    }
                }
                if (expanded) items(orderTasks(completed), key = { it.localId }) { task -> TaskRow(task, actions, viewModel::setCompleted) }
            }
        }
    }
    NewItemButton({ actions.newItem(LocalDate.now(), ItemType.TASK, state.selectedListId) }, Modifier.align(Alignment.BottomEnd))
    }
}

private fun orderTasks(tasks: List<TaskEntity>): List<TaskEntity> {
    val byParent = tasks.groupBy { it.parentId }
    val result = mutableListOf<TaskEntity>()
    val seen = mutableSetOf<String>()
    fun visit(task: TaskEntity) {
        if (!seen.add(task.localId)) return
        result.add(task)
        byParent[task.localId].orEmpty().sortedBy { it.position }.forEach(::visit)
    }
    tasks.filter { it.parentId == null || tasks.none { parent -> parent.localId == it.parentId } }
        .sortedWith(compareBy<TaskEntity> { it.position }.thenBy { it.title }).forEach(::visit)
    tasks.forEach(::visit)
    return result
}

@Composable
private fun TaskRow(task: TaskEntity, actions: ItemActions, complete: (TaskEntity, Boolean) -> Unit) {
    val date = task.dueDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag("pt-BR")))
    TaskCard(task, date, Modifier.padding(start = if (task.parentId == null) 0.dp else 20.dp),
        onClick = { actions.openTask(task.localId) }, onCompletedChange = { complete(task, it) })
}
