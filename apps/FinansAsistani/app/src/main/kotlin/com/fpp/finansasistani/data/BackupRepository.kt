package com.fpp.finansasistani.data

import android.content.Context
import android.net.Uri
import com.fpp.finans.core.FinanceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Yedekleri kullanıcının seçtiği dosyaya yazar/okur (Storage Access Framework).
 * Bu yöntem hiçbir depolama izni gerektirmez; dosya konumunu kullanıcı seçer.
 */
class BackupRepository(
    private val context: Context,
    private val repository: FinanceRepository,
) {
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val data = repository.snapshot()
        val bytes = BackupSerializer.toJson(data).toByteArray(Charsets.UTF_8)
        val resolver = context.contentResolver
        val stream = runCatching { resolver.openOutputStream(uri, "wt") }.getOrNull()
            ?: resolver.openOutputStream(uri)
            ?: throw IOException("Dosya yazmak için açılamadı.")
        stream.use { it.write(bytes) }
        data.recordCount
    }

    suspend fun import(uri: Uri): FinanceData = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Dosya okunamadı.")
        val text = input.use { stream ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_BACKUP_BYTES) throw BackupFormatException("Yedek dosyası çok büyük.")
                out.write(buffer, 0, read)
            }
            out.toString(Charsets.UTF_8.name())
        }
        val data = BackupSerializer.fromJson(text)
        repository.replaceAll(data)
        data
    }

    private companion object {
        const val MAX_BACKUP_BYTES = 5 * 1024 * 1024
    }
}
