package com.marcel.personaltrainer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.marcel.personaltrainer.data.ProgressRepository
import com.marcel.personaltrainer.model.Activity
import com.marcel.personaltrainer.model.CalendarPeriod
import com.marcel.personaltrainer.model.TargetUnit
import com.marcel.personaltrainer.model.formatTimer
import com.marcel.personaltrainer.model.timerDurationSeconds
import com.marcel.personaltrainer.ui.ActivityTimer
import com.marcel.personaltrainer.ui.ProgressUiState
import com.marcel.personaltrainer.ui.ProgressViewModel
import com.marcel.personaltrainer.ui.ProgressViewModelFactory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val viewModel: ProgressViewModel by viewModels {
        ProgressViewModelFactory(
            repository = ProgressRepository(applicationContext),
            reminderScheduler = ExerciseReminderScheduler(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimerSoundEffects(viewModel.timerSounds)
            DailyMovementTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        viewModel.setRemindersEnabled(true)
                    }
                }
                DailyMovementApp(
                    state = state,
                    onCompletedChange = viewModel::setCompleted,
                    onCalendarPeriodChange = viewModel::setCalendarPeriod,
                    onCalendarPrevious = { viewModel.moveCalendar(-1) },
                    onCalendarNext = { viewModel.moveCalendar(1) },
                    onAddActivity = viewModel::addActivity,
                    onDeleteActivity = viewModel::deleteActivity,
                    onToggleTimer = viewModel::toggleTimer,
                    onResetTimer = viewModel::resetTimer,
                    onRemindersEnabledChange = { enabled ->
                        if (
                            !enabled ||
                            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.setRemindersEnabled(enabled)
                        } else {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS,
                            )
                        }
                    },
                    onReminderTimeChange = viewModel::setReminderTime,
                )
            }
        }
    }
}

@Composable
private fun DailyMovementTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF006B5F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF9EF2E2),
            onPrimaryContainer = Color(0xFF00201C),
            secondary = Color(0xFF4A635E),
            secondaryContainer = Color(0xFFCDE8E1),
            onSecondaryContainer = Color(0xFF06201B),
            background = Color(0xFFF5FBF8),
            surface = Color(0xFFF5FBF8),
            surfaceVariant = Color(0xFFDAE5E1),
            outline = Color(0xFF6F7976),
        ),
        typography = Typography(
            headlineLarge = Typography().headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
            headlineMedium = Typography().headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.25).sp,
            ),
            titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
        content = content,
    )
}

@Composable
private fun DailyMovementApp(
    state: ProgressUiState,
    onCompletedChange: (String, Boolean) -> Unit,
    onCalendarPeriodChange: (CalendarPeriod) -> Unit,
    onCalendarPrevious: () -> Unit,
    onCalendarNext: () -> Unit,
    onAddActivity: (String, Set<DayOfWeek>, Int, TargetUnit, String) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onToggleTimer: (String) -> Unit,
    onResetTimer: (String) -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, LocalTime) -> Unit,
) {
    var selectedView by rememberSaveable { mutableIntStateOf(0) }
    var addingExercise by rememberSaveable { mutableStateOf(false) }
    val destinations = listOf(
        Triple("Today", Icons.Rounded.Home, "Today"),
        Triple("Calendar", Icons.Rounded.DateRange, "Calendar"),
        Triple("Exercises", Icons.AutoMirrored.Rounded.List, "Exercises"),
        Triple("Settings", Icons.Rounded.Settings, "Settings"),
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!addingExercise) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    destinations.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            selected = selectedView == index,
                            onClick = { selectedView = index },
                            icon = {
                                Icon(
                                    imageVector = destination.second,
                                    contentDescription = destination.third,
                                )
                            },
                            label = { Text(destination.first) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (addingExercise) {
                AddExerciseScreen(
                    onCancel = { addingExercise = false },
                    onSave = { name, weekdays, targetValue, targetUnit, videoUrl ->
                        onAddActivity(name, weekdays, targetValue, targetUnit, videoUrl)
                        addingExercise = false
                    },
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(
                        text = "Daily movement",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = "Small steps, every day",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                when (selectedView) {
                    0 -> ProgressScreen(
                        state = state,
                        onCompletedChange = onCompletedChange,
                        onToggleTimer = onToggleTimer,
                        onResetTimer = onResetTimer,
                    )

                    1 -> CalendarScreen(
                        state = state,
                        onPeriodChange = onCalendarPeriodChange,
                        onPrevious = onCalendarPrevious,
                        onNext = onCalendarNext,
                    )

                    2 -> ExerciseListScreen(
                        activities = state.allActivities,
                        onAdd = { addingExercise = true },
                        onDelete = onDeleteActivity,
                    )

                    else -> SettingsScreen(
                        settings = state.reminderSettings,
                        onEnabledChange = onRemindersEnabledChange,
                        onTimeChange = onReminderTimeChange,
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onCompletedChange: (String, Boolean) -> Unit,
    onToggleTimer: (String) -> Unit,
    onResetTimer: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            val completed = state.completedIds.size
            val total = state.activities.size
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = state.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (total == 0) {
                            "Nothing scheduled today"
                        } else {
                            "$completed of $total completed"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    if (total > 0) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { completed.toFloat() / total },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        )
                    }
                }
            }
        }
        items(state.activities, key = Activity::id) { activity ->
            ActivityCard(
                activity = activity,
                completed = activity.id in state.completedIds,
                timer = state.timer,
                onCompletedChange = { completed ->
                    onCompletedChange(activity.id, completed)
                },
                onToggleTimer = { onToggleTimer(activity.id) },
                onResetTimer = { onResetTimer(activity.id) },
            )
        }
    }
}

@Composable
private fun ActivityCard(
    activity: Activity,
    completed: Boolean,
    timer: ActivityTimer?,
    onCompletedChange: (Boolean) -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val timerDuration = activity.timerDurationSeconds()
    val activityTimer = timer?.takeIf { it.activityId == activity.id }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (completed) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (completed) 0.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = if (completed) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (completed) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = activity.name.firstOrNull()?.uppercase() ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = activity.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (timerDuration != null) {
                        if (activityTimer != null) {
                            Text(
                                text = formatTimer(activityTimer.remainingSeconds),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (
                            activityTimer != null &&
                            activityTimer.remainingSeconds != activityTimer.totalSeconds
                        ) {
                            IconButton(onClick = onResetTimer) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Reset timer",
                                )
                            }
                        }
                        IconButton(onClick = onToggleTimer) {
                            if (activityTimer?.isRunning == true) {
                                Text(
                                    text = "\u2161",
                                    modifier = Modifier.semantics {
                                        contentDescription = "Pause timer"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Start timer",
                                )
                            }
                        }
                    }
                }
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (activity.videoUrl.isNotBlank()) {
                    TextButton(
                        onClick = { uriHandler.openUri(activity.videoUrl) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text("Open video")
                    }
                }
            }
            Checkbox(
                checked = completed,
                onCheckedChange = onCompletedChange,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressScreenPreview() {
    val previewActivities = listOf(
        Activity(
            id = "preview",
            name = "Hamstring stretch",
            weekdays = DayOfWeek.entries.toSet(),
            targetValue = 30,
            targetUnit = TargetUnit.SECONDS,
        ),
    )
    DailyMovementTheme {
        ProgressScreen(
            state = ProgressUiState(
                date = LocalDate.of(2026, 6, 27),
                activities = previewActivities,
                completedIds = setOf("preview"),
            ),
            onCompletedChange = { _, _ -> },
            onToggleTimer = {},
            onResetTimer = {},
        )
    }
}
