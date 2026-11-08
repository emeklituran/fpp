package com.fpp.finansasistani.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: String,
    @ColumnInfo(name = "raises_per_year") val raisesPerYear: Int,
    @ColumnInfo(name = "raise_percent") val raisePercent: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val balance: Double,
    @ColumnInfo(name = "interest_rate") val annualInterestRate: Double,
    @ColumnInfo(name = "min_payment") val minPayment: Double,
    @ColumnInfo(name = "card_limit") val cardLimit: Double,
    @ColumnInfo(name = "remaining_installments") val remainingInstallments: Int,
    /** ISO-8601 (yyyy-MM-dd) veya null. */
    @ColumnInfo(name = "first_payment_date") val firstPaymentDate: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "fixed_expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "savings")
data class SavingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val strategy: String,
    @ColumnInfo(name = "monthly_amount") val monthlyAmount: Double,
    val percent: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
