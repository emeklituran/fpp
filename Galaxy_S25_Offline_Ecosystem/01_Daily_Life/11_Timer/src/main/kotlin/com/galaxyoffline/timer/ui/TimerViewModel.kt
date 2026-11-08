package com.galaxyoffline.timer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.timer.data.Phase
import com.galaxyoffline.timer.data.PomodoroConfig
import com.galaxyoffline.timer.data.Preset
import com.galaxyoffline.timer.data.PresetDatabase
import com.galaxyoffline.timer.data.TimerEngine
import com.galaxyoffline.timer.data.TimerMode
import com.galaxyoffline.timer.data.TimerState
import com.galaxyoffline.timer.data.TimerStore
import com.galaxyoffline.timer.ring.RingService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TimerViewModel(private val app: Application) : AndroidViewModel(app) {
    private val dao = PresetDatabase.get(app).presets()
    val messages = UiMessages()
    val state: StateFlow<TimerState> = TimerStore.state
    val config: StateFlow<PomodoroConfig> = TimerStore.config
    val today: StateFlow<Int> = TimerStore.todaySessions
    val presets: StateFlow<List<Preset>> = dao.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { TimerStore.load(app); checkOverdue() }

    private fun now() = System.currentTimeMillis()

    /** Alarm sistem tarafından geciktirildiyse ve uygulama açıldığında süre dolmuşsa uyarıyı hemen ver. */
    fun checkOverdue() { if (TimerStore.isOverdue()) RingService.fire(app) }

    fun startTimer(ms: Long, label: String = "") = TimerStore.update(app) { TimerEngine.start(it.copy(mode = TimerMode.TIMER, phase = Phase.TIMER, cycleWorkDone = 0), ms, now(), label) }
    fun startPomodoro(phase: Phase = Phase.WORK) = TimerStore.update(app) { TimerEngine.startPhase(it.copy(mode = TimerMode.POMODORO), phase, config.value, now()) }
    fun pause() = TimerStore.update(app) { TimerEngine.pause(it, now()) }
    fun resume() = TimerStore.update(app) { TimerEngine.resume(it, now()) }
    fun plusMinute() = TimerStore.update(app) { TimerEngine.addMinute(it, now()) }
    fun reset() { if (RingService.ringing.value) RingService.command(app, RingService.ACTION_STOP); TimerStore.update(app) { TimerEngine.reset(it).copy(cycleWorkDone = if (it.mode == TimerMode.POMODORO) it.cycleWorkDone else 0) } }
    fun skipPhase() = TimerStore.update(app) { s -> TimerEngine.startPhase(s, TimerEngine.nextPhase(s, config.value), config.value, now()) }
    fun setConfig(c: PomodoroConfig) = TimerStore.setConfig(app, c)
    fun savePreset(label: String, seconds: Long) = launchSafe(messages, "Kaydedilemedi") { dao.upsert(Preset(label = label.trim().ifBlank { TimerEngine.format(seconds * 1000) }, seconds = seconds)) }
    fun deletePreset(p: Preset) = launchSafe(messages) { dao.delete(p.id) }
}
