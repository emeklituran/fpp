package com.galaxyoffline.reminder.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.reminder.data.Reminder
import com.galaxyoffline.reminder.data.Repeat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorSheet(r: Reminder, onDismiss: () -> Unit, onSave: (Reminder) -> Unit, onDelete: (() -> Unit)?) {
    val zone = ZoneId.systemDefault()
    val initial = Instant.ofEpochMilli(if (r.done) maxOf(r.triggerAt, System.currentTimeMillis() + 3_600_000) else r.triggerAt).atZone(zone).toLocalDateTime()
    var title by rememberSaveable { mutableStateOf(r.title) }
    var note by rememberSaveable { mutableStateOf(r.note) }
    var day by rememberSaveable { mutableStateOf(initial.toLocalDate().toEpochDay()) }
    var minuteOfDay by rememberSaveable { mutableStateOf(initial.hour * 60 + initial.minute) }
    var repeat by rememberSaveable { mutableStateOf(r.repeatRule) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(day)
    val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    val at = LocalDateTime.of(date, time)
    val inPast = !at.isAfter(LocalDateTime.now())

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).imePadding().navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (r.id == 0L) "Yeni hatırlatma" else "Hatırlatmayı düzenle", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(title, { title = it.take(120) }, label = { Text("Ne hatırlatılsın?") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), modifier = Modifier.fillMaxWidth().testTag("rem_title"))
            OutlinedTextField(note, { note = it.take(1000) }, label = { Text("Not (isteğe bağlı)") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { pickDate = true }, label = { Text(TrFormat.formatDate(date)) }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) })
                AssistChip(onClick = { pickTime = true }, label = { Text(TrFormat.formatTime(time)) }, leadingIcon = { Icon(Icons.Outlined.Schedule, null) })
            }
            if (inPast) Text("Seçilen zaman geçmişte.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Repeat.entries.forEach { rp -> FilterChip(selected = repeat == rp, onClick = { repeat = rp }, label = { Text(rp.label) }) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) OutlinedButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(6.dp)); Text("Sil") }
                Spacer(Modifier.weight(1f))
                Button(enabled = title.isNotBlank() && !inPast, modifier = Modifier.testTag("rem_save"), onClick = {
                    val ms = at.atZone(zone).toInstant().toEpochMilli()
                    onSave(r.copy(title = title, note = note, anchorAt = ms, triggerAt = ms, repeat = repeat.name))
                }) { Text("Kaydet") }
            }
        }
    }
    if (pickDate) {
        val st = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { pickDate = false },
            confirmButton = { TextButton(onClick = {
                st.selectedDateMillis?.let { day = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                pickDate = false
            }) { Text("Tamam") } },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Vazgeç") } }) { DatePicker(st) }
    }
    if (pickTime) {
        val st = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        AlertDialog(onDismissRequest = { pickTime = false }, title = { Text("Saat seçin") }, text = { TimePicker(st) },
            confirmButton = { TextButton(onClick = { minuteOfDay = st.hour * 60 + st.minute; pickTime = false }) { Text("Tamam") } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Vazgeç") } })
    }
}
