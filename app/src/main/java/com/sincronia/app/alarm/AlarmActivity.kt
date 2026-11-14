package com.sincronia.app.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sincronia.app.R
import com.sincronia.app.ui.theme.SincronIATheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Full screen over the lock screen: time, title, type and the two buttons (spec 7.4). */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            SincronIATheme {
                val ringing by AlarmService.current.collectAsState()
                LaunchedEffect(ringing) { if (ringing == null) finish() }
                val ring = ringing ?: return@SincronIATheme
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ring.at.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.displayLarge)
                        Text(ring.title, style = MaterialTheme.typography.headlineMedium)
                        Text(ring.type, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(onClick = { send(AlarmService.ACTION_SNOOZE, ring.alarmId) }) {
                                Text(stringResource(R.string.alarm_snooze))
                            }
                            Button(onClick = { send(AlarmService.ACTION_DISMISS, ring.alarmId) }) {
                                Text(stringResource(R.string.alarm_dismiss))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun send(action: String, alarmId: Int) {
        startService(AlarmService.intent(this, action, alarmId))
        finish()
    }
}
