package com.marcel.personaltrainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marcel.personaltrainer.model.Activity
import com.marcel.personaltrainer.model.TargetUnit
import com.marcel.personaltrainer.model.isValidVideoUrl
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ExerciseListScreen(
    activities: List<Activity>,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Activity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "My exercises",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(onClick = onAdd) {
                    Text("Add exercise")
                }
            }
        }
        if (activities.isEmpty()) {
            item {
                Text(
                    text = "No exercises yet",
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        items(activities, key = Activity::id) { activity ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                ) {
                    Text(
                        text = activity.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = activity.description,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = activity.weekdays
                            .sortedBy(DayOfWeek::getValue)
                            .joinToString(" ") {
                                it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { pendingDelete = activity }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
        }
    }

    pendingDelete?.let { activity ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete exercise?") },
            text = { Text(activity.name) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(activity.id)
                        pendingDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun AddExerciseScreen(
    onCancel: () -> Unit,
    onSave: (String, Set<DayOfWeek>, Int, TargetUnit, String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedDaysMask by rememberSaveable { mutableIntStateOf(0) }
    var targetMode by rememberSaveable { mutableIntStateOf(0) }
    var targetValue by rememberSaveable { mutableStateOf("") }
    var durationUnit by rememberSaveable { mutableStateOf(TargetUnit.MINUTES.name) }
    var videoUrl by rememberSaveable { mutableStateOf("") }

    val selectedDays = DayOfWeek.entries
        .filter { selectedDaysMask and (1 shl it.ordinal) != 0 }
        .toSet()
    val parsedTarget = targetValue.toIntOrNull()
    val valid = name.isNotBlank() &&
        selectedDays.isNotEmpty() &&
        parsedTarget != null &&
        parsedTarget > 0 &&
        isValidVideoUrl(videoUrl)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) {
                    Text("<", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "Add exercise",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
            )
        }
        item {
            Text("Days", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            DayPicker(
                selectedDays = selectedDays,
                onToggle = { day ->
                    selectedDaysMask = selectedDaysMask xor (1 shl day.ordinal)
                },
                onAllDaysToggle = {
                    selectedDaysMask = if (selectedDays.size == DayOfWeek.entries.size) {
                        0
                    } else {
                        (1 shl DayOfWeek.entries.size) - 1
                    }
                },
            )
            if (selectedDays.isEmpty()) {
                Text(
                    text = "Select at least one day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item {
            Text("Target", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            PrimaryTabRow(selectedTabIndex = targetMode) {
                listOf("Repetitions", "Duration").forEachIndexed { index, label ->
                    Tab(
                        selected = targetMode == index,
                        onClick = { targetMode = index },
                        text = { Text(label) },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = targetValue,
                onValueChange = { targetValue = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(if (targetMode == 0) "Number of repetitions" else "Duration")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = targetValue.isNotEmpty() && (parsedTarget == null || parsedTarget <= 0),
            )
        }
        if (targetMode == 1) {
            item {
                PrimaryTabRow(
                    selectedTabIndex = if (durationUnit == TargetUnit.SECONDS.name) 0 else 1,
                ) {
                    listOf(TargetUnit.SECONDS, TargetUnit.MINUTES).forEach { unit ->
                        Tab(
                            selected = durationUnit == unit.name,
                            onClick = { durationUnit = unit.name },
                            text = { Text(unit.label.replaceFirstChar(Char::uppercase)) },
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = videoUrl,
                onValueChange = { videoUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Video link (optional)") },
                placeholder = { Text("https://...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                isError = !isValidVideoUrl(videoUrl),
                supportingText = {
                    if (!isValidVideoUrl(videoUrl)) {
                        Text("Enter a valid http or https link")
                    }
                },
            )
        }
        item {
            Button(
                onClick = {
                    onSave(
                        name,
                        selectedDays,
                        parsedTarget!!,
                        if (targetMode == 0) {
                            TargetUnit.REPETITIONS
                        } else {
                            TargetUnit.valueOf(durationUnit)
                        },
                        videoUrl,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = valid,
            ) {
                Text("Save exercise")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DayPicker(
    selectedDays: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    onAllDaysToggle: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = selectedDays.size == DayOfWeek.entries.size,
            onClick = onAllDaysToggle,
            label = { Text("All days") },
            modifier = Modifier.fillMaxWidth(),
        )
        DayOfWeek.entries.chunked(4).forEach { days ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                days.forEach { day ->
                    FilterChip(
                        selected = day in selectedDays,
                        onClick = { onToggle(day) },
                        label = {
                            Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - days.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
