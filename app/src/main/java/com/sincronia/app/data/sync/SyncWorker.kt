package com.sincronia.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
    } catch (_: Exception) {
        Result.retry()
    }
}

object SyncScheduler {
    fun enqueue(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync", ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<SyncWorker>().build(),
        )
    }
}
