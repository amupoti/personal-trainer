package com.marcel.personaltrainer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcel.personaltrainer.ExerciseReminderScheduler
import com.marcel.personaltrainer.data.ProgressRepository
import com.marcel.personaltrainer.model.Activity
import com.marcel.personaltrainer.model.CalendarPeriod
import com.marcel.personaltrainer.model.ExerciseInsights
import com.marcel.personaltrainer.model.ReminderSettings
import com.marcel.personaltrainer.model.StreakStats
import com.marcel.personaltrainer.model.TargetUnit
import com.marcel.personaltrainer.model.ThemePreference
import com.marcel.personaltrainer.model.calculateExerciseStats
import com.marcel.personaltrainer.model.calculateWeeklyProgress
import com.marcel.personaltrainer.model.calculateStreak
import com.marcel.personaltrainer.model.datesForPeriod
import com.marcel.personaltrainer.model.timerDurationSeconds
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DayProgress(
    val date: LocalDate,
    val completedCount: Int,
    val targetCount: Int,
    val completedActivities: List<Activity> = emptyList(),
)

internal fun dayProgress(
    date: LocalDate,
    activities: List<Activity>,
    completedIds: Set<String>,
): DayProgress {
    val targetActivities = activities.filter { it.isScheduledOn(date.dayOfWeek) }
    val completedActivities = activities.filter { it.id in completedIds }

    return DayProgress(
        date = date,
        completedCount = completedActivities.size,
        targetCount = targetActivities.size,
        completedActivities = completedActivities,
    )
}

data class ActivityTimer(
    val activityId: String,
    val totalSeconds: Long,
    val remainingSeconds: Long,
    val isRunning: Boolean,
)

enum class TimerSound {
    COUNTDOWN,
    COMPLETE,
}

data class ProgressUiState(
    val date: LocalDate = LocalDate.now(),
    val activities: List<Activity> = emptyList(),
    val suggestedActivities: List<Activity> = emptyList(),
    val allActivities: List<Activity> = emptyList(),
    val completedIds: Set<String> = emptySet(),
    val weeklyCompletedCount: Int = 0,
    val weeklyTargetCount: Int = 0,
    val calendarPeriod: CalendarPeriod = CalendarPeriod.WEEK,
    val calendarAnchorDate: LocalDate = date,
    val calendarDays: List<DayProgress> = emptyList(),
    val streak: StreakStats = StreakStats(),
    val weeklyExerciseStats: ExerciseInsights = calculateExerciseStats(
        activities = emptyList(),
        completionHistory = emptyMap(),
        dates = emptyList(),
    ),
    val monthlyExerciseStats: ExerciseInsights = calculateExerciseStats(
        activities = emptyList(),
        completionHistory = emptyMap(),
        dates = emptyList(),
    ),
    val timer: ActivityTimer? = null,
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
)

