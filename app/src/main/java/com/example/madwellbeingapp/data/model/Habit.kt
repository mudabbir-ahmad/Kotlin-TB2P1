package com.example.madwellbeingapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a habit tracked by the user.
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: HabitCategory,
    val startDate: Long, // epoch millis – auto-set on creation
    val targetFrequency: Int, // times per week the user wants to complete this habit
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 9, // default 09:00
    val reminderMinute: Int = 0
)

enum class HabitCategory(val displayName: String) {
    HEALTH("Health"),
    STUDY("Study"),
    FITNESS("Fitness"),
    WELLBEING("Wellbeing")
}

