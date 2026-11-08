package com.galaxyoffline.billreminder.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class Recurrence(val label: String, val months: Int) {
    MONTHLY("Her ay", 1), QUARTERLY("3 ayda bir", 3), SEMIANNUAL("6 ayda bir", 6), YEARLY("Her yıl", 12), ONCE("Tek seferlik", 0);
    companion object { fun of(s: String?) = entries.firstOrNull { it.name == s } ?: MONTHLY }
}

enum class BillKind(val label: String, val emoji: String) {
    ELECTRIC("Elektrik", "💡"), WATER("Su", "🚰"), GAS("Doğalgaz", "🔥"), INTERNET("İnternet / TV", "📡"), PHONE("Telefon", "📱"),
    RENT("Kira / Aidat", "🏠"), SUBSCRIPTION("Abonelik", "🔁"), INSURANCE("Sigorta", "🛡️"), CREDIT("Kredi / Kart", "💳"), TAX("Vergi", "🏛️"),
    EDUCATION("Eğitim", "🎓"), OTHER("Diğer", "🧾");
    companion object { fun of(s: String?) = entries.firstOrNull { it.name == s } ?: OTHER }
}

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Tahmini/sabit tutar (değişken faturalarda son tutar). */
    val amount: Double,
    val kind: String = BillKind.OTHER.name,
    val recurrence: String = Recurrence.MONTHLY.name,
    /** Bir sonraki son ödeme günü (epochDay). */
    @ColumnInfo(name = "due_date") val dueDate: Long,
    /** Ay sonu kaymasını önlemek için ilk girilen gün (1–31). */
    @ColumnInfo(name = "anchor_day") val anchorDay: Int,
    @ColumnInfo(name = "remind_days") val remindDays: Int = 3,
    val active: Boolean = true,
    val note: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    val due: LocalDate get() = LocalDate.ofEpochDay(dueDate)
    val rec get() = Recurrence.of(recurrence)
    val kindEnum get() = BillKind.of(kind)
}

@Entity(tableName = "payments", foreignKeys = [ForeignKey(entity = Bill::class, parentColumns = ["id"], childColumns = ["bill_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bill_id")])
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "bill_id") val billId: Long,
    val amount: Double,
    @ColumnInfo(name = "period_due") val periodDue: Long,
    @ColumnInfo(name = "paid_on") val paidOn: Long,
)

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY due_date") fun observeBills(): Flow<List<Bill>>
    @Query("SELECT * FROM bills WHERE active = 1") suspend fun activeBills(): List<Bill>
    @Query("SELECT * FROM payments ORDER BY paid_on DESC") fun observePayments(): Flow<List<Payment>>
    @Upsert suspend fun upsert(b: Bill): Long
    @Query("DELETE FROM bills WHERE id = :id") suspend fun delete(id: Long)
    @Insert suspend fun insertPayment(p: Payment)
    @Query("DELETE FROM payments WHERE id = :id") suspend fun deletePayment(id: Long)
}

@Database(entities = [Bill::class, Payment::class], version = 1, exportSchema = true)
abstract class BillDatabase : RoomDatabase() {
    abstract fun bills(): BillDao
    companion object {
        @Volatile private var instance: BillDatabase? = null
        fun get(c: Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, BillDatabase::class.java, "faturalar.db").build().also { instance = it }
        }
    }
}

enum class BillStatus(val label: String) { OVERDUE("Gecikti"), DUE_SOON("Yaklaşıyor"), UPCOMING("Sonraki"), DONE("Ödendi") }

object BillLogic {
    fun status(b: Bill, today: LocalDate): BillStatus = when {
        !b.active -> BillStatus.DONE
        b.due.isBefore(today) -> BillStatus.OVERDUE
        ChronoUnit.DAYS.between(today, b.due) <= b.remindDays -> BillStatus.DUE_SOON
        else -> BillStatus.UPCOMING
    }

    /** Bir sonraki dönem; ayın son gününe taşan çapa günü korunur (31 Ocak → 28 Şubat → 31 Mart). */
    fun nextDue(b: Bill): LocalDate? {
        if (b.rec == Recurrence.ONCE) return null
        val n = b.due.plusMonths(b.rec.months.toLong())
        return n.withDayOfMonth(minOf(b.anchorDay, n.lengthOfMonth()))
    }

    /** Ödeme kaydedildikten sonraki fatura durumu. */
    fun afterPayment(b: Bill): Bill = nextDue(b)?.let { b.copy(dueDate = it.toEpochDay()) } ?: b.copy(active = false)

    fun monthlyCost(bills: List<Bill>) = bills.filter { it.active && it.rec != Recurrence.ONCE }.sumOf { it.amount / it.rec.months }

    fun daysText(b: Bill, today: LocalDate): String {
        val d = ChronoUnit.DAYS.between(today, b.due)
        return when { !b.active -> "Ödendi"; d < 0 -> "${-d} gün gecikti"; d == 0L -> "Bugün son gün"; d == 1L -> "Yarın son gün"; else -> "$d gün kaldı" }
    }
}
