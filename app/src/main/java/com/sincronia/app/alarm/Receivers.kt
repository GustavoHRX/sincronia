package com.sincronia.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.sincronia.app.core.alarm.AlarmScheduler
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.data.ActiveAccount
import com.sincronia.app.data.sync.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Exact alarm fired: the system allows starting a foreground service from here (spec 7.4). */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        if (id == -1) return
        ContextCompat.startForegroundService(context, AlarmService.fire(context, id))
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
    }
}

private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/**
 * Reboot, app update, time and time zone changes, exact-alarm permission changes: reschedule.
 * Never starts AlarmService here (Android 15 forbids mediaPlayback after BOOT_COMPLETED); an alarm
 * that passed while the phone was off becomes "Alarme perdido".
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var database: AppDatabase
    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var notifier: AlarmNotifier
    @Inject lateinit var account: ActiveAccount

    override fun onReceive(context: Context, intent: Intent) = runAsync {
        val now = Instant.now()
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Second pass outside this broadcast's time budget (WorkManager), in case this one is cut short.
            SyncScheduler.refreshAlarmsNow(context)
            for (row in database.alarmDao().scheduled()) {
                val trigger = row.triggerAt ?: continue
                if (trigger <= now) {
                    notifier.missed(row.id, notifier.title(row.id))
                    database.alarmDao().upsert(row.copy(scheduled = false, dismissedTriggerAt = trigger, snoozedUntil = null))
                }
            }
        }
        // A reboot empties AlarmManager; for the other actions registering again is harmless (same
        // request code replaces), so never trust the table's "scheduled" here.
        scheduler.systemAlarmsCleared()
        // Waits for the stored account: a slow read right after boot must not look like "signed out".
        scheduler.rescheduleAll(account.current(), now)
    }
}

/** Optional daily reminder of today's tasks without a time (spec 7.4); off by default. */
@AndroidEntryPoint
class DailySummaryReceiver : BroadcastReceiver() {
    @Inject lateinit var database: AppDatabase
    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var notifier: AlarmNotifier
    @Inject lateinit var account: ActiveAccount

    override fun onReceive(context: Context, intent: Intent) = runAsync {
        val accountId = account.current() ?: return@runAsync
        val today = LocalDate.now()
        val timed = database.alarmDao().all(accountId).filter { it.itemType == "TASK" && it.mode == "TIME" }
            .map { it.itemLocalId }.toSet()
        val titles = database.taskDao().alarmCandidates(accountId, today, today.plusDays(1))
            .filter { it.localId !in timed }.map { it.title }
        notifier.dailySummary(titles)
        scheduler.rescheduleAll(accountId)
    }
}
