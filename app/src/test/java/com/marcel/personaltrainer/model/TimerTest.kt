package com.marcel.personaltrainer.model

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerTest {
    @Test
    fun durationTargetsConvertToSeconds() {
        assertEquals(30L, activity(TargetUnit.SECONDS, 30).timerDurationSeconds())
        assertEquals(120L, activity(TargetUnit.MINUTES, 2).timerDurationSeconds())
        assertNull(activity(TargetUnit.REPETITIONS, 10).timerDurationSeconds())
    }

    @Test
    fun timerUsesMinuteSecondFormat() {
        assertEquals("0:05", formatTimer(5))
        assertEquals("2:00", formatTimer(120))
        assertEquals("61:01", formatTimer(3661))
    }

    private fun activity(unit: TargetUnit, value: Int) =
        Activity(
            id = "timer",
            name = "Timer exercise",
            weekdays = setOf(DayOfWeek.MONDAY),
            targetValue = value,
            targetUnit = unit,
        )
}
