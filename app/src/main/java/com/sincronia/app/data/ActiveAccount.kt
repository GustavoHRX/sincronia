package com.sincronia.app.data

import com.sincronia.app.core.auth.AccountStore
import com.sincronia.app.data.demo.DemoPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/** Account shown on screen: "demo" in demonstration mode, otherwise the Google `sub`. */
@Singleton
class ActiveAccount @Inject constructor(demo: DemoPreferences, accounts: AccountStore) {
    private val stored: Flow<String?> = combine(demo.enabled, accounts.account) { isDemo, account ->
        if (isDemo) DEMO_ACCOUNT else account?.accountId
    }

    val id: Flow<String> = stored.filterNotNull().distinctUntilChanged()

    /**
     * Reads the stored account once, waiting for the disk however long it takes: null only when
     * nobody is signed in. Background code must use this, never `id` with a timeout — right after a
     * reboot the read can take seconds and a timeout looked like "signed out", cancelling every alarm.
     */
    suspend fun current(): String? = stored.first()

    companion object {
        const val DEMO_ACCOUNT = "demo"
    }
}
