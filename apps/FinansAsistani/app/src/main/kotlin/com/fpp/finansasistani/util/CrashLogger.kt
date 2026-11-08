package com.fpp.finansasistani.util

import android.content.Context
import android.os.Build
import com.fpp.finansasistani.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import kotlin.system.exitProcess

/**
 * Yakalanmamış hataları yalnızca cihazdaki özel uygulama klasörüne kaydeder.
 * Hiçbir yere gönderilmez; kullanıcı Ayarlar > Son hata kaydı bölümünden görüp silebilir.
 */
class CrashLogger(context: Context) {
    private val file = File(context.applicationContext.filesDir, "son_hata.txt")

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { file.writeText(buildReport(thread, throwable)) }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
        }
    }

    fun read(): String? = runCatching { if (file.exists()) file.readText() else null }.getOrNull()

    fun clear() {
        runCatching { file.delete() }
    }

    private fun buildReport(thread: Thread, throwable: Throwable): String {
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        return buildString {
            appendLine("Zaman: ${LocalDateTime.now()}")
            appendLine("Uygulama: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Cihaz: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("İş parçacığı: ${thread.name}")
            appendLine()
            append(trace.take(20_000))
        }
    }
}
