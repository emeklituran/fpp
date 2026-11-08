package com.galaxyoffline.stopwatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.ConfirmDialog
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.stopwatch.data.Session
import com.galaxyoffline.stopwatch.data.StopwatchLogic
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: StopwatchViewModel, onBack: () -> Unit) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    var detail by remember { mutableStateOf<Session?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Kayıtlı oturumlar") }, navigationIcon = { BackButton(onBack) }, actions = {
            if (!sessions.isNullOrEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Outlined.DeleteSweep, "Tümünü sil") }
        })
    }) { p ->
        val list = sessions
        when {
            list == null -> LoadingBox(Modifier.padding(p))
            list.isEmpty() -> Box(Modifier.padding(p)) { EmptyState(Icons.Outlined.Timer, "Kayıt yok", "Duraklattığınız kronometreyi kaydet simgesiyle saklayabilirsiniz.") }
            else -> LazyColumn(Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { s ->
                    val dt = Instant.ofEpochMilli(s.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    val laps = StopwatchLogic.decodeLaps(s.laps)
                    Card(onClick = { detail = s }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(TrFormat.stopwatch(s.totalMs), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace)
                                Text(listOf(s.label, "${laps.size} tur", "${TrFormat.relativeDay(dt.toLocalDate())} ${TrFormat.formatTime(dt.toLocalTime())}")
                                    .filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { vm.deleteSession(s) }) { Icon(Icons.Outlined.Delete, "Sil") }
                        }
                    }
                }
            }
        }
    }
    detail?.let { s ->
        AlertDialog(onDismissRequest = { detail = null }, title = { Text(s.label.ifBlank { "Oturum" } + " · " + TrFormat.stopwatch(s.totalMs)) },
            text = { Box(Modifier.heightIn(max = 400.dp)) { LapList(StopwatchLogic.decodeLaps(s.laps)) } },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("Kapat") } })
    }
    if (confirmClear) ConfirmDialog("Tüm oturumlar silinsin mi?", "Kayıtlı tüm kronometre oturumları silinecek.", "Sil",
        onConfirm = vm::clearSessions, onDismiss = { confirmClear = false }, destructive = true)
}
