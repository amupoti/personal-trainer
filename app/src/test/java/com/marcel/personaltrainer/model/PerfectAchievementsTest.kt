package com.marcel.personaltrainer.model

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerfectAchievementsTest {
    @Test
    fun calculatePerfectAchievementsCountsPerfectDays() {
        val first = activity("first", setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        val second = activity("second", setOf(DayOfWeek.MONDAY))
        val monday = LocalDate.of(2026, 7, 6)
        val tuesday = monday.plusDays(1)

        val achievements = calculatePerfectAchievements(
            activities = listOf(first, second),
            completionHistory = mapOf(
                monday to setOf(first.id, second.id),
                tuesday to setOf(first.id),
            ),
            today = tuesday,
        )

        assertEquals(2, achievements.perfectDayCount)
        assertTrue(achievements.isTodayPerfect)
    }

    @Test
    fun calculatePerfectAchievementsIgnoresIncompleteDays() {
        val first = activity("first", setOf(DayOfWeek.MONDAY))
        val second = activity("second", setOf(DayOfWeek.MONDAY))
        val monday = LocalDate.of(2026, 7, 6)

        val achievements = calculatePerfectAchievements(
            activities = listOf(first, second),
            completionHistory = mapOf(
                monday to setOf(first.id),
            ),
            today = monday,
        )

        assertEquals(0, achievements.perfectDayCount)
        assertFalse(achievements.isTodayPerfect)
    }

    @Test
    fun calculatePerfectAchievementsCountsPerfectWeeks() {
        val activity = activity("daily", DayOfWeek.entries.toSet())
        val weekStart = LocalDate.of(2026, 7, 6)
        val history = (0L..6L).associate { offset ->
            weekStart.plusDays(offset) to setOf(activity.id)
        }

        val achievements = calculatePerfectAchievements(
            activities = listOf(activity),
            completionHistory = history,
            today = weekStart.plusDays(6),
        )

        assertEquals(1, achievements.perfectWeekCount)
        assertTrue(achievements.isThisWeekPerfect)
    }

    @Test
    fun calculatePerfectAchievementsIgnoresFutureCompletions() {
        val activity = activity("daily", DayOfWeek.entries.toSet())
        val today = LocalDate.of(2026, 7, 6)

        val achievements = calculatePerfectAchievements(
            activities = listOf(activity),
            completionHistory = mapOf(
                today.plusDays(1) to setOf(activity.id),
            ),
            today = today,
        )

        assertEquals(0, achievements.perfectDayCount)
        assertEquals(0, achievements.perfectWeekCount)
        assertFalse(achievements.isTodayPerfect)
        assertFalse(achievements.isThisWeekPerfect)
    }

    private fun activity(id: String, weekdays: Set<DayOfWeek>) = Activity(
        id = id,
        name = id,
        weekdays = weekdays,
        targetValue = 1,
        targetUnit = TargetUnit.REPETITIONS,
    )
}
