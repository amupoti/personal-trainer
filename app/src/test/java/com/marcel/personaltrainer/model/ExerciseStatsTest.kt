package com.marcel.personaltrainer.model

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseStatsTest {
    @Test
    fun calculateExerciseStatsCountsScheduledAdherenceAcrossDates() {
        val first = activity("first", setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY))
        val second = activity("second", setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        val dates = (0L..6L).map { LocalDate.of(2026, 7, 6).plusDays(it) }

        val insights = calculateExerciseStats(
            activities = listOf(first, second),
            completionHistory = mapOf(
                dates[0] to setOf(first.id, second.id),
                dates[1] to setOf(first.id),
                dates[2] to setOf(first.id),
            ),
            dates = dates,
        )

        assertEquals(4, insights.completedCount)
        assertEquals(5, insights.scheduledCount)
        assertEquals(80f, insights.percentage, 0.001f)
        assertEquals(listOf("second", "first"), insights.exerciseStats.map { it.activity.id })
        assertEquals(1, insights.exerciseStats[0].completedCount)
        assertEquals(2, insights.exerciseStats[0].scheduledCount)
        assertEquals(50f, insights.exerciseStats[0].percentage, 0.001f)
        assertEquals(3, insights.exerciseStats[1].completedCount)
        assertEquals(3, insights.exerciseStats[1].scheduledCount)
        assertEquals(100f, insights.exerciseStats[1].percentage, 0.001f)
    }

    @Test
    fun calculateExerciseStatsIgnoresUnknownAndUnscheduledCompletions() {
        val known = activity("known", setOf(DayOfWeek.MONDAY))
        val unscheduled = activity("unscheduled", setOf(DayOfWeek.TUESDAY))
        val date = LocalDate.of(2026, 7, 6)

        val insights = calculateExerciseStats(
            activities = listOf(known, unscheduled),
            completionHistory = mapOf(date to setOf(known.id, unscheduled.id, "deleted")),
            dates = listOf(date),
        )

        assertEquals(1, insights.exerciseStats.size)
        assertEquals("known", insights.exerciseStats.single().activity.id)
        assertEquals(1, insights.completedCount)
        assertEquals(1, insights.scheduledCount)
        assertEquals(100f, insights.percentage, 0.001f)
    }

    @Test
    fun calculateExerciseStatsReturnsEmptyInsightsWhenThereAreNoScheduledExercises() {
        val insights = calculateExerciseStats(
            activities = listOf(activity("first", setOf(DayOfWeek.TUESDAY))),
            completionHistory = emptyMap(),
            dates = listOf(LocalDate.of(2026, 7, 6)),
        )

        assertEquals(0, insights.completedCount)
        assertEquals(0, insights.scheduledCount)
        assertTrue(insights.exerciseStats.isEmpty())
        assertNull(insights.bestWeekday)
        assertNull(insights.weakestWeekday)
    }

    @Test
    fun calculateExerciseStatsFindsBestAndWeakestWeekdays() {
        val activity = activity("daily", DayOfWeek.entries.toSet())
        val dates = (0L..2L).map { LocalDate.of(2026, 7, 6).plusDays(it) }

        val insights = calculateExerciseStats(
            activities = listOf(activity),
            completionHistory = mapOf(
                dates[0] to setOf(activity.id),
                dates[1] to emptySet(),
                dates[2] to setOf(activity.id),
            ),
            dates = dates,
        )

        assertEquals(DayOfWeek.MONDAY, insights.bestWeekday?.dayOfWeek)
        assertEquals(100f, insights.bestWeekday?.percentage ?: 0f, 0.001f)
        assertEquals(DayOfWeek.TUESDAY, insights.weakestWeekday?.dayOfWeek)
        assertEquals(0f, insights.weakestWeekday?.percentage ?: 0f, 0.001f)
    }

    @Test
    fun calculateExerciseStatsSortsByCompletenessRatioBeforeCount() {
        val lowerRatio = activity("lower", DayOfWeek.entries.toSet())
        val higherRatio = activity("higher", setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        val dates = (0L..6L).map { LocalDate.of(2026, 7, 6).plusDays(it) }

        val insights = calculateExerciseStats(
            activities = listOf(higherRatio, lowerRatio),
            completionHistory = mapOf(
                dates[0] to setOf(lowerRatio.id, higherRatio.id),
                dates[1] to setOf(lowerRatio.id, higherRatio.id),
                dates[2] to setOf(lowerRatio.id),
            ),
            dates = dates,
        )

        assertEquals(listOf("lower", "higher"), insights.exerciseStats.map { it.activity.id })
        assertEquals(3, insights.exerciseStats[0].completedCount)
        assertEquals(7, insights.exerciseStats[0].scheduledCount)
        assertEquals(2, insights.exerciseStats[1].completedCount)
        assertEquals(2, insights.exerciseStats[1].scheduledCount)
    }

    @Test
    fun calculateExerciseStatsIncludesTodayWhenItIsInDates() {
        val activity = activity("today", setOf(DayOfWeek.WEDNESDAY))
        val today = LocalDate.of(2026, 7, 8)

        val insights = calculateExerciseStats(
            activities = listOf(activity),
            completionHistory = mapOf(today to setOf(activity.id)),
            dates = listOf(today),
        )

        assertEquals(1, insights.completedCount)
        assertEquals(1, insights.scheduledCount)
        assertEquals(100f, insights.percentage, 0.001f)
        assertEquals("today", insights.exerciseStats.single().activity.id)
    }

    @Test
    fun calculateCompletionTrendReturnsUpWhenCurrentPercentageIsHigher() {
        val trend = calculateCompletionTrend(
            current = insights(completedCount = 4, scheduledCount = 5),
            previous = insights(completedCount = 2, scheduledCount = 5),
        )

        assertEquals(CompletionTrendDirection.UP, trend.direction)
        assertEquals(40, trend.percentagePointChange)
    }

    @Test
    fun calculateCompletionTrendReturnsDownWhenCurrentPercentageIsLower() {
        val trend = calculateCompletionTrend(
            current = insights(completedCount = 1, scheduledCount = 4),
            previous = insights(completedCount = 3, scheduledCount = 4),
        )

        assertEquals(CompletionTrendDirection.DOWN, trend.direction)
        assertEquals(50, trend.percentagePointChange)
    }

    @Test
    fun calculateCompletionTrendReturnsUnchangedWhenPercentagesMatch() {
        val trend = calculateCompletionTrend(
            current = insights(completedCount = 1, scheduledCount = 2),
            previous = insights(completedCount = 2, scheduledCount = 4),
        )

        assertEquals(CompletionTrendDirection.UNCHANGED, trend.direction)
        assertEquals(0, trend.percentagePointChange)
    }

    private fun insights(
        completedCount: Int,
        scheduledCount: Int,
    ) = ExerciseInsights(
        completedCount = completedCount,
        scheduledCount = scheduledCount,
        exerciseStats = emptyList(),
        bestWeekday = null,
        weakestWeekday = null,
    )

    private fun activity(id: String, weekdays: Set<DayOfWeek>) = Activity(
        id = id,
        name = id,
        weekdays = weekdays,
        targetValue = 1,
        targetUnit = TargetUnit.REPETITIONS,
    )
}
