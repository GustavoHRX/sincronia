package com.sincronia.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sincronia.app.MainActivity
import com.sincronia.app.R
import com.sincronia.app.core.alarm.AlarmNotices
import com.sincronia.app.core.database.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Channels and the plain notifications around alarms (spec 7.4, "Notificação"). */
@Singleton
class AlarmNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
) : AlarmNotices {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ALARM,
            context.getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null) // the sound comes from AlarmService
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        })
        manager.createNotificationChannel(NotificationChannel(CHANNEL_GENERAL,
            context.getString(R.string.channel_general), NotificationManager.IMPORTANCE_DEFAULT))
        manager.createNotificationChannel(NotificationChannel(CHANNEL_SYNC,
            context.getString(R.string.channel_sync), NotificationManager.IMPORTANCE_LOW))
    }

    override fun late(alarmId: Int, itemType: String, itemLocalId: String) {
        scope.launch { post(alarmId, context.getString(R.string.alarm_late, title(alarmId))) }
    }

    fun missed(alarmId: Int, title: String) = post(alarmId, context.getString(R.string.alarm_missed, title))

    /** Previous alarm when a new one takes over the screen and the sound. */
    fun replaced(alarmId: Int, title: String) = post(alarmId, title)

    fun dailySummary(titles: List<String>) {
        if (titles.isEmpty()) return
        post(DAILY_ID, context.getString(R.string.daily_summary_title, titles.size), titles.joinToString("\n"))
    }

    suspend fun title(alarmId: Int): String {
        val alarm = database.alarmDao().byId(alarmId) ?: return context.getString(R.string.alarm_title_default)
        val title = if (alarm.itemType == "EVENT") database.eventDao().byLocalId(alarm.accountId, alarm.itemLocalId)?.title
        else database.taskDao().byLocalId(alarm.accountId, alarm.itemLocalId)?.title
        return title?.ifBlank { null } ?: context.getString(R.string.alarm_title_default)
    }

    fun openApp(): PendingIntent = PendingIntent.getActivity(context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Spec 6.4, step 6: consent needed while the app was in the background. Opens Today, which has the button. */
    fun reconnect() = post(RECONNECT_ID, context.getString(R.string.reconnect_title), context.getString(R.string.reconnect_text))

    fun cancelReconnect() = NotificationManagerCompat.from(context).cancel(NOTICE_BASE + RECONNECT_ID)

    private fun post(id: Int, title: String, text: String? = null) {
        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .apply { text?.let { setContentText(it).setStyle(NotificationCompat.BigTextStyle().bigText(it)) } }
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
        notifyIfAllowed(context, NOTICE_BASE + id, notification)
    }

    companion object {
        /** Without POST_NOTIFICATIONS (Android 13+) nothing can be shown; Today's banner explains why. */
        fun notifyIfAllowed(context: Context, id: Int, notification: android.app.Notification) {
            if (android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
            try {
                NotificationManagerCompat.from(context).notify(id, notification)
            } catch (_: SecurityException) {
            }
        }

        const val CHANNEL_ALARM = "alarm"
        const val CHANNEL_GENERAL = "geral"
        const val CHANNEL_SYNC = "sync"
        const val DAILY_ID = Int.MAX_VALUE - 1
        const val TEST_ID = Int.MAX_VALUE - 2
        private const val RECONNECT_ID = -1
        /** Plain notices use ids apart from the ringing alarm notification. */
        private const val NOTICE_BASE = 100_000
    }
}
