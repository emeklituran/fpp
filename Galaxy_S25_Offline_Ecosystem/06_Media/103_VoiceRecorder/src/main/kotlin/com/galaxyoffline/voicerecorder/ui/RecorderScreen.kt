package com.galaxyoffline.voicerecorder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.ConfirmDialog
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.InfoBanner
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.voicerecorder.data.RecFiles
import com.galaxyoffline.voicerecorder.data.RecStatus
import com.galaxyoffline.voicerecorder.data.RecorderService
import com.galaxyoffline.voicerecorder.data.Recording
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreen(vm: RecorderViewModel, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    val rec by RecorderService.state.collectAsStateWithLifecycle()
    val list by vm.recordings.collectAsStateWithLifecycle()
    val play by vm.play.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var denied by remember { mutableStateOf(false) }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(rec.error) { rec.error?.let { snackbar.showSnackbar(it); RecorderService.clearError() } }
    fun startRec() { vm.stopPlayer(); RecorderService.start(ctx, vm.bitrate.value) }
    val perms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.RECORD_AUDIO] == true) { denied = false; startRec() } else denied = true
    }
    fun onRecord() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRec()
        else perms.launch(buildList { add(Manifest.permission.RECORD_AUDIO); if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }.toTypedArray())
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Ses Kaydedici") }, actions = { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") } }) },
        snackbarHost = { SnackbarHost(snackbar) }) { p ->
        Column(Modifier.padding(p).fillMaxSize()) {
            RecorderPanel(rec.status, rec::elapsed, rec.amplitude, onRecord = ::onRecord, onPause = { RecorderService.pause(ctx) },
                onResume = { RecorderService.resume(ctx) }, onStop = { RecorderService.stop(ctx) })
            if (denied) InfoBanner("Kayıt için mikrofon izni gerekli. Ayarlar › Uygulamalar › Ses Kaydedici › İzinler bölümünden açabilirsiniz.", Modifier.padding(16.dp),
                isError = true, icon = Icons.Outlined.MicOff)
            val items = list ?: run { LoadingBox(); return@Column }
            if (items.isEmpty()) { EmptyState(Icons.Filled.Mic, "Kayıt yok", "Kırmızı düğmeye dokunarak kayda başlayın. Kayıtlar yalnızca bu telefonda saklanır."); return@Column }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("rec_list")) {
                items(items, key = { it.id }) { r -> RecordingRow(r, play.takeIf { it.id == r.id }, vm, onPlay = { vm.toggle(r) }) }
            }
        }
    }
}

@Composable
private fun RecorderPanel(status: RecStatus, elapsed: (Long) -> Long, amplitude: Float, onRecord: () -> Unit, onPause: () -> Unit, onResume: () -> Unit, onStop: () -> Unit) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val levels = remember { mutableStateListOf<Float>() }
    LaunchedEffect(status) {
        if (status == RecStatus.IDLE) levels.clear()
        while (status == RecStatus.RECORDING) { now = SystemClock.elapsedRealtime(); kotlinx.coroutines.delay(250) }
        now = SystemClock.elapsedRealtime()
    }
    LaunchedEffect(amplitude) { if (status == RecStatus.RECORDING) { levels.add(amplitude); if (levels.size > 60) levels.removeAt(0) } }
    val bar = MaterialTheme.colorScheme.primary
    Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (status == RecStatus.IDLE) "00:00" else RecFiles.duration(elapsed(now)), fontSize = 48.sp, style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.testTag("rec_time"))
            Text(when (status) { RecStatus.IDLE -> "Kayda hazır"; RecStatus.RECORDING -> "Kaydediliyor…"; RecStatus.PAUSED -> "Duraklatıldı" },
                style = MaterialTheme.typography.labelLarge)
            Canvas(Modifier.fillMaxWidth().height(56.dp).padding(vertical = 8.dp)) {
                val w = size.width / 60f
                levels.forEachIndexed { i, l ->
                    val h = (0.06f + l * 0.94f) * size.height
                    drawRoundRect(bar, Offset(i * w + w * 0.2f, (size.height - h) / 2), Size(w * 0.6f, h), CornerRadius(w * 0.3f))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                if (status == RecStatus.IDLE) {
                    LargeFloatingActionButton(onClick = onRecord, containerColor = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("rec_start")) {
                        Icon(Icons.Filled.Mic, "Kayda başla", Modifier.size(36.dp))
                    }
                } else {
                    FilledTonalIconButton(onClick = if (status == RecStatus.PAUSED) onResume else onPause, modifier = Modifier.size(64.dp).testTag("rec_pause")) {
                        Icon(if (status == RecStatus.PAUSED) Icons.Filled.Mic else Icons.Filled.Pause, if (status == RecStatus.PAUSED) "Devam et" else "Duraklat")
                    }
                    FilledIconButton(onClick = onStop, modifier = Modifier.size(80.dp).testTag("rec_stop"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Filled.Stop, "Durdur ve kaydet", Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingRow(r: Recording, play: PlayState?, vm: RecorderViewModel, onPlay: () -> Unit) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPlay, modifier = Modifier.testTag("play_${r.id}")) {
                    Icon(if (play?.playing == true) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (play?.playing == true) "Duraklat" else "Oynat")
                }
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val dt = Instant.ofEpochMilli(r.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    Text("${RecFiles.duration(r.durationMs)} · ${RecFiles.size(r.sizeBytes)} · ${TrFormat.formatDateTime(dt)}", style = MaterialTheme.typography.bodySmall)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Diğer") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ Text("Yeniden adlandır") }, { menu = false; renaming = true })
                        DropdownMenuItem({ Text("Paylaş") }, { menu = false; vm.shareIntent(r)?.let { runCatching { ctx.startActivity(it) } } })
                        DropdownMenuItem({ Text("Müzik klasörüne aktar") }, { menu = false; vm.exportToMusic(r) })
                        DropdownMenuItem({ Text("Sil") }, { menu = false; deleting = true })
                    }
                }
            }
            if (play != null && play.durationMs > 0) {
                Slider(value = play.positionMs.toFloat(), onValueChange = { vm.seek(it.toInt()) }, valueRange = 0f..play.durationMs.toFloat(), modifier = Modifier.padding(horizontal = 8.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(RecFiles.duration(play.positionMs.toLong()), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { vm.skip(-10_000) }) { Icon(Icons.Outlined.Replay10, "10 saniye geri") }
                    IconButton(onClick = { vm.skip(10_000) }) { Icon(Icons.Outlined.Forward10, "10 saniye ileri") }
                    Spacer(Modifier.weight(1f))
                    Text(RecFiles.duration(play.durationMs.toLong()), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    if (renaming) {
        var t by remember { mutableStateOf(r.title) }
        AlertDialog(onDismissRequest = { renaming = false }, title = { Text("Yeniden adlandır") },
            text = { OutlinedTextField(t, { t = it.take(80) }, singleLine = true) },
            confirmButton = { TextButton(enabled = t.isNotBlank(), onClick = { vm.rename(r, t); renaming = false }) { Text("Kaydet") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Vazgeç") } })
    }
    if (deleting) ConfirmDialog("Kayıt silinsin mi?", "\"${r.title}\" kalıcı olarak silinir.", "Sil", onConfirm = { deleting = false; vm.delete(r) },
        onDismiss = { deleting = false }, destructive = true)
}
