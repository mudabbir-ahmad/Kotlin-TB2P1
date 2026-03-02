package com.example.madwellbeingapp.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.madwellbeingapp.ui.viewmodel.HabitViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HabitDetailScreen(
    habitId: Int,
    viewModel: HabitViewModel,
    onNavigateBack: () -> Unit,
    onEditHabit: (Int) -> Unit
) {
    LaunchedEffect(habitId) { viewModel.selectHabit(habitId) }

    val habit by viewModel.selectedHabit.collectAsState()
    val logs by viewModel.selectedHabitLogs.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val longestStreak by viewModel.longestStreak.collectAsState()
    val weeklyProgress by viewModel.weeklyProgress(habitId).collectAsState(initial = 0f)
    val todayLogs by viewModel.todayLogs.collectAsState()
    val isCompletedToday = todayLogs.any { it.habitId == habitId && it.completed }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && habit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Habit") },
            text = { Text("Are you sure you want to delete \"${habit!!.displayName}\"? All associated data will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    Log.i("HabitDetail", "User confirmed DELETE for habit '${habit!!.displayName}' (id=${habit!!.id})")
                    viewModel.deleteHabit(habit!!)
                    showDeleteDialog = false
                    onNavigateBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Custom header bar (replaces TopAppBar which uses material.icons)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "←",
                            modifier = Modifier
                                .clickable(onClick = onNavigateBack)
                                .padding(end = 12.dp),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = habit?.displayName ?: "Habit Detail",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row {
                        Text(
                            text = "✏️",
                            modifier = Modifier
                                .clickable { onEditHabit(habitId) }
                                .padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "🗑️",
                            modifier = Modifier
                                .clickable { showDeleteDialog = true }
                                .padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            if (habit == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading…")
                }
                return@Scaffold
            }

            val h = habit!!
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = h.activityType,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Started: ${dateFormat.format(Date(h.startDate))}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Target: ${h.targetFrequency}x per week",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (h.reminderEnabled) {
                            Text(
                                text = "Reminder: ${h.reminderHour.toString().padStart(2, '0')}:${h.reminderMinute.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Swipe to mark today as complete (or undo)
                SwipeToCompleteBar(
                    isCompletedToday = isCompletedToday,
                    onSwipeComplete = {
                        Log.i("HabitDetail", "Swipe-to-complete triggered for habit '${h.displayName}' (id=${h.id}), isUndo=${isCompletedToday}")
                        viewModel.toggleTodayLog(habitId)
                    }
                )

                // Streak cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "🔥 Current Streak",
                        value = "$currentStreak day${if (currentStreak != 1) "s" else ""}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "🏆 Longest Streak",
                        value = "$longestStreak day${if (longestStreak != 1) "s" else ""}",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Weekly progress
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Weekly Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { weeklyProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${(weeklyProgress * 100).toInt()}% of weekly goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Last 14 days heatmap
                Text(
                    text = "Last 14 Days",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                val logDates = logs.filter { it.completed }.map { normaliseToDay(it.date) }.toSet()
                val today = todayStartMillis()
                val last14 = (0..13).map { today - it * 86_400_000L }.reversed()

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(last14) { dayMillis ->
                        val completed = logDates.contains(dayMillis)
                        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (completed) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (completed) "✓" else "",
                                    color = if (completed) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dayFormat.format(Date(dayMillis)),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Total completions
                val totalCompletions = logs.count { it.completed }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📊", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Total Completions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "$totalCompletions time${if (totalCompletions != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Swipe-to-complete bar for the detail screen.
 * User must swipe right past a threshold to toggle completion.
 */
@Composable
private fun SwipeToCompleteBar(
    isCompletedToday: Boolean,
    onSwipeComplete: () -> Unit
) {
    val swipeThreshold = 200f
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        // Background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (isCompletedToday) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCompletedToday) "← Swipe → to Undo"
                else "← Swipe → to Complete Today",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCompletedToday) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.primary
            )
        }

        // Draggable thumb
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 56.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isCompletedToday) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary
                )
                .pointerInput(isCompletedToday) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > swipeThreshold) {
                                Log.d("SwipeGesture", "Detail swipe threshold reached, toggling completion")
                                onSwipeComplete()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(0f, swipeThreshold + 50f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCompletedToday) "✅" else "→",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun normaliseToDay(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun todayStartMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
