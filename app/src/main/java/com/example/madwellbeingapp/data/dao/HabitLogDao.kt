package com.example.madwellbeingapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.madwellbeingapp.data.model.HabitLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: HabitLog): Long

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getLogForDate(habitId: Int, date: Long): HabitLog?

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLogForDate(habitId: Int, date: Long): Int

    @Query("SELECT COUNT(*) FROM habit_logs WHERE habitId = :habitId AND completed = 1")
    fun getCompletedCount(habitId: Int): Flow<Int>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND completed = 1 ORDER BY date DESC")
    suspend fun getCompletedLogsDesc(habitId: Int): List<HabitLog>

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun getLogsForDate(date: Long): Flow<List<HabitLog>>
}
