package com.galaxyoffline.notes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.notes.App
import com.galaxyoffline.notes.data.Note
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class EditorState(
    val loaded: Boolean = false,
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val color: Int = 0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val missing: Boolean = false,
)

/** Not düzenleyici: değişiklikler 0,5 sn sonra otomatik kaydedilir; çıkışta son durum uygulama kapsamında yazılır. */
@OptIn(FlowPreview::class)
class EditorViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val application = app as App
    private val dao = application.database.notes()
    private val mutex = Mutex()
    val messages = UiMessages()

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()
    private var autosave: Job? = null

    init {
        val id = handle.get<Long>("id") ?: -1L
        viewModelScope.launch {
            val note = if (id > 0) runCatching { dao.get(id) }.getOrNull() else null
            _state.value = when {
                id > 0 && note == null -> EditorState(loaded = true, missing = true)
                note != null -> EditorState(true, note.id, note.title, note.content, note.color, note.pinned, note.archived,
                    note.createdAt, note.updatedAt)
                else -> EditorState(loaded = true)
            }
            autosave = launch { _state.drop(1).debounce(500).collect { persist() } }
        }
    }

    fun setTitle(v: String) = _state.update { it.copy(title = v.take(MAX_TITLE)) }
    fun setContent(v: String) = _state.update { it.copy(content = v.take(MAX_CONTENT)) }
    fun setColor(c: Int) = _state.update { it.copy(color = c) }
    fun togglePin() = _state.update { it.copy(pinned = !it.pinned) }

    /** Değişiklikleri veritabanına yazar. Boş yeni not oluşturulmaz. */
    private suspend fun persist() = mutex.withLock {
        val s = _state.value
        if (!s.loaded || s.missing) return@withLock
        val now = System.currentTimeMillis()
        try {
            if (s.id == 0L) {
                if (s.title.isBlank() && s.content.isBlank()) return@withLock
                val id = dao.insert(Note(title = s.title, content = s.content, color = s.color, pinned = s.pinned,
                    archived = s.archived, createdAt = now, updatedAt = now))
                _state.update { it.copy(id = id, createdAt = now, updatedAt = now) }
            } else {
                val existing = dao.get(s.id) ?: return@withLock
                if (existing.title != s.title || existing.content != s.content || existing.color != s.color ||
                    existing.pinned != s.pinned || existing.archived != s.archived) {
                    dao.update(existing.copy(title = s.title, content = s.content, color = s.color, pinned = s.pinned,
                        archived = s.archived, updatedAt = now))
                    _state.update { it.copy(updatedAt = now) }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Notes", "Kaydedilemedi", e)
            messages.post("Not kaydedilemedi")
        }
    }

    /** Ekrandan çıkarken: ekran kapansa da tamamlanması için uygulama kapsamında kaydedilir. */
    fun saveAndClose() {
        autosave?.cancel()
        val s = _state.value
        application.appScope.launch {
            if (s.id != 0L && s.title.isBlank() && s.content.isBlank()) {
                runCatching { dao.deleteForever(s.id) } // tamamen boşaltılan not saklanmaz
            } else {
                persist()
            }
        }
    }

    fun archive(onDone: () -> Unit) {
        _state.update { it.copy(archived = !it.archived, pinned = false) }
        saveAndClose(); onDone()
    }

    fun moveToTrash(onDone: () -> Unit) {
        autosave?.cancel()
        val s = _state.value
        application.appScope.launch {
            mutex.withLock {
                if (s.id != 0L) runCatching { dao.get(s.id)?.let { dao.update(it.copy(deletedAt = System.currentTimeMillis(), pinned = false)) } }
            }
        }
        onDone()
    }

    companion object {
        const val MAX_TITLE = 200
        const val MAX_CONTENT = 100_000
    }
}
