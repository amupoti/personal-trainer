package com.marcel.personaltrainer.model

import java.time.LocalDate

data class StreakStats(
    val current: Int = 0,
    val longest: Int = 0,
    val isTodayScheduled: Boolean = false,
    val isTodayComplete: Boolean = false,
)

fun calculateStreak(
    activities: List<Activity>,
    completionHistory: Map<LocalDate, Set<String>>,
    today: LocalDate,
): StreakStats {
    val todayTargets = targetIds(activities, today)
    val isTodayScheduled = todayTargets.isNotEmpty()
    val isTodayComplete = isTodayScheduled &&
        completionHistory[today].orEmpty().containsAll(todayTargets)
    val firstTrackedDate = completionHistory.keys
        .filterNot { it.isAfter(today) }
        .minOrNull()
        ?: return StreakStats(
            isTodayScheduled = isTodayScheduled,
            isTodayComplete = isTodayComplete,
        )

    var longest = 0
    var run = 0
    var date = firstTrackedDate
    while (!date.isAfter(today)) {
        val targets = targetIds(activities, date)
        if (targets.isNotEmpty()) {
            if (completionHistory[date].orEmpty().containsAll(targets)) {
                run += 1
                longest = maxOf(longest, run)
            } else {
                run = 0
            }
        }
        date = date.plusDays(1)
    }

    var current = 0
    date = if (isTodayScheduled && !isTodayComplete) today.minusDays(1) else today
    while (!date.isBefore(firstTrackedDate)) {
        val targets = targetIds(activities, date)
        if (targets.isNotEmpty()) {
            if (!completionHistory[date].orEmpty().containsAll(targets)) {
                break
            }
            current += 1
        }
        date = date.minusDays(1)
    }

    return StreakStats(
        current = current,
        longest = longest,
        isTodayScheduled = isTodayScheduled,
        isTodayComplete = isTodayComplete,
    )
}

private fun targetIds(activities: List<Activity>, date: LocalDate): Set<String> =
    activities
        .filter { it.isScheduledOn(date.dayOfWeek) }
        .map(Activity::id)
        .toSet()
