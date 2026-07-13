package com.marcel.personaltrainer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.marcel.personaltrainer.data.ProgressRepository
import com.marcel.personaltrainer.model.Activity
import com.marcel.personaltrainer.model.ReminderNotificationSummary
import com.marcel.personaltrainer.model.ReminderSettings
import com.marcel.personaltrainer.model.calculateStreak
import com.marcel.personaltrainer.model.delayUntilReminder
import com.marcel.personaltrainer.model.reminderNotificationSummary
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ExerciseReminderScheduler(context: Context) {
    private val context = context.applicationContext
    private val workManager = WorkManager.getInstance(this.context)

    fun sync(settings: ReminderSettings) {
        createNotificationChannel(context)
        if (!settings.enabled) {
            WORK_NAMES.forEach(workManager::cancelUniqueWork)
            return
        }
        schedule(WORK_NAMES[0], settings.firstTime)
        schedule(WORK_NAMES[1], settings.secondTime)
    }

    private fun schedule(name: String, time: LocalTime) {
        val request = PeriodicWorkRequestBuilder<ExerciseReminderWorker>(
            24,
            TimeUnit.HOURS,
        )
            .setInitialDelay(delayUntilReminder(time), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            name,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    private companion object {
        val WORK_NAMES = listOf("exercise-reminder-first", "exercise-reminder-second")
    }
}

class ExerciseReminderWorker(
    context: Context,
    parameters: WorkerParameters,
) : Worker(context, parameters) {
    override fun doWork(): Result {
        val repository = ProgressRepository(applicationContext)
        if (!repository.reminderSettings().enabled) {
            return Result.success()
        }
        val date = LocalDate.now()
        val activities = repository.activities()
        val completionHistory = repository.completionHistory()
        val summary = reminderNotificationSummary(
            activities = activities,
            completedIds = completionHistory[date].orEmpty(),
            date = date,
            currentStreak = calculateStreak(activities, completionHistory, date).current,
        ) ?: return Result.success()
        showExerciseNotification(
            context = applicationContext,
            contentText = reminderContentText(applicationContext, summary),
        )
        return Result.success()
    }
}

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        REMINDER_CHANNEL_ID,
        context.getString(R.string.reminder_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = context.getString(R.string.reminder_channel_description)
    }
    context.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(channel)
}

private fun showExerciseNotification(context: Context, contentText: String) {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    createNotificationChannel(context)
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.app_icon)
        .setContentTitle(context.getString(R.string.reminder_notification_title))
        .setContentText(contentText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
}

private fun reminderContentText(
    context: Context,
    summary: ReminderNotificationSummary,
): String {
    if (summary.isGeneralReminder) {
        return context.getString(R.string.reminder_general_message)
    }

    val names = summary.remainingActivities.joinToString(", ") { it.notificationName(context) }
    return when {
        summary.totalRemainingCount == 1 -> context.getString(
            R.string.reminder_one_left_message,
            names,
        )
        summary.totalRemainingCount == 2 && summary.remainingActivities.size == 2 -> context.getString(
            R.string.reminder_two_left_message,
            summary.remainingActivities[0].notificationName(context),
            summary.remainingActivities[1].notificationName(context),
        )
        summary.totalRemainingCount == 3 && summary.remainingActivities.size == 3 -> context.getString(
            R.string.reminder_three_left_message,
            summary.remainingActivities[0].notificationName(context),
            summary.remainingActivities[1].notificationName(context),
            summary.remainingActivities[2].notificationName(context),
        )
        summary.completedCount > 0 && summary.remainingActivities.size >= 2 -> context.getString(
            R.string.reminder_progress_more_message,
            summary.completedCount,
            summary.scheduledCount,
            summary.remainingActivities[0].notificationName(context),
            summary.remainingActivities[1].notificationName(context),
            summary.extraRemainingCount,
        )
        summary.currentStreak > 0 -> context.getString(
            R.string.reminder_keep_streak_message,
            summary.currentStreak,
        )
        else -> context.getString(
            R.string.reminder_remaining_more_message,
            summary.totalRemainingCount,
            names,
            summary.extraRemainingCount,
        )
    }
}

private fun Activity.notificationName(context: Context): String {
    if (!usesLocalizedName) return name
    val resource = when (id) {
        "hamstring_stretch" -> R.string.exercise_hamstring_stretch
        "glute_bridge" -> R.string.exercise_glute_bridge
        "pelvic_tilt" -> R.string.exercise_pelvic_tilt
        "cobra" -> R.string.exercise_cobra
        "opposite_leg_pull" -> R.string.exercise_opposite_leg_pull
        "band_arms" -> R.string.exercise_band_arms
        "band_knees" -> R.string.exercise_band_knees
        "standing_table_leg_curl" -> R.string.exercise_standing_table_leg_curl
        "side_plank" -> R.string.exercise_side_plank
        else -> return name
    }
    return context.getString(resource)
}

private const val REMINDER_CHANNEL_ID = "exercise_reminders"
private const val REMINDER_NOTIFICATION_ID = 1101
