package com.galaxyoffline.shoppinglist.data

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
import com.galaxyoffline.core.common.TrFormat
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(entity = ShoppingList::class, parentColumns = ["id"], childColumns = ["list_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("list_id")],
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "list_id") val listId: Long,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "adet",
    val category: String = Category.OTHER.name,
    /** Tahmini birim fiyat (TL); 0 = bilinmiyor. */
    val price: Double = 0.0,
    val checked: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Daha önce eklenen ürünler: öneri ve otomatik kategori için. */
@Entity(tableName = "history")
data class ItemHistory(
    @PrimaryKey val key: String,
    val name: String,
    val category: String,
    val unit: String,
    val price: Double,
    @ColumnInfo(name = "use_count") val useCount: Int,
    @ColumnInfo(name = "last_used") val lastUsed: Long,
)

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM lists ORDER BY created_at DESC") fun observeLists(): Flow<List<ShoppingList>>
    @Query("SELECT * FROM items") fun observeItems(): Flow<List<Item>>
    @Query("SELECT * FROM history ORDER BY use_count DESC, last_used DESC LIMIT 300") fun observeHistory(): Flow<List<ItemHistory>>
    @Insert suspend fun insertList(l: ShoppingList): Long
    @Update suspend fun updateList(l: ShoppingList)
    @Query("DELETE FROM lists WHERE id = :id") suspend fun deleteList(id: Long)
    @Upsert suspend fun upsertItem(i: Item)
    @Query("DELETE FROM items WHERE id = :id") suspend fun deleteItem(id: Long)
    @Query("DELETE FROM items WHERE list_id = :listId AND checked = 1") suspend fun clearChecked(listId: Long): Int
    @Query("UPDATE items SET checked = 0 WHERE list_id = :listId") suspend fun uncheckAll(listId: Long)
    @Query("SELECT * FROM history WHERE `key` = :key") suspend fun history(key: String): ItemHistory?
    @Upsert suspend fun upsertHistory(h: ItemHistory)
    @Query("DELETE FROM history") suspend fun clearHistory()
}

@Database(entities = [ShoppingList::class, Item::class, ItemHistory::class], version = 1, exportSchema = true)
abstract class ShoppingDatabase : RoomDatabase() {
    abstract fun shopping(): ShoppingDao

    companion object {
        @Volatile private var instance: ShoppingDatabase? = null
        fun get(context: Context): ShoppingDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ShoppingDatabase::class.java, "alisveris.db")
                .build().also { instance = it }
        }
    }
}

enum class Category(val label: String, val emoji: String) {
    PRODUCE("Meyve & Sebze", "🥦"), DAIRY("Süt & Kahvaltılık", "🧀"), MEAT("Et, Tavuk & Balık", "🍗"),
    BAKERY("Fırın & Unlu Mamul", "🥖"), PANTRY("Temel Gıda", "🍚"), FROZEN("Dondurulmuş", "🧊"),
    DRINKS("İçecek", "🥤"), SNACKS("Atıştırmalık", "🍫"), CLEANING("Temizlik", "🧽"),
    PERSONAL("Kişisel Bakım", "🧴"), BABY("Bebek", "🍼"), HOME("Ev & Mutfak", "🏠"), OTHER("Diğer", "🛒");

    companion object { fun of(key: String?) = entries.firstOrNull { it.name == key } ?: OTHER }
}

val UNITS = listOf("adet", "kg", "g", "L", "ml", "paket", "kutu", "demet")

object ShoppingLogic {
    private val tr = java.util.Locale.forLanguageTag("tr-TR")
    fun key(name: String) = name.trim().lowercase(tr)

    fun lineTotal(i: Item) = if (i.price > 0) i.price * i.quantity else 0.0
    fun total(items: List<Item>) = items.sumOf { lineTotal(it) }
    fun remaining(items: List<Item>) = items.filter { !it.checked }.sumOf { lineTotal(it) }

    /** Alınmamışlar kategori sırasıyla gruplanır; alınanlar en altta ayrı grup. */
    fun group(items: List<Item>): List<Pair<Category?, List<Item>>> {
        val open = items.filter { !it.checked }.groupBy { Category.of(it.category) }.toSortedMap(compareBy { it.ordinal })
            .map { (c, l) -> c as Category? to l.sortedBy { it.name.lowercase(tr) } }
        val done = items.filter { it.checked }.sortedBy { it.name.lowercase(tr) }
        return if (done.isEmpty()) open else open + (null to done)
    }

    fun suggestions(history: List<ItemHistory>, query: String, exclude: Set<String>): List<ItemHistory> {
        val q = key(query)
        if (q.isEmpty()) return emptyList()
        return history.filter { it.key.contains(q) && it.key !in exclude }.sortedWith(compareBy<ItemHistory> { !it.key.startsWith(q) }
            .thenByDescending { it.useCount }).take(5)
    }

    fun quantityText(i: Item): String = "${TrFormat.number(i.quantity, 2)} ${i.unit}"

    fun shareText(list: ShoppingList, items: List<Item>): String = buildString {
        appendLine("🛒 ${list.name}")
        group(items).forEach { (c, l) ->
            appendLine()
            appendLine(c?.let { "${it.emoji} ${it.label}" } ?: "✅ Alınanlar")
            l.forEach { appendLine("${if (it.checked) "☑" else "☐"} ${it.name} – ${quantityText(it)}") }
        }
        val t = total(items)
        if (t > 0) { appendLine(); append("Tahmini toplam: ${TrFormat.money(t)}") }
    }
}
