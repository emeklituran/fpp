package com.galaxyoffline.todo.ui

import android.app.Application
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.theme.GalaxyPreferences
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.todo.App
import com.galaxyoffline.todo.data.Task
import com.galaxyoffline.todo.data.TaskFilter
import com.galaxyoffline.todo.data.TaskList
import com.galaxyoffline.todo.data.TaskLogic
import com.galaxyoffline.todo.data.TaskSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class TodoState(
    val loading: Boolean = true,
    val lists: List<TaskList> = emptyList(),
    val selectedList: Long? = null,
    val filter: TaskFilter = TaskFilter.ALL,
    val sort: TaskSort = TaskSort.DUE,
    val tasks: List<Task> = emptyList(),
    val counts: Map<TaskFilter, Int> = emptyMap(),
    val listCounts: Map<Long, Int> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
)

class TodoViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as App).database.todo()
    private val prefs = GalaxyPreferences.get(app)
    val messages = UiMessages()
    private val selected = MutableStateFlow<Long?>(null)
    private val filter = MutableStateFlow(TaskFilter.ALL)

    val state: StateFlow<TodoState> = combine(dao.observeLists(), dao.observeTasks(), selected, filter, prefs.data) { lists, tasks, sel, f, p ->
        val today = LocalDate.now()
        val sort = TaskSort.entries.firstOrNull { it.name == p[KEY_SORT] } ?: TaskSort.DUE
        val listId = sel?.takeIf { id -> lists.any { it.id == id } }
        TodoState(false, lists, listId, f, sort, TaskLogic.filter(tasks, f, listId, today, sort), TaskLogic.counts(tasks, listId, today),
            tasks.filter { !it.done }.groupingBy { it.listId }.eachCount(), today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoState())

    init {
        // İlk açılışta varsayılan liste oluşturulur (örnek görev eklenmez).
        launchSafe(messages) { if (dao.listCount() == 0) dao.insertList(TaskList(name = "Görevlerim", createdAt = System.currentTimeMillis())) }
    }

    fun selectList(id: Long?) { selected.value = id }
    fun setFilter(f: TaskFilter) { filter.value = f }
    fun setSort(s: TaskSort) = launchSafe(messages) { prefs.edit { it[KEY_SORT] = s.name } }

    fun save(task: Task) = launchSafe(messages, "Görev kaydedilemedi") {
        val t = task.copy(title = task.title.trim().take(200), note = task.note.take(5000))
        if (t.title.isBlank()) { messages.post("Görev adı boş olamaz"); return@launchSafe }
        dao.upsertTask(if (t.createdAt == 0L) t.copy(createdAt = System.currentTimeMillis()) else t)
    }

    fun toggleDone(task: Task) = launchSafe(messages) {
        dao.upsertTask(task.copy(done = !task.done, completedAt = if (!task.done) System.currentTimeMillis() else null))
    }

    fun restore(task: Task) = launchSafe(messages) { dao.upsertTask(task) }
    fun delete(task: Task) = launchSafe(messages, "Görev silinemedi") { dao.deleteTask(task.id) }
    fun clearDone() = launchSafe(messages) {
        val n = dao.clearDone(state.value.selectedList)
        messages.post("$n tamamlanan görev silindi")
    }

    fun addList(name: String) = launchSafe(messages, "Liste eklenemedi") {
        val n = name.trim().take(60)
        if (n.isBlank()) return@launchSafe
        selected.value = dao.insertList(TaskList(name = n, createdAt = System.currentTimeMillis()))
    }
    fun renameList(l: TaskList, name: String) = launchSafe(messages) { if (name.isNotBlank()) dao.updateList(l.copy(name = name.trim().take(60))) }
    fun deleteList(l: TaskList) = launchSafe(messages, "Liste silinemedi") {
        if (state.value.lists.size <= 1) { messages.post("En az bir liste kalmalı"); return@launchSafe }
        dao.deleteList(l.id)
        if (selected.value == l.id) selected.value = null
        messages.post("\"${l.name}\" listesi silindi")
    }

    private companion object {
        val KEY_SORT = stringPreferencesKey("gorev_siralama")
    }
}
