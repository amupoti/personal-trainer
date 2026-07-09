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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marcel.personaltrainer.model.Activity
import com.marcel.personaltrainer.model.CompletionTrend
import com.marcel.personaltrainer.model.CompletionTrendDirection
import com.marcel.personaltrainer.model.ExerciseInsights
import com.marcel.personaltrainer.model.ExerciseStat
import com.marcel.personaltrainer.model.TargetUnit
import com.marcel.personaltrainer.model.WeekdayStat
import com.marcel.personaltrainer.model.isValidVideoUrl
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ExerciseScreen(
    activities: List<Activity>,
    weeklyStats: ExerciseInsights,
    weeklyTrend: CompletionTrend,
    monthlyStats: ExerciseInsights,
    monthlyTrend: CompletionTrend,
    yearlyStats: ExerciseInsights,
    yearlyTrend: CompletionTrend,
    onAdd: () -> Unit,
    onEdit: (Activity) -> Unit,
    onDelete: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            listOf(
                stringResource(R.string.exercise_tab_stats),
                stringResource(R.string.exercise_tab_manage),
            ).forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        if (selectedTab == 0) {
            ExerciseStatsScreen(
                weeklyStats = weeklyStats,
                weeklyTrend = weeklyTrend,
                monthlyStats = monthlyStats,
                monthlyTrend = monthlyTrend,
                yearlyStats = yearlyStats,
                yearlyTrend = yearlyTrend,
            )
        } else {
            ExerciseListScreen(
                activities = activities,
                onAdd = onAdd,
                onEdit = onEdit,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun ExerciseStatsScreen(
    weeklyStats: ExerciseInsights,
    weeklyTrend: CompletionTrend,
    monthlyStats: ExerciseInsights,
    monthlyTrend: CompletionTrend,
    yearlyStats: ExerciseInsights,
    yearlyTrend: CompletionTrend,
) {
    val locale = LocalLocale.current.platformLocale
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp, top = 14.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.exercise_insights),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            ExerciseStatsSection(
                title = stringResource(R.string.exercise_stats_this_week),
                insights = weeklyStats,
                trend = weeklyTrend,
                trendComparisonLabel = stringResource(R.string.exercise_trend_previous_week),
                locale = locale,
            )
        }
        item {
            ExerciseStatsSection(
                title = stringResource(R.string.exercise_stats_this_month),
                insights = monthlyStats,
                trend = monthlyTrend,
                trendComparisonLabel = stringResource(R.string.exercise_trend_previous_month),
                locale = locale,
            )
        }
        item {
            ExerciseStatsSection(
                title = stringResource(R.string.exercise_stats_this_year),
                insights = yearlyStats,
                trend = yearlyTrend,
                trendComparisonLabel = stringResource(R.string.exercise_trend_previous_year),
                locale = locale,
            )
        }
    }
}

@Composable
private fun ExerciseStatsSection(
    title: String,
    insights: ExerciseInsights,
    trend: CompletionTrend,
    trendComparisonLabel: String,
    locale: Locale,
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
            if (insights.scheduledCount == 0) {
                Text(
                    text = stringResource(R.string.exercise_stats_empty),
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                InsightSummary(
                    insights = insights,
                    trend = trend,
                    trendComparisonLabel = trendComparisonLabel,
                    locale = locale,
                )
                insights.exerciseStats.forEach { stat ->
                    ExerciseStatRow(stat)
                }
            }
        }
    }
}

