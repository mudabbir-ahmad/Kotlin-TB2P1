package com.example.madwellbeingapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a habit tracked by the user.
 * [name] is the activity type name in FULL CAPS (e.g. "GYM").
 * [details] is optional extra info with first-char uppercase (e.g. "Legs", "5k").
 * The display name combines them: "GYM(Legs)" when details is present, or just "GYM".
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
    /** Returns the formatted display name: "NAME(Details)" or just "NAME". */
    val displayName: String
        get() {
            val upperName = name.uppercase()
            if (details.isBlank()) return upperName
            val formattedDetails = if (details.length <= 1) details.uppercase()
            else details[0].uppercaseChar() + details.substring(1).lowercase()
            return "$upperName($formattedDetails)"
        }
}
