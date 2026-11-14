package com.sincronia.app.ui.onboarding

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sincronia.app.core.auth.AuthorizationOutcome
import com.sincronia.app.core.auth.GoogleAuthManager
import com.sincronia.app.core.auth.SignInResult
import com.sincronia.app.data.demo.DemoPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingMessage { NONE, CANCELLED, NO_ACCOUNT, MISSING_SCOPES, FAILED }

data class OnboardingUiState(
    val loginAvailable: Boolean,
    val busy: Boolean = false,
    val message: OnboardingMessage = OnboardingMessage.NONE,
    /** Consent screen waiting to be opened by the Activity. */
    val consent: PendingIntent? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val auth: GoogleAuthManager,
    private val demoPreferences: DemoPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState(loginAvailable = auth.isConfigured))
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun signIn(activity: Activity) {
        _state.update { it.copy(busy = true, message = OnboardingMessage.NONE) }
        viewModelScope.launch {
            when (auth.signIn(activity)) {
                SignInResult.Success -> apply(auth.authorize())
                SignInResult.Cancelled -> finish(OnboardingMessage.CANCELLED)
                SignInResult.NoGoogleAccount -> finish(OnboardingMessage.NO_ACCOUNT)
                SignInResult.Failed -> finish(OnboardingMessage.FAILED)
            }
        }
    }

    /** "Pedir de novo" after the user unchecked a scope. */
    fun retryAuthorization() {
        _state.update { it.copy(busy = true, message = OnboardingMessage.NONE) }
        viewModelScope.launch { apply(auth.authorize()) }
    }

    fun consentLaunched() = _state.update { it.copy(consent = null) }

    fun onConsentResult(data: Intent?) {
        viewModelScope.launch { apply(auth.onConsentResult(data)) }
    }

    fun enterDemo(onEntered: () -> Unit) {
        viewModelScope.launch {
            demoPreferences.enable()
            onEntered()
        }
    }

    // On success the account turns AUTHORIZED and the navigation leaves the onboarding.
    private fun apply(outcome: AuthorizationOutcome) = when (outcome) {
        is AuthorizationOutcome.Authorized -> finish(OnboardingMessage.NONE)
        is AuthorizationOutcome.NeedsConsent -> _state.update { it.copy(consent = outcome.pendingIntent) }
        is AuthorizationOutcome.MissingScopes -> finish(OnboardingMessage.MISSING_SCOPES)
        AuthorizationOutcome.SignedOut, AuthorizationOutcome.Failed -> finish(OnboardingMessage.FAILED)
    }

    private fun finish(message: OnboardingMessage) =
        _state.update { it.copy(busy = false, message = message, consent = null) }
}
