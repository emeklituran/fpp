package com.galaxyoffline.shoppinglist

import com.galaxyoffline.shoppinglist.data.Category
import com.galaxyoffline.shoppinglist.data.Item
import com.galaxyoffline.shoppinglist.data.ItemHistory
import com.galaxyoffline.shoppinglist.data.ShoppingList
import com.galaxyoffline.shoppinglist.data.ShoppingLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingLogicTest {
    private fun i(id: Long, name: String, cat: Category, qty: Double = 1.0, price: Double = 0.0, checked: Boolean = false) =
        Item(id, 1, name, qty, "adet", cat.name, price, checked, id)

    private val items = listOf(i(1, "Süt", Category.DAIRY, 2.0, 30.0), i(2, "Elma", Category.PRODUCE, 1.5, 40.0),
        i(3, "Ekmek", Category.BAKERY, checked = true, price = 10.0), i(4, "Peynir", Category.DAIRY))

    @Test fun totalsAndGrouping() {
        assertEquals(130.0, ShoppingLogic.total(items), 1e-9)
        assertEquals(120.0, ShoppingLogic.remaining(items), 1e-9)
        val g = ShoppingLogic.group(items)
        assertEquals(Category.PRODUCE, g[0].first)
        assertEquals(listOf("Peynir", "Süt"), g[1].second.map { it.name })
        assertNull(g.last().first)
    }

    @Test fun suggestionsPreferPrefix() {
        val h = listOf(ItemHistory("domates", "Domates", "PRODUCE", "kg", 0.0, 3, 1), ItemHistory("kuru domates", "Kuru domates", "PANTRY", "paket", 0.0, 9, 2))
        assertEquals("domates", ShoppingLogic.suggestions(h, "dom", emptySet()).first().key)
        assertTrue(ShoppingLogic.suggestions(h, "dom", setOf("domates", "kuru domates")).isEmpty())
    }

    @Test fun shareText() {
        val t = ShoppingLogic.shareText(ShoppingList(1, "Market", 0), items)
        assertTrue(t.contains("☐ Süt – 2 adet"))
        assertTrue(t.contains("☑ Ekmek"))
    }
}
