package com.galaxyoffline.stopwatch.data

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
import kotlinx.coroutines.flow.Flow

/**
 * Kronometre durumu. Süre, monoton saat (elapsedRealtime) ile ölçülür; telefon yeniden başlatılırsa
 * (monoton saat sıfırlanır) duvar saati farkına geçilir. Böylece uygulama kapansa da süre kaybolmaz.
 */
data class StopwatchState(
    val running: Boolean = false,
    val startedElapsed: Long = 0,
    val startedWall: Long = 0,
    val accumulated: Long = 0,
    /** Her turun bittiği andaki toplam süre (ms). */
    val laps: List<Long> = emptyList(),
)

object StopwatchLogic {
    fun elapsed(s: StopwatchState, nowElapsed: Long, nowWall: Long): Long {
        if (!s.running) return s.accumulated
        val delta = if (nowElapsed >= s.startedElapsed) nowElapsed - s.startedElapsed else (nowWall - s.startedWall).coerceAtLeast(0)
        return s.accumulated + delta
    }

    fun start(s: StopwatchState, nowElapsed: Long, nowWall: Long) =
        if (s.running) s else s.copy(running = true, startedElapsed = nowElapsed, startedWall = nowWall)

    fun pause(s: StopwatchState, nowElapsed: Long, nowWall: Long) =
        if (!s.running) s else s.copy(running = false, accumulated = elapsed(s, nowElapsed, nowWall))

    fun lap(s: StopwatchState, nowElapsed: Long, nowWall: Long): StopwatchState {
        val t = elapsed(s, nowElapsed, nowWall)
        return if (t <= (s.laps.lastOrNull() ?: 0) || s.laps.size >= MAX_LAPS) s else s.copy(laps = s.laps + t)
    }

    /** Tur süreleri (her tur ayrı). */
    fun lapDurations(laps: List<Long>): List<Long> = laps.mapIndexed { i, t -> t - (if (i == 0) 0 else laps[i - 1]) }

    /** En hızlı ve en yavaş tur indeksleri (en az 2 tur varsa). */
    fun bestWorst(laps: List<Long>): Pair<Int, Int>? {
        val d = lapDurations(laps)
        if (d.size < 2) return null
        return d.indexOf(d.min()) to d.indexOf(d.max())
    }

    fun encodeLaps(laps: List<Long>) = laps.joinToString(",")
    fun decodeLaps(s: String?): List<Long> = s?.split(',')?.mapNotNull { it.trim().toLongOrNull() }.orEmpty()

    const val MAX_LAPS = 999
}

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "total_ms") val totalMs: Long,
    val laps: String,
    val label: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY created_at DESC") fun observeAll(): Flow<List<Session>>
    @Insert suspend fun insert(s: Session): Long
    @Query("DELETE FROM sessions WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM sessions") suspend fun clear()
}

@Database(entities = [Session::class], version = 1, exportSchema = true)
abstract class StopwatchDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao
    companion object {
        @Volatile private var instance: StopwatchDatabase? = null
        fun get(context: Context): StopwatchDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, StopwatchDatabase::class.java, "kronometre.db").build().also { instance = it }
        }
    }
}
