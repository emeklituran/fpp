package com.fpp.finans.core

import java.time.LocalDate

/** Gelir türleri. [isRecurring] false ise gelir tek seferliktir (ilk ay borca aktarılır). */
enum class IncomeType(val label: String, val shortLabel: String, val isRecurring: Boolean) {
    SALARY("Maaş (düzenli ve zamlı)", "Maaş", true),
    REGULAR("Diğer düzenli gelir (kira, ek iş vb. – zamsız)", "Düzenli gelir", true),
    ONE_TIME("Tek seferlik gelir (ikramiye, miras vb.)", "Tek seferlik", false);

    companion object {
        fun fromKey(key: String?): IncomeType = entries.firstOrNull { it.name == key } ?: REGULAR
    }
}

enum class DebtType(val label: String) {
    CREDIT_CARD("Kredi kartı"),
    CONSUMER_LOAN("Tüketici kredisi"),
    MORTGAGE("Konut kredisi"),
    OVERDRAFT("KMH / Ek hesap"),
    INSTALLMENT("Sabit taksitli borç (okul, senet vb.)"),
    OTHER("Diğer borç");

    /** Faiz işleyen borç türü mü? Sabit taksitli borçlar faizsiz kabul edilir. */
    val hasInterest: Boolean get() = this != INSTALLMENT

    /** Aylık asgari ödeme/taksit tutarını kullanıcı mı girer? */
    val userEntersMinPayment: Boolean
        get() = this == CONSUMER_LOAN || this == MORTGAGE || this == OTHER || this == INSTALLMENT

    companion object {
        fun fromKey(key: String?): DebtType = entries.firstOrNull { it.name == key } ?: OTHER
    }
}

enum class SavingStrategy(val label: String) {
    FIXED("Sabit tutar"),
    PERCENTAGE("Yüzdesel pay");

    companion object {
        fun fromKey(key: String?): SavingStrategy = entries.firstOrNull { it.name == key } ?: FIXED
    }
}

data class Income(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: IncomeType,
    /** Yılda kaç kez zam beklendiği (0, 1, 2, 3, 4). Sadece maaş için anlamlıdır. */
    val raisesPerYear: Int = 0,
    /** Her zamda maaşa eklenecek ortalama artış yüzdesi. */
    val raisePercent: Double = 0.0,
    val createdAt: Long = 0,
)

data class Debt(
    val id: Long = 0,
    val name: String,
    val type: DebtType,
    val balance: Double,
    val annualInterestRate: Double = 0.0,
    /** Kredi/diğer: aylık taksit. Sabit taksitli borç: taksit tutarı. Kart/KMH için otomatik hesaplanır. */
    val minPayment: Double = 0.0,
    val cardLimit: Double = 0.0,
    val remainingInstallments: Int = 0,
    val firstPaymentDate: LocalDate? = null,
    val createdAt: Long = 0,
)

data class FixedExpense(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val createdAt: Long = 0,
)

data class SavingGoal(
    val id: Long = 0,
    val name: String,
    val strategy: SavingStrategy,
    val monthlyAmount: Double = 0.0,
    val percent: Double = 0.0,
    val createdAt: Long = 0,
)

/** Tüm kullanıcı verisinin anlık görüntüsü. */
data class FinanceData(
    val incomes: List<Income> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val expenses: List<FixedExpense> = emptyList(),
    val savings: List<SavingGoal> = emptyList(),
) {
    val isEmpty: Boolean
        get() = incomes.isEmpty() && debts.isEmpty() && expenses.isEmpty() && savings.isEmpty()

    val recordCount: Int
        get() = incomes.size + debts.size + expenses.size + savings.size
}
