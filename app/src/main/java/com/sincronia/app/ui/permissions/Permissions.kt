package com.sincronia.app.ui.permissions

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sincronia.app.R

/** Spec 7.5: what each permission looks like now. Null = not applicable on this Android version. */
data class PermissionStatus(val notifications: Boolean?, val fullScreen: Boolean?, val exactAlarms: Boolean?) {
    val alarmsBlocked: Boolean get() = notifications == false || exactAlarms == false
}

fun permissionStatus(context: Context): PermissionStatus = PermissionStatus(
    notifications = if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else null,
    fullScreen = if (Build.VERSION.SDK_INT >= 34) {
        context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    } else null,
    // USE_EXACT_ALARM covers Android 13+; only 12/12L needs the "Alarmes e lembretes" switch.
    exactAlarms = if (Build.VERSION.SDK_INT in 31..32) {
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    } else null,
)

/** Rechecked on every resume (spec 7.5: "Rechecar tudo no onResume da Hoje"). */
@Composable
fun rememberPermissionStatus(): PermissionStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(permissionStatus(context)) }
    LifecycleResumeEffect(Unit) {
        status = permissionStatus(context)
        onPauseOrDispose { }
    }
    return status
}

private fun Context.openAppSettings() = startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

private fun Context.openFullScreenSettings() {
    if (Build.VERSION.SDK_INT >= 34) startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
        Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT >= 31) startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Onboarding step after sign-in: each item shows "Permitido" or "Permitir". */
@Composable
fun PermissionsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(permissionStatus(context)) }
    LifecycleResumeEffect(Unit) {
        status = permissionStatus(context)
        onPauseOrDispose { }
    }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        status = permissionStatus(context)
        if (!it) context.openAppSettings()
    }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        Text(stringResource(R.string.permissions_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.permissions_text))
        status.notifications?.let { granted ->
            PermissionRow(stringResource(R.string.permission_notifications), granted) {
                if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        status.fullScreen?.let { granted ->
            PermissionRow(stringResource(R.string.permission_full_screen), granted) { context.openFullScreenSettings() }
        }
        status.exactAlarms?.let { granted ->
            PermissionRow(stringResource(R.string.permission_exact_alarms), granted) { context.openExactAlarmSettings() }
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.permissions_continue)) }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onAllow: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        if (granted) Text(stringResource(R.string.permission_granted), color = MaterialTheme.colorScheme.primary)
        else Button(onClick = onAllow) { Text(stringResource(R.string.permission_allow)) }
    }
}

/** Fixed banner on Today while alarms cannot appear (spec 7.5). */
@Composable
fun AlarmPermissionBanner(status: PermissionStatus) {
    val context = LocalContext.current
    // Most serious first: without notifications or exact alarms nothing rings; without full screen it rings as a notification.
    val (message, open) = when {
        status.notifications == false -> R.string.banner_notifications to { context.openAppSettings() }
        status.exactAlarms == false -> R.string.banner_exact_alarms to { context.openExactAlarmSettings() }
        status.fullScreen == false -> R.string.banner_full_screen to { context.openFullScreenSettings() }
        else -> return
    }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(stringResource(message), Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer)
        TextButton(onClick = open, modifier = Modifier.padding(start = 4.dp)) { Text(stringResource(R.string.permission_allow)) }
    }
}
