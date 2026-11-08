package com.galaxyoffline.stopwatch

import com.galaxyoffline.stopwatch.data.StopwatchLogic as L
import com.galaxyoffline.stopwatch.data.StopwatchState
import org.junit.Assert.assertEquals
import org.junit.Test

class StopwatchLogicTest {
    @Test fun startPauseResume() {
        var s = L.start(StopwatchState(), nowElapsed = 1_000, nowWall = 50_000)
        assertEquals(500, L.elapsed(s, 1_500, 50_500))
        s = L.pause(s, 3_000, 52_000)
        assertEquals(2_000, L.elapsed(s, 99_999, 999_999))
        s = L.start(s, 10_000, 60_000)
        assertEquals(3_000, L.elapsed(s, 11_000, 61_000))
    }

    @Test fun survivesRebootViaWallClock() {
        val s = L.start(StopwatchState(accumulated = 1_000), nowElapsed = 900_000, nowWall = 1_000_000)
        // Yeniden başlatma: monoton saat küçüldü; duvar saati 5 sn ilerledi.
        assertEquals(6_000, L.elapsed(s, nowElapsed = 2_000, nowWall = 1_005_000))
    }

    @Test fun lapsBestWorst() {
        var s = L.start(StopwatchState(), 0, 0)
        s = L.lap(s, 10_000, 10_000); s = L.lap(s, 15_000, 15_000); s = L.lap(s, 30_000, 30_000)
        assertEquals(listOf(10_000L, 5_000L, 15_000L), L.lapDurations(s.laps))
        assertEquals(1 to 2, L.bestWorst(s.laps))
        assertEquals(s.laps, L.decodeLaps(L.encodeLaps(s.laps)))
        assertEquals(3, L.lap(s, 30_000, 30_000).laps.size) // aynı anda ikinci tur alınmaz
    }
}
