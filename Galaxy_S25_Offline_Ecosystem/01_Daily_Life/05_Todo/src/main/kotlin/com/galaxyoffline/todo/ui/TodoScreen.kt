package com.galaxyoffline.todo.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.todo.data.Priority
import com.galaxyoffline.todo.data.Task
import com.galaxyoffline.todo.data.TaskFilter
import com.galaxyoffline.todo.data.TaskLogic
import com.galaxyoffline.todo.data.TaskSort
import kotlinx.coroutines.launch

@Composable
fun priorityColor(p: Int): Color = when (Priority.of(p)) {
    Priority.HIGH -> MaterialTheme.colorScheme.error
    Priority.NORMAL -> MaterialTheme.colorScheme.primary
    Priority.LOW -> MaterialTheme.colorScheme.outline
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(vm: TodoViewModel, onManageLists: () -> Unit, onSettings: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var listMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }

    fun undoable(message: String, task: Task, action: () -> Unit) {
        action()
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(message, "Geri al", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) vm.restore(task)
        }
    }

    val listName = s.lists.firstOrNull { it.id == s.selectedList }?.name ?: "Tüm listeler"
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        TextButton(onClick = { listMenu = true }) {
                            Text(listName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Filled.ArrowDropDown, "Liste seç", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = listMenu, onDismissRequest = { listMenu = false }) {
                            DropdownMenuItem(text = { Text("Tüm listeler") }, onClick = { vm.selectList(null); listMenu = false })
                            s.lists.forEach { l ->
                                DropdownMenuItem(text = { Text("${l.name} (${s.listCounts[l.id] ?: 0})") }, onClick = { vm.selectList(l.id); listMenu = false })
                            }
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Listeleri yönet…") }, onClick = { listMenu = false; onManageLists() })
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Outlined.Sort, "Sırala") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            TaskSort.entries.forEach { o ->
                                DropdownMenuItem(text = { Text(o.label, fontWeight = if (o == s.sort) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = { vm.setSort(o); sortMenu = false })
                            }
                        }
                    }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val listId = s.selectedList ?: s.lists.firstOrNull()?.id ?: return@ExtendedFloatingActionButton
                    editing = Task(listId = listId, title = "", createdAt = 0,
                        due = if (s.filter == TaskFilter.TODAY) s.today.toEpochDay() else null)
                },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Görev ekle") }, modifier = Modifier.testTag("fab_new"),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                TaskFilter.entries.forEach { f ->
                    FilterChip(selected = s.filter == f, onClick = { vm.setFilter(f) }, label = { Text("${f.label} ${s.counts[f] ?: 0}") },
                        modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            if (s.filter == TaskFilter.DONE && s.tasks.isNotEmpty()) {
                TextButton(onClick = vm::clearDone, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Icon(Icons.Outlined.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Tamamlananları temizle")
                }
            }
            when {
                s.loading -> LoadingBox()
                s.tasks.isEmpty() -> EmptyState(
                    if (s.filter == TaskFilter.DONE) Icons.Outlined.CheckCircle else Icons.Outlined.TaskAlt,
                    when (s.filter) {
                        TaskFilter.TODAY -> "Bugün için görev yok"
                        TaskFilter.UPCOMING -> "Önümüzdeki ${TaskLogic.UPCOMING_DAYS} gün boş"
                        TaskFilter.ALL -> "Yapılacak görev yok"
                        TaskFilter.DONE -> "Henüz tamamlanan görev yok"
                    },
                    "Yeni görev eklemek için \"Görev ekle\" düğmesine dokunun.",
                )
                else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(s.tasks, key = { it.id }) { task ->
                        TaskRow(task, TaskLogic.isOverdue(task, s.today), s.today, showList = s.selectedList == null,
                            listName = s.lists.firstOrNull { it.id == task.listId }?.name,
                            onToggle = { undoable(if (task.done) "Görev yeniden açıldı" else "Görev tamamlandı", task) { vm.toggleDone(task) } },
                            onClick = { editing = task })
                    }
                }
            }
        }
    }
    editing?.let { t ->
        TaskEditorSheet(
            task = t, lists = s.lists,
            onDismiss = { editing = null },
            onSave = { vm.save(it); editing = null },
            onDelete = if (t.id != 0L) ({ undoable("Görev silindi", t) { vm.delete(t) }; editing = null }) else null,
        )
    }
}

@Composable
private fun TaskRow(task: Task, overdue: Boolean, today: java.time.LocalDate, showList: Boolean, listName: String?,
                    onToggle: () -> Unit, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.done, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(task.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                if (task.note.isNotBlank()) Text(task.note, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    task.dueDate?.let { d ->
                        Icon(Icons.Outlined.Event, null, Modifier.size(14.dp), tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text((if (overdue) "Gecikti · " else "") + TrFormat.relativeDay(d, today), style = MaterialTheme.typography.labelSmall,
                            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                    }
                    if (showList && listName != null) Text(listName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (task.priority != Priority.NORMAL.value) {
                Icon(Icons.Filled.Flag, "Öncelik: ${Priority.of(task.priority).label}", tint = priorityColor(task.priority), modifier = Modifier.size(20.dp))
            }
        }
    }
}
