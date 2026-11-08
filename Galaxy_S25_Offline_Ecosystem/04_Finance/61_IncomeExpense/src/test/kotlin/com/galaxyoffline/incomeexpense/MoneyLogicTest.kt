package com.galaxyoffline.incomeexpense

import com.galaxyoffline.incomeexpense.data.Budget
import com.galaxyoffline.incomeexpense.data.MoneyLogic
import com.galaxyoffline.incomeexpense.data.Tx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MoneyLogicTest {
    private fun tx(id: Long, type: String, amount: Double, cat: String, d: LocalDate) = Tx(id, type, amount, cat, d.toEpochDay(), createdAt = id)

    @Test fun monthlySummaryAndBudgets() {
        val all = listOf(
            tx(1, "INCOME", 30000.0, "SALARY", LocalDate.of(2026, 3, 1)),
            tx(2, "EXPENSE", 5000.0, "MARKET", LocalDate.of(2026, 3, 5)),
            tx(3, "EXPENSE", 3000.0, "MARKET", LocalDate.of(2026, 3, 20)),
            tx(4, "EXPENSE", 2000.0, "BILLS", LocalDate.of(2026, 3, 10)),
            tx(5, "EXPENSE", 8000.0, "MARKET", LocalDate.of(2026, 2, 10)),
        )
        val s = MoneyLogic.summary(all, YearMonth.of(2026, 3), listOf(Budget("MARKET", 7000.0), Budget("FUN", 1000.0)))
        assertEquals(30000.0, s.income, 1e-9); assertEquals(10000.0, s.expense, 1e-9); assertEquals(20000.0, s.net, 1e-9)
        assertEquals(25.0, s.expenseChange!!, 1e-9)
        assertEquals("MARKET", s.categories.first().category.name)
        assertEquals(0.8, s.categories.first().share, 1e-9)
        assertTrue(s.categories.any { it.category.name == "FUN" && it.total == 0.0 && it.limit == 1000.0 })
        assertEquals(4, s.transactions.size)
    }

    @Test fun csvIsExcelFriendly() {
        val csv = MoneyLogic.csv(listOf(tx(1, "EXPENSE", 1234.5, "MARKET", LocalDate.of(2026, 1, 2)).copy(note = "süt \"tam\"")))
        assertTrue(csv.startsWith("﻿Tarih;"))
        assertTrue(csv.contains("2026-01-02;Gider;Market;1234,5;Kart;\"süt \"\"tam\"\"\""))
    }
}
