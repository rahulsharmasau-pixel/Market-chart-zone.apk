package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "market_chart_zone_prefs")

class PreferencesManager(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val notifPromptDoneKey = booleanPreferencesKey("notif_prompt_done")
    private val batteryPromptDoneKey = booleanPreferencesKey("battery_prompt_done")

    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[darkModeKey] ?: true
    }

    val notificationPromptDoneFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[notifPromptDoneKey] ?: false
    }

    val batteryPromptDoneFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[batteryPromptDoneKey] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[darkModeKey] = enabled }
    }

    suspend fun markNotificationPromptDone() {
        context.dataStore.edit { prefs -> prefs[notifPromptDoneKey] = true }
    }

    suspend fun markBatteryPromptDone() {
        context.dataStore.edit { prefs -> prefs[batteryPromptDoneKey] = true }
    }
}
