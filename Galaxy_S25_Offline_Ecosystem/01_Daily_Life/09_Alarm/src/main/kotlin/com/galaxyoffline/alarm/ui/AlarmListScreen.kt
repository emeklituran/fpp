package com.galaxyoffline.alarm.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.NotificationsOff
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.alarm.data.Alarm
import com.galaxyoffline.alarm.data.AlarmLogic
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.InfoBanner
import com.galaxyoffline.core.designsystem.components.LoadingBox
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(vm: AlarmListViewModel, onEdit: (Long) -> Unit, onSettings: () -> Unit) {
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val perms by vm.permissions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(30_000) } }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    LifecycleResumeEffect(Unit) { vm.refreshPermissions(); onPauseOrDispose {} }
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshPermissions() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Alarm") }, actions = { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") } }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { onEdit(-1) }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Alarm ekle") },
                modifier = Modifier.testTag("fab_new"))
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { p ->
        val list = alarms
        if (list == null) { LoadingBox(Modifier.padding(p)); return@Scaffold }
        LazyColumn(Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!perms.notifications) item {
                InfoBanner("Bildirim izni kapalı: alarm çaldığında ekran gösterilemez.", isError = true, icon = Icons.Outlined.NotificationsOff,
                    actionLabel = "İzin ver", onAction = {
                        if (Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    })
            }
            if (!perms.fullScreen) item {
                InfoBanner("Tam ekran alarm izni kapalı: kilit ekranında alarm yalnızca bildirim olarak görünür.", icon = Icons.Outlined.WarningAmber,
                    actionLabel = "Ayarları aç", onAction = {
                        runCatching { context.startActivity(Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT", Uri.parse("package:${context.packageName}"))) }
                    })
            }
            if (!perms.exact) item {
                InfoBanner("Tam zamanlı alarm izni yok; alarm gecikebilir.", isError = true, icon = Icons.Outlined.WarningAmber, actionLabel = "Ayarları aç",
                    onAction = { runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) } })
            }
            val next = list.filter { it.enabled }.map { it to AlarmLogic.nextTrigger(it, now) }.minByOrNull { it.second }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        if (next == null) {
                            Text("Etkin alarm yok", style = MaterialTheme.typography.titleMedium)
                        } else {
                            Text("Sonraki alarm", style = MaterialTheme.typography.labelLarge)
                            Text("${TrFormat.relativeDay(next.second.toLocalDate())} ${TrFormat.formatTime(next.second.toLocalTime())}",
                                style = MaterialTheme.typography.headlineSmall)
                            Text(AlarmLogic.untilText(now, next.second), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (list.isEmpty()) item { EmptyState(Icons.Outlined.Alarm, "Alarm yok", "Uyanmak veya bir şeyi unutmamak için alarm kurun.") }
            items(list, key = { it.id }) { a -> AlarmCard(a, onClick = { onEdit(a.id) }, onToggle = { vm.setEnabled(a, it) }) }
        }
    }
}

@Composable
private fun AlarmCard(a: Alarm, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val color = if (a.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(TrFormat.formatTime(a.time), fontSize = 44.sp, color = color)
                Text(listOf(a.label, AlarmLogic.describeDays(a.days)).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(a.enabled, onToggle, modifier = Modifier.testTag("switch_${a.id}"))
        }
    }
}
