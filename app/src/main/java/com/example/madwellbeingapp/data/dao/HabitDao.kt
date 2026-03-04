package com.example.madwellbeingapp.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.madwellbeingapp.data.model.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Upsert
    suspend fun upsert(habit: Habit): Long


    @Query("SELECT * FROM habits ORDER BY name ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: Int): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitByIdOnce(id: Int): Habit?

    /** Check if a habit with the same name+details already exists (case-insensitive). */
    @Query("SELECT * FROM habits WHERE LOWER(name) = LOWER(:name) AND LOWER(details) = LOWER(:details) LIMIT 1")
    suspend fun findDuplicate(name: String, details: String): Habit?
}
