package com.galaxyoffline.timer

import com.galaxyoffline.timer.data.Phase
import com.galaxyoffline.timer.data.PomodoroConfig
import com.galaxyoffline.timer.data.RunStatus
import com.galaxyoffline.timer.data.TimerEngine
import com.galaxyoffline.timer.data.TimerMode
import com.galaxyoffline.timer.data.TimerState
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerEngineTest {
    @Test fun startPauseResume() {
        var s = TimerEngine.start(TimerState(), 60_000, now = 1_000)
        assertEquals(40_000, s.remaining(21_000))
        s = TimerEngine.pause(s, 21_000)
        assertEquals(40_000, s.remaining(99_999))
        s = TimerEngine.resume(s, 100_000)
        assertEquals(140_000, s.endAt)
        s = TimerEngine.addMinute(s, 100_000)
        assertEquals(100_000, s.remaining(100_000))
        assertEquals(0, s.remaining(500_000))
    }

    @Test fun finishedPlusMinuteRestarts() {
        val s = TimerEngine.addMinute(TimerEngine.finish(TimerEngine.start(TimerState(), 5_000, 0)), 10_000)
        assertEquals(RunStatus.RUNNING, s.status); assertEquals(60_000, s.remaining(10_000))
    }

    @Test fun pomodoroCycle() {
        val cfg = PomodoroConfig(25, 5, 15, 4)
        var s = TimerState(mode = TimerMode.POMODORO)
        val phases = mutableListOf<Phase>()
        s = TimerEngine.startPhase(s, Phase.WORK, cfg, 0)
        repeat(9) {
            s = TimerEngine.finish(s)
            val next = TimerEngine.nextPhase(s, cfg); phases += next
            s = TimerEngine.startPhase(s, next, cfg, 0)
        }
        assertEquals(listOf(Phase.SHORT_BREAK, Phase.WORK, Phase.SHORT_BREAK, Phase.WORK, Phase.SHORT_BREAK, Phase.WORK, Phase.LONG_BREAK, Phase.WORK, Phase.SHORT_BREAK), phases)
        assertEquals(25 * 60_000L, TimerEngine.startPhase(s, Phase.WORK, cfg, 0).totalMs)
    }

    @Test fun formatting() {
        assertEquals("00:01", TimerEngine.format(1))
        assertEquals("01:00", TimerEngine.format(60_000))
        assertEquals("1:00:00", TimerEngine.format(3_600_000))
    }
}
