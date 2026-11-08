package com.galaxyoffline.todo.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "lists")
data class TaskList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(entity = TaskList::class, parentColumns = ["id"], childColumns = ["list_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("list_id")],
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "list_id") val listId: Long,
    val title: String,
    val note: String = "",
    /** Son tarih (LocalDate.toEpochDay) veya null. */
    val due: Long? = null,
    /** 0 düşük, 1 normal, 2 yüksek */
    val priority: Int = 1,
    val done: Boolean = false,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    val dueDate: LocalDate? get() = due?.let { LocalDate.ofEpochDay(it) }
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM lists ORDER BY created_at, id")
    fun observeLists(): Flow<List<TaskList>>

    @Query("SELECT COUNT(*) FROM lists")
    suspend fun listCount(): Int

    @Insert suspend fun insertList(l: TaskList): Long
    @Update suspend fun updateList(l: TaskList)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun deleteList(id: Long)

    @Query("SELECT * FROM tasks")
    fun observeTasks(): Flow<List<Task>>

    @Upsert suspend fun upsertTask(t: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("DELETE FROM tasks WHERE done = 1 AND (:listId IS NULL OR list_id = :listId)")
    suspend fun clearDone(listId: Long?): Int
}

@Database(entities = [TaskList::class, Task::class], version = 1, exportSchema = true)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todo(): TodoDao

    companion object {
        @Volatile private var instance: TodoDatabase? = null
        fun get(context: Context): TodoDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, TodoDatabase::class.java, "gorevler.db")
                .build().also { instance = it }
        }
    }
}

enum class TaskFilter(val label: String) { TODAY("Bugün"), UPCOMING("Yaklaşan"), ALL("Tümü"), DONE("Tamamlanan") }
enum class TaskSort(val label: String) { DUE("Son tarih"), PRIORITY("Öncelik"), CREATED("Eklenme") }
enum class Priority(val value: Int, val label: String) { LOW(0, "Düşük"), NORMAL(1, "Normal"), HIGH(2, "Yüksek");
    companion object { fun of(v: Int) = entries.firstOrNull { it.value == v } ?: NORMAL } }

object TaskLogic {
    const val UPCOMING_DAYS = 7L

    fun isOverdue(t: Task, today: LocalDate) = !t.done && t.due != null && t.due < today.toEpochDay()

    fun filter(tasks: List<Task>, filter: TaskFilter, listId: Long?, today: LocalDate, sort: TaskSort): List<Task> {
        val t = today.toEpochDay()
        val base = tasks.filter { listId == null || it.listId == listId }
        val filtered = when (filter) {
            TaskFilter.TODAY -> base.filter { !it.done && it.due != null && it.due <= t }
            TaskFilter.UPCOMING -> base.filter { !it.done && it.due != null && it.due > t && it.due <= t + UPCOMING_DAYS }
            TaskFilter.ALL -> base.filter { !it.done }
            TaskFilter.DONE -> base.filter { it.done }
        }
        if (filter == TaskFilter.DONE) return filtered.sortedByDescending { it.completedAt ?: 0 }
        return when (sort) {
            TaskSort.DUE -> filtered.sortedWith(compareBy<Task> { it.due ?: Long.MAX_VALUE }.thenByDescending { it.priority }.thenBy { it.createdAt })
            TaskSort.PRIORITY -> filtered.sortedWith(compareByDescending<Task> { it.priority }.thenBy { it.due ?: Long.MAX_VALUE })
            TaskSort.CREATED -> filtered.sortedByDescending { it.createdAt }
        }
    }

    fun counts(tasks: List<Task>, listId: Long?, today: LocalDate) =
        TaskFilter.entries.associateWith { filter(tasks, it, listId, today, TaskSort.DUE).size }
}
