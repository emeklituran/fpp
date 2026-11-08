package com.galaxyoffline.voicerecorder.data

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
import java.io.File

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Uygulama içi depodaki dosya adı (files/kayitlar/). */
    @ColumnInfo(name = "file_name") val fileName: String,
    val title: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "bitrate") val bitrate: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY created_at DESC") fun observe(): Flow<List<Recording>>
    @Query("SELECT * FROM recordings") suspend fun all(): List<Recording>
    @Upsert suspend fun upsert(r: Recording): Long
    @Query("DELETE FROM recordings WHERE id = :id") suspend fun delete(id: Long)
}

@Database(entities = [Recording::class], version = 1, exportSchema = true)
abstract class RecordingDatabase : RoomDatabase() {
    abstract fun recordings(): RecordingDao
    companion object {
        @Volatile private var instance: RecordingDatabase? = null
        fun get(c: Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, RecordingDatabase::class.java, "kayitlar.db").build().also { instance = it }
        }
    }
}

object RecFiles {
    fun dir(c: Context) = File(c.filesDir, "kayitlar").apply { mkdirs() }
    fun file(c: Context, r: Recording) = File(dir(c), r.fileName)

    /** Dosya adına uygun olmayan karakterleri temizler. */
    fun safeName(title: String): String = title.trim().replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").take(80).ifBlank { "Kayıt" }

    fun duration(ms: Long): String {
        val s = ms / 1000
        return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%02d:%02d".format(s / 60, s % 60)
    }

    fun size(bytes: Long): String = when {
        bytes >= 1_048_576 -> "%.1f MB".format(java.util.Locale.forLanguageTag("tr"), bytes / 1_048_576.0)
        else -> "${(bytes + 1023) / 1024} KB"
    }
}
