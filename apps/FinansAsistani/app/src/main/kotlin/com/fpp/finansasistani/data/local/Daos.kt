package com.fpp.finansasistani.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes ORDER BY created_at ASC, id ASC")
    suspend fun getAll(): List<IncomeEntity>

    @Query("SELECT * FROM incomes WHERE id = :id")
    suspend fun getById(id: Long): IncomeEntity?

    @Upsert
    suspend fun upsert(entity: IncomeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<IncomeEntity>)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM incomes")
    suspend fun clear()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts ORDER BY created_at ASC, id ASC")
    suspend fun getAll(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: Long): DebtEntity?

    @Upsert
    suspend fun upsert(entity: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DebtEntity>)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM debts")
    suspend fun clear()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM fixed_expenses ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM fixed_expenses ORDER BY created_at ASC, id ASC")
    suspend fun getAll(): List<ExpenseEntity>

    @Query("SELECT * FROM fixed_expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Upsert
    suspend fun upsert(entity: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExpenseEntity>)

    @Query("DELETE FROM fixed_expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM fixed_expenses")
    suspend fun clear()
}

@Dao
interface SavingDao {
    @Query("SELECT * FROM savings ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<SavingEntity>>

    @Query("SELECT * FROM savings ORDER BY created_at ASC, id ASC")
    suspend fun getAll(): List<SavingEntity>

    @Query("SELECT * FROM savings WHERE id = :id")
    suspend fun getById(id: Long): SavingEntity?

    @Upsert
    suspend fun upsert(entity: SavingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SavingEntity>)

    @Query("DELETE FROM savings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM savings")
    suspend fun clear()
}
