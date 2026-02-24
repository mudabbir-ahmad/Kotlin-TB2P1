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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.madwellbeingapp.ui.screens.AddEditHabitScreen
import com.example.madwellbeingapp.ui.screens.HabitDetailScreen
import com.example.madwellbeingapp.ui.screens.HomeScreen
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
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

@Composable
fun HabitNavGraph(viewModel: HabitViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onAddHabit = { navController.navigate("add_habit") },
                onHabitClick = { id -> navController.navigate("habit_detail/$id") }
            )
        }

        composable("add_habit") {
            AddEditHabitScreen(
                viewModel = viewModel,
                existingHabit = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit_habit/{habitId}",
            arguments = listOf(navArgument("habitId") { type = NavType.IntType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getInt("habitId") ?: return@composable
            // Observe the habit so the form is pre-populated
            val allHabits by viewModel.allHabits.collectAsState()
            val habit = allHabits.find { it.id == habitId }
            if (habit != null) {
                AddEditHabitScreen(
                    viewModel = viewModel,
                    existingHabit = habit,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = "habit_detail/{habitId}",
            arguments = listOf(navArgument("habitId") { type = NavType.IntType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getInt("habitId") ?: return@composable
            HabitDetailScreen(
                habitId = habitId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditHabit = { id -> navController.navigate("edit_habit/$id") }
            )
        }
    }
}