package com.galaxyoffline.alarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.alarm.data.AlarmLogic
import com.galaxyoffline.alarm.ring.AlarmRingService
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.ConfirmDialog
import com.galaxyoffline.core.designsystem.components.LoadingBox
import java.time.LocalDateTime

private val SNOOZE_OPTIONS = listOf(5, 10, 15, 20, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(vm: AlarmEditViewModel, onClose: () -> Unit) {
    val alarm by vm.alarm.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    val a = alarm

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if ((a?.id ?: 0L) == 0L) "Yeni alarm" else "Alarmı düzenle") }, navigationIcon = { BackButton(onClose) },
                actions = {
                    if ((a?.id ?: 0L) != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Sil") }
                    TextButton(onClick = { vm.save(onClose) }, modifier = Modifier.testTag("alarm_save")) { Text("Kaydet") }
                })
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { p ->
        if (a == null) { LoadingBox(Modifier.padding(p)); return@Scaffold }
        val picker = rememberTimePickerState(a.hour, a.minute, is24Hour = true)
        LaunchedEffect(picker.hour, picker.minute) { vm.update { it.copy(hour = picker.hour, minute = picker.minute) } }
        Column(Modifier.padding(p).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TimePicker(picker)
            val now = LocalDateTime.now()
            Text("Bu alarm ${AlarmLogic.untilText(now, AlarmLogic.nextTrigger(a, now))} çalacak",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text("Tekrar: ${AlarmLogic.describeDays(a.days)}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AlarmLogic.DAY_SHORT.forEachIndexed { i, d ->
                    FilterChip(selected = a.days and (1 shl i) != 0, onClick = { vm.update { it.copy(days = AlarmLogic.toggle(it.days, i)) } },
                        label = { Text(d.take(2)) })
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { vm.update { it.copy(days = AlarmLogic.WEEKDAYS) } }) { Text("Hafta içi") }
                TextButton(onClick = { vm.update { it.copy(days = AlarmLogic.EVERY_DAY) } }) { Text("Her gün") }
                TextButton(onClick = { vm.update { it.copy(days = 0) } }) { Text("Bir kez") }
            }
            OutlinedTextField(a.label, { v -> vm.update { it.copy(label = v.take(60)) } }, label = { Text("Etiket (isteğe bağlı)") },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("alarm_label"))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Titreşim", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(a.vibrate, { v -> vm.update { it.copy(vibrate = v) } })
            }
            Text("Erteleme süresi", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SNOOZE_OPTIONS.forEach { m -> FilterChip(a.snoozeMinutes == m, { vm.update { it.copy(snoozeMinutes = m) } }, label = { Text("$m dk") }) }
            }
            OutlinedButton(onClick = {
                ContextCompat.startForegroundService(context, AlarmRingService.ringIntent(context, a.id, preview = true))
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Önizle (sesi ve ekranı dene)")
            }
            Button(onClick = { vm.save(onClose) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Kaydet") }
        }
    }
    if (confirmDelete) ConfirmDialog("Alarm silinsin mi?", "Bu alarm kalıcı olarak silinecek.", "Sil",
        onConfirm = { vm.delete(onClose) }, onDismiss = { confirmDelete = false }, destructive = true)
}

