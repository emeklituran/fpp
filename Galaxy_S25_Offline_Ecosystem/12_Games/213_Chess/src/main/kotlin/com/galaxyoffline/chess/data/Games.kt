package com.galaxyoffline.chess.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

enum class GameMode(val label: String) { ANALYSIS("Analiz"), VS_COMPUTER("Bilgisayara karşı"), TWO_PLAYER("İki kişilik") }

/** Kayıtlı oyun: başlangıç FEN'i + UCI hamleleri. Tam oyun durumu bunlardan yeniden kurulur (tekrar geçmişi dahil). */
@Entity(tableName = "games")
data class SavedGame(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "start_fen") val startFen: String,
    /** Boşlukla ayrılmış UCI hamleleri. */
    val moves: String,
    val mode: String,
    val result: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    val moveList get() = moves.split(' ').filter { it.isNotBlank() }
}

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY updated_at DESC") fun observe(): Flow<List<SavedGame>>
    @Query("SELECT * FROM games WHERE id = :id") suspend fun get(id: Long): SavedGame?
    @Upsert suspend fun upsert(g: SavedGame): Long
    @Query("DELETE FROM games WHERE id = :id") suspend fun delete(id: Long)
}

@Database(entities = [SavedGame::class], version = 1, exportSchema = true)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun games(): GameDao
    companion object {
        @Volatile private var instance: ChessDatabase? = null
        fun get(c: Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, ChessDatabase::class.java, "satranc.db").build().also { instance = it }
        }
    }
}

/** Süren oyun (uygulama kapanıp açılınca kaldığı yerden devam). */
data class ActiveGame(
    val startFen: String, val moves: List<String>, val cursor: Int, val mode: GameMode, val userIsWhite: Boolean,
    val level: Int, val flipped: Boolean, val analysisOn: Boolean, val claimedDraw: String?, val savedId: Long?,
)

class ActiveGameStore(context: Context) {
    private val p = context.getSharedPreferences("aktif_oyun", Context.MODE_PRIVATE)
    fun load(): ActiveGame? {
        val fen = p.getString("fen", null) ?: return null
        return ActiveGame(
            fen, p.getString("moves", "")!!.split(' ').filter { it.isNotBlank() }, p.getInt("cursor", 0),
            runCatching { GameMode.valueOf(p.getString("mode", "")!!) }.getOrDefault(GameMode.ANALYSIS),
            p.getBoolean("white", true), p.getInt("level", 4), p.getBoolean("flipped", false), p.getBoolean("analysis", true),
            p.getString("claimed", null), p.getLong("saved", -1).takeIf { it > 0 },
        )
    }
    fun save(a: ActiveGame) = p.edit().putString("fen", a.startFen).putString("moves", a.moves.joinToString(" ")).putInt("cursor", a.cursor)
        .putString("mode", a.mode.name).putBoolean("white", a.userIsWhite).putInt("level", a.level).putBoolean("flipped", a.flipped)
        .putBoolean("analysis", a.analysisOn).putString("claimed", a.claimedDraw).putLong("saved", a.savedId ?: -1).apply()
}

/** Bilgisayar seviyeleri: Stockfish UCI_Elo (en az 1320) ve hamle başı düşünme süresi. Son seviye: tam güç. */
data class Level(val label: String, val elo: Int?, val moveMs: Long)
val LEVELS = listOf(
    Level("1 · Başlangıç (≈1320)", 1320, 300), Level("2 · Kolay (≈1500)", 1500, 400), Level("3 · Orta (≈1700)", 1700, 500),
    Level("4 · Kulüp (≈1900)", 1900, 600), Level("5 · Güçlü (≈2100)", 2100, 800), Level("6 · Uzman (≈2400)", 2400, 1000),
    Level("7 · Usta (≈2700)", 2700, 1200), Level("8 · Tam güç", null, 1500),
)
