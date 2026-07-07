package com.marcel.personaltrainer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marcel.personaltrainer.model.CalendarPeriod
import com.marcel.personaltrainer.ui.DayProgress
import com.marcel.personaltrainer.ui.ProgressUiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    state: ProgressUiState,
    onPeriodChange: (CalendarPeriod) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    var selectedDate by rememberSaveable { mutableStateOf(state.date.toString()) }
    val selectedDay = state.calendarDays.find { it.date.toString() == selectedDate }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_period),
                )
            }
            Text(
                text = periodTitle(state.calendarAnchorDate, state.calendarPeriod, locale),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_period),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CalendarPeriod.entries.forEach { period ->
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = state.calendarPeriod == period,
                    onClick = { onPeriodChange(period) },
                    label = {
                        Text(
                            stringResource(
                                when (period) {
                                    CalendarPeriod.WEEK -> R.string.calendar_period_week
                                    CalendarPeriod.MONTH -> R.string.calendar_period_month
                                },
                            ),
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        CalendarGrid(
            days = state.calendarDays,
            period = state.calendarPeriod,
            today = state.date,
            selectedDate = selectedDay?.date,
            onDateSelected = { selectedDate = it.toString() },
        )
        Spacer(Modifier.height(20.dp))
        selectedDay?.let { day ->
            SelectedDayCard(day = day, locale = locale)
            Spacer(Modifier.height(20.dp))
        }
        val completed = state.calendarDays.sumOf(DayProgress::completedCount)
        val targets = state.calendarDays.sumOf(DayProgress::targetCount)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.period_progress),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.period_completed_count, completed, targets),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (targets > 0) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { completed.toFloat() / targets },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    days: List<DayProgress>,
    period: CalendarPeriod,
    today: LocalDate,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
) {
    WeekdayHeader()
    Spacer(Modifier.height(8.dp))

    val leadingEmptyDays = when (period) {
        CalendarPeriod.WEEK -> 0
        CalendarPeriod.MONTH -> days.firstOrNull()?.date?.dayOfWeek?.value?.minus(1) ?: 0
    }
    val cells = buildList<DayProgress?> {
        repeat(leadingEmptyDays) { add(null) }
        addAll(days)
        while (size % 7 != 0) {
            add(null)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(if (period == CalendarPeriod.WEEK) 0.72f else 0.82f),
                    ) {
                        if (day != null) {
                            CalendarDay(
                                day = day,
                                isToday = day.date == today,
                                selected = day.date == selectedDate,
                                onClick = { onDateSelected(day.date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val locale = LocalLocale.current.platformLocale
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DayOfWeek.entries.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.NARROW, locale),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun CalendarDay(
    day: DayProgress,
    isToday: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val complete = day.targetCount > 0 && day.completedCount == day.targetCount
    val background = when {
        complete -> MaterialTheme.colorScheme.primaryContainer
        day.completedCount > 0 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = background,
        border = when {
            selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            isToday -> BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
            else -> null
        },
        tonalElevation = if (complete) 1.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                text = "${day.completedCount}/${day.targetCount}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    complete -> MaterialTheme.colorScheme.primary
                    day.targetCount == 0 -> MaterialTheme.colorScheme.outline
                    else -> Color.Unspecified
                },
            )
        }
    }
}

@Composable
private fun SelectedDayCard(
    day: DayProgress,
    locale: Locale,
) {
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
                text = day.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            if (day.completedActivities.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_completed_exercises_on_day),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    day.completedActivities.forEach { activity ->
                        Column {
                            Text(
                                text = activity.localizedName(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = activity.localizedDescription(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun periodTitle(
    date: LocalDate,
    period: CalendarPeriod,
    locale: Locale,
): String =
    when (period) {
        CalendarPeriod.WEEK -> {
            val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
            val end = date.minusDays((date.dayOfWeek.value - 1).toLong()).plusDays(6)
            "${date.minusDays((date.dayOfWeek.value - 1).toLong()).format(formatter)} - ${end.format(formatter)}"
        }

        CalendarPeriod.MONTH -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    }
