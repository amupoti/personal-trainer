package com.marcel.personaltrainer

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.marcel.personaltrainer.model.ReminderSettings
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    settings: ReminderSettings,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int, LocalTime) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily reminders",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Get two notifications with your remaining exercises.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = onEnabledChange,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    ReminderTimeRow(
                        label = "First reminder",
                        time = settings.firstTime,
                        onTimeChange = { onTimeChange(0, it) },
                    )
                    Spacer(Modifier.height(12.dp))
                    ReminderTimeRow(
                        label = "Second reminder",
                        time = settings.secondTime,
                        onTimeChange = { onTimeChange(1, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderTimeRow(
    label: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedButton(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onTimeChange(LocalTime.of(hour, minute)) },
                    time.hour,
                    time.minute,
                    true,
                ).show()
            },
        ) {
            Text(time.format(TIME_FORMATTER))
        }
    }
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
