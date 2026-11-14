package com.sincronia.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sincronia.app.R
import com.sincronia.app.core.alarm.AlarmMode
import com.sincronia.app.data.edit.ItemType
import com.sincronia.app.data.edit.ReadOnlyReason
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateLabel = DateTimeFormatter.ofPattern("EEE, d 'de' MMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))
private val timeLabel = DateTimeFormatter.ofPattern("HH:mm")
private val reminderMinutes = listOf(5, 10, 15, 30, 60)

@Composable
fun EditorScreen(onClose: () -> Unit, viewModel: EditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.closed) { if (state.closed) onClose() }
    if (state.loading) return
    val editable = state.readOnly == null && !state.busy
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(when {
            state.isNew -> R.string.editor_new
            state.type == ItemType.EVENT -> R.string.editor_event
            else -> R.string.editor_task
        }), style = MaterialTheme.typography.headlineSmall)

        if (state.isNew) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(ItemType.EVENT to R.string.editor_type_event, ItemType.TASK to R.string.editor_type_task)
                    .forEachIndexed { index, (type, label) ->
                        SegmentedButton(selected = state.type == type, onClick = { viewModel.edit { it.copy(type = type) } },
                            shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(stringResource(label)) }
                    }
            }
        }
        state.readOnly?.let { reason ->
            Banner(stringResource(when (reason) {
                ReadOnlyReason.RECURRING -> R.string.editor_read_only_recurring
                ReadOnlyReason.SPECIAL_TYPE -> R.string.editor_read_only_special
                ReadOnlyReason.NOT_ORGANIZER -> R.string.editor_read_only_guest
            }))
        }
        if (state.failed) {
            Banner(stringResource(R.string.editor_sync_failed))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::retrySync, enabled = !state.busy) { Text(stringResource(R.string.editor_retry)) }
                OutlinedButton(onClick = viewModel::discardChange, enabled = !state.busy) {
                    Text(stringResource(R.string.editor_discard))
                }
            }
        }

        OutlinedTextField(state.title, { v -> viewModel.edit { it.copy(title = v) } }, Modifier.fillMaxWidth(),
            enabled = editable, label = { Text(stringResource(R.string.editor_title)) }, singleLine = true)

        if (state.type == ItemType.EVENT) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.all_day), Modifier.weight(1f))
                Switch(state.allDay, { v -> viewModel.editTime { it.copy(allDay = v) } }, enabled = editable)
            }
            DateField(stringResource(R.string.editor_date), state.date, editable) { d ->
                viewModel.editTime { it.copy(date = d, endDate = if (it.endDate < d) d else it.endDate) }
            }
            if (state.allDay) {
                DateField(stringResource(R.string.editor_until), state.endDate, editable) { d -> viewModel.editTime { it.copy(endDate = d) } }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField(stringResource(R.string.editor_start), state.start, editable, Modifier.weight(1f)) { t ->
                        viewModel.editTime { it.copy(start = t, end = if (it.end == it.start.plusHours(1)) t.plusHours(1) else it.end) }
                    }
                    TimeField(stringResource(R.string.editor_end), state.end, editable, Modifier.weight(1f)) { t ->
                        viewModel.editTime { it.copy(end = t) }
                    }
                }
            }
            ReminderField(state.reminder, editable) { r -> viewModel.edit { it.copy(reminder = r) } }
            EventAlarmField(state, !state.busy, viewModel)
            OutlinedTextField(state.location, { v -> viewModel.edit { it.copy(location = v) } }, Modifier.fillMaxWidth(),
                enabled = editable, label = { Text(stringResource(R.string.editor_location)) }, singleLine = true)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.editor_has_date), Modifier.weight(1f))
                Switch(state.hasDueDate, { v -> viewModel.edit { it.copy(hasDueDate = v) } }, enabled = editable)
            }
            if (state.hasDueDate) {
                DateField(stringResource(R.string.editor_date), state.date, editable) { d -> viewModel.edit { it.copy(date = d) } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.editor_task_time), Modifier.weight(1f))
                    Switch(state.taskHasTime, { v -> viewModel.edit { it.copy(taskHasTime = v) } }, enabled = editable)
                }
                if (state.taskHasTime) {
                    TimeField(stringResource(R.string.editor_alarm_at), state.alarmTime, editable, Modifier.fillMaxWidth()) { t ->
                        viewModel.edit { it.copy(alarmTime = t) }
                    }
                }
            }
            ListField(state, editable) { id -> viewModel.edit { it.copy(listId = id) } }
        }
        OutlinedTextField(state.notes, { v -> viewModel.edit { it.copy(notes = v) } }, Modifier.fillMaxWidth(),
            enabled = editable, label = { Text(stringResource(R.string.editor_notes)) }, minLines = 3)

        when (state.message) {
            EditorMessage.TITLE_REQUIRED -> R.string.editor_title_required
            EditorMessage.END_BEFORE_START -> R.string.editor_end_before_start
            EditorMessage.OFFLINE -> R.string.editor_offline
            EditorMessage.PAST_ALARM -> R.string.editor_past_alarm
            EditorMessage.NONE -> null
        }?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.readOnly == null) {
                Button(onClick = viewModel::save, enabled = editable) { Text(stringResource(R.string.editor_save)) }
            } else {
                Button(onClick = viewModel::saveAlarmOnly, enabled = !state.busy) { Text(stringResource(R.string.editor_save_alarm)) }
            }
            OutlinedButton(onClick = onClose) { Text(stringResource(R.string.cancel)) }
            if (!state.isNew && state.readOnly == null) {
                TextButton(onClick = { confirmDelete = true }, enabled = editable) { Text(stringResource(R.string.editor_delete)) }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.editor_delete_confirm)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text(stringResource(R.string.editor_delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun Banner(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(text, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, date: LocalDate, enabled: Boolean, onPick: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${date.format(dateLabel)}")
    }
    if (open) {
        // The picker works in UTC millis; converting through UTC keeps the chosen day.
        val picker = rememberDatePickerState(date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(picker) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, time: LocalTime, enabled: Boolean, modifier: Modifier, onPick: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = modifier) {
        Text("$label: ${time.format(timeLabel)}")
    }
    if (open) {
        val picker = rememberTimePickerState(time.hour, time.minute, is24Hour = true)
        AlertDialog(onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { onPick(LocalTime.of(picker.hour, picker.minute)); open = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(picker) })
    }
}

@Composable
private fun ReminderField(reminder: Reminder, enabled: Boolean, onPick: (Reminder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val options = buildList {
        add(Reminder(true))
        add(Reminder(false, null))
        (reminderMinutes + listOfNotNull(reminder.minutes)).distinct().sorted().forEach { add(Reminder(false, it)) }
    }
    Box {
        OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("${stringResource(R.string.editor_reminder)}: ${reminderLabel(reminder)}")
        }
        DropdownMenu(open, { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(reminderLabel(option)) }, onClick = { onPick(option); open = false })
            }
        }
    }
}

@Composable
private fun reminderLabel(reminder: Reminder): String = when {
    reminder.useDefault -> stringResource(R.string.editor_reminder_default)
    reminder.minutes == null -> stringResource(R.string.editor_reminder_none)
    else -> stringResource(R.string.editor_reminder_minutes, reminder.minutes)
}

/** Local alarm of an event; editable even when the event itself is read-only. */
@Composable
private fun EventAlarmField(state: EditorState, enabled: Boolean, viewModel: EditorViewModel) {
    var open by remember { mutableStateOf(false) }
    val options: List<Pair<String, Int?>> = if (state.allDay) {
        listOf(AlarmMode.OFF to null, AlarmMode.TIME to null)
    } else {
        listOf(AlarmMode.AUTO to null, AlarmMode.OFF to null) + listOf(0, 5, 10, 15, 30, 60).map { AlarmMode.OFFSET to it }
    }
    val current = if (state.allDay && state.alarmMode != AlarmMode.TIME) AlarmMode.OFF else state.alarmMode
    Box {
        OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("${stringResource(R.string.editor_alarm)}: ${alarmLabel(current, state.alarmOffset)}")
        }
        DropdownMenu(open, { open = false }) {
            options.forEach { (mode, minutes) ->
                DropdownMenuItem(text = { Text(alarmLabel(mode, minutes ?: state.alarmOffset)) }, onClick = {
                    viewModel.edit { it.copy(alarmMode = mode, alarmOffset = minutes ?: it.alarmOffset) }
                    open = false
                })
            }
        }
    }
    if (current == AlarmMode.TIME) {
        TimeField(stringResource(R.string.editor_alarm_at), state.alarmTime, enabled, Modifier.fillMaxWidth()) { t ->
            viewModel.edit { it.copy(alarmTime = t) }
        }
    }
    if (state.seriesId != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.editor_series_off))
                Text(stringResource(R.string.editor_series_off_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(state.seriesOff, { v -> viewModel.edit { it.copy(seriesOff = v) } }, enabled = enabled)
        }
    }
}

@Composable
private fun alarmLabel(mode: String, minutes: Int): String = when (mode) {
    AlarmMode.AUTO -> stringResource(R.string.editor_alarm_auto)
    AlarmMode.OFF -> stringResource(R.string.editor_alarm_off)
    AlarmMode.TIME -> stringResource(R.string.editor_alarm_time)
    else -> if (minutes == 0) stringResource(R.string.editor_alarm_at_start) else stringResource(R.string.editor_reminder_minutes, minutes)
}

@Composable
private fun ListField(state: EditorState, enabled: Boolean, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = state.lists.firstOrNull { it.googleId == state.listId }?.title.orEmpty()
    Box {
        OutlinedButton(onClick = { open = true }, enabled = enabled && state.lists.size > 1, modifier = Modifier.fillMaxWidth()) {
            Text("${stringResource(R.string.editor_list)}: $current")
        }
        DropdownMenu(open, { open = false }) {
            state.lists.forEach { list ->
                DropdownMenuItem(text = { Text(list.title) }, onClick = { onPick(list.googleId); open = false })
            }
        }
    }
}
