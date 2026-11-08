package com.galaxyoffline.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.calendar.data.CalendarLogic
import com.galaxyoffline.calendar.data.Event
import com.galaxyoffline.calendar.data.EventRepeat
import com.galaxyoffline.calendar.data.Occurrence
import com.galaxyoffline.calendar.data.TurkishHolidays
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.EmptyState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private const val PAGES = 2400
private const val CENTER = PAGES / 2
private val WEEKDAYS = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: CalendarViewModel, onSettings: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val baseMonth = remember { YearMonth.from(today) }
    var selected by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var agenda by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Event?>(null) }
    val pager = rememberPagerState(initialPage = CENTER) { PAGES }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    val selDate = LocalDate.ofEpochDay(selected)
    val month = baseMonth.plusMonths((pager.currentPage - CENTER).toLong())

    fun goTo(d: LocalDate) {
        selected = d.toEpochDay()
        scope.launch { pager.animateScrollToPage(CENTER + ChronoUnit.MONTHS.between(baseMonth, YearMonth.from(d)).toInt()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (agenda) "Ajanda" else TrFormat.formatMonthYear(month.atDay(1))) },
                actions = {
                    IconButton(onClick = { agenda = false; goTo(today) }) { Icon(Icons.Outlined.Today, "Bugüne git") }
                    IconButton(onClick = { agenda = !agenda }) {
                        Icon(if (agenda) Icons.Outlined.CalendarMonth else Icons.Outlined.ViewAgenda, if (agenda) "Ay görünümü" else "Ajanda görünümü")
                    }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { editing = Event(title = "", date = selected, createdAt = 0) },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Etkinlik") }, modifier = Modifier.testTag("fab_new"))
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { p ->
        if (agenda) {
            AgendaList(s.events, s.showHolidays, today, Modifier.padding(p)) { editing = it }
            return@Scaffold
        }
        Column(Modifier.padding(p).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Önceki ay") }
                Row(Modifier.weight(1f)) {
                    WEEKDAYS.forEachIndexed { i, d ->
                        Text(d, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
                            color = if (i >= 5) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Sonraki ay") }
            }
            HorizontalPager(pager, Modifier.fillMaxWidth(), beyondViewportPageCount = 0) { page ->
                val m = baseMonth.plusMonths((page - CENTER).toLong())
                val grid = remember(m) { CalendarLogic.monthGrid(m) }
                val occ = remember(m, s.events) { CalendarLogic.occurrences(s.events, grid.first(), grid.last()).groupBy { it.date } }
                val holidays = remember(m, s.showHolidays) { if (s.showHolidays) TurkishHolidays.between(grid.first(), grid.last()) else emptyMap() }
                Column(Modifier.padding(horizontal = 8.dp)) {
                    grid.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { d ->
                                DayCell(d, inMonth = d.month == m.month, today = d == today, selected = d == selDate,
                                    holiday = holidays[d] != null, dots = occ[d].orEmpty().map { eventColor(it.event.color) }.take(3),
                                    modifier = Modifier.weight(1f)) { selected = d.toEpochDay() }
                            }
                        }
                    }
                }
            }
            DayDetails(selDate, s.events, s.showHolidays, Modifier.weight(1f)) { editing = it }
        }
    }
    editing?.let { e ->
        EventEditorSheet(e, onDismiss = { editing = null }, onSave = { vm.save(it); editing = null },
            onDelete = if (e.id != 0L) ({
                editing = null; vm.delete(e)
                scope.launch { if (snackbar.showSnackbar("Etkinlik silindi", "Geri al", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) vm.restore(e) }
            }) else null)
    }
}

@Composable
private fun DayCell(d: LocalDate, inMonth: Boolean, today: Boolean, selected: Boolean, holiday: Boolean, dots: List<Color>,
                    modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val textColor = when {
        selected -> c.onPrimary
        !inMonth -> c.onSurfaceVariant.copy(alpha = 0.45f)
        holiday || d.dayOfWeek.value >= 6 -> c.error
        else -> c.onSurface
    }
    Column(modifier.aspectRatio(0.9f).padding(2.dp).clickable(onClick = onClick)
        .semantics { contentDescription = TrFormat.formatDateWithDay(d) + (if (holiday) ", resmi tatil" else "") + (if (dots.isNotEmpty()) ", ${dots.size} etkinlik" else "") },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(34.dp).background(if (selected) c.primary else Color.Transparent, CircleShape)
            .then(if (today && !selected) Modifier.border(1.5.dp, c.primary, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
            Text("${d.dayOfMonth}", color = textColor, style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Normal)
        }
        Row(Modifier.height(8.dp).padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            dots.forEach { Box(Modifier.size(5.dp).background(it, CircleShape)) }
        }
    }
}

@Composable
private fun DayDetails(day: LocalDate, events: List<Event>, showHolidays: Boolean, modifier: Modifier, onOpen: (Event) -> Unit) {
    val occ = remember(day, events) { CalendarLogic.occurrences(events, day, day) }
    val holiday = if (showHolidays) TurkishHolidays.forYear(day.year)[day] else null
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(TrFormat.capitalize(TrFormat.formatDateWithDay(day)), style = MaterialTheme.typography.titleMedium) }
        if (holiday != null) item { HolidayRow(holiday) }
        if (occ.isEmpty() && holiday == null) item {
            Text("Bu gün için etkinlik yok.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(occ, key = { "${it.event.id}-${it.date}" }) { EventRow(it) { onOpen(it.event) } }
    }
}

@Composable
private fun AgendaList(events: List<Event>, showHolidays: Boolean, today: LocalDate, modifier: Modifier, onOpen: (Event) -> Unit) {
    val end = today.plusDays(60)
    val occ = remember(events, today) { CalendarLogic.occurrences(events, today, end).groupBy { it.date } }
    val holidays = if (showHolidays) TurkishHolidays.between(today, end) else emptyMap()
    val days = (occ.keys + holidays.keys).toSortedSet()
    if (days.isEmpty()) {
        Box(modifier) { EmptyState(Icons.Outlined.EventNote, "Önümüzdeki 60 gün boş", "Etkinlik eklemek için \"Etkinlik\" düğmesine dokunun.") }
        return
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { d ->
            item(key = "d$d") {
                Text(TrFormat.relativeDay(d, today) + " · " + TrFormat.capitalize(d.format(TrFormat.dayName)),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            }
            holidays[d]?.let { h -> item(key = "h$d") { HolidayRow(h) } }
            items(occ[d].orEmpty(), key = { "${it.event.id}-${it.date}" }) { EventRow(it) { onOpen(it.event) } }
        }
    }
}

@Composable
private fun HolidayRow(name: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Celebration, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(name, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EventRow(o: Occurrence, onClick: () -> Unit) {
    val e = o.event
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 4.dp, height = 36.dp).background(eventColor(e.color), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(e.title, style = MaterialTheme.typography.bodyLarge)
                val time = if (e.allDay) "Tüm gün" else listOfNotNull(e.startTime?.let(TrFormat::formatTime), e.endTime?.let(TrFormat::formatTime)).joinToString(" – ")
                Text(time + if (e.note.isNotBlank()) " · ${e.note}" else "", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (EventRepeat.of(e.repeat) != EventRepeat.NONE) Icon(Icons.Outlined.Repeat, EventRepeat.of(e.repeat).label, Modifier.size(18.dp))
        }
    }
}
