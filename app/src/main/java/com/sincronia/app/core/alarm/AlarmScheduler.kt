package com.sincronia.app.core.alarm

import androidx.room.withTransaction
import com.sincronia.app.core.database.AlarmEntity
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.data.settings.SettingsRepository
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** AlarmManager seen through what the scheduler needs; a fake replaces it in tests. */
interface AlarmClock {
    /** Exact alarm (setAlarmClock); requestCode = [alarmId]. False if the system refused it. */
    fun schedule(alarmId: Int, trigger: Instant, itemType: String, itemLocalId: String): Boolean
    fun cancel(alarmId: Int)
    /** Inexact daily summary (setAndAllowWhileIdle); null cancels it. */
    fun scheduleDailySummary(trigger: Instant?)
}

/** Plain notifications that are not alarms. */
interface AlarmNotices {
    /** Alarm time passed before the event started (spec 7.4). */
    fun late(alarmId: Int, itemType: String, itemLocalId: String)
}

/**
 * Spec 7.4: rescheduleAll() compares what is in AlarmManager (`scheduled`) with what should be,
 * cancels what left and schedules what is new or changed. Calling it twice changes nothing.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    private val database: AppDatabase,
    private val clock: AlarmClock,
    private val notices: AlarmNotices,
    private val settings: SettingsRepository,
) {
    private val mutex = Mutex()

    private data class Candidate(val itemType: String, val localId: String, val raw: Instant?, val eventStart: Instant?)

    suspend fun rescheduleAll(accountId: String?, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()) =
        mutex.withLock {
            if (accountId == null) return@withLock cancelAll()
            val today = now.atZone(zone).toLocalDate()
            val defaults = database.syncMetaDao().value(accountId, "$accountId:calendar:primary:defaultReminders")
                ?.let { runCatching { Json.decodeFromString(ListSerializer(Int.serializer()), it) }.getOrNull() }.orEmpty()
            val alarms = database.alarmDao().all(accountId).associateBy { it.itemType to it.itemLocalId }
            val seriesPrefix = "$accountId:${AlarmPlanner.SERIES_OFF_PREFIX}"
            val seriesOff = database.syncMetaDao().keysLike(accountId, "$seriesPrefix%").map { it.removePrefix(seriesPrefix) }.toSet()

            // Google reminders go up to 4 weeks before the start, so look that far for events.
            val events = database.eventDao().alarmCandidates(accountId, now.minus(Duration.ofDays(1)),
                now.plus(AlarmPlanner.WINDOW).plus(Duration.ofDays(28)), today.minusDays(1), today.plusDays(8))
            val tasks = database.taskDao().alarmCandidates(accountId, today.minusDays(1), today.plusDays(8))
            val candidates = events.map {
                Candidate(EVENT, it.localId, AlarmPlanner.eventTrigger(it, alarms[EVENT to it.localId], defaults, zone,
                    seriesOff = it.recurringEventId in seriesOff), it.startAt)
            } + tasks.map {
                Candidate(TASK, it.localId, AlarmPlanner.taskTrigger(it, alarms[TASK to it.localId], zone), null)
            }

            val desired = mutableListOf<Pair<Candidate, Instant>>()
            for (candidate in candidates) {
                val alarm = alarms[candidate.itemType to candidate.localId]
                val trigger = AlarmPlanner.effectiveTrigger(candidate.raw, alarm, now)
                if (AlarmPlanner.shouldSchedule(trigger, alarm, now)) {
                    desired += candidate to trigger!!
                } else if (AlarmPlanner.isLateNotice(trigger, alarm, candidate.eventStart, now)) {
                    val row = rowFor(accountId, candidate, alarm)
                    database.alarmDao().upsert(row.copy(dismissedTriggerAt = trigger, triggerAt = trigger))
                    notices.late(row.id, candidate.itemType, candidate.localId)
                }
            }
            val keep = desired.sortedBy { it.second }.take(AlarmPlanner.MAX_SCHEDULED)
            val keptIds = mutableSetOf<Int>()
            database.withTransaction {
                for ((candidate, trigger) in keep) {
                    val current = rowFor(accountId, candidate, alarms[candidate.itemType to candidate.localId])
                    keptIds += current.id
                    if (current.scheduled && current.triggerAt == trigger) continue
                    val ok = clock.schedule(current.id, trigger, candidate.itemType, candidate.localId)
                    database.alarmDao().upsert(current.copy(
                        triggerAt = trigger, scheduled = ok,
                        dismissedTriggerAt = if (current.triggerAt != trigger) null else current.dismissedTriggerAt,
                    ))
                }
                for (row in database.alarmDao().scheduled().filter { it.id !in keptIds }) {
                    clock.cancel(row.id)
                    database.alarmDao().upsert(row.copy(scheduled = false))
                }
            }
            val config = settings.current()
            clock.scheduleDailySummary(if (config.dailyReminderEnabled) {
                val time = today.atTime(config.dailyReminderTime).atZone(zone).toInstant()
                if (time > now) time else today.plusDays(1).atTime(config.dailyReminderTime).atZone(zone).toInstant()
            } else null)
        }

    /**
     * A reboot empties AlarmManager, but the table still says "scheduled": forget that, so the next
     * rescheduleAll() registers every alarm again instead of skipping it as already registered.
     */
    suspend fun systemAlarmsCleared() = mutex.withLock {
        for (row in database.alarmDao().scheduled()) database.alarmDao().upsert(row.copy(scheduled = false))
    }

    /** Desconectar (spec 6.4, step 8) and signed-out state. */
    suspend fun cancelAll() {
        for (row in database.alarmDao().scheduled()) {
            clock.cancel(row.id)
            database.alarmDao().upsert(row.copy(scheduled = false))
        }
        clock.scheduleDailySummary(null)
    }

    /** Existing row, or a new one: its autoincrement id is the PendingIntent requestCode. */
    private suspend fun rowFor(accountId: String, candidate: Candidate, existing: AlarmEntity?): AlarmEntity {
        existing?.let { return database.alarmDao().byId(it.id) ?: it }
        val id = database.alarmDao().upsert(AlarmEntity(accountId = accountId, itemType = candidate.itemType,
            itemLocalId = candidate.localId, mode = AlarmMode.AUTO))
        return database.alarmDao().byId(id.toInt())!!
    }

    private companion object {
        const val EVENT = "EVENT"
        const val TASK = "TASK"
    }
}
