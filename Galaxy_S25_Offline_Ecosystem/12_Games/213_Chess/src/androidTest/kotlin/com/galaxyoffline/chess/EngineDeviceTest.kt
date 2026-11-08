package com.galaxyoffline.chess

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.galaxyoffline.chess.engine.AnalysisRequest
import com.galaxyoffline.chess.engine.AnalysisUpdate
import com.galaxyoffline.chess.engine.EngineIO
import com.galaxyoffline.chess.engine.EngineSession
import com.galaxyoffline.chess.engine.ProcessEngineIO
import com.galaxyoffline.chess.engine.Score
import com.galaxyoffline.chess.engine.SearchLimit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Gerçek Stockfish ikilisi (nativeLibraryDir/libstockfish.so) ile süreç düzeyi testler. */
@RunWith(AndroidJUnit4::class)
class EngineDeviceTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val bin = File(ctx.applicationInfo.nativeLibraryDir, "libstockfish.so")
    private val lastIo = AtomicReference<EngineIO?>()
    private lateinit var session: EngineSession

    @Before fun setUp() {
        session = EngineSession({ ProcessEngineIO(bin, ctx.filesDir).also { lastIo.set(it) } })
        session.setOption("Threads", "2"); session.setOption("Hash", "16")
    }
    @After fun tearDown() = session.close()

    private fun run(fen: String, limit: SearchLimit, multiPv: Int = 1, timeoutS: Long = 60): AnalysisUpdate {
        val done = CountDownLatch(1); val last = AtomicReference<AnalysisUpdate>()
        session.analyze(AnalysisRequest(fen, limit, multiPv)) { u -> if (u.final) { last.set(u); done.countDown() } }
        assertTrue("motor $timeoutS sn içinde bitirmedi", done.await(timeoutS, TimeUnit.SECONDS))
        return last.get()
    }

    @Test fun binaryIsExtractedAndExecutable() {
        assertTrue("libstockfish.so çıkarılmamış: ${bin.absolutePath}", bin.isFile)
        assertTrue(bin.canExecute())
        session.start()
        assertTrue("motor adı: ${session.engineName}", session.engineName?.startsWith("Stockfish 19") == true)
    }

    @Test fun findsMateInOne() {
        val u = run("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1", SearchLimit.Nodes(200_000))
        assertNull(u.error)
        assertEquals("a1a8", u.bestMove)
        assertEquals(Score.Mate(1), u.lines.first().score)
    }

    @Test fun threeLinesFromStart() {
        val u = run("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", SearchLimit.Nodes(300_000), multiPv = 3)
        assertNull(u.error)
        assertEquals(listOf(1, 2, 3), u.lines.map { it.multipv })
        assertEquals(3, u.lines.map { it.pv.first() }.toSet().size)
    }

    @Test fun killedProcessReportsErrorThenRestarts() {
        val gotError = CountDownLatch(1); val err = AtomicReference<String?>()
        session.analyze(AnalysisRequest("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", SearchLimit.Infinite)) { u ->
            if (u.final) { err.set(u.error); gotError.countDown() }
        }
        Thread.sleep(500)
        lastIo.get()!!.kill()
        assertTrue(gotError.await(20, TimeUnit.SECONDS))
        assertNotNull("kapanma hata olarak bildirilmeli", err.get())
        val u = run("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1", SearchLimit.Nodes(100_000))
        assertNull(u.error)
        assertEquals("a1a8", u.bestMove)
        assertTrue(session.restarts >= 1)
    }
}