class ProgressViewModel(
    private val repository: ProgressRepository,
    private val reminderScheduler: ExerciseReminderScheduler,
) : ViewModel() {
    private val date = LocalDate.now()
    private val _uiState = MutableStateFlow(
        createState(
            date = date,
            completedIds = repository.completedActivityIds(date),
        ),
    )
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
    private val _timerSounds = MutableSharedFlow<TimerSound>(extraBufferCapacity = 8)
    val timerSounds: SharedFlow<TimerSound> = _timerSounds.asSharedFlow()
    private var timerJob: Job? = null

    init {
        reminderScheduler.sync(repository.reminderSettings())
    }

    fun setCompleted(activityId: String, completed: Boolean) {
        repository.setCompleted(date, activityId, completed)
        refreshActivities()
    }

    fun addActivity(
        name: String,
        weekdays: Set<DayOfWeek>,
        targetValue: Int,
        targetUnit: TargetUnit,
        videoUrl: String,
    ) {
        repository.addActivity(
            Activity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                weekdays = weekdays,
                targetValue = targetValue,
                targetUnit = targetUnit,
                videoUrl = videoUrl.trim(),
            ),
        )
        refreshActivities()
    }

    fun deleteActivity(activityId: String) {
        if (_uiState.value.timer?.activityId == activityId) {
            timerJob?.cancel()
            _uiState.value = _uiState.value.copy(timer = null)
        }
        repository.deleteActivity(activityId)
        refreshActivities()
    }

    fun updateActivity(
        activityId: String,
        name: String,
        weekdays: Set<DayOfWeek>,
        targetValue: Int,
        targetUnit: TargetUnit,
        videoUrl: String,
    ) {
        val activity = _uiState.value.allActivities.find { it.id == activityId } ?: return
        if (_uiState.value.timer?.activityId == activityId) {
            timerJob?.cancel()
            _uiState.value = _uiState.value.copy(timer = null)
        }
        repository.updateActivity(
            activity.edited(name, weekdays, targetValue, targetUnit, videoUrl),
        )
        refreshActivities()
    }

    fun setRemindersEnabled(enabled: Boolean) {
        updateReminderSettings(_uiState.value.reminderSettings.copy(enabled = enabled))
    }

    fun setReminderTime(index: Int, time: LocalTime) {
        val current = _uiState.value.reminderSettings
        updateReminderSettings(
            if (index == 0) {
                current.copy(firstTime = time)
            } else {
                current.copy(secondTime = time)
            },
        )
    }

    fun setThemePreference(themePreference: ThemePreference) {
        repository.saveThemePreference(themePreference)
        _uiState.value = _uiState.value.copy(themePreference = themePreference)
    }

    fun toggleTimer(activityId: String) {
        val activity = _uiState.value.activities.find { it.id == activityId } ?: return
        val totalSeconds = activity.timerDurationSeconds() ?: return
        val current = _uiState.value.timer

        if (current?.activityId == activityId && current.isRunning) {
            timerJob?.cancel()
            _uiState.value = _uiState.value.copy(timer = current.copy(isRunning = false))
            return
        }

        val timer = if (current?.activityId == activityId && current.remainingSeconds > 0) {
            current.copy(isRunning = true)
        } else {
            ActivityTimer(
                activityId = activityId,
                totalSeconds = totalSeconds,
                remainingSeconds = totalSeconds,
                isRunning = true,
            )
        }
        startTimer(timer)
    }

    fun resetTimer(activityId: String) {
        val activity = _uiState.value.activities.find { it.id == activityId } ?: return
        val totalSeconds = activity.timerDurationSeconds() ?: return
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            timer = ActivityTimer(
                activityId = activityId,
                totalSeconds = totalSeconds,
                remainingSeconds = totalSeconds,
                isRunning = false,
            ),
        )
    }

    fun setCalendarPeriod(period: CalendarPeriod) {
        _uiState.value = _uiState.value.copy(
            calendarPeriod = period,
            calendarDays = calendarProgress(_uiState.value.calendarAnchorDate, period),
        )
    }

    fun moveCalendar(direction: Int) {
        val current = _uiState.value
        val newAnchor = when (current.calendarPeriod) {
            CalendarPeriod.WEEK -> current.calendarAnchorDate.plusWeeks(direction.toLong())
            CalendarPeriod.MONTH ->
                current.calendarAnchorDate.withDayOfMonth(1).plusMonths(direction.toLong())
        }
        _uiState.value = current.copy(
            calendarAnchorDate = newAnchor,
            calendarDays = calendarProgress(newAnchor, current.calendarPeriod),
        )
    }

    private fun createState(
        date: LocalDate,
        completedIds: Set<String>,
    ): ProgressUiState {
        val activities = repository.activities()
        val scheduled = activities.filter { it.isScheduledOn(date.dayOfWeek) }
        val history = repository.completionHistory()
        val weeklyProgress = calculateWeeklyProgress(
            activities,
            history,
            date,
        )
        return ProgressUiState(
            date = date,
            activities = activities,
            suggestedActivities = scheduled,
            allActivities = activities,
            completedIds = completedIds.intersect(activities.map(Activity::id).toSet()),
            weeklyCompletedCount = weeklyProgress.completedCount,
            weeklyTargetCount = weeklyProgress.targetCount,
            calendarAnchorDate = date,
            calendarDays = calendarProgress(date, CalendarPeriod.WEEK),
            streak = calculateStreak(activities, history, date),
            weeklyExerciseStats = calculateExerciseStats(
                activities = activities,
                completionHistory = history,
                dates = datesForPeriod(date, CalendarPeriod.WEEK),
            ),
            monthlyExerciseStats = calculateExerciseStats(
                activities = activities,
                completionHistory = history,
                dates = datesForPeriod(date, CalendarPeriod.MONTH),
            ),
            reminderSettings = repository.reminderSettings(),
            themePreference = repository.themePreference(),
        )
    }

    private fun refreshActivities() {
        val current = _uiState.value
        val activities = repository.activities()
        val scheduled = activities.filter { it.isScheduledOn(date.dayOfWeek) }
        val history = repository.completionHistory()
        val weeklyProgress = calculateWeeklyProgress(
            activities,
            history,
            date,
        )
        _uiState.value = current.copy(
            activities = activities,
            suggestedActivities = scheduled,
            allActivities = activities,
            completedIds = repository.completedActivityIds(date)
                .intersect(activities.map(Activity::id).toSet()),
            weeklyCompletedCount = weeklyProgress.completedCount,
            weeklyTargetCount = weeklyProgress.targetCount,
            calendarDays = calendarProgress(current.calendarAnchorDate, current.calendarPeriod),
            streak = calculateStreak(activities, history, date),
            weeklyExerciseStats = calculateExerciseStats(
                activities = activities,
                completionHistory = history,
                dates = datesForPeriod(date, CalendarPeriod.WEEK),
            ),
            monthlyExerciseStats = calculateExerciseStats(
                activities = activities,
                completionHistory = history,
                dates = datesForPeriod(date, CalendarPeriod.MONTH),
            ),
        )
    }

    private fun calendarProgress(
        anchorDate: LocalDate,
        period: CalendarPeriod,
    ): List<DayProgress> {
        val activities = repository.activities()
        return datesForPeriod(anchorDate, period).map { day ->
            dayProgress(
                date = day,
                activities = activities,
                completedIds = repository.completedActivityIds(day),
            )
        }
    }

    private fun updateReminderSettings(settings: ReminderSettings) {
        repository.saveReminderSettings(settings)
        reminderScheduler.sync(settings)
        _uiState.value = _uiState.value.copy(reminderSettings = settings)
    }

    private fun startTimer(timer: ActivityTimer) {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(timer = timer)
        timerJob = viewModelScope.launch {
            var remaining = timer.remainingSeconds
            while (remaining > 0) {
                if (remaining <= 5) {
                    _timerSounds.emit(TimerSound.COUNTDOWN)
                }
                delay(1_000)
                remaining -= 1
                val active = _uiState.value.timer
                if (active?.activityId != timer.activityId) return@launch
                _uiState.value = _uiState.value.copy(
                    timer = active.copy(remainingSeconds = remaining),
                )
            }
            _uiState.value = _uiState.value.copy(
                timer = _uiState.value.timer?.copy(isRunning = false),
            )
            _timerSounds.emit(TimerSound.COMPLETE)
        }
    }
}
