package com.fpp.finansasistani.data

import androidx.room.withTransaction
import com.fpp.finans.core.Debt
import com.fpp.finans.core.FinanceData
import com.fpp.finans.core.FixedExpense
import com.fpp.finans.core.Income
import com.fpp.finans.core.SavingGoal
import com.fpp.finansasistani.data.local.FinanceDatabase
import com.fpp.finansasistani.data.local.toDomain
import com.fpp.finansasistani.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Tüm finans verisine tek erişim noktası. Veriler yalnızca cihazdaki Room veritabanında tutulur. */
class FinanceRepository(private val db: FinanceDatabase) {
    private val incomeDao = db.incomeDao()
    private val debtDao = db.debtDao()
    private val expenseDao = db.expenseDao()
    private val savingDao = db.savingDao()

    val data: Flow<FinanceData> = combine(
        incomeDao.observeAll(),
        debtDao.observeAll(),
        expenseDao.observeAll(),
        savingDao.observeAll(),
    ) { incomes, debts, expenses, savings ->
        FinanceData(
            incomes = incomes.map { it.toDomain() },
            debts = debts.map { it.toDomain() },
            expenses = expenses.map { it.toDomain() },
            savings = savings.map { it.toDomain() },
        )
    }

    suspend fun snapshot(): FinanceData = db.withTransaction {
        FinanceData(
            incomes = incomeDao.getAll().map { it.toDomain() },
            debts = debtDao.getAll().map { it.toDomain() },
            expenses = expenseDao.getAll().map { it.toDomain() },
            savings = savingDao.getAll().map { it.toDomain() },
        )
    }

    suspend fun getIncome(id: Long): Income? = incomeDao.getById(id)?.toDomain()
    suspend fun getDebt(id: Long): Debt? = debtDao.getById(id)?.toDomain()
    suspend fun getExpense(id: Long): FixedExpense? = expenseDao.getById(id)?.toDomain()
    suspend fun getSaving(id: Long): SavingGoal? = savingDao.getById(id)?.toDomain()

    suspend fun save(income: Income) = incomeDao.upsert(income.toEntity())
    suspend fun save(debt: Debt) = debtDao.upsert(debt.toEntity())
    suspend fun save(expense: FixedExpense) = expenseDao.upsert(expense.toEntity())
    suspend fun save(saving: SavingGoal) = savingDao.upsert(saving.toEntity())

    suspend fun delete(income: Income) = incomeDao.deleteById(income.id)
    suspend fun delete(debt: Debt) = debtDao.deleteById(debt.id)
    suspend fun delete(expense: FixedExpense) = expenseDao.deleteById(expense.id)
    suspend fun delete(saving: SavingGoal) = savingDao.deleteById(saving.id)

    /** Yedekten geri yükleme: mevcut verilerin tamamı tek bir işlemde (atomik) değiştirilir. */
    suspend fun replaceAll(data: FinanceData) = db.withTransaction {
        clearTables()
        incomeDao.insertAll(data.incomes.map { it.toEntity() })
        debtDao.insertAll(data.debts.map { it.toEntity() })
        expenseDao.insertAll(data.expenses.map { it.toEntity() })
        savingDao.insertAll(data.savings.map { it.toEntity() })
    }

    suspend fun clearAll() = db.withTransaction { clearTables() }

    private suspend fun clearTables() {
        incomeDao.clear()
        debtDao.clear()
        expenseDao.clear()
        savingDao.clear()
    }
}
