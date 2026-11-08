package com.galaxyoffline.core.designsystem.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import kotlin.system.exitProcess

/**
 * Yakalanmamış hataları yalnızca uygulamanın özel klasörüne yazar; hiçbir yere gönderilmez.
 * Kullanıcı Hakkında ekranından kaydı görebilir ve silebilir.
 */
object CrashLogger {
    private const val FILE = "son_hata.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { File(app.filesDir, FILE).writeText(report(app, thread, error)) }
            if (previous != null) previous.uncaughtException(thread, error) else {
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
        }
    }

    fun read(context: Context): String? =
        runCatching { File(context.filesDir, FILE).takeIf { it.exists() }?.readText() }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE).delete() }
    }

    private fun report(context: Context, thread: Thread, error: Throwable): String {
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        return buildString {
            appendLine("Zaman: ${LocalDateTime.now()}")
            appendLine("Uygulama: ${context.packageName} $version")
            appendLine("Cihaz: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("İş parçacığı: ${thread.name}")
            appendLine()
            append(trace.take(20_000))
        }
    }
}
