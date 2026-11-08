package com.galaxyoffline.qrcode.data

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

@Entity(tableName = "scans")
data class ScanEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val format: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY created_at DESC LIMIT 500") fun observe(): Flow<List<ScanEntry>>
    @Insert suspend fun insert(e: ScanEntry)
    @Query("DELETE FROM scans WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM scans") suspend fun clear()
}

@Database(entities = [ScanEntry::class], version = 1, exportSchema = true)
abstract class QrDatabase : RoomDatabase() {
    abstract fun scans(): ScanDao
    companion object {
        @Volatile private var instance: QrDatabase? = null
        fun get(c: Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, QrDatabase::class.java, "qr_gecmis.db").build().also { instance = it }
        }
    }
}
