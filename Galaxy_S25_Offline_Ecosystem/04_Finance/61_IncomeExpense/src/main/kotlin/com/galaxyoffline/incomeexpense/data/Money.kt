package com.galaxyoffline.incomeexpense.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import com.galaxyoffline.core.common.TrFormat
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

enum class TxType(val label: String) { EXPENSE("Gider"), INCOME("Gelir") }

enum class Category(val type: TxType, val label: String, val emoji: String) {
    MARKET(TxType.EXPENSE, "Market", "🛒"), BILLS(TxType.EXPENSE, "Faturalar", "💡"), RENT(TxType.EXPENSE, "Kira / Aidat", "🏠"),
    TRANSPORT(TxType.EXPENSE, "Ulaşım / Yakıt", "🚗"), FOOD(TxType.EXPENSE, "Yeme içme", "🍽️"), HEALTH(TxType.EXPENSE, "Sağlık", "💊"),
    EDUCATION(TxType.EXPENSE, "Eğitim", "📚"), CLOTHING(TxType.EXPENSE, "Giyim", "👕"), FUN(TxType.EXPENSE, "Eğlence", "🎬"),
    HOME(TxType.EXPENSE, "Ev / Bakım", "🔧"), TECH(TxType.EXPENSE, "Teknoloji", "📱"), DEBT(TxType.EXPENSE, "Kredi / Borç", "💳"),
    OTHER_EXPENSE(TxType.EXPENSE, "Diğer gider", "📦"),
    SALARY(TxType.INCOME, "Maaş", "💼"), SIDE(TxType.INCOME, "Ek gelir", "🧰"), RENT_INCOME(TxType.INCOME, "Kira geliri", "🏘️"),
    GIFT(TxType.INCOME, "Hediye", "🎁"), INVEST(TxType.INCOME, "Yatırım getirisi", "📈"), OTHER_INCOME(TxType.INCOME, "Diğer gelir", "💰");

    companion object {
        fun of(k: String?) = entries.firstOrNull { it.name == k } ?: OTHER_EXPENSE
        fun forType(t: TxType) = entries.filter { it.type == t }
    }
}

enum class Method(val label: String) { CASH("Nakit"), CARD("Kart"), TRANSFER("Havale/EFT"), OTHER("Diğer") }

@Entity(tableName = "transactions", indices = [Index("date")])
data class Tx(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Double,
    val category: String,
    val date: Long,
    val note: String = "",
    val method: String = Method.CARD.name,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    val txType get() = TxType.valueOf(type)
    val cat get() = Category.of(category)
    val localDate: LocalDate get() = LocalDate.ofEpochDay(date)
}

@Entity(tableName = "budgets")
data class Budget(@PrimaryKey val category: String, @ColumnInfo(name = "monthly_limit") val monthlyLimit: Double)

@Dao
interface MoneyDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC") fun observeAll(): Flow<List<Tx>>
    @Query("SELECT * FROM transactions ORDER BY date, created_at") suspend fun all(): List<Tx>
    @Upsert suspend fun upsert(t: Tx)
    @Query("DELETE FROM transactions WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM budgets") fun observeBudgets(): Flow<List<Budget>>
    @Upsert suspend fun upsertBudget(b: Budget)
    @Query("DELETE FROM budgets WHERE category = :c") suspend fun deleteBudget(c: String)
}

@Database(entities = [Tx::class, Budget::class], version = 1, exportSchema = true)
abstract class MoneyDatabase : RoomDatabase() {
    abstract fun money(): MoneyDao
    companion object {
        @Volatile private var instance: MoneyDatabase? = null
        fun get(c: Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, MoneyDatabase::class.java, "gelir_gider.db").build().also { instance = it }
        }
    }
}

data class CategoryTotal(val category: Category, val total: Double, val share: Double, val limit: Double?)

data class MonthSummary(
    val month: YearMonth,
    val income: Double,
    val expense: Double,
    val previousExpense: Double,
    val categories: List<CategoryTotal>,
    val transactions: List<Tx>,
) {
    val net get() = income - expense
    /** Önceki aya göre gider değişimi (%); önceki ay giderleri 0 ise null. */
    val expenseChange: Double? get() = if (previousExpense > 0) (expense - previousExpense) / previousExpense * 100 else null
}

object MoneyLogic {
    fun summary(all: List<Tx>, month: YearMonth, budgets: List<Budget>): MonthSummary {
        fun inMonth(t: Tx, m: YearMonth) = YearMonth.from(t.localDate) == m
        val txs = all.filter { inMonth(it, month) }
        val income = txs.filter { it.txType == TxType.INCOME }.sumOf { it.amount }
        val expenseTx = txs.filter { it.txType == TxType.EXPENSE }
        val expense = expenseTx.sumOf { it.amount }
        val prev = all.filter { inMonth(it, month.minusMonths(1)) && it.txType == TxType.EXPENSE }.sumOf { it.amount }
        val limits = budgets.associate { Category.of(it.category) to it.monthlyLimit }
        val cats = expenseTx.groupBy { it.cat }.map { (c, l) -> val s = l.sumOf { it.amount }; CategoryTotal(c, s, if (expense > 0) s / expense else 0.0, limits[c]) }
        val budgetOnly = limits.keys.filter { c -> cats.none { it.category == c } }.map { CategoryTotal(it, 0.0, 0.0, limits[it]) }
        return MonthSummary(month, income, expense, prev, (cats + budgetOnly).sortedByDescending { it.total }, txs)
    }

    /** Excel (Türkçe) uyumlu CSV: noktalı virgül ayırıcı, UTF-8 BOM, ondalık virgül. */
    fun csv(all: List<Tx>): String = buildString {
        append('﻿')
        appendLine("Tarih;Tür;Kategori;Tutar;Ödeme;Not")
        all.forEach { t ->
            val note = t.note.replace("\"", "\"\"")
            appendLine("${t.localDate};${t.txType.label};${t.cat.label};${TrFormat.number(t.amount, 2, grouping = false)};${Method.entries.firstOrNull { it.name == t.method }?.label ?: ""};\"$note\"")
        }
    }
}
