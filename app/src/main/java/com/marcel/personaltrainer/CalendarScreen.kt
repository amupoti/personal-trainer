package com.marcel.personaltrainer

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Text("<", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = periodTitle(state.calendarAnchorDate, state.calendarPeriod),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onNext) {
                Text(">", style = MaterialTheme.typography.titleLarge)
            }
        }
        PrimaryTabRow(
            selectedTabIndex = state.calendarPeriod.ordinal,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            CalendarPeriod.entries.forEach { period ->
                Tab(
                    selected = state.calendarPeriod == period,
                    onClick = { onPeriodChange(period) },
                    text = { Text(period.name.lowercase().replaceFirstChar(Char::uppercase)) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        CalendarGrid(
            days = state.calendarDays,
            period = state.calendarPeriod,
            today = state.date,
        )
        Spacer(Modifier.height(20.dp))
        val completed = state.calendarDays.sumOf(DayProgress::completedCount)
        val targets = state.calendarDays.sumOf(DayProgress::targetCount)
        Text(
            text = "$completed of $targets target exercises completed",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun CalendarGrid(
    days: List<DayProgress>,
    period: CalendarPeriod,
    today: LocalDate,
) {
    WeekdayHeader()
    Spacer(Modifier.height(6.dp))

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

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(if (period == CalendarPeriod.WEEK) 0.72f else 0.82f),
                    ) {
                        if (day != null) {
                            CalendarDay(day = day, isToday = day.date == today)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DayOfWeek.entries.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
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
) {
    val complete = day.targetCount > 0 && day.completedCount == day.targetCount
    val background = when {
        complete -> MaterialTheme.colorScheme.primaryContainer
        day.completedCount > 0 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.small,
        color = background,
        border = if (isToday) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
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
                color = if (complete) MaterialTheme.colorScheme.primary else Color.Unspecified,
            )
        }
    }
}

private fun periodTitle(date: LocalDate, period: CalendarPeriod): String =
    when (period) {
        CalendarPeriod.WEEK -> {
            val formatter = DateTimeFormatter.ofPattern("d MMM")
            val end = date.minusDays((date.dayOfWeek.value - 1).toLong()).plusDays(6)
            "${date.minusDays((date.dayOfWeek.value - 1).toLong()).format(formatter)} - ${end.format(formatter)}"
        }

        CalendarPeriod.MONTH -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
