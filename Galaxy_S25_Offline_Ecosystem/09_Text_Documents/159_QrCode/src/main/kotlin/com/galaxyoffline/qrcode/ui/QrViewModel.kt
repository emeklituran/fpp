package com.galaxyoffline.qrcode.ui

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.theme.GalaxyPreferences
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.qrcode.data.QrDatabase
import com.galaxyoffline.qrcode.data.ScanEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class QrViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = QrDatabase.get(app).scans()
    private val prefs = GalaxyPreferences.get(app)
    val messages = UiMessages()
    val history: StateFlow<List<ScanEntry>> = dao.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val saveHistory: StateFlow<Boolean> = prefs.data.map { it[K_SAVE] ?: true }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun record(text: String, format: String) = launchSafe(messages) {
        if (prefs.data.first()[K_SAVE] != false) dao.insert(ScanEntry(text = text.take(10_000), format = format, createdAt = System.currentTimeMillis()))
    }
    fun delete(e: ScanEntry) = launchSafe(messages) { dao.delete(e.id) }
    fun clear() = launchSafe(messages) { dao.clear(); messages.post("Geçmiş temizlendi") }
    fun setSaveHistory(v: Boolean) = launchSafe(messages) { prefs.edit { it[K_SAVE] = v } }

    private companion object { val K_SAVE = booleanPreferencesKey("gecmisi_kaydet") }
}
