package com.galaxyoffline.diary

import com.galaxyoffline.diary.data.DiaryEntry
import com.galaxyoffline.diary.data.DiaryStatsCalculator
import com.galaxyoffline.diary.data.Mood
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DiaryStatsTest {
    private val today = LocalDate.of(2026, 3, 10)
    private fun e(daysAgo: Long, mood: Int, text: String = "x") =
        DiaryEntry(id = daysAgo + 1, date = today.minusDays(daysAgo).toEpochDay(), title = "", content = text, mood = mood, createdAt = 0, updatedAt = 0)

    @Test fun streaksAndCounts() {
        val list = listOf(e(0, 5), e(1, 4), e(2, 3), e(5, 1), e(6, 1), e(7, 2), e(8, 2), e(40, 3))
        val st = DiaryStatsCalculator.compute(list, today)
        assertEquals(3, st.currentStreak)
        assertEquals(4, st.longestStreak)
        assertEquals(7, st.thisMonth)
        assertEquals(2, st.moodCounts[Mood.AWFUL])
    }

    @Test fun streakContinuesFromYesterday() {
        assertEquals(2, DiaryStatsCalculator.compute(listOf(e(1, 3), e(2, 3)), today).currentStreak)
        assertEquals(0, DiaryStatsCalculator.compute(listOf(e(3, 3)), today).currentStreak)
    }

    @Test fun search() {
        val list = listOf(e(0, 3, "Deniz kenarı"), e(1, 3, "İş toplantısı"))
        assertEquals(1, DiaryStatsCalculator.search(list, "DENİZ").size)
        assertEquals(1, DiaryStatsCalculator.search(list, "iş").size)
    }
}
