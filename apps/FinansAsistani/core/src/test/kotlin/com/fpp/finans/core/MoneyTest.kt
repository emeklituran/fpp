package com.fpp.finans.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesTurkishAndEnglishFormats() {
        assertEquals(1500.0, Money.parse("1500")!!, 0.0001)
        assertEquals(1500.75, Money.parse("1500,75")!!, 0.0001)
        assertEquals(1500.0, Money.parse("1.500")!!, 0.0001)
        assertEquals(1234567.89, Money.parse("1.234.567,89")!!, 0.0001)
        assertEquals(1500.75, Money.parse("1500.75")!!, 0.0001)
        assertEquals(1500.75, Money.parse("1,500.75")!!, 0.0001)
        assertEquals(2000.0, Money.parse("₺ 2.000")!!, 0.0001)
        assertEquals(42.5, Money.parse("%42,5")!!, 0.0001)
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("1.2.3"))
        assertNull(Money.parse(null))
    }

    @Test
    fun formatsTurkishCurrency() {
        assertEquals("1.234,50 ₺", Money.format(1234.5))
        assertEquals("0,00 ₺", Money.format(-0.001))
        assertEquals("%40", Money.formatPercent(40.0))
        assertEquals("1500,5", Money.formatInput(1500.5))
        assertEquals("2 yıl 3 ay", Dates.formatDuration(27))
    }
}
