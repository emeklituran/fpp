package com.galaxyoffline.alarm.data

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
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    /** Haftanın günleri bit maskesi: bit0 = Pazartesi … bit6 = Pazar. 0 = bir kez. */
    val days: Int = 0,
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    @ColumnInfo(name = "snooze_minutes") val snoozeMinutes: Int = 10,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    val time: LocalTime get() = LocalTime.of(hour, minute)
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute") fun observeAll(): Flow<List<Alarm>>
    @Query("SELECT * FROM alarms") suspend fun all(): List<Alarm>
    @Query("SELECT * FROM alarms WHERE id = :id") suspend fun get(id: Long): Alarm?
    @Insert suspend fun insert(a: Alarm): Long
    @Update suspend fun update(a: Alarm)
    @Query("DELETE FROM alarms WHERE id = :id") suspend fun delete(id: Long)
}

@Database(entities = [Alarm::class], version = 1, exportSchema = true)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarms(): AlarmDao
    companion object {
        @Volatile private var instance: AlarmDatabase? = null
        fun get(context: Context): AlarmDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AlarmDatabase::class.java, "alarmlar.db").build().also { instance = it }
        }
    }
}

object AlarmLogic {
    val DAY_SHORT = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    const val WEEKDAYS = 0b0011111
    const val WEEKEND = 0b1100000
    const val EVERY_DAY = 0b1111111

    fun hasDay(mask: Int, isoDayOfWeek: Int) = mask and (1 shl (isoDayOfWeek - 1)) != 0
    fun toggle(mask: Int, index: Int) = mask xor (1 shl index)

    /** [now]'dan sonraki ilk çalma anı (saniye 0). */
    fun nextTrigger(a: Alarm, now: LocalDateTime): LocalDateTime {
        val todayAt = now.toLocalDate().atTime(a.hour, a.minute)
        if (a.days == 0) return if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
        for (i in 0..7) {
            val candidate = todayAt.plusDays(i.toLong())
            if (candidate.isAfter(now) && hasDay(a.days, candidate.dayOfWeek.value)) return candidate
        }
        return todayAt.plusDays(7) // ulaşılmaz; maske boş değilse döngü bulur
    }

    fun describeDays(mask: Int): String = when (mask) {
        0 -> "Bir kez"
        EVERY_DAY -> "Her gün"
        WEEKDAYS -> "Hafta içi"
        WEEKEND -> "Hafta sonu"
        else -> DAY_SHORT.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.joinToString(", ")
    }

    /** "8 saat 5 dakika sonra" */
    fun untilText(now: LocalDateTime, at: LocalDateTime): String {
        val minutes = java.time.Duration.between(now, at).toMinutes().coerceAtLeast(0) + 1
        val d = minutes / (60 * 24); val h = (minutes / 60) % 24; val m = minutes % 60
        val parts = buildList {
            if (d > 0) add("$d gün"); if (h > 0) add("$h saat"); if (m > 0 && d == 0L) add("$m dakika")
        }
        return (if (parts.isEmpty()) "1 dakika" else parts.joinToString(" ")) + " sonra"
    }
}
