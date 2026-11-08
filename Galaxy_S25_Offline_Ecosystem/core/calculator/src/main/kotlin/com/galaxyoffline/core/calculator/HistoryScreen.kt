package com.galaxyoffline.core.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.Input
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.ConfirmDialog
import com.galaxyoffline.core.designsystem.components.EmptyState
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: CalculatorViewModel, onBack: () -> Unit) {
    val items by vm.history.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Geçmiş") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (items.isNotEmpty()) IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Geçmişi temizle")
                    }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EmptyState(Icons.Outlined.History, "Geçmiş boş", "Yaptığınız hesaplar burada listelenir.")
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { entry ->
                    HistoryRow(
                        entry,
                        onUseResult = { vm.useResult(entry); onBack() },
                        onEdit = { vm.useExpression(entry); onBack() },
                        onDelete = { vm.deleteHistory(entry) },
                    )
                }
            }
        }
    }
    if (confirmClear) {
        ConfirmDialog("Geçmiş silinsin mi?", "Tüm hesap geçmişi kalıcı olarak silinecek.", "Sil",
            onConfirm = vm::clearHistory, onDismiss = { confirmClear = false }, destructive = true)
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onUseResult: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(onClick = onUseResult, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(entry.expression, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End)
                Text("= ${entry.result}", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.End)
                val time = Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
                Text(TrFormat.relativeDay(time.toLocalDate()) + " " + TrFormat.formatTime(time.toLocalTime()),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Seçenekler") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Sonucu kullan") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Input, null) },
                        onClick = { menu = false; onUseResult() })
                    DropdownMenuItem(text = { Text("İfadeyi düzenle") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                        onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Sil") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                        onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
