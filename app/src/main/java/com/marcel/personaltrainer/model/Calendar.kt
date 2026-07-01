package com.marcel.personaltrainer.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class CalendarPeriod {
    WEEK,
    MONTH,
}

fun datesForPeriod(
    anchorDate: LocalDate,
    period: CalendarPeriod,
): List<LocalDate> =
    when (period) {
        CalendarPeriod.WEEK -> {
            val firstDay = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            (0L..6L).map(firstDay::plusDays)
        }

        CalendarPeriod.MONTH -> {
            val firstDay = anchorDate.withDayOfMonth(1)
            (0L until firstDay.lengthOfMonth()).map(firstDay::plusDays)
        }
    }
