package com.example.madwellbeingapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a habit tracked by the user.
 * [name] is the activity type in FULL CAPS (e.g. "GYM").
 * [details] is optional extra info with first-char uppercase (e.g. "Legs").
 * Display: "GYM(Legs)" when details present, or just "GYM".
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val details: String = "",
    val startDate: Long,
    val targetFrequency: Int,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val isActive: Boolean = true
) {
    /** Formatted display name: "NAME(Details)" or just "NAME". */
    val displayName: String
        get() {
            val upperName = name.uppercase()
            if (details.isBlank()) return upperName
            val fmt = if (details.length <= 1) details.uppercase()
            else details[0].uppercaseChar() + details.substring(1).lowercase()
            return "$upperName($fmt)"
        }
}
