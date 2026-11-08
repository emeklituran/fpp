package com.fpp.finansasistani.data.local

import com.fpp.finans.core.Debt
import com.fpp.finans.core.DebtType
import com.fpp.finans.core.FixedExpense
import com.fpp.finans.core.Income
import com.fpp.finans.core.IncomeType
import com.fpp.finans.core.SavingGoal
import com.fpp.finans.core.SavingStrategy
import java.time.LocalDate

private fun now() = System.currentTimeMillis()
private fun Long.orNow() = if (this > 0L) this else now()

fun IncomeEntity.toDomain() = Income(
    id = id,
    name = name,
    amount = amount,
    type = IncomeType.fromKey(type),
    raisesPerYear = raisesPerYear,
    raisePercent = raisePercent,
    createdAt = createdAt,
)

fun Income.toEntity() = IncomeEntity(
    id = id,
    name = name.trim(),
    amount = amount,
    type = type.name,
    raisesPerYear = raisesPerYear,
    raisePercent = raisePercent,
    createdAt = createdAt.orNow(),
)

fun DebtEntity.toDomain() = Debt(
    id = id,
    name = name,
    type = DebtType.fromKey(type),
    balance = balance,
    annualInterestRate = annualInterestRate,
    minPayment = minPayment,
    cardLimit = cardLimit,
    remainingInstallments = remainingInstallments,
    firstPaymentDate = firstPaymentDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    createdAt = createdAt,
)

fun Debt.toEntity() = DebtEntity(
    id = id,
    name = name.trim(),
    type = type.name,
    balance = balance,
    annualInterestRate = annualInterestRate,
    minPayment = minPayment,
    cardLimit = cardLimit,
    remainingInstallments = remainingInstallments,
    firstPaymentDate = firstPaymentDate?.toString(),
    createdAt = createdAt.orNow(),
)

fun ExpenseEntity.toDomain() = FixedExpense(id = id, name = name, amount = amount, createdAt = createdAt)

fun FixedExpense.toEntity() = ExpenseEntity(id = id, name = name.trim(), amount = amount, createdAt = createdAt.orNow())

fun SavingEntity.toDomain() = SavingGoal(
    id = id,
    name = name,
    strategy = SavingStrategy.fromKey(strategy),
    monthlyAmount = monthlyAmount,
    percent = percent,
    createdAt = createdAt,
)

fun SavingGoal.toEntity() = SavingEntity(
    id = id,
    name = name.trim(),
    strategy = strategy.name,
    monthlyAmount = monthlyAmount,
    percent = percent,
    createdAt = createdAt.orNow(),
)
