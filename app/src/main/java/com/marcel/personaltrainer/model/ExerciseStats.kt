package com.marcel.personaltrainer.model

import java.time.LocalDate

data class ExerciseStat(
    val activity: Activity,
    val completedCount: Int,
    val percentage: Float,
)

fun calculateExerciseStats(
    activities: List<Activity>,
    completionHistory: Map<LocalDate, Set<String>>,
    dates: List<LocalDate>,
): List<ExerciseStat> {
    val activitiesById = activities.associateBy(Activity::id)
    val counts = mutableMapOf<String, Int>()

    dates.forEach { date ->
        completionHistory[date].orEmpty().forEach { activityId ->
            if (activityId in activitiesById) {
                counts[activityId] = counts.getOrDefault(activityId, 0) + 1
            }
        }
    }

    val total = counts.values.sum()
    if (total == 0) return emptyList()

    return counts.entries
        .mapNotNull { (activityId, count) ->
            activitiesById[activityId]?.let { activity ->
                ExerciseStat(
                    activity = activity,
                    completedCount = count,
                    percentage = count * 100f / total,
                )
            }
        }
        .sortedWith(
            compareByDescending<ExerciseStat> { it.completedCount }
                .thenBy { it.activity.name },
        )
}
