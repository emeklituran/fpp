package com.fpp.finansasistani.ui.editors

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fpp.finans.core.Money
import com.fpp.finans.core.SavingGoal
import com.fpp.finans.core.SavingStrategy
import com.fpp.finansasistani.ui.components.EditorScaffold
import com.fpp.finansasistani.ui.components.InfoCard
import com.fpp.finansasistani.ui.components.NameField
import com.fpp.finansasistani.ui.components.NumberField
import com.fpp.finansasistani.viewmodel.FinanceViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingEditorScreen(id: Long, vm: FinanceViewModel, onBack: () -> Unit) {
    val isNew = id <= 0L
    val state by vm.state.collectAsStateWithLifecycle()
    var loaded by rememberSaveable { mutableStateOf(isNew) }
    var name by rememberSaveable { mutableStateOf("") }
    var strategy by rememberSaveable { mutableStateOf(SavingStrategy.FIXED) }
    var amount by rememberSaveable { mutableStateOf("") }
    var percent by rememberSaveable { mutableFloatStateOf(20f) }
    var createdAt by rememberSaveable { mutableLongStateOf(0L) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (!loaded) {
            val item = vm.loadSaving(id)
            if (item == null) {
                onBack()
            } else {
                name = item.name
                strategy = item.strategy
                amount = Money.formatInput(item.monthlyAmount)
                percent = item.percent.toFloat().coerceIn(0f, 100f)
                createdAt = item.createdAt
                loaded = true
            }
        }
    }
    if (!loaded) {
        LoadingBox()
        return
    }

    val isFixed = strategy == SavingStrategy.FIXED
    val otherPercent = state.data.savings
        .filter { it.id != id && it.strategy == SavingStrategy.PERCENTAGE }
        .sumOf { it.percent }

    fun save() {
        submitted = true
        if (Validate.name(name, true) != null) return
        if (isFixed && Validate.positive(amount, true) != null) return
        vm.save(
            SavingGoal(
                id = if (isNew) 0 else id,
                name = name.trim(),
                strategy = strategy,
                monthlyAmount = if (isFixed) Money.parse(amount) ?: 0.0 else 0.0,
                percent = if (isFixed) 0.0 else percent.roundToInt().toDouble(),
                createdAt = createdAt,
            ),
            onDone = onBack,
        )
    }

    EditorScaffold(title = if (isNew) "Yeni birikim hedefi" else "Birikim hedefini düzenle", onBack = onBack, onSave = ::save) {
        NameField(name, { name = it }, label = "Hedefin adı", placeholder = "Örn: Acil durum fonu", error = Validate.name(name, submitted))
        Text("Birikim stratejisi", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SavingStrategy.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = strategy == option,
                    onClick = { strategy = option },
                    shape = SegmentedButtonDefaults.itemShape(index, SavingStrategy.entries.size),
                ) { Text(option.label) }
            }
        }
        if (isFixed) {
            NumberField(
                value = amount,
                onValueChange = { amount = it },
                label = "Her ay ayrılacak tutar",
                error = Validate.positive(amount, submitted),
                imeAction = ImeAction.Done,
            )
            InfoCard("Sabit tutar, zorunlu giderler ve asgari borç ödemelerinden sonra kalan paradan ayrılır.")
        } else {
            Text(
                "Kalan paranın %${percent.roundToInt()} kadarı birikime aktarılsın",
                style = MaterialTheme.typography.bodyLarge,
            )
            Slider(
                value = percent,
                onValueChange = { percent = (it / 5f).roundToInt() * 5f },
                valueRange = 0f..100f,
                steps = 19,
            )
            val total = otherPercent + percent.roundToInt()
            if (total > 100) {
                InfoCard(
                    "Tüm yüzdesel hedeflerin toplamı ${Money.formatPercent(total)}. Hesaplamada toplam %100 ile sınırlanır.",
                    isError = true,
                )
            } else {
                InfoCard(
                    "Yüzde; gelirden sabit giderler, asgari ödemeler ve sabit tutarlı birikimler düşüldükten sonra kalan " +
                        "net fazla üzerinden hesaplanır. Kalan kısım borçları hızlandırmak için kullanılır.",
                )
            }
        }
    }
}
