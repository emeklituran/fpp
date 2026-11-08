package com.galaxyoffline.shoppinglist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galaxyoffline.core.designsystem.util.UiMessages
import com.galaxyoffline.core.designsystem.util.launchSafe
import com.galaxyoffline.shoppinglist.App
import com.galaxyoffline.shoppinglist.data.Item
import com.galaxyoffline.shoppinglist.data.ItemHistory
import com.galaxyoffline.shoppinglist.data.ShoppingList
import com.galaxyoffline.shoppinglist.data.ShoppingLogic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ShoppingState(
    val loading: Boolean = true,
    val lists: List<ShoppingList> = emptyList(),
    val items: Map<Long, List<Item>> = emptyMap(),
    val history: List<ItemHistory> = emptyList(),
)

class ShoppingViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as App).database.shopping()
    val messages = UiMessages()

    val state: StateFlow<ShoppingState> = combine(dao.observeLists(), dao.observeItems(), dao.observeHistory()) { l, i, h ->
        ShoppingState(false, l, i.groupBy { it.listId }, h)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingState())

    fun addList(name: String, onCreated: (Long) -> Unit = {}) = launchSafe(messages, "Liste oluşturulamadı") {
        val n = name.trim().take(60)
        if (n.isBlank()) return@launchSafe
        onCreated(dao.insertList(ShoppingList(name = n, createdAt = System.currentTimeMillis())))
    }
    fun renameList(l: ShoppingList, name: String) = launchSafe(messages) { if (name.isNotBlank()) dao.updateList(l.copy(name = name.trim().take(60))) }
    fun deleteList(l: ShoppingList) = launchSafe(messages, "Liste silinemedi") { dao.deleteList(l.id) }

    /** Hızlı ekleme: geçmişte varsa kategori, birim ve fiyat oradan gelir. */
    fun quickAdd(listId: Long, name: String) = launchSafe(messages, "Ürün eklenemedi") {
        val n = name.trim().take(80)
        if (n.isBlank()) return@launchSafe
        val h = dao.history(ShoppingLogic.key(n))
        val item = Item(listId = listId, name = h?.name ?: n, unit = h?.unit ?: "adet", category = h?.category ?: "OTHER",
            price = h?.price ?: 0.0, createdAt = System.currentTimeMillis())
        dao.upsertItem(item)
        remember(item)
    }

    fun save(item: Item) = launchSafe(messages, "Ürün kaydedilemedi") {
        if (item.name.isBlank()) { messages.post("Ürün adı boş olamaz"); return@launchSafe }
        val fixed = item.copy(name = item.name.trim().take(80), quantity = item.quantity.coerceIn(0.01, 99_999.0),
            price = item.price.coerceIn(0.0, 10_000_000.0))
        dao.upsertItem(fixed)
        remember(fixed)
    }

    private suspend fun remember(item: Item) {
        val key = ShoppingLogic.key(item.name)
        val old = dao.history(key)
        dao.upsertHistory(ItemHistory(key, item.name, item.category, item.unit, if (item.price > 0) item.price else old?.price ?: 0.0,
            (old?.useCount ?: 0) + 1, System.currentTimeMillis()))
    }

    fun toggle(item: Item) = launchSafe(messages) { dao.upsertItem(item.copy(checked = !item.checked)) }
    fun delete(item: Item) = launchSafe(messages) { dao.deleteItem(item.id) }
    fun restore(item: Item) = launchSafe(messages) { dao.upsertItem(item) }
    fun clearChecked(listId: Long) = launchSafe(messages) { messages.post("${dao.clearChecked(listId)} alınan ürün silindi") }
    fun uncheckAll(listId: Long) = launchSafe(messages) { dao.uncheckAll(listId) }
    fun clearHistory() = launchSafe(messages) { dao.clearHistory(); messages.post("Ürün önerileri temizlendi") }
}
