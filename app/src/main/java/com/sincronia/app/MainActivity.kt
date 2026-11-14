package com.sincronia.app

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.sincronia.app.core.auth.AccountStore
import com.sincronia.app.data.demo.DemoPreferences
import com.sincronia.app.data.sync.SyncScheduler
import com.sincronia.app.navigation.SincroniaNavHost
import com.sincronia.app.ui.theme.SincronIATheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var demoPreferences: DemoPreferences
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var settings: com.sincronia.app.data.settings.SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SincronIATheme {
                SincroniaNavHost(demoPreferences, accountStore, settings)
            }
        }
        lifecycleScope.launch {
            if (demoPreferences.enabled.first() || accountStore.current() != null) {
                SyncScheduler.enqueue(this@MainActivity)
                SyncScheduler.schedulePeriodic(this@MainActivity, settings.current().syncIntervalMinutes.toLong())
                SyncScheduler.scheduleAlarmRefresh(this@MainActivity)
            }
        }
    }
}
