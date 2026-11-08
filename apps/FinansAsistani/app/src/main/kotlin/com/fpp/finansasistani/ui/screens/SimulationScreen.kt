package com.fpp.finansasistani.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fpp.finans.core.CashFlow
import com.fpp.finans.core.Dates
import com.fpp.finans.core.Money
import com.fpp.finans.core.SimulationResult
import com.fpp.finans.core.Strategy
import com.fpp.finansasistani.ui.components.DebtLineChart
import com.fpp.finansasistani.ui.components.EmptyState
import com.fpp.finansasistani.ui.components.InfoCard
import com.fpp.finansasistani.ui.components.SectionTitle
import com.fpp.finansasistani.ui.components.StatCard
import com.fpp.finansasistani.ui.theme.LocalFinanceColors
import com.fpp.finansasistani.viewmodel.FinanceViewModel
import com.fpp.finansasistani.viewmodel.SimulationUiState
import com.fpp.finansasistani.viewmodel.SimulationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(
    financeVm: FinanceViewModel,
    simVm: SimulationViewModel,
    onAdd: (RecordKind) -> Unit,
    onOpenTable: () -> Unit,
) {
    val finance by financeVm.state.collectAsStateWithLifecycle()
    val strategy by simVm.strategy.collectAsStateWithLifecycle()
    val state by simVm.state.collectAsStateWithLifecycle()
    val stale by simVm.isStale.collectAsStateWithLifecycle()
    val cash = finance.cashFlow

    Scaffold(topBar = { TopAppBar(title = { Text("Strateji ve simülasyon") }) }) { padding ->
        val missingIncome = finance.data.incomes.isEmpty()
        val missingDebt = finance.data.debts.none { it.balance > 0 }
        if (!finance.loading && (missingIncome || missingDebt)) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.TrendingDown,
                    title = "Plan için veri gerekli",
                    message = "Simülasyonu çalıştırmak için en az bir gelir ve bakiyesi olan bir borç ekleyin.",
                    actionLabel = if (missingIncome) "Gelir ekle" else "Borç ekle",
                    onAction = { onAdd(if (missingIncome) RecordKind.INCOME else RecordKind.DEBT) },
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CashFlowCard(cash) }
            item { SectionTitle("Ödeme stratejiniz") }
            item { StrategySelector(selected = strategy, onSelect = simVm::selectStrategy) }
            item {
                Button(
                    onClick = simVm::run,
                    enabled = state !is SimulationUiState.Running,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (state is SimulationUiState.Running) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Simülasyonu çalıştır")
                }
            }
            if (stale) {
                item {
                    InfoCard(
                        "Verileriniz simülasyondan sonra değişti. Güncel sonuç için yeniden çalıştırın.",
                        icon = Icons.Outlined.WarningAmber,
                        action = { TextButton(onClick = simVm::run) { Text("Yeniden çalıştır") } },
                    )
                }
            }
            when (val s = state) {
                is SimulationUiState.Error -> item {
                    InfoCard(s.message, isError = true, icon = Icons.Outlined.WarningAmber)
                }
                is SimulationUiState.Done -> {
                    item { ResultHero(s.primary) }
                    item { ResultStats(s.primary) }
                    s.alternative?.let { alt -> item { ComparisonCard(s.primary, alt) } }
                    if (s.primary.shortfallMonths > 0) {
                        item {
                            InfoCard(
                                "${s.primary.shortfallMonths} ay boyunca asgari ödemelerin tamamı karşılanamıyor. " +
                                    "Bu aylarda gecikme faizi oluşabilir.",
                                isError = true,
                                icon = Icons.Outlined.WarningAmber,
                            )
                        }
                    }
                    item {
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Toplam borcun zamanla azalması", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(12.dp))
                                DebtLineChart(values = s.primary.balanceSeries, startDate = s.primary.startDate)
                            }
                        }
                    }
                    item { SectionTitle("Borçların kapanma sırası") }
                    itemsIndexed(s.primary.payoffs, key = { _, p -> "payoff-${p.debtId}" }) { index, payoff ->
                        PayoffRow(index + 1, payoff.name, payoff.month, Dates.formatMonthYear(payoff.date))
                    }
                    item {
                        OutlinedButton(onClick = onOpenTable, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                            Icon(Icons.Outlined.TableChart, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ay ay detaylı ödeme tablosu")
                        }
                    }
                }
                SimulationUiState.Idle, SimulationUiState.Running -> Unit
            }
        }
    }
}

