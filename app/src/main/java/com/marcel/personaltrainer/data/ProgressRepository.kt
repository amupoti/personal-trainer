package com.marcel.personaltrainer.data

import android.content.Context
import com.marcel.personaltrainer.model.ReminderSettings
import com.marcel.personaltrainer.model.ThemePreference
import java.time.LocalTime

class ProgressRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun reminderSettings(): ReminderSettings = ReminderSettings(
        enabled = preferences.getBoolean(REMINDERS_ENABLED_KEY, false),
        firstTime = storedTime(FIRST_REMINDER_TIME_KEY, ReminderSettings().firstTime),
        secondTime = storedTime(SECOND_REMINDER_TIME_KEY, ReminderSettings().secondTime),
    )

    fun saveReminderSettings(settings: ReminderSettings) {
        preferences.edit()
            .putBoolean(REMINDERS_ENABLED_KEY, settings.enabled)
            .putString(FIRST_REMINDER_TIME_KEY, settings.firstTime.toString())
            .putString(SECOND_REMINDER_TIME_KEY, settings.secondTime.toString())
            .apply()
    }

    fun themePreference(): ThemePreference =
        ThemePreference.fromStoredValue(preferences.getString(THEME_PREFERENCE_KEY, null))

    fun saveThemePreference(themePreference: ThemePreference) {
        preferences.edit().putString(THEME_PREFERENCE_KEY, themePreference.name).apply()
    }

    private fun storedTime(key: String, default: LocalTime): LocalTime =
        preferences.getString(key, null)
            ?.let { value -> runCatching { LocalTime.parse(value) }.getOrNull() }
            ?: default

    private companion object {
        const val REMINDERS_ENABLED_KEY = "reminders_enabled"
        const val FIRST_REMINDER_TIME_KEY = "first_reminder_time"
        const val SECOND_REMINDER_TIME_KEY = "second_reminder_time"
        const val THEME_PREFERENCE_KEY = "theme_preference"
    }
}
