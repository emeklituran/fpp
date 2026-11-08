package com.fpp.finansasistani.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [IncomeEntity::class, DebtEntity::class, ExpenseEntity::class, SavingEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun incomeDao(): IncomeDao
    abstract fun debtDao(): DebtDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun savingDao(): SavingDao

    companion object {
        private const val NAME = "finans_asistani.db"

        // Yeni şema sürümlerinde buraya Migration nesneleri eklenmelidir; veriler asla sessizce silinmez.
        fun build(context: Context): FinanceDatabase =
            Room.databaseBuilder(context.applicationContext, FinanceDatabase::class.java, NAME)
                .build()
    }
}
