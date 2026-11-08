package com.galaxyoffline.billreminder.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.billreminder.data.Bill
import com.galaxyoffline.billreminder.data.BillKind
import com.galaxyoffline.billreminder.data.BillLogic
import com.galaxyoffline.billreminder.data.BillStatus
import com.galaxyoffline.billreminder.data.Recurrence
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.InfoBanner
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.core.designsystem.components.SectionHeader
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(vm: BillViewModel, onHistory: (Long) -> Unit, onSettings: () -> Unit) {
    val bills by vm.bills.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Bill?>(null) }
    var notifOk by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) { notifOk = NotificationManagerCompat.from(ctx).areNotificationsEnabled(); onPauseOrDispose {} }
    val req = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    val today = LocalDate.now()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Faturalar") }, actions = { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { editing = Bill(name = "", amount = 0.0, dueDate = today.plusDays(7).toEpochDay(),
            anchorDay = today.plusDays(7).dayOfMonth, createdAt = 0) }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Fatura ekle") }, modifier = Modifier.testTag("fab_new")) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { p ->
        val list = bills ?: run { LoadingBox(Modifier.padding(p)); return@Scaffold }
        LazyColumn(Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!notifOk) item {
                InfoBanner("Bildirim izni kapalı; son ödeme hatırlatmaları gösterilemez.", isError = true, icon = Icons.Outlined.NotificationsOff, actionLabel = "İzin ver",
                    onAction = { if (Build.VERSION.SDK_INT >= 33) req.launch(Manifest.permission.POST_NOTIFICATIONS) })
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        val m = BillLogic.monthlyCost(list)
                        Text("Aylık düzenli giderler", style = MaterialTheme.typography.labelLarge)
                        Text(TrFormat.money(m), style = MaterialTheme.typography.headlineSmall)
                        Text("Yıllık yaklaşık ${TrFormat.money(m * 12)} · ${list.count { it.active }} etkin kayıt", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (list.isEmpty()) item { EmptyState(Icons.Outlined.ReceiptLong, "Fatura yok", "Elektrik, su, internet, kira ve aboneliklerinizi ekleyin; son ödeme yaklaşınca hatırlatalım.") }
            BillStatus.entries.forEach { st ->
                val group = list.filter { BillLogic.status(it, today) == st }
                if (group.isNotEmpty()) {
                    item(key = "h$st") { SectionHeader("${st.label} (${group.size})") }
                    items(group, key = { it.id }) { b -> BillCard(b, st, today, onPay = { vm.markPaid(b) }, onEdit = { editing = b }, onHistory = { onHistory(b.id) }) }
                }
            }
        }
    }
    editing?.let { b ->
        BillEditor(b, onDismiss = { editing = null }, onSave = { vm.save(it) { editing = null } }, onDelete = if (b.id != 0L) ({
            editing = null; vm.delete(b)
            scope.launch { if (snackbar.showSnackbar("\"${b.name}\" silindi", "Geri al", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) vm.restore(b) }
        }) else null)
    }
}

@Composable
private fun BillCard(b: Bill, st: BillStatus, today: LocalDate, onPay: () -> Unit, onEdit: () -> Unit, onHistory: () -> Unit) {
    val warn = st == BillStatus.OVERDUE
    Card(onClick = onEdit, colors = CardDefaults.cardColors(containerColor = if (warn) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(b.kindEnum.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(b.name, style = MaterialTheme.typography.titleSmall)
                    Text("${b.rec.label} · ${if (b.active) TrFormat.formatDate(b.due) else "tamamlandı"}", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(TrFormat.money(b.amount), style = MaterialTheme.typography.titleSmall)
                    Text(BillLogic.daysText(b, today), style = MaterialTheme.typography.labelSmall,
                        color = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onHistory) { Text("Geçmiş") }
                if (b.active) FilledTonalButton(onClick = onPay, modifier = Modifier.testTag("pay_${b.id}")) { Text("Ödendi") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BillEditor(b: Bill, onDismiss: () -> Unit, onSave: (Bill) -> Unit, onDelete: (() -> Unit)?) {
    var name by rememberSaveable { mutableStateOf(b.name) }
    var amount by rememberSaveable { mutableStateOf(if (b.amount > 0) TrFormat.number(b.amount, 2, false) else "") }
    var kind by rememberSaveable { mutableStateOf(b.kindEnum) }
    var rec by rememberSaveable { mutableStateOf(b.rec) }
    var due by rememberSaveable { mutableLongStateOf(b.dueDate) }
    var remind by rememberSaveable { mutableIntStateOf(b.remindDays) }
    var pick by rememberSaveable { mutableStateOf(false) }
    val value = if (amount.isBlank()) 0.0 else TrFormat.parseDecimal(amount)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (b.id == 0L) "Yeni fatura / abonelik" else "Düzenle", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it.take(80) }, label = { Text("Ad (ör. Elektrik, Netflix)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("bill_name"))
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(12) }, label = { Text("Tutar (tahmini)") }, suffix = { Text("₺") },
                singleLine = true, isError = value == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().testTag("bill_amount"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { BillKind.entries.forEach { k -> FilterChip(kind == k, { kind = k }, label = { Text("${k.emoji} ${k.label}") }) } }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Recurrence.entries.forEach { r -> FilterChip(rec == r, { rec = r }, label = { Text(r.label) }) } }
            AssistChip(onClick = { pick = true }, label = { Text("Son ödeme: ${TrFormat.formatDate(LocalDate.ofEpochDay(due))}") }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) })
            Text("Kaç gün önce hatırlatılsın?", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(0, 1, 3, 5, 7).forEach { d -> FilterChip(remind == d, { remind = d }, label = { Text(if (d == 0) "O gün" else "$d gün") }) } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) OutlinedButton(onClick = onDelete) { Text("Sil") }
                Spacer(Modifier.weight(1f))
                Button(enabled = name.isNotBlank() && value != null, modifier = Modifier.testTag("bill_save"), onClick = {
                    onSave(b.copy(name = name, amount = value ?: 0.0, kind = kind.name, recurrence = rec.name, dueDate = due, remindDays = remind, active = true))
                }) { Text("Kaydet") }
            }
        }
    }
    if (pick) {
        val st = rememberDatePickerState(initialSelectedDateMillis = LocalDate.ofEpochDay(due).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { pick = false }, confirmButton = { TextButton(onClick = {
            st.selectedDateMillis?.let { due = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }; pick = false
        }) { Text("Tamam") } }, dismissButton = { TextButton(onClick = { pick = false }) { Text("Vazgeç") } }) { DatePicker(st) }
    }
}
