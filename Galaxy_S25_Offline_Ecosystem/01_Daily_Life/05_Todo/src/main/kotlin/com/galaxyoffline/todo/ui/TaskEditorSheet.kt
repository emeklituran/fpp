package com.galaxyoffline.todo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.todo.data.Priority
import com.galaxyoffline.todo.data.Task
import com.galaxyoffline.todo.data.TaskList
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(task: Task, lists: List<TaskList>, onDismiss: () -> Unit, onSave: (Task) -> Unit, onDelete: (() -> Unit)?) {
    var title by rememberSaveable { mutableStateOf(task.title) }
    var note by rememberSaveable { mutableStateOf(task.note) }
    var due by rememberSaveable { mutableStateOf(task.due) }
    var priority by rememberSaveable { mutableIntStateOf(task.priority) }
    var listId by rememberSaveable { mutableStateOf(task.listId) }
    var pick by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).imePadding().navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (task.id == 0L) "Yeni görev" else "Görevi düzenle", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(title, { title = it; error = false }, label = { Text("Görev") }, singleLine = true, isError = error,
                supportingText = if (error) ({ Text("Görev adı gerekli") }) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().testTag("task_title"))
            OutlinedTextField(note, { note = it }, label = { Text("Not (isteğe bağlı)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = { pick = true }, leadingIcon = { Icon(Icons.Outlined.Event, null) },
                    label = { Text(due?.let { TrFormat.relativeDay(LocalDate.ofEpochDay(it)) } ?: "Son tarih ekle") })
                if (due != null) IconButton(onClick = { due = null }) { Icon(Icons.Outlined.Close, "Tarihi kaldır") }
            }
            Text("Öncelik", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Priority.entries.forEachIndexed { i, p ->
                    SegmentedButton(selected = priority == p.value, onClick = { priority = p.value },
                        shape = SegmentedButtonDefaults.itemShape(i, Priority.entries.size)) { Text(p.label) }
                }
            }
            if (lists.size > 1) {
                Text("Liste", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    lists.take(4).forEach { l -> FilterChip(selected = listId == l.id, onClick = { listId = l.id }, label = { Text(l.name, maxLines = 1) }) }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    OutlinedButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(6.dp)); Text("Sil") }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    if (title.isBlank()) error = true
                    else onSave(task.copy(title = title, note = note, due = due, priority = priority, listId = listId))
                }, modifier = Modifier.testTag("task_save")) { Text("Kaydet") }
            }
        }
    }
    if (pick) {
        val st = rememberDatePickerState(initialSelectedDateMillis = (due?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now())
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { pick = false },
            confirmButton = { TextButton(onClick = {
                st.selectedDateMillis?.let { due = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                pick = false
            }) { Text("Tamam") } },
            dismissButton = { TextButton(onClick = { pick = false }) { Text("Vazgeç") } }) { DatePicker(st) }
    }
}