@Composable
private fun InsightSummary(
    insights: ExerciseInsights,
    trend: CompletionTrend,
    trendComparisonLabel: String,
    locale: Locale,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(
                R.string.exercise_insight_completed_of_scheduled,
                insights.completedCount,
                insights.scheduledCount,
                insights.percentage.roundToInt(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        TrendInsightRow(trend, trendComparisonLabel)
        insights.bestWeekday?.let { weekday ->
            WeekdayInsightRow(
                label = stringResource(R.string.exercise_insight_best_day),
                stat = weekday,
                locale = locale,
            )
        }
        insights.weakestWeekday?.let { weekday ->
            WeekdayInsightRow(
                label = stringResource(R.string.exercise_insight_weakest_day),
                stat = weekday,
                locale = locale,
            )
        }
    }
}

@Composable
private fun TrendInsightRow(
    trend: CompletionTrend,
    comparisonLabel: String,
) {
    val text = when (trend.direction) {
        CompletionTrendDirection.UP -> stringResource(
            R.string.exercise_insight_trend_up,
            trend.percentagePointChange,
            comparisonLabel,
        )

        CompletionTrendDirection.DOWN -> stringResource(
            R.string.exercise_insight_trend_down,
            trend.percentagePointChange,
            comparisonLabel,
        )

        CompletionTrendDirection.UNCHANGED -> stringResource(
            R.string.exercise_insight_trend_unchanged,
            comparisonLabel,
        )
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun WeekdayInsightRow(
    label: String,
    stat: WeekdayStat,
    locale: Locale,
) {
    Text(
        text = stringResource(
            R.string.exercise_insight_weekday_detail,
            label,
            stat.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
            stat.percentage.roundToInt(),
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun ExerciseStatRow(stat: ExerciseStat) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stat.activity.localizedName(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    R.string.exercise_stat_percentage,
                    stat.percentage.roundToInt(),
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        LinearProgressIndicator(
            progress = { stat.percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.exercise_stat_completed_of_scheduled,
                stat.completedCount,
                stat.scheduledCount,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
fun ExerciseListScreen(
    activities: List<Activity>,
    onAdd: () -> Unit,
    onEdit: (Activity) -> Unit,
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
                    text = stringResource(R.string.my_exercises),
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
                    Text(stringResource(R.string.add_exercise))
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
                            text = stringResource(R.string.build_your_routine),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.add_exercise_empty_message),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
        items(activities, key = Activity::id) { activity ->
            val displayName = activity.localizedName()
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
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = activity.localizedDescription(),
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
                    Row {
                        IconButton(onClick = { onEdit(activity) }) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(
                                    R.string.edit_exercise_content_description,
                                    displayName,
                                ),
                            )
                        }
                        IconButton(onClick = { pendingDelete = activity }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(
                                    R.string.delete_exercise_content_description,
                                    displayName,
                                ),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { activity ->
        val displayName = activity.localizedName()
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_exercise_question)) },
            text = { Text(displayName) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(activity.id)
                        pendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun ExerciseFormScreen(
    activity: Activity?,
    initialName: String,
    onCancel: () -> Unit,
    onSave: (String, Set<DayOfWeek>, Int, TargetUnit, String) -> Unit,
) {
    var name by rememberSaveable(activity?.id) { mutableStateOf(initialName) }
    var selectedDaysMask by rememberSaveable(activity?.id) {
        mutableIntStateOf(
            activity?.weekdays?.fold(0) { mask, day -> mask or (1 shl day.ordinal) } ?: 0,
        )
    }
    var targetMode by rememberSaveable(activity?.id) {
        mutableIntStateOf(
            if (activity == null || activity.targetUnit == TargetUnit.REPETITIONS) 0 else 1,
        )
    }
    var targetValue by rememberSaveable(activity?.id) {
        mutableStateOf(activity?.targetValue?.toString().orEmpty())
    }
    var durationUnit by rememberSaveable(activity?.id) {
        mutableStateOf(
            activity?.targetUnit
                ?.takeUnless { it == TargetUnit.REPETITIONS }
                ?.name
                ?: TargetUnit.MINUTES.name,
        )
    }
    var videoUrl by rememberSaveable(activity?.id) {
        mutableStateOf(activity?.videoUrl.orEmpty())
    }

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
                        contentDescription = stringResource(R.string.back),
                    )
                }
                Text(
                    text = stringResource(
                        if (activity == null) R.string.add_exercise else R.string.edit_exercise,
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
        item {
            FormSection(title = stringResource(R.string.exercise)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.video_link_optional)) },
                    placeholder = { Text(stringResource(R.string.video_link_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    isError = !isValidVideoUrl(videoUrl),
                    supportingText = {
                        if (!isValidVideoUrl(videoUrl)) {
                            Text(stringResource(R.string.invalid_video_link))
                        }
                    },
                )
            }
        }
        item {
            FormSection(title = stringResource(R.string.schedule)) {
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
                        text = stringResource(R.string.select_at_least_one_day),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        item {
            FormSection(title = stringResource(R.string.target)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        stringResource(R.string.repetitions),
                        stringResource(R.string.duration),
                    ).forEachIndexed { index, label ->
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
                        Text(
                            stringResource(
                                if (targetMode == 0) {
                                    R.string.number_of_repetitions
                                } else {
                                    R.string.duration
                                },
                            ),
                        )
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
                                    Text(unit.localizedLabel())
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
                Text(stringResource(R.string.save_exercise))
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
            label = { Text(stringResource(R.string.all_days)) },
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

@Composable
internal fun Activity.localizedDescription(): String {
    val resource = when (targetUnit) {
        TargetUnit.REPETITIONS -> R.plurals.target_repetitions
        TargetUnit.SECONDS -> R.plurals.target_seconds
        TargetUnit.MINUTES -> R.plurals.target_minutes
    }
    return pluralStringResource(resource, targetValue, targetValue)
}

@Composable
internal fun Activity.localizedName(): String {
    if (!usesLocalizedName) return name
    val resource = when (id) {
        "hamstring_stretch" -> R.string.exercise_hamstring_stretch
        "glute_bridge" -> R.string.exercise_glute_bridge
        "pelvic_tilt" -> R.string.exercise_pelvic_tilt
        "cobra" -> R.string.exercise_cobra
        "opposite_leg_pull" -> R.string.exercise_opposite_leg_pull
        "band_arms" -> R.string.exercise_band_arms
        "band_knees" -> R.string.exercise_band_knees
        "standing_table_leg_curl" -> R.string.exercise_standing_table_leg_curl
        "side_plank" -> R.string.exercise_side_plank
        else -> return name
    }
    return stringResource(resource)
}

@Composable
private fun TargetUnit.localizedLabel(): String =
    stringResource(
        when (this) {
            TargetUnit.REPETITIONS -> R.string.repetitions
            TargetUnit.SECONDS -> R.string.seconds
            TargetUnit.MINUTES -> R.string.minutes
        },
    )