@Composable
private fun CashFlowCard(cash: CashFlow) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Nakit akışı analizi", style = MaterialTheme.typography.titleSmall)
            Line("Aylık gelir", Money.format(cash.monthlyIncome))
            Line("Zorunlu giderler (asgari + sabit)", "− " + Money.format(cash.mandatoryOutflow))
            Line("Net kullanılabilir fazla", Money.format(cash.netSurplus), bold = true)
            Line("Birikime aktarılacak", "− " + Money.format(cash.totalSavings))
            Line(
                "Borca ekstra güç",
                Money.format(cash.extraPower),
                bold = true,
                color = if (cash.hasDeficit) LocalFinanceColors.current.debt else LocalFinanceColors.current.income,
            )
            Text(
                "Her ay borçlara toplam ${Money.format(cash.debtBudget.coerceAtLeast(0.0))} ayrılır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Line(
    label: String,
    value: String,
    bold: Boolean = false,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color,
        )
    }
}

@Composable
private fun StrategySelector(selected: Strategy, onSelect: (Strategy) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Strategy.entries.forEach { option ->
            val isSelected = option == selected
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = isSelected, onClick = { onSelect(option) }, role = Role.RadioButton),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isSelected, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(option.label, style = MaterialTheme.typography.titleSmall)
                        Text(option.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHero(result: SimulationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.EventAvailable, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Borçsuz tarih (${result.strategy.label})", style = MaterialTheme.typography.labelLarge)
            }
            Text(
                Dates.formatMonthYear(result.payoffDate),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Bu plana sadık kalırsanız tüm borçlarınız ${Dates.formatDuration(result.months)} sonra bitecek.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ResultStats(result: SimulationResult) {
    val colors = LocalFinanceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Toplam faiz",
                value = Money.format(result.totalInterest),
                icon = Icons.Outlined.Percent,
                tint = colors.debt,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = "Toplam ödeme",
                value = Money.format(result.totalPaid),
                icon = Icons.Outlined.CheckCircle,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
        }
        StatCard(
            title = "Bu sürede biriken tutar",
            value = Money.format(result.totalSaved),
            icon = Icons.Outlined.Savings,
            tint = colors.saving,
            supporting = "Birikim hedefleri ve borçlara aktarılamayan artan para (faiz getirisi hariç).",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ComparisonCard(primary: SimulationResult, alternative: SimulationResult) {
    val interestDiff = alternative.totalInterest - primary.totalInterest
    val monthDiff = alternative.months - primary.months
    val text = buildString {
        append("${alternative.strategy.label} ile borçlar ${Dates.formatDuration(alternative.months)} sürer ")
        append("ve ${Money.format(alternative.totalInterest)} faiz ödenir. ")
        when {
            interestDiff > 0.5 -> append("${primary.strategy.label} size ${Money.format(interestDiff)} faiz tasarrufu sağlıyor")
            interestDiff < -0.5 -> append("${primary.strategy.label} ${Money.format(-interestDiff)} daha fazla faize mal oluyor")
            else -> append("İki yöntemin faiz maliyeti aynı")
        }
        when {
            monthDiff > 0 -> append(" ve $monthDiff ay daha erken bitiyor.")
            monthDiff < 0 -> append(", ancak ${-monthDiff} ay daha geç bitiyor.")
            else -> append("; bitiş süresi aynı.")
        }
    }
    InfoCard(text, icon = Icons.AutoMirrored.Outlined.TrendingDown)
}

@Composable
private fun PayoffRow(order: Int, name: String, month: Int, date: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$order.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(date, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$month. ay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
