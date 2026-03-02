package com.example.madwellbeingapp.ui.viewmodel

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.madwellbeingapp.HabitApp
import com.example.madwellbeingapp.R
import com.example.madwellbeingapp.data.model.ActivityType
import com.example.madwellbeingapp.data.model.Habit
import com.example.madwellbeingapp.data.model.HabitLog
import com.example.madwellbeingapp.data.repository.HabitRepository
import com.example.madwellbeingapp.notification.HabitNotificationScheduler
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
        private const val TAG = "HabitViewModel"

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
                Log.w(TAG, "ACTIVITY TYPE DUPLICATE: '$formatted' already exists")
                onResult(false)
                return@launch
            }
            repository.addActivityType(ActivityType(name = formatted))
            Log.i(TAG, "ACTIVITY TYPE CREATED: '$formatted'")
            onResult(true)
        }
    }

    fun deleteActivityType(activityType: ActivityType) {
        viewModelScope.launch {
            repository.deleteActivityType(activityType)
            Log.i(TAG, "ACTIVITY TYPE DELETED: '${activityType.name}'")
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
        computeStreaks(habitId)
    }

    private fun computeStreaks(habitId: Int) {
        viewModelScope.launch {
            val logs = repository.getCompletedLogsDesc(habitId)
            if (logs.isEmpty()) {
                _currentStreak.value = 0
                _longestStreak.value = 0
                return@launch
            }
            val days = logs.map { normaliseToDay(it.date) }.distinct().sortedDescending()
            var current = 0
            val today = todayStart
            val yesterday = today - 86_400_000L
            if (days.first() == today || days.first() == yesterday) {
                var expected = days.first()
                for (day in days) {
                    if (day == expected) { current++; expected -= 86_400_000L } else break
                }
            }
            var longest = 1; var streak = 1
            for (i in 1 until days.size) {
                if (days[i] == days[i - 1] - 86_400_000L) {
                    streak++; if (streak > longest) longest = streak
                } else streak = 1
            }
            _currentStreak.value = current
            _longestStreak.value = longest
        }
    }

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
                Log.w(TAG, "DUPLICATE BLOCKED: '$formattedName($formattedDetails)' already exists")
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
            val id = repository.insertHabit(habit).toInt()
            Log.i(TAG, "HABIT CREATED: name='${habit.displayName}', type='$formattedType', frequency=${targetFrequency}x/week, id=$id")
            if (reminderEnabled) {
                HabitNotificationScheduler.schedule(
                    getApplication(), id, habit.displayName, reminderHour, reminderMinute
                )
            }
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
            Log.i(TAG, "HABIT UPDATED: name='${updated.displayName}', id=${updated.id}, type='${updated.activityType}', frequency=${updated.targetFrequency}x/week")
            if (updated.reminderEnabled) {
                HabitNotificationScheduler.schedule(
                    getApplication(), updated.id, updated.displayName,
                    updated.reminderHour, updated.reminderMinute
                )
            } else {
                HabitNotificationScheduler.cancel(getApplication(), updated.id)
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            HabitNotificationScheduler.cancel(getApplication(), habit.id)
            repository.deleteHabit(habit)
            Log.i(TAG, "HABIT DELETED: name='${habit.displayName}', id=${habit.id}")
        }
    }

    // ── Daily logging ───────────────────────────────────────
    fun toggleTodayLog(habitId: Int) {
        viewModelScope.launch {
            val existing = repository.getLogForDate(habitId, todayStart)
            val habit = repository.getHabitByIdOnce(habitId)
            repository.toggleLog(habitId, todayStart)
            if (existing == null) {
                Log.i(TAG, "HABIT COMPLETED: habit='${habit?.displayName}' (id=$habitId), date=$todayStart")
            } else {
                Log.i(TAG, "HABIT UNCOMPLETION: habit='${habit?.displayName}' (id=$habitId), date=$todayStart")
            }
            if (_selectedHabitId.value == habitId) computeStreaks(habitId)
            checkStreakCelebration(habitId)
        }
    }

    private suspend fun checkStreakCelebration(habitId: Int) {
        val logs = repository.getCompletedLogsDesc(habitId)
        if (logs.isEmpty()) return
        val days = logs.map { normaliseToDay(it.date) }.distinct().sortedDescending()
        if (days.first() != todayStart) return
        var streak = 0; var expected = todayStart
        for (day in days) {
            if (day == expected) { streak++; expected -= 86_400_000L } else break
        }
        if (streak > 0 && streak % 7 == 0) sendCelebration(habitId, streak)
    }

    private suspend fun sendCelebration(habitId: Int, streak: Int) {
        val habit = repository.getHabitByIdOnce(habitId) ?: return
        val ctx = getApplication<HabitApp>()
        Log.i(TAG, "STREAK MILESTONE: habit='${habit.displayName}', streak=$streak days!")
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(ctx, HabitApp.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Streak Milestone!")
                .setContentText("$streak-day streak on \"${habit.displayName}\"!")
                .setAutoCancel(true).build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(ctx)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Streak Milestone!")
                .setContentText("$streak-day streak on \"${habit.displayName}\"!")
                .setAutoCancel(true).build()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return
        }
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(habitId + 10_000, notification)
    }

    // ── Helpers ──────────────────────────────────────────────
    private fun normaliseToDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
