package com.fpp.finansasistani.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fpp.finans.core.Dates
import com.fpp.finans.core.Money
import com.fpp.finans.core.SimulationResult
import com.fpp.finansasistani.ui.components.EmptyState
import com.fpp.finansasistani.viewmodel.SimulationUiState
import com.fpp.finansasistani.viewmodel.SimulationViewModel

private val MonthWidth = 56.dp
private val DateWidth = 120.dp
private val ValueWidth = 128.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentTableScreen(simVm: SimulationViewModel, onBack: () -> Unit) {
    val state by simVm.state.collectAsStateWithLifecycle()
    val result = (state as? SimulationUiState.Done)?.primary
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(result?.let { "Ödeme tablosu · ${it.strategy.label}" } ?: "Ödeme tablosu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        if (result == null) {
            Column(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Outlined.TableChart,
                    title = "Tablo yok",
                    message = "Önce Plan sekmesinden simülasyonu çalıştırın.",
                    actionLabel = "Geri dön",
                    onAction = onBack,
                )
            }
        } else {
            PaymentTable(result, Modifier.padding(padding))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaymentTable(result: SimulationResult, modifier: Modifier) {
    val horizontal = rememberScrollState()
    val tableWidth = MonthWidth + DateWidth + ValueWidth * (result.debtNames.size + 3)
    val done = MaterialTheme.colorScheme.primary
    val headerBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val stripe = MaterialTheme.colorScheme.surfaceContainerLow
    val headerStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
    val cellStyle = MaterialTheme.typography.bodySmall

    Box(modifier.fillMaxSize().horizontalScroll(horizontal)) {
        LazyColumn(Modifier.width(tableWidth).fillMaxHeight()) {
            stickyHeader(key = "header") {
                Column(Modifier.background(headerBg)) {
                    Row {
                        Cell("Ay", MonthWidth, headerStyle)
                        Cell("Tarih", DateWidth, headerStyle, TextAlign.Start)
                        result.debtNames.forEach { name -> Cell(name, ValueWidth, headerStyle, maxLines = 2) }
                        Cell("Ödenen", ValueWidth, headerStyle)
                        Cell("Faiz", ValueWidth, headerStyle)
                        Cell("Kalan toplam", ValueWidth, headerStyle)
                    }
                    HorizontalDivider()
                }
            }
            items(result.rows, key = { it.month }) { row ->
                Row(Modifier.background(if (row.month % 2 == 0) stripe else Color.Transparent)) {
                    Cell(row.month.toString(), MonthWidth, cellStyle)
                    Cell(Dates.formatMonthYear(row.date), DateWidth, cellStyle, TextAlign.Start)
                    row.balances.forEach { balance ->
                        if (balance <= 0.0) {
                            Cell("✓ Bitti", ValueWidth, cellStyle.copy(color = done, fontWeight = FontWeight.SemiBold))
                        } else {
                            Cell(Money.format(balance, withSymbol = false), ValueWidth, cellStyle)
                        }
                    }
                    Cell(Money.format(row.payment, withSymbol = false), ValueWidth, cellStyle)
                    Cell(Money.format(row.interest, withSymbol = false), ValueWidth, cellStyle)
                    Cell(Money.format(row.totalBalance, withSymbol = false), ValueWidth, cellStyle.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun Cell(
    text: String,
    width: Dp,
    style: TextStyle,
    align: TextAlign = TextAlign.End,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        style = style,
        textAlign = align,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(width).padding(horizontal = 8.dp, vertical = 10.dp),
    )
}
