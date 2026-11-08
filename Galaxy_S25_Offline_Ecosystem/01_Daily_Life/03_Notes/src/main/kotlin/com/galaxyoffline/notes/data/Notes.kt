package com.galaxyoffline.notes.data

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

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    /** [NoteColors] dizinindeki renk (0 = varsayılan). */
    val color: Int = 0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    /** Çöp kutusuna taşındığı an; null ise not etkin. */
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updated_at DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteForever(id: Long)

    @Query("DELETE FROM notes WHERE deleted_at IS NOT NULL")
    suspend fun emptyTrash()

    @Query("DELETE FROM notes WHERE deleted_at IS NOT NULL AND deleted_at < :before")
    suspend fun purgeTrash(before: Long): Int
}

@Database(entities = [Note::class], version = 1, exportSchema = true)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notes(): NoteDao

    companion object {
        @Volatile private var instance: NotesDatabase? = null
        fun get(context: Context): NotesDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, NotesDatabase::class.java, "notlar.db")
                .build().also { instance = it }
        }
    }
}

/** Not filtreleme/sıralama mantığı (saf, test edilebilir). */
enum class NoteFilter(val label: String) { NOTES("Notlar"), ARCHIVE("Arşiv"), TRASH("Çöp kutusu") }

enum class NoteSort(val label: String) { UPDATED("Son düzenleme"), CREATED("Oluşturma tarihi"), TITLE("Başlık (A-Z)") }

object NoteQuery {
    const val TRASH_DAYS = 30L
    private val tr = java.util.Locale.forLanguageTag("tr-TR")

    fun apply(all: List<Note>, filter: NoteFilter, query: String, sort: NoteSort): List<Note> {
        val q = query.trim().lowercase(tr)
        val filtered = all.filter {
            when (filter) {
                NoteFilter.NOTES -> it.deletedAt == null && !it.archived
                NoteFilter.ARCHIVE -> it.deletedAt == null && it.archived
                NoteFilter.TRASH -> it.deletedAt != null
            }
        }.filter { q.isEmpty() || it.title.lowercase(tr).contains(q) || it.content.lowercase(tr).contains(q) }
        val sorted = when (sort) {
            NoteSort.UPDATED -> filtered.sortedByDescending { it.updatedAt }
            NoteSort.CREATED -> filtered.sortedByDescending { it.createdAt }
            NoteSort.TITLE -> filtered.sortedBy { it.title.ifBlank { it.content }.lowercase(tr) }
        }
        // Sabitlenen notlar her zaman üstte (çöp kutusu hariç).
        return if (filter == NoteFilter.TRASH) sorted else sorted.sortedByDescending { it.pinned }
    }
}
