package com.galaxyoffline.notes.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.ConfirmDialog
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.InfoBanner
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.core.designsystem.components.SearchField
import com.galaxyoffline.notes.data.Note
import com.galaxyoffline.notes.data.NoteFilter
import com.galaxyoffline.notes.data.NoteQuery
import com.galaxyoffline.notes.data.NoteSort
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(vm: NotesViewModel, onOpen: (Long) -> Unit, onNew: () -> Unit, onSettings: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var sortMenu by remember { mutableStateOf(false) }
    var confirmEmpty by rememberSaveable { mutableStateOf(false) }
    var trashDialog by remember { mutableStateOf<Note?>(null) }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }

    fun trash(note: Note) {
        vm.moveToTrash(note)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar("Not çöp kutusuna taşındı", "Geri al", duration = SnackbarDuration.Short) ==
                SnackbarResult.ActionPerformed) vm.undoTrash(note)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notlar") },
                actions = {
                    IconButton(onClick = { vm.setGrid(!s.grid) }) {
                        Icon(if (s.grid) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                            contentDescription = if (s.grid) "Liste görünümü" else "Izgara görünümü")
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sırala") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            NoteSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label, fontWeight = if (sort == s.sort) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = { vm.setSort(sort); sortMenu = false },
                                )
                            }
                        }
                    }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Ayarlar") }
                },
            )
        },
        floatingActionButton = {
            if (s.filter == NoteFilter.NOTES) {
                ExtendedFloatingActionButton(onClick = onNew, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Yeni not") },
                    modifier = Modifier.testTag("fab_new"))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(s.query, vm::setQuery, "Notlarda ara", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                NoteFilter.entries.forEach { f ->
                    FilterChip(
                        selected = s.filter == f,
                        onClick = { vm.setFilter(f) },
                        label = { Text("${f.label} (${s.counts[f] ?: 0})") },
                        leadingIcon = {
                            Icon(when (f) {
                                NoteFilter.NOTES -> Icons.Outlined.StickyNote2
                                NoteFilter.ARCHIVE -> Icons.Outlined.Inventory2
                                NoteFilter.TRASH -> Icons.Outlined.Delete
                            }, null, Modifier.size(18.dp))
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            when {
                s.loading -> LoadingBox()
                s.notes.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.StickyNote2,
                    title = if (s.query.isNotBlank()) "Sonuç bulunamadı" else when (s.filter) {
                        NoteFilter.NOTES -> "Henüz not yok"
                        NoteFilter.ARCHIVE -> "Arşiv boş"
                        NoteFilter.TRASH -> "Çöp kutusu boş"
                    },
                    message = if (s.query.isNotBlank()) "\"${s.query}\" için eşleşen not yok." else when (s.filter) {
                        NoteFilter.NOTES -> "İlk notunuzu yazmak için \"Yeni not\" düğmesine dokunun."
                        NoteFilter.ARCHIVE -> "Arşivlediğiniz notlar burada saklanır."
                        NoteFilter.TRASH -> "Silinen notlar ${NoteQuery.TRASH_DAYS} gün burada bekler."
                    },
                    actionLabel = if (s.filter == NoteFilter.NOTES && s.query.isBlank()) "Yeni not" else null,
                    onAction = onNew,
                )
                else -> LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(if (s.grid) 2 else 1),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().testTag("notes_list"),
                ) {
                    if (s.filter == NoteFilter.TRASH) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            InfoBanner(
                                "Çöp kutusundaki notlar ${NoteQuery.TRASH_DAYS} gün sonra kalıcı olarak silinir.",
                                actionLabel = "Çöpü boşalt", onAction = { confirmEmpty = true },
                            )
                        }
                    }
                    items(s.notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { if (s.filter == NoteFilter.TRASH) trashDialog = note else onOpen(note.id) },
                            menu = { close ->
                                when (s.filter) {
                                    NoteFilter.TRASH -> {
                                        MenuItem("Geri yükle", Icons.Outlined.RestoreFromTrash) { close(); vm.restore(note) }
                                        MenuItem("Kalıcı olarak sil", Icons.Outlined.DeleteForever) { close(); vm.deleteForever(note) }
                                    }
                                    else -> {
                                        if (!note.archived) MenuItem(if (note.pinned) "Sabitlemeyi kaldır" else "Sabitle",
                                            Icons.Outlined.PushPin) { close(); vm.togglePin(note) }
                                        MenuItem(if (note.archived) "Arşivden çıkar" else "Arşivle",
                                            if (note.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive) {
                                            close(); vm.setArchived(note, !note.archived)
                                        }
                                        MenuItem("Çöp kutusuna taşı", Icons.Outlined.Delete) { close(); trash(note) }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
    if (confirmEmpty) {
        ConfirmDialog("Çöp kutusu boşaltılsın mı?", "Çöp kutusundaki tüm notlar kalıcı olarak silinecek.", "Boşalt",
            onConfirm = vm::emptyTrash, onDismiss = { confirmEmpty = false }, destructive = true)
    }
    trashDialog?.let { note ->
        ConfirmDialog("Not çöp kutusunda", "Bu notu düzenlemek için önce geri yükleyin.", "Geri yükle",
            onConfirm = { vm.restore(note) }, onDismiss = { trashDialog = null })
    }
}

@Composable
private fun MenuItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text) }, leadingIcon = { Icon(icon, null) }, onClick = onClick)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, menu: @Composable (close: () -> Unit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Card(
            colors = CardDefaults.cardColors(containerColor = noteColor(note.color)),
            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { open = true },
                onLongClickLabel = "Seçenekler"),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    if (note.title.isNotBlank()) {
                        Text(note.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f))
                    } else Spacer(Modifier.weight(1f))
                    if (note.pinned) Icon(Icons.Filled.PushPin, "Sabitlendi", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                if (note.content.isNotBlank()) {
                    Text(note.content, style = MaterialTheme.typography.bodyMedium, maxLines = 8, overflow = TextOverflow.Ellipsis)
                }
                val dt = Instant.ofEpochMilli(note.updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                Text(TrFormat.relativeDay(dt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) { menu { open = false } }
        Spacer(Modifier.width(0.dp))
    }
}
