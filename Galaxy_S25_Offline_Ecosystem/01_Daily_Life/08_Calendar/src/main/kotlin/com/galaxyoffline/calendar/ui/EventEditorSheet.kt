package com.galaxyoffline.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.galaxyoffline.calendar.data.Event
import com.galaxyoffline.calendar.data.EventRepeat
import com.galaxyoffline.core.common.TrFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorSheet(e: Event, onDismiss: () -> Unit, onSave: (Event) -> Unit, onDelete: (() -> Unit)?) {
    var title by rememberSaveable { mutableStateOf(e.title) }
    var note by rememberSaveable { mutableStateOf(e.note) }
    var date by rememberSaveable { mutableLongStateOf(e.date) }
    var allDay by rememberSaveable { mutableStateOf(e.allDay) }
    var start by rememberSaveable { mutableIntStateOf(e.startMinute ?: 9 * 60) }
    var end by rememberSaveable { mutableIntStateOf(e.endMinute ?: 10 * 60) }
    var color by rememberSaveable { mutableIntStateOf(e.color) }
    var repeat by rememberSaveable { mutableStateOf(EventRepeat.of(e.repeat)) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(0) } // 0 yok, 1 başlangıç, 2 bitiş
    val invalidTime = !allDay && end < start

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding()
            .padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (e.id == 0L) "Yeni etkinlik" else "Etkinliği düzenle", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(title, { title = it.take(120) }, label = { Text("Başlık") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("ev_title"))
            AssistChip(onClick = { pickDate = true }, label = { Text(TrFormat.formatDateWithDay(LocalDate.ofEpochDay(date))) },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tüm gün", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(allDay, { allDay = it })
            }
            if (!allDay) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { pickTime = 1 }, label = { Text("Başlangıç ${fmt(start)}") }, leadingIcon = { Icon(Icons.Outlined.Schedule, null) })
                AssistChip(onClick = { pickTime = 2 }, label = { Text("Bitiş ${fmt(end)}") })
            }
            if (invalidTime) Text("Bitiş saati başlangıçtan önce olamaz.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventRepeat.entries.forEach { r -> FilterChip(repeat == r, { repeat = r }, label = { Text(r.label) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EventColors.forEachIndexed { i, (name, c) ->
                    Box(Modifier.size(32.dp).background(c, CircleShape)
                        .then(if (i == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                        .clickable { color = i }.semantics { contentDescription = name }, contentAlignment = Alignment.Center) {
                        if (i == color) Icon(Icons.Outlined.Check, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            OutlinedTextField(note, { note = it.take(2000) }, label = { Text("Not / konum (isteğe bağlı)") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) OutlinedButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(6.dp)); Text("Sil") }
                Spacer(Modifier.weight(1f))
                Button(enabled = title.isNotBlank() && !invalidTime, modifier = Modifier.testTag("ev_save"), onClick = {
                    onSave(e.copy(title = title, note = note, date = date, allDay = allDay, startMinute = if (allDay) null else start,
                        endMinute = if (allDay) null else end, color = color, repeat = repeat.name))
                }) { Text("Kaydet") }
            }
        }
    }
    if (pickDate) {
        val st = rememberDatePickerState(initialSelectedDateMillis = LocalDate.ofEpochDay(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { pickDate = false },
            confirmButton = { TextButton(onClick = {
                st.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                pickDate = false
            }) { Text("Tamam") } },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Vazgeç") } }) { DatePicker(st) }
    }
    if (pickTime != 0) {
        val cur = if (pickTime == 1) start else end
        val st = rememberTimePickerState(cur / 60, cur % 60, is24Hour = true)
        AlertDialog(onDismissRequest = { pickTime = 0 }, title = { Text(if (pickTime == 1) "Başlangıç saati" else "Bitiş saati") }, text = { TimePicker(st) },
            confirmButton = { TextButton(onClick = {
                val v = st.hour * 60 + st.minute
                if (pickTime == 1) { start = v; if (end < v) end = (v + 60).coerceAtMost(23 * 60 + 59) } else end = v
                pickTime = 0
            }) { Text("Tamam") } },
            dismissButton = { TextButton(onClick = { pickTime = 0 }) { Text("Vazgeç") } })
    }
}

private fun fmt(m: Int) = TrFormat.formatTime(LocalTime.of(m / 60, m % 60))
