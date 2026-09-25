package com.example

import com.example.data.RecurrenceHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class RecurrenceHelperTest {

    @Test
    fun testDailyRecurrence() {
        val next = RecurrenceHelper.computeNextDueDate("2026-09-23", RecurrenceHelper.DAILY)
        assertEquals("2026-09-24", next)
    }

    @Test
    fun testWeeklyRecurrence() {
        val next = RecurrenceHelper.computeNextDueDate("2026-09-23", RecurrenceHelper.WEEKLY)
        assertEquals("2026-09-30", next)
    }

    @Test
    fun testMonthlyRecurrence() {
        val next = RecurrenceHelper.computeNextDueDate("2026-09-23", RecurrenceHelper.MONTHLY)
        assertEquals("2026-10-23", next)
    }

    @Test
    fun testWeekdayRecurrence() {
        // Friday to Monday
        val next = RecurrenceHelper.computeNextDueDate("2026-09-25", RecurrenceHelper.WEEKDAYS)
        assertEquals("2026-09-28", next)
    }

    @Test
    fun testCustomRecurrence() {
        val next = RecurrenceHelper.computeNextDueDate("2026-09-23", "CUSTOM:3:DAYS")
        assertEquals("2026-09-26", next)

        val nextWeeks = RecurrenceHelper.computeNextDueDate("2026-09-23", "CUSTOM:2:WEEKS")
        assertEquals("2026-10-07", nextWeeks)
    }

    @Test
    fun testFormatRuleLabels() {
        assertEquals("Hàng ngày", RecurrenceHelper.formatRuleLabel(RecurrenceHelper.DAILY))
        assertEquals("Ngày trong tuần (T2 - T6)", RecurrenceHelper.formatRuleLabel(RecurrenceHelper.WEEKDAYS))
        assertEquals("Hàng tuần", RecurrenceHelper.formatRuleLabel(RecurrenceHelper.WEEKLY))
        assertEquals("Hàng tháng", RecurrenceHelper.formatRuleLabel(RecurrenceHelper.MONTHLY))
        assertEquals("Mỗi 3 ngày", RecurrenceHelper.formatRuleLabel("CUSTOM:3:DAYS"))
    }
}
