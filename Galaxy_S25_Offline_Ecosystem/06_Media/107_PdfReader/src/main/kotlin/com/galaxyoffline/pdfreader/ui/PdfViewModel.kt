package com.galaxyoffline.pdfreader.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.theme.GalaxyPreferences
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.pdfreader.data.PdfDatabase
import com.galaxyoffline.pdfreader.data.PdfDocument
import com.galaxyoffline.pdfreader.data.Recent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class OpenDoc(val uri: Uri, val name: String, val doc: PdfDocument, val startPage: Int)

class PdfViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = PdfDatabase.get(app).recents()
    private val prefs = GalaxyPreferences.get(app)
    val messages = UiMessages()
    val recents: StateFlow<List<Recent>> = dao.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val night: StateFlow<Boolean> = prefs.data.map { it[K_NIGHT] ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private val _open = MutableStateFlow<OpenDoc?>(null)
    val open: StateFlow<OpenDoc?> = _open.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun open(uri: Uri, persist: Boolean) = launchSafe(messages, "Dosya açılamadı") {
        _loading.value = true
        val ctx = getApplication<Application>()
        if (persist) runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val name = withContext(Dispatchers.IO) {
            runCatching { ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } }.getOrNull()
        } ?: "Belge.pdf"
        when (val r = withContext(Dispatchers.IO) { PdfDocument.open(ctx, uri) }) {
            is PdfDocument.OpenResult.Error -> { messages.post(r.message); if (persist) dao.delete(uri.toString()) }
            is PdfDocument.OpenResult.Ok -> {
                val prev = dao.get(uri.toString())
                val start = (prev?.lastPage ?: 0).coerceIn(0, r.doc.pageCount - 1)
                _open.value?.doc?.close()
                _open.value = OpenDoc(uri, name, r.doc, start)
                dao.upsert(Recent(uri.toString(), name, r.doc.pageCount, start, System.currentTimeMillis()))
            }
        }
        _loading.value = false
    }

    fun savePage(page: Int) { val o = _open.value ?: return; launchSafe { dao.setPage(o.uri.toString(), page) } }
    fun close() { _open.value?.doc?.close(); _open.value = null }
    fun remove(r: Recent) = launchSafe(messages) {
        dao.delete(r.uri)
        runCatching { getApplication<Application>().contentResolver.releasePersistableUriPermission(Uri.parse(r.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    fun setNight(v: Boolean) = launchSafe(messages) { prefs.edit { it[K_NIGHT] = v } }

    override fun onCleared() { _open.value?.doc?.close() }

    private companion object { val K_NIGHT = booleanPreferencesKey("gece_modu") }
}
