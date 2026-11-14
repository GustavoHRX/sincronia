package com.sincronia.app.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.sincronia.app.alarm.AndroidAlarmClock
import com.sincronia.app.core.alarm.AlarmScheduler
import com.sincronia.app.core.auth.AccountStore
import com.sincronia.app.core.auth.AuthState
import com.sincronia.app.core.log.AppLogger
import com.sincronia.app.core.log.DiagnosticsExporter
import com.sincronia.app.data.AccountSession
import com.sincronia.app.data.ActiveAccount
import com.sincronia.app.data.demo.DemoPreferences
import com.sincronia.app.data.settings.AppSettings
import com.sincronia.app.data.settings.SettingsRepository
import com.sincronia.app.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URL
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AccountUi(
    val name: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val demo: Boolean = false,
    val needsReauth: Boolean = false,
)

data class SettingsUiState(
    val account: AccountUi = AccountUi(),
    val settings: AppSettings = AppSettings(),
    val lastSyncAt: Instant? = null,
    /** RUNNING, ENQUEUED (waiting for internet) or null. */
    val syncState: WorkInfo.State? = null,
    val disconnecting: Boolean = false,
    val exporting: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    accounts: AccountStore,
    demo: DemoPreferences,
    private val settingsRepository: SettingsRepository,
    private val activeAccount: ActiveAccount,
    private val alarms: AlarmScheduler,
    private val session: AccountSession,
    private val clock: AndroidAlarmClock,
    private val diagnostics: DiagnosticsExporter,
    private val logger: AppLogger,
) : ViewModel() {
    private val disconnecting = MutableStateFlow(false)
    private val exporting = MutableStateFlow(false)
    private val shareChannel = Channel<Uri>(Channel.BUFFERED)

    /** content:// URI of a diagnostics file ready for the share sheet. */
    val shareRequests = shareChannel.receiveAsFlow()

    private val accountUi = combine(accounts.account, demo.enabled) { account, isDemo ->
        AccountUi(account?.displayName, account?.email, account?.photoUrl, isDemo, account?.authState == AuthState.NEEDS_REAUTH)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        accountUi,
        settingsRepository.settings,
        settingsRepository.lastSyncAt,
        SyncScheduler.manualSyncState(context),
        combine(disconnecting, exporting) { d, e -> d to e },
    ) { account, settings, lastSync, syncState, (busy, exportBusy) ->
        SettingsUiState(account, settings, lastSync, syncState, busy, exportBusy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** Google profile photo, loaded once per URL; null shows the initial instead. */
    val photo: StateFlow<Bitmap?> = accounts.account.map { it?.photoUrl }.distinctUntilChanged().map { url ->
        url?.let { loadPhoto(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private suspend fun loadPhoto(url: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            (URL(url).openConnection().apply { connectTimeout = 5_000; readTimeout = 5_000 }).getInputStream()
                .use(BitmapFactory::decodeStream)
        }.onFailure { logger.w(TAG, "Profile photo not loaded", it) }.getOrNull()
    }

    fun setNewEventAlarm(minutes: Int) = save { it.copy(newEventAlarmMinutes = minutes) }

    fun setSnooze(minutes: Int) = save { it.copy(snoozeMinutes = minutes) }

    /** The daily summary is an alarm: reschedule right away (M6, "valem no próximo alarme"). */
    fun setDailyReminder(enabled: Boolean) = save(rescheduleAlarms = true) { it.copy(dailyReminderEnabled = enabled) }

    fun setDailyReminderTime(time: LocalTime) = save(rescheduleAlarms = true) {
        it.copy(dailyReminderEnabled = true, dailyReminderTime = time)
    }

    /** Null = the phone's default alarm sound. */
    fun setAlarmSound(uri: String?) = save { it.copy(alarmSoundUri = uri) }

    fun setSyncInterval(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(syncIntervalMinutes = minutes) }
            SyncScheduler.schedulePeriodic(context, minutes.toLong())
            logger.i(TAG, "Sync interval $minutes min")
        }
    }

    fun syncNow() = SyncScheduler.enqueue(context)

    private fun save(rescheduleAlarms: Boolean = false, change: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepository.update(change)
            if (rescheduleAlarms) alarms.rescheduleAll(activeAccount.current())
        }
    }

    fun exportDiagnostics() {
        if (exporting.value) return
        exporting.value = true
        viewModelScope.launch {
            try {
                shareChannel.send(diagnostics.export())
                logger.i(TAG, "Diagnostics exported")
            } catch (e: java.io.IOException) {
                logger.e(TAG, "Diagnostics export failed", e)
            } catch (e: IllegalArgumentException) {
                logger.e(TAG, "Diagnostics export failed", e)
            } finally {
                exporting.value = false
            }
        }
    }

    /** Debug "Testar alarme em 10 s" (spec 8, M5). */
    fun testAlarm(): Boolean = clock.scheduleTest()

    fun disconnect() {
        if (disconnecting.value) return
        disconnecting.value = true
        viewModelScope.launch {
            try {
                session.disconnect()
            } finally {
                disconnecting.value = false
            }
        }
    }

    private companion object {
        const val TAG = "Settings"
    }
}
