package com.marcel.personaltrainer.ui

import androidx.lifecycle.ViewModel
import com.marcel.personaltrainer.data.ProgressRepository
import com.marcel.personaltrainer.model.Activity
import com.marcel.personaltrainer.model.CalendarPeriod
import com.marcel.personaltrainer.model.TargetUnit
import com.marcel.personaltrainer.model.datesForPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DayProgress(
    val date: LocalDate,
    val completedCount: Int,
    val targetCount: Int,
)

data class ProgressUiState(
    val date: LocalDate = LocalDate.now(),
    val activities: List<Activity> = emptyList(),
    val allActivities: List<Activity> = emptyList(),
    val completedIds: Set<String> = emptySet(),
    val calendarPeriod: CalendarPeriod = CalendarPeriod.WEEK,
    val calendarAnchorDate: LocalDate = date,
    val calendarDays: List<DayProgress> = emptyList(),
)

class ProgressViewModel(
    private val repository: ProgressRepository,
) : ViewModel() {
    private val date = LocalDate.now()
    private val _uiState = MutableStateFlow(
        createState(
            date = date,
            completedIds = repository.completedActivityIds(date),
        ),
    )
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

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
        repository.deleteActivity(activityId)
        refreshActivities()
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
        return ProgressUiState(
            date = date,
            activities = scheduled,
            allActivities = activities,
            completedIds = completedIds.intersect(scheduled.map(Activity::id).toSet()),
            calendarAnchorDate = date,
            calendarDays = calendarProgress(date, CalendarPeriod.WEEK),
        )
    }

    private fun refreshActivities() {
        val current = _uiState.value
        val activities = repository.activities()
        val scheduled = activities.filter { it.isScheduledOn(date.dayOfWeek) }
        _uiState.value = current.copy(
            activities = scheduled,
            allActivities = activities,
            completedIds = repository.completedActivityIds(date)
                .intersect(scheduled.map(Activity::id).toSet()),
            calendarDays = calendarProgress(current.calendarAnchorDate, current.calendarPeriod),
        )
    }

    private fun calendarProgress(
        anchorDate: LocalDate,
        period: CalendarPeriod,
    ): List<DayProgress> {
        val activities = repository.activities()
        return datesForPeriod(anchorDate, period).map { day ->
            val targetIds = activities
                .filter { it.isScheduledOn(day.dayOfWeek) }
                .map(Activity::id)
                .toSet()
            DayProgress(
                date = day,
                completedCount = repository.completedActivityIds(day).count(targetIds::contains),
                targetCount = targetIds.size,
            )
        }
    }
}
