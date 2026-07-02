package com.marcel.personaltrainer.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class ReminderSettings(
    val enabled: Boolean = false,
    val firstTime: LocalTime = LocalTime.of(9, 0),
    val secondTime: LocalTime = LocalTime.of(18, 0),
)

fun remainingActivityCount(
    activities: List<Activity>,
    completedIds: Set<String>,
    date: LocalDate,
): Int = activities.count { activity ->
    activity.isScheduledOn(date.dayOfWeek) && activity.id !in completedIds
}

fun delayUntilReminder(
    time: LocalTime,
    now: ZonedDateTime = ZonedDateTime.now(),
): Long {
    var next = now.toLocalDate().atTime(time).atZone(now.zone)
    if (!next.isAfter(now)) {
        next = next.plusDays(1)
    }
    return ChronoUnit.MILLIS.between(now, next)
}
