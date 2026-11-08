package com.galaxyoffline.timer.data

enum class TimerMode { TIMER, POMODORO }
enum class Phase(val label: String) { TIMER("Zamanlayıcı"), WORK("Çalışma"), SHORT_BREAK("Kısa mola"), LONG_BREAK("Uzun mola") }
enum class RunStatus { IDLE, RUNNING, PAUSED, FINISHED }

data class PomodoroConfig(val workMin: Int = 25, val shortMin: Int = 5, val longMin: Int = 15, val longEvery: Int = 4) {
    fun minutes(p: Phase) = when (p) { Phase.WORK -> workMin; Phase.SHORT_BREAK -> shortMin; Phase.LONG_BREAK -> longMin; Phase.TIMER -> 0 }
}

/**
 * Tek zamanlayıcının durumu. Zaman duvar saati (ms) ile tutulur; böylece süreç kapansa bile
 * kayıtlı [endAt] üzerinden doğru kalan süre hesaplanır.
 */
data class TimerState(
    val mode: TimerMode = TimerMode.TIMER,
    val phase: Phase = Phase.TIMER,
    val status: RunStatus = RunStatus.IDLE,
    val totalMs: Long = 0,
    val endAt: Long = 0,
    val pausedRemaining: Long = 0,
    val label: String = "",
    /** Bu döngüde tamamlanan çalışma oturumu sayısı (uzun mola hesabı için). */
    val cycleWorkDone: Int = 0,
) {
    fun remaining(now: Long): Long = when (status) {
        RunStatus.RUNNING -> (endAt - now).coerceAtLeast(0)
        RunStatus.PAUSED -> pausedRemaining
        RunStatus.IDLE -> totalMs
        RunStatus.FINISHED -> 0
    }
    fun progress(now: Long): Float = if (totalMs <= 0) 0f else 1f - remaining(now).toFloat() / totalMs
}

object TimerEngine {
    const val MAX_MS = 100L * 3600_000 - 1000

    fun start(s: TimerState, durationMs: Long, now: Long, label: String = s.label): TimerState {
        val d = durationMs.coerceIn(1000, MAX_MS)
        return s.copy(status = RunStatus.RUNNING, totalMs = d, endAt = now + d, pausedRemaining = 0, label = label)
    }
    fun pause(s: TimerState, now: Long) = if (s.status != RunStatus.RUNNING) s else s.copy(status = RunStatus.PAUSED, pausedRemaining = s.remaining(now))
    fun resume(s: TimerState, now: Long) = if (s.status != RunStatus.PAUSED) s else s.copy(status = RunStatus.RUNNING, endAt = now + s.pausedRemaining)
    fun reset(s: TimerState) = s.copy(status = RunStatus.IDLE, endAt = 0, pausedRemaining = 0, totalMs = if (s.mode == TimerMode.TIMER) s.totalMs else s.totalMs)

    /** +1 dk: çalışırken bitişi uzatır, bitmişse 1 dakikalık yeni sayım başlatır. */
    fun addMinute(s: TimerState, now: Long): TimerState = when (s.status) {
        RunStatus.RUNNING -> s.copy(endAt = minOf(s.endAt + 60_000, now + MAX_MS), totalMs = s.totalMs + 60_000)
        RunStatus.PAUSED -> s.copy(pausedRemaining = s.pausedRemaining + 60_000, totalMs = s.totalMs + 60_000)
        RunStatus.FINISHED -> start(s, 60_000, now)
        RunStatus.IDLE -> s
    }

    fun finish(s: TimerState): TimerState {
        val done = if (s.mode == TimerMode.POMODORO && s.phase == Phase.WORK) s.cycleWorkDone + 1 else s.cycleWorkDone
        return s.copy(status = RunStatus.FINISHED, cycleWorkDone = done)
    }

    /** Pomodoro'da bitişten sonraki aşama: her [PomodoroConfig.longEvery] çalışmadan sonra uzun mola. */
    fun nextPhase(s: TimerState, cfg: PomodoroConfig): Phase = when (s.phase) {
        Phase.WORK -> if (s.cycleWorkDone > 0 && s.cycleWorkDone % cfg.longEvery == 0) Phase.LONG_BREAK else Phase.SHORT_BREAK
        Phase.SHORT_BREAK, Phase.LONG_BREAK, Phase.TIMER -> Phase.WORK
    }

    fun startPhase(s: TimerState, phase: Phase, cfg: PomodoroConfig, now: Long): TimerState {
        val cycle = if (phase == Phase.WORK && s.phase == Phase.LONG_BREAK) 0 else s.cycleWorkDone
        return start(s.copy(mode = TimerMode.POMODORO, phase = phase, cycleWorkDone = cycle), cfg.minutes(phase) * 60_000L, now, phase.label)
    }

    fun format(ms: Long): String {
        val t = (ms + 999) / 1000
        val h = t / 3600; val m = t / 60 % 60; val sec = t % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }
}
