package com.example.madwellbeingapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a habit tracked by the user.
 * [name] is the activity type name chosen from the user-created dropdown (e.g. "Gym").
 * [details] is optional extra info typed by the user (e.g. "Legs", "5K").
 * The display name combines them: "Gym(Legs)" when details is present, or just "Gym".
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val details: String = "",
    val activityType: String = "",  // the user-created activity type name
    val startDate: Long, // epoch millis – auto-set on creation
    val targetFrequency: Int, // times per week the user wants to complete this habit
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 9, // default 09:00
    val reminderMinute: Int = 0,
    val isActive: Boolean = true
) {
    /** Returns the formatted display name: "Name(Details)" or just "Name". */
    val displayName: String
        get() = if (details.isBlank()) name else "$name($details)"
}
