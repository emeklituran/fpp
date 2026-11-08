package com.galaxyoffline.incomeexpense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.LoadingBox
import com.galaxyoffline.core.designsystem.components.SectionHeader
import com.galaxyoffline.incomeexpense.data.CategoryTotal
import com.galaxyoffline.incomeexpense.data.Tx
import com.galaxyoffline.incomeexpense.data.TxType
import kotlinx.coroutines.launch

val IncomeColor = Color(0xFF2E7D32)
val ExpenseColor = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MoneyViewModel, onBudgets: () -> Unit, onSettings: () -> Unit) {
    val s by vm.summary.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Tx?>(null) }
    LaunchedEffect(vm) { vm.messages.flow.collect { snackbar.showSnackbar(it) } }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Gelir Gider") }, actions = {
            IconButton(onClick = onBudgets) { Icon(Icons.Outlined.Savings, "Bütçeler") }
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Ayarlar") }
        }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { editing = Tx(type = TxType.EXPENSE.name, amount = 0.0, category = "MARKET",
            date = java.time.LocalDate.now().toEpochDay(), createdAt = 0) }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Kayıt ekle") },
            modifier = Modifier.testTag("fab_new")) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { p ->
        val sum = s ?: run { LoadingBox(Modifier.padding(p)); return@Scaffold }
        LazyColumn(Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.shiftMonth(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Önceki ay") }
                    Text(TrFormat.formatMonthYear(sum.month.atDay(1)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { vm.shiftMonth(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Sonraki ay") }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Net", style = MaterialTheme.typography.labelLarge)
                        Text(TrFormat.money(sum.net), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("net"))
                        Row { Text("Gelir  ", style = MaterialTheme.typography.bodyMedium); Text(TrFormat.money(sum.income), color = IncomeColor, style = MaterialTheme.typography.bodyMedium) }
                        Row { Text("Gider  ", style = MaterialTheme.typography.bodyMedium); Text(TrFormat.money(sum.expense), color = ExpenseColor, style = MaterialTheme.typography.bodyMedium) }
                        sum.expenseChange?.let { c ->
                            Text("Giderler geçen aya göre ${if (c >= 0) "%" + TrFormat.number(c, 1) + " arttı" else "%" + TrFormat.number(-c, 1) + " azaldı"}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (sum.categories.isNotEmpty()) {
                item { SectionHeader("Harcama dağılımı") }
                items(sum.categories, key = { "c" + it.category.name }) { CategoryBar(it) }
            }
            if (sum.transactions.isEmpty()) item {
                EmptyState(Icons.Outlined.AccountBalanceWallet, "Bu ay kayıt yok", "Gelir ve giderlerinizi ekledikçe aylık özet ve grafikler burada oluşur.")
            } else {
                sum.transactions.groupBy { it.localDate }.forEach { (d, list) ->
                    item(key = "d$d") { SectionHeader(TrFormat.capitalize(TrFormat.relativeDay(d)) + " · " + d.format(TrFormat.dayName)) }
                    items(list, key = { it.id }) { t -> TxRow(t) { editing = t } }
                }
            }
        }
    }
    editing?.let { t ->
        TxEditorSheet(t, onDismiss = { editing = null }, onSave = { vm.save(it) { editing = null } }, onDelete = if (t.id != 0L) ({
            editing = null; vm.delete(t)
            scope.launch { if (snackbar.showSnackbar("Kayıt silindi", "Geri al", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) vm.restore(t) }
        }) else null)
    }
}

@Composable
private fun CategoryBar(c: CategoryTotal) {
    val over = c.limit != null && c.total > c.limit
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${c.category.emoji}  ${c.category.label}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(TrFormat.money(c.total) + (c.limit?.let { " / " + TrFormat.money(it) } ?: ""), style = MaterialTheme.typography.bodyMedium,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
        val frac = (if (c.limit != null && c.limit > 0) c.total / c.limit else c.share).toFloat().coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().height(8.dp).padding(top = 2.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary))
        }
        if (over) Text("Bütçe ${TrFormat.money(c.total - c.limit!!)} aşıldı", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun TxRow(t: Tx, onClick: () -> Unit) {
    val income = t.txType == TxType.INCOME
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t.cat.emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.cat.label, style = MaterialTheme.typography.bodyLarge)
                if (t.note.isNotBlank()) Text(t.note, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text((if (income) "+" else "−") + TrFormat.money(t.amount), color = if (income) IncomeColor else ExpenseColor, style = MaterialTheme.typography.titleSmall)
        }
    }
}
