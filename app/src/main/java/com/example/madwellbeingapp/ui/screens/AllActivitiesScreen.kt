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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import kotlin.math.roundToInt

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
                        val completed = completedIds.contains(habit.id)
                        AllActivitiesSwipeCard(
                            habit = habit,
                            completed = completed,
                            viewModel = viewModel,
                            onSwipeRight = {
                                if (habit.isActive) viewModel.toggleTodayLog(habit.id)
                            },
                            onSwipeLeft = {
                                if (habit.isActive) viewModel.disableHabit(habit)
                                else viewModel.enableHabit(habit)
                            },
                            onClick = { onHabitClick(habit.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Swipeable card for All Activities screen.
 * • Active habits: swipe left→right = complete/undo, right→left = disable
 * • Disabled habits: shown greyed out, right→left = re-enable
 */
@Composable
private fun AllActivitiesSwipeCard(
    habit: Habit,
    completed: Boolean,
    viewModel: HabitViewModel,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onClick: () -> Unit
) {
    var streak by remember { mutableIntStateOf(0) }
    LaunchedEffect(habit.id, completed) {
        streak = viewModel.computeStreakForHabit(habit.id)
    }

    val threshold = 150f
    var offsetX by remember { mutableStateOf(0f) }
    val isActive = habit.isActive
    val contentAlpha = if (isActive) 1f else 0.45f

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Right-swipe reveal (complete/undo – only for active habits)
        if (isActive) {
            Box(
                modifier = Modifier.matchParentSize().background(
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
        }
        // Left-swipe reveal (disable / re-enable)
        Box(
            modifier = Modifier.matchParentSize().background(
                if (isActive) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                if (isActive) "Disable ←  " else "Enable ←  ",
                modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }

        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(isActive, completed) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > threshold) onSwipeRight()
                            else if (offsetX < -threshold) onSwipeLeft()
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, d ->
                            val minBound = -(threshold + 50f)
                            val maxBound = if (isActive) threshold + 50f else 0f
                            offsetX = (offsetX + d).coerceIn(minBound, maxBound)
                        }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    !isActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    completed -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                habit.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                            )
                            if (!isActive) {
                                Text(
                                    "  Disabled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Text(
                            "${habit.targetFrequency}x / week",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                        )
                    }
                    // Streak counter
                    if (isActive && streak > 0) {
                        val visibleStreak = streak.coerceAtMost(7)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "✓".repeat(visibleStreak) + if (streak > 7) "+" else "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                            )
                            Text(
                                "${streak}d streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (!isActive) "← swipe to re-enable"
                    else if (completed) "→ undo  ← disable"
                    else "→ complete  ← disable",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
