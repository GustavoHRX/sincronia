package com.sincronia.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sincronia.app.BuildConfig
import com.sincronia.app.R

@Composable
fun DemoOnboarding(onEnter: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.welcome_message), modifier = Modifier.padding(vertical = 24.dp))
        if (BuildConfig.DEBUG) {
            Button(onClick = onEnter) { Text(stringResource(R.string.demo_enter)) }
        }
        Button(onClick = {}, enabled = false, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.google_login))
        }
        Text(stringResource(R.string.google_unavailable), modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyMedium)
    }
}
