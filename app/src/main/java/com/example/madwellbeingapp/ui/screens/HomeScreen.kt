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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Extended header with summary
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("←", modifier = Modifier.clickable(onClick = onNavigateBack).padding(end = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    Text("Manage Activities", style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(12.dp))
                val done = activeHabits.count { completedIds.contains(it.id) }
                val total = activeHabits.size
                Text("$done / $total habits completed today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Medium)
                if (total > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { done.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.onPrimary,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
                }
            }

            // List or empty state
            if (activeHabits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f).padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No habits yet!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap the button below to create your first habit", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeHabits, key = { it.id }) { habit ->
                        SwipeHabitCard(
                            habit = habit,
                            completed = completedIds.contains(habit.id),
                            onSwipe = { viewModel.toggleTodayLog(habit.id) },
                            onClick = { onHabitClick(habit.id) },
                            viewModel = viewModel
                        )
                    }
                }
            }

            Button(
                onClick = onAddHabit,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("+ Add New Habit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Habit card with swipe-to-complete gesture. */
@Composable
private fun SwipeHabitCard(
    habit: Habit, completed: Boolean, onSwipe: () -> Unit,
    onClick: () -> Unit, viewModel: HabitViewModel
) {
    val weeklyProgress by viewModel.weeklyProgress(habit.id).collectAsState(initial = 0f)
    val threshold = 150f
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
        // Swipe background
        Box(
            modifier = Modifier.matchParentSize().background(
                if (completed) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                if (completed) "  ← Undo" else "  Swipe → to Complete",
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                color = if (completed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
        // Sliding card
        Card(
            modifier = Modifier.fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(completed) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > threshold) {
                                Log.d("SwipeGesture", "Swiped '${habit.displayName}'")
                                onSwipe()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, d -> offsetX = (offsetX + d).coerceIn(0f, threshold + 50f) }
                    )
                }.clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (completed) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(
                        if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (completed) "✓" else "",
                        color = if (completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(habit.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(habit.activityType + "  •  ${habit.targetFrequency}x/week",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { weeklyProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Weekly: ${(weeklyProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        if (!completed) Text("Swipe → to complete",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
