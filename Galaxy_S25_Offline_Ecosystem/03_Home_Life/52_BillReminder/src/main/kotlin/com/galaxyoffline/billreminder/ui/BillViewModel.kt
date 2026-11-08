package com.galaxyoffline.billreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.billreminder.data.Bill
import com.galaxyoffline.billreminder.data.BillDatabase
import com.galaxyoffline.billreminder.data.BillLogic
import com.galaxyoffline.billreminder.data.DailyCheck
import com.galaxyoffline.billreminder.data.Payment
import com.galaxyoffline.core.common.TrFormat
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class BillViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = BillDatabase.get(app).bills()
    val messages = UiMessages()
    val bills: StateFlow<List<Bill>?> = dao.observeBills().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val payments: StateFlow<List<Payment>> = dao.observePayments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(b: Bill, onDone: () -> Unit) = launchSafe(messages, "Kaydedilemedi") {
        if (b.name.isBlank()) { messages.post("Ad gerekli"); return@launchSafe }
        if (!(b.amount >= 0 && b.amount < 1e10)) { messages.post("Geçerli bir tutar girin"); return@launchSafe }
        dao.upsert(b.copy(name = b.name.trim().take(80), anchorDay = b.due.dayOfMonth, createdAt = if (b.createdAt == 0L) System.currentTimeMillis() else b.createdAt))
        DailyCheck.run(getApplication()); onDone()
    }

    fun markPaid(b: Bill, amount: Double = b.amount) = launchSafe(messages, "Ödeme kaydedilemedi") {
        dao.insertPayment(Payment(billId = b.id, amount = amount, periodDue = b.dueDate, paidOn = LocalDate.now().toEpochDay()))
        val next = BillLogic.afterPayment(b)
        dao.upsert(next)
        messages.post(if (next.active) "${b.name} ödendi. Sonraki: ${TrFormat.formatDate(next.due)}" else "${b.name} ödendi")
    }

    fun delete(b: Bill) = launchSafe(messages) { dao.delete(b.id) }
    fun restore(b: Bill) = launchSafe(messages) { dao.upsert(b) }
    fun deletePayment(p: Payment) = launchSafe(messages) { dao.deletePayment(p.id) }
}
