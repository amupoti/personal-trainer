package com.marcel.personaltrainer.model

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakTest {
    private val dailyActivity = activity(DayOfWeek.entries.toSet())

    @Test
    fun unfinishedTodayPreservesStreakThroughYesterday() {
        val today = LocalDate.of(2026, 7, 8)
        val stats = calculateStreak(
            activities = listOf(dailyActivity),
            completionHistory = completions(today.minusDays(2), today.minusDays(1)),
            today = today,
        )

        assertEquals(2, stats.current)
        assertEquals(2, stats.longest)
        assertTrue(stats.isTodayScheduled)
        assertFalse(stats.isTodayComplete)
    }

    @Test
    fun completedTodayExtendsCurrentStreak() {
        val today = LocalDate.of(2026, 7, 8)
        val stats = calculateStreak(
            activities = listOf(dailyActivity),
            completionHistory = completions(today.minusDays(2), today.minusDays(1), today),
            today = today,
        )

        assertEquals(3, stats.current)
        assertEquals(3, stats.longest)
        assertTrue(stats.isTodayComplete)
    }

    @Test
    fun dayCountsOnlyWhenEveryScheduledActivityIsComplete() {
        val today = LocalDate.of(2026, 7, 8)
        val secondActivity = dailyActivity.copy(id = "second-exercise")
        val stats = calculateStreak(
            activities = listOf(dailyActivity, secondActivity),
            completionHistory = mapOf(today to setOf(dailyActivity.id)),
            today = today,
        )

        assertEquals(0, stats.current)
        assertEquals(0, stats.longest)
        assertFalse(stats.isTodayComplete)
    }

    @Test
    fun missedScheduledDayResetsCurrentButKeepsLongest() {
        val today = LocalDate.of(2026, 7, 8)
        val stats = calculateStreak(
            activities = listOf(dailyActivity),
            completionHistory = completions(today.minusDays(3), today.minusDays(2)),
            today = today,
        )

        assertEquals(0, stats.current)
        assertEquals(2, stats.longest)
    }

    @Test
    fun restDaysNeitherIncrementNorBreakStreak() {
        val today = LocalDate.of(2026, 7, 9)
        val monday = LocalDate.of(2026, 7, 6)
        val wednesday = LocalDate.of(2026, 7, 8)
        val stats = calculateStreak(
            activities = listOf(activity(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))),
            completionHistory = completions(monday, wednesday),
            today = today,
        )

        assertEquals(2, stats.current)
        assertEquals(2, stats.longest)
        assertFalse(stats.isTodayScheduled)
    }

    private fun activity(weekdays: Set<DayOfWeek>) = Activity(
        id = "exercise",
        name = "Exercise",
        weekdays = weekdays,
        targetValue = 1,
        targetUnit = TargetUnit.REPETITIONS,
    )

    private fun completions(vararg dates: LocalDate): Map<LocalDate, Set<String>> =
        dates.associateWith { setOf(dailyActivity.id) }
}
