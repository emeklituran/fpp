package com.galaxyoffline.habittracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.ConfirmDialog
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.InfoBanner
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.core.designsystem.components.SectionHeader
import com.galaxyoffline.habittracker.data.Habit
import com.galaxyoffline.habittracker.data.HabitLogic
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private val EMOJIS = listOf("✅", "💧", "🏃", "📚", "🧘", "🥗", "💊", "😴", "🦷", "🚭", "💪", "🎸", "✍️", "🌱", "🧹", "🙏", "📵", "🚶")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(vm: HabitViewModel, onOpen: (Long) -> Unit, onSettings: () -> Unit) {
    val habits by vm.habits.collectAsStateWithLifecycle()
    val checks by vm.checks.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<Habit?>(null) }
    var notifOk by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) { notifOk = NotificationManagerCompat.from(ctx).areNotificationsEnabled(); onPauseOrDispose {} }
    val req = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    val today = LocalDate.now()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Alışkanlıklar") }, actions = { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { editing = Habit(name = "", createdDay = today.toEpochDay()) }, icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Alışkanlık ekle") }, modifier = Modifier.testTag("fab_new")) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { p ->
        val all = habits ?: run { LoadingBox(Modifier.padding(p)); return@Scaffold }
        val active = all.filter { !it.archived }
        val archived = all.filter { it.archived }
        LazyColumn(Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!notifOk && active.any { it.remindMinute != null }) item {
                InfoBanner("Bildirim izni kapalı; hatırlatmalar gösterilemez.", isError = true, icon = Icons.Outlined.NotificationsOff, actionLabel = "İzin ver",
                    onAction = { if (Build.VERSION.SDK_INT >= 33) req.launch(Manifest.permission.POST_NOTIFICATIONS) })
            }
            val planned = active.filter { it.scheduledOn(today) }
            if (active.isNotEmpty()) item {
                val done = planned.count { today.toEpochDay() in checks[it.id].orEmpty() }
                Text("${TrFormat.formatDateWithDay(today)} · bugün $done / ${planned.size} tamamlandı", style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp).testTag("today_summary"))
            }
            if (active.isEmpty()) item { EmptyState(Icons.Outlined.TaskAlt, "Henüz alışkanlık yok", "Su içmek, yürüyüş, kitap okumak… Her gün tek dokunuşla işaretleyin, serinizi izleyin.") }
            items(active, key = { it.id }) { h -> HabitRow(h, checks[h.id].orEmpty(), today, onToggle = { d -> vm.toggle(h, d) }, onOpen = { onOpen(h.id) }) }
            if (archived.isNotEmpty()) {
                item { SectionHeader("Arşiv (${archived.size})") }
                items(archived, key = { it.id }) { h ->
                    Card(onClick = { onOpen(h.id) }) { Text("${h.emoji}  ${h.name}", Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyLarge) }
                }
            }
        }
    }
    editing?.let { h -> HabitEditor(h, onDismiss = { editing = null }, onSave = { vm.save(it) { editing = null } }) }
}

