package com.marcel.personaltrainer.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarTest {
    @Test
    fun weekStartsOnMondayAndContainsSevenDays() {
        val dates = datesForPeriod(
            anchorDate = LocalDate.of(2026, 7, 1),
            period = CalendarPeriod.WEEK,
        )

        assertEquals(LocalDate.of(2026, 6, 29), dates.first())
        assertEquals(LocalDate.of(2026, 7, 5), dates.last())
        assertEquals(7, dates.size)
    }

    @Test
    fun monthContainsEveryDateInLeapFebruary() {
        val dates = datesForPeriod(
            anchorDate = LocalDate.of(2024, 2, 15),
            period = CalendarPeriod.MONTH,
        )

        assertEquals(LocalDate.of(2024, 2, 1), dates.first())
        assertEquals(LocalDate.of(2024, 2, 29), dates.last())
        assertEquals(29, dates.size)
    }
}
