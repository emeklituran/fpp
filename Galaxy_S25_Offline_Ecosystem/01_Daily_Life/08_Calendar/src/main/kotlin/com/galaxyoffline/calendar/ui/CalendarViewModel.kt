package com.galaxyoffline.calendar.ui

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.calendar.data.CalendarDatabase
import com.galaxyoffline.calendar.data.Event
import com.galaxyoffline.core.designsystem.theme.GalaxyPreferences
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CalendarState(val loading: Boolean = true, val events: List<Event> = emptyList(), val showHolidays: Boolean = true)

class CalendarViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = CalendarDatabase.get(app).events()
    private val prefs = GalaxyPreferences.get(app)
    val messages = UiMessages()

    val state: StateFlow<CalendarState> = combine(dao.observeAll(), prefs.data) { e, p ->
        CalendarState(false, e, p[KEY_HOLIDAYS] ?: true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarState())

    fun save(e: Event) = launchSafe(messages, "Etkinlik kaydedilemedi") {
        if (e.title.isBlank()) { messages.post("Başlık boş olamaz"); return@launchSafe }
        if (!e.allDay && e.startMinute != null && e.endMinute != null && e.endMinute < e.startMinute) {
            messages.post("Bitiş saati başlangıçtan önce olamaz"); return@launchSafe
        }
        dao.upsert(e.copy(title = e.title.trim().take(120), note = e.note.take(2000),
            createdAt = if (e.createdAt == 0L) System.currentTimeMillis() else e.createdAt))
    }
    fun delete(e: Event) = launchSafe(messages) { dao.delete(e.id) }
    fun restore(e: Event) = launchSafe(messages) { dao.upsert(e) }
    fun setShowHolidays(v: Boolean) = launchSafe(messages) { prefs.edit { it[KEY_HOLIDAYS] = v } }

    private companion object { val KEY_HOLIDAYS = booleanPreferencesKey("tatilleri_goster") }
}
