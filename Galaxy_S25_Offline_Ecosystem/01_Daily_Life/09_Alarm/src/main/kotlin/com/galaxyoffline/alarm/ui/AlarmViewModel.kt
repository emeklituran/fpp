package com.galaxyoffline.alarm.ui

import android.app.Application
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.alarm.data.Alarm
import com.galaxyoffline.alarm.data.AlarmDatabase
import com.galaxyoffline.alarm.data.AlarmLogic
import com.galaxyoffline.alarm.ring.AlarmScheduler
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class AlarmPermissions(val notifications: Boolean = true, val fullScreen: Boolean = true, val exact: Boolean = true)

class AlarmListViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AlarmDatabase.get(app).alarms()
    private val ctx = app
    val messages = UiMessages()

    val alarms: StateFlow<List<Alarm>?> = dao.observeAll().map { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _perms = MutableStateFlow(AlarmPermissions())
    val permissions: StateFlow<AlarmPermissions> = _perms.asStateFlow()

    fun refreshPermissions() {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        _perms.value = AlarmPermissions(
            notifications = NotificationManagerCompat.from(ctx).areNotificationsEnabled(),
            fullScreen = if (Build.VERSION.SDK_INT >= 34) nm.canUseFullScreenIntent() else true,
            exact = AlarmScheduler.canSchedule(ctx),
        )
    }

    fun setEnabled(a: Alarm, enabled: Boolean) = launchSafe(messages, "Alarm güncellenemedi") {
        val u = a.copy(enabled = enabled)
        dao.update(u)
        if (enabled) announce(u) else AlarmScheduler.cancel(ctx, a.id)
    }

    fun delete(a: Alarm) = launchSafe(messages) { AlarmScheduler.cancel(ctx, a.id); dao.delete(a.id) }
    fun restore(a: Alarm) = launchSafe(messages) { dao.insert(a); if (a.enabled) AlarmScheduler.schedule(ctx, a) }

    private fun announce(a: Alarm) {
        AlarmScheduler.schedule(ctx, a)
        val now = LocalDateTime.now()
        messages.post("Alarm ${AlarmLogic.untilText(now, AlarmLogic.nextTrigger(a, now))} çalacak")
    }
}

class AlarmEditViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val dao = AlarmDatabase.get(app).alarms()
    private val ctx = app
    val messages = UiMessages()
    private val _alarm = MutableStateFlow<Alarm?>(null)
    val alarm: StateFlow<Alarm?> = _alarm.asStateFlow()

    init {
        val id = handle.get<Long>("id") ?: -1L
        viewModelScope.launch {
            val now = LocalDateTime.now().plusHours(1)
            _alarm.value = (if (id > 0) runCatching { dao.get(id) }.getOrNull() else null)
                ?: Alarm(hour = now.hour, minute = 0, createdAt = 0)
        }
    }

    fun update(transform: (Alarm) -> Alarm) { _alarm.value = _alarm.value?.let(transform) }

    fun save(onDone: () -> Unit) {
        val a = _alarm.value ?: return
        launchSafe(messages, "Alarm kaydedilemedi") {
            val fixed = a.copy(label = a.label.trim().take(60), enabled = true)
            val saved = if (fixed.id == 0L) fixed.copy(id = dao.insert(fixed.copy(createdAt = System.currentTimeMillis()))) else fixed.also { dao.update(it) }
            AlarmScheduler.schedule(ctx, saved)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) = launchSafe(messages) {
        val a = _alarm.value ?: return@launchSafe
        if (a.id != 0L) { AlarmScheduler.cancel(ctx, a.id); dao.delete(a.id) }
        onDone()
    }
}
