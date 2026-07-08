package com.marcel.personaltrainer.model

import java.time.DayOfWeek
import java.time.LocalDate

data class ExerciseInsights(
    val completedCount: Int,
    val scheduledCount: Int,
    val exerciseStats: List<ExerciseStat>,
    val bestWeekday: WeekdayStat?,
    val weakestWeekday: WeekdayStat?,
) {
    val percentage: Float
        get() = if (scheduledCount == 0) 0f else completedCount * 100f / scheduledCount
}

data class ExerciseStat(
    val activity: Activity,
    val completedCount: Int,
    val scheduledCount: Int,
    val percentage: Float,
)

data class WeekdayStat(
    val dayOfWeek: DayOfWeek,
    val completedCount: Int,
    val scheduledCount: Int,
) {
    val percentage: Float
        get() = if (scheduledCount == 0) 0f else completedCount * 100f / scheduledCount
}

fun calculateExerciseStats(
    activities: List<Activity>,
    completionHistory: Map<LocalDate, Set<String>>,
    dates: List<LocalDate>,
): ExerciseInsights {
    val activityCounts = activities.associateWith { ActivityCounts() }.toMutableMap()
    val weekdayCounts = DayOfWeek.entries.associateWith { ActivityCounts() }.toMutableMap()

    dates.forEach { date ->
        val completedIds = completionHistory[date].orEmpty()
        activities.forEach { activity ->
            if (activity.isScheduledOn(date.dayOfWeek)) {
                val activityCount = activityCounts.getValue(activity)
                val weekdayCount = weekdayCounts.getValue(date.dayOfWeek)
                activityCount.scheduled += 1
                weekdayCount.scheduled += 1
                if (activity.id in completedIds) {
                    activityCount.completed += 1
                    weekdayCount.completed += 1
                }
            }
        }
    }

    val exerciseStats = activityCounts
        .mapNotNull { (activity, counts) ->
            if (counts.scheduled == 0) {
                null
            } else {
                ExerciseStat(
                    activity = activity,
                    completedCount = counts.completed,
                    scheduledCount = counts.scheduled,
                    percentage = counts.completed * 100f / counts.scheduled,
                )
            }
        }
        .sortedWith(exerciseStatComparator())

    val weekdayStats = weekdayCounts
        .mapNotNull { (dayOfWeek, counts) ->
            if (counts.scheduled == 0) {
                null
            } else {
                WeekdayStat(
                    dayOfWeek = dayOfWeek,
                    completedCount = counts.completed,
                    scheduledCount = counts.scheduled,
                )
            }
        }

    return ExerciseInsights(
        completedCount = exerciseStats.sumOf(ExerciseStat::completedCount),
        scheduledCount = exerciseStats.sumOf(ExerciseStat::scheduledCount),
        exerciseStats = exerciseStats,
        bestWeekday = weekdayStats.sortedWith(bestWeekdayComparator()).firstOrNull(),
        weakestWeekday = weekdayStats.sortedWith(weakestWeekdayComparator()).firstOrNull(),
    )
}

private fun bestWeekdayComparator(): Comparator<WeekdayStat> =
    compareByDescending<WeekdayStat> { it.percentage }
        .thenByDescending { it.scheduledCount }
        .thenBy { it.dayOfWeek.value }

private fun weakestWeekdayComparator(): Comparator<WeekdayStat> =
    compareBy<WeekdayStat> { it.percentage }
        .thenByDescending { it.scheduledCount }
        .thenBy { it.dayOfWeek.value }

private fun exerciseStatComparator(): Comparator<ExerciseStat> =
    compareBy<ExerciseStat> { it.percentage }
        .thenByDescending { it.scheduledCount }
        .thenBy { it.activity.name }

private data class ActivityCounts(
    var completed: Int = 0,
    var scheduled: Int = 0,
)
