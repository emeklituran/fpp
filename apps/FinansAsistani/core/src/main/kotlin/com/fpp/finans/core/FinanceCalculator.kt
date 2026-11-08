package com.fpp.finans.core

import kotlin.math.max

/** Aylık nakit akışı özeti. Tüm tutarlar TL'dir. */
data class CashFlow(
    val monthlyIncome: Double,
    val oneTimeIncome: Double,
    val fixedExpenses: Double,
    val minimumPayments: Double,
    val netSurplus: Double,
    val fixedSavings: Double,
    val percentageSavings: Double,
    val totalDebt: Double,
    val totalCardLimit: Double,
    val cardMinimumRatio: Double,
) {
    /** Sabit giderler + borç asgari ödemeleri. */
    val mandatoryOutflow: Double get() = fixedExpenses + minimumPayments

    val totalSavings: Double get() = fixedSavings + percentageSavings

    /** Asgari ödemelerin üzerine borçlara aktarılabilecek ek tutar. Negatifse bütçe açığı vardır. */
    val extraPower: Double get() = netSurplus - totalSavings

    /** Her ay borçlara ayrılabilecek toplam tutar (asgari ödemeler + ekstra güç). */
    val debtBudget: Double get() = minimumPayments + extraPower

    val hasDeficit: Boolean get() = extraPower < -0.005
}

object FinanceCalculator {
    /** BDDK düzenlemesine göre kart limiti eşiği (toplam limit üzerinden değerlendirilir). */
    const val CARD_LIMIT_THRESHOLD = 50_000.0
    const val LOW_LIMIT_MIN_RATIO = 0.20
    const val HIGH_LIMIT_MIN_RATIO = 0.40

    fun totalCardLimit(debts: List<Debt>): Double =
        debts.filter { it.type == DebtType.CREDIT_CARD }.sumOf { it.cardLimit.safe() }

    fun cardMinimumRatio(totalCardLimit: Double): Double =
        if (totalCardLimit > CARD_LIMIT_THRESHOLD) HIGH_LIMIT_MIN_RATIO else LOW_LIMIT_MIN_RATIO

    fun monthlyRate(annualPercent: Double): Double = annualPercent.safe() / 100.0 / 12.0

    /** Borcun bugünkü bakiyesine göre aylık asgari ödemesi. */
    fun minimumPayment(debt: Debt, cardRatio: Double): Double {
        val balance = debt.balance.safe()
        if (balance <= 0.0) return 0.0
        val value = when (debt.type) {
            DebtType.CREDIT_CARD -> balance * cardRatio
            DebtType.OVERDRAFT -> balance * monthlyRate(debt.annualInterestRate)
            DebtType.INSTALLMENT -> if (debt.remainingInstallments > 0) debt.minPayment.safe() else balance
            else -> debt.minPayment.safe()
        }
        return value.coerceIn(0.0, balance)
    }

    fun cashFlow(data: FinanceData): CashFlow =
        cashFlow(data.incomes, data.debts, data.expenses, data.savings)

    fun cashFlow(
        incomes: List<Income>,
        debts: List<Debt>,
        expenses: List<FixedExpense>,
        savings: List<SavingGoal>,
    ): CashFlow {
        val monthlyIncome = incomes.filter { it.type.isRecurring }.sumOf { it.amount.safe() }
        val oneTime = incomes.filterNot { it.type.isRecurring }.sumOf { it.amount.safe() }
        val fixed = expenses.sumOf { it.amount.safe() }
        val limit = totalCardLimit(debts)
        val ratio = cardMinimumRatio(limit)
        val minimums = debts.sumOf { minimumPayment(it, ratio) }
        val surplus = monthlyIncome - fixed - minimums

        val fixedSavings = savings.filter { it.strategy == SavingStrategy.FIXED }
            .sumOf { it.monthlyAmount.safe() }
        val percent = savings.filter { it.strategy == SavingStrategy.PERCENTAGE }
            .sumOf { it.percent.safe() }
            .coerceIn(0.0, 100.0)
        val percentageSavings = max(0.0, surplus - fixedSavings) * percent / 100.0

        return CashFlow(
            monthlyIncome = monthlyIncome,
            oneTimeIncome = oneTime,
            fixedExpenses = fixed,
            minimumPayments = minimums,
            netSurplus = surplus,
            fixedSavings = fixedSavings,
            percentageSavings = percentageSavings,
            totalDebt = debts.sumOf { it.balance.safe() },
            totalCardLimit = limit,
            cardMinimumRatio = ratio,
        )
    }

    /** NaN/sonsuz/negatif değerleri 0'a çeker; bozuk veri hesaplamayı çökertmez. */
    internal fun Double.safe(): Double = if (isFinite() && this > 0.0) this else 0.0
}
