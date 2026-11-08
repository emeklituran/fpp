package com.galaxyoffline.alarm

import com.galaxyoffline.alarm.data.Alarm
import com.galaxyoffline.alarm.data.AlarmLogic
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class AlarmLogicTest {
    // 2026-11-09 Pazartesi
    private val monday8 = LocalDateTime.of(2026, 11, 9, 8, 0)
    private fun a(h: Int, m: Int, days: Int = 0) = Alarm(hour = h, minute = m, days = days, createdAt = 0)

    @Test fun oneTime() {
        assertEquals(LocalDateTime.of(2026, 11, 9, 9, 30), AlarmLogic.nextTrigger(a(9, 30), monday8))
        assertEquals(LocalDateTime.of(2026, 11, 10, 7, 0), AlarmLogic.nextTrigger(a(7, 0), monday8))
        assertEquals(LocalDateTime.of(2026, 11, 10, 8, 0), AlarmLogic.nextTrigger(a(8, 0), monday8)) // tam şu an -> yarın
    }

    @Test fun weekdaysAndWeekend() {
        assertEquals(LocalDateTime.of(2026, 11, 10, 7, 0), AlarmLogic.nextTrigger(a(7, 0, AlarmLogic.WEEKDAYS), monday8))
        assertEquals(LocalDateTime.of(2026, 11, 14, 10, 0), AlarmLogic.nextTrigger(a(10, 0, AlarmLogic.WEEKEND), monday8))
        val friday23 = LocalDateTime.of(2026, 11, 13, 23, 0)
        assertEquals(LocalDateTime.of(2026, 11, 16, 7, 0), AlarmLogic.nextTrigger(a(7, 0, AlarmLogic.WEEKDAYS), friday23))
        // Yalnızca pazartesi, şu an pazartesi 08:00, alarm 07:00 -> gelecek pazartesi
        assertEquals(LocalDateTime.of(2026, 11, 16, 7, 0), AlarmLogic.nextTrigger(a(7, 0, 0b1), monday8))
    }

    @Test fun describe() {
        assertEquals("Hafta içi", AlarmLogic.describeDays(AlarmLogic.WEEKDAYS))
        assertEquals("Pzt, Çar", AlarmLogic.describeDays(0b101))
        assertEquals("1 saat 30 dakika sonra", AlarmLogic.untilText(monday8, monday8.plusMinutes(89)))
    }
}
