package com.galaxyoffline.pdfreader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.Closeable

@Entity(tableName = "recents")
data class Recent(
    @PrimaryKey val uri: String,
    val name: String,
    val pages: Int,
    @ColumnInfo(name = "last_page") val lastPage: Int,
    @ColumnInfo(name = "opened_at") val openedAt: Long,
)

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY opened_at DESC LIMIT 30") fun observe(): Flow<List<Recent>>
    @Query("SELECT * FROM recents WHERE uri = :uri") suspend fun get(uri: String): Recent?
    @Upsert suspend fun upsert(r: Recent)
    @Query("UPDATE recents SET last_page = :page WHERE uri = :uri") suspend fun setPage(uri: String, page: Int)
    @Query("DELETE FROM recents WHERE uri = :uri") suspend fun delete(uri: String)
}

@Database(entities = [Recent::class], version = 1, exportSchema = true)
abstract class PdfDatabase : RoomDatabase() {
    abstract fun recents(): RecentDao
    companion object {
        @Volatile private var instance: PdfDatabase? = null
        fun get(c: Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, PdfDatabase::class.java, "pdf_son.db").build().also { instance = it }
        }
    }
}

/**
 * Android'in yerleşik PdfRenderer'ı üzerinde güvenli sarmalayıcı. PdfRenderer aynı anda tek sayfa açabildiği
 * için tüm işlemler tek iş parçacıklı bir dağıtıcıda sıraya alınır. Sayfalar küçük bir bellek önbelleğinde tutulur.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PdfDocument private constructor(private val fd: ParcelFileDescriptor, private val renderer: PdfRenderer) : Closeable {
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)
    private val cache = object : LruCache<String, Bitmap>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    val pageCount: Int = renderer.pageCount
    /** Sayfa en-boy oranları (yükseklik / genişlik). */
    val ratios: List<Float> = (0 until pageCount).map { i -> renderer.openPage(i).use { it.height.toFloat() / it.width } }
    @Volatile private var closed = false

    suspend fun render(index: Int, widthPx: Int): Bitmap? = withContext(dispatcher) {
        if (closed || index !in 0 until pageCount || widthPx <= 0) return@withContext null
        val w = widthPx.coerceAtMost(3000)
        val key = "$index@$w"
        cache.get(key)?.let { return@withContext it }
        runCatching {
            renderer.openPage(index).use { page ->
                val h = (w * page.height.toFloat() / page.width).toInt().coerceIn(1, 6000)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bmp
            }
        }.getOrNull()?.also { cache.put(key, it) }
    }

    override fun close() {
        closed = true
        runCatching { renderer.close() }
        runCatching { fd.close() }
        cache.evictAll()
    }

    sealed interface OpenResult {
        data class Ok(val doc: PdfDocument) : OpenResult
        data class Error(val message: String) : OpenResult
    }

    companion object {
        fun open(context: Context, uri: Uri): OpenResult = try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return OpenResult.Error("Dosya açılamadı")
            try {
                OpenResult.Ok(PdfDocument(fd, PdfRenderer(fd)))
            } catch (e: SecurityException) {
                fd.close(); OpenResult.Error("Bu PDF parola korumalı; parola korumalı dosyalar desteklenmiyor.")
            } catch (e: Exception) {
                fd.close(); OpenResult.Error("Dosya geçerli bir PDF değil veya bozuk.")
            }
        } catch (e: SecurityException) {
            OpenResult.Error("Dosyaya erişim izni yok. Dosyayı yeniden seçin.")
        } catch (e: Exception) {
            OpenResult.Error("Dosya bulunamadı veya okunamadı.")
        }
    }
}
