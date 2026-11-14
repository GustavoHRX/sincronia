package com.sincronia.app.ui.onboarding

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sincronia.app.BuildConfig
import com.sincronia.app.R

@Composable
fun Onboarding(onDemoEntered: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val activity = LocalActivity.current
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onConsentResult(it.data)
    }
    LaunchedEffect(state.consent) {
        state.consent?.let {
            viewModel.consentLaunched()
            consentLauncher.launch(IntentSenderRequest.Builder(it.intentSender).build())
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.welcome_message), modifier = Modifier.padding(vertical = 24.dp),
            textAlign = TextAlign.Center)
        if (state.busy) {
            CircularProgressIndicator(Modifier.padding(bottom = 16.dp))
        }
        if (state.message == OnboardingMessage.MISSING_SCOPES) {
            Button(onClick = viewModel::retryAuthorization, enabled = !state.busy) {
                Text(stringResource(R.string.google_ask_again))
            }
        } else {
            Button(onClick = { activity?.let(viewModel::signIn) }, enabled = state.loginAvailable && !state.busy) {
                Text(stringResource(R.string.google_login))
            }
        }
        if (BuildConfig.DEBUG) {
            OutlinedButton(onClick = { viewModel.enterDemo(onDemoEntered) }, enabled = !state.busy,
                modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.demo_enter))
            }
        }
        val message = when {
            !state.loginAvailable -> R.string.google_unavailable
            state.message == OnboardingMessage.CANCELLED -> R.string.google_cancelled
            state.message == OnboardingMessage.NO_ACCOUNT -> R.string.google_no_account
            state.message == OnboardingMessage.MISSING_SCOPES -> R.string.google_missing_scopes
            state.message == OnboardingMessage.FAILED -> R.string.google_failed
            else -> null
        }
        message?.let {
            Text(stringResource(it), modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
