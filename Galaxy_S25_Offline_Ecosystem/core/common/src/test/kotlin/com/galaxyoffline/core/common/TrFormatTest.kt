package com.galaxyoffline.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

class TrFormatTest {
    @Test fun parsesDecimals() {
        assertEquals(1500.75, TrFormat.parseDecimal("1.500,75")!!, 1e-9)
        assertEquals(1500.75, TrFormat.parseDecimal("1500.75")!!, 1e-9)
        assertEquals(1.5, TrFormat.parseDecimal("1,5")!!, 1e-9)
        assertEquals(1500.0, TrFormat.parseDecimal("1.500")!!, 1e-9)
        assertNull(TrFormat.parseDecimal("abc"))
        assertNull(TrFormat.parseDecimal(""))
    }

    @Test fun formats() {
        assertEquals("1.234,5", TrFormat.number(1234.5))
        assertEquals("1.234,50 ₺", TrFormat.money(1234.5))
        assertEquals("01:05,30", TrFormat.stopwatch(65_300))
        assertEquals("1:00:00,00", TrFormat.stopwatch(3_600_000))
        assertEquals("2 saat 5 dakika", TrFormat.humanDuration(Duration.ofMinutes(125)))
        assertEquals(3, TrFormat.wordCount("  bir iki\nüç "))
        val today = LocalDate.of(2026, 1, 10)
        assertEquals("Bugün", TrFormat.relativeDay(today, today))
        assertEquals("Yarın", TrFormat.relativeDay(today.plusDays(1), today))
    }
}
