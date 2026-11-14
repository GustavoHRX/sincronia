package com.sincronia.app.data

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.sincronia.app.alarm.AlarmService
import com.sincronia.app.core.alarm.AlarmScheduler
import com.sincronia.app.data.settings.SettingsRepository
import com.sincronia.app.core.auth.AccountStore
import com.sincronia.app.core.auth.GoogleAuthManager
import com.sincronia.app.core.database.AppDatabase
import com.sincronia.app.core.log.AppLogger
import com.sincronia.app.data.demo.DemoPreferences
import com.sincronia.app.data.sync.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** "Desconectar", in the order of spec 6.4, step 8. */
@Singleton
class AccountSession @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: GoogleAuthManager,
    private val accounts: AccountStore,
    private val demo: DemoPreferences,
    private val database: AppDatabase,
    private val logger: AppLogger,
    private val alarms: AlarmScheduler,
    private val settings: SettingsRepository,
) {
    suspend fun disconnect() {
        auth.revokeAndForget()
        SyncScheduler.cancelAll(context)
        alarms.cancelAll()
        context.stopService(Intent(context, AlarmService::class.java))
        // "Reconecte", "Alarme perdido" and the daily summary no longer make sense without an account.
        NotificationManagerCompat.from(context).cancelAll()
        withContext(Dispatchers.IO) { database.clearAllTables() }
        accounts.clear()
        demo.clear()
        settings.clear()
        logger.i(TAG, "Disconnected")
    }

    private companion object {
        const val TAG = "Session"
    }
}
