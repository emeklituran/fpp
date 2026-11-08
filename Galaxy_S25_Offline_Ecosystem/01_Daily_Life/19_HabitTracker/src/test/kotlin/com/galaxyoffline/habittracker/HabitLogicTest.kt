package com.galaxyoffline.habittracker

import com.galaxyoffline.habittracker.data.Habit
import com.galaxyoffline.habittracker.data.HabitLogic
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitLogicTest {
    private val today = LocalDate.of(2026, 5, 15) // Cuma
    private fun days(vararg back: Int) = back.map { today.minusDays(it.toLong()).toEpochDay() }.toSet()
    private fun habit(mask: Int = 127) = Habit(id = 1, name = "x", daysMask = mask, createdDay = today.minusDays(60).toEpochDay())

    @Test fun streakNotBrokenBeforeTodayIsDone() {
        val s = HabitLogic.stats(habit(), days(1, 2, 3), today)
        assertEquals(3, s.current)
        assertEquals(4, HabitLogic.stats(habit(), days(0, 1, 2, 3), today).current)
    }

    @Test fun gapBreaksStreakAndBestIsKept() {
        val s = HabitLogic.stats(habit(), days(0, 1, 3, 4, 5, 6, 7), today)
        assertEquals(2, s.current); assertEquals(5, s.best); assertEquals(7, s.total)
    }

    @Test fun unscheduledDaysDoNotBreak() {
        // Yalnızca hafta içi: Cuma(0), Per(1), Çar(2), Sal(3), Pzt(4), (Paz 5, Cmt 6 boş), Cum(7)
        val s = HabitLogic.stats(habit(31), days(0, 1, 2, 3, 4, 7), today)
        assertEquals(6, s.current)
    }

    @Test fun rateAndLabels() {
        val h = habit().copy(createdDay = today.minusDays(9).toEpochDay())
        assertEquals(50, HabitLogic.stats(h, days(0, 1, 2, 3, 4), today).rate30)
        assertEquals("Hafta içi", HabitLogic.daysLabel(31))
        assertEquals("Pzt Çar", HabitLogic.daysLabel(0b101))
    }
}
