package com.marcel.personaltrainer.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSettingsTest {
    @Test
    fun remainingCountOnlyIncludesScheduledIncompleteActivities() {
        val activities = listOf(
            activity("complete", DayOfWeek.MONDAY),
            activity("remaining", DayOfWeek.MONDAY),
            activity("tomorrow", DayOfWeek.TUESDAY),
        )

        assertEquals(
            1,
            remainingActivityCount(
                activities = activities,
                completedIds = setOf("complete"),
                date = LocalDate.of(2026, 6, 29),
            ),
        )
    }

    @Test
    fun reminderDelayUsesNextOccurrence() {
        val now = ZonedDateTime.of(
            2026,
            6,
            29,
            10,
            0,
            0,
            0,
            ZoneId.of("UTC"),
        )

        assertEquals(
            23 * 60 * 60 * 1_000L,
            delayUntilReminder(LocalTime.of(9, 0), now),
        )
    }

    private fun activity(id: String, day: DayOfWeek) = Activity(
        id = id,
        name = id,
        weekdays = setOf(day),
        targetValue = 1,
        targetUnit = TargetUnit.REPETITIONS,
    )
}
