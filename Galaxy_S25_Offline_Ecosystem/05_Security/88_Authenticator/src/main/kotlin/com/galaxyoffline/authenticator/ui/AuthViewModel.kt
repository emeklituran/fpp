package com.galaxyoffline.authenticator.ui

import android.app.Application
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.authenticator.data.AccountRepository
import com.galaxyoffline.authenticator.data.AuthDatabase
import com.galaxyoffline.authenticator.data.Entry
import com.galaxyoffline.authenticator.data.LockState
import com.galaxyoffline.core.crypto.Base32
import com.galaxyoffline.core.crypto.OtpAccount
import com.galaxyoffline.core.crypto.OtpAuthUri
import com.galaxyoffline.core.designsystem.theme.GalaxyPreferences
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.AEADBadTagException

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AccountRepository(AuthDatabase.get(app).accounts())
    private val prefs = GalaxyPreferences.get(app)
    val messages = UiMessages()
    private val _entries = MutableStateFlow<List<Entry>?>(null)
    val entries: StateFlow<List<Entry>?> = _entries.asStateFlow()
    val lockEnabled: StateFlow<Boolean> = prefs.data.map { it[K_LOCK] ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            val enabled = prefs.data.first()[K_LOCK] ?: false
            LockState.enabled = enabled
            if (enabled) LockState.lockIfEnabled()
            reload()
        }
    }

    private suspend fun reload() { _entries.value = withContext(Dispatchers.IO) { repo.all() } }

    /** Ekleme: otpauth URI, QR içeriği veya elle girilen bilgiler. Aynı hesap ikinci kez eklenmez. */
    fun addUri(text: String, onDone: () -> Unit = {}) {
        val acc = try { OtpAuthUri.parse(text) } catch (e: OtpAuthUri.ParseException) { messages.post(e.message ?: "Geçersiz kod"); return }
        add(acc, onDone)
    }

    fun addManual(issuer: String, account: String, secret: String, a: OtpAccount, onDone: () -> Unit) {
        val clean = secret.replace(" ", "").uppercase()
        if (!Base32.isValid(clean)) { messages.post("Gizli anahtar geçersiz (Base32, en az 16 karakter)"); return }
        if (account.isBlank() && issuer.isBlank()) { messages.post("Hizmet veya hesap adı girin"); return }
        add(a.copy(issuer = issuer.trim(), account = account.trim(), secret = clean), onDone)
    }

    private fun add(acc: OtpAccount, onDone: () -> Unit) = launchSafe(messages, "Hesap eklenemedi") {
        if (_entries.value.orEmpty().any { it.account.secret == acc.secret }) { messages.post("Bu hesap zaten ekli"); return@launchSafe }
        withContext(Dispatchers.IO) { repo.add(acc) }
        reload(); messages.post("${acc.issuer.ifBlank { acc.account }} eklendi"); onDone()
    }

    fun rename(e: Entry, issuer: String, account: String) = launchSafe(messages) {
        withContext(Dispatchers.IO) { repo.update(e.copy(account = e.account.copy(issuer = issuer.trim(), account = account.trim()))) }; reload()
    }
    fun delete(e: Entry) = launchSafe(messages) { withContext(Dispatchers.IO) { repo.delete(e.id) }; reload() }

    fun setLock(v: Boolean) = launchSafe(messages) { prefs.edit { it[K_LOCK] = v }; LockState.enabled = v }

    fun export(uri: Uri, password: String) = launchSafe(messages, "Yedek oluşturulamadı") {
        if (password.length < 8) { messages.post("Yedek parolası en az 8 karakter olmalı"); return@launchSafe }
        val json = withContext(Dispatchers.Default) { AccountRepository.exportJson(_entries.value.orEmpty().map { it.account }, password.toCharArray()) }
        withContext(Dispatchers.IO) { getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } ?: error("dosya") }
        messages.post("${_entries.value.orEmpty().size} hesap şifreli olarak dışa aktarıldı")
    }

    fun import(uri: Uri, password: String) = launchSafe(messages, "Yedek içe aktarılamadı") {
        val text = withContext(Dispatchers.IO) { getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }
            ?: run { messages.post("Dosya okunamadı"); return@launchSafe }
        val list = try { withContext(Dispatchers.Default) { AccountRepository.importJson(text, password.toCharArray()) } }
        catch (e: AEADBadTagException) { messages.post("Yedek parolası hatalı"); return@launchSafe }
        catch (e: Exception) { messages.post(e.message ?: "Geçersiz yedek"); return@launchSafe }
        val existing = _entries.value.orEmpty().map { it.account.secret }.toSet()
        val fresh = list.filter { it.secret !in existing }
        withContext(Dispatchers.IO) { fresh.forEach { repo.add(it) } }
        reload(); messages.post("${fresh.size} hesap içe aktarıldı" + if (fresh.size < list.size) " (${list.size - fresh.size} zaten vardı)" else "")
    }

    private companion object { val K_LOCK = booleanPreferencesKey("uygulama_kilidi") }
}
