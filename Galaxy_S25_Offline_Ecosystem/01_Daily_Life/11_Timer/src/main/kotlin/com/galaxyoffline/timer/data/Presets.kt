package com.galaxyoffline.timer.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "presets")
data class Preset(@PrimaryKey(autoGenerate = true) val id: Long = 0, val label: String, val seconds: Long)

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY seconds") fun observe(): Flow<List<Preset>>
    @Upsert suspend fun upsert(p: Preset): Long
    @Query("DELETE FROM presets WHERE id = :id") suspend fun delete(id: Long)
}

@Database(entities = [Preset::class], version = 1, exportSchema = true)
abstract class PresetDatabase : RoomDatabase() {
    abstract fun presets(): PresetDao
    companion object {
        @Volatile private var instance: PresetDatabase? = null
        fun get(c: Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, PresetDatabase::class.java, "zamanlayici.db").build().also { instance = it }
        }
    }
}
