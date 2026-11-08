package com.galaxyoffline.diary.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.ConfirmDialog
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.diary.data.Mood
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(vm: EntryViewModel, onClose: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }

    val back = { if (s.dirty) confirmDiscard = true else onClose() }
    BackHandler(onBack = back)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s.id == 0L) "Yeni kayıt" else "Kaydı düzenle") },
                navigationIcon = { BackButton(back) },
                actions = {
                    if (s.id != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Sil") }
                    TextButton(onClick = { vm.save(onClose) }, modifier = Modifier.testTag("save")) { Text("Kaydet") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            !s.loaded -> LoadingBox(Modifier.padding(padding))
            s.missing -> Box(Modifier.padding(padding)) { EmptyState(Icons.Outlined.Delete, "Kayıt bulunamadı", "Bu kayıt silinmiş olabilir.", actionLabel = "Geri dön", onAction = onClose) }
            else -> Column(
                Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AssistChip(onClick = { pickDate = true }, label = { Text(TrFormat.formatDateWithDay(s.date)) },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) })
                Text("Bugün nasıl hissediyorsunuz?", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Mood.entries.forEach { m ->
                        FilterChip(selected = s.mood == m, onClick = { vm.setMood(m) }, label = { Text(m.emoji, style = MaterialTheme.typography.titleLarge) },
                            modifier = Modifier.semantics { contentDescription = m.label })
                    }
                }
                Text(s.mood.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(s.title, vm::setTitle, label = { Text("Başlık (isteğe bağlı)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("entry_title"))
                OutlinedTextField(s.content, vm::setContent, label = { Text("Neler yaşadınız?") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).testTag("entry_content"))
                Text("${TrFormat.wordCount(s.content)} kelime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = s.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { vm.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    pickDate = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Vazgeç") } },
        ) { DatePicker(state) }
    }
    if (confirmDiscard) ConfirmDialog("Değişiklikler kaydedilmedi", "Çıkarsanız yazdıklarınız kaybolacak.", "Kaydetmeden çık",
        onConfirm = onClose, onDismiss = { confirmDiscard = false }, destructive = true)
    if (confirmDelete) ConfirmDialog("Kayıt silinsin mi?", "Bu günlük kaydı kalıcı olarak silinecek.", "Sil",
        onConfirm = { vm.delete(onClose) }, onDismiss = { confirmDelete = false }, destructive = true)
}
