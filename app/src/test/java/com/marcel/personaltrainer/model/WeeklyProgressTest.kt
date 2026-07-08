package com.marcel.personaltrainer.model

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyProgressTest {
    @Test
    fun completionsOnAnyDayCountTowardWeeklyActivityTarget() {
        val activity = activity(
            id = "stretch",
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )
        val weekStart = LocalDate.of(2026, 7, 6)

        val progress = calculateWeeklyProgress(
            activities = listOf(activity),
            completionHistory = mapOf(
                weekStart.plusDays(1) to setOf(activity.id),
                weekStart.plusDays(3) to setOf(activity.id),
            ),
            date = weekStart,
        )

        assertEquals(2, progress.completedCount)
        assertEquals(3, progress.targetCount)
    }

    @Test
    fun extraCompletionsDoNotExceedWeeklyActivityTarget() {
        val activity = activity(
            id = "bridge",
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
        )
        val weekStart = LocalDate.of(2026, 7, 6)

        val progress = calculateWeeklyProgress(
            activities = listOf(activity),
            completionHistory = (0L..6L).associate { offset ->
                weekStart.plusDays(offset) to setOf(activity.id)
            },
            date = weekStart.plusDays(2),
        )

        assertEquals(2, progress.completedCount)
        assertEquals(2, progress.targetCount)
        assertEquals(0, progress.remainingCount)
        assertEquals(100f, progress.percentage, 0.001f)
        assertEquals(true, progress.isComplete)
    }

    @Test
    fun weeklyProgressSummarizesRemainingGoal() {
        val progress = WeeklyProgress(
            completedCount = 3,
            targetCount = 5,
        )

        assertEquals(2, progress.remainingCount)
        assertEquals(60f, progress.percentage, 0.001f)
        assertEquals(false, progress.isComplete)
    }

    @Test
    fun weeklyProgressHandlesNoTarget() {
        val progress = WeeklyProgress(
            completedCount = 0,
            targetCount = 0,
        )

        assertEquals(0, progress.remainingCount)
        assertEquals(0f, progress.percentage, 0.001f)
        assertEquals(false, progress.isComplete)
    }

    private fun activity(id: String, weekdays: Set<DayOfWeek>) = Activity(
        id = id,
        name = "Exercise",
        weekdays = weekdays,
        targetValue = 1,
        targetUnit = TargetUnit.REPETITIONS,
    )
}
