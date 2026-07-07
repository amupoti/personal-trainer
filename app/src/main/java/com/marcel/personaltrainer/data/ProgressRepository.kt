package com.marcel.personaltrainer.data

import android.content.Context
import com.marcel.personaltrainer.model.Activity
import com.marcel.personaltrainer.model.ReminderSettings
import com.marcel.personaltrainer.model.TargetUnit
import com.marcel.personaltrainer.model.ThemePreference
import com.marcel.personaltrainer.model.remainingActivityCount
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

class ProgressRepository(context: Context) {
    private val context = context.applicationContext
    private val preferences =
        context.getSharedPreferences("daily_progress", Context.MODE_PRIVATE)
    private val defaultActivities: List<Activity> by lazy {
        context.assets.open(DEFAULT_ACTIVITIES_FILE).bufferedReader().use { reader ->
            decodeActivities(reader.readText())
        }
    }

    fun activities(): List<Activity> {
        val stored = preferences.getString(ACTIVITIES_KEY, null)
        if (stored == null) {
            saveActivities(defaultActivities)
            return defaultActivities
        }
        return runCatching { decodeActivities(stored) }.getOrElse { defaultActivities }
    }

    fun addActivity(activity: Activity) {
        saveActivities(activities() + activity)
    }

    fun updateActivity(activity: Activity) {
        saveActivities(
            activities().map { existing ->
                if (existing.id == activity.id) activity else existing
            },
        )
    }

    fun deleteActivity(activityId: String) {
        saveActivities(activities().filterNot { it.id == activityId })
    }

    fun completedActivityIds(date: LocalDate): Set<String> =
        preferences.getStringSet(date.toString(), emptySet()).orEmpty().toSet()

    fun completionHistory(): Map<LocalDate, Set<String>> =
        preferences.all.keys.mapNotNull { key ->
            val date = runCatching { LocalDate.parse(key) }.getOrNull()
                ?: return@mapNotNull null
            date to completedActivityIds(date)
        }.toMap()

    fun setCompleted(date: LocalDate, activityId: String, completed: Boolean) {
        val updated = completedActivityIds(date).toMutableSet()
        if (completed) {
            updated.add(activityId)
        } else {
            updated.remove(activityId)
        }
        preferences.edit().putStringSet(date.toString(), updated).apply()
    }

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

    fun remainingActivityCount(date: LocalDate): Int = remainingActivityCount(
        activities = activities(),
        completedIds = completedActivityIds(date),
        date = date,
    )

    private fun storedTime(key: String, default: LocalTime): LocalTime =
        preferences.getString(key, null)
            ?.let { value -> runCatching { LocalTime.parse(value) }.getOrNull() }
            ?: default

    private fun saveActivities(activities: List<Activity>) {
        val json = JSONArray()
        activities.forEach { activity ->
            json.put(
                JSONObject()
                    .put("id", activity.id)
                    .put("name", activity.name)
                    .put("weekdays", JSONArray(activity.weekdays.map(DayOfWeek::name)))
                    .put("targetValue", activity.targetValue)
                    .put("targetUnit", activity.targetUnit.name)
                    .put("videoUrl", activity.videoUrl)
                    .put("usesLocalizedName", activity.usesLocalizedName),
            )
        }
        preferences.edit().putString(ACTIVITIES_KEY, json.toString()).apply()
    }

    private fun decodeActivities(value: String): List<Activity> {
        val json = JSONArray(value)
        return (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            val weekdaysJson = item.getJSONArray("weekdays")
            Activity(
                id = item.getString("id"),
                name = item.getString("name"),
                weekdays = (0 until weekdaysJson.length())
                    .map { DayOfWeek.valueOf(weekdaysJson.getString(it)) }
                    .toSet(),
                targetValue = item.getInt("targetValue"),
                targetUnit = TargetUnit.valueOf(item.getString("targetUnit")),
                videoUrl = item.optString("videoUrl"),
                usesLocalizedName = item.optBoolean(
                    "usesLocalizedName",
                    item.getString("id") in DEFAULT_ACTIVITY_IDS,
                ),
            )
        }
    }

    private companion object {
        const val ACTIVITIES_KEY = "custom_activities"
        const val DEFAULT_ACTIVITIES_FILE = "default_exercises.json"
        const val REMINDERS_ENABLED_KEY = "reminders_enabled"
        const val FIRST_REMINDER_TIME_KEY = "first_reminder_time"
        const val SECOND_REMINDER_TIME_KEY = "second_reminder_time"
        const val THEME_PREFERENCE_KEY = "theme_preference"
        val DEFAULT_ACTIVITY_IDS = setOf(
            "hamstring_stretch",
            "glute_bridge",
            "pelvic_tilt",
            "cobra",
            "opposite_leg_pull",
            "band_arms",
            "band_knees",
            "standing_table_leg_curl",
            "side_plank",
        )
    }
}
