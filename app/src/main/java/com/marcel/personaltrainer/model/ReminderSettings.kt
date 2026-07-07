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

data class ReminderNotificationSummary(
    val remainingActivities: List<Activity> = emptyList(),
    val totalRemainingCount: Int = 0,
    val isGeneralReminder: Boolean = false,
) {
    val extraRemainingCount: Int
        get() = totalRemainingCount - remainingActivities.size
}

fun remainingActivityCount(
    activities: List<Activity>,
    completedIds: Set<String>,
    date: LocalDate,
): Int = activities.count { activity ->
    activity.isScheduledOn(date.dayOfWeek) && activity.id !in completedIds
}

fun reminderNotificationSummary(
    activities: List<Activity>,
    completedIds: Set<String>,
    date: LocalDate,
    maxActivityNames: Int = 3,
): ReminderNotificationSummary? {
    val scheduledActivities = activities.filter { it.isScheduledOn(date.dayOfWeek) }
    if (scheduledActivities.isEmpty()) {
        return ReminderNotificationSummary(isGeneralReminder = true)
    }

    val remainingActivities = scheduledActivities.filterNot { it.id in completedIds }
    if (remainingActivities.isEmpty()) return null

    return ReminderNotificationSummary(
        remainingActivities = remainingActivities.take(maxActivityNames),
        totalRemainingCount = remainingActivities.size,
    )
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
