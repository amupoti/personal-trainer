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
}
