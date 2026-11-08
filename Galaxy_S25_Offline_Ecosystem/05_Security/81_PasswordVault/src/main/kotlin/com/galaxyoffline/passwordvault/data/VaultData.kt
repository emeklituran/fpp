package com.galaxyoffline.passwordvault.data

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
import com.galaxyoffline.core.crypto.CryptoBox
import org.json.JSONArray
import org.json.JSONObject

/** Veritabanında yalnızca şifreli içerik ve zaman damgaları bulunur; başlık dahil tüm alanlar şifrelidir. */
@Entity(tableName = "entries")
data class EncryptedEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blob: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries") suspend fun all(): List<EncryptedEntry>
    @Query("SELECT * FROM entries WHERE id = :id") suspend fun get(id: Long): EncryptedEntry?
    @Upsert suspend fun upsert(e: EncryptedEntry): Long
    @Query("DELETE FROM entries WHERE id = :id") suspend fun delete(id: Long)
}

@Database(entities = [EncryptedEntry::class], version = 1, exportSchema = true)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun entries(): EntryDao
    companion object {
        @Volatile private var instance: VaultDatabase? = null
        fun get(c: Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, VaultDatabase::class.java, "kasa.db").build().also { instance = it }
        }
    }
}

enum class ItemType(val label: String, val userLabel: String, val secretLabel: String, val extraLabel: String?) {
    LOGIN("Giriş", "Kullanıcı adı / e-posta", "Şifre", "Web sitesi"),
    CARD("Banka kartı", "Kart üzerindeki ad", "Kart numarası", "Son kullanma (AA/YY)"),
    WIFI("Wi-Fi", "Ağ adı (SSID)", "Wi-Fi şifresi", null),
    NOTE("Güvenli not", "", "", null),
    OTHER("Diğer", "Kullanıcı / kimlik", "Gizli bilgi", "Ek bilgi");
    companion object { fun of(s: String?) = entries.firstOrNull { it.name == s } ?: OTHER }
}

data class VaultItem(
    val id: Long = 0,
    val type: ItemType = ItemType.LOGIN,
    val title: String = "",
    val username: String = "",
    val secret: String = "",
    val extra: String = "",
    /** Kartlarda CVV gibi ikinci gizli bilgi. */
    val pin: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    fun toJson(): JSONObject = JSONObject().put("type", type.name).put("title", title).put("username", username).put("secret", secret)
        .put("extra", extra).put("pin", pin).put("notes", notes).put("favorite", favorite)

    companion object {
        fun fromJson(o: JSONObject, id: Long = 0, created: Long = 0, updated: Long = 0) = VaultItem(
            id, ItemType.of(o.optString("type")), o.optString("title"), o.optString("username"), o.optString("secret"),
            o.optString("extra"), o.optString("pin"), o.optString("notes"), o.optBoolean("favorite"), created, updated)
    }
}

class VaultRepository(private val dao: EntryDao) {
    suspend fun loadAll(dek: ByteArray): List<VaultItem> = dao.all().mapNotNull { e ->
        runCatching { VaultItem.fromJson(JSONObject(CryptoBox.decryptString(dek, e.blob)), e.id, e.createdAt, e.updatedAt) }.getOrNull()
    }

    suspend fun save(dek: ByteArray, item: VaultItem): VaultItem {
        val now = System.currentTimeMillis()
        val created = if (item.createdAt == 0L) now else item.createdAt
        val id = dao.upsert(EncryptedEntry(item.id, CryptoBox.encryptString(dek, item.toJson().toString()), created, now))
        return item.copy(id = if (item.id == 0L) id else item.id, createdAt = created, updatedAt = now)
    }

    suspend fun delete(id: Long) = dao.delete(id)

    companion object {
        /** Şifreli yedek: ana şifreden (veya yedek parolasından) bağımsız tuzla türetilen anahtar. */
        fun exportJson(items: List<VaultItem>, password: CharArray): String {
            val salt = CryptoBox.randomBytes(16)
            val key = CryptoBox.deriveKey(password, salt)
            val plain = JSONArray(items.map { it.toJson() }).toString()
            val data = CryptoBox.encrypt(key, plain.toByteArray(Charsets.UTF_8))
            CryptoBox.wipe(key)
            return JSONObject().put("format", "galaxy-kasa-yedek").put("version", 1).put("kdf", "PBKDF2-HMAC-SHA256")
                .put("iterations", CryptoBox.DEFAULT_ITERATIONS).put("salt", CryptoBox.b64(salt)).put("cipher", "AES-256-GCM")
                .put("data", CryptoBox.b64(data)).toString(2)
        }

        /** Yanlış parolada AEADBadTagException; biçim hatasında IllegalArgumentException. */
        fun importJson(text: String, password: CharArray): List<VaultItem> {
            val o = JSONObject(text)
            require(o.optString("format") == "galaxy-kasa-yedek") { "Bu dosya bir Şifre Kasası yedeği değil" }
            val key = CryptoBox.deriveKey(password, CryptoBox.unb64(o.getString("salt")), o.optInt("iterations", CryptoBox.DEFAULT_ITERATIONS))
            try {
                val arr = JSONArray(String(CryptoBox.decrypt(key, CryptoBox.unb64(o.getString("data"))), Charsets.UTF_8))
                return (0 until arr.length()).map { VaultItem.fromJson(arr.getJSONObject(it)) }
            } finally { CryptoBox.wipe(key) }
        }
    }
}
