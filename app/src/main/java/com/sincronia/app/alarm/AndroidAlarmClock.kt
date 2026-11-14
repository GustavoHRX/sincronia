package com.sincronia.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sincronia.app.core.alarm.AlarmClock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

/** setAlarmClock with an explicit intent, requestCode = AlarmEntity.id (spec 7.4). */
class AndroidAlarmClock @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notifier: AlarmNotifier,
) : AlarmClock {
    private val manager get() = context.getSystemService(AlarmManager::class.java)

    override fun schedule(alarmId: Int, trigger: Instant, itemType: String, itemLocalId: String): Boolean {
        // Never schedule the past: Android would fire it at once.
        if (trigger <= Instant.now()) return false
        return try {
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger.toEpochMilli(), notifier.openApp()), fire(alarmId))
            true
        } catch (_: SecurityException) {
            false // "Alarmes e lembretes" denied on Android 12/12L; the Today banner explains it.
        }
    }

    override fun cancel(alarmId: Int) = manager.cancel(fire(alarmId))

    override fun scheduleDailySummary(trigger: Instant?) {
        val intent = PendingIntent.getBroadcast(context, AlarmNotifier.DAILY_ID,
            Intent(context, DailySummaryReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        if (trigger == null) manager.cancel(intent)
        else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toEpochMilli(), intent)
    }

    /** Debug "Testar alarme em 10 s". */
    fun scheduleTest(): Boolean = try {
        val at = System.currentTimeMillis() + 10_000
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(at, notifier.openApp()), fire(AlarmNotifier.TEST_ID))
        true
    } catch (_: SecurityException) {
        false
    }

    private fun fire(alarmId: Int): PendingIntent = PendingIntent.getBroadcast(context, alarmId,
        Intent(context, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}
