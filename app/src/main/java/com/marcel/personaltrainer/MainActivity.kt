package com.marcel.personaltrainer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.marcel.personaltrainer.model.MilestoneBadge
import com.marcel.personaltrainer.model.PerfectAchievements
import com.marcel.personaltrainer.model.TargetUnit
import com.marcel.personaltrainer.model.ThemePreference
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
import kotlin.math.roundToInt

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
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            TimerSoundEffects(viewModel.timerSounds)
            DailyMovementTheme(themePreference = state.themePreference) {
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
                    onUpdateActivity = viewModel::updateActivity,
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
                    onThemePreferenceChange = viewModel::setThemePreference,
                )
            }
        }
    }
}

@Composable
private fun DailyMovementTheme(
    themePreference: ThemePreference,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF81D5C7),
            onPrimary = Color(0xFF003730),
            primaryContainer = Color(0xFF005047),
            onPrimaryContainer = Color(0xFF9EF2E2),
            secondary = Color(0xFFB1CCC5),
            secondaryContainer = Color(0xFF334B46),
            onSecondaryContainer = Color(0xFFCDE8E1),
            background = Color(0xFF0E1513),
            surface = Color(0xFF0E1513),
            surfaceVariant = Color(0xFF3F4946),
            outline = Color(0xFF89938F),
        )
    } else {
        lightColorScheme(
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
        )
    }
    val systemBarColor = Color.Transparent.toArgb()
    val activity = LocalContext.current as? ComponentActivity
    SideEffect {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = systemBarColor,
                darkScrim = systemBarColor,
                detectDarkMode = { darkTheme },
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = systemBarColor,
                darkScrim = systemBarColor,
                detectDarkMode = { darkTheme },
            ),
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
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
    onUpdateActivity: (String, String, Set<DayOfWeek>, Int, TargetUnit, String) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onToggleTimer: (String) -> Unit,
    onResetTimer: (String) -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, LocalTime) -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
) {
    var selectedView by rememberSaveable { mutableIntStateOf(0) }
    var addingExercise by rememberSaveable { mutableStateOf(false) }
    var editingExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingExercise = state.allActivities.find { it.id == editingExerciseId }
    val showingExerciseForm = addingExercise || editingExercise != null
    val destinations = listOf(
        Pair(stringResource(R.string.navigation_today), Icons.Rounded.Home),
        Pair(stringResource(R.string.navigation_calendar), Icons.Rounded.DateRange),
        Pair(stringResource(R.string.navigation_exercises), Icons.AutoMirrored.Rounded.List),
        Pair(stringResource(R.string.navigation_settings), Icons.Rounded.Settings),
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!showingExerciseForm) {
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
                                    contentDescription = destination.first,
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
            if (showingExerciseForm) {
                ExerciseFormScreen(
                    activity = editingExercise,
                    initialName = editingExercise?.localizedName().orEmpty(),
                    onCancel = {
                        addingExercise = false
                        editingExerciseId = null
                    },
                    onSave = { name, weekdays, targetValue, targetUnit, videoUrl ->
                        if (editingExercise == null) {
                            onAddActivity(name, weekdays, targetValue, targetUnit, videoUrl)
                        } else {
                            onUpdateActivity(
                                editingExercise.id,
                                name,
                                weekdays,
                                targetValue,
                                targetUnit,
                                videoUrl,
                            )
                        }
                        addingExercise = false
                        editingExerciseId = null
                    },
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(
                        text = stringResource(R.string.title_daily_movement),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = stringResource(R.string.subtitle_daily_movement),
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

                    2 -> ExerciseScreen(
                        activities = state.allActivities,
                        weeklyStats = state.weeklyExerciseStats,
                        monthlyStats = state.monthlyExerciseStats,
                        onAdd = { addingExercise = true },
                        onEdit = { editingExerciseId = it.id },
                        onDelete = onDeleteActivity,
                    )

                    else -> SettingsScreen(
                        settings = state.reminderSettings,
                        themePreference = state.themePreference,
                        onEnabledChange = onRemindersEnabledChange,
                        onTimeChange = onReminderTimeChange,
                        onThemePreferenceChange = onThemePreferenceChange,
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
    val locale = LocalLocale.current.platformLocale
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            val weeklyProgress = state.weeklyProgress
            val completed = weeklyProgress.completedCount
            val total = weeklyProgress.targetCount
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = state.date.format(
                            DateTimeFormatter.ofPattern("EEEE, d MMMM", locale),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (total == 0) {
                            stringResource(R.string.no_exercises)
                        } else {
                            stringResource(R.string.weekly_completed_count, completed, total)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    if (total > 0) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { weeklyProgress.percentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(
                                R.string.weekly_goal_percentage,
                                weeklyProgress.percentage.roundToInt(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (weeklyProgress.isComplete) {
                                stringResource(R.string.weekly_goal_complete)
                            } else {
                                pluralStringResource(
                                    R.plurals.weekly_goal_remaining,
                                    weeklyProgress.remainingCount,
                                    weeklyProgress.remainingCount,
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
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
                    Text(
                        text = stringResource(R.string.daily_streak),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = pluralStringResource(
                            R.plurals.streak_days,
                            state.streak.current,
                            state.streak.current,
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            when {
                                !state.streak.isTodayScheduled -> R.string.streak_rest_day
                                state.streak.isTodayComplete -> R.string.streak_extended_today
                                state.streak.current > 0 -> R.string.complete_today_keep_streak
                                else -> R.string.complete_today_start_streak
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.longest_streak,
                            pluralStringResource(
                                R.plurals.streak_days,
                                state.streak.longest,
                                state.streak.longest,
                            ),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        if (state.milestoneBadges.isNotEmpty()) {
            item {
                MilestoneBadgesCard(badges = state.milestoneBadges)
            }
        }
        item {
            PerfectAchievementsCard(achievements = state.perfectAchievements)
        }
        val suggestedIds = state.suggestedActivities.map(Activity::id).toSet()
        val otherActivities = state.activities.filterNot { it.id in suggestedIds }
        if (state.suggestedActivities.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.suggested_today),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        items(state.suggestedActivities, key = Activity::id) { activity ->
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
        if (otherActivities.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.other_exercises),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        items(otherActivities, key = Activity::id) { activity ->
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
private fun PerfectAchievementsCard(
    achievements: PerfectAchievements,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.perfect_achievements),
                style = MaterialTheme.typography.titleMedium,
            )
            AchievementCountRow(
                label = stringResource(R.string.perfect_days),
                count = pluralStringResource(
                    R.plurals.perfect_day_count,
                    achievements.perfectDayCount,
                    achievements.perfectDayCount,
                ),
                complete = achievements.isTodayPerfect,
                status = if (achievements.isTodayPerfect) {
                    stringResource(R.string.perfect_today_complete)
                } else {
                    stringResource(R.string.perfect_today_incomplete)
                },
            )
            AchievementCountRow(
                label = stringResource(R.string.perfect_weeks),
                count = pluralStringResource(
                    R.plurals.perfect_week_count,
                    achievements.perfectWeekCount,
                    achievements.perfectWeekCount,
                ),
                complete = achievements.isThisWeekPerfect,
                status = if (achievements.isThisWeekPerfect) {
                    stringResource(R.string.perfect_week_complete)
                } else {
                    stringResource(R.string.perfect_week_incomplete)
                },
            )
        }
    }
}

@Composable
private fun AchievementCountRow(
    label: String,
    count: String,
    complete: Boolean,
    status: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = if (complete) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (complete) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = count.takeWhile(Char::isDigit),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = count,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun MilestoneBadgesCard(
    badges: List<MilestoneBadge>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.milestone_badges),
                style = MaterialTheme.typography.titleMedium,
            )
            badges.forEach { badge ->
                MilestoneBadgeRow(badge = badge)
            }
        }
    }
}

@Composable
private fun MilestoneBadgeRow(
    badge: MilestoneBadge,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = if (badge.isUnlocked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (badge.isUnlocked) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = badge.completedDayTarget.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pluralStringResource(
                    R.plurals.milestone_completed_days,
                    badge.completedDayTarget,
                    badge.completedDayTarget,
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (badge.isUnlocked) {
                    stringResource(R.string.milestone_unlocked)
                } else {
                    pluralStringResource(
                        R.plurals.milestone_days_remaining,
                        badge.remainingCount,
                        badge.remainingCount,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
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
    val displayName = activity.localizedName()
    val pauseTimerDescription = stringResource(R.string.pause_timer)
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
                            text = displayName.firstOrNull()?.uppercase() ?: "",
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
                        text = displayName,
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
                                    contentDescription = stringResource(R.string.reset_timer),
                                )
                            }
                        }
                        IconButton(onClick = onToggleTimer) {
                            if (activityTimer?.isRunning == true) {
                                Text(
                                    text = "\u2161",
                                    modifier = Modifier.semantics {
                                        contentDescription = pauseTimerDescription
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = stringResource(R.string.start_timer),
                                )
                            }
                        }
                    }
                }
                Text(
                    text = activity.localizedDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (activity.videoUrl.isNotBlank()) {
                    TextButton(
                        onClick = { uriHandler.openUri(activity.videoUrl) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text(stringResource(R.string.open_video))
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
    DailyMovementTheme(themePreference = ThemePreference.LIGHT) {
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
