package com.marcel.personaltrainer.model

import java.time.LocalDate

data class MilestoneBadge(
    val completedDayTarget: Int,
    val completedDayCount: Int,
) {
    val isUnlocked: Boolean
        get() = completedDayCount >= completedDayTarget

    val remainingCount: Int
        get() = maxOf(completedDayTarget - completedDayCount, 0)
}

fun calculateMilestoneBadges(
    activities: List<Activity>,
    completionHistory: Map<LocalDate, Set<String>>,
    today: LocalDate,
): List<MilestoneBadge> {
    val completedDays = completionHistory.keys
        .filterNot { it.isAfter(today) }
        .count { date ->
            val targets = activities
                .filter { it.isScheduledOn(date.dayOfWeek) }
                .map(Activity::id)
                .toSet()
            targets.isNotEmpty() && completionHistory[date].orEmpty().containsAll(targets)
        }

    return listOf(7, 30, 100).map { target ->
        MilestoneBadge(
            completedDayTarget = target,
            completedDayCount = completedDays,
        )
    }
}
