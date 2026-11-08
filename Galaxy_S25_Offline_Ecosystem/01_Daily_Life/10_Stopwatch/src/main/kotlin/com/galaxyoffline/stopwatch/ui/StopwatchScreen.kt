package com.galaxyoffline.stopwatch.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.stopwatch.data.StopwatchLogic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchScreen(vm: StopwatchViewModel, onHistory: () -> Unit, onSettings: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    var saveDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    val running = s?.running == true
    var elapsed by remember { mutableLongStateOf(0L) }
    // Yalnızca çalışırken ve ekran görünürken kare başına güncellenir (pil dostu).
    LaunchedEffect(s) {
        elapsed = vm.elapsedNow()
        while (s?.running == true) { withFrameMillis { elapsed = vm.elapsedNow() } }
    }
    val activity = LocalContext.current as? Activity
    DisposableEffect(running, settings.keepScreenOn) {
        if (running && settings.keepScreenOn) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    fun tap(action: () -> Unit) { if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); action() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Kronometre") }, actions = {
                IconButton(onClick = onHistory) { Icon(Icons.Outlined.History, "Kayıtlı oturumlar") }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") }
            })
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { p ->
        val st = s
        if (st == null) { LoadingBox(Modifier.padding(p)); return@Scaffold }
        Column(Modifier.padding(p).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth(0.82f).aspectRatio(1f).padding(8.dp), contentAlignment = Alignment.Center) {
                val track = MaterialTheme.colorScheme.surfaceContainerHighest
                val arc = MaterialTheme.colorScheme.primary
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val d = size.minDimension - stroke
                    val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
                    drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
                    val sweep = (elapsed % 60_000) / 60_000f * 360f
                    drawArc(arc, -90f, sweep, false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Text(TrFormat.stopwatch(elapsed), fontSize = if (elapsed >= 3_600_000) 44.sp else 56.sp, fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.testTag("time").semantics { contentDescription = "Geçen süre ${TrFormat.stopwatch(elapsed)}" })
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                if (running) {
                    FilledTonalIconButton(onClick = { tap(vm::lap) }, modifier = Modifier.size(64.dp).testTag("lap")) { Icon(Icons.Outlined.Flag, "Tur") }
                } else {
                    FilledTonalIconButton(onClick = { tap(vm::reset) }, enabled = elapsed > 0, modifier = Modifier.size(64.dp).testTag("reset")) {
                        Icon(Icons.Outlined.RestartAlt, "Sıfırla")
                    }
                }
                LargeFloatingActionButton(onClick = { tap(vm::startPause) }, modifier = Modifier.testTag("start")) {
                    Icon(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (running) "Duraklat" else "Başlat", Modifier.size(40.dp))
                }
                FilledTonalIconButton(onClick = { saveDialog = true }, enabled = !running && elapsed > 0, modifier = Modifier.size(64.dp).testTag("save")) {
                    Icon(Icons.Outlined.Save, "Oturumu kaydet")
                }
            }
            LapList(st.laps, Modifier.weight(1f))
        }
    }
    if (saveDialog) {
        var label by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { saveDialog = false }, title = { Text("Oturumu kaydet") },
            text = { OutlinedTextField(label, { label = it.take(60) }, label = { Text("Etiket (isteğe bağlı)") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.saveSession(label); saveDialog = false }, modifier = Modifier.testTag("save_ok")) { Text("Kaydet") } },
            dismissButton = { TextButton(onClick = { saveDialog = false }) { Text("Vazgeç") } })
    }
}

@Composable
fun LapList(laps: List<Long>, modifier: Modifier = Modifier) {
    if (laps.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Text("Tur almak için çalışırken bayrak simgesine dokunun.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
        }
        return
    }
    val durations = StopwatchLogic.lapDurations(laps)
    val bw = StopwatchLogic.bestWorst(laps)
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)) {
        itemsIndexed(laps.indices.reversed().toList()) { _, i ->
            val color = when (i) {
                bw?.first -> MaterialTheme.colorScheme.primary
                bw?.second -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Tur ${i + 1}", style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.width(80.dp))
                Text(TrFormat.stopwatch(durations[i]), style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace,
                    color = color, modifier = Modifier.weight(1f))
                Text(TrFormat.stopwatch(laps[i]), style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(0.dp))
            }
            HorizontalDivider()
        }
    }
}
