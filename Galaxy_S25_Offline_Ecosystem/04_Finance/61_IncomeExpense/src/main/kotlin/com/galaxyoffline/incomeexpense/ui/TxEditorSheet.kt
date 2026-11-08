package com.galaxyoffline.incomeexpense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.incomeexpense.data.Category
import com.galaxyoffline.incomeexpense.data.Method
import com.galaxyoffline.incomeexpense.data.Tx
import com.galaxyoffline.incomeexpense.data.TxType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TxEditorSheet(t: Tx, onDismiss: () -> Unit, onSave: (Tx) -> Unit, onDelete: (() -> Unit)?) {
    var type by rememberSaveable { mutableStateOf(t.txType) }
    var amount by rememberSaveable { mutableStateOf(if (t.amount > 0) TrFormat.number(t.amount, 2, false) else "") }
    var category by rememberSaveable { mutableStateOf(t.cat) }
    var date by rememberSaveable { mutableLongStateOf(t.date) }
    var note by rememberSaveable { mutableStateOf(t.note) }
    var method by rememberSaveable { mutableStateOf(Method.entries.firstOrNull { it.name == t.method } ?: Method.CARD) }
    var pick by rememberSaveable { mutableStateOf(false) }
    val value = TrFormat.parseDecimal(amount)
    if (category.type != type) category = Category.forType(type).first()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TxType.entries.forEachIndexed { i, tt -> SegmentedButton(type == tt, { type = tt }, SegmentedButtonDefaults.itemShape(i, 2)) { Text(tt.label) } }
            }
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(14) }, label = { Text("Tutar") }, suffix = { Text("₺") },
                singleLine = true, isError = amount.isNotEmpty() && (value == null || value <= 0), textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().testTag("tx_amount"))
            Text("Kategori", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Category.forType(type).forEach { c -> FilterChip(category == c, { category = c }, label = { Text("${c.emoji} ${c.label}") }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = { pick = true }, label = { Text(TrFormat.relativeDay(LocalDate.ofEpochDay(date))) }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) })
                Method.entries.forEach { m -> FilterChip(method == m, { method = m }, label = { Text(m.label) }) }
            }
            OutlinedTextField(note, { note = it.take(300) }, label = { Text("Not (isteğe bağlı)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) OutlinedButton(onClick = onDelete) { Text("Sil") }
                Spacer(Modifier.weight(1f))
                Button(enabled = value != null && value > 0, modifier = Modifier.testTag("tx_save"), onClick = {
                    onSave(t.copy(type = type.name, amount = value!!, category = category.name, date = date, note = note, method = method.name))
                }) { Text("Kaydet") }
            }
        }
    }
    if (pick) {
        val st = rememberDatePickerState(initialSelectedDateMillis = LocalDate.ofEpochDay(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { pick = false }, confirmButton = { TextButton(onClick = {
            st.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }; pick = false
        }) { Text("Tamam") } }, dismissButton = { TextButton(onClick = { pick = false }) { Text("Vazgeç") } }) { DatePicker(st) }
    }
}
