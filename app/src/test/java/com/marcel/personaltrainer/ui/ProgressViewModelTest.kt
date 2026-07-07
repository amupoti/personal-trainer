package com.marcel.personaltrainer.ui

import com.marcel.personaltrainer.model.Activity
import com.marcel.personaltrainer.model.TargetUnit
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressViewModelTest {
    @Test
    fun dayProgressCountsCompletedUnscheduledExercisesAboveScheduledTarget() {
        val date = LocalDate.of(2026, 7, 6)
        val scheduled = listOf(
            activity("scheduled-1", setOf(DayOfWeek.MONDAY)),
            activity("scheduled-2", setOf(DayOfWeek.MONDAY)),
        )
        val extra = listOf(
            activity("extra-1", setOf(DayOfWeek.TUESDAY)),
            activity("extra-2", setOf(DayOfWeek.WEDNESDAY)),
            activity("extra-3", setOf(DayOfWeek.THURSDAY)),
        )

        val progress = dayProgress(
            date = date,
            activities = scheduled + extra,
            completedIds = (scheduled + extra).map(Activity::id).toSet(),
        )

        assertEquals(5, progress.completedCount)
        assertEquals(2, progress.targetCount)
        assertEquals(
            listOf("scheduled-1", "scheduled-2", "extra-1", "extra-2", "extra-3"),
            progress.completedActivities.map(Activity::id),
        )
    }

    @Test
    fun dayProgressRecordsOnlyCompletedActivitiesForSelectedDay() {
        val date = LocalDate.of(2026, 7, 6)
        val scheduled = activity("scheduled", setOf(DayOfWeek.MONDAY))
        val completedExtra = activity("completed-extra", setOf(DayOfWeek.TUESDAY))
        val incompleteExtra = activity("incomplete-extra", setOf(DayOfWeek.WEDNESDAY))

        val progress = dayProgress(
            date = date,
            activities = listOf(scheduled, completedExtra, incompleteExtra),
            completedIds = setOf(scheduled.id, completedExtra.id),
        )

        assertEquals(2, progress.completedCount)
        assertEquals(1, progress.targetCount)
        assertEquals(
            listOf("scheduled", "completed-extra"),
            progress.completedActivities.map(Activity::id),
        )
    }

    private fun activity(id: String, weekdays: Set<DayOfWeek>) = Activity(
        id = id,
        name = id,
        weekdays = weekdays,
        targetValue = 1,
        targetUnit = TargetUnit.REPETITIONS,
    )
}
