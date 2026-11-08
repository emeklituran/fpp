package com.galaxyoffline.billreminder

import com.galaxyoffline.billreminder.data.Bill
import com.galaxyoffline.billreminder.data.BillLogic
import com.galaxyoffline.billreminder.data.BillStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class BillLogicTest {
    private fun bill(due: LocalDate, rec: String = "MONTHLY", amount: Double = 300.0) =
        Bill(id = 1, name = "x", amount = amount, recurrence = rec, dueDate = due.toEpochDay(), anchorDay = due.dayOfMonth, createdAt = 1)

    @Test fun statuses() {
        val today = LocalDate.of(2026, 5, 10)
        assertEquals(BillStatus.OVERDUE, BillLogic.status(bill(today.minusDays(1)), today))
        assertEquals(BillStatus.DUE_SOON, BillLogic.status(bill(today.plusDays(3)), today))
        assertEquals(BillStatus.UPCOMING, BillLogic.status(bill(today.plusDays(4)), today))
        assertEquals("2 gün gecikti", BillLogic.daysText(bill(today.minusDays(2)), today))
    }

    @Test fun nextDueKeepsMonthEnd() {
        var b = bill(LocalDate.of(2026, 1, 31))
        b = BillLogic.afterPayment(b); assertEquals(LocalDate.of(2026, 2, 28), b.due)
        b = BillLogic.afterPayment(b); assertEquals(LocalDate.of(2026, 3, 31), b.due)
        assertEquals(LocalDate.of(2027, 1, 31), BillLogic.afterPayment(bill(LocalDate.of(2026, 1, 31), "YEARLY")).due)
        assertFalse(BillLogic.afterPayment(bill(LocalDate.of(2026, 1, 1), "ONCE")).active)
    }

    @Test fun monthlyCost() {
        val list = listOf(bill(LocalDate.now(), "MONTHLY", 300.0), bill(LocalDate.now(), "YEARLY", 1200.0), bill(LocalDate.now(), "QUARTERLY", 300.0), bill(LocalDate.now(), "ONCE", 9999.0))
        assertEquals(300.0 + 100.0 + 100.0, BillLogic.monthlyCost(list), 1e-9)
    }
}
