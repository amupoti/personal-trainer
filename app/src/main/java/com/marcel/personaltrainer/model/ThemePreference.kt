package com.marcel.personaltrainer.model

enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM,
    ;

    companion object {
        fun fromStoredValue(value: String?): ThemePreference =
            entries.find { it.name == value } ?: SYSTEM
    }
}
