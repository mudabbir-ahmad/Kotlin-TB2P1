package com.example.madwellbeingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.madwellbeingapp.data.model.Habit
import com.example.madwellbeingapp.ui.viewmodel.HabitViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HabitViewModel,
    onAddHabit: () -> Unit,
    onHabitClick: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val habits by viewModel.allHabits.collectAsState()
    val todayLogs by viewModel.todayLogs.collectAsState()
    val completedIds = todayLogs.filter { it.completed }.map { it.habitId }.toSet()
    val activeHabits = habits.filter { it.isActive }

    // Toggle between "Week" and "Month" view for the progress dropdown
    var showMonth by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(title = "Manage Activities", onBack = onNavigateBack)

            // Summary bar
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                val done = activeHabits.count { completedIds.contains(it.id) }
                Text(
                    "$done / ${activeHabits.size} habits completed today",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // Week / Month toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (showMonth) "Showing: Month" else "Showing: Week",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    TextButton(onClick = { showMonth = !showMonth }) {
                        Text(
                            if (showMonth) "Switch to Week ▲" else "Expand to Month ▼",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (activeHabits.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No habits yet!", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap the button below to create your first habit",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeHabits, key = { it.id }) { habit ->
                        val completed = completedIds.contains(habit.id)
                        SwipeHabitCard(
                            habit = habit,
                            completed = completed,
                            onSwipeRight = { viewModel.toggleTodayLog(habit.id) },
                            onSwipeLeft = { viewModel.disableHabit(habit) },
                            onClick = { onHabitClick(habit.id) },
                            viewModel = viewModel,
                            showMonth = showMonth
                        )
                    }
                }
            }

            Button(
                onClick = onAddHabit,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("+ Add New Habit", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Habit card:
 *  • swipe left→right = mark complete (or undo)
 *  • swipe right→left = disable
 *  • streak counter (✓ per consecutive day) replaces the old − button
 */
@Composable
private fun SwipeHabitCard(
    habit: Habit, completed: Boolean, onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit, onClick: () -> Unit, viewModel: HabitViewModel,
    showMonth: Boolean
) {
    val progress by (if (showMonth) viewModel.monthlyProgress(habit.id)
    else viewModel.weeklyProgress(habit.id)).collectAsState(initial = 0f)

    // Streak counter
    var streak by remember { mutableIntStateOf(0) }
    LaunchedEffect(habit.id, completed) {
        streak = viewModel.computeStreakForHabit(habit.id)
    }

    val threshold = 150f
    var offsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Right-swipe reveal (complete / undo)
        Box(
            modifier = Modifier.matchParentSize()
                .background(
                    if (completed) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                if (completed) "  ← Undo" else "  → Complete",
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                color = if (completed) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }
        // Left-swipe reveal (disable)
        Box(
            modifier = Modifier.matchParentSize()
                .background(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                "Disable ←  ",
                modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Sliding card
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(completed) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > threshold) onSwipeRight()
                            else if (offsetX < -threshold) onSwipeLeft()
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, d ->
                            offsetX = (offsetX + d).coerceIn(-(threshold + 50f), threshold + 50f)
                        }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (completed) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(habit.displayName, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            habit.activityType + "  •  ${habit.targetFrequency}x/week",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${if (showMonth) "Monthly" else "Weekly"}: ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Streak counter – show ✓ per consecutive day (max 7 visible)
                    val visibleStreak = streak.coerceAtMost(7)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "✓".repeat(visibleStreak) + if (streak > 7) "+" else "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (streak > 0) {
                            Text(
                                "${streak}d streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (!completed) {
                        Text("→ complete  ← disable",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    } else {
                        Text("→ undo  ← disable",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
