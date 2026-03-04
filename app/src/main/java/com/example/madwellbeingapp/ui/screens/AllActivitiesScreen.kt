package com.example.madwellbeingapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.madwellbeingapp.data.model.Habit
import com.example.madwellbeingapp.ui.viewmodel.HabitViewModel

@Composable
fun AllActivitiesScreen(
    viewModel: HabitViewModel,
    onNavigateBack: () -> Unit,
    onHabitClick: (Int) -> Unit
) {
    val habits by viewModel.allHabits.collectAsState()
    val todayLogs by viewModel.todayLogs.collectAsState()
    val completedIds = todayLogs.filter { it.completed }.map { it.habitId }.toSet()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(title = "All Activities", onBack = onNavigateBack)

            if (habits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center) {
                    Text("No activities yet. Go to Manage Activities to create one!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(habits, key = { it.id }) { habit ->
                        val done = completedIds.contains(habit.id)
                        HabitCard(
                            habit = habit,
                            viewModel = viewModel,
                            onClick = { onHabitClick(habit.id) },
                            circleColor = cardCircleColor(habit, done),
                            circleText = cardCircleText(habit, done),
                            circleTextColor = if (!habit.isActive)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else if (done) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = cardContainerColor(habit, done),
                            contentAlpha = if (!habit.isActive) 0.5f else 1f,
                            badge = if (!habit.isActive) {
                                { Text("Disabled", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error) }
                            } else null,
                            trailing = if (!habit.isActive) {
                                {
                                    Button(
                                        onClick = { viewModel.enableHabit(habit) },
                                        modifier = Modifier.size(32.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            } else {
                                {
                                    Button(
                                        onClick = { viewModel.disableHabit(habit) },
                                        modifier = Modifier.size(32.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Text("−", fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun cardCircleColor(habit: Habit, completedToday: Boolean) = when {
    !habit.isActive -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    completedToday  -> MaterialTheme.colorScheme.primary
    else            -> MaterialTheme.colorScheme.surfaceVariant
}

private fun cardCircleText(habit: Habit, completedToday: Boolean) = when {
    !habit.isActive -> "—"
    completedToday  -> "✓"
    else            -> ""
}

@Composable
private fun cardContainerColor(habit: Habit, completedToday: Boolean) = when {
    !habit.isActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    completedToday  -> MaterialTheme.colorScheme.primaryContainer
    else            -> MaterialTheme.colorScheme.surface
}