@Composable
private fun HabitRow(h: Habit, checked: Set<Long>, today: LocalDate, onToggle: (LocalDate) -> Unit, onOpen: () -> Unit) {
    val stats = remember(h, checked, today) { HabitLogic.stats(h, checked, today) }
    Card(onClick = onOpen, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(h.emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(h.name, style = MaterialTheme.typography.titleMedium)
                    Text("🔥 ${stats.current} gün seri · ${HabitLogic.daysLabel(h.daysMask)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("streak_${h.id}"))
                }
                DayDot(today, checked, h, big = true, tag = "check_${h.id}", onToggle = onToggle)
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                (6 downTo 1).forEach { back ->
                    val d = today.minusDays(back.toLong())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(HabitLogic.SHORT[d.dayOfWeek.value - 1], style = MaterialTheme.typography.labelSmall)
                        DayDot(d, checked, h, big = false, onToggle = onToggle)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDot(d: LocalDate, checked: Set<Long>, h: Habit, big: Boolean, tag: String? = null, onToggle: (LocalDate) -> Unit) {
    val done = d.toEpochDay() in checked
    val planned = h.scheduledOn(d)
    val size = if (big) 44.dp else 30.dp
    val c = MaterialTheme.colorScheme
    Box(Modifier.size(size).clip(CircleShape)
        .background(if (done) c.primary else c.surface)
        .border(2.dp, if (done) c.primary else if (planned) c.outline else c.outlineVariant, CircleShape)
        .clickable { onToggle(d) }
        .semantics { contentDescription = "${TrFormat.formatDate(d)} ${if (done) "yapıldı" else "yapılmadı"}" }
        .then(if (tag != null) Modifier.testTag(tag) else Modifier), contentAlignment = Alignment.Center) {
        if (done) Icon(Icons.Filled.Check, null, tint = c.onPrimary, modifier = Modifier.size(size * 0.6f))
        else if (!big) Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = if (planned) c.onSurface else c.outline)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: HabitViewModel, id: Long, onBack: () -> Unit) {
    val habits by vm.habits.collectAsStateWithLifecycle()
    val checks by vm.checks.collectAsStateWithLifecycle()
    val h = habits?.firstOrNull { it.id == id }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    Scaffold(topBar = { TopAppBar(title = { Text(h?.let { "${it.emoji} ${it.name}" } ?: "") }, navigationIcon = { BackButton(onBack) }, actions = {
        if (h != null) {
            IconButton(onClick = { editing = true }) { Icon(Icons.Outlined.Edit, "Düzenle") }
            IconButton(onClick = { vm.archive(h, !h.archived) }) { Icon(if (h.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, if (h.archived) "Arşivden çıkar" else "Arşivle") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Sil") }
        }
    }) }) { p ->
        if (h == null) { LoadingBox(Modifier.padding(p)); return@Scaffold }
        val set = checks[h.id].orEmpty()
        val s = remember(h, set) { HabitLogic.stats(h, set, today) }
        Column(Modifier.padding(p).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Güncel seri", "${s.current}", Modifier.weight(1f)); Stat("En iyi seri", "${s.best}", Modifier.weight(1f))
                Stat("Son 30 gün", "%${s.rate30}", Modifier.weight(1f)); Stat("Toplam", "${s.total}", Modifier.weight(1f))
            }
            Text("Son 20 hafta (dokunarak geçmiş günleri düzeltebilirsiniz)", style = MaterialTheme.typography.labelLarge)
            Heatmap(h, set, today) { d -> vm.toggle(h, d) }
            Text(buildString {
                append(HabitLogic.daysLabel(h.daysMask))
                h.remindMinute?.let { append(" · hatırlatma %02d:%02d".format(it / 60, it % 60)) }
                if (h.archived) append(" · arşivde")
            }, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (editing && h != null) HabitEditor(h, onDismiss = { editing = false }, onSave = { vm.save(it) { editing = false } })
    if (confirmDelete && h != null) ConfirmDialog(title = "Alışkanlık silinsin mi?", text = "\"${h.name}\" ve tüm geçmişi kalıcı olarak silinir. Yalnızca gizlemek için arşivleyebilirsiniz.",
        confirmLabel = "Sil", destructive = true, onConfirm = { confirmDelete = false; vm.delete(h); onBack() }, onDismiss = { confirmDelete = false })
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun Heatmap(h: Habit, set: Set<Long>, today: LocalDate, onToggle: (LocalDate) -> Unit) {
    val weeks = 20
    val firstMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks((weeks - 1).toLong())
    val c = MaterialTheme.colorScheme
    Row(Modifier.horizontalScroll(rememberScrollState(Int.MAX_VALUE)), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { HabitLogic.SHORT.forEach { Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { Text(it.take(1), style = MaterialTheme.typography.labelSmall) } } }
        repeat(weeks) { w ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(7) { dIdx ->
                    val d = firstMonday.plusDays((w * 7 + dIdx).toLong())
                    val future = d.isAfter(today)
                    val done = d.toEpochDay() in set
                    Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(when {
                        future -> c.surface
                        done -> c.primary
                        h.scheduledOn(d) -> c.surfaceVariant
                        else -> c.surfaceContainerLowest
                    }).then(if (future) Modifier else Modifier.clickable { onToggle(d) }))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditor(h: Habit, onDismiss: () -> Unit, onSave: (Habit) -> Unit) {
    var name by rememberSaveable { mutableStateOf(h.name) }
    var emoji by rememberSaveable { mutableStateOf(h.emoji) }
    var mask by rememberSaveable { mutableIntStateOf(h.daysMask) }
    var remind by rememberSaveable { mutableStateOf(h.remindMinute != null) }
    var minute by rememberSaveable { mutableIntStateOf(h.remindMinute ?: (20 * 60)) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val ctx = LocalContext.current
    val req = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (h.id == 0L) "Yeni alışkanlık" else "Düzenle", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it.take(60) }, label = { Text("Ad (ör. 2 litre su)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("habit_name"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                EMOJIS.forEach { e -> FilterChip(emoji == e, { emoji = e }, label = { Text(e) }) }
            }
            Text("Hangi günler?", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HabitLogic.SHORT.forEachIndexed { i, s ->
                    val on = mask and (1 shl i) != 0
                    FilterChip(on, { val n = mask xor (1 shl i); if (n != 0) mask = n }, label = { Text(s) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { mask = 127 }) { Text("Her gün") }; TextButton(onClick = { mask = 31 }) { Text("Hafta içi") }; TextButton(onClick = { mask = 96 }) { Text("Hafta sonu") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Günlük hatırlatma", style = MaterialTheme.typography.bodyLarge)
                    if (remind) TextButton(onClick = { pickTime = true }) { Text("Saat: %02d:%02d".format(minute / 60, minute % 60)) }
                }
                Switch(remind, { remind = it
                    if (it && Build.VERSION.SDK_INT >= 33 && !NotificationManagerCompat.from(ctx).areNotificationsEnabled()) req.launch(Manifest.permission.POST_NOTIFICATIONS) })
            }
            Button(enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth().testTag("habit_save"), onClick = {
                onSave(h.copy(name = name, emoji = emoji, daysMask = mask, remindMinute = if (remind) minute else null))
            }) { Text("Kaydet") }
        }
    }
    if (pickTime) {
        val st = rememberTimePickerState(initialHour = minute / 60, initialMinute = minute % 60, is24Hour = true)
        AlertDialog(onDismissRequest = { pickTime = false }, title = { Text("Saat seçin") }, text = { TimePicker(st) },
            confirmButton = { TextButton(onClick = { minute = st.hour * 60 + st.minute; pickTime = false }) { Text("Tamam") } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Vazgeç") } })
    }
}
