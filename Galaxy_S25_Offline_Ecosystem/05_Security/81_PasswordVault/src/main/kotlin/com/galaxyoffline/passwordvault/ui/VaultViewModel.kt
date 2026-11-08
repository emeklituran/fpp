package com.galaxyoffline.passwordvault.ui

import android.app.Application
import android.net.Uri
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.crypto.CryptoBox
import com.galaxyoffline.core.designsystem.theme.GalaxyPreferences
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.passwordvault.data.VaultDatabase
import com.galaxyoffline.passwordvault.data.VaultItem
import com.galaxyoffline.passwordvault.data.VaultKeys
import com.galaxyoffline.passwordvault.data.VaultRepository
import com.galaxyoffline.passwordvault.data.VaultSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher

data class VaultSettings(val autoLockSeconds: Int = 60, val clipboardSeconds: Int = 30)

class VaultViewModel(app: Application) : AndroidViewModel(app) {
    val keys = VaultKeys(app)
    private val repo = VaultRepository(VaultDatabase.get(app).entries())
    private val prefs = GalaxyPreferences.get(app)
    val messages = UiMessages()

    private val _setUp = MutableStateFlow(keys.isSetUp)
    val setUp: StateFlow<Boolean> = _setUp.asStateFlow()
    val unlocked = VaultSession.unlocked
    private val _items = MutableStateFlow<List<VaultItem>>(emptyList())
    val items: StateFlow<List<VaultItem>> = _items.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _lockout = MutableStateFlow(0)
    val lockoutSeconds: StateFlow<Int> = _lockout.asStateFlow()
    private val _bio = MutableStateFlow(keys.biometricEnabled)
    val biometricEnabled: StateFlow<Boolean> = _bio.asStateFlow()

    val settings: StateFlow<VaultSettings> = prefs.data.map { VaultSettings(it[K_LOCK] ?: 60, it[K_CLIP] ?: 30) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, VaultSettings())

    init {
        viewModelScope.launch { settings.collect { VaultSession.autoLockMs = it.autoLockSeconds * 1000L } }
        viewModelScope.launch { unlocked.collect { if (it) reload() else _items.value = emptyList() } }
        tickLockout()
    }

