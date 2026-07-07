package com.marcel.personaltrainer.model

import java.time.LocalDate

data class WeeklyProgress(
    val completedCount: Int,
    val targetCount: Int,
)

fun calculateWeeklyProgress(
    activities: List<Activity>,
    completionHistory: Map<LocalDate, Set<String>>,
    date: LocalDate,
): WeeklyProgress {
    val weekStart = date.minusDays((date.dayOfWeek.value - 1).toLong())
    var completedCount = 0
    var targetCount = 0

    activities.forEach { activity ->
        val target = activity.weekdays.size
        val completed = (0L..6L).count { offset ->
            activity.id in completionHistory[weekStart.plusDays(offset)].orEmpty()
        }
        completedCount += minOf(completed, target)
        targetCount += target
    }

    return WeeklyProgress(completedCount, targetCount)
}
