package com.example.madwellbeingapp.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.madwellbeingapp.data.model.ActivityType
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityTypeDao {

    @Upsert
    suspend fun upsert(activityType: ActivityType): Long


    @Query("SELECT * FROM activity_types ORDER BY name ASC")
    fun getAll(): Flow<List<ActivityType>>

    /** Check if an activity type with this name already exists (case-insensitive). */
    @Query("SELECT * FROM activity_types WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): ActivityType?
}

