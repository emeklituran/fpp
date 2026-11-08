package com.galaxyoffline.reminder

import com.galaxyoffline.reminder.data.Repeat
import com.galaxyoffline.reminder.data.RepeatLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class RepeatLogicTest {
    private val anchor = LocalDateTime.of(2026, 1, 31, 9, 0)

    @Test fun oneTime() {
        assertEquals(anchor, RepeatLogic.nextAfter(anchor, Repeat.NONE, anchor.minusMinutes(1)))
        assertNull(RepeatLogic.nextAfter(anchor, Repeat.NONE, anchor))
    }

    @Test fun dailyWeeklyYearly() {
        assertEquals(LocalDateTime.of(2026, 3, 11, 9, 0), RepeatLogic.nextAfter(anchor, Repeat.DAILY, LocalDateTime.of(2026, 3, 10, 9, 0)))
        assertEquals(LocalDateTime.of(2026, 2, 7, 9, 0), RepeatLogic.nextAfter(anchor, Repeat.WEEKLY, LocalDateTime.of(2026, 2, 1, 0, 0)))
        assertEquals(LocalDateTime.of(2031, 1, 31, 9, 0), RepeatLogic.nextAfter(anchor, Repeat.YEARLY, LocalDateTime.of(2030, 6, 1, 0, 0)))
    }

    @Test fun monthlyDoesNotDriftFromMonthEnd() {
        // 31 Ocak -> 28 Şubat -> 31 Mart (çapadan hesaplandığı için kaymaz)
        assertEquals(LocalDateTime.of(2026, 2, 28, 9, 0), RepeatLogic.nextAfter(anchor, Repeat.MONTHLY, LocalDateTime.of(2026, 2, 1, 0, 0)))
        assertEquals(LocalDateTime.of(2026, 3, 31, 9, 0), RepeatLogic.nextAfter(anchor, Repeat.MONTHLY, LocalDateTime.of(2026, 3, 1, 0, 0)))
    }
}
