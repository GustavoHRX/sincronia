package com.sincronia.app.core.auth

import android.accounts.Account
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.sincronia.app.BuildConfig
import com.sincronia.app.core.log.AppLogger
import com.sincronia.app.core.network.AccessTokenProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

sealed interface SignInResult {
    data object Success : SignInResult
    data object Cancelled : SignInResult
    data object NoGoogleAccount : SignInResult
    data object Failed : SignInResult
}

sealed interface AuthorizationOutcome {
    data class Authorized(val accessToken: String) : AuthorizationOutcome
    /** Consent screen needed; only an Activity can open it. */
    data class NeedsConsent(val pendingIntent: PendingIntent) : AuthorizationOutcome
    data class MissingScopes(val missing: Set<String>) : AuthorizationOutcome
    data object SignedOut : AuthorizationOutcome
    data object Failed : AuthorizationOutcome
}

/** Spec 6.4: Credential Manager for the identity, AuthorizationClient for the two scopes. */
@Singleton
class GoogleAuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val accountStore: AccountStore,
    private val logger: AppLogger,
) : AccessTokenProvider {
    private val client get() = Identity.getAuthorizationClient(context)

    /** Access token (about 1 h); kept only in memory, never on disk. */
    @Volatile var accessToken: String? = null
        private set

    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    suspend fun signIn(activity: Activity): SignInResult {
        if (!isConfigured) return SignInResult.Failed
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                logger.w(TAG, "Unexpected credential type")
                return SignInResult.Failed
            }
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            val accountId = IdTokenClaims.subject(google.idToken) ?: run {
                logger.w(TAG, "ID token without sub")
                return SignInResult.Failed
            }
            accountStore.save(accountId, google.id, google.displayName, google.profilePictureUri?.toString())
            logger.i(TAG, "Signed in")
            SignInResult.Success
        } catch (_: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (_: NoCredentialException) {
            SignInResult.NoGoogleAccount
        } catch (error: GetCredentialException) {
            logger.w(TAG, "Sign-in failed: ${error.type}")
            SignInResult.Failed
        }
    }

    /**
     * Silent when consent already exists: returns the cached or renewed token. Call it at the start
     * of every sync (step 4).
     */
    suspend fun authorize(): AuthorizationOutcome {
        val account = accountStore.current() ?: return AuthorizationOutcome.SignedOut
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(GoogleScopes.ALL.map(::Scope))
            .setAccount(Account(account.email, "com.google"))
            .build()
        return try {
            handle(client.authorize(request).await())
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            logger.w(TAG, "Authorization failed", error)
            AuthorizationOutcome.Failed
        }
    }

    /** Result of the consent screen opened from [AuthorizationOutcome.NeedsConsent]. */
    suspend fun onConsentResult(data: Intent?): AuthorizationOutcome = try {
        handle(client.getAuthorizationResultFromIntent(data))
    } catch (error: Exception) {
        logger.w(TAG, "Consent not granted", error)
        AuthorizationOutcome.Failed
    }

    /** Step 5: drop a rejected token so the next [authorize] fetches a fresh one. */
    suspend fun clearToken(token: String) {
        if (accessToken == token) accessToken = null
        runCatching { client.clearToken(ClearTokenRequest.builder().setToken(token).build()).await() }
            .onFailure { logger.w(TAG, "clearToken failed", it) }
    }

    override fun currentToken(): String? = accessToken

    override suspend fun refresh(rejected: String?): String? {
        rejected?.let { clearToken(it) }
        return (authorize() as? AuthorizationOutcome.Authorized)?.accessToken
    }

    /**
     * Spec 6.4, step 8, network part: revoke both scopes, then forget the credential. Failures
     * (offline) are only logged; the caller disconnects locally anyway.
     */
    suspend fun revokeAndForget() {
        accessToken = null
        accountStore.current()?.let { account ->
            runCatching {
                client.revokeAccess(RevokeAccessRequest.builder()
                    .setAccount(Account(account.email, "com.google"))
                    .setScopes(GoogleScopes.ALL.map(::Scope))
                    .build()).await()
            }.onFailure { logger.w(TAG, "revokeAccess failed; disconnecting locally", it) }
        }
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
            .onFailure { logger.w(TAG, "clearCredentialState failed", it) }
    }

    private suspend fun handle(result: AuthorizationResult): AuthorizationOutcome {
        if (result.hasResolution()) {
            val pendingIntent = result.pendingIntent ?: return AuthorizationOutcome.Failed
            return AuthorizationOutcome.NeedsConsent(pendingIntent)
        }
        val missing = GoogleScopes.missing(result.grantedScopes)
        if (missing.isNotEmpty()) {
            accountStore.setAuthState(AuthState.MISSING_SCOPES)
            return AuthorizationOutcome.MissingScopes(missing)
        }
        val token = result.accessToken ?: return AuthorizationOutcome.Failed
        accessToken = token
        accountStore.setAuthState(AuthState.AUTHORIZED)
        return AuthorizationOutcome.Authorized(token)
    }

    private companion object {
        const val TAG = "GoogleAuth"
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
