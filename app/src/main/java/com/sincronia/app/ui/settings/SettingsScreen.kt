package com.sincronia.app.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkInfo
import com.sincronia.app.BuildConfig
import com.sincronia.app.R
import com.sincronia.app.data.settings.AppSettings
import com.sincronia.app.ui.permissions.rememberPermissionStatus
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm")

/** F10: account, alarms, sync, permissions, battery and diagnostics. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val photo by viewModel.photo.collectAsState()
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    val shareTitle = stringResource(R.string.settings_diagnostics_share)

    LaunchedEffect(Unit) {
        viewModel.shareRequests.collect { uri ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(send, shareTitle))
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineMedium)

        Section(R.string.settings_account)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Avatar(photo?.asImageBitmap(), state.account.name ?: state.account.email)
            Column(Modifier.weight(1f)) {
                if (state.account.demo) {
                    Text(stringResource(R.string.settings_demo_account))
                } else {
                    state.account.name?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                    state.account.email?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (state.account.needsReauth) {
            Text(stringResource(R.string.settings_reauth), color = MaterialTheme.colorScheme.error)
        }
        OutlinedButton(onClick = { confirm = true }, enabled = !state.disconnecting) {
            Text(stringResource(R.string.settings_disconnect))
        }

        Section(R.string.settings_section_alarms)
        Label(R.string.settings_new_event_alarm)
        // Six options do not fit one segmented row on a phone: chips wrap to a second line.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppSettings.NEW_EVENT_ALARM_OPTIONS.forEach { minutes ->
                FilterChip(
                    selected = minutes == state.settings.newEventAlarmMinutes,
                    onClick = { viewModel.setNewEventAlarm(minutes) },
                    label = {
                        Text(if (minutes == 0) stringResource(R.string.settings_at_start) else stringResource(R.string.settings_minutes, minutes))
                    },
                )
            }
        }
        Label(R.string.settings_snooze)
        Choices(AppSettings.SNOOZE_OPTIONS, state.settings.snoozeMinutes, viewModel::setSnooze) {
            stringResource(R.string.settings_minutes, it)
        }
        AlarmSoundRow(state.settings.alarmSoundUri, viewModel::setAlarmSound)
        DailyReminderRow(state.settings, viewModel::setDailyReminder, viewModel::setDailyReminderTime)
        if (BuildConfig.DEBUG) {
            var testSet by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { testSet = viewModel.testAlarm() }) { Text(stringResource(R.string.settings_test_alarm)) }
            if (testSet) Text(stringResource(R.string.settings_test_alarm_set), style = MaterialTheme.typography.bodyMedium)
        }

        Section(R.string.settings_section_sync)
        Label(R.string.settings_sync_every)
        Choices(AppSettings.SYNC_INTERVAL_OPTIONS, state.settings.syncIntervalMinutes, viewModel::setSyncInterval) {
            stringResource(R.string.settings_minutes, it)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = viewModel::syncNow, enabled = state.syncState == null) {
                Text(stringResource(R.string.settings_sync_now))
            }
            if (state.syncState == WorkInfo.State.RUNNING) CircularProgressIndicator(Modifier.size(24.dp))
        }
        Text(syncStatus(state.syncState, state.lastSyncAt), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Section(R.string.settings_section_device)
        PermissionsSummary(context)
        // Only a shortcut: setAlarmClock rings in Doze anyway; it helps the sync on some brands (spec 7.5).
        OutlinedButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }) { Text(stringResource(R.string.settings_battery)) }
        OutlinedButton(onClick = viewModel::exportDiagnostics, enabled = !state.exporting) {
            Text(stringResource(R.string.settings_diagnostics))
        }
        Text(stringResource(R.string.settings_diagnostics_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.settings_disconnect_confirm_title)) },
            text = { Text(stringResource(R.string.settings_disconnect_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { confirm = false; viewModel.disconnect() }) {
                    Text(stringResource(R.string.settings_disconnect))
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Section(title: Int) {
    HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Label(text: Int) = Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)

@Composable
private fun Choices(options: List<Int>, selected: Int, onPick: (Int) -> Unit, label: @Composable (Int) -> String) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, value ->
            SegmentedButton(selected = value == selected, onClick = { onPick(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)) { Text(label(value), maxLines = 1) }
        }
    }
}

@Composable
private fun Avatar(photo: androidx.compose.ui.graphics.ImageBitmap?, name: String?) {
    val modifier = Modifier.size(56.dp).clip(CircleShape)
    if (photo != null) {
        Image(photo, contentDescription = stringResource(R.string.settings_photo), modifier = modifier,
            contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(name?.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun AlarmSoundRow(current: String?, onPick: (String?) -> Unit) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        @Suppress("DEPRECATION")
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        val default = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
        // "Padrão" or the same sound as the system default: keep following the phone's default.
        onPick(uri?.takeIf { it != default && it != Settings.System.DEFAULT_ALARM_ALERT_URI }?.toString())
    }
    val name = soundName(context, current) ?: stringResource(R.string.settings_alarm_sound_default)
    Row(Modifier.fillMaxWidth().clickable {
        picker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_ALARM_ALERT_URI)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current?.let(Uri::parse) ?: Settings.System.DEFAULT_ALARM_ALERT_URI))
    }.padding(vertical = 8.dp)) {
        Text(stringResource(R.string.settings_alarm_sound), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(name, color = MaterialTheme.colorScheme.primary)
    }
}

private fun soundName(context: Context, uri: String?): String? = uri?.let {
    runCatching { RingtoneManager.getRingtone(context, it.toUri())?.getTitle(context) }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyReminderRow(settings: AppSettings, onToggle: (Boolean) -> Unit, onTime: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_daily_reminder), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.settings_daily_reminder_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = settings.dailyReminderEnabled, onCheckedChange = onToggle)
    }
    if (settings.dailyReminderEnabled) {
        TextButton(onClick = { open = true }) {
            Text(stringResource(R.string.settings_daily_reminder_at, settings.dailyReminderTime.format(timeFormat)))
        }
    }
    if (open) {
        val picker = rememberTimePickerState(settings.dailyReminderTime.hour, settings.dailyReminderTime.minute, is24Hour = true)
        AlertDialog(onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = { onTime(LocalTime.of(picker.hour, picker.minute)); open = false }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(picker) })
    }
}

@Composable
private fun syncStatus(state: WorkInfo.State?, lastSync: Instant?): String = when {
    state == WorkInfo.State.RUNNING -> stringResource(R.string.settings_syncing)
    state != null -> stringResource(R.string.settings_sync_waiting)
    lastSync == null -> stringResource(R.string.settings_last_sync_never)
    else -> stringResource(R.string.settings_last_sync, lastSync.atZone(ZoneId.systemDefault()).format(dateTimeFormat))
}

@Composable
private fun PermissionsSummary(context: Context) {
    val status = rememberPermissionStatus()
    Label(R.string.permissions_title)
    listOf(
        R.string.permission_notifications to status.notifications,
        R.string.permission_full_screen to status.fullScreen,
        R.string.permission_exact_alarms to status.exactAlarms,
    ).forEach { (label, granted) ->
        if (granted == null) return@forEach
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(label), Modifier.weight(1f))
            Text(stringResource(if (granted) R.string.permission_granted else R.string.settings_permission_denied),
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }
    OutlinedButton(onClick = {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }) { Text(stringResource(R.string.settings_open_app_settings)) }
}
