package com.example.madwellbeingapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
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

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders for your habits"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "habit_reminders"
    }
}

