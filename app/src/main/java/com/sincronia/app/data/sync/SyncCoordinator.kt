package com.sincronia.app.data.sync

import androidx.room.withTransaction
import com.sincronia.app.alarm.AlarmNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import com.sincronia.app.core.alarm.AlarmScheduler
import com.sincronia.app.core.auth.AccountStore
import com.sincronia.app.core.auth.AuthState
import com.sincronia.app.core.auth.AuthorizationOutcome
import com.sincronia.app.core.auth.GoogleAuthManager
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.log.AppLogger
import java.io.IOException
import com.sincronia.app.data.calendar.CalendarSource
import com.sincronia.app.data.demo.DemoPreferences
import com.sincronia.app.data.settings.SettingsRepository
import com.sincronia.app.data.tasks.TaskSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Spec 7.2 pipeline: authorize -> outbox -> download -> alarms. Outbox upload is M4, alarms are M5. */
@Singleton
class SyncCoordinator @Inject constructor(
    private val database: AppDatabase,
    private val calendar: CalendarSource,
    private val tasks: TaskSource,
    private val preferences: DemoPreferences,
    private val accounts: AccountStore,
    private val auth: GoogleAuthManager,
    private val googleSync: GoogleSync,
    private val outbox: OutboxProcessor,
    private val alarms: AlarmScheduler,
    private val settings: SettingsRepository,
    private val logger: AppLogger,
    private val notifier: AlarmNotifier,
) {
    private val mutex = Mutex()

    suspend fun run() = mutex.withLock {
        val demo = preferences.enabled.first()
        try {
            val synced = if (demo) { runDemo(); true } else runGoogle()
            // "Sincronizar agora" in Ajustes shows this time (F10); it also clears the failure banner.
            if (synced) settings.markSynced(Instant.now())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            // M7: Today shows "Não foi possível sincronizar" until the next sync reaches Google.
            settings.markSyncFailed()
            throw error
        } finally {
            // 4. Recalculate and reschedule the alarms (spec 7.4), also after a failed download.
            withContext(NonCancellable) {
                alarms.rescheduleAll(if (demo) "demo" else accounts.current()?.accountId)
            }
        }
    }

    /** True when the download reached Google. */
    private suspend fun runGoogle(): Boolean {
        val account = accounts.current() ?: return false
        return when (auth.authorize()) {
            is AuthorizationOutcome.Authorized -> {
                notifier.cancelReconnect()
                outbox.process(account.accountId)
                googleSync.download(account.accountId, LocalDate.now(), ZoneId.systemDefault())
                // Edits made during the download go up before the worker ends (spec 7.2).
                if (outbox.hasReady(account.accountId)) outbox.process(account.accountId)
                true
            }
            // Spec 6.4, step 6: keep everything, pause the sync; Today shows the banner, plus one notification.
            is AuthorizationOutcome.NeedsConsent -> {
                if (account.authState != AuthState.NEEDS_REAUTH) notifier.reconnect()
                accounts.setAuthState(AuthState.NEEDS_REAUTH)
                logger.w(TAG, "Consent needed, sync paused")
                false
            }
            is AuthorizationOutcome.MissingScopes -> { logger.w(TAG, "Scopes missing, sync skipped"); false }
            AuthorizationOutcome.SignedOut -> false
            AuthorizationOutcome.Failed -> throw IOException("Authorization failed")
        }
    }

    private suspend fun runDemo() {
        val accountId = "demo"
        // 1. Demo authorization is skipped. 2. Outbox has no network processor until M4.
        // 3. Fetch complete snapshots first, then apply together; keep pending local edits.
        val today = LocalDate.now()
        val events = calendar.events(today, ZoneId.systemDefault())
        val lists = tasks.lists()
        val downloadedTasks = tasks.tasks(today)
        database.withTransaction {
            val outbox = database.outboxDao().blockingDownload(accountId)
            val pending = outbox.map { it.itemType to it.itemLocalId }.toSet()
            val eventIds = events.map { it.googleId }.toSet()
            val taskIds = downloadedTasks.map { it.googleId }.toSet()
            database.eventDao().upsert(events.filter { ("EVENT" to it.localId) !in pending })
            database.taskListDao().upsert(lists)
            database.taskDao().upsert(downloadedTasks.filter { ("TASK" to it.localId) !in pending })
            database.eventDao().synced(accountId).filter { it.googleId !in eventIds && ("EVENT" to it.localId) !in pending }
                .takeIf { it.isNotEmpty() }?.let { stale ->
                    database.eventDao().upsert(stale.map { it.copy(deleted = true) })
                }
            // The demo task source is a complete snapshot, unlike future incremental Google downloads.
            database.taskDao().all(accountId).filter { it.googleId !in taskIds && it.syncState == "SYNCED" && ("TASK" to it.localId) !in pending }
                .takeIf { it.isNotEmpty() }?.let { stale ->
                    database.taskDao().upsert(stale.map { it.copy(deleted = true) })
                }
        }
    }

    private companion object {
        const val TAG = "Sync"
    }
}
