package com.galaxyoffline.reminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.reminder.alarm.ReminderScheduler
import com.galaxyoffline.reminder.data.Reminder
import com.galaxyoffline.reminder.data.ReminderDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class Permissions(val notifications: Boolean = true, val exact: Boolean = true)

data class ReminderState(val loading: Boolean = true, val upcoming: List<Reminder> = emptyList(), val past: List<Reminder> = emptyList())

class ReminderViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = ReminderDatabase.get(app).reminders()
    private val ctx = app
    val messages = UiMessages()

    val state: StateFlow<ReminderState> = dao.observeAll().map { all ->
        val (past, upcoming) = all.partition { it.done || !it.enabled && it.triggerAt < System.currentTimeMillis() }
        ReminderState(false, upcoming.sortedBy { it.triggerAt }, past.sortedByDescending { it.triggerAt })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderState())

    private val _permissions = MutableStateFlow(Permissions())
    val permissions: StateFlow<Permissions> = _permissions.asStateFlow()

    fun refreshPermissions() {
        _permissions.value = Permissions(ReminderScheduler.notificationsAllowed(ctx), ReminderScheduler.canExact(ctx))
    }

    fun save(r: Reminder) = launchSafe(messages, "Hatırlatma kaydedilemedi") {
        if (r.title.isBlank()) { messages.post("Başlık boş olamaz"); return@launchSafe }
        if (r.triggerAt <= System.currentTimeMillis()) { messages.post("Lütfen gelecekte bir zaman seçin"); return@launchSafe }
        val fixed = r.copy(title = r.title.trim().take(120), note = r.note.take(1000), done = false, enabled = true)
        val saved = if (fixed.id == 0L) fixed.copy(id = dao.insert(fixed.copy(createdAt = System.currentTimeMillis())))
        else fixed.also { dao.update(it) }
        ReminderScheduler.schedule(ctx, saved)
        messages.post("Hatırlatma kuruldu" + if (!ReminderScheduler.canExact(ctx)) " (yaklaşık zamanlı)" else "")
    }

    fun setEnabled(r: Reminder, enabled: Boolean) = launchSafe(messages) {
        if (enabled && r.triggerAt <= System.currentTimeMillis() && r.repeatRule == com.galaxyoffline.reminder.data.Repeat.NONE) {
            messages.post("Geçmiş zamanlı hatırlatma açılamaz; düzenleyip yeni zaman seçin"); return@launchSafe
        }
        val u = r.copy(enabled = enabled)
        dao.update(u)
        if (enabled) ReminderScheduler.rescheduleAll(ctx) else ReminderScheduler.cancel(ctx, r.id)
    }

    fun delete(r: Reminder) = launchSafe(messages, "Silinemedi") { ReminderScheduler.cancel(ctx, r.id); dao.delete(r.id) }
    fun restore(r: Reminder) = launchSafe(messages) { dao.insert(r); ReminderScheduler.rescheduleAll(ctx) }
    fun clearPast() = launchSafe(messages) { messages.post("${dao.clearDone()} geçmiş hatırlatma silindi") }
}
