package com.fpp.finansasistani.ui.editors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import com.fpp.finans.core.FixedExpense
import com.fpp.finans.core.Money
import com.fpp.finansasistani.ui.components.EditorScaffold
import com.fpp.finansasistani.ui.components.InfoCard
import com.fpp.finansasistani.ui.components.NameField
import com.fpp.finansasistani.ui.components.NumberField
import com.fpp.finansasistani.viewmodel.FinanceViewModel

@Composable
fun ExpenseEditorScreen(id: Long, vm: FinanceViewModel, onBack: () -> Unit) {
    val isNew = id <= 0L
    var loaded by rememberSaveable { mutableStateOf(isNew) }
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var createdAt by rememberSaveable { mutableLongStateOf(0L) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (!loaded) {
            val item = vm.loadExpense(id)
            if (item == null) {
                onBack()
            } else {
                name = item.name
                amount = Money.formatInput(item.amount)
                createdAt = item.createdAt
                loaded = true
            }
        }
    }
    if (!loaded) {
        LoadingBox()
        return
    }

    fun save() {
        submitted = true
        if (Validate.name(name, true) != null || Validate.positive(amount, true) != null) return
        vm.save(
            FixedExpense(id = if (isNew) 0 else id, name = name.trim(), amount = Money.parse(amount) ?: 0.0, createdAt = createdAt),
            onDone = onBack,
        )
    }

    EditorScaffold(title = if (isNew) "Yeni sabit gider" else "Gideri düzenle", onBack = onBack, onSave = ::save) {
        NameField(name, { name = it }, label = "Giderin adı", placeholder = "Örn: Kira, elektrik, abonelik", error = Validate.name(name, submitted))
        NumberField(
            value = amount,
            onValueChange = { amount = it },
            label = "Aylık tutar",
            error = Validate.positive(amount, submitted),
            imeAction = ImeAction.Done,
        )
        InfoCard("Sabit giderler her ay gelirden düşülür. Borç taksitlerini buraya değil, Borçlar bölümüne ekleyin.")
    }
}
