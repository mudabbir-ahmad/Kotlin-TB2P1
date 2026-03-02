package com.example.madwellbeingapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created activity type (e.g. "Gym", "Running", "Yoga").
 * These populate the dropdown when creating a new habit.
 */
@Entity(tableName = "activity_types")
data class ActivityType(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

