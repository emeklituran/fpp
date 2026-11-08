package com.fpp.finansasistani.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fpp.finans.core.CashFlow
import com.fpp.finans.core.DebtType
import com.fpp.finans.core.Money
import com.fpp.finansasistani.ui.components.IconBadge
import com.fpp.finansasistani.ui.components.InfoCard
import com.fpp.finansasistani.ui.components.SectionTitle
import com.fpp.finansasistani.ui.components.StatCard
import com.fpp.finansasistani.ui.theme.LocalFinanceColors
import com.fpp.finansasistani.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: FinanceViewModel,
    onAdd: (RecordKind) -> Unit,
    onOpenRecords: () -> Unit,
    onOpenSimulation: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finans Asistanı") },
                actions = {
                    IconButton(onClick = onOpenHelp) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Yardım")
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val cash = state.cashFlow
        val colors = LocalFinanceColors.current
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.data.isEmpty) {
                item { WelcomeCard(onAdd = onAdd, onOpenHelp = onOpenHelp) }
                return@LazyColumn
            }
            item { SurplusHero(cash) }
            if (cash.monthlyIncome > 0) {
                item { BudgetBreakdown(cash) }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = "Toplam borç",
                        value = Money.format(cash.totalDebt),
                        icon = Icons.Outlined.CreditCard,
                        tint = colors.debt,
                        supporting = "${state.data.debts.size} borç kaydı",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = "Kart limitleri",
                        value = Money.format(cash.totalCardLimit),
                        icon = Icons.Outlined.AccountBalanceWallet,
                        tint = MaterialTheme.colorScheme.tertiary,
                        supporting = "Asgari oran ${Money.formatPercent(cash.cardMinimumRatio * 100)}",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = "Aylık birikim",
                        value = Money.format(cash.totalSavings),
                        icon = Icons.Outlined.Savings,
                        tint = colors.saving,
                        supporting = "${state.data.savings.size} hedef",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = "Borca ekstra güç",
                        value = Money.format(cash.extraPower),
                        icon = Icons.Outlined.Bolt,
                        tint = if (cash.hasDeficit) colors.debt else colors.income,
                        supporting = "Asgarinin üzerine",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (cash.oneTimeIncome > 0) {
                item {
                    InfoCard(
                        "Tek seferlik gelirleriniz (${Money.format(cash.oneTimeIncome)}) aylık bütçeye eklenmedi; " +
                            "simülasyonun ilk ayında borçlara aktarılır.",
                        icon = Icons.Outlined.Redeem,
                    )
                }
            }
            if (state.data.debts.isNotEmpty()) {
                item {
                    SectionTitle("Borçlarım") { TextButton(onClick = onOpenRecords) { Text("Tümü") } }
                }
                val ratio = cash.cardMinimumRatio
                items(state.data.debts.sortedByDescending { it.balance }.take(5), key = { it.id }) { debt ->
                    DebtRow(
                        name = debt.name,
                        detail = when (debt.type) {
                            DebtType.INSTALLMENT -> "${debt.type.label} · ${debt.remainingInstallments} taksit"
                            else -> "${debt.type.label} · yıllık ${Money.formatPercent(debt.annualInterestRate)}"
                        },
                        amount = Money.format(debt.balance),
                        share = if (cash.totalDebt > 0) (debt.balance / cash.totalDebt).toFloat() else 0f,
                        minimum = Money.format(com.fpp.finans.core.FinanceCalculator.minimumPayment(debt, ratio)),
                    )
                }
                item { SimulationCta(onOpenSimulation) }
            }
            item { QuickAdd(onAdd) }
        }
    }
}

@Composable
private fun SurplusHero(cash: CashFlow) {
    val deficit = cash.netSurplus < 0
    val container = if (deficit) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val content = if (deficit) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (deficit) "Aylık bütçe açığı" else "Net kullanılabilir fazla",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                Money.format(cash.netSurplus),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            HeroLine("Aylık gelir", Money.format(cash.monthlyIncome))
            HeroLine("Sabit giderler", "− " + Money.format(cash.fixedExpenses))
            HeroLine("Borç asgari ödemeleri", "− " + Money.format(cash.minimumPayments))
            if (deficit) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Zorunlu ödemeler gelirinizi aşıyor. Giderleri gözden geçirin.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/** Gelirin nereye gittiğini gösteren yatay yığılmış çubuk. */
@Composable
private fun BudgetBreakdown(cash: CashFlow) {
    val colors = LocalFinanceColors.current
    val segments = listOf(
        Triple("Sabit giderler", cash.fixedExpenses, colors.expense),
        Triple("Asgari ödemeler", cash.minimumPayments, colors.debt),
        Triple("Birikim", cash.totalSavings, colors.saving),
        Triple("Ekstra borç ödemesi", cash.extraPower.coerceAtLeast(0.0), colors.income),
    ).filter { it.second > 0.0 }
    val total = segments.sumOf { it.second }.coerceAtLeast(cash.monthlyIncome)
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Gelirinizin dağılımı", style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                segments.forEach { (_, value, color) ->
                    val weight = (value / total).toFloat()
                    if (weight > 0.001f) {
                        Box(Modifier.weight(weight).fillMaxHeight().background(color))
                    }
                }
                val rest = (1.0 - segments.sumOf { it.second } / total).toFloat()
                if (rest > 0.001f) Spacer(Modifier.weight(rest))
            }
            segments.forEach { (label, value, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(color, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        "${Money.format(value)} · ${Money.formatPercent(value / total * 100)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtRow(name: String, detail: String, amount: String, share: Float, minimum: String) {
    val debtColor = LocalFinanceColors.current.debt
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(amount, style = MaterialTheme.typography.titleSmall, color = debtColor)
                    Text(
                        "Asgari $minimum",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(share.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(debtColor.copy(alpha = 0.8f)),
                )
            }
        }
    }
}

@Composable
private fun SimulationCta(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.AutoMirrored.Outlined.TrendingDown, MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Borçsuz güne ne kadar kaldı?", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Çığ veya kartopu yöntemiyle ödeme planınızı oluşturun.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun QuickAdd(onAdd: (RecordKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Hızlı ekle")
        RecordKind.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { kind ->
                    FilledTonalButton(onClick = { onAdd(kind) }, modifier = Modifier.weight(1f)) {
                        Icon(kind.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(kind.addLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard(onAdd: (RecordKind) -> Unit, onOpenHelp: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hoş geldiniz!", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Gelir, gider ve borçlarınızı ekleyin; uygulama nakit akışınızı çıkarsın ve borçlarınızı en " +
                        "verimli şekilde kapatmanız için ay ay bir plan hazırlasın.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Tüm veriler yalnızca bu cihazda saklanır. Hesap, üyelik veya internet gerekmez.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onOpenHelp, modifier = Modifier.align(Alignment.End)) {
                    Text("Nasıl kullanılır?")
                }
            }
        }
        StepRow(1, "Gelirlerinizi ekleyin", "Maaşınız ve beklediğiniz zamlar.")
        StepRow(2, "Borçlarınızı ekleyin", "Kart, kredi, KMH ve taksitli borçlar.")
        StepRow(3, "Sabit gider ve birikim hedefleri", "Kira, faturalar ve aylık birikim.")
        StepRow(4, "Planınızı oluşturun", "Plan sekmesinden simülasyonu çalıştırın.")
        QuickAdd(onAdd)
    }
}

@Composable
private fun StepRow(number: Int, title: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

