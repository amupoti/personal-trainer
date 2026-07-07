package com.marcel.personaltrainer.model

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseStatsTest {
    @Test
    fun calculateExerciseStatsCountsCompletionsAcrossDates() {
        val first = activity("first")
        val second = activity("second")
        val dates = (0L..6L).map { LocalDate.of(2026, 7, 6).plusDays(it) }

        val stats = calculateExerciseStats(
            activities = listOf(first, second),
            completionHistory = mapOf(
                dates[0] to setOf(first.id, second.id),
                dates[1] to setOf(first.id),
                dates[2] to setOf(first.id),
            ),
            dates = dates,
        )

        assertEquals(listOf("first", "second"), stats.map { it.activity.id })
        assertEquals(3, stats[0].completedCount)
        assertEquals(1, stats[1].completedCount)
        assertEquals(75f, stats[0].percentage, 0.001f)
        assertEquals(25f, stats[1].percentage, 0.001f)
    }

    @Test
    fun calculateExerciseStatsIgnoresUnknownActivityIds() {
        val known = activity("known")
        val date = LocalDate.of(2026, 7, 6)

        val stats = calculateExerciseStats(
            activities = listOf(known),
            completionHistory = mapOf(date to setOf(known.id, "deleted")),
            dates = listOf(date),
        )

        assertEquals(1, stats.size)
        assertEquals("known", stats.single().activity.id)
        assertEquals(1, stats.single().completedCount)
        assertEquals(100f, stats.single().percentage, 0.001f)
    }

    @Test
    fun calculateExerciseStatsReturnsEmptyListWhenThereAreNoCompletions() {
        val stats = calculateExerciseStats(
            activities = listOf(activity("first")),
            completionHistory = emptyMap(),
            dates = listOf(LocalDate.of(2026, 7, 6)),
        )

        assertTrue(stats.isEmpty())
    }

    private fun activity(id: String) = Activity(
        id = id,
        name = id,
        weekdays = setOf(DayOfWeek.MONDAY),
        targetValue = 1,
        targetUnit = TargetUnit.REPETITIONS,
    )
}
