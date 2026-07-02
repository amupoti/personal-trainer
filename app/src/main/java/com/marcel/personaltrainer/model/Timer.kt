package com.marcel.personaltrainer.model

fun Activity.timerDurationSeconds(): Long? =
    when (targetUnit) {
        TargetUnit.REPETITIONS -> null
        TargetUnit.SECONDS -> targetValue.toLong()
        TargetUnit.MINUTES -> targetValue.toLong() * 60
    }

fun formatTimer(seconds: Long): String =
    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
