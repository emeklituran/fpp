package com.fpp.finans.core

import com.fpp.finans.core.FinanceCalculator.safe
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

enum class Strategy(val label: String, val description: String) {
    AVALANCHE(
        "Çığ yöntemi",
        "Ekstra ödeme önce en yüksek faizli borca gider. En hızlı ve en az faizli yol.",
    ),
    SNOWBALL(
        "Kartopu yöntemi",
        "Ekstra ödeme önce en küçük bakiyeli borca gider. Küçük zaferlerle motive eder.",
    );

    val other: Strategy get() = if (this == AVALANCHE) SNOWBALL else AVALANCHE

    fun order(debts: List<Debt>): List<Debt> = when (this) {
        AVALANCHE -> debts.sortedWith(
            compareByDescending<Debt> { it.annualInterestRate.safe() }.thenBy { it.balance.safe() },
        )
        SNOWBALL -> debts.sortedWith(
            compareBy<Debt> { it.balance.safe() }.thenByDescending { it.annualInterestRate.safe() },
        )
    }

    companion object {
        fun fromKey(key: String?): Strategy = entries.firstOrNull { it.name == key } ?: AVALANCHE
    }
}

/** Simülasyonun bir ayı. [balances] sırası [SimulationResult.debtNames] ile aynıdır. */
data class MonthRow(
    val month: Int,
    val date: YearMonth,
    val balances: List<Double>,
    val totalBalance: Double,
    val interest: Double,
    val payment: Double,
)

data class DebtPayoff(val debtId: Long, val name: String, val month: Int, val date: YearMonth)

data class SimulationResult(
    val strategy: Strategy,
    val startDate: YearMonth,
    val months: Int,
    val initialDebt: Double,
    val totalInterest: Double,
    val totalPaid: Double,
    val totalSaved: Double,
    val debtNames: List<String>,
    val rows: List<MonthRow>,
    val payoffs: List<DebtPayoff>,
    val shortfallMonths: Int,
) {
    val payoffDate: YearMonth get() = startDate.plusMonths(months.toLong())
    val years: Int get() = months / 12
    val remainderMonths: Int get() = months % 12

    /** Grafik için: 0. ay başlangıç bakiyesi, ardından her ayın sonu. */
    val balanceSeries: List<Double> get() = listOf(initialDebt) + rows.map { it.totalBalance }
}

sealed interface SimulationOutcome {
    data class Success(val result: SimulationResult) : SimulationOutcome
    data class Failure(val reason: String) : SimulationOutcome
}

object DebtSimulator {
    const val MAX_MONTHS = 600
    private const val EPS = 0.005

    fun simulate(data: FinanceData, strategy: Strategy, start: YearMonth = YearMonth.now()): SimulationOutcome =
        try {
            run(data, strategy, start)
        } catch (e: RuntimeException) {
            SimulationOutcome.Failure("Hesaplama sırasında beklenmeyen bir hata oluştu: ${e.message ?: e.javaClass.simpleName}")
        }

