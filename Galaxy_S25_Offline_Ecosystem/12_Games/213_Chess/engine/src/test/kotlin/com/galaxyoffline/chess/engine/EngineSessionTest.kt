package com.galaxyoffline.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class EngineSessionTest {
    private val fenA = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    private val fenB = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"

    private class Collector {
        val updates = CopyOnWriteArrayList<AnalysisUpdate>()
        val done = CountDownLatch(1)
        val listener: (AnalysisUpdate) -> Unit = { updates += it; if (it.final) done.countDown() }
        fun await(ms: Long = 5_000) = assertTrue("son güncelleme gelmedi", done.await(ms, TimeUnit.MILLISECONDS))
    }

    @Test fun handshakeAndOptions() {
        val fake = FakeEngine()
        val s = EngineSession({ fake })
        s.setOption("Threads", "3"); s.setOption("Hash", "64")
        s.start()
        assertEquals("FakeFish 1", s.engineName)
        assertTrue(fake.received.containsAll(listOf("uci", "setoption name Threads value 3", "setoption name Hash value 64", "isready")))
        s.close()
    }

    @Test fun timedAnalysisDeliversThreeLinesAndFinalBestMove() {
        val s = EngineSession({ FakeEngine() })
        val c = Collector()
        s.analyze(AnalysisRequest(fenA, SearchLimit.Time(300), multiPv = 3), c.listener)
        c.await()
        val last = c.updates.last()
        assertTrue(last.final); assertNull(last.error)
        assertEquals(listOf(1, 2, 3), last.lines.map { it.multipv })
        assertEquals(FakeEngine.pvTag(fenA), last.bestMove)
        assertTrue("ara güncelleme bekleniyordu", c.updates.any { !it.final })
        // Alt/üst sınır satırları (cp 999) hiçbir güncellemede yer almaz
        assertTrue(c.updates.all { u -> u.lines.none { it.score == Score.Cp(999) } })
        s.close()
    }

    @Test fun newRequestCancelsOldAndOldResultsNeverArrive() {
        val s = EngineSession({ FakeEngine() })
        val old = Collector(); val new = Collector()
        val g1 = s.analyze(AnalysisRequest(fenA, SearchLimit.Infinite), old.listener)
        Thread.sleep(150)
        val g2 = s.analyze(AnalysisRequest(fenB, SearchLimit.Time(200)), new.listener)
        new.await()
        assertTrue(g2 > g1)
        val oldCountAfterCancel = old.updates.size
        Thread.sleep(300)
        assertEquals("iptal edilen arama güncelleme göndermeye devam etti", oldCountAfterCancel, old.updates.size)
        assertTrue("iptal edilen aramanın son sonucu dinleyiciye gitmemeli", old.updates.none { it.final })
        assertTrue(new.updates.all { it.fen == fenB && it.generation == g2 })
        assertTrue(new.updates.last().lines.all { l -> l.pv.first().endsWith(fenB.hashCode().toString()) })
        s.close()
    }

    @Test fun stopFinishesInfiniteSearchWithResult() {
        val s = EngineSession({ FakeEngine() })
        val c = Collector()
        s.analyze(AnalysisRequest(fenA, SearchLimit.Infinite), c.listener)
        Thread.sleep(150); s.stop()
        c.await()
        assertNotNull(c.updates.last().bestMove)
        s.close()
    }

    @Test fun hungEngineIsKilledAfterGraceAndRestarted() {
        var created = 0
        val s = EngineSession({ created++; if (created == 1) FakeEngine(FakeEngine.Mode.HANG) else FakeEngine() }, stopGraceMs = 200, killGraceMs = 200)
        val c = Collector()
        s.analyze(AnalysisRequest(fenA, SearchLimit.Time(100)), c.listener)
        c.await(5_000)
        assertNotNull("zaman aşımı hatası bekleniyordu", c.updates.last().error)
        val c2 = Collector()
        s.analyze(AnalysisRequest(fenB, SearchLimit.Time(100)), c2.listener)
        c2.await()
        assertNull(c2.updates.last().error)
        assertEquals(2, created); assertEquals(1, s.restarts)
        s.close()
    }

    @Test fun crashIsReportedAndNextRequestRestarts() {
        var created = 0
        val s = EngineSession({ created++; if (created == 1) FakeEngine(FakeEngine.Mode.CRASH_ON_GO) else FakeEngine() })
        val c = Collector()
        s.analyze(AnalysisRequest(fenA, SearchLimit.Time(200)), c.listener)
        c.await()
        assertTrue(c.updates.last().error!!.contains("kapandı"))
        val c2 = Collector()
        s.analyze(AnalysisRequest(fenA, SearchLimit.Time(150)), c2.listener)
        c2.await()
        assertNull(c2.updates.last().error)
        assertEquals(2, created)
        s.close()
    }

    @Test fun silentEngineFailsStartWithClearError() {
        val s = EngineSession({ FakeEngine(FakeEngine.Mode.SILENT) })
        val c = Collector()
        s.analyze(AnalysisRequest(fenA, SearchLimit.Time(100)), c.listener)
        c.await(8_000)
        assertTrue(c.updates.single().error!!.contains("uciok"))
        s.close()
    }

    @Test fun throttleLimitsUpdateRate() {
        val s = EngineSession({ FakeEngine(infoEveryMs = 1) }, throttleMs = 125)
        val c = Collector()
        s.analyze(AnalysisRequest(fenA, SearchLimit.Time(1_000)), c.listener)
        c.await()
        val interim = c.updates.count { !it.final }
        assertTrue("1 sn'de en fazla ~9 ara güncelleme olmalı; $interim geldi", interim in 1..10)
        s.close()
    }
}
