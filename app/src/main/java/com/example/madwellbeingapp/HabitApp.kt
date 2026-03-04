package com.example.madwellbeingapp

import android.app.Application
import com.example.madwellbeingapp.data.HabitDatabase
import com.example.madwellbeingapp.data.repository.HabitRepository

/**
 * Application subclass that provides the database and repository singletons.
 */
class HabitApp : Application() {

    val database by lazy { HabitDatabase.getDatabase(this) }
    val repository by lazy {
        HabitRepository(database.habitDao(), database.habitLogDao(), database.activityTypeDao())
    }

    companion object {
        const val CHANNEL_ID = "habit_reminders"
    }
}