    private fun run(data: FinanceData, strategy: Strategy, start: YearMonth): SimulationOutcome {
        val active = data.debts.filter { it.balance.safe() > EPS }
        if (active.isEmpty()) {
            return SimulationOutcome.Failure("Simülasyon için bakiyesi olan en az bir borç ekleyin.")
        }
        if (data.incomes.none { it.amount.safe() > 0.0 }) {
            return SimulationOutcome.Failure("Simülasyon için en az bir gelir ekleyin.")
        }
        val cash = FinanceCalculator.cashFlow(data.incomes, active, data.expenses, data.savings)
        if (cash.hasDeficit) {
            return SimulationOutcome.Failure(
                "Bütçenizde aylık ${Money.format(-cash.extraPower)} açık var. Asgari ödemeler, sabit giderler " +
                    "ve birikim hedefleri gelirinizi aşıyor. Giderleri veya birikim hedeflerini azaltın.",
            )
        }

        val ordered = strategy.order(active)
        val n = ordered.size
        val balances = DoubleArray(n) { ordered[it].balance.safe() }
        val remaining = IntArray(n) { ordered[it].remainingInstallments }
        val rates = DoubleArray(n) {
            if (ordered[it].type.hasInterest) FinanceCalculator.monthlyRate(ordered[it].annualInterestRate) else 0.0
        }
        val payoffMonth = IntArray(n)
        val ratio = cash.cardMinimumRatio

        // Zamlı maaşlar: tutar, zam aralığı (ay) ve zam yüzdesi.
        val salaries = data.incomes.filter {
            it.type == IncomeType.SALARY && it.raisesPerYear in 1..12 && it.raisePercent.safe() > 0.0
        }
        val salaryAmounts = DoubleArray(salaries.size) { salaries[it].amount.safe() }
        val raiseIntervals = IntArray(salaries.size) { max(1, 12 / salaries[it].raisesPerYear) }

        var budget = cash.debtBudget
        var totalInterest = 0.0
        var totalPaid = 0.0
        var totalSaved = 0.0
        var shortfallMonths = 0
        val rows = ArrayList<MonthRow>()
        val minimums = DoubleArray(n)
        var month = 0

        while (balances.any { it > EPS }) {
            if (month >= MAX_MONTHS) {
                return SimulationOutcome.Failure(
                    "Bu plan ${MAX_MONTHS / 12} yıldan uzun sürüyor. Aylık ödemeler faizi karşılamıyor olabilir; " +
                        "verilerinizi (faiz oranları, taksitler, ilk ödeme tarihleri) gözden geçirin.",
                )
            }
            month++
            val date = start.plusMonths(month.toLong())

            // 1) Maaş zamları: artışın tamamı borç bütçesine eklenir.
            for (i in salaries.indices) {
                if (month > 1 && (month - 1) % raiseIntervals[i] == 0) {
                    val increase = salaryAmounts[i] * salaries[i].raisePercent.safe() / 100.0
                    salaryAmounts[i] += increase
                    budget += increase
                }
            }

            var available = budget + if (month == 1) cash.oneTimeIncome else 0.0
            totalSaved += cash.totalSavings

            // 2) Faiz işletme ve bu ayın asgari ödemeleri.
            var monthInterest = 0.0
            for (i in 0 until n) {
                minimums[i] = 0.0
                if (balances[i] <= EPS) continue
                val debt = ordered[i]
                if (debt.type == DebtType.INSTALLMENT) {
                    val started = debt.firstPaymentDate?.let { !YearMonth.from(it).isAfter(date) } ?: true
                    if (started) {
                        minimums[i] = if (remaining[i] > 0) min(balances[i], debt.minPayment.safe()) else balances[i]
                    }
                } else {
                    val interest = balances[i] * rates[i]
                    balances[i] += interest
                    monthInterest += interest
                    minimums[i] = when (debt.type) {
                        DebtType.CREDIT_CARD -> balances[i] * ratio
                        DebtType.OVERDRAFT -> interest
                        else -> debt.minPayment.safe()
                    }.coerceAtMost(balances[i])
                }
            }

            // 3) Asgari ödemeler.
            var monthPaid = 0.0
            var shortfall = false
            for (i in 0 until n) {
                if (minimums[i] <= 0.0) continue
                val pay = min(minimums[i], max(available, 0.0))
                if (pay < minimums[i] - EPS) shortfall = true
                balances[i] -= pay
                available -= pay
                monthPaid += pay
                if (ordered[i].type == DebtType.INSTALLMENT && pay > EPS) remaining[i]--
            }

            // 4) Kalan tutar strateji sırasına göre borçlara aktarılır (çığ / kartopu).
            //    Faizsiz sabit taksitli borçlar takvimine göre ödenir, erken kapatılmaz.
            for (i in 0 until n) {
                if (available <= EPS) break
                if (ordered[i].type == DebtType.INSTALLMENT || balances[i] <= EPS) continue
                val pay = min(balances[i], available)
                balances[i] -= pay
                available -= pay
                monthPaid += pay
            }

            // Borçlara aktarılamayan para (ör. yalnızca henüz başlamamış taksitler kaldıysa) birikime eklenir.
            if (available > EPS) totalSaved += available
            if (shortfall) shortfallMonths++

            for (i in 0 until n) {
                if (balances[i] <= EPS) {
                    balances[i] = 0.0
                    if (payoffMonth[i] == 0) payoffMonth[i] = month
                }
            }

            totalInterest += monthInterest
            totalPaid += monthPaid
            rows += MonthRow(
                month = month,
                date = date,
                balances = balances.toList(),
                totalBalance = balances.sum(),
                interest = monthInterest,
                payment = monthPaid,
            )
        }

        val payoffs = ordered.indices
            .map { DebtPayoff(ordered[it].id, ordered[it].name, payoffMonth[it], start.plusMonths(payoffMonth[it].toLong())) }
            .sortedWith(compareBy<DebtPayoff> { it.month }.thenBy { it.name })

        return SimulationOutcome.Success(
            SimulationResult(
                strategy = strategy,
                startDate = start,
                months = month,
                initialDebt = ordered.sumOf { it.balance.safe() },
                totalInterest = totalInterest,
                totalPaid = totalPaid,
                totalSaved = totalSaved,
                debtNames = ordered.map { it.name },
                rows = rows,
                payoffs = payoffs,
                shortfallMonths = shortfallMonths,
            ),
        )
    }
}
