package com.galaxyoffline.authenticator.data

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
import com.galaxyoffline.core.crypto.OtpAccount
import com.galaxyoffline.core.crypto.OtpAuthUri
import com.galaxyoffline.core.security.KeystoreKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Hesap bilgileri (gizli anahtar dahil) Android Keystore anahtarıyla şifrelenmiş otpauth URI'si olarak saklanır. */
@Entity(tableName = "accounts")
data class StoredAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val blob: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY sort_order, id") suspend fun all(): List<StoredAccount>
    @Upsert suspend fun upsert(a: StoredAccount): Long
    @Query("DELETE FROM accounts WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM accounts") suspend fun maxOrder(): Int
}

@Database(entities = [StoredAccount::class], version = 1, exportSchema = true)
abstract class AuthDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    companion object {
        @Volatile private var instance: AuthDatabase? = null
        fun get(c: Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, AuthDatabase::class.java, "dogrulayici.db").build().also { instance = it }
        }
    }
}

data class Entry(val id: Long, val account: OtpAccount, val sortOrder: Int, val createdAt: Long)

class AccountRepository(private val dao: AccountDao) {
    private fun seal(a: OtpAccount) = CryptoBox.b64(KeystoreKeys.seal(ALIAS, OtpAuthUri.build(a).toByteArray()))
    private fun open(blob: String) = OtpAuthUri.parse(String(KeystoreKeys.open(ALIAS, CryptoBox.unb64(blob))))

    suspend fun all(): List<Entry> = dao.all().mapNotNull { s -> runCatching { Entry(s.id, open(s.blob), s.sortOrder, s.createdAt) }.getOrNull() }
    suspend fun add(a: OtpAccount) = dao.upsert(StoredAccount(blob = seal(a), sortOrder = dao.maxOrder() + 1, createdAt = System.currentTimeMillis()))
    suspend fun update(e: Entry) = dao.upsert(StoredAccount(e.id, seal(e.account), e.sortOrder, e.createdAt))
    suspend fun delete(id: Long) = dao.delete(id)

    companion object {
        const val ALIAS = "dogrulayici_hesaplar"

        fun exportJson(list: List<OtpAccount>, password: CharArray): String {
            val salt = CryptoBox.randomBytes(16)
            val key = CryptoBox.deriveKey(password, salt)
            val data = CryptoBox.encrypt(key, JSONArray(list.map { OtpAuthUri.build(it) }).toString().toByteArray())
            CryptoBox.wipe(key)
            return JSONObject().put("format", "galaxy-dogrulayici-yedek").put("version", 1).put("iterations", CryptoBox.DEFAULT_ITERATIONS)
                .put("salt", CryptoBox.b64(salt)).put("data", CryptoBox.b64(data)).toString(2)
        }

        fun importJson(text: String, password: CharArray): List<OtpAccount> {
            val o = JSONObject(text)
            require(o.optString("format") == "galaxy-dogrulayici-yedek") { "Bu dosya bir Doğrulayıcı yedeği değil" }
            val key = CryptoBox.deriveKey(password, CryptoBox.unb64(o.getString("salt")), o.optInt("iterations", CryptoBox.DEFAULT_ITERATIONS))
            try {
                val arr = JSONArray(String(CryptoBox.decrypt(key, CryptoBox.unb64(o.getString("data")))))
                return (0 until arr.length()).map { OtpAuthUri.parse(arr.getString(it)) }
            } finally { CryptoBox.wipe(key) }
        }
    }
}

/** İsteğe bağlı uygulama kilidi durumu (biyometri / cihaz ekran kilidi). */
object LockState {
    @Volatile var enabled = false
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()
    fun lockIfEnabled() { if (enabled) _locked.value = true }
    fun unlock() { _locked.value = false }
}
