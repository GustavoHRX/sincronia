package com.sincronia.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.sincronia.app.core.alarm.AlarmScheduler
import com.sincronia.app.core.network.isTransient
import com.sincronia.app.data.ActiveAccount
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val coordinator: SyncCoordinator,
) : CoroutineWorker(context, params) {
    override suspend fun doWork() = try {
        coordinator.run()
        Result.success()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        // Transient errors wait for WorkManager's backoff; final ones wait for the next sync.
        if (isTransient(error)) Result.retry() else Result.success()
    }
}

/** Daily, without network: completes the 7-day alarm window even offline (spec 7.4). */
@HiltWorker
class AlarmRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val alarms: AlarmScheduler,
    private val account: ActiveAccount,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        alarms.rescheduleAll(account.current())
        return Result.success()
    }
}

object SyncScheduler {
    private const val ONE_TIME = "sync"
    private const val PERIODIC = "sync-periodic"
    private const val ALARMS = "alarms-daily"
    private const val ALARMS_NOW = "alarms-now"
    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Opening the app, pull to refresh and (from M4) every outbox write. */
    fun enqueue(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(network).build(),
        )
    }

    /** Spec 7.2: every 15, 30 or 60 min, from Ajustes (UPDATE keeps the next run when the interval is unchanged). */
    fun schedulePeriodic(context: Context, minutes: Long = 30) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SyncWorker>(minutes, TimeUnit.MINUTES).setConstraints(network).build(),
        )
    }

    /** "Sincronizar agora" (F10): RUNNING, ENQUEUED (waiting for internet) or null when idle. */
    fun manualSyncState(context: Context): Flow<WorkInfo.State?> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(ONE_TIME).map { infos ->
            infos.firstOrNull { !it.state.isFinished }?.state
        }

    fun scheduleAlarmRefresh(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ALARMS, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<AlarmRefreshWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    /** One-off run of [AlarmRefreshWorker] (after a reboot), alongside the daily one. */
    fun refreshAlarmsNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            ALARMS_NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<AlarmRefreshWorker>().build(),
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(ONE_TIME)
            cancelUniqueWork(PERIODIC)
            cancelUniqueWork(ALARMS)
            cancelUniqueWork(ALARMS_NOW)
        }
    }
}
