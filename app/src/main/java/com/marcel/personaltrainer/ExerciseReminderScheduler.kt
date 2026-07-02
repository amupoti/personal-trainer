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
import com.marcel.personaltrainer.model.ReminderSettings
import com.marcel.personaltrainer.model.delayUntilReminder
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
        showExerciseNotification(
            context = applicationContext,
            remaining = repository.remainingActivityCount(LocalDate.now()),
        )
        return Result.success()
    }
}

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        REMINDER_CHANNEL_ID,
        "Exercise reminders",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Daily reminders for unfinished exercises"
    }
    context.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(channel)
}

private fun showExerciseNotification(context: Context, remaining: Int) {
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
    val label = if (remaining == 1) "exercise" else "exercises"
    val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.app_icon)
        .setContentTitle("Today's exercises")
        .setContentText("$remaining $label remaining")
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
}

private const val REMINDER_CHANNEL_ID = "exercise_reminders"
private const val REMINDER_NOTIFICATION_ID = 1101
