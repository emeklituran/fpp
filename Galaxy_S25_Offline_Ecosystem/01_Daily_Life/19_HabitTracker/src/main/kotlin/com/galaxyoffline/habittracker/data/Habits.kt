package com.galaxyoffline.habittracker.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "✅",
    /** Haftanın günleri bit maskesi: Pazartesi = bit 0 … Pazar = bit 6. 127 = her gün. */
    @ColumnInfo(name = "days_mask") val daysMask: Int = 127,
    /** Günün dakikası (ör. 20:30 → 1230); null = hatırlatma yok. */
    @ColumnInfo(name = "remind_minute") val remindMinute: Int? = null,
    val archived: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_day") val createdDay: Long,
) {
    fun scheduledOn(d: LocalDate) = HabitLogic.scheduled(daysMask, d.dayOfWeek)
}

@Entity(tableName = "checks", primaryKeys = ["habit_id", "day"],
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habit_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("day")])
data class Check(@ColumnInfo(name = "habit_id") val habitId: Long, val day: Long)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY archived, sort_order, id") fun observeHabits(): Flow<List<Habit>>
    @Query("SELECT * FROM habits WHERE archived = 0") suspend fun activeHabits(): List<Habit>
    @Query("SELECT * FROM habits WHERE id = :id") suspend fun habit(id: Long): Habit?
    @Query("SELECT * FROM checks WHERE day >= :fromDay") fun observeChecks(fromDay: Long): Flow<List<Check>>
    @Query("SELECT day FROM checks WHERE habit_id = :id ORDER BY day") suspend fun days(id: Long): List<Long>
    @Query("SELECT COUNT(*) FROM checks WHERE habit_id = :id AND day = :day") suspend fun isChecked(id: Long, day: Long): Int
    @Upsert suspend fun upsert(h: Habit): Long
    @Query("DELETE FROM habits WHERE id = :id") suspend fun delete(id: Long)
    @Upsert suspend fun check(c: Check)
    @Query("DELETE FROM checks WHERE habit_id = :id AND day = :day") suspend fun uncheck(id: Long, day: Long)
}

@Database(entities = [Habit::class, Check::class], version = 1, exportSchema = true)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habits(): HabitDao
    companion object {
        @Volatile private var instance: HabitDatabase? = null
        fun get(c: Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, HabitDatabase::class.java, "aliskanliklar.db").build().also { instance = it }
        }
    }
}

data class HabitStats(val current: Int, val best: Int, val total: Int, val rate30: Int)

/** Saf seri/istatistik hesapları (birim testli). */
object HabitLogic {
    fun scheduled(mask: Int, d: DayOfWeek) = mask and (1 shl (d.value - 1)) != 0

    /**
     * Seri: planlı günler üzerinden sayılır; planlı olmayan günler seriyi bozmaz.
     * Bugün henüz işaretlenmediyse seri dünden itibaren hesaplanır (gün bitmeden seri bozulmaz).
     */
    fun stats(h: Habit, checked: Set<Long>, today: LocalDate): HabitStats {
        val start = LocalDate.ofEpochDay(minOf(h.createdDay, checked.minOrNull() ?: h.createdDay))
        var current = 0
        var d = today
        if (h.scheduledOn(today) && today.toEpochDay() !in checked) d = today.minusDays(1)
        while (!d.isBefore(start)) {
            val done = d.toEpochDay() in checked
            if (h.scheduledOn(d)) { if (done) current++ else break } else if (done) current++
            d = d.minusDays(1)
        }
        var best = 0; var run = 0; d = start
        while (!d.isAfter(today)) {
            val done = d.toEpochDay() in checked
            if (h.scheduledOn(d)) { if (done) { run++; best = maxOf(best, run) } else if (d != today) run = 0 }
            else if (done) { run++; best = maxOf(best, run) }
            d = d.plusDays(1)
        }
        val from = maxOf(start, today.minusDays(29))
        var planned = 0; var done = 0; d = from
        while (!d.isAfter(today)) {
            if (h.scheduledOn(d)) { planned++; if (d.toEpochDay() in checked) done++ }
            d = d.plusDays(1)
        }
        return HabitStats(current, maxOf(best, current), checked.size, if (planned == 0) 0 else done * 100 / planned)
    }

    fun daysLabel(mask: Int): String = when (mask) {
        127 -> "Her gün"; 31 -> "Hafta içi"; 96 -> "Hafta sonu"
        else -> DayOfWeek.entries.filter { scheduled(mask, it) }.joinToString(" ") { SHORT[it.value - 1] }
    }
    val SHORT = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
}
