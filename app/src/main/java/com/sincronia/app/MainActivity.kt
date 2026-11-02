package com.sincronia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SincronIATheme {
                SincroniaNavHost(demoPreferences)
            }
        }
        lifecycleScope.launch {
            if (demoPreferences.enabled.first()) SyncScheduler.enqueue(this@MainActivity)
        }
    }
}
