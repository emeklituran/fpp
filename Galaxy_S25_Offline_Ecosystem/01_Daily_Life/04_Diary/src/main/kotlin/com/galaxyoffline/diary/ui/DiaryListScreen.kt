package com.galaxyoffline.diary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.core.designsystem.components.SearchField
import com.galaxyoffline.diary.data.DiaryEntry
import com.galaxyoffline.diary.data.Mood
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(vm: DiaryListViewModel, onOpen: (Long) -> Unit, onNew: () -> Unit, onStats: () -> Unit, onSettings: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Günlüğüm") },
                actions = {
                    IconButton(onClick = { searching = !searching; if (!searching) vm.setQuery("") }) {
                        Icon(if (searching) Icons.Outlined.Close else Icons.Outlined.Search, if (searching) "Aramayı kapat" else "Ara")
                    }
                    IconButton(onClick = onStats) { Icon(Icons.Outlined.BarChart, "İstatistikler") }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNew, icon = { Icon(Icons.Filled.Edit, null) }, text = { Text("Bugün ne oldu?") },
                modifier = Modifier.testTag("fab_new"))
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (searching) SearchField(s.query, vm::setQuery, "Günlükte ara", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            when {
                s.loading -> LoadingBox()
                s.entries.isEmpty() -> EmptyState(
                    Icons.AutoMirrored.Outlined.MenuBook,
                    if (s.query.isNotBlank()) "Sonuç yok" else "Günlüğünüz boş",
                    if (s.query.isNotBlank()) "Aramanızla eşleşen kayıt bulunamadı." else "Gününüzü ve ruh halinizi kaydetmeye başlayın. Kayıtlar yalnızca bu cihazda tutulur.",
                    actionLabel = if (s.query.isBlank()) "İlk kaydı yaz" else null, onAction = onNew,
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (s.query.isBlank()) item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.LocalFireDepartment, null)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${s.stats.currentStreak} günlük seri", style = MaterialTheme.typography.titleMedium)
                                    Text("Bu ay ${s.stats.thisMonth} kayıt · toplam ${s.stats.total}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    val groups = s.entries.groupBy { YearMonth.from(it.localDate) }
                    groups.forEach { (month, list) ->
                        item(key = "m$month") {
                            Text(TrFormat.formatMonthYear(month.atDay(1)), style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                        }
                        items(list, key = { it.id }) { EntryCard(it) { onOpen(it.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryCard(e: DiaryEntry, onClick: () -> Unit) {
    val mood = Mood.of(e.mood)
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.widthIn(min = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${e.localDate.dayOfMonth}", style = MaterialTheme.typography.headlineSmall)
                Text(e.localDate.format(TrFormat.dayName).take(3), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (e.title.isNotBlank()) Text(e.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(e.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.padding(start = 8.dp)) { Text(mood.emoji, fontSize = 26.sp, modifier = Modifier) }
        }
    }
}
