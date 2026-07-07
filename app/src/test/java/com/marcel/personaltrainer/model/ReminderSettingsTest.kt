package com.marcel.personaltrainer.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun reminderSummaryIncludesOnlyScheduledIncompleteActivities() {
        val activities = listOf(
            activity("complete", DayOfWeek.MONDAY),
            activity("remaining", DayOfWeek.MONDAY),
            activity("tomorrow", DayOfWeek.TUESDAY),
        )

        val summary = reminderNotificationSummary(
            activities = activities,
            completedIds = setOf("complete"),
            date = LocalDate.of(2026, 6, 29),
        )

        assertEquals(1, summary?.totalRemainingCount)
        assertEquals(listOf("remaining"), summary?.remainingActivities?.map(Activity::id))
        assertEquals(0, summary?.extraRemainingCount)
    }

    @Test
    fun reminderSummaryLimitsVisibleActivityNamesAndCountsExtras() {
        val activities = listOf(
            activity("first", DayOfWeek.MONDAY),
            activity("second", DayOfWeek.MONDAY),
            activity("third", DayOfWeek.MONDAY),
            activity("fourth", DayOfWeek.MONDAY),
            activity("fifth", DayOfWeek.MONDAY),
        )

        val summary = reminderNotificationSummary(
            activities = activities,
            completedIds = emptySet(),
            date = LocalDate.of(2026, 6, 29),
        )

        assertEquals(5, summary?.totalRemainingCount)
        assertEquals(
            listOf("first", "second", "third"),
            summary?.remainingActivities?.map(Activity::id),
        )
        assertEquals(2, summary?.extraRemainingCount)
    }

    @Test
    fun reminderSummarySkipsNotificationWhenScheduledActivitiesAreComplete() {
        val activities = listOf(
            activity("complete", DayOfWeek.MONDAY),
            activity("also-complete", DayOfWeek.MONDAY),
        )

        val summary = reminderNotificationSummary(
            activities = activities,
            completedIds = activities.map(Activity::id).toSet(),
            date = LocalDate.of(2026, 6, 29),
        )

        assertNull(summary)
    }

    @Test
    fun reminderSummaryUsesGeneralReminderWhenNothingIsScheduled() {
        val summary = reminderNotificationSummary(
            activities = listOf(activity("tomorrow", DayOfWeek.TUESDAY)),
            completedIds = emptySet(),
            date = LocalDate.of(2026, 6, 29),
        )

        assertTrue(summary?.isGeneralReminder == true)
        assertEquals(0, summary?.totalRemainingCount)
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
