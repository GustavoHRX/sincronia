package com.sincronia.app.ui.today

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.sincronia.app.alarm.AlarmNotifier
import com.sincronia.app.core.auth.AccountStore
import com.sincronia.app.core.auth.AuthState
import com.sincronia.app.core.auth.AuthorizationOutcome
import com.sincronia.app.core.auth.GoogleAuthManager
import com.sincronia.app.data.settings.SettingsRepository
import com.sincronia.app.data.sync.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import com.sincronia.app.core.database.EventEntity
import com.sincronia.app.core.database.TaskEntity
import com.sincronia.app.core.log.AppLogger
import com.sincronia.app.data.ActiveAccount
import com.sincronia.app.data.edit.EditRepository
import com.sincronia.app.data.calendar.EventRepository
import com.sincronia.app.data.sync.SyncCoordinator
import com.sincronia.app.data.tasks.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    events: EventRepository,
    tasks: TaskRepository,
    private val sync: SyncCoordinator,
    private val logger: AppLogger,
    activeAccount: ActiveAccount,
    private val edits: EditRepository,
    private val alarms: com.sincronia.app.core.database.AlarmDao,
    accounts: AccountStore,
    settings: SettingsRepository,
    private val auth: GoogleAuthManager,
    private val notifier: AlarmNotifier,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val zone = ZoneId.systemDefault()

    // Ticks at every minute boundary: moves the "now" line and switches to the next day after midnight.
    private val clock = flow {
        while (true) {
            val now = Instant.now()
            emit(now)
            delay(MINUTE_MILLIS - now.toEpochMilli() % MINUTE_MILLIS)
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val today = clock.map { it.atZone(zone).toLocalDate() }.distinctUntilChanged()
    private val refreshing = MutableStateFlow(false)
    private val reconnect = MutableStateFlow(ReconnectState())

    private val day = combine(today, activeAccount.id) { date, accountId -> date to accountId }.flatMapLatest { (date, accountId) ->
        combine(
            events.day(accountId, date),
            tasks.overdue(accountId, date),
            tasks.day(accountId, date),
            tasks.completedOn(accountId, date, zone),
            alarms.observeTaskTimes(accountId),
        ) { dayEvents, overdue, todayTasks, completed, times ->
            DayItems(date, dayEvents, overdue, todayTasks, completed,
                times.mapNotNull { a -> a.localTime?.let { a.itemLocalId to it } }.toMap())
        }
    }

    val uiState: StateFlow<TodayUiState> = combine(day, clock, refreshing) { items, now, isRefreshing ->
        TodayPlanner.build(
            date = items.date,
            now = now,
            zone = zone,
            events = items.events,
            overdue = items.overdue,
            todayTasks = items.todayTasks,
            completed = items.completed,
            refreshing = isRefreshing,
            taskTimes = items.taskTimes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** M7 banners above the day: lost authorization (spec 6.4, step 6) and the last sync failing. */
    val status: StateFlow<TodayStatus> = combine(accounts.account, settings.syncFailed, reconnect) { account, failed, r ->
        TodayStatus(needsReconnect = account?.authState == AuthState.NEEDS_REAUTH, syncFailed = failed, reconnect = r)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayStatus())

    /** "Reconectar": asks Google again from the foreground; the consent screen is opened by TodayScreen. */
    fun reconnect() {
        if (reconnect.value.busy) return
        reconnect.value = ReconnectState(busy = true)
        viewModelScope.launch { applyAuthorization(auth.authorize()) }
    }

    fun consentLaunched() = reconnect.update { it.copy(consent = null) }

    fun onConsentResult(data: Intent?) {
        viewModelScope.launch { applyAuthorization(auth.onConsentResult(data)) }
    }

    private fun applyAuthorization(outcome: AuthorizationOutcome) {
        when (outcome) {
            // GoogleAuthManager marks the account AUTHORIZED: the banner goes away and the sync resumes.
            is AuthorizationOutcome.Authorized -> {
                notifier.cancelReconnect()
                SyncScheduler.enqueue(context)
                reconnect.value = ReconnectState()
            }
            is AuthorizationOutcome.NeedsConsent -> reconnect.value = ReconnectState(busy = true, consent = outcome.pendingIntent)
            else -> reconnect.value = ReconnectState(failed = true)
        }
    }

    /** Pull to refresh runs the same pipeline as SyncWorker; its Mutex avoids overlapping runs. */
    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            try {
                sync.run()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                logger.w(TAG, "Pull to refresh failed", error)
            } finally {
                refreshing.value = false
            }
        }
    }

    fun setCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch { edits.setCompleted(task, completed) }
    }

    data class ReconnectState(val busy: Boolean = false, val failed: Boolean = false, val consent: PendingIntent? = null)

    data class TodayStatus(val needsReconnect: Boolean = false, val syncFailed: Boolean = false, val reconnect: ReconnectState = ReconnectState())

    private data class DayItems(
        val date: LocalDate,
        val events: List<EventEntity>,
        val overdue: List<TaskEntity>,
        val todayTasks: List<TaskEntity>,
        val completed: List<TaskEntity>,
        val taskTimes: Map<String, java.time.LocalTime>,
    )

    private companion object {
        const val TAG = "Today"
        const val MINUTE_MILLIS = 60_000L
    }
}
