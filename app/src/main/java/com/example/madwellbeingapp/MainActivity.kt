package com.example.madwellbeingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.madwellbeingapp.ui.screens.ActiveOnlyScreen
import com.example.madwellbeingapp.ui.screens.AddEditHabitScreen
import com.example.madwellbeingapp.ui.screens.AllActivitiesScreen
import com.example.madwellbeingapp.ui.screens.CalendarScreen
import com.example.madwellbeingapp.ui.screens.HabitDetailScreen
import com.example.madwellbeingapp.ui.screens.HomeScreen
import com.example.madwellbeingapp.ui.screens.LandingScreen
import com.example.madwellbeingapp.ui.theme.MadWellbeingAppTheme
import com.example.madwellbeingapp.ui.viewmodel.HabitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MadWellbeingAppTheme { HabitNavGraph() } }
    }
}

// ── Screen destinations ─────────────────────────────────────
sealed class Screen {
    data object Landing : Screen()
    data object AllActivities : Screen()
    data object Home : Screen()
    data object ActiveOnly : Screen()
    data object Calendar : Screen()
    data object AddHabit : Screen()
    data class EditHabit(val habitId: Int) : Screen()
    data class HabitDetail(val habitId: Int) : Screen()
}

@Composable
fun HabitNavGraph(vm: HabitViewModel = viewModel()) {
    val backStack = remember { mutableStateListOf<Any>(Screen.Landing) }

    fun nav(screen: Screen) = backStack.add(screen)
    fun back() { if (backStack.size > 1) backStack.removeLast() }

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<Screen.Landing> {
                LandingScreen(
                    onAllActivities = { nav(Screen.AllActivities) },
                    onManageActivities = { nav(Screen.Home) },
                    onActiveOnly = { nav(Screen.ActiveOnly) },
                    onCalendar = { nav(Screen.Calendar) }
                )
            }
            entry<Screen.AllActivities> {
                AllActivitiesScreen(vm, onNavigateBack = ::back, onHabitClick = { nav(Screen.HabitDetail(it)) })
            }
            entry<Screen.Home> {
                HomeScreen(vm, onAddHabit = { nav(Screen.AddHabit) }, onHabitClick = { nav(Screen.HabitDetail(it)) }, onNavigateBack = ::back)
            }
            entry<Screen.ActiveOnly> {
                ActiveOnlyScreen(vm, onNavigateBack = ::back, onHabitClick = { nav(Screen.HabitDetail(it)) })
            }
            entry<Screen.Calendar> {
                CalendarScreen(vm, onNavigateBack = ::back, onHabitClick = { nav(Screen.HabitDetail(it)) })
            }
            entry<Screen.AddHabit> {
                AddEditHabitScreen(vm, existingHabit = null, onNavigateBack = ::back)
            }
            entry<Screen.EditHabit> { key ->
                val allHabits by vm.allHabits.collectAsState()
                allHabits.find { it.id == key.habitId }?.let { habit ->
                    AddEditHabitScreen(vm, existingHabit = habit, onNavigateBack = ::back)
                }
            }
            entry<Screen.HabitDetail> { key ->
                HabitDetailScreen(key.habitId, vm, onNavigateBack = ::back, onEditHabit = { nav(Screen.EditHabit(it)) })
            }
        }
    )
}