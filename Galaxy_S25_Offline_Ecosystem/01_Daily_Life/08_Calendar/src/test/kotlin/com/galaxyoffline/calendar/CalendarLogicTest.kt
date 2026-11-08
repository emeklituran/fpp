package com.galaxyoffline.calendar

import com.galaxyoffline.calendar.data.CalendarLogic
import com.galaxyoffline.calendar.data.Event
import com.galaxyoffline.calendar.data.EventRepeat
import com.galaxyoffline.calendar.data.TurkishHolidays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarLogicTest {
    private fun ev(id: Long, d: LocalDate, r: EventRepeat = EventRepeat.NONE) = Event(id = id, title = "e$id", date = d.toEpochDay(), repeat = r.name, createdAt = 0)

    @Test fun gridStartsOnMonday() {
        val g = CalendarLogic.monthGrid(YearMonth.of(2026, 11))
        assertEquals(42, g.size)
        assertEquals(DayOfWeek.MONDAY, g.first().dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 26), g.first())
    }

    @Test fun repeats() {
        val from = LocalDate.of(2026, 3, 1); val to = LocalDate.of(2026, 3, 31)
        val weekly = CalendarLogic.occurrences(listOf(ev(1, LocalDate.of(2026, 1, 5), EventRepeat.WEEKLY)), from, to)
        assertEquals(listOf(2, 9, 16, 23, 30), weekly.map { it.date.dayOfMonth })
        val monthly = CalendarLogic.occurrences(listOf(ev(2, LocalDate.of(2025, 12, 15), EventRepeat.MONTHLY)), from, to)
        assertEquals(listOf(LocalDate.of(2026, 3, 15)), monthly.map { it.date })
        val yearly = CalendarLogic.occurrences(listOf(ev(3, LocalDate.of(1990, 3, 8), EventRepeat.YEARLY)), from, to)
        assertEquals(listOf(LocalDate.of(2026, 3, 8)), yearly.map { it.date })
        assertTrue(CalendarLogic.occurrences(listOf(ev(4, LocalDate.of(2026, 4, 1))), from, to).isEmpty())
    }

    @Test fun holidays() {
        val h = TurkishHolidays.forYear(2026)
        assertEquals("Cumhuriyet Bayramı", h[LocalDate.of(2026, 10, 29)])
        assertEquals(7, h.size)
        assertEquals(6, TurkishHolidays.forYear(2016).size)
    }
}
