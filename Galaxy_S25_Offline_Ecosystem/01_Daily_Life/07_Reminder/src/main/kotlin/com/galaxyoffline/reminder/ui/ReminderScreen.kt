package com.galaxyoffline.reminder.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.InfoBanner
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.core.designsystem.components.SectionHeader
import com.galaxyoffline.reminder.data.Reminder
import com.galaxyoffline.reminder.data.Repeat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(vm: ReminderViewModel, onSettings: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val perms by vm.permissions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Reminder?>(null) }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    LifecycleResumeEffect(Unit) { vm.refreshPermissions(); onPauseOrDispose {} }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshPermissions() }

    fun delete(r: Reminder) {
        vm.delete(r)
        scope.launch {
            if (snackbar.showSnackbar("\"${r.title}\" silindi", "Geri al", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) vm.restore(r)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hatırlatıcı") }, actions = { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") } }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { editing = newReminder() }, icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Hatırlatma ekle") }, modifier = Modifier.testTag("fab_new"))
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { p ->
        if (s.loading) { LoadingBox(Modifier.padding(p)); return@Scaffold }
        LazyColumn(Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!perms.notifications) item {
                InfoBanner("Bildirim izni kapalı. Hatırlatmalar gösterilemez.", isError = true, icon = Icons.Outlined.NotificationsOff,
                    actionLabel = "İzin ver", onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    })
            }
            if (!perms.exact) item {
                InfoBanner("Tam zamanlı alarm izni verilmedi; Android hatırlatmaları birkaç dakika geciktirebilir.",
                    icon = Icons.Outlined.WarningAmber, actionLabel = "Ayarları aç", onAction = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                        }
                    })
            }
            if (s.upcoming.isEmpty() && s.past.isEmpty()) {
                item { EmptyState(Icons.Outlined.NotificationsActive, "Hatırlatma yok", "İlaç, toplantı, fatura… Zamanı gelince size bildirim gönderelim.") }
            }
            if (s.upcoming.isNotEmpty()) item { SectionHeader("Yaklaşan") }
            items(s.upcoming, key = { it.id }) { r -> ReminderCard(r, onClick = { editing = r }, onToggle = { vm.setEnabled(r, it) }) }
            if (s.past.isNotEmpty()) item {
                SectionHeader("Geçmiş") { TextButton(onClick = vm::clearPast) { Text("Temizle") } }
            }
            items(s.past, key = { it.id }) { r -> ReminderCard(r, onClick = { editing = r }, onToggle = null) }
        }
    }
    editing?.let { r ->
        ReminderEditorSheet(r, onDismiss = { editing = null }, onSave = { vm.save(it); editing = null },
            onDelete = if (r.id != 0L) ({ editing = null; delete(r) }) else null)
    }
}

@Composable
private fun ReminderCard(r: Reminder, onClick: () -> Unit, onToggle: ((Boolean) -> Unit)?) {
    val dt = r.triggerDateTime()
    val active = r.enabled && !r.done
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${TrFormat.relativeDay(dt.toLocalDate())} · ${TrFormat.formatTime(dt.toLocalTime())}",
                    style = MaterialTheme.typography.bodyMedium, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (r.repeatRule != Repeat.NONE) {
                        Icon(Icons.Outlined.Repeat, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
                        Text(r.repeatRule.label, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (r.note.isNotBlank()) Text(r.note, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onToggle != null) Switch(checked = r.enabled, onCheckedChange = onToggle)
            else Box { Icon(Icons.Outlined.AlarmOff, "Geçmiş", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun newReminder(): Reminder {
    val at = java.time.LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    val ms = at.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    return Reminder(title = "", anchorAt = ms, triggerAt = ms, createdAt = 0)
}
