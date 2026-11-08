package com.galaxyoffline.reminder.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

enum class Repeat(val label: String) {
    NONE("Tekrar yok"), DAILY("Her gün"), WEEKLY("Her hafta"), MONTHLY("Her ay"), YEARLY("Her yıl");
    companion object { fun of(k: String?) = entries.firstOrNull { it.name == k } ?: NONE }
}

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    /** İlk planlanan an (tekrarlar bundan hesaplanır; ay sonu kaymasını önler). */
    @ColumnInfo(name = "anchor_at") val anchorAt: Long,
    /** Bir sonraki tetiklenme anı (epoch ms). */
    @ColumnInfo(name = "trigger_at") val triggerAt: Long,
    val repeat: String = Repeat.NONE.name,
    val enabled: Boolean = true,
    /** Tek seferlik hatırlatma tetiklendi veya tamamlandı. */
    val done: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    val repeatRule: Repeat get() = Repeat.of(repeat)
    fun triggerDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime = Instant.ofEpochMilli(triggerAt).atZone(zone).toLocalDateTime()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY trigger_at") fun observeAll(): Flow<List<Reminder>>
    @Query("SELECT * FROM reminders") suspend fun all(): List<Reminder>
    @Query("SELECT * FROM reminders WHERE id = :id") suspend fun get(id: Long): Reminder?
    @Insert suspend fun insert(r: Reminder): Long
    @Update suspend fun update(r: Reminder)
    @Query("DELETE FROM reminders WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM reminders WHERE done = 1") suspend fun clearDone(): Int
}

@Database(entities = [Reminder::class], version = 1, exportSchema = true)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminders(): ReminderDao
    companion object {
        @Volatile private var instance: ReminderDatabase? = null
        fun get(context: Context): ReminderDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ReminderDatabase::class.java, "hatirlatmalar.db")
                .build().also { instance = it }
        }
    }
}

/** Tekrar kuralları (saf, test edilebilir). */
object RepeatLogic {
    fun advance(anchor: LocalDateTime, repeat: Repeat, steps: Long): LocalDateTime = when (repeat) {
        Repeat.NONE -> anchor
        Repeat.DAILY -> anchor.plusDays(steps)
        Repeat.WEEKLY -> anchor.plusWeeks(steps)
        Repeat.MONTHLY -> anchor.plusMonths(steps)
        Repeat.YEARLY -> anchor.plusYears(steps)
    }

    /** [now]'dan sonraki ilk tekrar. Tekrarsız kurallarda null. */
    fun nextAfter(anchor: LocalDateTime, repeat: Repeat, now: LocalDateTime): LocalDateTime? {
        if (repeat == Repeat.NONE) return if (anchor.isAfter(now)) anchor else null
        if (anchor.isAfter(now)) return anchor
        // Kaba tahminle başlayıp ileri sarılır (uzun aralıklarda bile hızlı).
        val approxDays = java.time.Duration.between(anchor, now).toDays()
        var steps = when (repeat) {
            Repeat.DAILY -> approxDays
            Repeat.WEEKLY -> approxDays / 7
            Repeat.MONTHLY -> approxDays / 31
            Repeat.YEARLY -> approxDays / 366
            Repeat.NONE -> 0
        }.coerceAtLeast(0)
        var candidate = advance(anchor, repeat, steps)
        while (!candidate.isAfter(now)) { steps++; candidate = advance(anchor, repeat, steps) }
        return candidate
    }
}
