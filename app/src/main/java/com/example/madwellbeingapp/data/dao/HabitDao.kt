package com.example.madwellbeingapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.madwellbeingapp.data.model.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit): Int

    @Delete
    suspend fun delete(habit: Habit): Int

    @Query("SELECT * FROM habits ORDER BY name ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: Int): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitByIdOnce(id: Int): Habit?
}
