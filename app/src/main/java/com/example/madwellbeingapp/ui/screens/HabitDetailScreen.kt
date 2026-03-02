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
            text = { Text("Delete \"${habit!!.displayName}\"? All data will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    Log.i("HabitDetail", "DELETE '${habit!!.displayName}' id=${habit!!.id}")
                    viewModel.deleteHabit(habit!!)
                    showDeleteDialog = false
                    onNavigateBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = habit?.displayName ?: "Habit Detail",
                onBack = onNavigateBack,
                trailing = {
                    Row {
                        Text("✏️", modifier = Modifier.clickable { onEditHabit(habitId) }.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.titleLarge)
                        Text("🗑️", modifier = Modifier.clickable { showDeleteDialog = true }.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.titleLarge)
                    }
                }
            )

            if (habit == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading…") }
                return@Scaffold
            }

            val h = habit!!
            val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info card
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(h.activityType, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Started: ${fmt.format(Date(h.startDate))}", style = MaterialTheme.typography.bodyMedium)
                        Text("Target: ${h.targetFrequency}x per week", style = MaterialTheme.typography.bodyMedium)
                        if (h.reminderEnabled) {
                            Text("Reminder: ${h.reminderHour.toString().padStart(2, '0')}:${h.reminderMinute.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Swipe bar
                SwipeBar(isCompletedToday) {
                    Log.i("HabitDetail", "Swipe '${h.displayName}' id=${h.id} undo=$isCompletedToday")
                    viewModel.toggleTodayLog(habitId)
                }

                // Streaks
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("🔥 Current", "$currentStreak day${if (currentStreak != 1) "s" else ""}", Modifier.weight(1f))
                    StatCard("🏆 Longest", "$longestStreak day${if (longestStreak != 1) "s" else ""}", Modifier.weight(1f))
                }

                // Weekly progress
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Weekly Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { weeklyProgress },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${(weeklyProgress * 100).toInt()}% of weekly goal",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Last 14 days
                Text("Last 14 Days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val logDates = logs.filter { it.completed }.map { normalise(it.date) }.toSet()
                val today = todayMillis()
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items((0..13).map { today - it * 86_400_000L }.reversed()) { day ->
                        val done = logDates.contains(day)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(
                                    if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (done) "✓" else "",
                                    color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(SimpleDateFormat("dd", Locale.getDefault()).format(Date(day)),
                                style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        }
                    }
                }

                // Total completions
                val total = logs.count { it.completed }
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Total Completions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("$total time${if (total != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SwipeBar(isCompleted: Boolean, onSwipe: () -> Unit) {
    val threshold = 200f
    var offsetX by remember { mutableFloatStateOf(0f) }
    Box(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp))) {
        Box(
            modifier = Modifier.matchParentSize().background(
                if (isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (isCompleted) "Swipe → to Undo" else "Swipe → to Complete",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                color = if (isCompleted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
        }
        Box(
            modifier = Modifier.size(80.dp, 56.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary)
                .pointerInput(isCompleted) {
                    detectHorizontalDragGestures(
                        onDragEnd = { if (offsetX > threshold) onSwipe(); offsetX = 0f },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, d -> offsetX = (offsetX + d).coerceIn(0f, threshold + 50f) })
                },
            contentAlignment = Alignment.Center
        ) {
            Text(if (isCompleted) "✅" else "→", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
    }
}

private fun normalise(millis: Long): Long {
    val c = Calendar.getInstance(); c.timeInMillis = millis
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun todayMillis(): Long {
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}
