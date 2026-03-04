package com.example.madwellbeingapp.data.repository

import com.example.madwellbeingapp.data.dao.ActivityTypeDao
import com.example.madwellbeingapp.data.dao.HabitDao
import com.example.madwellbeingapp.data.dao.HabitLogDao
import com.example.madwellbeingapp.data.model.ActivityType
import com.example.madwellbeingapp.data.model.Habit
import com.example.madwellbeingapp.data.model.HabitLog
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for habit, log, and activity-type data.
 * Abstracts the data sources away from the ViewModels.
 */
class HabitRepository(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val activityTypeDao: ActivityTypeDao
) {
    // ── Habits ──────────────────────────────────────────────
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()

    fun getHabitById(id: Int): Flow<Habit?> = habitDao.getHabitById(id)

    suspend fun getHabitByIdOnce(id: Int): Habit? = habitDao.getHabitByIdOnce(id)

    suspend fun insertHabit(habit: Habit): Long = habitDao.upsert(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.upsert(habit)


    /** Returns an existing habit with the same name+details (case-insensitive), or null. */
    suspend fun findDuplicate(name: String, details: String): Habit? =
        habitDao.findDuplicate(name, details)

    // ── Activity Types ──────────────────────────────────────
    val allActivityTypes: Flow<List<ActivityType>> = activityTypeDao.getAll()

    suspend fun addActivityType(activityType: ActivityType): Long =
        activityTypeDao.upsert(activityType)


    suspend fun findActivityTypeByName(name: String): ActivityType? =
        activityTypeDao.findByName(name)

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
            habitLogDao.upsert(HabitLog(habitId = habitId, date = date, completed = true))
        }
    }

    fun getCompletedCount(habitId: Int): Flow<Int> =
        habitLogDao.getCompletedCount(habitId)

    suspend fun getCompletedLogsDesc(habitId: Int): List<HabitLog> =
        habitLogDao.getCompletedLogsDesc(habitId)

    fun getLogsBetween(startDate: Long, endDate: Long): Flow<List<HabitLog>> =
        habitLogDao.getLogsBetween(startDate, endDate)
}
