package com.sincronia.app.core.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class AuthState {
    /** Signed in, consent not finished yet. */
    SIGNED_IN,
    AUTHORIZED,
    /** The user unchecked Calendar or Tasks on the consent screen. */
    MISSING_SCOPES,
    /** Consent is needed but the app was in the background (spec 6.4, step 6). */
    NEEDS_REAUTH,
}

data class GoogleAccount(
    val accountId: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val authState: AuthState,
)

private val Context.accountDataStore by preferencesDataStore("account")

@Singleton
class AccountStore @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val accountIdKey = stringPreferencesKey("account_id")
    private val emailKey = stringPreferencesKey("email")
    private val nameKey = stringPreferencesKey("display_name")
    private val photoKey = stringPreferencesKey("photo_url")
    private val stateKey = stringPreferencesKey("auth_state")

    val account: Flow<GoogleAccount?> = context.accountDataStore.data.map { prefs ->
        val id = prefs[accountIdKey] ?: return@map null
        GoogleAccount(
            accountId = id,
            email = prefs[emailKey].orEmpty(),
            displayName = prefs[nameKey],
            photoUrl = prefs[photoKey],
            authState = prefs[stateKey]?.let { runCatching { AuthState.valueOf(it) }.getOrNull() } ?: AuthState.SIGNED_IN,
        )
    }

    suspend fun current(): GoogleAccount? = account.first()

    suspend fun save(accountId: String, email: String, displayName: String?, photoUrl: String?) {
        context.accountDataStore.edit {
            it[accountIdKey] = accountId
            it[emailKey] = email
            if (displayName != null) it[nameKey] = displayName else it.remove(nameKey)
            if (photoUrl != null) it[photoKey] = photoUrl else it.remove(photoKey)
            it[stateKey] = AuthState.SIGNED_IN.name
        }
    }

    suspend fun setAuthState(state: AuthState) {
        context.accountDataStore.edit { if (it[accountIdKey] != null) it[stateKey] = state.name }
    }

    suspend fun clear() {
        context.accountDataStore.edit { it.clear() }
    }
}
