package com.marcel.personaltrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marcel.personaltrainer.data.ProgressRepository
import com.marcel.personaltrainer.model.Activity
import com.marcel.personaltrainer.model.CalendarPeriod
import com.marcel.personaltrainer.model.TargetUnit
import com.marcel.personaltrainer.ui.ProgressUiState
import com.marcel.personaltrainer.ui.ProgressViewModel
import com.marcel.personaltrainer.ui.ProgressViewModelFactory
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val viewModel: ProgressViewModel by viewModels {
        ProgressViewModelFactory(ProgressRepository(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyMovementTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                DailyMovementApp(
                    state = state,
                    onCompletedChange = viewModel::setCompleted,
                    onCalendarPeriodChange = viewModel::setCalendarPeriod,
                    onCalendarPrevious = { viewModel.moveCalendar(-1) },
                    onCalendarNext = { viewModel.moveCalendar(1) },
                    onAddActivity = viewModel::addActivity,
                    onDeleteActivity = viewModel::deleteActivity,
                )
            }
        }
    }
}

@Composable
private fun DailyMovementTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2E6653),
            secondary = Color(0xFF52634F),
            background = Color(0xFFF7F9F4),
            surface = Color(0xFFFFFFFF),
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
) {
    var selectedView by rememberSaveable { mutableIntStateOf(0) }
    var addingExercise by rememberSaveable { mutableStateOf(false) }
    Scaffold { padding ->
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
                Text(
                    text = "Daily movement",
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                PrimaryTabRow(selectedTabIndex = selectedView) {
                    listOf("Today", "Calendar", "Exercises").forEachIndexed { index, label ->
                        Tab(
                            selected = selectedView == index,
                            onClick = { selectedView = index },
                            text = { Text(label) },
                        )
                    }
                }
                when (selectedView) {
                    0 -> ProgressScreen(
                        state = state,
                        onCompletedChange = onCompletedChange,
                    )

                    1 -> CalendarScreen(
                        state = state,
                        onPeriodChange = onCalendarPeriodChange,
                        onPrevious = onCalendarPrevious,
                        onNext = onCalendarNext,
                    )

                    else -> ExerciseListScreen(
                        activities = state.allActivities,
                        onAdd = { addingExercise = true },
                        onDelete = onDeleteActivity,
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
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${state.completedIds.size} of ${state.activities.size} completed",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        items(state.activities, key = Activity::id) { activity ->
            ActivityCard(
                activity = activity,
                completed = activity.id in state.completedIds,
                onCompletedChange = { completed ->
                    onCompletedChange(activity.id, completed)
                },
            )
        }
        item {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ActivityCard(
    activity: Activity,
    completed: Boolean,
    onCompletedChange: (Boolean) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
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
        )
    }
}
