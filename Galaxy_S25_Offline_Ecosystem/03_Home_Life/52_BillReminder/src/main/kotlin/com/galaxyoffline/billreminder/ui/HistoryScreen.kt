package com.galaxyoffline.billreminder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.ActionRow
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.EmptyState
import com.galaxyoffline.core.designsystem.components.GroupCard
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: BillViewModel, billId: Long, onBack: () -> Unit) {
    val bills by vm.bills.collectAsStateWithLifecycle()
    val all by vm.payments.collectAsStateWithLifecycle()
    val bill = bills?.firstOrNull { it.id == billId }
    val list = all.filter { it.billId == billId }
    Scaffold(topBar = { TopAppBar(title = { Text(bill?.name ?: "Geçmiş") }, navigationIcon = { BackButton(onBack) }) }) { p ->
        if (list.isEmpty()) Box(Modifier.padding(p)) { EmptyState(Icons.Outlined.History, "Ödeme kaydı yok", "\"Ödendi\" dediğiniz ödemeler burada listelenir.") }
        else LazyColumn(Modifier.padding(p).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Toplam ${list.size} ödeme · ${TrFormat.money(list.sumOf { it.amount })}") }
            items(list, key = { it.id }) { pmt ->
                GroupCard {
                    ActionRow(TrFormat.money(pmt.amount), subtitle = "Dönem: ${TrFormat.formatDate(LocalDate.ofEpochDay(pmt.periodDue))} · ödendi ${TrFormat.formatDate(LocalDate.ofEpochDay(pmt.paidOn))}",
                        trailing = { IconButton(onClick = { vm.deletePayment(pmt) }) { Icon(Icons.Outlined.Delete, "Kaydı sil") } })
                }
            }
        }
    }
}
