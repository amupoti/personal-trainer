package com.marcel.personaltrainer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.marcel.personaltrainer.ExerciseReminderScheduler
import com.marcel.personaltrainer.data.ProgressRepository

class ProgressViewModelFactory(
    private val repository: ProgressRepository,
    private val reminderScheduler: ExerciseReminderScheduler,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ProgressViewModel(repository, reminderScheduler) as T
}
