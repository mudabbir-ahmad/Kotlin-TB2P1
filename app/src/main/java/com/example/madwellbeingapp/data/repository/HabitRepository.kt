package com.example.madwellbeingapp.data.repository

import com.example.madwellbeingapp.data.dao.HabitDao
import com.example.madwellbeingapp.data.dao.HabitLogDao
import com.example.madwellbeingapp.data.model.Habit
import com.example.madwellbeingapp.data.model.HabitLog
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for habit and log data.
 * Abstracts the data sources away from the ViewModels.
 */
class HabitRepository(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao
) {
    // ── Habits ──────────────────────────────────────────────
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()

    fun getHabitById(id: Int): Flow<Habit?> = habitDao.getHabitById(id)

    suspend fun getHabitByIdOnce(id: Int): Habit? = habitDao.getHabitByIdOnce(id)

    suspend fun insertHabit(habit: Habit): Long = habitDao.insert(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.update(habit)

    suspend fun deleteHabit(habit: Habit) = habitDao.delete(habit)

    // ── Logs ────────────────────────────────────────────────
    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>> =
        habitLogDao.getLogsForHabit(habitId)

    fun getLogsForDate(date: Long): Flow<List<HabitLog>> =
        habitLogDao.getLogsForDate(date)

    suspend fun getLogForDate(habitId: Int, date: Long): HabitLog? =
        habitLogDao.getLogForDate(habitId, date)

    suspend fun toggleLog(habitId: Int, date: Long) {
        val existing = habitLogDao.getLogForDate(habitId, date)
        if (existing != null) {
            habitLogDao.deleteLogForDate(habitId, date)
        } else {
            habitLogDao.insert(HabitLog(habitId = habitId, date = date, completed = true))
        }
    }

    fun getCompletedCount(habitId: Int): Flow<Int> =
        habitLogDao.getCompletedCount(habitId)

    suspend fun getCompletedLogsDesc(habitId: Int): List<HabitLog> =
        habitLogDao.getCompletedLogsDesc(habitId)
}

