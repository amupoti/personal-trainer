package com.marcel.personaltrainer.data

import android.content.Context
import com.marcel.personaltrainer.model.Activity
import com.marcel.personaltrainer.model.TargetUnit
import java.time.DayOfWeek
import java.time.LocalDate
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

    fun deleteActivity(activityId: String) {
        saveActivities(activities().filterNot { it.id == activityId })
    }

    fun completedActivityIds(date: LocalDate): Set<String> =
        preferences.getStringSet(date.toString(), emptySet()).orEmpty().toSet()

    fun setCompleted(date: LocalDate, activityId: String, completed: Boolean) {
        val updated = completedActivityIds(date).toMutableSet()
        if (completed) {
            updated.add(activityId)
        } else {
            updated.remove(activityId)
        }
        preferences.edit().putStringSet(date.toString(), updated).apply()
    }

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
                    .put("videoUrl", activity.videoUrl),
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
            )
        }
    }

    private companion object {
        const val ACTIVITIES_KEY = "custom_activities"
        const val DEFAULT_ACTIVITIES_FILE = "default_exercises.json"
    }
}
