package com.sincronia.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.sincronia.app.R
import com.sincronia.app.core.alarm.AlarmScheduler
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.data.ActiveAccount
import com.sincronia.app.data.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What is ringing now; AlarmActivity shows it and closes when it becomes null. */
data class Ringing(val alarmId: Int, val title: String, val type: String, val at: Instant)

/**
 * Spec 7.4, "Como tocar": sound and vibration live here, never in the Activity. mediaPlayback
 * foreground service (not shortService), stops after 10 minutes as "Alarme perdido".
 */
@AndroidEntryPoint
class AlarmService : Service() {
    @Inject lateinit var database: AppDatabase
    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var notifier: AlarmNotifier
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var account: ActiveAccount

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private val timeout = Runnable { ringing.value?.let { missed(it) } }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1) ?: -1
        when (intent?.action) {
            ACTION_FIRE -> fire(id)
            ACTION_SNOOZE -> finish(id, snooze = true)
            ACTION_DISMISS -> finish(id, snooze = false)
            else -> if (ringing.value == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun fire(alarmId: Int) {
        // startForeground within 5 s, before any database read.
        val generic = getString(R.string.alarm_title_default)
        startForegroundCompat(notification(Ringing(alarmId, generic, "", Instant.now())))
        scope.launch {
            val previous = ringing.value
            val isTest = alarmId == AlarmNotifier.TEST_ID
            val row = if (isTest) null else database.alarmDao().byId(alarmId)
            val title = if (isTest) getString(R.string.alarm_test_title) else notifier.title(alarmId)
            val type = when {
                isTest -> ""
                row?.itemType == "TASK" -> getString(R.string.editor_task)
                else -> getString(R.string.editor_event)
            }
            row?.let { database.alarmDao().upsert(it.copy(scheduled = false)) }
            // Two at once: the new one takes the screen and the sound, the old one becomes a notification.
            if (previous != null && previous.alarmId != alarmId) notifier.replaced(previous.alarmId, previous.title)
            val now = Ringing(alarmId, title, type, Instant.now())
            ringing.value = now
            AlarmNotifier.notifyIfAllowed(this@AlarmService, NOTIFICATION_ID, notification(now))
            startSound(settings.current().alarmSoundUri)
            handler.removeCallbacks(timeout)
            handler.postDelayed(timeout, RING_LIMIT.toMillis())
        }
    }

    private fun finish(alarmId: Int, snooze: Boolean) {
        val current = ringing.value
        stopSound()
        ringing.value = null
        scope.launch {
            if (alarmId != AlarmNotifier.TEST_ID) {
                database.alarmDao().byId(alarmId)?.let { row ->
                    val minutes = settings.current().snoozeMinutes.toLong()
                    database.alarmDao().upsert(if (snooze) {
                        row.copy(snoozedUntil = Instant.now().plus(Duration.ofMinutes(minutes)), scheduled = false)
                    } else {
                        row.copy(dismissedTriggerAt = row.snoozedUntil ?: row.triggerAt, snoozedUntil = null, scheduled = false)
                    })
                }
            } else if (snooze && current != null) {
                // The test alarm has no row; snoozing it simply ends it.
            }
            scheduler.rescheduleAll(account.current())
            stopForegroundAndSelf()
        }
    }

    private fun missed(current: Ringing) {
        stopSound()
        ringing.value = null
        notifier.missed(current.alarmId, current.title)
        stopForegroundAndSelf()
    }

    /** Sound picked in Ajustes (F10); if it was deleted or cannot play, the phone's default alarm. */
    private fun startSound(chosen: String?) {
        stopSound()
        val fallback = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val candidates = listOfNotNull(chosen?.let(Uri::parse), fallback).distinct()
        player = candidates.firstNotNullOfOrNull { uri -> play(uri) }
        val vibrator = getSystemService(Vibrator::class.java)
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 1)
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }

    private fun play(uri: Uri): MediaPlayer? {
        val media = MediaPlayer()
        return runCatching {
            media.apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setWakeMode(this@AlarmService, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrElse { media.release(); null }
    }

    private fun stopSound() {
        handler.removeCallbacks(timeout)
        player?.runCatching { stop(); release() }
        player = null
        getSystemService(Vibrator::class.java).cancel()
    }

    private fun notification(ring: Ringing): Notification {
        val full = PendingIntent.getActivity(this, ring.alarmId,
            Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, AlarmNotifier.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(ring.title)
            .setContentText(ring.type)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .addAction(0, getString(R.string.alarm_snooze), action(ACTION_SNOOZE, ring.alarmId))
            .addAction(0, getString(R.string.alarm_dismiss), action(ACTION_DISMISS, ring.alarmId))
            .build()
    }

    private fun action(action: String, alarmId: Int) = PendingIntent.getService(this, alarmId * 2 + if (action == ACTION_SNOOZE) 0 else 1,
        intent(this, action, alarmId), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun startForegroundCompat(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun stopForegroundAndSelf() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSound()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_FIRE = "com.sincronia.app.alarm.FIRE"
        const val ACTION_SNOOZE = "com.sincronia.app.alarm.SNOOZE"
        const val ACTION_DISMISS = "com.sincronia.app.alarm.DISMISS"
        private const val NOTIFICATION_ID = 42
        private val RING_LIMIT: Duration = Duration.ofMinutes(10)

        private val ringing = MutableStateFlow<Ringing?>(null)
        val current: StateFlow<Ringing?> = ringing.asStateFlow()

        fun intent(context: Context, action: String, alarmId: Int): Intent =
            Intent(context, AlarmService::class.java).setAction(action).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)

        fun fire(context: Context, alarmId: Int) = intent(context, ACTION_FIRE, alarmId)
    }
}
