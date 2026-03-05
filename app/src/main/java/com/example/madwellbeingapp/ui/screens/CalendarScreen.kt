package com.example.madwellbeingapp.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.madwellbeingapp.data.model.Habit
import com.example.madwellbeingapp.ui.viewmodel.HabitViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: HabitViewModel,
    onNavigateBack: () -> Unit,
    onHabitClick: (Int) -> Unit
) {
    val habits by viewModel.allHabits.collectAsState()
    val activeHabits = habits.filter { it.isActive }

    // Current displayed month
    var displayYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var displayMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }

    // Compute start/end of displayed month
    val monthStart = remember(displayYear, displayMonth) {
        val cal = Calendar.getInstance()
        cal.set(displayYear, displayMonth, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val monthEnd = remember(displayYear, displayMonth) {
        val cal = Calendar.getInstance()
        cal.set(displayYear, displayMonth, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        cal.timeInMillis
    }

    // Logs for displayed month
    val monthLogs by viewModel.getLogsBetween(monthStart, monthEnd).collectAsState(initial = emptyList())

    // Compute which days have completions and how many habits were completed
    val dayCompletionMap = remember(monthLogs) {
        val map = mutableMapOf<Long, MutableSet<Int>>() // dayMillis -> set of habitIds
        for (log in monthLogs) {
            val dayMillis = normaliseDay(log.date)
            map.getOrPut(dayMillis) { mutableSetOf() }.add(log.habitId)
        }
        map
    }

    // Streaks for active habits
    val streakMap = remember { mutableMapOf<Int, Int>() }
    LaunchedEffect(activeHabits) {
        for (habit in activeHabits) {
            if (!streakMap.containsKey(habit.id)) {
                streakMap[habit.id] = viewModel.computeStreakForHabit(habit.id)
            }
        }
    }

    // Calendar grid data
    val daysInMonth = remember(displayYear, displayMonth) {
        val cal = Calendar.getInstance()
        cal.set(displayYear, displayMonth, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val firstDayOfWeek = remember(displayYear, displayMonth) {
        val cal = Calendar.getInstance()
        cal.set(displayYear, displayMonth, 1)
        // Monday=0, Sunday=6
        (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }
    val monthLabel = remember(displayYear, displayMonth) {
        val cal = Calendar.getInstance()
        cal.set(displayYear, displayMonth, 1)
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    // Total active habits count for day intensity
    val totalActive = activeHabits.size

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(title = "Calendar", onBack = onNavigateBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Month navigation
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            if (displayMonth == 0) { displayMonth = 11; displayYear-- }
                            else displayMonth--
                        }) {
                            Text("◀", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(monthLabel, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        TextButton(onClick = {
                            if (displayMonth == 11) { displayMonth = 0; displayYear++ }
                            else displayMonth++
                        }) {
                            Text("▶", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Weekday headers
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                            Text(
                                text = day, modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Calendar grid rows
                val totalCells = firstDayOfWeek + daysInMonth
                val rows = (totalCells + 6) / 7
                items(rows) { rowIndex ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..6) {
                            val cellIndex = rowIndex * 7 + col
                            val dayNum = cellIndex - firstDayOfWeek + 1
                            if (dayNum in 1..daysInMonth) {
                                val dayCal = Calendar.getInstance()
                                dayCal.set(displayYear, displayMonth, dayNum, 0, 0, 0)
                                dayCal.set(Calendar.MILLISECOND, 0)
                                val dayMillis = dayCal.timeInMillis
                                val count = dayCompletionMap[dayMillis]?.size ?: 0
                                val today = isToday(dayMillis)
                                val intensity = if (totalActive > 0) (count.toFloat() / totalActive).coerceIn(0f, 1f) else 0f

                                Box(
                                    modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                                        .background(
                                            when {
                                                today -> MaterialTheme.colorScheme.primaryContainer
                                                count == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                intensity < 0.5f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                intensity < 1f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                else -> MaterialTheme.colorScheme.primary
                                            },
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            dayNum.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                intensity >= 1f -> MaterialTheme.colorScheme.onPrimary
                                                today -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (count > 0) {
                                            Text("$count", style = MaterialTheme.typography.labelSmall,
                                                color = if (intensity >= 1f) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                                else MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            } else {
                                // Empty cell
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }

                // Legend
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(12.dp).background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
                        Text(" None  ", style = MaterialTheme.typography.labelSmall)
                        Box(modifier = Modifier.size(12.dp).background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
                        Text(" Some  ", style = MaterialTheme.typography.labelSmall)
                        Box(modifier = Modifier.size(12.dp).background(
                            MaterialTheme.colorScheme.primary,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
                        Text(" All", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Streak section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Habit Streaks", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                }

                if (activeHabits.isEmpty()) {
                    item {
                        Text("No active habits yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }
                } else {
                    items(activeHabits, key = { it.id }) { habit ->
                        val streak = streakMap[habit.id] ?: 0
                        StreakCard(habit = habit, streak = streak, onClick = { onHabitClick(habit.id) })
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun StreakCard(habit: Habit, streak: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (streak > 0) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fire icon for active streak
            Box(
                modifier = Modifier.size(40.dp).background(
                    if (streak > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (streak > 0) "🔥" else "—", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.displayName, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Text("${habit.targetFrequency}x / week",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$streak", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (streak > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (streak == 1) "day" else "days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun normaliseDay(millis: Long): Long {
    val c = Calendar.getInstance(); c.timeInMillis = millis
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun isToday(millis: Long): Boolean {
    val todayCal = Calendar.getInstance()
    todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0)
    todayCal.set(Calendar.SECOND, 0); todayCal.set(Calendar.MILLISECOND, 0)
    return millis == todayCal.timeInMillis
}
