package com.marcel.personaltrainer.model

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityTest {
    @Test
    fun activityIsOnlyScheduledOnSelectedDays() {
        val activity = Activity(
            id = "hamstrings",
            name = "Hamstring stretch",
            weekdays = setOf(DayOfWeek.MONDAY),
            targetValue = 30,
            targetUnit = TargetUnit.SECONDS,
        )

        assertTrue(activity.isScheduledOn(DayOfWeek.MONDAY))
        assertFalse(activity.isScheduledOn(DayOfWeek.TUESDAY))
        assertEquals("30 seconds", activity.description)
    }

    @Test
    fun videoLinkMustUseHttpOrHttps() {
        assertTrue(isValidVideoUrl(""))
        assertTrue(isValidVideoUrl("https://example.com/video"))
        assertFalse(isValidVideoUrl("example.com/video"))
        assertFalse(isValidVideoUrl("javascript:alert(1)"))
    }

    @Test
    fun editingActivityPreservesIdentityAndUpdatesFields() {
        val activity = Activity(
            id = "hamstrings",
            name = "Hamstring stretch",
            weekdays = setOf(DayOfWeek.MONDAY),
            targetValue = 30,
            targetUnit = TargetUnit.SECONDS,
            usesLocalizedName = true,
        )

        val edited = activity.edited(
            name = " Seated hamstring stretch ",
            weekdays = setOf(DayOfWeek.TUESDAY),
            targetValue = 12,
            targetUnit = TargetUnit.REPETITIONS,
            videoUrl = " https://example.com/stretch ",
        )

        assertEquals(activity.id, edited.id)
        assertEquals("Seated hamstring stretch", edited.name)
        assertEquals(setOf(DayOfWeek.TUESDAY), edited.weekdays)
        assertEquals(12, edited.targetValue)
        assertEquals(TargetUnit.REPETITIONS, edited.targetUnit)
        assertEquals("https://example.com/stretch", edited.videoUrl)
        assertFalse(edited.usesLocalizedName)
    }
}
