package com.galaxyoffline.incomeexpense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.components.BackButton
import com.galaxyoffline.core.designsystem.components.InfoBanner
import com.galaxyoffline.incomeexpense.data.Category
import com.galaxyoffline.incomeexpense.data.TxType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(vm: MoneyViewModel, onBack: () -> Unit) {
    val budgets by vm.budgets.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Aylık bütçeler") }, navigationIcon = { BackButton(onBack) }) }) { p ->
        LazyColumn(Modifier.padding(p).fillMaxSize().imePadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { InfoBanner("Kategori için aylık harcama sınırı belirleyin. Sınır aşılınca ana ekranda kırmızıyla gösterilir. Boş bırakılan kategoride sınır yoktur.") }
            items(Category.forType(TxType.EXPENSE), key = { it.name }) { c ->
                val current = budgets.firstOrNull { it.category == c.name }?.monthlyLimit
                var text by remember(current) { mutableStateOf(current?.let { TrFormat.number(it, 2, false) } ?: "") }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${c.emoji}  ${c.label}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    OutlinedTextField(text, { v -> text = v.filter { it.isDigit() || it == ',' || it == '.' }.take(12); vm.setBudget(c.name, TrFormat.parseDecimal(text)) },
                        suffix = { Text("₺") }, singleLine = true, placeholder = { Text("Sınır yok") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(170.dp))
                }
            }
        }
    }
}
