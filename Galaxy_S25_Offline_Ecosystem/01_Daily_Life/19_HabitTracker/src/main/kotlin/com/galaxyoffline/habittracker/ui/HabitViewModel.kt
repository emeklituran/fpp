package com.galaxyoffline.habittracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.habittracker.data.Check
import com.galaxyoffline.habittracker.data.Habit
import com.galaxyoffline.habittracker.data.HabitDatabase
import com.galaxyoffline.habittracker.data.Reminders
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class HabitViewModel(private val app: Application) : AndroidViewModel(app) {
    private val dao = HabitDatabase.get(app).habits()
    val messages = UiMessages()
    val habits: StateFlow<List<Habit>?> = dao.observeHabits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    /** Son ~1 yılın işaretleri: alışkanlık id → gün kümesi. */
    val checks: StateFlow<Map<Long, Set<Long>>> = dao.observeChecks(LocalDate.now().minusDays(400).toEpochDay())
        .map { l -> l.groupBy({ it.habitId }, { it.day }).mapValues { it.value.toSet() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init { launchSafe(messages) { Reminders.rescheduleAll(app) } }

    fun save(h: Habit, onDone: () -> Unit) = launchSafe(messages, "Kaydedilemedi") {
        val clean = h.copy(name = h.name.trim(), createdDay = if (h.id == 0L) LocalDate.now().toEpochDay() else h.createdDay)
        val id = dao.upsert(clean)
        Reminders.schedule(app, if (clean.id == 0L) clean.copy(id = id) else clean)
        onDone()
    }

    fun toggle(h: Habit, day: LocalDate) = launchSafe(messages) {
        val d = day.toEpochDay()
        if (dao.isChecked(h.id, d) > 0) dao.uncheck(h.id, d) else dao.check(Check(h.id, d))
        if (day == LocalDate.now()) androidx.core.app.NotificationManagerCompat.from(app).cancel(h.id.toInt())
    }

    fun archive(h: Habit, archived: Boolean) = launchSafe(messages) {
        val n = h.copy(archived = archived); dao.upsert(n); Reminders.schedule(app, n)
    }

    fun delete(h: Habit) = launchSafe(messages) { Reminders.cancel(app, h.id); dao.delete(h.id) }
}
