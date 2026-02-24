package com.example.madwellbeingapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.madwellbeingapp.HabitApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-schedules all habit alarms after the device reboots.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as HabitApp
            CoroutineScope(Dispatchers.IO).launch {
                val habits = app.repository.allHabits.first()
                for (habit in habits) {
                    if (habit.reminderEnabled) {
                        HabitNotificationScheduler.schedule(
                            context, habit.id, habit.name,
                            habit.reminderHour, habit.reminderMinute
                        )
                    }
                }
            }
        }
    }
}

