package com.example.madwellbeingapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.madwellbeingapp.ui.screens.ActiveOnlyScreen
import com.example.madwellbeingapp.ui.screens.AddEditHabitScreen
import com.example.madwellbeingapp.ui.screens.AllActivitiesScreen
import com.example.madwellbeingapp.ui.screens.HabitDetailScreen
import com.example.madwellbeingapp.ui.screens.HomeScreen
import com.example.madwellbeingapp.ui.screens.LandingScreen
import com.example.madwellbeingapp.ui.theme.MadWellbeingAppTheme
import com.example.madwellbeingapp.ui.viewmodel.HabitViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or not */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MadWellbeingAppTheme {
                HabitNavGraph()
            }
        }
    }
}

/**
 * Simple sealed class representing each screen destination.
 * We avoid using Navigation Compose library and use manual state-based navigation.
 */
sealed class Screen {
    /** Landing page with 3 navigation buttons — first page the user sees. */
    data object Landing : Screen()
    /** All activities — past completed + currently active. */
    data object AllActivities : Screen()
    /** Manage activities — add new habits, swipe to complete. */
    data object Home : Screen()
    /** Active only — habits still to-do today. */
    data object ActiveOnly : Screen()
    data object AddHabit : Screen()
    data class EditHabit(val habitId: Int) : Screen()
    data class HabitDetail(val habitId: Int) : Screen()
}

@Composable
fun HabitNavGraph(viewModel: HabitViewModel = viewModel()) {
    // Manual back-stack — starts on the Landing screen
    val backStack = remember { mutableStateListOf<Screen>(Screen.Landing) }
    val currentScreen = backStack.last()

    fun navigateTo(screen: Screen) {
        backStack.add(screen)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    when (currentScreen) {
        is Screen.Landing -> {
            LandingScreen(
                onAllActivities = { navigateTo(Screen.AllActivities) },
                onManageActivities = { navigateTo(Screen.Home) },
                onActiveOnly = { navigateTo(Screen.ActiveOnly) }
            )
        }

        is Screen.AllActivities -> {
            AllActivitiesScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBack() },
                onHabitClick = { id -> navigateTo(Screen.HabitDetail(id)) }
            )
        }

        is Screen.Home -> {
            HomeScreen(
                viewModel = viewModel,
                onAddHabit = { navigateTo(Screen.AddHabit) },
                onHabitClick = { id -> navigateTo(Screen.HabitDetail(id)) },
                onNavigateBack = { navigateBack() }
            )
        }

        is Screen.ActiveOnly -> {
            ActiveOnlyScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBack() },
                onHabitClick = { id -> navigateTo(Screen.HabitDetail(id)) }
            )
        }

        is Screen.AddHabit -> {
            AddEditHabitScreen(
                viewModel = viewModel,
                existingHabit = null,
                onNavigateBack = { navigateBack() }
            )
        }

        is Screen.EditHabit -> {
            val allHabits by viewModel.allHabits.collectAsState()
            val habit = allHabits.find { it.id == currentScreen.habitId }
            if (habit != null) {
                AddEditHabitScreen(
                    viewModel = viewModel,
                    existingHabit = habit,
                    onNavigateBack = { navigateBack() }
                )
            }
        }

        is Screen.HabitDetail -> {
            HabitDetailScreen(
                habitId = currentScreen.habitId,
                viewModel = viewModel,
                onNavigateBack = { navigateBack() },
                onEditHabit = { id -> navigateTo(Screen.EditHabit(id)) }
            )
        }
    }
}