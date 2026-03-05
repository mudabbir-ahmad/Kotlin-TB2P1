package com.example.madwellbeingapp.ui.viewmodel

import android.app.Application
import android.util.Log
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

private const val TAG = "HabitViewModel"
private const val DAY_MS = 86_400_000L

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository =
        (application as HabitApp).repository

    // ── Text formatting helpers (static) ────────────────────
    companion object {
        /** Activity name → FULL CAPS.  "gym" → "GYM" */
        fun formatName(input: String): String = input.trim().uppercase()

        /** Detail text → first char upper, rest lower.  "LEGS" → "Legs" */
        fun formatDetails(input: String): String {
            val t = input.trim()
            if (t.isBlank()) return t
            return t[0].uppercaseChar() + t.substring(1).lowercase()
        }

        /** Normalise epoch millis to the start-of-day (00:00:00.000). */
        fun startOfDay(millis: Long): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = millis
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        /** Today at 00:00:00.000. */
        fun todayMillis(): Long = startOfDay(System.currentTimeMillis())
    }

    // ── Activity Types ──────────────────────────────────────
    val allActivityTypes: StateFlow<List<ActivityType>> = repository.allActivityTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addActivityType(name: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val formatted = formatName(name)
            if (formatted.isBlank()) { onResult(false); return@launch }
            if (repository.findActivityTypeByName(formatted) != null) {
                onResult(false); return@launch
            }
            repository.addActivityType(ActivityType(name = formatted))
            Log.d(TAG, "Activity type created: $formatted")
            onResult(true)
        }
    }

    // ── All habits ──────────────────────────────────────────
    val allHabits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Today's logs ────────────────────────────────────────
    private val todayStart: Long get() = todayMillis()

    val todayLogs: StateFlow<List<HabitLog>> = repository.getLogsForDate(todayStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Active habits not yet completed today. */
    val activeHabitsNotCompletedToday: StateFlow<List<Habit>> =
        combine(allHabits, todayLogs) { habits, logs ->
            val doneIds = logs.filter { it.completed }.map { it.habitId }.toSet()
            habits.filter { it.isActive && it.id !in doneIds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Selected habit detail ───────────────────────────────
    private val _selectedHabitId = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedHabit: StateFlow<Habit?> = _selectedHabitId
        .flatMapLatest { id -> if (id != null) repository.getHabitById(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedHabitLogs: StateFlow<List<HabitLog>> = _selectedHabitId
        .flatMapLatest { id -> if (id != null) repository.getLogsForHabit(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Streaks ─────────────────────────────────────────────
    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _longestStreak = MutableStateFlow(0)
    val longestStreak: StateFlow<Int> = _longestStreak.asStateFlow()

    fun selectHabit(habitId: Int) {
        _selectedHabitId.value = habitId
        viewModelScope.launch {
            val (cur, lng) = computeStreaks(habitId)
            _currentStreak.value = cur
            _longestStreak.value = lng
        }
    }

    suspend fun computeStreakForHabit(habitId: Int): Int = computeStreaks(habitId).first

    private suspend fun computeStreaks(habitId: Int): Pair<Int, Int> {
        val logs = repository.getCompletedLogsDesc(habitId)
        if (logs.isEmpty()) return 0 to 0
        val days = logs.map { startOfDay(it.date) }.distinct().sortedDescending()

        // Current streak
        var current = 0
        val today = todayStart
        if (days.first() == today || days.first() == today - DAY_MS) {
            var expected = days.first()
            for (day in days) {
                if (day == expected) { current++; expected -= DAY_MS } else break
            }
        }
        // Longest streak
        var longest = 1; var run = 1
        for (i in 1 until days.size) {
            if (days[i] == days[i - 1] - DAY_MS) { run++; if (run > longest) longest = run }
            else run = 1
        }
        return current to longest
    }

    // ── Weekly progress ─────────────────────────────────────
    fun weeklyProgress(habitId: Int): Flow<Float> =
        repository.getLogsForHabit(habitId).map { logs ->
            val weekAgo = todayStart - 6 * DAY_MS
            val count = logs.count { it.completed && it.date >= weekAgo }
            val target = repository.getHabitByIdOnce(habitId)?.targetFrequency ?: 7
            (count.toFloat() / target).coerceIn(0f, 1f)
        }

    // ── Duplicate check ─────────────────────────────────────
    private suspend fun isDuplicate(name: String, details: String, excludeId: Int? = null): Boolean {
        val existing = repository.findDuplicate(name, details)
        return existing != null && existing.id != excludeId
    }

    // ── CRUD ────────────────────────────────────────────────
    fun addHabit(
        name: String,
        details: String = "",
        targetFrequency: Int,
        reminderEnabled: Boolean = true,
        reminderHour: Int = 9,
        reminderMinute: Int = 0,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val fmtName = formatName(name)
            val fmtDetails = formatDetails(details)

            if (isDuplicate(fmtName, fmtDetails)) { onResult(false); return@launch }

            repository.upsertHabit(
                Habit(
                    name = fmtName, details = fmtDetails,
                    startDate = System.currentTimeMillis(),
                    targetFrequency = targetFrequency,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour, reminderMinute = reminderMinute
                )
            )
            Log.d(TAG, "Habit created: $fmtName($fmtDetails)")
            onResult(true)
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.upsertHabit(
                habit.copy(
                    name = formatName(habit.name),
                    details = formatDetails(habit.details)
                )
            )
            Log.d(TAG, "Habit updated: ${habit.displayName}")
        }
    }

    fun disableHabit(habit: Habit) {
        viewModelScope.launch { repository.upsertHabit(habit.copy(isActive = false)) }
    }

    fun enableHabit(habit: Habit) {
        viewModelScope.launch { repository.upsertHabit(habit.copy(isActive = true)) }
    }

    // ── Daily logging ───────────────────────────────────────
    fun toggleTodayLog(habitId: Int) {
        viewModelScope.launch {
            repository.toggleLog(habitId, todayStart)
            Log.d(TAG, "Toggled today's log for habit id=$habitId")
            if (_selectedHabitId.value == habitId) {
                val (c, l) = computeStreaks(habitId)
                _currentStreak.value = c; _longestStreak.value = l
            }
        }
    }

    // ── Calendar support ────────────────────────────────────
    fun getLogsBetween(start: Long, end: Long): Flow<List<HabitLog>> =
        repository.getLogsBetween(start, end)
}
