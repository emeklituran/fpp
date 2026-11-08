package com.galaxyoffline.chess.engine

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.Writer

/** Motorla metin satırı alışverişi. Gerçek süreç ya da testler için sahte motor. */
interface EngineIO {
    fun send(line: String)
    /** Bir sonraki çıktı satırı; süreç kapanırsa null. Engelleyicidir. */
    fun readLine(): String?
    fun isAlive(): Boolean
    fun kill()
}

/** Stockfish'i ayrı süreç olarak çalıştırır (Android'de yerel kütüphane klasöründeki `libstockfish.so`). */
class ProcessEngineIO(executable: File, workDir: File) : EngineIO {
    private val process: Process = ProcessBuilder(executable.absolutePath).directory(workDir).redirectErrorStream(true).start()
    private val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.US_ASCII))
    private val writer: Writer = process.outputStream.bufferedWriter(Charsets.US_ASCII)

    override fun send(line: String) {
        try { writer.write(line); writer.write("\n"); writer.flush() } catch (_: Exception) { /* süreç kapanmış; okuyucu EOF görür */ }
    }
    override fun readLine(): String? = try { reader.readLine() } catch (_: Exception) { null }
    override fun isAlive() = process.isAlive
    override fun kill() { process.destroyForcibly() }
}
