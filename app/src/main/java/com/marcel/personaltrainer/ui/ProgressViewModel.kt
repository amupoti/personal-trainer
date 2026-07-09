package com.marcel.personaltrainer.ui

import androidx.lifecycle.ViewModel
import com.marcel.personaltrainer.ReminderScheduler
import com.marcel.personaltrainer.data.ProgressRepository
import com.marcel.personaltrainer.model.ReminderSettings
import com.marcel.personaltrainer.model.ThemePreference
import java.time.LocalTime

data class ProgressUiState(
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
)

class ProgressViewModel(
    private val repository: ProgressRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(
        ProgressUiState(
            reminderSettings = repository.reminderSettings(),
            themePreference = repository.themePreference(),
        ),
    )
    val uiState: kotlinx.coroutines.flow.StateFlow<ProgressUiState> = _uiState

    init {
        reminderScheduler.sync(repository.reminderSettings())
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

    private fun updateReminderSettings(settings: ReminderSettings) {
        repository.saveReminderSettings(settings)
        reminderScheduler.sync(settings)
        _uiState.value = _uiState.value.copy(reminderSettings = settings)
    }
}
