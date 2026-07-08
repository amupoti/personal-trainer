package com.marcel.personaltrainer.model

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestoneBadgeTest {
    @Test
    fun calculateMilestoneBadgesUnlocksReachedThresholds() {
        val activity = activity("daily", DayOfWeek.entries.toSet())
        val start = LocalDate.of(2026, 7, 1)
        val history = (0L until 8L).associate { offset ->
            start.plusDays(offset) to setOf(activity.id)
        }

        val badges = calculateMilestoneBadges(
            activities = listOf(activity),
            completionHistory = history,
            today = start.plusDays(7),
        )

        assertEquals(listOf(7, 30, 100), badges.map(MilestoneBadge::completedDayTarget))
        assertTrue(badges[0].isUnlocked)
        assertEquals(0, badges[0].remainingCount)
        assertFalse(badges[1].isUnlocked)
        assertEquals(22, badges[1].remainingCount)
        assertFalse(badges[2].isUnlocked)
        assertEquals(92, badges[2].remainingCount)
    }

    @Test
    fun calculateMilestoneBadgesCountsOnlyCompleteScheduledDays() {
        val first = activity("first", setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        val second = activity("second", setOf(DayOfWeek.MONDAY))
        val monday = LocalDate.of(2026, 7, 6)
        val tuesday = monday.plusDays(1)

        val badges = calculateMilestoneBadges(
            activities = listOf(first, second),
            completionHistory = mapOf(
                monday to setOf(first.id),
                tuesday to setOf(first.id),
            ),
            today = tuesday,
        )

        assertEquals(1, badges.first().completedDayCount)
        assertEquals(6, badges.first().remainingCount)
    }

    @Test
    fun calculateMilestoneBadgesIgnoresFutureCompletions() {
        val activity = activity("daily", DayOfWeek.entries.toSet())
        val today = LocalDate.of(2026, 7, 6)

        val badges = calculateMilestoneBadges(
            activities = listOf(activity),
            completionHistory = mapOf(
                today.plusDays(1) to setOf(activity.id),
            ),
            today = today,
        )

        assertEquals(0, badges.first().completedDayCount)
        assertEquals(7, badges.first().remainingCount)
    }

    private fun activity(id: String, weekdays: Set<DayOfWeek>) = Activity(
        id = id,
        name = id,
        weekdays = weekdays,
        targetValue = 1,
        targetUnit = TargetUnit.REPETITIONS,
    )
}
