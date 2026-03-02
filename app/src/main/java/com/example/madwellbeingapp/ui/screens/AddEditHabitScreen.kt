package com.example.madwellbeingapp.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.madwellbeingapp.data.model.Habit
import com.example.madwellbeingapp.ui.viewmodel.HabitViewModel
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Screen for creating or editing a habit.
 *
 * - Activity type is chosen from a dropdown populated by user-created types stored in Room.
 * - A "+" button lets the user add new activity types on the fly.
 * - Details field lets the user specialise the activity (e.g. "Legs").
 * - Reminder time uses a clock-style dial picker instead of typing.
 * - All text is title-cased. Duplicates are blocked.
 * - rememberSaveable preserves state across rotation.
 */
@Composable
fun AddEditHabitScreen(
    viewModel: HabitViewModel,
    existingHabit: Habit? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isEditing = existingHabit != null

    // Activity types from the database
    val activityTypes by viewModel.allActivityTypes.collectAsState()

    // ── Form state (survives rotation) ──────────────────────
    var selectedActivityName by rememberSaveable { mutableStateOf(existingHabit?.name ?: "") }
    var details by rememberSaveable { mutableStateOf(existingHabit?.details ?: "") }
    var targetFrequency by rememberSaveable {
        mutableStateOf((existingHabit?.targetFrequency ?: 7).toString())
    }
    var reminderEnabled by rememberSaveable { mutableStateOf(existingHabit?.reminderEnabled ?: true) }
    var reminderHour by rememberSaveable { mutableIntStateOf(existingHabit?.reminderHour ?: 9) }
    var reminderMinute by rememberSaveable { mutableIntStateOf(existingHabit?.reminderMinute ?: 0) }

    var nameError by rememberSaveable { mutableStateOf(false) }
    var showActivityDialog by rememberSaveable { mutableStateOf(false) }
    var showAddTypeDialog by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    // ── Activity type picker dialog ─────────────────────────
    if (showActivityDialog) {
        AlertDialog(
            onDismissRequest = { showActivityDialog = false },
            title = { Text("Select Activity Type") },
            text = {
                if (activityTypes.isEmpty()) {
                    Text(
                        "No activity types yet.\nTap the + button to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn {
                        items(activityTypes) { type ->
                            Text(
                                text = type.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedActivityName = type.name
                                        nameError = false
                                        showActivityDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (type.name == selectedActivityName)
                                    FontWeight.Bold else FontWeight.Normal,
                                color = if (type.name == selectedActivityName)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActivityDialog = false }) { Text("Close") }
            }
        )
    }

    // ── Add new activity type dialog ────────────────────────
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (typeError) {
                        Text(
                            "Name is empty or already exists",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
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
                                selectedActivityName = HabitViewModel.toTitleCase(newTypeName.trim())
                                nameError = false
                                showAddTypeDialog = false
                            } else {
                                typeError = true
                            }
                        }
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTypeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Time picker dialog (clock dial) ─────────────────────
    if (showTimePicker) {
        var tempHour by remember { mutableIntStateOf(reminderHour) }
        var tempMinute by remember { mutableIntStateOf(reminderMinute) }
        var pickingHour by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    if (pickingHour) "Select Hour" else "Select Minute",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Display current selection
                    Text(
                        text = "${tempHour.toString().padStart(2, '0')}:${tempMinute.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Toggle hour/minute
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { pickingHour = true }) {
                            Text(
                                "Hour",
                                fontWeight = if (pickingHour) FontWeight.Bold else FontWeight.Normal,
                                color = if (pickingHour) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { pickingHour = false }) {
                            Text(
                                "Minute",
                                fontWeight = if (!pickingHour) FontWeight.Bold else FontWeight.Normal,
                                color = if (!pickingHour) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Clock dial
                    ClockDial(
                        value = if (pickingHour) tempHour else tempMinute,
                        maxValue = if (pickingHour) 24 else 60,
                        step = if (pickingHour) 1 else 5,
                        onValueChange = {
                            if (pickingHour) tempHour = it else tempMinute = it
                        },
                        labels = if (pickingHour) (0..23 step 3).toList()
                        else (0..55 step 5).toList()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    reminderHour = tempHour
                    reminderMinute = tempMinute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    // ── Screen layout ───────────────────────────────────────
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScreenHeader(
                title = if (isEditing) "Edit Habit" else "New Habit",
                onBack = onNavigateBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Activity type dropdown + add button ─────────
                Text(
                    text = "Activity Type",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showActivityDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(1.dp),
                        colors = if (nameError) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ) else CardDefaults.cardColors()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
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
                            Text(
                                text = "▼",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // "+" button to add a new activity type
                    Button(
                        onClick = { showAddTypeDialog = true },
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                if (nameError) {
                    Text(
                        text = "Please select an activity type",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // ── Details text field ──────────────────────────
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details (optional)") },
                    placeholder = { Text("e.g. Legs, Upper Body, 5K") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Live preview
                if (selectedActivityName.isNotBlank()) {
                    val previewName = if (details.isBlank())
                        HabitViewModel.toTitleCase(selectedActivityName.trim())
                    else
                        HabitViewModel.toTitleCase(selectedActivityName.trim()) +
                                "(" + HabitViewModel.toTitleCase(details.trim()) + ")"
                    Text(
                        text = "Will display as: $previewName",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // ── Target frequency ────────────────────────────
                OutlinedTextField(
                    value = targetFrequency,
                    onValueChange = { targetFrequency = it.filter { c -> c.isDigit() } },
                    label = { Text("Target (times per week)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // ── Reminder toggle ─────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Daily Reminder", style = MaterialTheme.typography.bodyLarge)
                    Button(
                        onClick = { reminderEnabled = !reminderEnabled },
                        colors = if (reminderEnabled) ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) else ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (reminderEnabled) "ON" else "OFF")
                    }
                }

                if (reminderEnabled) {
                    // ── Reminder time — clock dial button ───────
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reminder Time", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${reminderHour.toString().padStart(2, '0')}:${reminderMinute.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Save button ─────────────────────────────────
                Button(
                    onClick = {
                        if (selectedActivityName.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        val freq = targetFrequency.toIntOrNull() ?: 7

                        if (isEditing && existingHabit != null) {
                            viewModel.updateHabit(
                                existingHabit.copy(
                                    name = selectedActivityName.trim(),
                                    details = details.trim(),
                                    activityType = selectedActivityName.trim(),
                                    targetFrequency = freq,
                                    reminderEnabled = reminderEnabled,
                                    reminderHour = reminderHour,
                                    reminderMinute = reminderMinute
                                )
                            )
                            Log.i("AddEditHabitScreen", "User updated habit: name='${selectedActivityName.trim()}'")
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
                                    if (success) {
                                        Log.i("AddEditHabitScreen", "User created habit: name='${selectedActivityName.trim()}'")
                                        onNavigateBack()
                                    } else {
                                        Toast.makeText(context, "This activity already exists!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
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

// ─────────────────────────────────────────────────────────────
// Clock-style dial picker — pure Compose Canvas, no 3rd-party
// ─────────────────────────────────────────────────────────────

/**
 * A circular dial the user drags to pick a value.
 * [value] is the current selection, [maxValue] is the range (24 for hours, 60 for minutes).
 * [step] snaps to multiples (1 for hours, 5 for minutes).
 * [labels] are the numbers drawn around the ring.
 */
@Composable
private fun ClockDial(
    value: Int,
    maxValue: Int,
    step: Int,
    onValueChange: (Int) -> Unit,
    labels: List<Int>
) {
    val sizeDp = 220.dp
    val sizePx = with(LocalDensity.current) { sizeDp.toPx() }
    val center = sizePx / 2f
    val radius = center * 0.82f
    val labelRadius = center * 0.68f
    val knobRadius = 18f

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    // Convert current value to angle (0 at top = -PI/2)
    fun valueToAngle(v: Int): Float {
        return (2f * PI.toFloat() * v / maxValue) - (PI.toFloat() / 2f)
    }

    var currentAngle by remember(value) { mutableFloatStateOf(valueToAngle(value)) }

    Box(
        modifier = Modifier
            .size(sizeDp)
            .pointerInput(maxValue, step) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val pos = change.position
                    val angle = atan2(pos.y - center, pos.x - center)
                    currentAngle = angle

                    // Convert angle back to value
                    var normalized = angle + (PI.toFloat() / 2f)
                    if (normalized < 0) normalized += 2f * PI.toFloat()
                    var raw = (normalized / (2f * PI.toFloat()) * maxValue).roundToInt()
                    // Snap to step
                    raw = ((raw.toFloat() / step).roundToInt() * step).coerceIn(0, maxValue - 1)
                    onValueChange(raw)
                }
            }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            // Outer ring
            drawCircle(
                color = surfaceVariantColor,
                radius = radius,
                center = Offset(center, center),
                style = Stroke(width = 4f)
            )

            // Draw labels around the circle
            for (lbl in labels) {
                val a = valueToAngle(lbl)
                val lx = center + labelRadius * cos(a)
                val ly = center + labelRadius * sin(a)
                drawCircle(
                    color = if (lbl == value) primaryColor else Color.Transparent,
                    radius = 16f,
                    center = Offset(lx, ly)
                )
            }

            // Line from center to knob
            val knobAngle = valueToAngle(value)
            val kx = center + radius * cos(knobAngle)
            val ky = center + radius * sin(knobAngle)
            drawLine(
                color = primaryColor,
                start = Offset(center, center),
                end = Offset(kx, ky),
                strokeWidth = 3f
            )

            // Knob
            drawCircle(color = primaryColor, radius = knobRadius, center = Offset(kx, ky))

            // Center dot
            drawCircle(color = primaryColor, radius = 6f, center = Offset(center, center))
        }

        // Draw text labels on top of canvas using Compose Text
        labels.forEach { lbl ->
            val a = valueToAngle(lbl)
            val lx = center + labelRadius * cos(a)
            val ly = center + labelRadius * sin(a)
            val offsetXDp = with(LocalDensity.current) { lx.toDp() }
            val offsetYDp = with(LocalDensity.current) { ly.toDp() }
            Text(
                text = lbl.toString().padStart(2, '0'),
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = (offsetXDp - 16.dp).coerceAtLeast(0.dp))
                    .padding(top = (offsetYDp - 8.dp).coerceAtLeast(0.dp)),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (lbl == value) FontWeight.Bold else FontWeight.Normal,
                color = if (lbl == value) onPrimaryColor else onSurfaceColor,
                textAlign = TextAlign.Center
            )
        }

        // Big selected value in the center
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
