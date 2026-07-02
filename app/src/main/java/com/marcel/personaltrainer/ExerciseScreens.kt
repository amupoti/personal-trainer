package com.marcel.personaltrainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marcel.personaltrainer.model.Activity
import com.marcel.personaltrainer.model.TargetUnit
import com.marcel.personaltrainer.model.isValidVideoUrl
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun ExerciseListScreen(
    activities: List<Activity>,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Activity?>(null) }
    val locale = LocalLocale.current.platformLocale

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "My exercises",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                FilledTonalButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Add exercise")
                }
            }
        }
        if (activities.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Build your routine",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Add an exercise to start tracking your movement.",
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
        items(activities, key = Activity::id) { activity ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activity.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = activity.description,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = activity.weekdays
                                    .sortedBy(DayOfWeek::getValue)
                                    .joinToString(" · ") {
                                        it.getDisplayName(TextStyle.SHORT, locale)
                                    },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    IconButton(onClick = { pendingDelete = activity }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete ${activity.name}",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { activity ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete exercise?") },
            text = { Text(activity.name) },
            confirmButton = {
                Button(
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
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Text(
                    text = "Add exercise",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
        item {
            FormSection(title = "Exercise") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                )
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
        }
        item {
            FormSection(title = "Schedule") {
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
        }
        item {
            FormSection(title = "Target") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Repetitions", "Duration").forEachIndexed { index, label ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = targetMode == index,
                            onClick = { targetMode = index },
                            label = { Text(label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { targetValue = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(if (targetMode == 0) "Number of repetitions" else "Duration")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = targetValue.isNotEmpty() &&
                        (parsedTarget == null || parsedTarget <= 0),
                )
                if (targetMode == 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(TargetUnit.SECONDS, TargetUnit.MINUTES).forEach { unit ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = durationUnit == unit.name,
                                onClick = { durationUnit = unit.name },
                                label = {
                                    Text(unit.label.replaceFirstChar(Char::uppercase))
                                },
                            )
                        }
                    }
                }
            }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = valid,
            ) {
                Text("Save exercise")
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}

@Composable
private fun DayPicker(
    selectedDays: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    onAllDaysToggle: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
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
                            Text(day.getDisplayName(TextStyle.SHORT, locale))
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
