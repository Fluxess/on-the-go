package ru.routealert.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("route_alert_prefs")

class PreferencesRepository(private val context: Context) {
    val defaultAlertType: Flow<AlertType> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_DEFAULT_ALERT] ?: AlertType.BOTH.name
        runCatching { AlertType.valueOf(raw) }.getOrDefault(AlertType.BOTH)
    }

    val helpSeen: Flow<Boolean> = context.dataStore.data.map { it[KEY_HELP_SEEN] ?: false }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    val mapNightMode: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_MAP_NIGHT] ?: false
    }

    /** Temporary unlock for Pro features (share / family). */
    val isPro: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_PRO] ?: true
    }

    suspend fun setDefaultAlertType(type: AlertType) {
        context.dataStore.edit { it[KEY_DEFAULT_ALERT] = type.name }
    }

    suspend fun setHelpSeen(seen: Boolean) {
        context.dataStore.edit { it[KEY_HELP_SEEN] = seen }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setMapNightMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_MAP_NIGHT] = enabled }
    }

    suspend fun setPro(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PRO] = enabled }
    }

    companion object {
        private val KEY_DEFAULT_ALERT = stringPreferencesKey("default_alert_type")
        private val KEY_HELP_SEEN = booleanPreferencesKey("help_seen")
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_MAP_NIGHT = booleanPreferencesKey("map_night_mode")
        private val KEY_PRO = booleanPreferencesKey("pro_unlocked")

        @Volatile
        private var instance: PreferencesRepository? = null

        fun get(context: Context): PreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: PreferencesRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
