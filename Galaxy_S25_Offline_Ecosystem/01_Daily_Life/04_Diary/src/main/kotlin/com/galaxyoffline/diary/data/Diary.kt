package com.galaxyoffline.diary.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "entries", indices = [Index("date")])
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** LocalDate.toEpochDay() */
    val date: Long,
    val title: String,
    val content: String,
    /** 1 (çok kötü) – 5 (harika) */
    val mood: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    val localDate: LocalDate get() = LocalDate.ofEpochDay(date)
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM entries ORDER BY date DESC, created_at DESC")
    fun observeAll(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM entries ORDER BY date ASC, created_at ASC")
    suspend fun all(): List<DiaryEntry>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: Long): DiaryEntry?

    @Insert suspend fun insert(e: DiaryEntry): Long
    @Update suspend fun update(e: DiaryEntry)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [DiaryEntry::class], version = 1, exportSchema = true)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diary(): DiaryDao

    companion object {
        @Volatile private var instance: DiaryDatabase? = null
        fun get(context: Context): DiaryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, DiaryDatabase::class.java, "gunluk.db")
                .build().also { instance = it }
        }
    }
}

enum class Mood(val value: Int, val emoji: String, val label: String) {
    AWFUL(1, "😞", "Çok kötü"), BAD(2, "🙁", "Kötü"), OKAY(3, "😐", "Normal"), GOOD(4, "🙂", "İyi"), GREAT(5, "😄", "Harika");

    companion object {
        fun of(v: Int) = entries.firstOrNull { it.value == v } ?: OKAY
    }
}

data class DiaryStats(
    val total: Int,
    val thisMonth: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val moodCounts: Map<Mood, Int>,
    val averageMood: Double?,
)

object DiaryStatsCalculator {
    /** Seri: en az bir kayıt bulunan ardışık günler. Bugün yazılmadıysa dünden geriye sayılır. */
    fun compute(entries: List<DiaryEntry>, today: LocalDate = LocalDate.now()): DiaryStats {
        val days = entries.map { it.date }.toSortedSet()
        var current = 0
        var cursor = if (today.toEpochDay() in days) today.toEpochDay() else today.toEpochDay() - 1
        while (cursor in days) { current++; cursor-- }
        var longest = 0
        var run = 0
        var prev: Long? = null
        for (d in days) {
            run = if (prev != null && d == prev + 1) run + 1 else 1
            longest = maxOf(longest, run)
            prev = d
        }
        val counts = Mood.entries.associateWith { m -> entries.count { it.mood == m.value } }
        return DiaryStats(
            total = entries.size,
            thisMonth = entries.count { val d = it.localDate; d.year == today.year && d.month == today.month },
            currentStreak = current,
            longestStreak = longest,
            moodCounts = counts,
            averageMood = if (entries.isEmpty()) null else entries.map { it.mood }.average(),
        )
    }

    fun search(entries: List<DiaryEntry>, query: String): List<DiaryEntry> {
        val tr = java.util.Locale.forLanguageTag("tr-TR")
        val q = query.trim().lowercase(tr)
        if (q.isEmpty()) return entries
        return entries.filter { it.title.lowercase(tr).contains(q) || it.content.lowercase(tr).contains(q) }
    }
}
