package com.sincronia.app.data.demo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.sincronia.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.demoDataStore by preferencesDataStore("demo")

@Singleton
class DemoPreferences @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val enabledKey = booleanPreferencesKey("demo_enabled")
    val enabled: Flow<Boolean> = context.demoDataStore.data.map { BuildConfig.DEBUG && (it[enabledKey] ?: false) }

    suspend fun clear() {
        context.demoDataStore.edit { it.clear() }
    }

    suspend fun enable() {
        if (BuildConfig.DEBUG) context.demoDataStore.edit { it[enabledKey] = true }
    }
}
