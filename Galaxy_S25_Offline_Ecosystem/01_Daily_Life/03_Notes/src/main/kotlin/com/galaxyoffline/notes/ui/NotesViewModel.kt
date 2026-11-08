package com.galaxyoffline.notes.ui

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.theme.GalaxyPreferences
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.notes.App
import com.galaxyoffline.notes.data.Note
import com.galaxyoffline.notes.data.NoteFilter
import com.galaxyoffline.notes.data.NoteQuery
import com.galaxyoffline.notes.data.NoteSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class NotesUiState(
    val loading: Boolean = true,
    val notes: List<Note> = emptyList(),
    val filter: NoteFilter = NoteFilter.NOTES,
    val query: String = "",
    val sort: NoteSort = NoteSort.UPDATED,
    val grid: Boolean = true,
    val counts: Map<NoteFilter, Int> = emptyMap(),
)

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as App).database.notes()
    private val prefs = GalaxyPreferences.get(app)
    val messages = UiMessages()

    private val filter = MutableStateFlow(NoteFilter.NOTES)
    private val query = MutableStateFlow("")

    val state: StateFlow<NotesUiState> = combine(dao.observeAll(), filter, query, prefs.data) { all, f, q, p ->
        val sort = NoteSort.entries.firstOrNull { it.name == p[KEY_SORT] } ?: NoteSort.UPDATED
        NotesUiState(
            loading = false,
            notes = NoteQuery.apply(all, f, q, sort),
            filter = f,
            query = q,
            sort = sort,
            grid = p[KEY_GRID] ?: true,
            counts = NoteFilter.entries.associateWith { NoteQuery.apply(all, it, "", sort).size },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    init {
        // 30 günden eski çöp kutusu notları kalıcı olarak silinir.
        launchSafe {
            val removed = dao.purgeTrash(System.currentTimeMillis() - NoteQuery.TRASH_DAYS * 24 * 3600 * 1000)
            if (removed > 0) messages.post("$removed eski not çöp kutusundan kalıcı olarak silindi")
        }
    }

    fun setFilter(f: NoteFilter) { filter.value = f }
    fun setQuery(q: String) { query.value = q }
    fun setSort(s: NoteSort) = launchSafe(messages) { prefs.edit { it[KEY_SORT] = s.name } }
    fun setGrid(g: Boolean) = launchSafe(messages) { prefs.edit { it[KEY_GRID] = g } }

    private fun change(note: Note, transform: (Note) -> Note) = launchSafe(messages, "Not güncellenemedi") {
        dao.update(transform(note))
    }

    fun togglePin(note: Note) = change(note) { it.copy(pinned = !it.pinned) }
    fun setArchived(note: Note, archived: Boolean) = launchSafe(messages, "Not güncellenemedi") {
        dao.update(note.copy(archived = archived, pinned = if (archived) false else note.pinned))
        messages.post(if (archived) "Not arşivlendi" else "Not arşivden çıkarıldı")
    }
    fun moveToTrash(note: Note) = change(note) { it.copy(deletedAt = System.currentTimeMillis(), pinned = false) }
    fun restore(note: Note) = change(note) { it.copy(deletedAt = null) }
    fun undoTrash(note: Note) = launchSafe(messages) { dao.update(note) }
    fun deleteForever(note: Note) = launchSafe(messages, "Not silinemedi") { dao.deleteForever(note.id) }
    fun emptyTrash() = launchSafe(messages, "Çöp kutusu boşaltılamadı") { dao.emptyTrash(); messages.post("Çöp kutusu boşaltıldı") }

    companion object {
        val KEY_SORT = stringPreferencesKey("not_siralama")
        val KEY_GRID = booleanPreferencesKey("not_izgara")
        val KEY_COUNT = booleanPreferencesKey("kelime_sayisi")
    }
}
