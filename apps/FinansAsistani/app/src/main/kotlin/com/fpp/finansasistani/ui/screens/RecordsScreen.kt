package com.fpp.finansasistani.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fpp.finans.core.DebtType
import com.fpp.finans.core.FinanceCalculator
import com.fpp.finans.core.IncomeType
import com.fpp.finans.core.Money
import com.fpp.finans.core.SavingStrategy
import com.fpp.finansasistani.ui.components.EmptyState
import com.fpp.finansasistani.ui.components.IconBadge
import com.fpp.finansasistani.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

/** Liste satırı için arayüz modeli. */
private data class RecordItem(
    val key: Long,
    val title: String,
    val subtitle: String,
    val amount: String,
    val source: Any,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    vm: FinanceViewModel,
    snackbar: SnackbarHostState,
    onAdd: (RecordKind) -> Unit,
    onEdit: (RecordKind, Long) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val kind = RecordKind.entries[tab.coerceIn(0, RecordKind.entries.lastIndex)]
    val scope = rememberCoroutineScope()
    val cardRatio = state.cashFlow.cardMinimumRatio

    val records: List<RecordItem> = when (kind) {
        RecordKind.INCOME -> state.data.incomes.map {
            val detail = if (it.type == IncomeType.SALARY && it.raisesPerYear > 0) {
                "Maaş · yılda ${it.raisesPerYear} zam, her biri ${Money.formatPercent(it.raisePercent)}"
            } else {
                it.type.shortLabel
            }
            RecordItem(it.id, it.name, detail, Money.format(it.amount), it)
        }
        RecordKind.DEBT -> state.data.debts.map {
            val detail = when (it.type) {
                DebtType.INSTALLMENT -> "Taksit ${Money.format(it.minPayment)} × ${it.remainingInstallments}"
                DebtType.CREDIT_CARD -> "Kart · faiz ${Money.formatPercent(it.annualInterestRate)} · limit ${Money.format(it.cardLimit)}"
                else -> "${it.type.label} · faiz ${Money.formatPercent(it.annualInterestRate)} · asgari " +
                    Money.format(FinanceCalculator.minimumPayment(it, cardRatio))
            }
            RecordItem(it.id, it.name, detail, Money.format(it.balance), it)
        }
        RecordKind.EXPENSE -> state.data.expenses.map {
            RecordItem(it.id, it.name, "Aylık sabit gider", Money.format(it.amount), it)
        }
        RecordKind.SAVING -> state.data.savings.map {
            val detail = if (it.strategy == SavingStrategy.FIXED) "Sabit tutar · aylık"
            else "Net fazlanın ${Money.formatPercent(it.percent)} kadarı"
            val amount = if (it.strategy == SavingStrategy.FIXED) Money.format(it.monthlyAmount) else Money.formatPercent(it.percent)
            RecordItem(it.id, it.name, detail, amount, it)
        }
    }
    val total = when (kind) {
        RecordKind.INCOME -> state.cashFlow.monthlyIncome
        RecordKind.DEBT -> state.cashFlow.totalDebt
        RecordKind.EXPENSE -> state.cashFlow.fixedExpenses
        RecordKind.SAVING -> state.cashFlow.totalSavings
    }
    val totalLabel = when (kind) {
        RecordKind.INCOME -> "Aylık düzenli gelir"
        RecordKind.DEBT -> "Toplam borç bakiyesi"
        RecordKind.EXPENSE -> "Aylık sabit gider"
        RecordKind.SAVING -> "Aylık birikim payı"
    }

    fun delete(item: RecordItem) {
        vm.delete(item.source)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "\"${item.title}\" silindi",
                actionLabel = "Geri al",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) vm.restore(item.source)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kayıtlar") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAdd(kind) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(kind.addLabel) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                RecordKind.entries.forEachIndexed { index, k ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(k.tabLabel, maxLines = 1) },
                    )
                }
            }
            if (records.isEmpty()) {
                EmptyState(
                    icon = kind.icon,
                    title = "Henüz kayıt yok",
                    message = when (kind) {
                        RecordKind.INCOME -> "Maaşınızı ve diğer gelirlerinizi ekleyin."
                        RecordKind.DEBT -> "Kredi kartı, kredi, KMH veya taksitli borçlarınızı ekleyin."
                        RecordKind.EXPENSE -> "Kira, fatura ve abonelik gibi düzenli giderlerinizi ekleyin."
                        RecordKind.SAVING -> "Her ay kenara ayırmak istediğiniz birikim hedeflerini ekleyin."
                    },
                    actionLabel = kind.addLabel,
                    onAction = { onAdd(kind) },
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "total") {
                        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                totalLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(Money.format(total), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    items(records, key = { it.key }) { item ->
                        RecordCard(
                            item = item,
                            icon = kind.icon,
                            tint = kind.tint,
                            onClick = { onEdit(kind, item.key) },
                            onDelete = { delete(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCard(
    item: RecordItem,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(item.amount, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = tint)
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Seçenekler")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Düzenle") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menu = false; onClick() },
                    )
                    DropdownMenuItem(
                        text = { Text("Sil") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
    }
}
