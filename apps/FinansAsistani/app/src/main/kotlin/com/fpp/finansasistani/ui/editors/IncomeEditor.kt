package com.fpp.finansasistani.ui.editors

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.fpp.finans.core.Income
import com.fpp.finans.core.IncomeType
import com.fpp.finans.core.Money
import com.fpp.finansasistani.ui.components.DropdownField
import com.fpp.finansasistani.ui.components.EditorScaffold
import com.fpp.finansasistani.ui.components.InfoCard
import com.fpp.finansasistani.ui.components.NameField
import com.fpp.finansasistani.ui.components.NumberField
import com.fpp.finansasistani.viewmodel.FinanceViewModel

private val RAISE_OPTIONS = listOf(0, 1, 2, 4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeEditorScreen(id: Long, vm: FinanceViewModel, onBack: () -> Unit) {
    val isNew = id <= 0L
    var loaded by rememberSaveable { mutableStateOf(isNew) }
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(IncomeType.SALARY) }
    var amount by rememberSaveable { mutableStateOf("") }
    var raises by rememberSaveable { mutableIntStateOf(2) }
    var raisePercent by rememberSaveable { mutableStateOf("") }
    var createdAt by rememberSaveable { mutableLongStateOf(0L) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (!loaded) {
            val item = vm.loadIncome(id)
            if (item == null) {
                onBack()
            } else {
                name = item.name
                type = item.type
                amount = Money.formatInput(item.amount)
                raises = item.raisesPerYear.takeIf { it in RAISE_OPTIONS } ?: 0
                raisePercent = Money.formatInput(item.raisePercent)
                createdAt = item.createdAt
                loaded = true
            }
        }
    }
    if (!loaded) {
        LoadingBox()
        return
    }

    val isSalary = type == IncomeType.SALARY
    val nameError = Validate.name(name, submitted)
    val amountError = Validate.positive(amount, submitted)
    val percentError = if (isSalary && raises > 0) Validate.optional(raisePercent, submitted, max = 500.0) else null

    fun save() {
        submitted = true
        val ok = Validate.name(name, true) == null && Validate.positive(amount, true) == null &&
            (!isSalary || raises == 0 || Validate.optional(raisePercent, true, 500.0) == null)
        if (!ok) return
        vm.save(
            Income(
                id = if (isNew) 0 else id,
                name = name.trim(),
                amount = Money.parse(amount) ?: 0.0,
                type = type,
                raisesPerYear = if (isSalary) raises else 0,
                raisePercent = if (isSalary && raises > 0) Validate.parseOrZero(raisePercent) else 0.0,
                createdAt = createdAt,
            ),
            onDone = onBack,
        )
    }

    EditorScaffold(title = if (isNew) "Yeni gelir" else "Geliri düzenle", onBack = onBack, onSave = ::save) {
        DropdownField(
            label = "Gelir türü",
            options = IncomeType.entries,
            selected = type,
            optionLabel = { it.label },
            onSelected = { type = it },
        )
        NameField(name, { name = it }, label = "Gelir kaynağının adı", placeholder = "Örn: Maaş", error = nameError)
        NumberField(
            value = amount,
            onValueChange = { amount = it },
            label = if (type.isRecurring) "Aylık net tutar" else "Tutar",
            error = amountError,
            imeAction = if (isSalary) ImeAction.Next else ImeAction.Done,
        )
        if (isSalary) {
            Text("Yılda kaç kez zam bekleniyor?", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                RAISE_OPTIONS.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = raises == option,
                        onClick = { raises = option },
                        shape = SegmentedButtonDefaults.itemShape(index, RAISE_OPTIONS.size),
                    ) { Text(if (option == 0) "Yok" else "$option kez") }
                }
            }
            if (raises > 0) {
                NumberField(
                    value = raisePercent,
                    onValueChange = { raisePercent = it },
                    label = "Her zamdaki ortalama artış",
                    suffix = "%",
                    error = percentError,
                    hint = "Örn: yılda 2 zam ve her birinde %20 artış.",
                    imeAction = ImeAction.Done,
                )
            }
        }
        InfoCard(
            when (type) {
                IncomeType.SALARY -> "Zamlarla oluşan gelir artışı, simülasyonda tamamen borç ödemesine aktarılır."
                IncomeType.REGULAR -> "Düzenli gelir her ay aynı tutarda hesaplanır."
                IncomeType.ONE_TIME -> "Tek seferlik gelir aylık bütçeye eklenmez; simülasyonun ilk ayında borçlara aktarılır."
            },
        )
    }
}
