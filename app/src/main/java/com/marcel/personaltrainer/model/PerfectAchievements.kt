package com.marcel.personaltrainer.model

import java.time.LocalDate

data class PerfectAchievements(
    val perfectDayCount: Int,
    val perfectWeekCount: Int,
    val isTodayPerfect: Boolean,
    val isThisWeekPerfect: Boolean,
)

fun calculatePerfectAchievements(
    activities: List<Activity>,
    completionHistory: Map<LocalDate, Set<String>>,
    today: LocalDate,
): PerfectAchievements {
    val trackedDates = completionHistory.keys.filterNot { it.isAfter(today) }
    val perfectDayCount = trackedDates.count { date ->
        isPerfectDay(activities, completionHistory, date)
    }
    val firstTrackedDate = trackedDates.minOrNull()
    val perfectWeekCount = if (firstTrackedDate == null) {
        0
    } else {
        perfectWeeksBetween(
            activities = activities,
            completionHistory = completionHistory,
            start = weekStart(firstTrackedDate),
            end = weekStart(today),
        )
    }

    return PerfectAchievements(
        perfectDayCount = perfectDayCount,
        perfectWeekCount = perfectWeekCount,
        isTodayPerfect = isPerfectDay(activities, completionHistory, today),
        isThisWeekPerfect = calculateWeeklyProgress(
            activities = activities,
            completionHistory = completionHistory,
            date = today,
        ).isComplete,
    )
}

private fun perfectWeeksBetween(
    activities: List<Activity>,
    completionHistory: Map<LocalDate, Set<String>>,
    start: LocalDate,
    end: LocalDate,
): Int {
    var count = 0
    var week = start
    while (!week.isAfter(end)) {
        if (
            calculateWeeklyProgress(
                activities = activities,
                completionHistory = completionHistory,
                date = week,
            ).isComplete
        ) {
            count += 1
        }
        week = week.plusWeeks(1)
    }
    return count
}

private fun isPerfectDay(
    activities: List<Activity>,
    completionHistory: Map<LocalDate, Set<String>>,
    date: LocalDate,
): Boolean {
    val targets = activities
        .filter { it.isScheduledOn(date.dayOfWeek) }
        .map(Activity::id)
        .toSet()
    return targets.isNotEmpty() && completionHistory[date].orEmpty().containsAll(targets)
}

private fun weekStart(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value - 1).toLong())
