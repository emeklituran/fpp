package com.galaxyoffline.diary.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.diary.App
import com.galaxyoffline.diary.data.DiaryEntry
import com.galaxyoffline.diary.data.DiaryStats
import com.galaxyoffline.diary.data.DiaryStatsCalculator
import com.galaxyoffline.diary.data.Mood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class DiaryListState(
    val loading: Boolean = true,
    val entries: List<DiaryEntry> = emptyList(),
    val query: String = "",
    val stats: DiaryStats = DiaryStatsCalculator.compute(emptyList()),
)

class DiaryListViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as App).database.diary()
    val messages = UiMessages()
    private val query = MutableStateFlow("")

    val state: StateFlow<DiaryListState> = combine(dao.observeAll(), query) { all, q ->
        DiaryListState(false, DiaryStatsCalculator.search(all, q), q, DiaryStatsCalculator.compute(all))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryListState())

    fun setQuery(q: String) { query.value = q }

    /** Tüm günlüğü tarih sırasıyla düz metin olarak kullanıcının seçtiği dosyaya yazar. */
    fun export(uri: Uri) = launchSafe(messages, "Dışa aktarılamadı") {
        val all = dao.all()
        val text = buildString {
            appendLine("Kişisel Günlük – ${all.size} kayıt")
            appendLine()
            all.forEach { e ->
                val m = Mood.of(e.mood)
                appendLine("${TrFormat.formatDateWithDay(e.localDate)} · ${m.emoji} ${m.label}")
                if (e.title.isNotBlank()) appendLine(e.title)
                appendLine(e.content)
                appendLine("────────────")
            }
        }
        withContext(Dispatchers.IO) {
            getApplication<App>().contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                ?: error("Dosya açılamadı")
        }
        messages.post("${all.size} kayıt dışa aktarıldı")
    }
}

data class EntryState(
    val loaded: Boolean = false,
    val id: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val title: String = "",
    val content: String = "",
    val mood: Mood = Mood.OKAY,
    val createdAt: Long = 0,
    val original: DiaryEntry? = null,
    val missing: Boolean = false,
) {
    val dirty: Boolean
        get() = if (original == null) title.isNotBlank() || content.isNotBlank()
        else original.title != title || original.content != content || original.mood != mood.value || original.date != date.toEpochDay()
}

class EntryViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val dao = (app as App).database.diary()
    val messages = UiMessages()
    private val _state = MutableStateFlow(EntryState())
    val state: StateFlow<EntryState> = _state.asStateFlow()

    init {
        val id = handle.get<Long>("id") ?: -1L
        viewModelScope.launch {
            val e = if (id > 0) runCatching { dao.get(id) }.getOrNull() else null
            _state.value = when {
                id > 0 && e == null -> EntryState(loaded = true, missing = true)
                e != null -> EntryState(true, e.id, e.localDate, e.title, e.content, Mood.of(e.mood), e.createdAt, e)
                else -> EntryState(loaded = true)
            }
        }
    }

    fun setDate(d: LocalDate) = _state.update { it.copy(date = d) }
    fun setTitle(v: String) = _state.update { it.copy(title = v.take(200)) }
    fun setContent(v: String) = _state.update { it.copy(content = v.take(100_000)) }
    fun setMood(m: Mood) = _state.update { it.copy(mood = m) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.content.isBlank() && s.title.isBlank()) { messages.post("Boş kayıt kaydedilemez"); return }
        launchSafe(messages, "Kayıt kaydedilemedi") {
            val now = System.currentTimeMillis()
            if (s.id == 0L) {
                dao.insert(DiaryEntry(date = s.date.toEpochDay(), title = s.title.trim(), content = s.content, mood = s.mood.value,
                    createdAt = now, updatedAt = now))
            } else {
                dao.update(DiaryEntry(s.id, s.date.toEpochDay(), s.title.trim(), s.content, s.mood.value, s.createdAt, now))
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) = launchSafe(messages, "Kayıt silinemedi") {
        val s = _state.value
        if (s.id != 0L) dao.delete(s.id)
        onDone()
    }
}
