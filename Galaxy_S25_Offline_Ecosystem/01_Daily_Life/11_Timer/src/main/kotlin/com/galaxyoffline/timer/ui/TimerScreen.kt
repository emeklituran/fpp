package com.galaxyoffline.timer.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.designsystem.components.InfoBanner
import com.galaxyoffline.core.designsystem.components.SectionHeader
import com.galaxyoffline.timer.data.Phase
import com.galaxyoffline.timer.data.PomodoroConfig
import com.galaxyoffline.timer.data.RunStatus
import com.galaxyoffline.timer.data.TimerEngine
import com.galaxyoffline.timer.data.TimerMode
import com.galaxyoffline.timer.data.TimerState
import com.galaxyoffline.timer.data.TimerStore
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(vm: TimerViewModel, onSettings: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(if (s.mode == TimerMode.POMODORO) 1 else 0) }
    var notifOk by remember { mutableStateOf(true) }
    var exactOk by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        notifOk = NotificationManagerCompat.from(ctx).areNotificationsEnabled(); exactOk = TimerStore.canExact(ctx); vm.checkOverdue(); onPauseOrDispose {}
    }
    val req = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    fun askNotif() { if (!notifOk && Build.VERSION.SDK_INT >= 33) req.launch(Manifest.permission.POST_NOTIFICATIONS) }
    val busy = s.status != RunStatus.IDLE
    Scaffold(topBar = { TopAppBar(title = { Text("Zamanlayıcı") }, actions = { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") } }) },
        snackbarHost = { SnackbarHost(snackbar) }) { p ->
        Column(Modifier.padding(p).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Geri sayım") }, modifier = Modifier.testTag("tab_timer"))
                Tab(tab == 1, { tab = 1 }, text = { Text("Pomodoro") }, modifier = Modifier.testTag("tab_pomodoro"))
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!notifOk) InfoBanner("Bildirim izni kapalı: süre dolduğunda kilit ekranında uyarı gösterilemez (ses yine çalar).", isError = true,
                    actionLabel = "İzin ver", onAction = ::askNotif)
                if (!exactOk) InfoBanner("Tam zamanlı alarm izni kapalı; uyarı birkaç dakika gecikebilir. Ayarlar › Uygulamalar › Zamanlayıcı › Alarmlar ve hatırlatıcılar.")
                val tabMode = if (tab == 0) TimerMode.TIMER else TimerMode.POMODORO
                if (busy && s.mode != tabMode) {
                    InfoBanner("Şu anda ${if (s.mode == TimerMode.POMODORO) "Pomodoro" else "geri sayım"} çalışıyor. Aynı anda tek zamanlayıcı çalışır.",
                        actionLabel = "Ona git", onAction = { tab = if (s.mode == TimerMode.POMODORO) 1 else 0 })
                } else if (tab == 0) {
                    if (busy) Running(s, vm, pomodoro = false) else TimerSetup(vm) { askNotif() }
                } else {
                    PomodoroTab(s, vm, busy) { askNotif() }
                }
            }
        }
    }
}