    private fun tickLockout() = viewModelScope.launch {
        while (true) {
            _lockout.value = ((keys.lockedUntil - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            delay(1000)
        }
    }

    private suspend fun reload() {
        val k = VaultSession.key() ?: return
        _items.value = withContext(Dispatchers.Default) { repo.loadAll(k) }.sortedWith(compareByDescending<VaultItem> { it.favorite }.thenBy { it.title.lowercase() })
    }

    fun setup(password: String, confirm: String) {
        if (password.length < 8) { _error.value = "Ana şifre en az 8 karakter olmalı"; return }
        if (password != confirm) { _error.value = "Şifreler eşleşmiyor"; return }
        _busy.value = true; _error.value = null
        launchSafe(messages, "Kasa oluşturulamadı") {
            val chars = password.toCharArray()
            val dek = withContext(Dispatchers.Default) { keys.setup(chars) }
            CryptoBox.wipe(chars)
            _setUp.value = true
            VaultSession.open(dek)
            _busy.value = false
        }
    }

    fun unlock(password: String) {
        if (keys.lockedUntil > System.currentTimeMillis()) return
        _busy.value = true; _error.value = null
        launchSafe(messages, "Kilit açılamadı") {
            val chars = password.toCharArray()
            val dek = withContext(Dispatchers.Default) { keys.unlock(chars) }
            CryptoBox.wipe(chars)
            if (dek == null) {
                keys.recordFailure()
                _error.value = "Ana şifre hatalı (${keys.failures}. deneme)"
            } else {
                keys.resetFailures(); VaultSession.open(dek)
            }
            _busy.value = false
        }
    }

    fun bioDecryptCipher(): Cipher? = keys.bioDecryptCipher().also { if (it == null) { _bio.value = false } }
    fun bioEncryptCipher(): Cipher = keys.bioEncryptCipher()

    fun unlockWithBiometric(c: Cipher) {
        val dek = keys.unlockBiometric(c)
        if (dek == null) _error.value = "Biyometrik açma başarısız; ana şifreyi kullanın" else { keys.resetFailures(); VaultSession.open(dek) }
    }

    fun enableBiometric(c: Cipher) {
        val k = VaultSession.key() ?: return
        runCatching { keys.enableBiometric(c, k) }.onSuccess { _bio.value = true; messages.post("Biyometrik açma etkin") }
            .onFailure { messages.post("Biyometri etkinleştirilemedi") }
    }
    fun disableBiometric() { keys.disableBiometric(); _bio.value = false; messages.post("Biyometrik açma kapatıldı") }

    fun lock() = VaultSession.lock()

    fun save(item: VaultItem, onDone: () -> Unit = {}) = launchSafe(messages, "Kayıt kaydedilemedi") {
        val k = VaultSession.key() ?: return@launchSafe
        if (item.title.isBlank()) { messages.post("Başlık gerekli"); return@launchSafe }
        repo.save(k, item.copy(title = item.title.trim().take(120)))
        reload(); onDone()
    }

    fun toggleFavorite(item: VaultItem) = save(item.copy(favorite = !item.favorite))
    fun delete(item: VaultItem) = launchSafe(messages) { repo.delete(item.id); reload() }
    fun restore(item: VaultItem) = save(item.copy(id = 0))

    fun changePassword(old: String, new: String, confirm: String, onDone: () -> Unit) {
        if (new.length < 8) { messages.post("Yeni şifre en az 8 karakter olmalı"); return }
        if (new != confirm) { messages.post("Yeni şifreler eşleşmiyor"); return }
        launchSafe(messages, "Şifre değiştirilemedi") {
            val ok = withContext(Dispatchers.Default) { keys.unlock(old.toCharArray()) }
            if (ok == null) { messages.post("Mevcut ana şifre hatalı"); return@launchSafe }
            withContext(Dispatchers.Default) { keys.changePassword(ok, new.toCharArray()) }
            CryptoBox.wipe(ok)
            messages.post("Ana şifre değiştirildi"); onDone()
        }
    }

    fun setAutoLock(s: Int) = launchSafe(messages) { prefs.edit { it[K_LOCK] = s } }
    fun setClipboard(s: Int) = launchSafe(messages) { prefs.edit { it[K_CLIP] = s } }

    fun export(uri: Uri, password: String) = launchSafe(messages, "Yedek oluşturulamadı") {
        val ok = withContext(Dispatchers.Default) { keys.unlock(password.toCharArray()) }
        if (ok == null) { messages.post("Ana şifre hatalı"); return@launchSafe }
        CryptoBox.wipe(ok)
        val json = withContext(Dispatchers.Default) { VaultRepository.exportJson(_items.value, password.toCharArray()) }
        withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } ?: error("dosya")
        }
        messages.post("${_items.value.size} kayıt şifreli olarak dışa aktarıldı")
    }

    fun import(uri: Uri, password: String) = launchSafe(messages, "Yedek içe aktarılamadı") {
        val k = VaultSession.key() ?: return@launchSafe
        val text = withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { s -> s.readBytes().takeIf { it.size < 10_000_000 }?.toString(Charsets.UTF_8) }
        } ?: run { messages.post("Dosya okunamadı"); return@launchSafe }
        val list = try {
            withContext(Dispatchers.Default) { VaultRepository.importJson(text, password.toCharArray()) }
        } catch (e: AEADBadTagException) { messages.post("Yedek parolası hatalı"); return@launchSafe
        } catch (e: Exception) { messages.post(e.message ?: "Geçersiz yedek dosyası"); return@launchSafe }
        list.forEach { repo.save(k, it.copy(id = 0, createdAt = 0)) }
        reload(); messages.post("${list.size} kayıt içe aktarıldı")
    }

    private companion object {
        val K_LOCK = intPreferencesKey("otomatik_kilit_sn")
        val K_CLIP = intPreferencesKey("pano_temizleme_sn")
    }
}
