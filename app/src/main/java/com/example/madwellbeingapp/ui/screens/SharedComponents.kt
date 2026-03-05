package com.example.madwellbeingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.madwellbeingapp.data.model.Habit
import com.example.madwellbeingapp.ui.viewmodel.HabitViewModel

/** Standard header bar with a back button and a title. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) {
                    Text(
                        text = "←",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (trailing != null) trailing()
        }
    }
}

/**
 * Standard habit card used on list screens.
 */
@Composable
fun HabitCard(
    habit: Habit,
    viewModel: HabitViewModel,
    onClick: () -> Unit,
    circleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    circleText: String = "",
    circleTextColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    contentAlpha: Float = 1f,
    badge: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp)
                    .background(
                        circleColor.copy(alpha = contentAlpha),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = circleText,
                    color = circleTextColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habit.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        badge()
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${habit.targetFrequency}x / week",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }
    }
}
