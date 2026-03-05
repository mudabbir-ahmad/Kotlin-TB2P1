package com.example.madwellbeingapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.madwellbeingapp.ui.viewmodel.HabitViewModel

@Composable
fun ActiveOnlyScreen(
    viewModel: HabitViewModel,
    onNavigateBack: () -> Unit,
    onHabitClick: (Int) -> Unit
) {
    val activeHabits by viewModel.activeHabitsNotCompletedToday.collectAsState()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(title = "Active — To Do", onBack = onNavigateBack)

            if (activeHabits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("All caught up!", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("You've completed all your active habits for today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeHabits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            onClick = { onHabitClick(habit.id) },
                            circleText = if (habit.name.isNotBlank()) habit.name.first().toString() else "•",
                            circleTextColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
