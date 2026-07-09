package com.marcel.personaltrainer.model

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSettingsTest {
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
}
