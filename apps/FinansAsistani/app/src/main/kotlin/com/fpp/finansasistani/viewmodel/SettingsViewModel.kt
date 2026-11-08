package com.fpp.finansasistani.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fpp.finansasistani.data.AppSettings
import com.fpp.finansasistani.data.BackupFormatException
import com.fpp.finansasistani.data.BackupRepository
import com.fpp.finansasistani.data.FinanceRepository
import com.fpp.finansasistani.data.SettingsRepository
import com.fpp.finansasistani.data.ThemeMode
import com.fpp.finansasistani.util.CrashLogger
import com.fpp.finansasistani.util.MessageBus
import com.fpp.finansasistani.util.launchSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val financeRepository: FinanceRepository,
    private val crashLogger: CrashLogger,
    private val bus: MessageBus,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _crashLog = MutableStateFlow<String?>(null)
    val crashLog: StateFlow<String?> = _crashLog.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        launchSafe(bus) { _crashLog.value = withContext(Dispatchers.IO) { crashLogger.read() } }
    }

    fun setThemeMode(mode: ThemeMode) = launchSafe(bus, "Tema kaydedilemedi.") {
        settingsRepository.setThemeMode(mode)
    }

    fun setDynamicColor(enabled: Boolean) = launchSafe(bus, "Ayar kaydedilemedi.") {
        settingsRepository.setDynamicColor(enabled)
    }

    fun export(uri: Uri) = launchSafe(bus, "Yedek oluşturulamadı.") {
        withBusy {
            val count = backupRepository.export(uri)
            bus.post("Yedek kaydedildi ($count kayıt).")
        }
    }

    fun import(uri: Uri) = launchSafe(bus, "Yedek geri yüklenemedi.") {
        withBusy {
            try {
                val data = backupRepository.import(uri)
                bus.post("Yedek geri yüklendi (${data.recordCount} kayıt).")
            } catch (e: BackupFormatException) {
                bus.post(e.message ?: "Yedek dosyası geçersiz.")
            }
        }
    }

    fun clearAllData() = launchSafe(bus, "Veriler silinemedi.") {
        financeRepository.clearAll()
        bus.post("Tüm veriler silindi.")
    }

    fun clearCrashLog() = launchSafe(bus) {
        withContext(Dispatchers.IO) { crashLogger.clear() }
        _crashLog.value = null
    }

    private suspend fun withBusy(block: suspend () -> Unit) {
        _busy.value = true
        try {
            block()
        } finally {
            _busy.value = false
        }
    }
}
