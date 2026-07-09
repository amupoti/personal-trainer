package com.marcel.personaltrainer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.marcel.personaltrainer.ReminderScheduler
import com.marcel.personaltrainer.data.ProgressRepository

class ProgressViewModelFactory(
    private val repository: ProgressRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ProgressViewModel(repository, reminderScheduler) as T
}
