package com.marcel.personaltrainer.model

import java.net.URI
import java.time.DayOfWeek

enum class TargetUnit(val label: String) {
    REPETITIONS("repetitions"),
    SECONDS("seconds"),
    MINUTES("minutes"),
}

data class Activity(
    val id: String,
    val name: String,
    val weekdays: Set<DayOfWeek>,
    val targetValue: Int,
    val targetUnit: TargetUnit,
    val videoUrl: String = "",
    val usesLocalizedName: Boolean = false,
) {
    val description: String
        get() = "$targetValue ${targetUnit.label}"

    fun isScheduledOn(dayOfWeek: DayOfWeek): Boolean = dayOfWeek in weekdays

    fun edited(
        name: String,
        weekdays: Set<DayOfWeek>,
        targetValue: Int,
        targetUnit: TargetUnit,
        videoUrl: String,
    ): Activity = copy(
        name = name.trim(),
        weekdays = weekdays,
        targetValue = targetValue,
        targetUnit = targetUnit,
        videoUrl = videoUrl.trim(),
        usesLocalizedName = false,
    )
}

fun isValidVideoUrl(value: String): Boolean {
    if (value.isBlank()) return true
    return runCatching {
        val uri = URI(value)
        uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}
