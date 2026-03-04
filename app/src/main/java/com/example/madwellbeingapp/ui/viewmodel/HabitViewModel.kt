package com.example.madwellbeingapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.madwellbeingapp.HabitApp
import com.example.madwellbeingapp.data.model.ActivityType
import com.example.madwellbeingapp.data.model.Habit
import com.example.madwellbeingapp.data.model.HabitLog
import com.example.madwellbeingapp.data.repository.HabitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository =
        (application as HabitApp).repository

    companion object {
        /**
         * Converts text to title-case: lowercase everything, then uppercase
         * the first letter of each word. Handles parentheses properly so
         * "GYM(LEGS)" → "Gym(Legs)" and "gym(legs)" → "Gym(Legs)".
         */
        fun toTitleCase(input: String): String {
            if (input.isBlank()) return input
            val sb = StringBuilder()
            var capitalizeNext = true
            for (ch in input) {
                when {
                    ch == '(' || ch == ')' || ch == ' ' || ch == '-' -> {
                        sb.append(ch)
                        capitalizeNext = true
                    }
                    capitalizeNext -> {
                        sb.append(ch.uppercaseChar())
                        capitalizeNext = false
                    }
                    else -> {
                        sb.append(ch.lowercaseChar())
                    }
                }
            }
            return sb.toString()
        }
    }

    // ── Activity Types (user-created, stored in Room) ───────
    val allActivityTypes: StateFlow<List<ActivityType>> = repository.allActivityTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Adds a new activity type. [onResult] returns true on success, false if duplicate.
     */
    fun addActivityType(name: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val formatted = toTitleCase(name.trim())
            if (formatted.isBlank()) {
                onResult(false)
                return@launch
            }
            val existing = repository.findActivityTypeByName(formatted)
            if (existing != null) {
                onResult(false)
                return@launch
            }
            repository.addActivityType(ActivityType(name = formatted))
            onResult(true)
        }
    }


    // ── All habits ──────────────────────────────────────────
    val allHabits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Today's logs ────────────────────────────────────────
    private val todayStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val todayLogs: StateFlow<List<HabitLog>> = repository.getLogsForDate(todayStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Active habits still to-do today.
     */
    val activeHabitsNotCompletedToday: StateFlow<List<Habit>> =
        combine(allHabits, todayLogs) { habits, logs ->
            val completedIds = logs.filter { it.completed }.map { it.habitId }.toSet()
            habits.filter { it.isActive && !completedIds.contains(it.id) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Selected habit detail ───────────────────────────────
    private val _selectedHabitId = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedHabit: StateFlow<Habit?> = _selectedHabitId
        .flatMapLatest { id ->
            if (id != null) repository.getHabitById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedHabitLogs: StateFlow<List<HabitLog>> = _selectedHabitId
        .flatMapLatest { id ->
            if (id != null) repository.getLogsForHabit(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Streak for selected habit ───────────────────────────
    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _longestStreak = MutableStateFlow(0)
    val longestStreak: StateFlow<Int> = _longestStreak.asStateFlow()

    fun selectHabit(habitId: Int) {
        _selectedHabitId.value = habitId
        viewModelScope.launch {
            val (current, longest) = computeStreakPair(habitId)
            _currentStreak.value = current
            _longestStreak.value = longest
        }
    }

    /** Returns (currentStreak, longestStreak) for a habit. */
    private suspend fun computeStreakPair(habitId: Int): Pair<Int, Int> {
        val logs = repository.getCompletedLogsDesc(habitId)
        if (logs.isEmpty()) return 0 to 0
        val days = logs.map { normaliseToDay(it.date) }.distinct().sortedDescending()
        // Current streak
        var current = 0
        val today = todayStart
        if (days.first() == today || days.first() == today - 86_400_000L) {
            var expected = days.first()
            for (day in days) {
                if (day == expected) { current++; expected -= 86_400_000L } else break
            }
        }
        // Longest streak
        var longest = 1; var streak = 1
        for (i in 1 until days.size) {
            if (days[i] == days[i - 1] - 86_400_000L) {
                streak++; if (streak > longest) longest = streak
            } else streak = 1
        }
        return current to longest
    }

    /** Public accessor for calendar/other screens needing just current streak. */
    suspend fun computeStreakForHabit(habitId: Int): Int = computeStreakPair(habitId).first

    // ── Weekly progress for a habit ─────────────────────────
    fun weeklyProgress(habitId: Int): Flow<Float> {
        return repository.getLogsForHabit(habitId).map { logs ->
            val weekAgo = todayStart - 6 * 86_400_000L
            val thisWeekCount = logs.count { it.completed && it.date >= weekAgo }
            val habit = repository.getHabitByIdOnce(habitId)
            val target = habit?.targetFrequency ?: 7
            (thisWeekCount.toFloat() / target).coerceIn(0f, 1f)
        }
    }

    // ── Duplicate check ─────────────────────────────────────
    suspend fun isDuplicate(name: String, details: String, excludeId: Int? = null): Boolean {
        val existing = repository.findDuplicate(name.trim(), details.trim())
        return existing != null && existing.id != excludeId
    }

    // ── CRUD operations ─────────────────────────────────────

    fun addHabit(
        name: String,
        details: String = "",
        activityType: String = "",
        targetFrequency: Int,
        reminderEnabled: Boolean = true,
        reminderHour: Int = 9,
        reminderMinute: Int = 0,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val formattedName = toTitleCase(name.trim())
            val formattedDetails = toTitleCase(details.trim())
            val formattedType = toTitleCase(activityType.trim())

            if (isDuplicate(formattedName, formattedDetails)) {
                onResult(false)
                return@launch
            }

            val habit = Habit(
                name = formattedName,
                details = formattedDetails,
                activityType = formattedType,
                startDate = System.currentTimeMillis(),
                targetFrequency = targetFrequency,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
            repository.insertHabit(habit)
            onResult(true)
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            val updated = habit.copy(
                name = toTitleCase(habit.name.trim()),
                details = toTitleCase(habit.details.trim()),
                activityType = toTitleCase(habit.activityType.trim())
            )
            repository.updateHabit(updated)
        }
    }

    fun disableHabit(habit: Habit) {
        viewModelScope.launch {
            val updated = habit.copy(isActive = false)
            repository.updateHabit(updated)
        }
    }

    fun enableHabit(habit: Habit) {
        viewModelScope.launch {
            val updated = habit.copy(isActive = true)
            repository.updateHabit(updated)
        }
    }

    // ── Daily logging ───────────────────────────────────────
    fun toggleTodayLog(habitId: Int) {
        viewModelScope.launch {
            repository.toggleLog(habitId, todayStart)
            if (_selectedHabitId.value == habitId) {
                val (c, l) = computeStreakPair(habitId)
                _currentStreak.value = c
                _longestStreak.value = l
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────
    private fun normaliseToDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // ── Calendar support ────────────────────────────────────
    fun getLogsBetween(startDate: Long, endDate: Long): Flow<List<HabitLog>> =
        repository.getLogsBetween(startDate, endDate)
}
