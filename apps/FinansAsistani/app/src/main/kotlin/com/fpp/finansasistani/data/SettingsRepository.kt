package com.fpp.finansasistani.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fpp.finans.core.Strategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode(val label: String) {
    SYSTEM("Sistem ayarı"),
    LIGHT("Aydınlık"),
    DARK("Karanlık");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.name == key } ?: SYSTEM
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val strategy: Strategy = Strategy.AVALANCHE,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "ayarlar")

/** Uygulama tercihleri (tema, renk, strateji) DataStore ile cihazda saklanır. */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                themeMode = ThemeMode.fromKey(prefs[KEY_THEME]),
                dynamicColor = prefs[KEY_DYNAMIC] ?: false,
                strategy = Strategy.fromKey(prefs[KEY_STRATEGY]),
            )
        }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[KEY_DYNAMIC] = enabled }
    }

    suspend fun setStrategy(strategy: Strategy) {
        store.edit { it[KEY_STRATEGY] = strategy.name }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("tema")
        val KEY_DYNAMIC = booleanPreferencesKey("dinamik_renk")
        val KEY_STRATEGY = stringPreferencesKey("strateji")
    }
}
