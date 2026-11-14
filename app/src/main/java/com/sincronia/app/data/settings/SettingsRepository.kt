package com.sincronia.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Defaults from the M5 row of spec 8, edited by the F10 screen (M6). */
data class AppSettings(
    val newEventAlarmMinutes: Int = 10,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: LocalTime = LocalTime.of(9, 0),
    val snoozeMinutes: Int = 10,
    val syncIntervalMinutes: Int = 30,
    /** Ringtone picked in Ajustes; null = the phone's default alarm sound. */
    val alarmSoundUri: String? = null,
) {
    companion object {
        val NEW_EVENT_ALARM_OPTIONS = listOf(0, 5, 10, 15, 30, 60)
        val SNOOZE_OPTIONS = listOf(5, 10, 15)
        val SYNC_INTERVAL_OPTIONS = listOf(15, 30, 60)
    }
}

private val Context.settingsDataStore by preferencesDataStore("settings")

@Singleton
class SettingsRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val newEventAlarm = intPreferencesKey("new_event_alarm_minutes")
    private val dailyEnabled = booleanPreferencesKey("daily_reminder_enabled")
    private val dailyTime = stringPreferencesKey("daily_reminder_time")
    private val snooze = intPreferencesKey("snooze_minutes")
    private val syncInterval = intPreferencesKey("sync_interval_minutes")
    private val alarmSound = stringPreferencesKey("alarm_sound_uri")
    private val permissionsSeen = booleanPreferencesKey("permissions_seen")
    private val lastSync = stringPreferencesKey("last_sync_at")
    private val lastSyncFailed = booleanPreferencesKey("last_sync_failed")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            newEventAlarmMinutes = p[newEventAlarm] ?: d.newEventAlarmMinutes,
            dailyReminderEnabled = p[dailyEnabled] ?: d.dailyReminderEnabled,
            dailyReminderTime = p[dailyTime]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: d.dailyReminderTime,
            snoozeMinutes = p[snooze] ?: d.snoozeMinutes,
            syncIntervalMinutes = p[syncInterval] ?: d.syncIntervalMinutes,
            alarmSoundUri = p[alarmSound],
        )
    }

    suspend fun current(): AppSettings = settings.first()

    /** Onboarding step "Permissões" already shown (spec 7.5). */
    val permissionsStepSeen: Flow<Boolean> = context.settingsDataStore.data.map { it[permissionsSeen] ?: false }

    suspend fun markPermissionsSeen() {
        context.settingsDataStore.edit { it[permissionsSeen] = true }
    }

    /** End of the last sync that reached Google (or the demo sources) without error. */
    val lastSyncAt: Flow<Instant?> = context.settingsDataStore.data.map { p ->
        p[lastSync]?.let { runCatching { Instant.parse(it) }.getOrNull() }
    }

    suspend fun markSynced(at: Instant) {
        context.settingsDataStore.edit {
            it[lastSync] = at.toString()
            it.remove(lastSyncFailed)
        }
    }

    /** M7: the last sync attempt ended in an error (offline, Google down); cleared by the next success. */
    val syncFailed: Flow<Boolean> = context.settingsDataStore.data.map { it[lastSyncFailed] ?: false }

    suspend fun markSyncFailed() {
        context.settingsDataStore.edit { it[lastSyncFailed] = true }
    }

    suspend fun update(change: (AppSettings) -> AppSettings) {
        val next = change(current())
        context.settingsDataStore.edit {
            it[newEventAlarm] = next.newEventAlarmMinutes
            it[dailyEnabled] = next.dailyReminderEnabled
            it[dailyTime] = next.dailyReminderTime.toString()
            it[snooze] = next.snoozeMinutes
            it[syncInterval] = next.syncIntervalMinutes
            if (next.alarmSoundUri != null) it[alarmSound] = next.alarmSoundUri else it.remove(alarmSound)
        }
    }

    suspend fun clear() {
        context.settingsDataStore.edit { it.clear() }
    }
}
