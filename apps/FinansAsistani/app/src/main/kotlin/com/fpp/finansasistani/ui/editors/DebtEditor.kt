package com.fpp.finansasistani.ui.editors

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fpp.finans.core.Dates
import com.fpp.finans.core.Debt
import com.fpp.finans.core.DebtType
import com.fpp.finans.core.FinanceCalculator
import com.fpp.finans.core.Money
import com.fpp.finansasistani.ui.components.DropdownField
import com.fpp.finansasistani.ui.components.EditorScaffold
import com.fpp.finansasistani.ui.components.InfoCard
import com.fpp.finansasistani.ui.components.NameField
import com.fpp.finansasistani.ui.components.NumberField
import com.fpp.finansasistani.viewmodel.FinanceViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtEditorScreen(id: Long, vm: FinanceViewModel, onBack: () -> Unit) {
    val isNew = id <= 0L
    val state by vm.state.collectAsStateWithLifecycle()
    var loaded by rememberSaveable { mutableStateOf(isNew) }
    var type by rememberSaveable { mutableStateOf(DebtType.CREDIT_CARD) }
    var name by rememberSaveable { mutableStateOf("") }
    var balance by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var minPayment by rememberSaveable { mutableStateOf("") }
    var cardLimit by rememberSaveable { mutableStateOf("") }
    var installments by rememberSaveable { mutableStateOf("") }
    var firstDateEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().plusMonths(1).toEpochDay()) }
    var createdAt by rememberSaveable { mutableLongStateOf(0L) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (!loaded) {
            val item = vm.loadDebt(id)
            if (item == null) {
                onBack()
            } else {
                type = item.type
                name = item.name
                balance = Money.formatInput(item.balance)
                rate = Money.formatInput(item.annualInterestRate)
                minPayment = Money.formatInput(item.minPayment)
                cardLimit = Money.formatInput(item.cardLimit)
                installments = if (item.remainingInstallments > 0) item.remainingInstallments.toString() else ""
                item.firstPaymentDate?.let { firstDateEpochDay = it.toEpochDay() }
                createdAt = item.createdAt
                loaded = true
            }
        }
    }
    if (!loaded) {
        LoadingBox()
        return
    }

    val isInstallment = type == DebtType.INSTALLMENT
    val isCard = type == DebtType.CREDIT_CARD
    val needsMinPayment = type == DebtType.CONSUMER_LOAN || type == DebtType.MORTGAGE
    val firstDate = LocalDate.ofEpochDay(firstDateEpochDay)

    fun installmentError(show: Boolean): String? {
        if (!show) return null
        val n = installments.toIntOrNull()
        return if (n == null || n !in 1..1200) "1 ile 1200 arasında bir sayı girin." else null
    }

    fun errors(show: Boolean): List<String?> = buildList {
        add(Validate.name(name, show))
        if (isInstallment) {
            add(Validate.positive(minPayment, show))
            add(installmentError(show))
        } else {
            add(Validate.positive(balance, show))
            add(Validate.optional(rate, show, max = 1000.0))
            if (isCard) add(Validate.optional(cardLimit, show))
            if (needsMinPayment) add(Validate.positive(minPayment, show))
            if (type == DebtType.OTHER) add(Validate.optional(minPayment, show))
        }
    }

    fun save() {
        submitted = true
        if (errors(true).any { it != null }) return
        val payment = Validate.parseOrZero(minPayment)
        val count = installments.toIntOrNull() ?: 0
        val debt = if (isInstallment) {
            Debt(
                id = if (isNew) 0 else id,
                name = name.trim(),
                type = type,
                balance = payment * count,
                annualInterestRate = 0.0,
                minPayment = payment,
                remainingInstallments = count,
                firstPaymentDate = firstDate,
                createdAt = createdAt,
            )
        } else {
            Debt(
                id = if (isNew) 0 else id,
                name = name.trim(),
                type = type,
                balance = Money.parse(balance) ?: 0.0,
                annualInterestRate = Validate.parseOrZero(rate),
                minPayment = if (needsMinPayment || type == DebtType.OTHER) payment else 0.0,
                cardLimit = if (isCard) Validate.parseOrZero(cardLimit) else 0.0,
                createdAt = createdAt,
            )
        }
        vm.save(debt, onDone = onBack)
    }

    EditorScaffold(title = if (isNew) "Yeni borç" else "Borcu düzenle", onBack = onBack, onSave = ::save) {
        DropdownField(
            label = "Borç türü",
            options = DebtType.entries,
            selected = type,
            optionLabel = { it.label },
            onSelected = { type = it },
        )
        NameField(
            name, { name = it },
            label = "Borcun adı",
            placeholder = if (isCard) "Örn: X Bank kartı" else "Örn: İhtiyaç kredisi",
            error = Validate.name(name, submitted),
        )

        if (isInstallment) {
            NumberField(
                value = minPayment,
                onValueChange = { minPayment = it },
                label = "Aylık taksit tutarı",
                error = Validate.positive(minPayment, submitted),
            )
            NumberField(
                value = installments,
                onValueChange = { installments = it },
                label = "Kalan taksit sayısı",
                suffix = "taksit",
                decimal = false,
                error = installmentError(submitted),
                imeAction = ImeAction.Done,
            )
            OutlinedTextField(
                value = firstDate.format(Dates.fullDate),
                onValueChange = {},
                readOnly = true,
                label = { Text("İlk ödeme tarihi") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Tarih seç")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { showDatePicker = true }) { Text("Tarihi değiştir") }
            val total = Validate.parseOrZero(minPayment) * (installments.toIntOrNull() ?: 0)
            InfoCard(
                "Toplam kalan borç: ${Money.format(total)}. Sabit taksitli borçlar faizsizdir ve ilk ödeme " +
                    "tarihinden itibaren takvimine göre ödenir; ekstra ödeme bu borçlara aktarılmaz.",
            )
        } else {
            NumberField(
                value = balance,
                onValueChange = { balance = it },
                label = if (isCard) "Güncel kart borcu" else "Güncel kalan borç",
                error = Validate.positive(balance, submitted),
            )
            NumberField(
                value = rate,
                onValueChange = { rate = it },
                label = "Yıllık faiz oranı",
                suffix = "%",
                error = Validate.optional(rate, submitted, max = 1000.0),
                hint = "Aylık oran biliniyorsa 12 ile çarpın (ör. aylık %4,25 → yıllık %51).",
            )
            if (isCard) {
                NumberField(
                    value = cardLimit,
                    onValueChange = { cardLimit = it },
                    label = "Kart limiti",
                    error = Validate.optional(cardLimit, submitted),
                    imeAction = ImeAction.Done,
                )
                val otherLimits = state.data.debts
                    .filter { it.type == DebtType.CREDIT_CARD && it.id != id }
                    .sumOf { it.cardLimit }
                val totalLimit = otherLimits + Validate.parseOrZero(cardLimit)
                val ratio = FinanceCalculator.cardMinimumRatio(totalLimit)
                val estimate = Validate.parseOrZero(balance) * ratio
                InfoCard(
                    "Toplam kart limitiniz ${Money.format(totalLimit)} olduğu için asgari ödeme oranı " +
                        "${Money.formatPercent(ratio * 100)} uygulanır. Tahmini asgari ödeme: ${Money.format(estimate)}.",
                )
            }
            if (type == DebtType.OVERDRAFT) {
                val interest = Validate.parseOrZero(balance) * FinanceCalculator.monthlyRate(Validate.parseOrZero(rate))
                InfoCard("KMH'de asgari ödeme aylık faiz kadardır: yaklaşık ${Money.format(interest)}.")
            }
            if (needsMinPayment || type == DebtType.OTHER) {
                NumberField(
                    value = minPayment,
                    onValueChange = { minPayment = it },
                    label = if (needsMinPayment) "Aylık taksit tutarı" else "Aylık asgari ödeme (isteğe bağlı)",
                    error = if (needsMinPayment) Validate.positive(minPayment, submitted) else Validate.optional(minPayment, submitted),
                    imeAction = ImeAction.Done,
                )
                val monthlyInterest = Validate.parseOrZero(balance) * FinanceCalculator.monthlyRate(Validate.parseOrZero(rate))
                val payment = Validate.parseOrZero(minPayment)
                if (payment > 0 && monthlyInterest >= payment) {
                    InfoCard(
                        "Aylık ödeme, aylık faizi (${Money.format(monthlyInterest)}) karşılamıyor. Ekstra ödeme yapılmazsa bu borç azalmaz.",
                        isError = true,
                        icon = Icons.Outlined.WarningAmber,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = firstDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        firstDateEpochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Vazgeç") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
