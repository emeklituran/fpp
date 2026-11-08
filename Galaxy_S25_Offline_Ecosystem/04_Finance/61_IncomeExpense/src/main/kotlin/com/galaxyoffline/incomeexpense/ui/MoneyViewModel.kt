package com.galaxyoffline.incomeexpense.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.incomeexpense.data.Budget
import com.galaxyoffline.incomeexpense.data.MoneyDatabase
import com.galaxyoffline.incomeexpense.data.MoneyLogic
import com.galaxyoffline.incomeexpense.data.MonthSummary
import com.galaxyoffline.incomeexpense.data.Tx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.time.YearMonth

class MoneyViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = MoneyDatabase.get(app).money()
    val messages = UiMessages()
    val month = MutableStateFlow(YearMonth.now())
    val budgets: StateFlow<List<Budget>> = dao.observeBudgets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<MonthSummary?> = combine(dao.observeAll(), month, dao.observeBudgets()) { all, m, b -> MoneyLogic.summary(all, m, b) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun shiftMonth(d: Long) { month.value = month.value.plusMonths(d) }

    fun save(t: Tx, onDone: () -> Unit) = launchSafe(messages, "Kayıt kaydedilemedi") {
        if (!(t.amount > 0 && t.amount < 1e11)) { messages.post("Geçerli bir tutar girin"); return@launchSafe }
        dao.upsert(t.copy(note = t.note.trim().take(300), createdAt = if (t.createdAt == 0L) System.currentTimeMillis() else t.createdAt))
        onDone()
    }
    fun delete(t: Tx) = launchSafe(messages) { dao.delete(t.id) }
    fun restore(t: Tx) = launchSafe(messages) { dao.upsert(t) }
    fun setBudget(category: String, limit: Double?) = launchSafe(messages) {
        if (limit == null || limit <= 0) dao.deleteBudget(category) else dao.upsertBudget(Budget(category, limit))
    }

    fun exportCsv(uri: Uri) = launchSafe(messages, "Dışa aktarılamadı") {
        val all = dao.all()
        withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(MoneyLogic.csv(all).toByteArray(Charsets.UTF_8)) } ?: error("dosya")
        }
        messages.post("${all.size} kayıt CSV olarak dışa aktarıldı")
    }
}
