package com.example.madwellbeingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.madwellbeingapp.ui.viewmodel.HabitViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val todayLogs by viewModel.todayLogs.collectAsState()
    val isCompletedToday = todayLogs.any { it.habitId == habitId && it.completed }



    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = habit?.displayName ?: "Habit Detail",
                onBack = onNavigateBack,
                trailing = {
                    TextButton(onClick = { onEditHabit(habitId) }) {
                        Text("Edit", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )

            if (habit == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading…")
                }
                return@Scaffold
            }

            val h = habit!!
            val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(h.name, style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Started: ${fmt.format(Date(h.startDate))}",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Target: ${h.targetFrequency}x per week",
                            style = MaterialTheme.typography.bodyMedium)
                        if (h.reminderEnabled) {
                            Text("Reminder: ${h.reminderHour.toString().padStart(2, '0')}:${h.reminderMinute.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Complete/Undo button
                Button(
                    onClick = { viewModel.toggleTodayLog(habitId) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(
                        if (isCompletedToday) "Undo Today" else "Mark Complete",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Streaks
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Current Streak",
                        "$currentStreak day${if (currentStreak != 1) "s" else ""}",
                        Modifier.weight(1f))
                    StatCard("Longest Streak",
                        "$longestStreak day${if (longestStreak != 1) "s" else ""}",
                        Modifier.weight(1f))
                }

                // Weekly progress
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Weekly Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val detailProgress by viewModel.weeklyProgress(habitId)
                            .collectAsState(initial = 0f)
                        Text(
                            "${(detailProgress * 100).toInt()}% of weekly goal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Last 14 days
                Text("Last 14 Days", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                val logDates = logs.filter { it.completed }.map { HabitViewModel.startOfDay(it.date) }.toSet()
                val today = HabitViewModel.todayMillis()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (0..13).map { today - it * 86_400_000L }.reversed().forEach { day ->
                        val done = logDates.contains(day)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp)
                                    .background(
                                        if (done) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (done) "✓" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (done) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                SimpleDateFormat("dd", Locale.getDefault()).format(Date(day)),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Total completions
                val total = logs.count { it.completed }
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Total Completions", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text("$total time${if (total != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

