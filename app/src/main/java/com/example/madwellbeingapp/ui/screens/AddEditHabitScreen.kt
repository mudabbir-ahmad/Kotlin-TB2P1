package com.example.madwellbeingapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.madwellbeingapp.data.model.Habit
import com.example.madwellbeingapp.ui.viewmodel.HabitViewModel

/**
 * Screen for creating or editing a habit.
 */
@Composable
fun AddEditHabitScreen(
    viewModel: HabitViewModel,
    existingHabit: Habit? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isEditing = existingHabit != null
    val activityTypes by viewModel.allActivityTypes.collectAsState()

    // Form state
    var selectedActivityName by remember { mutableStateOf(existingHabit?.name ?: "") }
    var details by remember { mutableStateOf(existingHabit?.details ?: "") }
    var targetFrequency by remember { mutableStateOf((existingHabit?.targetFrequency ?: 7).toString()) }
    var reminderEnabled by remember { mutableStateOf(existingHabit?.reminderEnabled ?: true) }
    var reminderHour by remember { mutableStateOf(existingHabit?.reminderHour ?: 9) }
    var reminderMinute by remember { mutableStateOf(existingHabit?.reminderMinute ?: 0) }

    var nameError by remember { mutableStateOf(false) }
    var showActivityDialog by remember { mutableStateOf(false) }
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Activity type picker dialog
    if (showActivityDialog) {
        AlertDialog(
            onDismissRequest = { showActivityDialog = false },
            title = { Text("Select Activity Type") },
            text = {
                if (activityTypes.isEmpty()) {
                    Text("No activity types yet.\nTap the + button to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(activityTypes) { type ->
                            TextButton(onClick = {
                                selectedActivityName = type.name
                                nameError = false
                                showActivityDialog = false
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = type.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (type.name == selectedActivityName) FontWeight.Bold
                                    else FontWeight.Normal,
                                    color = if (type.name == selectedActivityName) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showActivityDialog = false }) { Text("Close") } }
        )
    }

    // Add new activity type dialog
    if (showAddTypeDialog) {
        var newTypeName by remember { mutableStateOf("") }
        var typeError by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddTypeDialog = false },
            title = { Text("New Activity Type") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTypeName,
                        onValueChange = { newTypeName = it; typeError = false },
                        label = { Text("Type name") },
                        placeholder = { Text("e.g. Gym, Running, Yoga") },
                        singleLine = true,
                        isError = typeError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (typeError) {
                        Text("Name is empty or already exists",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTypeName.isBlank()) {
                        typeError = true
                    } else {
                        viewModel.addActivityType(newTypeName) { success ->
                            if (success) {
                                selectedActivityName = HabitViewModel.formatName(newTypeName.trim())
                                nameError = false
                                showAddTypeDialog = false
                            } else {
                                typeError = true
                            }
                        }
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddTypeDialog = false }) { Text("Cancel") } }
        )
    }

    // Time picker dialog (simple number-based instead of clock dial)
    if (showTimePicker) {
        var tempHour by remember { mutableStateOf(reminderHour.toString()) }
        var tempMinute by remember { mutableStateOf(reminderMinute.toString()) }
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set Reminder Time") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tempHour,
                        onValueChange = { tempHour = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Hour") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = tempMinute,
                        onValueChange = { tempMinute = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    reminderHour = (tempHour.toIntOrNull() ?: 9).coerceIn(0, 23)
                    reminderMinute = (tempMinute.toIntOrNull() ?: 0).coerceIn(0, 59)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    // Screen layout
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = if (isEditing) "Edit Habit" else "New Habit",
                onBack = onNavigateBack
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Activity type dropdown + add button
                Text("Activity Type", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        onClick = { showActivityDialog = true },
                        modifier = Modifier.weight(1f),
                        elevation = CardDefaults.cardElevation(1.dp),
                        colors = if (nameError) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer)
                        else CardDefaults.cardColors()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedActivityName.isBlank()) "Tap to select…"
                                else selectedActivityName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedActivityName.isBlank())
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text("▼", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = { showAddTypeDialog = true },
                        modifier = Modifier.size(52.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("+", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                    }
                }
                if (nameError) {
                    Text("Please select an activity type",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp))
                }

                // Details
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details (optional)") },
                    placeholder = { Text("e.g. Legs, Upper Body, 5K") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live preview
                if (selectedActivityName.isNotBlank()) {
                    val previewName = if (details.isBlank())
                        HabitViewModel.formatName(selectedActivityName.trim())
                    else
                        HabitViewModel.formatName(selectedActivityName.trim()) +
                                "(" + HabitViewModel.formatDetails(details.trim()) + ")"
                    Text("Will display as: $previewName",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp))
                }

                // Target frequency
                OutlinedTextField(
                    value = targetFrequency,
                    onValueChange = { targetFrequency = it.filter { c -> c.isDigit() } },
                    label = { Text("Target (times per week)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Reminder toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Daily Reminder", style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { reminderEnabled = !reminderEnabled }) {
                        Text(if (reminderEnabled) "ON" else "OFF")
                    }
                }

                if (reminderEnabled) {
                    Card(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reminder Time", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Save button
                Button(
                    onClick = {
                        if (selectedActivityName.isBlank()) { nameError = true; return@Button }
                        val freq = targetFrequency.toIntOrNull() ?: 7

                        if (isEditing) {
                            viewModel.updateHabit(
                                existingHabit!!.copy(
                                    name = selectedActivityName.trim(),
                                    details = details.trim(),
                                    activityType = selectedActivityName.trim(),
                                    targetFrequency = freq,
                                    reminderEnabled = reminderEnabled,
                                    reminderHour = reminderHour,
                                    reminderMinute = reminderMinute
                                )
                            )
                            onNavigateBack()
                        } else {
                            viewModel.addHabit(
                                name = selectedActivityName.trim(),
                                details = details.trim(),
                                activityType = selectedActivityName.trim(),
                                targetFrequency = freq,
                                reminderEnabled = reminderEnabled,
                                reminderHour = reminderHour,
                                reminderMinute = reminderMinute,
                                onResult = { success ->
                                    if (success) onNavigateBack()
                                    else Toast.makeText(context, "This activity already exists!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(
                        if (isEditing) "Update Habit" else "Create Habit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
