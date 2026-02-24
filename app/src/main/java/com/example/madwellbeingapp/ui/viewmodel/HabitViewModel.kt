package com.example.madwellbeingapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.madwellbeingapp.HabitApp
import com.example.madwellbeingapp.R
import com.example.madwellbeingapp.data.model.Habit
import com.example.madwellbeingapp.data.model.HabitCategory
import com.example.madwellbeingapp.data.model.HabitLog
import com.example.madwellbeingapp.data.repository.HabitRepository
import com.example.madwellbeingapp.notification.HabitNotificationScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository =
        (application as HabitApp).repository

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

    // ── Selected habit detail ───────────────────────────────
    private val _selectedHabitId = MutableStateFlow<Int?>(null)
    val selectedHabitId = _selectedHabitId.asStateFlow()

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

            // Normalise dates to day-only & sort descending
            val days = logs.map { normaliseToDay(it.date) }.distinct().sortedDescending()

            // Current streak: consecutive days ending today or yesterday
            var current = 0
            val today = todayStart
            val yesterday = today - 86_400_000L
            if (days.first() == today || days.first() == yesterday) {
                var expected = days.first()
                for (day in days) {
                    if (day == expected) {
                        current++
                        expected -= 86_400_000L
                    } else break
                }
            }

            // Longest streak
            var longest = 1
            var streak = 1
            val sorted = days.sortedDescending()
            for (i in 1 until sorted.size) {
                if (sorted[i] == sorted[i - 1] - 86_400_000L) {
                    streak++
                    if (streak > longest) longest = streak
                } else {
                    streak = 1
                }
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

    // ── CRUD operations ─────────────────────────────────────
    fun addHabit(
        name: String,
        category: HabitCategory,
        targetFrequency: Int,
        reminderEnabled: Boolean = true,
        reminderHour: Int = 9,
        reminderMinute: Int = 0
    ) {
        viewModelScope.launch {
            val habit = Habit(
                name = name,
                category = category,
                startDate = System.currentTimeMillis(),
                targetFrequency = targetFrequency,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
            val id = repository.insertHabit(habit).toInt()
            if (reminderEnabled) {
                HabitNotificationScheduler.schedule(
                    getApplication(), id, name, reminderHour, reminderMinute
                )
            }
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            if (habit.reminderEnabled) {
                HabitNotificationScheduler.schedule(
                    getApplication(), habit.id, habit.name,
                    habit.reminderHour, habit.reminderMinute
                )
            } else {
                HabitNotificationScheduler.cancel(getApplication(), habit.id)
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            HabitNotificationScheduler.cancel(getApplication(), habit.id)
            repository.deleteHabit(habit)
        }
    }

    // ── Daily logging ───────────────────────────────────────
    fun toggleTodayLog(habitId: Int) {
        viewModelScope.launch {
            repository.toggleLog(habitId, todayStart)
            // Refresh streaks if currently viewing this habit
            if (_selectedHabitId.value == habitId) {
                computeStreaks(habitId)
            }
            // Celebrate streak milestones
            checkStreakCelebration(habitId)
        }
    }

    private suspend fun checkStreakCelebration(habitId: Int) {
        val logs = repository.getCompletedLogsDesc(habitId)
        if (logs.isEmpty()) return
        val days = logs.map { normaliseToDay(it.date) }.distinct().sortedDescending()
        val today = todayStart
        if (days.first() != today) return
        var streak = 0
        var expected = today
        for (day in days) {
            if (day == expected) {
                streak++
                expected -= 86_400_000L
            } else break
        }
        if (streak > 0 && streak % 7 == 0) {
            // Send a celebration notification
            sendCelebration(habitId, streak)
        }
    }

    private suspend fun sendCelebration(habitId: Int, streak: Int) {
        val habit = repository.getHabitByIdOnce(habitId) ?: return
        val ctx = getApplication<HabitApp>()
        val notification = androidx.core.app.NotificationCompat.Builder(ctx, HabitApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎉 Streak Milestone!")
            .setContentText("Amazing! You've hit a $streak-day streak on \"${habit.name}\"!")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            androidx.core.app.NotificationManagerCompat.from(ctx)
                .notify(habitId + 10_000, notification)
        }
    }

    // ── Helpers ──────────────────────────────────────────────
    private fun normaliseToDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}


