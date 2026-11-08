package com.galaxyoffline.core.calculator

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

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Düzenlemeye geri yüklenebilen iç ifade. */
    @ColumnInfo(name = "raw_expression") val rawExpression: String,
    /** Ekranda gösterilen ifade ve sonuç. */
    val expression: String,
    val result: String,
    val value: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY created_at DESC, id DESC")
    fun observe(): Flow<List<HistoryEntry>>

    @Insert
    suspend fun insert(entry: HistoryEntry)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM history")
    suspend fun clear()

    /** En yeni [keep] kayıt dışındakileri siler (geçmiş sınırsız büyümez). */
    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY created_at DESC, id DESC LIMIT :keep)")
    suspend fun trim(keep: Int)
}

@Database(entities = [HistoryEntry::class], version = 1, exportSchema = true)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun history(): HistoryDao

    companion object {
        @Volatile private var instance: HistoryDatabase? = null
        fun get(context: Context): HistoryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, HistoryDatabase::class.java, "hesap_gecmisi.db")
                .build().also { instance = it }
        }
    }
}
