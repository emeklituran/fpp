package com.galaxyoffline.core.designsystem.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

data class ThemeSettings(val mode: ThemeMode = ThemeMode.SYSTEM, val dynamicColor: Boolean = false)

private val Context.galaxyStore: DataStore<Preferences> by preferencesDataStore(name = "galaxy_ayarlar")

/**
 * Uygulamanın yerel tercih deposu (DataStore). Tema ayarlarını içerir; uygulamalar kendi anahtarlarını
 * [data] ve [edit] ile aynı depoda saklayabilir. Her APK'nın deposu kendi özel klasöründedir.
 */
class GalaxyPreferences private constructor(context: Context) {
    private val store = context.applicationContext.galaxyStore

    val data: Flow<Preferences> = store.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val theme: Flow<ThemeSettings> = data.map {
        ThemeSettings(ThemeMode.fromKey(it[KEY_THEME]), it[KEY_DYNAMIC] ?: false)
    }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[KEY_THEME] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = edit { it[KEY_DYNAMIC] = enabled }

    suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        try {
            store.edit { block(it) }
        } catch (e: IOException) {
            android.util.Log.e("GalaxyPreferences", "Ayar yazılamadı", e)
        }
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("tema")
        private val KEY_DYNAMIC = booleanPreferencesKey("dinamik_renk")

        @Volatile private var instance: GalaxyPreferences? = null
        fun get(context: Context): GalaxyPreferences =
            instance ?: synchronized(this) { instance ?: GalaxyPreferences(context).also { instance = it } }
    }
}
