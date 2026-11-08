package com.galaxyoffline.stopwatch.ui

import android.app.Application
import android.os.SystemClock
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.theme.GalaxyPreferences
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.stopwatch.data.Session
import com.galaxyoffline.stopwatch.data.StopwatchDatabase
import com.galaxyoffline.stopwatch.data.StopwatchLogic
import com.galaxyoffline.stopwatch.data.StopwatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StopwatchSettings(val keepScreenOn: Boolean = true, val haptics: Boolean = true)

class StopwatchViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = GalaxyPreferences.get(app)
    private val dao = StopwatchDatabase.get(app).sessions()
    val messages = UiMessages()

    private val _state = MutableStateFlow<StopwatchState?>(null)
    val state: StateFlow<StopwatchState?> = _state.asStateFlow()

    val sessions = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val settings: StateFlow<StopwatchSettings> = prefs.data.map { StopwatchSettings(it[KEY_SCREEN] ?: true, it[KEY_HAPTICS] ?: true) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StopwatchSettings())

    init {
        viewModelScope.launch {
            val p = runCatching { prefs.data.first() }.getOrNull()
            _state.value = if (p == null) StopwatchState() else StopwatchState(
                running = p[KEY_RUNNING] ?: false,
                startedElapsed = p[KEY_START_E] ?: 0,
                startedWall = p[KEY_START_W] ?: 0,
                accumulated = p[KEY_ACC] ?: 0,
                laps = StopwatchLogic.decodeLaps(p[KEY_LAPS]),
            )
        }
    }

    private fun nowE() = SystemClock.elapsedRealtime()
    private fun nowW() = System.currentTimeMillis()
    fun elapsedNow(): Long = _state.value?.let { StopwatchLogic.elapsed(it, nowE(), nowW()) } ?: 0

    private fun set(s: StopwatchState) {
        _state.value = s
        launchSafe(messages, "Durum kaydedilemedi") {
            prefs.edit {
                it[KEY_RUNNING] = s.running; it[KEY_START_E] = s.startedElapsed; it[KEY_START_W] = s.startedWall
                it[KEY_ACC] = s.accumulated; it[KEY_LAPS] = StopwatchLogic.encodeLaps(s.laps)
            }
        }
    }

    fun startPause() { val s = _state.value ?: return; set(if (s.running) StopwatchLogic.pause(s, nowE(), nowW()) else StopwatchLogic.start(s, nowE(), nowW())) }
    fun lap() { val s = _state.value ?: return; set(StopwatchLogic.lap(s, nowE(), nowW())) }
    fun reset() { set(StopwatchState()) }

    fun saveSession(label: String) {
        val s = _state.value ?: return
        val total = StopwatchLogic.elapsed(s, nowE(), nowW())
        if (total <= 0) return
        launchSafe(messages, "Oturum kaydedilemedi") {
            dao.insert(Session(totalMs = total, laps = StopwatchLogic.encodeLaps(s.laps), label = label.trim().take(60), createdAt = nowW()))
            messages.post("Oturum kaydedildi")
            set(StopwatchState())
        }
    }

    fun deleteSession(s: Session) = launchSafe(messages) { dao.delete(s.id) }
    fun clearSessions() = launchSafe(messages) { dao.clear() }
    fun setKeepScreenOn(v: Boolean) = launchSafe(messages) { prefs.edit { it[KEY_SCREEN] = v } }
    fun setHaptics(v: Boolean) = launchSafe(messages) { prefs.edit { it[KEY_HAPTICS] = v } }

    private companion object {
        val KEY_RUNNING = booleanPreferencesKey("sw_calisiyor")
        val KEY_START_E = longPreferencesKey("sw_baslangic_monoton")
        val KEY_START_W = longPreferencesKey("sw_baslangic_duvar")
        val KEY_ACC = longPreferencesKey("sw_birikmis")
        val KEY_LAPS = stringPreferencesKey("sw_turlar")
        val KEY_SCREEN = booleanPreferencesKey("ekrani_acik_tut")
        val KEY_HAPTICS = booleanPreferencesKey("titresim")
    }
}