@Composable
private fun Running(s: TimerState, vm: TimerViewModel, pomodoro: Boolean) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.status, s.endAt) { while (s.status == RunStatus.RUNNING) { now = System.currentTimeMillis(); delay(200) }; now = System.currentTimeMillis() }
    val track = MaterialTheme.colorScheme.surfaceVariant
    val prog = MaterialTheme.colorScheme.primary
    val progress = s.progress(now)
    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            drawArc(track, 0f, 360f, false, style = Stroke(16.dp.toPx(), cap = StrokeCap.Round))
            drawArc(prog, -90f, 360f * (1f - progress), false, style = Stroke(16.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (pomodoro || s.label.isNotBlank()) Text(s.label.ifBlank { s.phase.label }, style = MaterialTheme.typography.titleMedium)
            Text(if (s.status == RunStatus.FINISHED) "00:00" else TimerEngine.format(s.remaining(now)), fontSize = 56.sp, modifier = Modifier.testTag("remaining"))
            Text(when (s.status) { RunStatus.PAUSED -> "Duraklatıldı"; RunStatus.FINISHED -> "Süre doldu"; else -> "" }, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag("run_status"))
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        FilledTonalIconButton(onClick = vm::reset, modifier = Modifier.size(56.dp).testTag("reset")) { Icon(Icons.Outlined.Refresh, "Sıfırla") }
        if (s.status != RunStatus.FINISHED) FilledIconButton(onClick = if (s.status == RunStatus.RUNNING) vm::pause else vm::resume, modifier = Modifier.size(80.dp).testTag("pause_resume")) {
            Icon(if (s.status == RunStatus.RUNNING) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (s.status == RunStatus.RUNNING) "Duraklat" else "Devam et", Modifier.size(40.dp))
        }
        if (pomodoro) FilledTonalIconButton(onClick = vm::skipPhase, modifier = Modifier.size(56.dp)) { Icon(Icons.Outlined.SkipNext, "Sonraki aşamaya geç") }
        else FilledTonalButton(onClick = vm::plusMinute, modifier = Modifier.height(56.dp).testTag("plus_minute")) { Text("+1 dk") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerSetup(vm: TimerViewModel, beforeStart: () -> Unit) {
    var h by rememberSaveable { mutableIntStateOf(0) }
    var m by rememberSaveable { mutableIntStateOf(5) }
    var sec by rememberSaveable { mutableIntStateOf(0) }
    var label by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val presets by vm.presets.collectAsStateWithLifecycle()
    val total = (h * 3600L + m * 60L + sec) * 1000
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stepper("saat", h, 0..99) { h = it }; Text(":", fontSize = 40.sp); Stepper("dakika", m, 0..59) { m = it }; Text(":", fontSize = 40.sp); Stepper("saniye", sec, 0..59) { sec = it }
    }
    OutlinedTextField(label, { label = it.take(40) }, label = { Text("Etiket (isteğe bağlı)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(enabled = total > 0, onClick = { beforeStart(); vm.startTimer(total, label) }, modifier = Modifier.fillMaxWidth().height(56.dp).testTag("start")) {
        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Başlat", style = MaterialTheme.typography.titleMedium)
    }
    SectionHeader("Hızlı başlat")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(1, 3, 5, 10, 15, 30, 60).forEach { min ->
            AssistChip(onClick = { beforeStart(); vm.startTimer(min * 60_000L, label) }, label = { Text(if (min == 60) "1 sa" else "$min dk") }, modifier = Modifier.testTag("quick_$min"))
        }
    }
    SectionHeader("Kayıtlı süreler", action = { TextButton(enabled = total > 0, onClick = { saving = true }) { Text("Bu süreyi kaydet") } })
    if (presets.isEmpty()) Text("Sık kullandığınız süreleri (ör. Yumurta 7 dk, Çay 4 dk) kaydedin.", style = MaterialTheme.typography.bodySmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        presets.forEach { pr ->
            InputChip(selected = false, onClick = { beforeStart(); vm.startTimer(pr.seconds * 1000, pr.label) }, label = { Text("${pr.label} · ${TimerEngine.format(pr.seconds * 1000)}") },
                trailingIcon = { IconButton(onClick = { vm.deletePreset(pr) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.Close, "Sil", Modifier.size(16.dp)) } })
        }
    }
    if (saving) {
        var name by remember { mutableStateOf(label) }
        AlertDialog(onDismissRequest = { saving = false }, title = { Text("Süreyi kaydet") },
            text = { OutlinedTextField(name, { name = it.take(30) }, label = { Text("Ad (ör. Çay demleme)") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.savePreset(name, total / 1000); saving = false }) { Text("Kaydet") } },
            dismissButton = { TextButton(onClick = { saving = false }) { Text("Vazgeç") } })
    }
}

@Composable
private fun Stepper(unit: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { onChange(if (value >= range.last) range.first else value + 1) }) { Icon(Icons.Outlined.Add, "$unit artır") }
        Text("%02d".format(value), fontSize = 44.sp, modifier = Modifier.testTag("step_$unit"))
        Text(unit, style = MaterialTheme.typography.labelSmall)
        IconButton(onClick = { onChange(if (value <= range.first) range.last else value - 1) }) { Icon(Icons.Outlined.Remove, "$unit azalt") }
    }
}

@Composable
private fun PomodoroTab(s: TimerState, vm: TimerViewModel, busy: Boolean, beforeStart: () -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    if (busy) Running(s, vm, pomodoro = true)
    else {
        Icon(Icons.Outlined.Timer, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("${cfg.workMin} dk çalışma · ${cfg.shortMin} dk mola", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { beforeStart(); vm.startPomodoro() }, modifier = Modifier.fillMaxWidth().height(56.dp).testTag("pomo_start")) { Text("Çalışmaya başla") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        val filled = if (s.cycleWorkDone > 0 && s.cycleWorkDone % cfg.longEvery == 0) cfg.longEvery else s.cycleWorkDone % cfg.longEvery
        repeat(cfg.longEvery) { i ->
            val c = MaterialTheme.colorScheme
            Box(Modifier.size(14.dp).background(if (i < filled) c.primary else c.surfaceVariant, CircleShape))
        }
        Spacer(Modifier.width(8.dp))
        Text("Bugün $today oturum", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("pomo_today"))
    }
    if (!busy) {
        SectionHeader("Süreler (dakika)")
        ConfigRow("Çalışma", cfg.workMin, 5..90) { vm.setConfig(cfg.copy(workMin = it)) }
        ConfigRow("Kısa mola", cfg.shortMin, 1..30) { vm.setConfig(cfg.copy(shortMin = it)) }
        ConfigRow("Uzun mola", cfg.longMin, 5..60) { vm.setConfig(cfg.copy(longMin = it)) }
        ConfigRow("Uzun mola her", cfg.longEvery, 2..8, suffix = "oturumda") { vm.setConfig(cfg.copy(longEvery = it)) }
    }
}

@Composable
private fun ConfigRow(label: String, value: Int, range: IntRange, suffix: String = "dk", onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        IconButton(enabled = value > range.first, onClick = { onChange(value - 1) }) { Icon(Icons.Outlined.Remove, "$label azalt") }
        Text("$value $suffix", style = MaterialTheme.typography.titleMedium)
        IconButton(enabled = value < range.last, onClick = { onChange(value + 1) }) { Icon(Icons.Outlined.Add, "$label artır") }
    }
}
